package com.mashxall.unified

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-protected personal-backup envelope independent of Android-only JSON APIs.
 *
 * AES-GCM authenticates encrypted data. PBKDF2 metadata is versioned and stored
 * with the envelope so future versions can migrate it without guessing.
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
    private const val MAX_PLAINTEXT_BYTES = 16 * 1024 * 1024
    private const val MAX_ENVELOPE_BYTES = 40 * 1024 * 1024
    private val REQUIRED_FIELDS = setOf(
        "format", "version", "kdf", "iterations", "cipher", "tag_bits",
        "salt_hex", "iv_hex", "ciphertext_hex"
    )

    fun encrypt(plainText: String, passphrase: CharArray): ByteArray {
        require(passphrase.size >= 8) { "Passphrase must contain at least 8 characters" }
        val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
        require(plainBytes.size <= MAX_PLAINTEXT_BYTES) { "Backup contents are too large" }
        val salt = ByteArray(SALT_BYTES)
        val iv = ByteArray(IV_BYTES)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(iv)

        var keyBytes: ByteArray? = null
        var cipherText: ByteArray? = null
        try {
            val kdf = preferredKdf()
            keyBytes = deriveKey(passphrase, salt, ITERATIONS, kdf)
            val cipher = Cipher.getInstance(CIPHER_NAME)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad(kdf, ITERATIONS))
            cipherText = cipher.doFinal(plainBytes)
            val envelope = listOf(
                "format=$FORMAT",
                "version=$VERSION",
                "kdf=$kdf",
                "iterations=$ITERATIONS",
                "cipher=$CIPHER_NAME",
                "tag_bits=$TAG_BITS",
                "salt_hex=${toHex(salt)}",
                "iv_hex=${toHex(iv)}",
                "ciphertext_hex=${toHex(cipherText)}"
            ).joinToString(separator = "\n", postfix = "\n")
            return envelope.toByteArray(StandardCharsets.UTF_8)
        } finally {
            passphrase.fill('\u0000')
            keyBytes?.fill(0)
            cipherText?.fill(0)
            plainBytes.fill(0)
            salt.fill(0)
            iv.fill(0)
        }
    }

    fun decrypt(payload: ByteArray, passphrase: CharArray): String {
        require(payload.isNotEmpty() && payload.size <= MAX_ENVELOPE_BYTES) { "Backup file size is invalid" }
        require(passphrase.size >= 8) { "Passphrase must contain at least 8 characters" }
        var keyBytes: ByteArray? = null
        var cipherText: ByteArray? = null
        var salt: ByteArray? = null
        var iv: ByteArray? = null
        try {
            val envelope = parseEnvelope(String(payload, StandardCharsets.UTF_8))
            require(envelope["format"] == FORMAT) { "Not an encrypted Mashxall backup" }
            require(envelope["version"]?.toIntOrNull() == VERSION) { "Unsupported backup version" }
            require(envelope["cipher"] == CIPHER_NAME) { "Unsupported backup cipher" }
            val kdf = envelope["kdf"].orEmpty()
            require(kdf == "PBKDF2WithHmacSHA256" || kdf == "PBKDF2WithHmacSHA1") {
                "Unsupported backup key derivation"
            }
            val iterations = envelope["iterations"]?.toIntOrNull() ?: -1
            require(iterations in 100_000..1_000_000) { "Invalid backup key-derivation work factor" }
            require(envelope["tag_bits"]?.toIntOrNull() == TAG_BITS) { "Unsupported backup authentication tag" }
            salt = fromHex(envelope["salt_hex"].orEmpty())
            iv = fromHex(envelope["iv_hex"].orEmpty())
            cipherText = fromHex(envelope["ciphertext_hex"].orEmpty())
            require(salt.size == SALT_BYTES && iv.size == IV_BYTES) { "Invalid backup encryption parameters" }
            require(cipherText.size in (TAG_BITS / 8 + 1)..(MAX_PLAINTEXT_BYTES + TAG_BITS / 8)) {
                "Invalid encrypted backup payload"
            }

            keyBytes = deriveKey(passphrase, salt, iterations, kdf)
            val cipher = Cipher.getInstance(CIPHER_NAME)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad(kdf, iterations))
            val plainBytes = cipher.doFinal(cipherText)
            return try {
                String(plainBytes, StandardCharsets.UTF_8)
            } finally {
                plainBytes.fill(0)
            }
        } finally {
            passphrase.fill('\u0000')
            keyBytes?.fill(0)
            cipherText?.fill(0)
            salt?.fill(0)
            iv?.fill(0)
        }
    }

    private fun parseEnvelope(text: String): Map<String, String> {
        require(text.length <= MAX_ENVELOPE_BYTES) { "Backup envelope is too large" }
        val fields = linkedMapOf<String, String>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val separator = line.indexOf('=')
            require(separator > 0) { "Invalid backup envelope line" }
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1)
            require(key in REQUIRED_FIELDS) { "Unknown backup envelope field" }
            require(value.isNotBlank()) { "Missing backup envelope value" }
            require(!fields.containsKey(key)) { "Duplicate backup envelope field" }
            fields[key] = value
        }
        require(fields.keys == REQUIRED_FIELDS) { "Backup envelope is incomplete" }
        return fields
    }

    private fun preferredKdf(): String =
        if (runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }.isSuccess) {
            "PBKDF2WithHmacSHA256"
        } else {
            // Compatibility fallback for older Android crypto providers.
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
        require(value.length <= (MAX_PLAINTEXT_BYTES + TAG_BITS / 8) * 2) { "Backup field is too large" }
        val output = ByteArray(value.length / 2)
        for (index in output.indices) {
            val high = value[index * 2].digitToIntOrNull(16) ?: error("Invalid hexadecimal backup field")
            val low = value[index * 2 + 1].digitToIntOrNull(16) ?: error("Invalid hexadecimal backup field")
            output[index] = ((high shl 4) or low).toByte()
        }
        return output
    }
}
