# Excel Dashboard — Cloudflare Worker

> 📂 **Panduan Deployment:** Lihat [DEPLOYMENT_GUIDE_CLOUDFLARE.md](./DEPLOYMENT_GUIDE_CLOUDFLARE.md) untuk langkah-langkah setup production.

> ⚠️ **Konteks untuk AI assistant:** Project ini adalah **dashboard admin untuk app Tutorial Excel Indonesia**. Seluruh struktur database (`excels`, `ingredients`, `cooking_steps`) digunakan untuk menyimpan data tutorial Excel: `ingredients` untuk rumus terkait, dan `cooking_steps` untuk langkah-langkah tutorial. **Jangan rekomendasikan konten di luar topik Microsoft Excel.**

Dashboard admin untuk **pre-generate tutorial Excel + gambar** via Cloudflare Workers AI. Mobile app (`../excel_mobile`) hanya konsumsi data hasil generate dari endpoint publik — **hemat token AI** karena generate cuma sekali per tutorial, bukan per-device.

## Arsitektur

```
Admin (browser)
  ↓ login
[Cloudflare Worker (Hono)]
  ├─ /                    → list tutorial (HTML)
  ├─ /admin/excels/:id    → detail + tombol generate / publish (HTML)
  ├─ /api/excels          → JSON publik untuk mobile (status='published' saja)
  ├─ /api/excels/:id      → JSON detail untuk mobile
  ├─ /api/suggestions     → POST: saran tutorial mirip (zero-LLM SQL ranking)
  ├─ /api/requests        → POST: submit demand-driven generate (rate-limited)
  ├─ /api/requests/extra  → POST: submit setelah rewarded ad (bucket terpisah)
  ├─ /api/requests/:id    → GET: polling status generate
  ├─ /api/excels/:id/report → POST: laporan UGC (Google Play compliance)
  ├─ /api/images/excels/:filename → KV image proxy
  ↓
[D1 (SQLite)]
  ├─ excels (id, title, category, description, ..., status)
  ├─ ingredients (excel_id, name, quantity, unit, ...)   ← rumus terkait
  ├─ cooking_steps (excel_id, step_order, instruction)   ← langkah tutorial
  ├─ generation_requests (id, query, slug_target, status, requested_at, ...)
  └─ excel_reports (id, excel_id, reason, detail, created_at)

[KV (key-value)]
  ├─ excels/<slug>.png    ← gambar tutorial (binary)
  ├─ ai_pool_cache        ← cache pool akun Cloudflare AI (1h TTL)
  └─ rate_limit:<bucket>:<ip>  ← rate limit counters (1h TTL)

[Cloudflare Workers AI (rotating account pool)]
  ├─ @cf/meta/llama-3.1-8b-instruct        ← teks tutorial
  └─ @cf/stabilityai/stable-diffusion-xl   ← gambar tutorial
```

## Struktur Data Utama
 
Berikut adalah cara pemetaan data tutorial Excel ke dalam kolom database:
 
| SQL field/table | Fungsi di App Excel | Contoh Isi / Interpretasi |
|---|---|---|
| `excels` table | Daftar tutorial Excel | - |
| `excels.cooking_time_minutes` | **Estimasi Pemahaman** | 15 (artinya estimasi 15 menit belajar) |
| `excels.servings` | **Versi Excel Minimal** | 2013, 2019, 365, dll |
| `ingredients` table | **Rumus/Fungsi Terkait** | Kumpulan fungsi yang digunakan (mis. VLOOKUP, IFERROR) |
| `ingredients.name/quantity/unit` | Nama / Syntax / Kategori | `VLOOKUP` / `=VLOOKUP(...)` / `Lookup` |
| `cooking_steps` table | **Halaman/Langkah Tutorial** | Urutan instruksi tutorial |
| `cooking_steps.instruction` | Isi Langkah | "Klik sel A1 lalu ketik rumus..." |
| `excels.tags` | Keyword | "gaji", "rekap", "vlookup" |

---

## Stack

| Komponen | Versi | Catatan |
|---|---|---|
| **Hono** | 4.6 | Framework Web ringan untuk Workers |
| **TypeScript** | 5.7 | Strict mode |
| **Wrangler CLI** | 4.84 | Deploy tool Cloudflare |
| **D1** | runtime | SQLite serverless |
| **KV** | runtime | Image storage (sengaja **tanpa R2** untuk hemat) |
| **Workers AI** | runtime | Llama 3.1 (text) + SDXL (image) via 64-account rotation |
| **Cron** | runtime | `0 * * * *` — auto-process 1 draft per jam |

---

## Setup Awal (dari nol)

### 1. Install dependencies

```bash
cd excel_web
npm install
```

### 2. Buat resource Cloudflare baru

Project `wrangler.toml` sudah di-stub `database_id` & `kv_namespaces.id` jadi `TODO_*`. **Pastikan membuat rsc baru di Cloudflare** agar data tidak tercampur dengan proyek lain.

```bash
# D1 database
npx wrangler d1 create excel-db
# Copy "database_id" dari output ke wrangler.toml line 28

# KV namespace untuk image
npx wrangler kv namespace create IMAGES
# Copy "id" dari output ke wrangler.toml line 32
```

### 3. Setup Apps Script untuk content sync

Buat Google Apps Script baru (deploy as web app) yang return JSON list tutorial Excel awal. Format:

```json
[
  { "id": "vlookup-dasar", "title": "VLOOKUP Dasar untuk Pemula", "category": "Lookup" },
  ...
]
```

Set URL-nya ke `TUTORIALS_SYNC_URL` di `wrangler.toml` line 38.

`AI_POOL_URL` menggunakan pool akun Cloudflare AI yang dapat digunakan bersama untuk optimasi kuota.

### 4. Apply schema D1

```bash
npx wrangler d1 execute excel-db --file=schema.sql --remote
npx wrangler d1 execute excel-db --file=migrations/002_requests.sql --remote
npx wrangler d1 execute excel-db --file=migrations/003_auto_generate.sql --remote
npx wrangler d1 execute excel-db --file=migrations/004_reports.sql --remote
```

### 5. Seed 35 tutorial awal (status='draft')

```bash
npx wrangler d1 execute excel-db --file=seeds/seed.sql --remote
```

### 6. Setup `.dev.vars` untuk local dev

Copy `.dev.vars.example` → `.dev.vars`, isi `ADMIN_EMAIL` dan rahasia lain.

### 7. (Opsional) Custom domain

Kalau punya custom domain di Cloudflare, ubah `wrangler.toml` line 11 `pattern = "excel.plasodig.my.id"` ke domain Anda. Atau hapus block `[[routes]]` untuk pakai default `*.workers.dev`.

### 8. Deploy

```bash
npx wrangler deploy
```

---

## Alur Kerja Admin

1. **Sync content awal** dari Apps Script → klik "Sync dari GAS" di admin dashboard → 35 draft tutorial muncul
2. **Edit metadata** per tutorial (title, category, description) di halaman detail
3. **Generate** (tombol "Generate All") → background:
   - AI generate teks (Llama 3.1) + gambar (SDXL) paralel
   - Validasi output via `contentSafety.ts`
   - Upload gambar ke KV
   - Set status='published' kalau OK
4. **Auto-publish** via cron tiap 1 jam (1 draft per cycle, hemat AI quota)

---

## Alur Demand-Driven Generation (dari mobile)

1. User buka tab Cari di mobile, ketik query "VLOOKUP gabung" yang belum ada di katalog
2. Mobile call `POST /api/requests` dengan query
3. Server:
   - Validasi query via `validateExcelQuery()` (blokir non-Excel topic)
   - Cek rate limit per IP (5/jam normal, 10/jam extra dengan rewarded ad)
   - Insert ke `generation_requests` table dengan status='processing'
   - `ctx.waitUntil(processGenerationRequest(...))` background
4. Mobile polling `GET /api/requests/:id` tiap 3 detik
5. Background task:
   - Validate query → fetch AI pool → generate text + image paralel → validate output → upload ke KV → save ke DB → mark request completed
6. Mobile dapat `excelId` (id tutorial baru) → buka detail tutorial di aplikasi.

---

## Stack Routes

| Route | Method | Purpose | Auth |
|---|---|---|---|
| `/login` | GET/POST | Login admin (email-only, simple) | - |
| `/logout` | POST | Logout | - |
| `/admin/` | GET | Dashboard list tutorial | ✓ |
| `/admin/excels/:id` | GET | Edit detail | ✓ |
| `/admin/excels/:id/update` | POST | Save metadata | ✓ |
| `/admin/excels/:id/generate-text` | POST | Manual text gen | ✓ |
| `/admin/excels/:id/generate-image` | POST | Manual image gen | ✓ |
| `/admin/excels/:id/generate-all` | POST | Both paralel + auto-publish | ✓ |
| `/admin/excels/:id/publish` | POST | Set status='published' | ✓ |
| `/admin/excels/:id/unpublish` | POST | Set status='generated' | ✓ |
| `/admin/sync` | POST | Bulk sync dari Apps Script | ✓ |
| `/admin/requests` | GET | Monitor demand-driven jobs | ✓ |
| `/admin/reports` | GET | Lihat laporan UGC | ✓ |
| `/admin/autobot/run` | POST | Force trigger cron sekarang | ✓ |
| `/api/excels` | GET | List published (mobile) | - |
| `/api/excels/:id` | GET | Detail (mobile) | - |
| `/api/suggestions` | POST | Saran zero-LLM (mobile) | - |
| `/api/requests` | POST | Submit generate (mobile) | rate-limited |
| `/api/requests/extra` | POST | Submit setelah rewarded ad | rate-limited |
| `/api/requests/:id` | GET | Polling status (mobile) | - |
| `/api/excels/:id/report` | POST | Laporkan content (mobile) | - |
| `/api/images/excels/:file` | GET | KV image proxy (mobile) | - |

---

## Struktur Project

```
excel_web/
├── wrangler.toml                       Cloudflare Worker config
├── package.json                        Hono, TypeScript, Wrangler
├── schema.sql                          D1 schema awal
├── migrations/                         D1 migration sequential
│   ├── 002_requests.sql               Demand-driven request table
│   ├── 003_auto_generate.sql          Cron-related fields
│   └── 004_reports.sql                UGC report table
├── seeds/
│   ├── seed.sql                       INSERT 35 draft tutorial Excel
│   └── excels.json                    JSON version (untuk reference manual)
├── src/
│   ├── index.ts                       Hono app entry + routing + cron handler
│   ├── types.ts                       TypeScript bindings (Env, GeneratedExcelContent, dll)
│   ├── db.ts                          D1 query helpers
│   ├── ai/
│   │   ├── client.ts                  Workers AI call (text + image)
│   │   ├── pool.ts                    Account rotation (64 slot pool dari GAS)
│   │   └── prompts.ts                 System prompt + image prompt khusus tutorial Excel
│   ├── routes/
│   │   ├── public.ts                  /api/* + landing page
│   │   ├── admin.ts                   /admin/* (auth-protected)
│   │   └── auth.ts                    /login, /logout
│   ├── middleware/
│   │   ├── auth.ts                    Session cookie validation
│   │   └── rateLimit.ts               Per-bucket per-IP rate limit (KV)
│   ├── service/
│   │   ├── generator.ts               Demand-driven background generator
│   │   ├── autobot.ts                 Hourly cron processor
│   │   ├── contentSafety.ts           Pre/post validation (blokir non-Excel topic)
│   │   └── suggestions.ts             Zero-LLM SQL ranking
│   ├── storage/
│   │   ├── kv.ts                      Image upload ke KV
│   │   └── r2.ts                      Stub (tidak dipakai — KV-only untuk hemat)
│   └── views/
│       ├── landing.ts                 Halaman publik
│       ├── dashboard.ts               Admin list tutorial
│       ├── detail.ts                  Admin editor + gen buttons
│       ├── requests.ts                Admin monitor jobs
│       ├── reports.ts                 Admin lihat laporan
│       ├── login.ts                   Login form
│       ├── legal.ts                   Privacy / TOS
│       └── layout.ts                  HTML wrapper
└── tsconfig.json
```

---

## Yang HARUS Anda lakukan sebelum deploy production

1. **Generate D1 + KV ID baru** dan replace `TODO_*` di `wrangler.toml`
2. **Setup Apps Script** untuk `TUTORIALS_SYNC_URL`
3. **Pastikan custom domain** sudah disetup di Cloudflare DNS (atau hapus `[[routes]]`)
4. **Test demand-driven generation** end-to-end di local: `npm run dev` → trigger via curl
5. **Pastikan prompt safety** di `src/ai/prompts.ts` sesuai topik Anda — sekarang sudah strict Excel-only

---

## Lihat juga

- `../excel_mobile/README.md` — dokumentasi mobile app yang konsumsi dashboard ini
- `../excel_mobile/PROJECT_CONTEXT.md` — roadmap, status migrasi, decision log
