import { generateImage, generateExcel } from "../ai/client";
import { getAccountPool, PoolIterator } from "../ai/pool";
import {
    getExcelFull,
    saveGeneratedContent,
    setImageKey,
    setStatus,
} from "../db";
import { uploadExcelImage } from "../storage/kv";
import type { Bindings } from "../types";

/**
 * Cron tiap jam untuk proses draft dari bulk sync (GAS spreadsheet). Demand-driven
 * requests dari user TIDAK di-pick oleh cron — mereka di-dispatch langsung via
 * ctx.waitUntil di route POST /api/requests (lihat src/service/generator.ts).
 */
export async function autoProcessDrafts(env: Bindings): Promise<void> {
    const { results: drafts } = await env.DB.prepare(
        "SELECT id FROM excels WHERE status = 'draft' LIMIT 1",
    ).all<{ id: string }>();

    if (!drafts || drafts.length === 0) {
        console.log("Autobot: Tidak ada draft untuk diproses.");
        return;
    }

    const pool = await getAccountPool(env.IMAGES, env.AI_POOL_URL, env.ACCOUNT_POOL_JSON);
    console.log(`Autobot: Pool AI siap dengan ${pool.length} akun.`);

    for (const draft of drafts) {
        try {
            console.log(`Autobot: Menghasilkan tutorial untuk ${draft.id}...`);
            const full = await getExcelFull(env.DB, draft.id, "");
            if (!full) continue;

            const [text, image] = await Promise.all([
                generateExcel(new PoolIterator(pool), full.excel.title, full.excel.category),
                generateImage(new PoolIterator(pool), full.excel.title, full.excel.category),
            ]);

            await saveGeneratedContent(env.DB, draft.id, text);
            const key = await uploadExcelImage(env.IMAGES, draft.id, image);
            await setImageKey(env.DB, draft.id, key);
            await setStatus(env.DB, draft.id, "published");

            console.log(`Autobot: SUCCESS ${draft.id} published.`);
        } catch (e) {
            console.error(`Autobot: FAILED ${draft.id}:`, e);
        }
    }
}
