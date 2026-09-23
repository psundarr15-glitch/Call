package com.example.callvault

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Encrypted format: salt(16 bytes) + iv(12 bytes) + ciphertext
object BackupCrypto {

    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv   = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key  = deriveKey(password, salt)
        val c    = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return salt + iv + c.doFinal(data)
    }

    // Bug 4 fix: decrypt was completely missing
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        require(data.size > 28) { "Data too short to be a valid backup" }
        val salt       = data.sliceArray(0 until 16)
        val iv         = data.sliceArray(16 until 28)
        val ciphertext = data.sliceArray(28 until data.size)
        val key        = deriveKey(password, salt)
        val c          = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return c.doFinal(ciphertext)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password, salt, 120_000, 256))
            .encoded
}
