package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts sensitive persisted values (auth session tokens and runtime endpoint config) at rest
 * using Android Keystore AES-256-GCM when available, with a deterministic stream-XOR fallback
 * on headless/JVM test environments so plaintext credentials are never written to disk.
 */
object SecureStorageCodec {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "anitrack_local_vault_aes_v1"
    private const val GCM_PREFIX = "v2g:"
    private const val FALLBACK_PREFIX = "v2x:"
    private const val GCM_TAG_BITS = 128
    private const val IV_LEN_BYTES = 12

    private val random = SecureRandom()

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainBytes)
            val ivB64 = Base64.getEncoder().encodeToString(iv)
            val dataB64 = Base64.getEncoder().encodeToString(encrypted)
            "$GCM_PREFIX$ivB64:$dataB64"
        }.getOrElse {
            encryptFallback(plainBytes)
        }
    }

    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        return when {
            stored.startsWith(GCM_PREFIX) -> runCatching {
                val payload = stored.removePrefix(GCM_PREFIX)
                val sep = payload.indexOf(':')
                if (sep <= 0) return@runCatching ""
                val iv = Base64.getDecoder().decode(payload.substring(0, sep))
                val encrypted = Base64.getDecoder().decode(payload.substring(sep + 1))
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateAesKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(encrypted), Charsets.UTF_8)
            }.getOrDefault("")

            stored.startsWith(FALLBACK_PREFIX) -> runCatching {
                val payload = stored.removePrefix(FALLBACK_PREFIX)
                val sep = payload.indexOf(':')
                if (sep <= 0) return@runCatching ""
                val nonce = Base64.getDecoder().decode(payload.substring(0, sep))
                val cipherBytes = Base64.getDecoder().decode(payload.substring(sep + 1))
                val plainBytes = xorWithStreamKey(cipherBytes, nonce)
                String(plainBytes, Charsets.UTF_8)
            }.getOrDefault("")

            else -> stored
        }
    }

    private fun getOrCreateAesKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encryptFallback(plainBytes: ByteArray): String {
        val nonce = ByteArray(12)
        random.nextBytes(nonce)
        val cipherBytes = xorWithStreamKey(plainBytes, nonce)
        val nonceB64 = Base64.getEncoder().encodeToString(nonce)
        val dataB64 = Base64.getEncoder().encodeToString(cipherBytes)
        return "$FALLBACK_PREFIX$nonceB64:$dataB64"
    }

    private fun xorWithStreamKey(input: ByteArray, nonce: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val seed = byteArrayOf(0x41, 0x55, 0x76, 0x32, 0x19, 0x6E, 0x2F, 0x5B)
        val output = ByteArray(input.size)
        var offset = 0
        var counter = 0
        while (offset < input.size) {
            digest.reset()
            digest.update(seed)
            digest.update(nonce)
            digest.update(counter.toByte())
            val block = digest.digest()
            val take = minOf(block.size, input.size - offset)
            for (i in 0 until take) {
                output[offset + i] = (input[offset + i].toInt() xor block[i].toInt()).toByte()
            }
            offset += take
            counter++
        }
        return output
    }
}
