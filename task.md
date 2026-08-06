# Product Requirement Document (PRD) & Technical Specification

## Aplikasi Pencatatan Keuangan Pribadi — Android Native

---

## 1. Ringkasan Eksekutif dan Tujuan

- **Nama proyek:** DompetKu.
- **Jenis proyek:** Aplikasi Android pencatatan keuangan pribadi berbasis *offline-first*.
- **Tujuan:** Membantu pengguna mencatat pemasukan dan pengeluaran harian dengan cepat serta memahami distribusi keuangan bulanan tanpa bergantung pada internet.
- **Target pengguna:** Individu, pekerja, dan mahasiswa yang membutuhkan pencatatan keuangan sederhana, privat, dan mudah digunakan.
- **Prinsip produk:** Data disimpan secara lokal; MVP tidak menggunakan akun, iklan, backend, atau sinkronisasi cloud.

---

## 2. Ruang Lingkup MVP

### In-Scope

- Pencatatan, pengubahan, dan penghapusan transaksi pemasukan maupun pengeluaran.
- Kategori bawaan serta CRUD kategori kustom.
- Dashboard berisi total saldo, pemasukan bulanan, pengeluaran bulanan, dan lima transaksi terbaru.
- Riwayat transaksi yang dikelompokkan berdasarkan tanggal.
- Grafik distribusi pengeluaran per kategori untuk bulan yang dipilih.
- Navigasi antarlayar dan penanganan kondisi *loading*, *empty*, serta *error*.
- Penyimpanan lokal menggunakan Room dan infrastruktur migrasi skema eksplisit.
- Pengujian unit, database, dan migrasi yang relevan.

### Out-of-Scope

- Akun dan autentikasi pengguna.
- Sinkronisasi cloud atau backend terpusat.
- Pencarian dan filter transaksi tingkat lanjut.
- Sistem anggaran per kategori.
- Multi-dompet atau multi-rekening.
- Integrasi bank atau dompet digital.
- Ekspor/impor CSV atau PDF dan *backup* lokal.
- PIN atau autentikasi biometrik.

---

## 3. Fitur P0 — Must Have

1. **Dashboard dan ringkasan saldo**
   - Total saldo keseluruhan.
   - Total pemasukan dan pengeluaran pada bulan berjalan.
   - Lima transaksi terbaru.
   - *Floating Action Button* (`+`) untuk menambah transaksi.

2. **CRUD transaksi**
   - Form tambah dan edit berisi nominal, kategori, tanggal dan waktu, serta catatan opsional.
   - Tipe pemasukan/pengeluaran mengikuti tipe kategori yang dipilih.
   - Penghapusan transaksi menggunakan dialog konfirmasi.
   - Nominal wajib lebih besar dari nol.

3. **CRUD kategori**
   - Kategori bawaan untuk pemasukan dan pengeluaran tersedia sejak penggunaan pertama.
   - Pengguna dapat menambah, mengubah, dan menghapus kategori kustom.
   - Kategori bawaan tidak dapat dihapus.
   - Kategori yang masih digunakan transaksi tidak dapat dihapus.

4. **Riwayat transaksi**
   - Seluruh transaksi ditampilkan dari yang terbaru.
   - Transaksi dikelompokkan berdasarkan tanggal.

5. **Grafik dan analisis visual**
   - Pemilih bulan dan tahun.
   - Grafik pai distribusi pengeluaran per kategori.
   - Warna grafik mengikuti warna kategori.

6. **UI state dan error handling**
   - Tampilan khusus untuk kondisi *loading*, data kosong, dan kesalahan.
   - Kesalahan aksi ditampilkan melalui *snackbar* atau pesan yang mudah dipahami.

---

## 4. Spesifikasi Teknis dan Arsitektur

### Tech Stack

- **Platform:** Android Native.
- **Bahasa:** Kotlin.
- **UI:** Jetpack Compose dan Material 3.
- **Navigasi:** Navigation Compose.
- **Arsitektur:** MVVM dengan *Unidirectional Data Flow*; pemisahan lapisan data, domain, dan UI dilakukan secara pragmatis.
- **Database lokal:** Room di atas SQLite.
- **Asynchronous/stream:** Kotlin Coroutines, `Flow`, `StateFlow`, dan `SharedFlow` bila diperlukan.
- **Dependency injection:** Hilt.
- **Grafik:** Vico atau pustaka grafik Compose-native yang kompatibel dengan versi Compose proyek.
- **Pengujian:** JUnit, kotlinx-coroutines-test, Room testing, dan AndroidX Test.

### Keputusan Model Data

- Nominal disimpan sebagai `Long` dalam satuan Rupiah utuh untuk menghindari kesalahan pembulatan *floating point*.
- Tipe transaksi tidak disimpan ulang pada tabel transaksi. Tipe diturunkan dari kategori untuk menghindari inkonsistensi data.
- Waktu disimpan sebagai Unix epoch dalam milidetik (`Long`).
- ID menggunakan UUID dalam bentuk `String`.
- Kategori yang direferensikan transaksi menggunakan aturan `ON DELETE RESTRICT`.
- Setiap perubahan versi database produksi harus menggunakan migrasi eksplisit. `fallbackToDestructiveMigration()` tidak digunakan pada *production build*.

### Room Entity

```kotlin
enum class TransactionType {
    INCOME,
    EXPENSE
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorCode: String,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["transactionDate"])]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Long,
    val categoryId: String,
    val transactionDate: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Referensi Skema SQLite

```sql
CREATE TABLE IF NOT EXISTS `categories` (
    `id` TEXT NOT NULL,
    `name` TEXT NOT NULL,
    `type` TEXT NOT NULL,
    `iconName` TEXT NOT NULL,
    `colorCode` TEXT NOT NULL,
    `isDefault` INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `transactions` (
    `id` TEXT NOT NULL,
    `amount` INTEGER NOT NULL CHECK (`amount` > 0),
    `categoryId` TEXT NOT NULL,
    `transactionDate` INTEGER NOT NULL,
    `note` TEXT,
    `createdAt` INTEGER NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`categoryId`) REFERENCES `categories` (`id`)
        ON UPDATE NO ACTION ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS `index_transactions_categoryId`
    ON `transactions` (`categoryId`);

CREATE INDEX IF NOT EXISTS `index_transactions_transactionDate`
    ON `transactions` (`transactionDate`);
```

---

## 5. Alur Pengguna dan Struktur Layar

### Alur Utama Tambah Transaksi

```text
[Buka aplikasi]
      ↓
[Dashboard] → [Tekan FAB +] → [Form transaksi]
                                      ↓
                         [Isi nominal dan kategori]
                         [Pilih tanggal dan waktu]
                         [Isi catatan opsional]
                                      ↓
                                  [Simpan]
                                      ↓
                    [Dashboard diperbarui otomatis]
```

### Struktur Navigasi

1. **Dashboard**
   - Ringkasan saldo, pemasukan, dan pengeluaran.
   - Lima transaksi terbaru.
   - FAB tambah transaksi.

2. **Riwayat**
   - Daftar transaksi lengkap yang dikelompokkan per tanggal.
   - Aksi edit dan hapus transaksi.

3. **Laporan**
   - Pemilih bulan dan tahun.
   - Grafik pai pengeluaran per kategori.

4. **Kategori**
   - Daftar kategori pemasukan dan pengeluaran.
   - Aksi tambah, edit, dan hapus kategori kustom.

5. **Form Transaksi**
   - Dibuka dari FAB untuk tambah atau dari riwayat untuk edit.
   - Bukan bagian dari *bottom navigation*.

---

## 6. Task List dan Definition of Done

| No. | Status | Task | Detail | Definition of Done |
|---:|:---:|---|---|---|
| 1 | [x] | **Audit dan setup arsitektur** | Audit prototype, rapikan struktur paket, konfigurasi Hilt, Room, Coroutines, dan dependensi pengujian. | Proyek berhasil dikompilasi; struktur lapisan jelas; dependensi tidak konflik; prototype yang dipertahankan teridentifikasi. |
| 2 | [x] | **Database dan seed kategori** | Buat entity, converter, DAO, database, repository, serta *pre-populate* kategori bawaan. | Database terbentuk pada penggunaan pertama; kategori bawaan hanya ditanam sekali; nominal menggunakan `Long`; DAO CRUD lulus pengujian. |
| 3 | [x] | **Navigation dan shell UI** | Buat Navigation Compose, *bottom navigation*, `Scaffold`, rute form, serta struktur awal ViewModel dan UI state. | Semua layar dapat dinavigasi tanpa crash; tab aktif benar; tombol kembali bekerja sesuai ekspektasi. |
| 4 | [x] | **CRUD kategori** | Implementasikan repository, use case/ViewModel, daftar, dialog/form, dan validasi kategori. | Kategori kustom dapat ditambah dan diubah; kategori bawaan serta kategori yang digunakan transaksi tidak dapat dihapus; pesan kesalahan jelas. |
| 5 | [x] | **CRUD transaksi** | Implementasikan form tambah/edit, validasi nominal, pemilih kategori dan waktu, penyimpanan, serta dialog hapus. | Transaksi valid dapat dibuat, diubah, dan dihapus; input tidak valid ditolak tanpa crash; data tetap tersedia setelah aplikasi dibuka ulang. |
| 6 | [x] | **Dashboard dan kalkulasi** | Buat query agregasi dan hubungkan hasilnya secara reaktif ke dashboard. | Saldo, pemasukan bulanan, pengeluaran bulanan, dan lima transaksi terbaru berubah otomatis setelah mutasi data. |
| 7 | [x] | **Riwayat transaksi** | Tampilkan transaksi kronologis dengan pengelompokan tanggal serta aksi edit/hapus. | Semua transaksi tersusun dari terbaru, header tanggal benar, dan perubahan data langsung tercermin pada daftar. |
| 8 | [x] | **Laporan grafik** | Buat query agregasi per kategori dan tampilkan grafik pai untuk periode terpilih. | Nilai dan persentase grafik sesuai data bulan terpilih; warna sesuai kategori; kondisi tanpa data ditangani. |
| 9 | [x] | **UI states dan polish** | Tambahkan komponen *loading*, *empty*, *error*, snackbar, aksesibilitas dasar, serta responsivitas layar. | Seluruh layar menangani ketiga state; tidak ada teks terpotong pada ukuran layar target; aksi utama memiliki label aksesibilitas. |
| 10 | [ ] | **Migrasi, testing, dan verifikasi rilis** | Tambahkan ekspor schema Room, migrasi eksplisit, migration test, unit test kalkulasi/ViewModel, serta pengujian alur utama. | Seluruh test lulus; migrasi mempertahankan data; build debug berhasil; alur tambah-edit-hapus dan kalkulasi lolos pemeriksaan manual tanpa crash. |

---

## 7. Kriteria Penerimaan MVP

MVP dinyatakan selesai apabila:

- Seluruh task 1–10 memenuhi *Definition of Done* dan berstatus selesai.
- Aplikasi dapat digunakan sepenuhnya tanpa koneksi internet.
- Data transaksi dan kategori tetap tersedia setelah aplikasi ditutup dan dibuka kembali.
- Dashboard dan laporan menghasilkan angka yang sesuai dengan data transaksi.
- Tidak ada kehilangan data saat migrasi skema yang didukung.
- Alur utama tidak mengalami crash pada perangkat atau emulator target.
- Build debug dan seluruh pengujian otomatis berhasil.

---

## 8. Fase Setelah MVP

Urutan pengembangan lanjutan yang disarankan:

1. Pencarian dan filter transaksi.
2. Sistem anggaran per kategori.
3. Ekspor/impor dan *backup* lokal.
4. PIN atau autentikasi biometrik.
5. Multi-dompet.
6. Sinkronisasi cloud dan akun pengguna.
