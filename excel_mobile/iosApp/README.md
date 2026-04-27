# iOS App Setup

Folder ini berisi Swift entry point untuk iOS (`iOSApp.swift`, `ContentView.swift`, `Info.plist`).

**Xcode project file (`iosApp.xcodeproj`) belum di-generate** karena tidak bisa dibuat dari Windows.

## Langkah di macOS

1. Buka Android Studio, install plugin **Kotlin Multiplatform**.
2. Buka project root (folder `tutorial/`) di Android Studio.
3. Sync Gradle. Ini akan build framework `ComposeApp` untuk iOS target.
4. Dari Android Studio, pilih "Open in Xcode" untuk folder `iosApp/` — atau buat `iosApp.xcodeproj` secara manual:
   - New Project → iOS App → nama `iosApp`
   - Hapus `ContentView.swift` dan `iosApp.swift` yang di-generate
   - Drag-drop file `.swift` dari folder ini ke project
   - Di Build Phases, tambahkan "Run Script Phase" yang memanggil `../gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
   - Di Framework Search Paths, tambahkan path ke built framework
5. Select iPhone simulator dan run.

Untuk setup otomatis, direkomendasikan pakai template dari **Kotlin Multiplatform Wizard** (`kmp.jetbrains.com`) lalu copy-paste Swift file dari sini.

## Catatan arsitektur

- Bridge Kotlin-Swift via framework `ComposeApp` (lihat `composeApp/build.gradle.kts`)
- Entry point Kotlin: `MainViewController.kt` di `composeApp/src/iosMain`
- Swift memanggil: `MainViewControllerKt.MainViewController()` — mengembalikan `UIViewController` yang dibungkus `UIViewControllerRepresentable`
