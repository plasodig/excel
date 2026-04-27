// Content safety gate untuk demand-driven generation Tutorial Excel.
// Layer pertahanan berlapis:
//   1. Query validation — tolak input yang tidak relevan dengan Excel/spreadsheet.
//   2. Prompt hardening — system prompt sudah tolak non-Excel topic (lihat prompts.ts).
//   3. Post-generation validator — tolak output dengan keuangan personal/medis/syntax invalid.
//
// Semua pemeriksaan murni string matching, tidak pakai LLM tambahan → gratis dan deterministik.

import type { GeneratedExcelContent } from "../types";

/**
 * Keyword yang LANGSUNG block di sisi query. Case-insensitive. Berfokus kategori yang
 * paling berisiko Google Play policy + topik di luar scope Excel.
 */
const QUERY_BLOCKLIST: readonly string[] = [
  // NSFW / dewasa
  "porn", "sex", "seks", "telanjang", "bugil", "nude", "erotic", "bdsm",
  // Kekerasan / senjata
  "bomb", "bom ", "senjata", "gun", "pistol", "bunuh", "pembunuh",
  // Narkoba
  "narkoba", "ganja", "cocaine", "heroin", "sabu", "meth", "ekstasi", "ecstasy",
  "opium", "mariyuana", "marijuana",
  // Klaim medis (bukan ranah Excel)
  "obat ", "penyembuh", "menyembuhkan", "terapi medis", "diagnosis",
  // Saran keuangan personal / investasi (regulasi OJK)
  "saham bagus", "trading saham", "investasi terbaik", "kripto bagus",
  "pinjol", "pinjam online", "rekomendasi reksadana",
  // Topik politik / agama / hate
  "politik partai", "kampanye partai", "hitler", "nazi", "rasis",
  "self harm", "bunuh diri", "suicide",
  // Software lain — di luar scope Excel
  "google docs", "powerpoint", "microsoft word ", "photoshop",
  "tutorial canva", "tutorial capcut",
  // Crack / pirated
  "crack excel", "bajak excel", "excel gratis full", "serial key",
];

/**
 * Whitelist hint: keyword Excel-related yang boost confidence query valid.
 * Tidak strict — query yang tidak match whitelist masih bisa lolos kalau lulus blocklist.
 */
export const EXCEL_HINTS: readonly string[] = [
  "excel", "spreadsheet", "rumus", "formula", "fungsi", "function",
  "vlookup", "hlookup", "xlookup", "index", "match", "sumif", "countif",
  "averageif", "if", "ifs", "iferror", "pivottable", "pivot table", "pivot",
  "chart", "grafik", "macro", "makro", "vba", "filter", "sort", "format",
  "conditional formatting", "format kondisional", "data validation",
  "absensi", "rekap", "gaji", "umkm", "stok", "omzet", "penjualan",
  "tabel", "kolom", "baris", "cell", "sel", "sheet", "workbook",
];

const MIN_QUERY_LEN = 3;
const MAX_QUERY_LEN = 100;

export interface SafetyResult {
  ok: boolean;
  reason?: string;
}

export function validateExcelQuery(rawQuery: string): SafetyResult {
  const q = rawQuery.toLowerCase().trim();
  if (q.length < MIN_QUERY_LEN) {
    return { ok: false, reason: "Query terlalu pendek (min 3 karakter)." };
  }
  if (q.length > MAX_QUERY_LEN) {
    return { ok: false, reason: "Query terlalu panjang (max 100 karakter)." };
  }
  if (!/[a-z]/.test(q)) {
    return { ok: false, reason: "Query harus mengandung huruf." };
  }
  for (const bad of QUERY_BLOCKLIST) {
    if (q.includes(bad)) {
      return { ok: false, reason: "Query di luar topik Excel atau mengandung kata terlarang." };
    }
  }
  // Soft check: kalau tidak ada hint Excel sama sekali, kasih warning halus
  // (tidak block — biarkan AI yang final-decide via prompt tolak {"error":"not_excel_topic"}).
  return { ok: true };
}

/**
 * Pattern yang TIDAK BOLEH muncul di output: klaim medis, saran finansial spesifik,
 * atau syntax fungsi yang jelas-jelas tidak ada di Excel.
 */
const REJECT_PATTERNS: readonly RegExp[] = [
  // Klaim medis
  /\bmenyembuhkan\b/i,
  /\bobat\s+(untuk|alami)\b/i,
  /\bmengobati\s+(penyakit|kanker|diabetes|jantung)\b/i,
  /\bterapi\s+medis\b/i,
  // Saran finansial personal
  /\bsaham\s+(yang\s+pasti|terbaik\s+tahun)\b/i,
  /\bpinjam\s+online\b/i,
  /\binvestasi\s+(jamin|pasti\s+untung)\b/i,
  // Syntax invalid (fungsi yang dibuat-buat AI)
  /=\s*XYLOOKUP\b/i,    // contoh: tidak ada XYLOOKUP
  /=\s*MEGALOOKUP\b/i,  // hallucinated
];

/**
 * Validasi hasil generate AI — reject kalau output melanggar policy atau syntax invalid.
 */
export function validateGeneratedContent(
  content: GeneratedExcelContent,
): SafetyResult {
  if (!content.description || content.description.length < 20) {
    return { ok: false, reason: "Description terlalu pendek — kemungkinan bukan tutorial valid." };
  }
  if (content.steps.length === 0) {
    return { ok: false, reason: "Langkah-langkah kosong — output AI tidak valid." };
  }
  // ingredients (= rumus terkait) boleh kosong untuk tutorial konseptual (mis. "Cara
  // mengaktifkan Developer Tab"), tapi minimal 1 rekomendasi kuat
  if (content.ingredients.length === 0 && content.steps.length < 3) {
    return { ok: false, reason: "Tutorial terlalu tipis — minimal 3 langkah atau 1 rumus terkait." };
  }

  const haystack = [
    content.description,
    ...content.steps.map((s) => s.instruction),
    ...content.ingredients.map((i) => `${i.name} ${i.quantity}`),
  ].join(" ");

  for (const pattern of REJECT_PATTERNS) {
    if (pattern.test(haystack)) {
      return {
        ok: false,
        reason: "Konten mengandung klaim atau syntax yang tidak diperbolehkan.",
      };
    }
  }

  return { ok: true };
}
