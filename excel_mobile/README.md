# Tutorial Excel Indonesia — KMP Mobile App

> ⚠️ **Konteks untuk AI assistant:** Project ini adalah **app Tutorial Excel** untuk masyarakat Indonesia (admin/staff/UMKM/mahasiswa). **BUKAN** app resep masakan. Codebase di-clone dari project `resep_mobile` (di `D:\belajar\kotlin\resep\resep_mobile`) lalu di-pivot domain pada 2026-04-25. Beberapa nama field & SQL table mungkin masih warisan resep (`relatedFunctions`, `tutorial_steps`, `cookingTimeMinutes`) — itu **sengaja di-repurpose** supaya tidak perlu rombak schema dari nol. **Jangan rekomendasikan konten masakan / cooking apapun.**

App **Tutorial Excel + Studi Kasus Indonesia** berbasis **Kotlin Multiplatform** (Android + iOS) dengan arsitektur multi-module, **offline-first**, dan **Compose Multiplatform**.

**Model konten:** seluruh tutorial (teks + gambar) **sudah di-pre-generate** di sisi `excel_web` (Cloudflare Worker admin di folder tetangga). Mobile app murni berperan sebagai **konsumen API publik** — tidak ada panggilan AI, tidak ada credential, tidak ada generate on-device. Cache lokal tahan selamanya.

```
┌──────────────┐    GET /api/excels                ┌──────────────────────────┐
│ Android/iOS  │ ────────────────────────────────▶ │  excel_web (dashboard)   │
│ device       │    GET /api/excels/:id            │  Cloudflare Worker       │
│              │ ◀──────────────────────────────── │  D1 + KV (pre-generated) │
└──────┬───────┘    JSON manifest / detail         └──────────────────────────┘
       │
       ▼
SQLDelight (teks) + Coil disk cache (gambar)
```

**Base URL:** `https://excel.plasodig.my.id` (custom domain Cloudflare — perlu setup terpisah).
Override via `local.properties` → `EXCEL_API_BASE=https://...` (tanpa trailing slash).

---

## Domain Model — Repurposed Schema

Karena codebase warisan dari resep, beberapa field name di-repurpose. **Jangan rebrand SQL table tanpa migrate** — kalau memang mau, butuh effort cascade besar.

| Field/Class | Asal (resep) | Sekarang (excel) — interpretasi |
|---|---|---|
| `Excel` (data class) | `Recipe` | Satu item tutorial Excel |
| `ExcelCategory` enum | masakan kategori | BasicFormula / Function / Lookup / PivotTable / Chart / Macro / Format / Database |
| `cookingTimeMinutes` | menit memasak | **Estimasi menit untuk paham tutorial** (5-60) |
| `servings` | porsi | **Versi Excel minimal** (2010, 2013, 2016, 2019, 2021, 365) |
| `relatedFunctions: List<RelatedFunction>` | bahan masakan | **Rumus/fungsi Excel terkait** (mis. `[VLOOKUP, IFERROR]`) |
| `RelatedFunction.name/quantity/unit` | nama/jumlah/satuan bahan | **nama fungsi / contoh syntax / kategori (Lookup, Math, Logical, dll)** |
| `steps: List<TutorialStep>` | langkah memasak | **Langkah-langkah tutorial step-by-step** |
| `TutorialStep.instruction` | instruksi memasak | Instruksi tutorial |
| `tags` | tag masakan | Topik/keyword tutorial Excel |

---

## Prasyarat

| Tool | Versi | Keterangan |
| --- | --- | --- |
| **JDK** | 17+ | Terpasang bersama Android Studio atau OpenJDK |
| **Android Studio** | Ladybug+ | Include Android SDK + Gradle |
| **Android SDK** | API 34–35 | SDK Manager → install |
| **Plugin KMP** | Latest | Settings → Plugins → "Kotlin Multiplatform" |

> iOS build butuh macOS + Xcode. Lihat `iosApp/README.md`.

---

## Setup

Set base URL API di `local.properties` (root project):

```properties
EXCEL_API_BASE=https://excel.plasodig.my.id
```

Kalau kosong, app fallback ke default `https://excel.plasodig.my.id` (placeholder; perlu setup dashboard dulu di `../excel_web`).

Build & install Android:

```bash
./gradlew.bat :composeApp:installDebug
```

Atau jalankan dari Android Studio → Run ▶.

---

## Cara kerja (offline-first + SWR)

1. **Startup** — `refresh()` dipanggil background untuk fetch manifest. Hasil di-upsert ke `ExcelEntity`, **tanpa menimpa** relatedFunctions/steps yang sudah pernah di-fetch.

2. **List / Search / Favorites** — UI subscribe ke Flow dari SQLDelight. Tidak pernah blocking network; cache adalah sumber kebenaran.

3. **Detail tutorial** — saat dibuka:
   - DB emit summary dulu → banner "Memuat detail tutorial…" tampil
   - `EnsureExcelDetailUseCase(id)` cek: kalau rumus/fungsi terkait kosong di DB → call `GET /api/excels/:id` lalu upsert detail
   - DB observer emit isi baru → UI render Rumus & Fungsi Terkait + Langkah-Langkah
   - Call berikutnya untuk tutorial yang sama = no-op (sudah lengkap)

4. **Gambar** — URL dari manifest (`imageUrl`) di-resolve jadi URL absolut oleh `ApiConfig.resolveImageUrl()`, lalu di-load via Coil dengan **memory cache + disk cache** (50 MB). Gambar tahan offline setelah dilihat sekali.

5. **Demand-driven generation** — di tab Cari, kalau hasil lokal kosong:
   - Server kasih saran tutorial mirip (zero-LLM SQL ranking, gratis & instan)
   - User tap "Buat tutorial ini" → submit ke `POST /api/requests` (rate limit 5/jam/IP)
   - Mobile polling status tiap 3 detik sampai completed
   - Kalau kena rate limit (429) → dialog escape hatch tonton rewarded ad → submit via `/api/requests/extra` (10/jam/IP bucket terpisah)

6. **Error handling** — network gagal → banner merah "Gagal memuat" + tombol "Coba lagi". Data lama dari cache tetap tampil.

---

## Struktur project

```
excel_mobile/
├── composeApp/                 entry Android + iOS (Compose MP host)
│   ├── src/androidMain/        MainActivity.kt + ExcelApplication.kt (Koin init)
│   ├── src/commonMain/         App.kt, RootComponent, AppModule, ImageLoader setup
│   └── src/iosMain/            MainViewController.kt (dipanggil dari Swift)
├── iosApp/                     wrapper Swift untuk iOS
├── core/
│   ├── common/                 AppResult<T>, AppError, DispatcherProvider
│   ├── domain/                 model (Excel, ExcelCategory, dll) + repository interface + use cases
│   ├── data/
│   │   ├── config/             ApiConfig (resolve base URL + imageUrl)
│   │   ├── remote/             RemoteExcelApi (Ktor client ke excel_web)
│   │   ├── mapper/             DTO ↔ domain model mapping
│   │   ├── repository/         ExcelRepositoryImpl — offline-first + SWR
│   │   └── di/                 Koin DataModule + UseCaseModule
│   ├── database/               SQLDelight schema (Excel, RelatedFunction, TutorialStep, Favorite)
│   ├── designsystem/           Material3 theme + reusable components
│   └── ads/                    AdMob + UMP consent + rewarded ad
├── feature/
│   ├── excel-list/             daftar tutorial + filter kategori
│   ├── excel-detail/           detail + fetch lifecycle + report dialog
│   ├── search/                 pencarian + demand-driven generation
│   └── favorites/              tutorial favorit (local-only)
├── build-logic/                convention plugins Gradle
└── gradle/libs.versions.toml   version catalog
```

---

## Fitur app

- **Daftar tutorial Excel** dengan filter kategori (Rumus Dasar, Fungsi, Lookup, PivotTable, Chart, Makro, Format, Database)
- **Detail tutorial** dengan rumus/fungsi terkait + langkah-langkah + checkbox per langkah
- **Pencarian** debounced 250 ms (SQLite LIKE)
- **Demand-driven generation** — minta tutorial baru kalau belum ada (rate-limited + rewarded ad escape)
- **Favorit** lokal di SQLDelight
- **Offline-first** — semua UI baca dari cache, network hanya untuk fill gap
- **Image caching** — Coil memory + disk cache, gambar tahan offline setelah dilihat
- **Dark mode** otomatis mengikuti system
- **Reporting UGC** — required Google Play Generative AI policy (tap Flag icon di TopBar)
- **Full Bahasa Indonesia**

---

## Tech stack

| Area | Library |
| --- | --- |
| Bahasa | Kotlin 2.1 |
| UI | Compose Multiplatform 1.7 |
| Navigation | Decompose 3.2 |
| DI | Koin 4 |
| Database | SQLDelight 2 (reactive Flow queries) |
| Networking | Ktor Client 3 (OkHttp Android, Darwin iOS) |
| Image | Coil 3 (custom ImageLoader: memory 32 MB + disk 50 MB) |
| Ads | Google Mobile Ads 23.6 + UMP 3.1 |
| Logging | Kermit |
| Build | Gradle 8.10 + Version Catalog + Convention Plugins |

Arsitektur mobile: **Multi-module + MVI (Decompose) + offline-first + SWR**.

---

## Yang HARUS Anda lakukan sebelum publish

1. **Ganti AdMob ID test → production** di `core/ads/src/commonMain/.../AdUnitIds.kt` + `composeApp/src/androidMain/AndroidManifest.xml`. Sekarang masih test ID Google.
2. **Ganti app icon** di `composeApp/src/androidMain/res/drawable/ic_launcher.xml` dan `iconexcel.jpeg` (masih warisan visual project resep).
3. **Setup signing config production** di `composeApp/build.gradle.kts` — sekarang release pakai debug signing (release build tidak bisa diupload ke Play Store).
4. **Setup `excel_web` dashboard** di `../excel_web/` dulu (D1 + KV + custom domain), lalu update `local.properties` → `EXCEL_API_BASE=https://...`.

---

## Troubleshooting

**Daftar tutorial kosong**
Cek `local.properties` → `EXCEL_API_BASE`. Cek log Logcat tag `ExcelRepo` atau `RemoteExcelApi` untuk status HTTP.

**Detail stuck di "Memuat detail tutorial…"**
Tunggu ~5 detik. Kalau belum tampil:
- Cek koneksi internet
- Verify `GET <EXCEL_API_BASE>/api/excels/<id>` returns 200 di browser
- Cek log Logcat tag `ExcelRepo`

**Gambar tidak muncul**
URL di manifest bisa relative (`/api/images/excels/xxx.jpg`). `ApiConfig.resolveImageUrl()` auto-resolve. Coil disk cache di `<cacheDir>/excel_images`. Clear app data untuk reset cache.

---

## Ringkasan zero-to-running

```bash
# 1. Set base URL API di local.properties
#    EXCEL_API_BASE=https://your-worker.workers.dev

# 2. Build & install Android
./gradlew.bat :composeApp:installDebug
```

Tap tutorial mana saja → detail fetched dari dashboard → cached selamanya di device.

---

## Lihat juga

- `PROJECT_CONTEXT.md` — roadmap, decision log, status migrasi pivot dari resep
- `../excel_web/README.md` — dokumentasi backend dashboard (Cloudflare Worker)
