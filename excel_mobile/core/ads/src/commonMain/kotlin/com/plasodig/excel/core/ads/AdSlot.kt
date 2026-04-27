package com.plasodig.excel.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Penempatan ad yang didukung. Enum terpusat supaya mudah toggle placement dari satu tempat.
 * Tambah varian baru di sini lalu handle di implementasi expect.
 */
enum class AdPlacement {
    /** Banner adaptif di footer detail tutorial — muncul setelah semua langkah masak. */
    DetailFooterBanner,

    /**
     * Native Advanced ad di daftar tutorial — disisipkan setiap 8 item.
     * Tampilan blend dengan ExcelCard (media, judul, deskripsi, tombol CTA, label "Sponsored").
     */
    ListInFeedNative,
}

/**
 * Render iklan AdMob sesuai placement. Di Android, meng-inflate AdView + load request.
 * Di iOS, sementara no-op (return kosong) karena butuh CocoaPods setup di iosApp/.
 *
 * Caller **tidak perlu** tahu implementasi platform — cukup `AdSlot(AdPlacement.X)`.
 *
 * Policy notes (AdMob):
 * - Jangan kasih margin 0 dengan tombol interaktif di sekitar — minimal 20 dp buffer.
 * - Jangan pasang di atas tombol favorit card atau bottom bar (accidental click risk).
 * - Banner adaptif otomatis pakai ukuran optimal untuk lebar layar.
 */
@Composable
expect fun AdSlot(
    placement: AdPlacement,
    modifier: Modifier = Modifier,
)
