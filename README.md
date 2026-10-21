📌 Pengertian Proyek
Catatan Keuangan adalah sebuah aplikasi Android berbasis Native (Kotlin) yang dirancang untuk membantu pengguna mencatat dan mengelola arus kas keuangan harian secara praktis. Aplikasi ini memungkinkan pengguna untuk memantau pemasukan dan pengeluaran secara terorganisir serta memberikan ringkasan transaksi agar pengguna dapat menjaga kestabilan finansial pribadi.

✨ Fitur-Fitur Utama
Pencatatan Transaksi Harian

Menambahkan data transaksi baru (Pemasukan atau Pengeluaran).

Menginput nominal uang, kategori transaksi, dan tanggal pencatatan.

Manajemen Data (CRUD)

View Transaksi: Menampilkan riwayat daftar transaksi keuangan secara terurut.

Hapus Transaksi: Fitur untuk menghapus catatan transaksi yang salah atau tidak lagi diperlukan.

Format Mata Uang Otomatis

Konversi angka nominal ke format mata uang Rupiah (Rp) secara otomatis.

Antarmuka Modern (Jetpack Compose / Material Design)

Tampilan antarmuka yang responsif, bersih, dan gampang digunakan (user-friendly).

Penyimpanan Lokal (Local Database)

Data transaksi tersimpan aman di dalam penyimpanan lokal perangkat menggunakan database SQLite / Room.

📁 Struktur Singkat Proyek
Berikut adalah gambaran struktur folder dan file utama dalam proyek Android ini:

Plaintext
Catatan-Keuangan/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/myapplication/
│   │       │   ├── MainActivity.kt        # Activity utama & logika tampilan layar
│   │       │   ├── AppDatabase.kt         # Konfigurasi database lokal (Room)
│   │       │   ├── FinancialDao.kt        # Data Access Object (Query Database)
│   │       │   └── ui/theme/              # Tema, warna, dan gaya tampilan aplikasi
│   │       └── AndroidManifest.xml        # Konfigurasi izin & komponen aplikasi Android
│   └── build.gradle.kts                   # Dependency & pustaka yang digunakan di modul app
├── gradle/                                # Script & konfigurasi Gradle Wrapper
├── build.gradle.kts                       # Konfigurasi Gradle tingkat proyek (Root)
└── settings.gradle.kts                    # Pengaturan modul proyek
