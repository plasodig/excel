import { html, raw } from "hono/html";

const CSS = `
  :root {
    --bg: #0f1117;
    --surface: #1a1d27;
    --surface2: #22263a;
    --border: #2a2e3f;
    --text: #e8e9ed;
    --text-muted: #8b8fa3;
    --primary: #f97316;
    --font: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: var(--font); background: var(--bg); color: var(--text); min-height: 100vh; line-height: 1.6; }
  a { color: var(--primary); text-decoration: none; }
  a:hover { text-decoration: underline; }
  .container { max-width: 800px; margin: 0 auto; padding: 40px 24px; }
  .nav { margin-bottom: 40px; }
  .nav a { display: inline-flex; align-items: center; gap: 6px; padding: 10px 20px; border-radius: 50px; background: var(--surface); border: 1px solid var(--border); font-size: 14px; font-weight: 600; color: var(--text-muted); transition: all .2s; }
  .nav a:hover { border-color: var(--primary); color: var(--text); text-decoration: none; }
  .content { background: var(--surface); padding: 40px; border-radius: 16px; border: 1px solid var(--border); }
  h1 { font-size: 2.5rem; font-weight: 800; margin-bottom: 24px; color: #fff; }
  h2 { font-size: 1.5rem; font-weight: 700; margin-top: 32px; margin-bottom: 16px; color: #fff; }
  p { margin-bottom: 16px; color: var(--text-muted); }
  ul { margin-bottom: 16px; padding-left: 24px; color: var(--text-muted); }
  li { margin-bottom: 8px; }
  footer { text-align: center; padding: 40px 24px; color: var(--text-muted); font-size: 14px; }
  
  @media (max-width: 600px) {
    .content { padding: 24px; }
    h1 { font-size: 2rem; }
  }
`;

export function privacyPolicyPage() {
  return html`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Privacy Policy - Tutorial Excel Indonesia</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <style>${raw(CSS)}</style>
</head>
<body>
  <div class="container">
    <nav class="nav">
      <a href="/">← Back to Home</a>
    </nav>
    <div class="content">
      <h1>Privacy Policy</h1>
      <p><strong>Effective Date:</strong> ${new Date().toISOString().split("T")[0]}</p>
      
      <p>This Privacy Policy applies to the "Tutorial Excel Indonesia" Android application (Package Name: <code>com.plasodig.excel</code>) and its associated website <code>excel.plasodig.my.id</code> (collectively referred to as the "Service"), operated by Plasodig ("we", "us", or "our").</p>
      
      <h2>1. Information We Collect</h2>
      <p>We may collect information to provide and improve our Service. This includes:</p>
      <ul>
        <li><strong>Usage Data:</strong> We may collect non-personally identifiable information automatically, such as device type, operating system version, app usage statistics, and IP addresses for analytical and rate-limiting purposes.</li>
        <li><strong>Advertising ID:</strong> For our Android application, we use Google AdMob to display advertisements. AdMob may collect and use your device's unique advertising identifier (Advertising ID) and other related information to provide personalized ads.</li>
        <li><strong>Search Queries &amp; Tutorial Requests:</strong> When you use the search or "buat tutorial" features, the content of your search terms is stored permanently to build the Excel tutorial catalog. These queries are visible to our admin team for moderation.</li>
        <li><strong>User Reports:</strong> When you flag a tutorial via the in-app "Report" button, we store the reason, any free-text detail you submit, and your IP address for moderation purposes.</li>
      </ul>

      <h2>2. AI-Generated Content</h2>
      <p><strong>Penting:</strong> Seluruh tutorial yang ditampilkan — termasuk rumus, langkah-langkah, dan ilustrasi — di-generate oleh kecerdasan buatan (Cloudflare Workers AI: Meta Llama untuk teks, Black Forest Labs FLUX untuk gambar). Kami tidak menjamin akurasi, kelengkapan, keamanan, atau keaslian konten tersebut.</p>
      <ul>
        <li>Selalu verifikasi rumus dan langkah-langkah di Microsoft Excel resmi sebelum diterapkan pada data penting.</li>
        <li>Konten tutorial bukan merupakan saran keuangan, investasi, atau hukum profesional. Hubungi ahli yang kompeten untuk kebutuhan spesifik.</li>
        <li>Jika Anda menemukan konten yang tidak akurat, menyinggung, atau berbahaya, silakan gunakan tombol "Report" agar kami dapat meninjaunya.</li>
      </ul>
      <p>The app implements safety filters on both input (query blocklist) and output (health-claim detection) to reduce harmful generation, but no AI safety system is perfect.</p>

      <h2>3. How We Use Information</h2>
      <p>The information we collect is used for:</p>
      <ul>
        <li>Providing, maintaining, and improving the Service.</li>
        <li>Rate-limiting abusive usage (5 generation requests per IP per hour, 20 suggestion queries per IP per hour, 10 reports per IP per hour).</li>
        <li>Moderating user-submitted content via admin dashboard review.</li>
        <li>Serving personalized advertisements through Google AdMob (when applicable).</li>
      </ul>

      <h2>4. Data Retention</h2>
      <ul>
        <li><strong>Alamat IP (rate limit):</strong> disimpan sementara di edge cache selama maksimal 1 jam, lalu otomatis dihapus.</li>
        <li><strong>Query pencarian &amp; permintaan tutorial:</strong> disimpan secara permanen di database sebagai bagian dari katalog publik. Data pengenal pribadi tidak diasosiasikan dengan query ini.</li>
        <li><strong>Laporan pengguna:</strong> disimpan selama tutorial yang dilaporkan masih ada dalam sistem untuk riwayat audit.</li>
        <li><strong>Cache gambar di perangkat (mobile):</strong> disimpan secara lokal di perangkat Anda (maksimal 50 MB), dihapus otomatis via LRU atau saat hapus data aplikasi.</li>
      </ul>

      <h2>5. Third-Party Services</h2>
      <p>Our app uses the following third-party services:</p>
      <ul>
        <li><strong>Cloudflare Workers AI</strong> — for excel text and image generation. Cloudflare processes your query text during generation. See <a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener">Cloudflare Privacy Policy</a>.</li>
        <li><strong>Google AdMob</strong> — when enabled, displays ads and may collect advertising identifiers. See <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">Google Privacy Policy</a>. You can opt out of personalized ads via your Android device's Ad Settings.</li>
      </ul>

      <h2>6. User Rights</h2>
      <ul>
        <li><strong>Report content:</strong> use the in-app flag button to report any excel. After 3 verified reports, the excel is auto-unpublished pending admin review.</li>
        <li><strong>Request removal:</strong> if you submitted a query that you want removed from the catalog, contact us via the email below.</li>
        <li><strong>Access/deletion:</strong> since we do not collect personal identifiers, individual-level data deletion is generally not applicable. For specific concerns, contact us.</li>
      </ul>

      <h2>7. Data Security</h2>
      <p>The security of your data is important to us, but remember that no method of transmission over the Internet or method of electronic storage is 100% secure. While we strive to use commercially acceptable means to protect your personal information, we cannot guarantee its absolute security.</p>

      <h2>8. Children's Privacy</h2>
      <p>Our Service does not address anyone under the age of 13 ("Children"). We do not knowingly collect personally identifiable information from anyone under the age of 13. If you are a parent or guardian and you are aware that your Child has provided us with Personal Data, please contact us.</p>

      <h2>9. Changes to This Privacy Policy</h2>
      <p>We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.</p>

      <h2>10. Contact Us</h2>
      <p>If you have any questions about this Privacy Policy, please contact us:</p>
      <ul>
        <li>Email: <a href="mailto:plasodig@gmail.com">plasodig@gmail.com</a></li>
        <li>Contact Page: <a href="/contacts">excel.plasodig.my.id/contacts</a></li>
        <li>Terms of Use: <a href="/terms">excel.plasodig.my.id/terms</a></li>
      </ul>
    </div>
  </div>
  <footer>
    <p>&copy; ${new Date().getFullYear()} Tutorial Excel Indonesia — <a href="https://excel.plasodig.my.id">excel.plasodig.my.id</a></p>
  </footer>
</body>
</html>`;
}

export function termsOfUsePage() {
  return html`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Terms of Use - Tutorial Excel Indonesia</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <style>${raw(CSS)}</style>
</head>
<body>
  <div class="container">
    <nav class="nav">
      <a href="/">← Back to Home</a>
    </nav>
    <div class="content">
      <h1>Terms of Use</h1>
      <p><strong>Effective Date:</strong> ${new Date().toISOString().split("T")[0]}</p>
 
      <p>Dengan menggunakan aplikasi "Tutorial Excel Indonesia" (<code>com.plasodig.excel</code>) atau website <code>excel.plasodig.my.id</code> ("Layanan"), Anda menyetujui Ketentuan Penggunaan ini. Jika Anda tidak setuju, mohon untuk tidak menggunakan Layanan.</p>
 
      <h2>1. Karakter Konten</h2>
      <p>Seluruh tutorial dalam Layanan ini <strong>di-generate oleh kecerdasan buatan (AI)</strong>. Operator Layanan dan penyedia AI tidak memberikan jaminan dalam bentuk apa pun terkait akurasi, otentikasi, keamanan, atau kelengkapan tutorial. Rumus, parameter, langkah-langkah, dan teknik yang ditampilkan mungkin tidak tepat.</p>
 
      <h2>2. Bukan Saran Profesional</h2>
      <p>Konten dalam Layanan ini bukan merupakan saran keuangan, akuntansi, investasi, atau hukum profesional. Jangan mengandalkan tutorial AI untuk keputusan bisnis atau finansial yang kritis tanpa verifikasi dari ahli.</p>
 
      <h2>3. Tanggung Jawab Pengguna</h2>
      <p>Anda menggunakan rumusan dan teknik Excel dalam Layanan ini atas risiko Anda sendiri. Kami tidak bertanggung jawab atas kesalahan kalkulasi, kehilangan data, atau kerugian finansial yang mungkin terjadi akibat penerapan konten dari Layanan ini.</p>
 
      <h2>4. Penggunaan yang Diperbolehkan</h2>
      <p>Anda dilarang:</p>
      <ul>
        <li>Mengirimkan permintaan konten di luar topik Excel (senjata, obat-obatan, konten dewasa, klaim medis, ujaran kebencian, dll).</li>
        <li>Mencoba melewati filter keamanan atau batasan (rate limit).</li>
        <li>Menggunakan Layanan untuk membuat konten yang bermaksud merugikan orang lain.</li>
        <li>Melakukan reverse-engineer, scraping, atau mendistribusikan Layanan tanpa izin.</li>
      </ul>
      <p>Kami berhak memblokir akses dan melaporkan kueri yang melanggar ketentuan.</p>
 
      <h2>5. Pelaporan Konten</h2>
      <p>Jika Anda menemukan tutorial yang tidak akurat, menyinggung, atau berbahaya, silakan gunakan tombol Report. Laporan akan ditinjau oleh admin; tutorial yang mendapatkan 3 laporan atau lebih akan otomatis disembunyikan untuk ditinjau lebih lanjut.</p>
 
      <h2>6. Batasan Kewajiban</h2>
      <p>DENGAN TINGKAT MAKSIMAL YANG DIIZINKAN OLEH HUKUM, KAMI TIDAK BERTANGGUNG JAWAB ATAS KERUGIAN LANGSUNG MAUPUN TIDAK LANGSUNG, TERMASUK NAMUN TIDAK TERBATAS PADA KERUGIAN FINANSIAL, KEHILANGAN DATA, ATAU KERUSAKAN SISTEM, YANG TIMBUL DARI PENGGUNAAN LAYANAN INI.</p>
 
      <h2>7. Hak Kekayaan Intelektual</h2>
      <p>Teks tutorial di-generate oleh model Meta Llama (Llama 3 Community License). Gambar di-generate oleh Black Forest Labs FLUX.1. Kode aplikasi dan UI adalah hak cipta Plasodig. Anda diperbolehkan menggunakan tutorial untuk tujuan pembelajaran pribadi.</p>
 
      <h2>8. Perubahan</h2>
      <p>Kami dapat memperbarui Ketentuan ini sewaktu-waktu. Penggunaan berkelanjutan atas Layanan merupakan persetujuan atas Ketentuan terbaru.</p>
 
      <h2>9. Kontak</h2>
      <p>Pertanyaan? <a href="mailto:plasodig@gmail.com">plasodig@gmail.com</a> | <a href="/privacy-policy">Privacy Policy</a></p>
    </div>
  </div>
  <footer>
    <p>&copy; ${new Date().getFullYear()} Tutorial Excel Indonesia — <a href="https://excel.plasodig.my.id">excel.plasodig.my.id</a></p>
  </footer>
</body>
</html>`;
}

export function contactPage() {
  return html`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Contact Us - Tutorial Excel Indonesia</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
  <style>${raw(CSS)}</style>
  <style>
    .contact-card { display: flex; align-items: center; gap: 16px; padding: 24px; background: var(--surface2); border-radius: 12px; margin-top: 24px; border: 1px solid var(--border); }
    .contact-icon { font-size: 32px; background: rgba(249,115,22,0.1); width: 64px; height: 64px; display: flex; align-items: center; justify-content: center; border-radius: 50%; color: var(--primary); }
    .contact-info h3 { margin-bottom: 4px; color: #fff; font-size: 18px; }
    .contact-info p { margin: 0; }
  </style>
</head>
<body>
  <div class="container">
    <nav class="nav">
      <a href="/">← Back to Home</a>
    </nav>
    <div class="content">
      <h1>Contact Us</h1>
      <p>Jika Anda memiliki pertanyaan seputar aplikasi <strong>Tutorial Excel Indonesia</strong> atau website kami, silakan hubungi kami. Kami akan merespons sesegera mungkin.</p>
      
      <div class="contact-card">
        <div class="contact-icon">✉️</div>
        <div class="contact-info">
          <h3>Email Kami</h3>
          <p><a href="mailto:plasodig@gmail.com">plasodig@gmail.com</a></p>
        </div>
      </div>
      
      <div class="contact-card">
        <div class="contact-icon">🌐</div>
        <div class="contact-info">
          <h3>Website</h3>
          <p><a href="https://excel.plasodig.my.id">excel.plasodig.my.id</a></p>
        </div>
      </div>
      
      <div style="margin-top: 40px; padding-top: 24px; border-top: 1px solid var(--border);">
        <p><strong>App Package Name:</strong> <code>com.plasodig.excel</code></p>
        <p>Sebelum mengirim email bantuan, harap pastikan Anda telah membaca <a href="/privacy-policy">Privacy Policy</a> kami.</p>
      </div>
    </div>
  </div>
  <footer>
    <p>&copy; ${new Date().getFullYear()} Tutorial Excel Indonesia — <a href="https://excel.plasodig.my.id">excel.plasodig.my.id</a></p>
  </footer>
</body>
</html>`;
}
