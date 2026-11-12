package com.pankobyte.launcher.kid.pin

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

const val PIN_LENGTH = 4
const val FREE_TRIES = 5
/**
 * The fallback door for a parent who forgot their PIN. It is the same on every phone and works
 * wherever the parent PIN is asked for. Wrong tries and the lock apply to it like any other code.
 */
const val MASTER_PIN = "7294"

fun isMasterPin(pin: String): Boolean = pin == MASTER_PIN

private const val FIRST_LOCK_MS = 30_000L
private const val MAX_LOCK_MS = 15 * 60_000L

/**
 * Lock time after [failCount] wrong tries in a row.
 * Tries 1 to 4 are free. The 5th locks for 30 s and each further one doubles it, up to 15 minutes.
 */
fun lockoutMs(failCount: Int): Long {
    if (failCount < FREE_TRIES) return 0
    val doublings = (failCount - FREE_TRIES).coerceAtMost(10)
    return (FIRST_LOCK_MS shl doublings).coerceAtMost(MAX_LOCK_MS)
}

object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison of the derived keys. */
    fun verify(secret: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(secret, salt), expected)

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
}
