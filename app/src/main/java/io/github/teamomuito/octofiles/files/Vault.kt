package io.github.teamomuito.octofiles.files

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.File
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted folder in app-private storage. Each file is AES-256-GCM under a key that lives in
 * the Android Keystore. Names are kept in the local database, not in the file names.
 *
 * The biometric prompt is enforced by the UI. The key itself doesn't require authentication,
 * so anything that can run as this app could decrypt, which is the usual trade-off for this design.
 */
class Vault(context: Context, private val db: FilesDb) {
    private val dir = File(context.filesDir, "vault").apply { mkdirs() }

    fun items(): List<VaultRow> = db.vaultAll()

    /** Encrypts a copy of [src] into the vault. Remove the original yourself once this returns true. */
    fun add(src: File): Boolean = runCatching {
        val id = UUID.randomUUID().toString()
        encrypt(src, File(dir, id))
        db.vaultAdd(VaultRow(id, src.name, src.length(), System.currentTimeMillis()))
        true
    }.getOrDefault(false)

    /** Decrypts one item into [folder] under its original name. */
    fun export(row: VaultRow, folder: File): File? = runCatching {
        folder.mkdirs()
        val target = Fs.uniqueIn(folder, row.name)
        decrypt(File(dir, row.id), target)
        target
    }.getOrNull()

    fun remove(row: VaultRow) {
        File(dir, row.id).delete()
        db.vaultRemove(row.id)
    }

    private fun key(): SecretKey {
        val store = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getEntry(ALIAS, null) as? java.security.KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(src: File, dst: File) {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        dst.outputStream().use { out ->
            out.write(cipher.iv)
            CipherOutputStream(out, cipher).use { cos ->
                src.inputStream().use { it.copyTo(cos) }
            }
        }
    }

    private fun decrypt(src: File, dst: File) {
        src.inputStream().use { input ->
            val iv = ByteArray(IV_BYTES)
            DataInputStream(input).readFully(iv)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            dst.outputStream().use { out ->
                CipherInputStream(input, cipher).use { it.copyTo(out) }
            }
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "octo-files-vault"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
