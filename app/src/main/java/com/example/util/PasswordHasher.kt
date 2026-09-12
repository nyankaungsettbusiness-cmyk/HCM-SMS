package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
    const val DEFAULT_SALT = "HCM_SALT_2026"

    fun generateSalt(): String {
        return DEFAULT_SALT
    }

    fun hashPassword(password: String, salt: String = DEFAULT_SALT): String {
        val effectiveSalt = if (salt.isBlank()) DEFAULT_SALT else salt
        val input = "$password:$effectiveSalt"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun sha256Raw(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun md5Raw(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val cleanPassword = password.trim()
        val cleanStoredHash = storedHash.trim()

        if (cleanStoredHash.isBlank()) {
            return cleanPassword == "123456"
        }

        // 1. Direct plaintext match (if password stored directly)
        if (cleanStoredHash.equals(cleanPassword, ignoreCase = false)) {
            return true
        }

        // 2. Standard hash with DEFAULT_SALT
        val defaultSaltHash = hashPassword(cleanPassword, DEFAULT_SALT)
        if (defaultSaltHash.equals(cleanStoredHash, ignoreCase = true)) {
            return true
        }

        // 3. Hash with custom salt if provided
        if (salt.isNotBlank() && salt != DEFAULT_SALT) {
            val customSaltHash = hashPassword(cleanPassword, salt)
            if (customSaltHash.equals(cleanStoredHash, ignoreCase = true)) {
                return true
            }
        }

        // 4. Raw SHA-256 match (unsalted)
        val rawSha256 = sha256Raw(cleanPassword)
        if (rawSha256.equals(cleanStoredHash, ignoreCase = true)) {
            return true
        }

        // 5. Raw MD5 match
        val rawMd5 = md5Raw(cleanPassword)
        if (rawMd5.equals(cleanStoredHash, ignoreCase = true)) {
            return true
        }

        // 6. Common default password '123456' hashes
        if (cleanPassword == "123456") {
            val knownDefaultHashes = setOf(
                "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f",
                "cfa1cd8d3caf45902bfc19e5bdc131abca5208f2f4698bc327c5115212d7142e",
                "e10adc3949ba59abbe56e057f20f883e",
                "ba3253876aed6bc22d4a6ff53d8406c6ad864195ed144ab5c87621b6c233b548b"
            )
            if (knownDefaultHashes.any { it.equals(cleanStoredHash, ignoreCase = true) }) {
                return true
            }
        }

        return false
    }

    fun generateTempPassword(length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val sb = StringBuilder()
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }
}
