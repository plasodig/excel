// Layer query D1. Mengembalikan tipe tegas, tidak ada raw SQL bocor ke route.

import type {
  Category,
  GeneratedExcelContent,
  GenerationRequestRow,
  IngredientRow,
  ExcelFull,
  ExcelReportRow,
  ExcelRow,
  ExcelStatus,
  ReportReason,
  StepRow,
} from "./types";

/** Ambang auto-unpublish: kalau excel punya >= N reports, set status ke 'generated'. */
export const REPORT_UNPUBLISH_THRESHOLD = 3;

export async function listExcels(db: D1Database): Promise<ExcelRow[]> {
  const { results } = await db
    .prepare(
      `SELECT * FROM excels ORDER BY
       CASE status WHEN 'draft' THEN 0 WHEN 'generated' THEN 1 ELSE 2 END,
       title ASC`,
    )
    .all<ExcelRow>();
  return results ?? [];
}

export async function listPublishedExcels(db: D1Database): Promise<ExcelRow[]> {
  const { results } = await db
    .prepare(`SELECT * FROM excels WHERE status='published' ORDER BY title ASC`)
    .all<ExcelRow>();
  return results ?? [];
}

export async function listPublishedExcelsPaged(
  db: D1Database,
  opts: { category?: string; limit: number; offset: number },
): Promise<ExcelRow[]> {
  const limit = Math.max(1, Math.min(opts.limit, 100));
  const offset = Math.max(0, opts.offset);
  const sql = opts.category
    ? `SELECT * FROM excels WHERE status='published' AND category = ? ORDER BY title ASC LIMIT ? OFFSET ?`
    : `SELECT * FROM excels WHERE status='published' ORDER BY title ASC LIMIT ? OFFSET ?`;
  const stmt = opts.category
    ? db.prepare(sql).bind(opts.category, limit, offset)
    : db.prepare(sql).bind(limit, offset);
  const { results } = await stmt.all<ExcelRow>();
  return results ?? [];
}

export async function countPublishedExcels(
  db: D1Database,
  category?: string,
): Promise<number> {
  const sql = category
    ? `SELECT COUNT(*) AS n FROM excels WHERE status='published' AND category = ?`
    : `SELECT COUNT(*) AS n FROM excels WHERE status='published'`;
  const stmt = category ? db.prepare(sql).bind(category) : db.prepare(sql);
  const row = await stmt.first<{ n: number }>();
  return row?.n ?? 0;
}

export async function getExcelFull(
  db: D1Database,
  id: string,
  publicBase: string,
): Promise<ExcelFull | null> {
  const excel = await db
    .prepare(`SELECT * FROM excels WHERE id = ?`)
    .bind(id)
    .first<ExcelRow>();
  if (!excel) return null;

  const ing = await db
    .prepare(`SELECT * FROM ingredients WHERE excel_id = ? ORDER BY position ASC`)
    .bind(id)
    .all<IngredientRow>();
  const steps = await db
    .prepare(`SELECT * FROM excel_steps WHERE excel_id = ? ORDER BY step_order ASC`)
    .bind(id)
    .all<StepRow>();

  return {
    excel,
    ingredients: ing.results ?? [],
    steps: steps.results ?? [],
    imageUrl: imageUrlFor(excel.image_key, publicBase),
  };
}

export function imageUrlFor(imageKey: string | null, _publicBase?: string): string | null {
  if (!imageKey) return null;
  // KV tidak punya public URL langsung, jadi kita arahkan ke worker endpoint kita sendiri.
  return `/api/images/${imageKey}`;
}

export async function saveGeneratedContent(
  db: D1Database,
  id: string,
  content: GeneratedExcelContent,
): Promise<void> {
  const now = Date.now();
  const tagsCsv = content.tags.join(",");

  // D1 tidak mendukung BEGIN..COMMIT lewat exec; pakai batch untuk atomicity.
  const stmts: D1PreparedStatement[] = [
    db.prepare(`DELETE FROM ingredients WHERE excel_id = ?`).bind(id),
    db.prepare(`DELETE FROM excel_steps WHERE excel_id = ?`).bind(id),
    db
      .prepare(
        `UPDATE excels SET
           description = ?, difficulty = ?, cooking_time_minutes = ?, servings = ?,
           tags_csv = ?, generated_at = ?, updated_at = ?,
           status = CASE WHEN status='published' THEN 'published' ELSE 'generated' END
         WHERE id = ?`,
      )
      .bind(
        content.description,
        content.difficulty,
        content.cookingTimeMinutes,
        content.servings,
        tagsCsv,
        now,
        now,
        id,
      ),
  ];

  content.ingredients.forEach((ing, idx) => {
    stmts.push(
      db
        .prepare(
          `INSERT INTO ingredients (excel_id, position, name, quantity, unit) VALUES (?, ?, ?, ?, ?)`,
        )
        .bind(id, idx + 1, ing.name, ing.quantity, ing.unit),
    );
  });
  content.steps.forEach((step) => {
    stmts.push(
      db
        .prepare(
          `INSERT INTO excel_steps (excel_id, step_order, instruction) VALUES (?, ?, ?)`,
        )
        .bind(id, step.order, step.instruction),
    );
  });

  await db.batch(stmts);
}

export async function setImageKey(
  db: D1Database,
  id: string,
  imageKey: string,
): Promise<void> {
  const now = Date.now();
  await db
    .prepare(
      `UPDATE excels SET image_key = ?, image_generated_at = ?, updated_at = ? WHERE id = ?`,
    )
    .bind(imageKey, now, now, id)
    .run();
}

export async function setStatus(
  db: D1Database,
  id: string,
  status: ExcelStatus,
): Promise<void> {
  const now = Date.now();
  const publishedAt = status === "published" ? now : null;
  await db
    .prepare(
      `UPDATE excels SET status = ?, published_at = COALESCE(?, published_at), updated_at = ? WHERE id = ?`,
    )
    .bind(status, publishedAt, now, id)
    .run();
}

export async function updateMeta(
  db: D1Database,
  id: string,
  patch: {
    title?: string;
    category?: Category;
    description?: string;
    difficulty?: string;
    cookingTimeMinutes?: number;
    servings?: number;
    tagsCsv?: string;
  },
): Promise<void> {
  const fields: string[] = [];
  const values: (string | number)[] = [];
  if (patch.title !== undefined) { fields.push("title = ?"); values.push(patch.title); }
  if (patch.category !== undefined) { fields.push("category = ?"); values.push(patch.category); }
  if (patch.description !== undefined) { fields.push("description = ?"); values.push(patch.description); }
  if (patch.difficulty !== undefined) { fields.push("difficulty = ?"); values.push(patch.difficulty); }
  if (patch.cookingTimeMinutes !== undefined) { fields.push("cooking_time_minutes = ?"); values.push(patch.cookingTimeMinutes); }
  if (patch.servings !== undefined) { fields.push("servings = ?"); values.push(patch.servings); }
  if (patch.tagsCsv !== undefined) { fields.push("tags_csv = ?"); values.push(patch.tagsCsv); }
  if (fields.length === 0) return;
  fields.push("updated_at = ?");
  values.push(Date.now());
  values.push(id);
  await db.prepare(`UPDATE excels SET ${fields.join(", ")} WHERE id = ?`).bind(...values).run();
}

export async function syncExcels(
  db: D1Database,
  data: { name: string; category: Category }[]
): Promise<{ added: number; skipped: number }> {
  let added = 0;
  let skipped = 0;

  // Kita gunakan batch untuk efisiensi. 
  // Strategi: INSERT OR IGNORE berdasarkan ID (ID di-generate dari nama yang di-slugify).
  const stmts: D1PreparedStatement[] = [];

  for (const item of data) {
    const slug = item.name
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "");

    if (!slug) continue;

    stmts.push(
      db
        .prepare(`INSERT OR IGNORE INTO excels (id, title, category, status, updated_at) VALUES (?, ?, ?, 'draft', ?)`)
        .bind(slug, item.name, item.category, Date.now())
    );
  }

  const results = await db.batch(stmts);
  results.forEach(r => {
    if (r.meta.changes > 0) added++;
    else skipped++;
  });

  return { added, skipped };
}

// ----- generation_requests -------------------------------------------------

export function slugifyQuery(raw: string): string {
  return raw
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export async function findExcelById(
  db: D1Database,
  id: string,
): Promise<ExcelRow | null> {
  const row = await db.prepare(`SELECT * FROM excels WHERE id = ?`).bind(id).first<ExcelRow>();
  return row ?? null;
}

export async function createProcessingRequest(
  db: D1Database,
  req: {
    id: string;
    query: string;
    slugTarget: string;
    clientIp: string | null;
  },
): Promise<void> {
  await db
    .prepare(
      `INSERT INTO generation_requests
       (id, query, slug_target, status, client_ip, requested_at)
       VALUES (?, ?, ?, 'processing', ?, ?)`,
    )
    .bind(req.id, req.query, req.slugTarget, req.clientIp, Date.now())
    .run();
}

export async function getRequest(
  db: D1Database,
  id: string,
): Promise<GenerationRequestRow | null> {
  const row = await db
    .prepare(`SELECT * FROM generation_requests WHERE id = ?`)
    .bind(id)
    .first<GenerationRequestRow>();
  return row ?? null;
}

/**
 * Cari request processing untuk slug yang sama → dedup supaya tidak spawn 2 job paralel
 * untuk tutorial yang sama.
 */
export async function findProcessingRequestBySlug(
  db: D1Database,
  slug: string,
): Promise<GenerationRequestRow | null> {
  const row = await db
    .prepare(
      `SELECT * FROM generation_requests
       WHERE slug_target = ? AND status = 'processing'
       ORDER BY requested_at DESC
       LIMIT 1`,
    )
    .bind(slug)
    .first<GenerationRequestRow>();
  return row ?? null;
}

export async function listAllRequests(
  db: D1Database,
  limit: number = 50,
): Promise<GenerationRequestRow[]> {
  const { results } = await db
    .prepare(
      `SELECT * FROM generation_requests
       ORDER BY requested_at DESC
       LIMIT ?`,
    )
    .bind(limit)
    .all<GenerationRequestRow>();
  return results ?? [];
}

export async function markRequestCompleted(
  db: D1Database,
  id: string,
  excelId: string,
): Promise<void> {
  await db
    .prepare(
      `UPDATE generation_requests
       SET status = 'completed', completed_at = ?, resulting_excel_id = ?
       WHERE id = ?`,
    )
    .bind(Date.now(), excelId, id)
    .run();
}

export async function markRequestFailed(
  db: D1Database,
  id: string,
  errorMessage: string,
): Promise<void> {
  await db
    .prepare(
      `UPDATE generation_requests
       SET status = 'failed', completed_at = ?, error_message = ?
       WHERE id = ?`,
    )
    .bind(Date.now(), errorMessage, id)
    .run();
}

// ----- excel_reports ------------------------------------------------------

/**
 * Simpan satu report user + increment counter di excels. Kalau setelah increment
 * count >= threshold, auto-unpublish (set status='generated'). Return status terbaru.
 */
export async function createReport(
  db: D1Database,
  req: {
    id: string;
    excelId: string;
    reason: ReportReason;
    detail: string;
    clientIp: string | null;
  },
): Promise<{ reportCount: number; unpublished: boolean }> {
  const now = Date.now();
  await db.batch([
    db
      .prepare(
        `INSERT INTO excel_reports (id, excel_id, reason, detail, client_ip, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
      )
      .bind(req.id, req.excelId, req.reason, req.detail, req.clientIp, now),
    db
      .prepare(`UPDATE excels SET report_count = report_count + 1 WHERE id = ?`)
      .bind(req.excelId),
  ]);

  const row = await db
    .prepare(`SELECT report_count, status FROM excels WHERE id = ?`)
    .bind(req.excelId)
    .first<{ report_count: number; status: string }>();
  const reportCount = row?.report_count ?? 0;

  let unpublished = false;
  if (row?.status === "published" && reportCount >= REPORT_UNPUBLISH_THRESHOLD) {
    await db
      .prepare(`UPDATE excels SET status = 'generated', updated_at = ? WHERE id = ?`)
      .bind(now, req.excelId)
      .run();
    unpublished = true;
  }

  return { reportCount, unpublished };
}

export type ReportWithExcel = ExcelReportRow & {
  excel_title: string | null;
  excel_status: string | null;
};

export async function listRecentReports(
  db: D1Database,
  limit: number = 50,
): Promise<ReportWithExcel[]> {
  const { results } = await db
    .prepare(
      `SELECT r.*, excels.title AS excel_title, excels.status AS excel_status
       FROM excel_reports r
       LEFT JOIN excels ON excels.id = r.excel_id
       ORDER BY r.created_at DESC
       LIMIT ?`,
    )
    .bind(limit)
    .all<ReportWithExcel>();
  return results ?? [];
}
