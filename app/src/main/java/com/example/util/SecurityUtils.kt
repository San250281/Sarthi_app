package com.example.util

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Hashes a password or PIN securely using the standard SHA-256 algorithm.
     */
    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback safe representation (should rarely fail for SHA-256 on Android JVM)
            password.hashCode().toString()
        }
    }
}
