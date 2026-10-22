package com.junps.dompetku.core.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import com.junps.dompetku.data.local.DompetKuDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: DompetKuDatabase,
) {
    private val random = SecureRandom()

    suspend fun backupDatabase(destinationUri: Uri, password: CharArray): Boolean = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "dompetku-backup-${UUID.randomUUID()}")
        try {
            requireValidPassword(password)
            check(tempDir.mkdirs()) { "Tidak dapat membuat file sementara." }

            val databaseSnapshot = File(tempDir, DATABASE_FILE)
            val walSnapshot = File(tempDir, WAL_FILE)
            val sourceFiles = listOf(
                DATABASE_FILE to databaseSnapshot,
                WAL_FILE to walSnapshot,
            )

            // Hold Room's write lock while copying the database and WAL as one consistent snapshot.
            database.runInTransaction {
                sourceFiles.forEach { (name, destination) ->
                    val source = context.getDatabasePath(name)
                    if (source.exists()) source.copyTo(destination, overwrite = true)
                }
            }
            check(databaseSnapshot.isFile && databaseSnapshot.length() > 0) { "Database tidak ditemukan." }

            val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
            val iv = ByteArray(IV_BYTES).also(random::nextBytes)
            val header = createHeader(salt, iv)
            val cipher = createCipher(Cipher.ENCRYPT_MODE, password, salt, iv, header)
            val output = context.contentResolver.openOutputStream(destinationUri, "wt")
                ?: error("Lokasi penyimpanan tidak dapat dibuka.")

            output.use { rawOutput ->
                val boundedOutput = LimitedOutputStream(rawOutput, MAX_BACKUP_BYTES)
                boundedOutput.write(header)
                ZipOutputStream(BufferedOutputStream(CipherOutputStream(boundedOutput, cipher))).use { zip ->
                    sourceFiles.filter { (_, file) -> file.isFile }.forEach { (name, file) ->
                        zip.putNextEntry(ZipEntry(name))
                        file.inputStream().buffered().use { copyBounded(it, zip, MAX_DATABASE_BYTES) }
                        zip.closeEntry()
                    }
                }
            }
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        } finally {
            password.fill('\u0000')
            tempDir.deleteRecursively()
        }
    }

    suspend fun restoreDatabase(sourceUri: Uri, password: CharArray): Boolean = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "dompetku-restore-${UUID.randomUUID()}")
        try {
            requireValidPassword(password)
            check(tempDir.mkdirs()) { "Tidak dapat membuat file sementara." }

            val archive = File(tempDir, "backup.zip")
            val databaseSnapshot = File(tempDir, DATABASE_FILE)
            val walSnapshot = File(tempDir, WAL_FILE)
            decryptArchive(sourceUri, password, archive)
            extractDatabase(archive, databaseSnapshot, walSnapshot)
            check(databaseSnapshot.length() in 1..MAX_DATABASE_BYTES) { "File database tidak valid." }
            validateDatabase(databaseSnapshot)
            importDatabase(databaseSnapshot)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        } finally {
            password.fill('\u0000')
            tempDir.deleteRecursively()
        }
    }

    private fun decryptArchive(sourceUri: Uri, password: CharArray, archive: File) {
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: error("File cadangan tidak dapat dibuka.")
        input.use { rawInput ->
            val dataInput = DataInputStream(BufferedInputStream(rawInput))
            val header = ByteArray(HEADER_BYTES)
            dataInput.readFully(header)
            check(header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "Format file bukan backup DompetKu." }
            check(header[MAGIC.size].toInt() == FORMAT_VERSION) { "Versi backup tidak didukung." }

            val saltStart = MAGIC.size + 1
            val salt = header.copyOfRange(saltStart, saltStart + SALT_BYTES)
            val iv = header.copyOfRange(saltStart + SALT_BYTES, HEADER_BYTES)
            val cipher = createCipher(Cipher.DECRYPT_MODE, password, salt, iv, header)
            CipherInputStream(dataInput, cipher).use { decrypted ->
                archive.outputStream().buffered().use { output ->
                    copyBounded(decrypted, output, MAX_BACKUP_BYTES)
                }
            }
        }
    }

    private fun extractDatabase(archive: File, databaseSnapshot: File, walSnapshot: File) {
        var foundDatabase = false
        var foundWal = false
        ZipInputStream(BufferedInputStream(archive.inputStream())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                check(!entry.isDirectory) { "Arsip backup tidak valid." }
                val destination = when (entry.name) {
                    DATABASE_FILE -> {
                        check(!foundDatabase) { "Arsip memiliki entri duplikat." }
                        foundDatabase = true
                        databaseSnapshot
                    }
                    WAL_FILE -> {
                        check(!foundWal) { "Arsip memiliki entri duplikat." }
                        foundWal = true
                        walSnapshot
                    }
                    else -> error("Arsip berisi file yang tidak diizinkan.")
                }
                destination.outputStream().buffered().use { output ->
                    copyBounded(zip, output, MAX_DATABASE_BYTES)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        check(foundDatabase) { "File database tidak ada di dalam backup." }
    }

    private fun validateDatabase(file: File) {
        val restored = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            check(readPragmaInt(restored, "user_version") == readPragmaInt(database.openHelper.readableDatabase, "user_version")) {
                "Versi database backup tidak cocok dengan aplikasi ini."
            }
            val tables = mutableSetOf<String>()
            restored.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            check(tables.containsAll(REQUIRED_TABLES)) { "Struktur database backup tidak dikenal." }

            restored.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok") { "Database backup rusak." }
            }
            restored.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                check(!cursor.moveToFirst()) { "Relasi data di backup tidak valid." }
            }
        } finally {
            restored.close()
        }
    }

    private fun importDatabase(snapshot: File) {
        val liveDatabase = database.openHelper.writableDatabase
        var isAttached = false
        try {
            database.runInTransaction {
                liveDatabase.execSQL("ATTACH DATABASE ? AS incoming_backup", arrayOf(snapshot.absolutePath))
                isAttached = true
                liveDatabase.execSQL("DELETE FROM recurring_transactions")
                liveDatabase.execSQL("DELETE FROM transactions")
                liveDatabase.execSQL("DELETE FROM categories")
                liveDatabase.execSQL("DELETE FROM accounts")
                liveDatabase.execSQL(
                    "INSERT INTO accounts (id, name, initialBalance, createdAt) " +
                        "SELECT id, name, initialBalance, createdAt FROM incoming_backup.accounts",
                )
                liveDatabase.execSQL(
                    "INSERT INTO categories (id, name, type, iconName, colorCode, isDefault, monthlyBudget) " +
                        "SELECT id, name, type, iconName, colorCode, isDefault, monthlyBudget FROM incoming_backup.categories",
                )
                liveDatabase.execSQL(
                    "INSERT INTO transactions (id, amount, categoryId, accountId, transactionDate, note, createdAt) " +
                        "SELECT id, amount, categoryId, accountId, transactionDate, note, createdAt FROM incoming_backup.transactions",
                )
                liveDatabase.execSQL(
                    "INSERT INTO recurring_transactions " +
                        "(id, amount, categoryId, accountId, type, note, frequency, nextExecutionTime, isActive) " +
                        "SELECT id, amount, categoryId, accountId, type, note, frequency, nextExecutionTime, isActive " +
                        "FROM incoming_backup.recurring_transactions",
                )
            }
        } finally {
            if (isAttached) liveDatabase.execSQL("DETACH DATABASE incoming_backup")
        }
    }

    private fun createCipher(mode: Int, password: CharArray, salt: ByteArray, iv: ByteArray, header: ByteArray): Cipher {
        val keyBytes = deriveKey(password, salt)
        try {
            return Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
                updateAAD(header)
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val encodedPassword = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password))
        val passwordBytes = ByteArray(encodedPassword.remaining())
        encodedPassword.get(passwordBytes)
        val blockInput = salt + byteArrayOf(0, 0, 0, 1)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(passwordBytes, "HmacSHA256"))
        var current = mac.doFinal(blockInput)
        val result = current.copyOf()
        try {
            repeat(PBKDF2_ITERATIONS - 1) {
                val next = mac.doFinal(current)
                current.fill(0)
                current = next
                for (index in result.indices) result[index] = (result[index].toInt() xor current[index].toInt()).toByte()
            }
            return result
        } finally {
            if (encodedPassword.hasArray()) encodedPassword.array().fill(0)
            passwordBytes.fill(0)
            blockInput.fill(0)
            current.fill(0)
        }
    }

    private fun createHeader(salt: ByteArray, iv: ByteArray): ByteArray =
        MAGIC + byteArrayOf(FORMAT_VERSION.toByte()) + salt + iv

    private fun requireValidPassword(password: CharArray) {
        require(password.size in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH) {
            "Kata sandi backup harus 12–128 karakter."
        }
    }

    private fun copyBounded(input: InputStream, output: OutputStream, maximumBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            check(total <= maximumBytes) { "Ukuran file backup melewati batas." }
            output.write(buffer, 0, count)
        }
    }

    private fun readPragmaInt(database: SupportSQLiteDatabase, pragma: String): Int =
        database.query("PRAGMA $pragma").use { cursor ->
            check(cursor.moveToFirst()) { "Database tidak valid." }
            cursor.getInt(0)
        }

    private fun readPragmaInt(database: SQLiteDatabase, pragma: String): Int =
        database.rawQuery("PRAGMA $pragma", null).use { cursor ->
            check(cursor.moveToFirst()) { "Database tidak valid." }
            cursor.getInt(0)
        }

    private class LimitedOutputStream(
        output: OutputStream,
        private val maximumBytes: Long,
    ) : FilterOutputStream(output) {
        private var bytesWritten = 0L

        override fun write(value: Int) {
            check(bytesWritten < maximumBytes) { "Ukuran backup melewati batas." }
            out.write(value)
            bytesWritten++
        }

        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            check(bytesWritten + length <= maximumBytes) { "Ukuran backup melewati batas." }
            out.write(buffer, offset, length)
            bytesWritten += length
        }
    }

    private companion object {
        const val DATABASE_FILE = DompetKuDatabase.DATABASE_NAME
        const val WAL_FILE = "$DATABASE_FILE-wal"
        val MAGIC = "DOMPETKU".toByteArray(Charsets.US_ASCII)
        const val FORMAT_VERSION = 1
        const val SALT_BYTES = 16
        const val IV_BYTES = 12
        const val HEADER_BYTES = 8 + 1 + SALT_BYTES + IV_BYTES
        const val PBKDF2_ITERATIONS = 310_000
        const val KEY_BITS = 256
        const val GCM_TAG_BITS = 128
        const val MIN_PASSWORD_LENGTH = 12
        const val MAX_PASSWORD_LENGTH = 128
        const val MAX_DATABASE_BYTES = 128L * 1024 * 1024
        const val MAX_BACKUP_BYTES = 260L * 1024 * 1024
        val REQUIRED_TABLES = setOf("accounts", "categories", "transactions", "recurring_transactions")
    }
}
