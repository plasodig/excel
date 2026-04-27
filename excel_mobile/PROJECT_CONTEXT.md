# PROJECT_CONTEXT — Tutorial Excel Indonesia

> 📍 **Tujuan dokumen:** Self-documenting context supaya AI assistant atau developer baru langsung tahu **apa proyek ini, kenapa di-pivot, status migrasi, dan apa yang BELUM diberesin**. Baca ini DULU sebelum melakukan refactor besar.

---

## Identitas Proyek

| Atribut | Nilai |
|---|---|
| Nama | Tutorial Excel Indonesia (codename: `excel`) |
| Domain | Tutorial Microsoft Excel + studi kasus Indonesia |
| Target user | Admin/staff kantor, UMKM, mahasiswa, pelamar kerja Indonesia |
| Platform | Android (primary) + iOS (secondary) via Kotlin Multiplatform |
| Backend | `../excel_web` — Cloudflare Worker (Hono + D1 + KV) |
| Status | **Pivot fresh dari project resep — pra-launch, belum publish** |

---

## Sejarah Pivot

**Asal:** Project ini di-clone pada **2026-04-25** dari sister project `D:\belajar\kotlin\resep\` yang awalnya untuk **resep masakan Nusantara**.

**Alasan pivot:**

1. **App Soal CPNS sebelumnya kena suspend Google Play.** User tidak ingin membuat lagi yang masuk kategori "government certification test prep" (UTBK, BUMN, kedinasan) karena risk profile policy sama.
2. **Codebase resep tidak terbukti traction.** Daripada bangun 5 app paralel yang dangkal, lebih baik clone ke 1 domain baru dengan probabilitas market dapat cukup tinggi.
3. **Tutorial Excel + studi kasus Indonesia** dipilih karena:
   - Pasar konsisten sepanjang tahun (admin/staff/UMKM/mahasiswa)
   - Saingan di Play Store mostly tutorial bahasa Inggris yang dipindah, **bukan dibuat ulang dengan kasus Indonesia**
   - AI sangat capable generate tutorial Excel + contoh syntax
   - USP offline-first cocok untuk daerah dengan internet tidak stabil
   - Risiko policy mendekati nol (tidak terkait kesehatan/keuangan/pemerintah)

**Project resep** (`D:\belajar\kotlin\resep\`) **TIDAK dimatikan** — masih aktif sebagai project paralel yang punya backend Cloudflare aktif (`resep.plasodig.my.id`).

---

## Decision Log (keputusan final, jangan dinegosiasi ulang tanpa konteks baru)

| # | Keputusan | Alasan |
|---|---|---|
| D1 | **Clone full codebase**, tidak mulai dari scratch | Hemat waktu, struktur Kotlin Multiplatform + dashboard worker + cron generator semua sudah jadi |
| D2 | **Repurpose schema, jangan rombak** | Field `cookingTimeMinutes` jadi estimasi menit, `servings` jadi versi Excel, `relatedFunctions` jadi rumus terkait, `tutorial_steps` jadi langkah tutorial. Cascade rebrand SQL+DAO+mapper terlalu mahal untuk benefit semantik kecil |
| D3 | **Domain class `Excel`, `ExcelCategory`, `ExcelRepository` dll** | Replace literal Recipe → Excel di semua identifier supaya code reads correctly untuk Excel domain |
| D4 | **Enum ExcelCategory: 8 topik utama Excel** | BasicFormula, Function, Lookup, PivotTable, Chart, Macro, Format, Database — covering ~80% kebutuhan tutorial |
| D5 | **AI prompt total rewrite** untuk Excel | `excel_web/src/ai/prompts.ts` strict tutor Excel Indonesia, JSON output schema tetap ekuivalen |
| D6 | **Wrangler D1/KV ID stubbed** `TODO_GENERATE_NEW_*` | Mencegah deploy nabrak DB project resep production — wajib generate baru |
| D7 | **`AI_POOL_URL` boleh shared** dengan project resep | Multi-tenant cara hemat akun — pool akun Cloudflare Workers AI digunakan kedua project |
| D8 | **`TUTORIALS_SYNC_URL` HARUS Apps Script baru** | Konten Excel berbeda total dari resep, butuh sumber data terpisah |
| D9 | **Custom domain target: `excel.plasodig.my.id`** | Setup DNS Cloudflare belum dilakukan — bisa pakai default `*.workers.dev` dulu |
| D10 | **35 tutorial seed awal di-define** di `excel_web/seeds/seed.sql` | Cover 8 kategori, status='draft' menunggu generate |

---

## Status Migrasi (per 2026-04-25)

### ✅ Selesai

- [x] Clone resep_mobile → excel_mobile (141 file)
- [x] Clone resep_web → excel_web (39 file)
- [x] Folder rename `com\plasodig\resep` → `com\plasodig\excel` (semua source set)
- [x] Folder rename `feature/recipe-list` → `feature/excel-list`, `feature/recipe-detail` → `feature/excel-detail`
- [x] Mass replace `Resep`/`resep` → `Excel`/`excel` (branding + identifier)
- [x] Mass replace `Recipe`/`recipe` → `Excel`/`excel` (domain class + variable + API path)
- [x] Rename 23 file `Recipe*.kt` → `Excel*.kt`
- [x] Rebrand enum `ExcelCategory` ke 8 topik Excel
- [x] Update default `ExcelCategory.MainCourse` → `BasicFormula` di mapper & repo
- [x] Update UI label detail screen ("Bahan-bahan" → "Rumus & Fungsi Terkait", dll)
- [x] Web: rewrite `prompts.ts` total untuk konten Excel (system + image prompt)
- [x] Web: rewrite `contentSafety.ts` (blokir non-Excel topic + saran finansial/medis)
- [x] Web: rename `validateFoodQuery` → `validateExcelQuery` + update 2 caller
- [x] Web: replace seeds (35 tutorial Excel di `seed.sql` + `excels.json`)
- [x] Web: stub `database_id`, `kv_namespaces.id`, `TUTORIALS_SYNC_URL` di `wrangler.toml`
- [x] Verifikasi grep: 0 referensi `recipe`/`Recipe`/`RECIPE`/`resep`/`Resep`/`RESEP`
- [x] README mobile + web di-rewrite dengan konteks Excel
- [x] Dokumentasi PROJECT_CONTEXT (file ini)
- [x] Rename data class `Ingredient` → `RelatedFunction` + cascade SQL (Ingredient.sq → RelatedFunction.sq), DAO method, mapper, DTO, UI. Domain class 100% Excel-clean.
- [x] Rename data class `CookingStep` → `TutorialStep` + cascade SQL (CookingStep.sq → TutorialStep.sq), DAO method, mapper, DTO. Domain class 100% Excel-clean.
- [x] Verifikasi grep akhir: 0 referensi `Recipe`/`Ingredient`/`CookingStep` di code (hanya di docs sebagai historical context yang intentional).

- [x] **Build verification** — `./gradlew :composeApp:assembleDebug` SUKSES (2026-04-25). APK debug 24 MB ter-generate di `composeApp/build/outputs/apk/debug/composeApp-debug.apk`. Hanya ada 2 warning expect/actual class beta (KT-61573) — non-blocking.

### 🟡 In progress / direkomendasi tapi belum dieksekusi
- [ ] **Pertimbangkan ganti field `cookingTimeMinutes` → `estimatedMinutes`** di domain & SQL kalau mau 100% bersih. Effort: medium (~5 file). Risk: SQLDelight generate ulang Entity field.
- [ ] **Pertimbangkan ganti field `servings` → `excelVersion`** di domain & SQL. Effort sama.

### ❌ Belum direncanakan / pending decision

- [ ] **App icon** — `iconexcel.jpeg` masih ikon visual project resep. Belum ada brand asset Excel.
- [ ] **`ic_launcher.xml`** drawable Android — masih ikon resep.
- [ ] **Setup Cloudflare resource baru** — D1 (`excel-db`), KV (IMAGES), custom domain. User belum eksekusi.
- [ ] **Apps Script `TUTORIALS_SYNC_URL`** — belum dibuat. Sementara stub.
- [ ] **AdMob unit ID production** — masih test ID. Wajib ganti sebelum publish.
- [ ] **Signing config production** — release build masih pakai debug signing.
- [ ] **Privacy policy + Terms of Service URL** — wajib untuk Play Store listing.
- [ ] **Marketing assets** — screenshots, feature graphic, short description. Belum ada.
- [ ] **CI/CD** — GitHub Actions Android build, Wrangler auto-deploy.

---

## Field Repurposing Cheat Sheet

Karena schema warisan dari resep, beberapa field name terkesan aneh untuk app Excel. Ini bukan bug — sengaja dipertahankan untuk hindari cascade rename. **Kalau Anda AI yang baca code:**

| Lihat ini di code | Anggap ini sebagai | Contoh isi untuk app Excel |
|---|---|---|
| `Excel.cookingTimeMinutes` | Estimasi menit untuk paham tutorial | 15 (= 15 menit baca + praktek) |
| `Excel.servings` | Versi Excel minimal yang dibutuhkan | 365 (Microsoft 365), 2021, 2019, 2016, 2013, 2010 |
| `Excel.relatedFunctions: List<RelatedFunction>` | List rumus/fungsi Excel terkait | `[{name:"VLOOKUP", quantity:"=VLOOKUP(A2,Master!A:D,2,FALSE)", unit:"Lookup"}, ...]` |
| `RelatedFunction.name` | Nama fungsi Excel | "VLOOKUP", "IFERROR", "SUMIFS" |
| `RelatedFunction.quantity` | Contoh syntax fungsi | "=VLOOKUP(lookup_value, table, col, FALSE)" |
| `RelatedFunction.unit` | Kategori fungsi | "Lookup", "Math", "Logical", "Text", "Date", "Reference", "Statistical", "Database" |
| `Excel.steps: List<TutorialStep>` | List langkah tutorial | (urutan langkah-langkah) |
| `TutorialStep.instruction` | Instruksi tutorial 1 langkah | "Di sheet 'Rekap', ketik =VLOOKUP(A2, Master!A:D, 2, FALSE)" |
| `TutorialStep.durationMinutes` | (jarang dipakai) — biarkan null | null |
| `Excel.tags` | Topik/keyword tutorial | `["vlookup", "hr", "gaji", "karyawan"]` |
| `excels` table di SQL | Daftar tutorial Excel | - |
| `relatedFunctions` table di SQL | Rumus/fungsi terkait per tutorial | - |
| `tutorial_steps` table di SQL | Langkah tutorial per tutorial | - |
| `excel_reports` table | Laporan UGC user | - |

---

## Cara Test Cepat (kalau ingin verifikasi semuanya jalan)

### Mobile
```bash
cd D:\belajar\kotlin\excel\excel_mobile
./gradlew.bat :composeApp:assembleDebug    # akan compile semua module
./gradlew.bat :composeApp:installDebug     # install ke emulator/device
```

### Web
```bash
cd D:\belajar\kotlin\excel\excel_web
rm package-lock.json && npm install        # fresh install
npx wrangler dev                            # local dev di http://localhost:8787
# Test endpoint:
curl http://localhost:8787/api/excels
```

---

## Roadmap Singkat

### Tahap 1: Foundation (sekarang)
- ✅ Codebase clone + rename + repurpose
- ✅ Dokumentasi self-explanatory
- ⏳ Build verifikasi & fix error compile

### Tahap 2: Backend Bootstrap
- Setup Cloudflare resource (D1, KV, custom domain)
- Generate 35 tutorial Excel awal via dashboard
- Test end-to-end flow demand-driven generation

### Tahap 3: Branding & UX Polish
- App icon + splash screen Excel-themed
- Marketing copy untuk Play Store
- Setup signing config production
- Test rewarded ad flow

### Tahap 4: Pre-launch
- Privacy policy + TOS URL
- Internal testing (10-20 user)
- Iterate prompt + content quality
- AdMob production ID

### Tahap 5: Launch
- Play Store submission
- Monitor crash + retention
- Iterate berdasarkan feedback

---

## Untuk AI Assistant yang Lanjut Mengerjakan

**Hindari rekomendasi:**
- Konten masakan, kuliner, resep apapun (project sudah pivot dari resep)
- Bank Soal CPNS / UTBK / BUMN / kedinasan (sebelumnya kena suspend)
- Konten kesehatan / saran finansial / hukum spesifik (regulasi ketat)
- Domain class/field dengan nama Indonesia food (Sambal, MainCourse, Pembuka, dll) — sudah dihapus

**Boleh & disarankan:**
- Konten tutorial Microsoft Excel
- Studi kasus Indonesia (rekap absensi UMR, omzet warung, gaji buruh, dll)
- Fungsi/rumus Excel resmi (VLOOKUP, INDEX, MATCH, IFS, FILTER, dll)
- Tips produktivitas spreadsheet
- Comparison Excel vs Google Sheets (boleh disebut sebagai pembanding)

**Kalau user minta sesuatu, baca dulu:**
1. README.md mobile (`D:\belajar\kotlin\excel\excel_mobile\README.md`)
2. README.md web (`D:\belajar\kotlin\excel\excel_web\README.md`)
3. Dokumen ini (PROJECT_CONTEXT.md)

Sebelum suggest perubahan domain class atau schema SQL, **konfirmasi dulu dengan user** karena cost cascade rename tinggi.
