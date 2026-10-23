package com.mashxall.unified

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/**
 * Password-protected personal backup envelope.
 *
 * AES-GCM authenticates the encrypted payload and its versioned metadata.
 * PBKDF2 parameters are stored in the envelope so future versions can migrate
 * the format without guessing. The caller must supply a fresh CharArray; this
 * class clears it and all intermediate key material before returning.
 */
internal object BackupCrypto {
    const val FORMAT = "mashxall-encrypted-backup"
    private const val VERSION = 1
    private const val CIPHER_NAME = "AES/GCM/NoPadding"
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val MAX_PAYLOAD_BYTES = 16 * 1024 * 1024

    fun encrypt(plainText: String, passphrase: CharArray): ByteArray {
        require(passphrase.size >= 8) { "Passphrase must contain at least 8 characters" }
        val salt = ByteArray(SALT_BYTES)
        val iv = ByteArray(IV_BYTES)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(iv)

        var keyBytes: ByteArray? = null
        try {
            val kdf = preferredKdf()
            keyBytes = deriveKey(passphrase, salt, ITERATIONS, kdf)
            val cipher = Cipher.getInstance(CIPHER_NAME)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad(kdf, ITERATIONS))
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val envelope = JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("kdf", kdf)
                .put("iterations", ITERATIONS)
                .put("cipher", CIPHER_NAME)
                .put("tag_bits", TAG_BITS)
                .put("salt_hex", toHex(salt))
                .put("iv_hex", toHex(iv))
                .put("ciphertext_hex", toHex(cipherText))
            return envelope.toString(2).toByteArray(StandardCharsets.UTF_8)
        } finally {
            passphrase.fill('\u0000')
            keyBytes?.fill(0)
            salt.fill(0)
            iv.fill(0)
        }
    }

    fun decrypt(payload: ByteArray, passphrase: CharArray): String {
        require(payload.isNotEmpty() && payload.size <= MAX_PAYLOAD_BYTES) { "Backup file size is invalid" }
        require(passphrase.size >= 8) { "Passphrase must contain at least 8 characters" }
        var keyBytes: ByteArray? = null
        var cipherText: ByteArray? = null
        var salt: ByteArray? = null
        var iv: ByteArray? = null
        try {
            val envelope = JSONObject(String(payload, StandardCharsets.UTF_8))
            require(envelope.optString("format") == FORMAT) { "Not an encrypted Mashxall backup" }
            require(envelope.optInt("version") == VERSION) { "Unsupported backup version" }
            require(envelope.optString("cipher") == CIPHER_NAME) { "Unsupported backup cipher" }
            val kdf = envelope.optString("kdf")
            require(kdf == "PBKDF2WithHmacSHA256" || kdf == "PBKDF2WithHmacSHA1") { "Unsupported backup key derivation" }
            val iterations = envelope.optInt("iterations", -1)
            require(iterations in 100_000..1_000_000) { "Invalid backup key-derivation work factor" }
            require(envelope.optInt("tag_bits", -1) == TAG_BITS) { "Unsupported backup authentication tag" }
            salt = fromHex(envelope.getString("salt_hex"))
            iv = fromHex(envelope.getString("iv_hex"))
            cipherText = fromHex(envelope.getString("ciphertext_hex"))
            require(salt.size == SALT_BYTES && iv.size == IV_BYTES) { "Invalid backup encryption parameters" }
            require(cipherText.size in (TAG_BITS / 8 + 1)..MAX_PAYLOAD_BYTES) { "Invalid encrypted backup payload" }

            keyBytes = deriveKey(passphrase, salt, iterations, kdf)
            val cipher = Cipher.getInstance(CIPHER_NAME)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad(kdf, iterations))
            return String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } finally {
            passphrase.fill('\u0000')
            keyBytes?.fill(0)
            cipherText?.fill(0)
            salt?.fill(0)
            iv?.fill(0)
        }
    }

    private fun preferredKdf(): String =
        if (runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }.isSuccess) {
            "PBKDF2WithHmacSHA256"
        } else {
            // Broad compatibility fallback for older Android crypto providers.
            "PBKDF2WithHmacSHA1"
        }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int, kdf: String): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(kdf).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun aad(kdf: String, iterations: Int): ByteArray =
        "$FORMAT|$VERSION|$kdf|$iterations|$CIPHER_NAME|$TAG_BITS".toByteArray(StandardCharsets.UTF_8)

    private fun toHex(bytes: ByteArray): String {
        val alphabet = "0123456789abcdef"
        val output = CharArray(bytes.size * 2)
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            output[index * 2] = alphabet[value ushr 4]
            output[index * 2 + 1] = alphabet[value and 0x0f]
        }
        return String(output)
    }

    private fun fromHex(value: String): ByteArray {
        require(value.isNotEmpty() && value.length % 2 == 0) { "Invalid hexadecimal backup field" }
        require(value.length <= MAX_PAYLOAD_BYTES * 2) { "Backup field is too large" }
        val output = ByteArray(value.length / 2)
        for (index in output.indices) {
            val high = value[index * 2].digitToIntOrNull(16) ?: error("Invalid hexadecimal backup field")
            val low = value[index * 2 + 1].digitToIntOrNull(16) ?: error("Invalid hexadecimal backup field")
            output[index] = ((high shl 4) or low).toByte()
        }
        return output
    }
}
