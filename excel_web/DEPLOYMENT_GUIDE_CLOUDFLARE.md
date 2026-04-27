# Panduan Deploy Tutorial Excel Indonesia ke Cloudflare Workers

Dokumen ini berisi langkah-langkah teknis untuk mendeploy backend `excel_web` ke infrastruktur Cloudflare (Workers, D1, dan KV).

## Prasyarat
1. Akun Cloudflare yang aktif.
2. Node.js (v18 ke atas) terinstall di lokal.
3. Wrangler CLI terinstall: `npm install -g wrangler`
4. Login ke Cloudflare via CLI: `wrangler login`

---

## 1. Persiapan Resource Cloudflare

### A. Buat Database D1
Jalankan perintah berikut untuk membuat database SQL (D1):
```bash
wrangler d1 create excel-db
```
**PENTING:** Salin `database_id` dari output perintah di atas, lalu tempelkan ke `wrangler.toml` pada bagian:
```toml
[[d1_databases]]
binding = "DB"
database_name = "excel-db"
database_id = "PASTE_ID_DI_SINI"
```

### B. Buat KV Namespace
Jalankan perintah berikut untuk membuat penyimpanan gambar (KV):
```bash
wrangler kv namespace create IMAGES
```
**PENTING:** Salin `id` dari output, lalu tempelkan ke `wrangler.toml` pada bagian:
```toml
[[kv_namespaces]]
binding = "IMAGES"
id = "PASTE_ID_DI_SINI"
```

---

## 2. Setup Secrets & Environment

Cloudflare memerlukan beberapa secret yang bersifat sensitif dan tidak boleh dicatat di `wrangler.toml`. Masukkan secret berikut via dashboard Cloudflare atau command line:

```bash
# Password untuk masuk ke dashboard admin (/admin)
wrangler secret put ADMIN_PASSWORD

# String random untuk enkripsi session
wrangler secret put SESSION_SECRET

# JSON berisi data pool akun Cloudflare AI (Array of Objects)
# Format: [{"id": 1, "account_id": "...", "api_key": "..."}]
wrangler secret put ACCOUNT_POOL_JSON
```

---

## 3. Inisialisasi Database (Remote)

Gunakan perintah npm script yang sudah tersedia di `package.json` untuk membuat tabel dan memasukkan data tutorial awal (seed) ke database production:

```bash
npm run db:init:remote
```
Perintah ini mengeksekusi `schema.sql` dan `seeds/seed.sql` secara remote.

---

## 4. Deploy Aplikasi

Jalankan perintah deploy:
```bash
npm run deploy
```

Setelah berhasil, aplikasi Anda akan live di sub-domain `*.workers.dev`.

---

## 5. Konfigurasi Domain Kustom (Opsional)

Jika Anda ingin menggunakan domain sendiri (misalnya `excel.plasodig.my.id`):
1. Masuk ke Dashboard Cloudflare Web.
2. Navigasi ke **Workers & Pages** > **excel-dashboard** > **Settings** > **Triggers**.
3. Di bagian **Custom Domains**, klik **Add Custom Domain**.
4. Masukkan domain Anda dan biarkan Cloudflare mengatur DNS & SSL.

---

## 6. Verifikasi & Pasca-Deploy

1. **Akses Dashboard Admin**: Buka `https://your-app.workers.dev/admin` dan masukkan `ADMIN_PASSWORD`.
2. **Sinkronisasi Data**: Klik menu Sync atau gunakan endpoint `/admin/sync` untuk menarik data draft terbaru dari Google Sheets.
3. **Trigger AI Generation**: Tutorial awal (seed) berstatus `draft`. Anda perlu masuk ke menu detail di dashboard admin dan klik **Generate All** untuk menghasilkan teks tutorial dan gambar via AI.
4. **Cek API**: Buka `https://your-app.workers.dev/api/excels` untuk memastikan data JSON sudah tersedia bagi aplikasi mobile.

---

## Tips Maintenance
- **Rate Limit**: Jika kuota AI habis, perbarui data `ACCOUNT_POOL_JSON` dengan akun baru.
- **Log**: Gunakan `wrangler tail` untuk memantau error secara real-time di production.
- **Cron Job**: Worker ini dikonfigurasi untuk berjalan setiap jam (`0 * * * *`) guna memproses tutorial draft secara otomatis melalui fungsi `autoProcessDrafts`.
