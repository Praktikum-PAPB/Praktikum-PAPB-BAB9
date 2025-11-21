# KameraKu (CameraX Implementation) - Modul PAPB 9

Aplikasi kamera sederhana berbasis Android yang dibangun menggunakan Jetpack CameraX dan Jetpack Compose. Proyek ini mendemonstrasikan penggunaan API CameraX untuk pratinjau kamera, pengambilan gambar, pengaturan flash, perpindahan kamera, dan penyimpanan media yang aman.

## 📱 Fitur Utama

- **Live Preview**: Menampilkan umpan kamera secara real-time menggunakan PreviewView.
- **Image Capture**: Mengambil foto dengan latensi rendah.
- **Penyimpanan Galeri**: Menyimpan foto secara otomatis ke folder Pictures/KameraKu menggunakan MediaStore.
- **Switch Camera**: Beralih antara kamera belakang dan depan.
- **Flash/Torch**: Mode flash (Auto/On/Off) saat pengambilan gambar.
- **Thumbnail**: Menampilkan preview kecil dari foto terakhir yang diambil.

## 🛠️ Teknologi & Dependencies

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Camera Library**: CameraX (v1.3.4)
  - camera-core
  - camera-camera2
  - camera-lifecycle
  - camera-view

## 📚 Penjelasan Teknis (Tugas Praktikum)

### 1. Alur Izin (Permission Flow)

Aplikasi menggunakan sistem izin runtime modern Android untuk mengakses kamera.

- **Manifest**: Izin dideklarasikan di AndroidManifest.xml dengan `<uses-permission android:name="android.permission.CAMERA" />`.
- **Runtime Request**:
  - Saat aplikasi dibuka, `LaunchedEffect` memicu `ActivityResultLauncher`.
  - Dialog sistem muncul meminta persetujuan pengguna.
  - Hasil (`granted` atau `denied`) disimpan dalam variabel state `hasPermission`.
  - **Jika Diizinkan**, UI Kamera ditampilkan. **Jika Ditolak**, pesan peringatan ditampilkan.

### 2. Penyimpanan & MediaStore

Aplikasi menggunakan Scoped Storage melalui API MediaStore, sehingga tidak memerlukan izin `WRITE_EXTERNAL_STORAGE` pada Android 10 (API 29) ke atas.

- **ContentValues**: Metadata foto disiapkan (Nama file, MIME type `image/jpeg`, dan lokasi `Pictures/KameraKu`).
- **Insert**: ContentResolver membuat entri baru di database media sistem dan mengembalikan URI.
- **MediaScanner**: Setelah foto tersimpan, `MediaScannerConnection.scanFile()` dipanggil.
  - Fungsi ini krusial agar foto langsung muncul di aplikasi Galeri/Google Photos tanpa perlu restart HP.

### 3. Penanganan Rotasi (Rotation)

Untuk memastikan foto yang diambil dalam mode Landscape tidak tersimpan miring (Portrait) atau terbalik:

- Aplikasi membaca orientasi layar perangkat saat tombol "Ambil Foto" ditekan menggunakan `view.display.rotation`.
- Nilai rotasi tersebut dikirim ke objek `ImageCapture` melalui properti `targetRotation`.

```kotlin
// Contoh Kode
ic.targetRotation = view.display?.rotation ?: Surface.ROTATION_0
```

CameraX secara otomatis menyesuaikan metadata EXIF foto agar tampil tegak lurus di galeri sesuai orientasi fisik saat pemotretan.

## 🚀 Cara Menjalankan

1. Buka proyek di Android Studio.
2. Pastikan koneksi internet aktif untuk mengunduh dependencies Gradle.
3. Sambungkan perangkat Android fisik (Disarankan) atau gunakan Emulator.
4. Klik Run (Tombol Play Hijau).
5. Izinkan akses kamera saat diminta.

## 📝 Catatan Troubleshooting

**Jika foto tidak muncul di Galeri Emulator:**
- Pastikan kode `MediaScannerConnection` sudah aktif (sudah diimplementasikan di versi ini).
- Cek aplikasi "Files" atau "Google Photos" di dalam emulator, folder Pictures -> KameraKu.

---

**Dibuat oleh:**  
**Nama:** Anak Agung Ngurah Aditya Wirayudha  
**Mahasiswa Universitas Brawijaya**
