package io.github.architectplatform.core.secrets

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Local encrypted secret store backed by a JSON file on disk.
 *
 * Each entry is encrypted with AES/GCM/NoPadding using a key derived from a
 * machine-specific passphrase via PBKDF2WithHmacSHA256.  A fresh 12-byte IV is
 * generated per entry so every write is unique.
 *
 * Storage format (secrets.enc):
 * ```json
 * [
 *   { "key": "MY_SECRET", "iv": "<base64>", "value": "<base64 ciphertext>" }
 * ]
 * ```
 */
class SecretStore(
    private val storePath: Path = Path.of(System.getProperty("user.home"), ".architect", "secrets.enc"),
) {
    private val mapper = jacksonObjectMapper()
    private val random = SecureRandom()

    // ── Key derivation ────────────────────────────────────────────────────────

    /** Machine-specific salt so the derived key is tied to this host/user. */
    private val machineSalt: ByteArray by lazy {
        val hostname = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("localhost")
        val username = System.getProperty("user.name", "user")
        "$hostname:$username".toByteArray(Charsets.UTF_8)
    }

    private fun deriveKey(): SecretKeySpec {
        val passphrase = (machineSalt.toString(Charsets.UTF_8) + "-architect-secrets").toCharArray()
        val spec = PBEKeySpec(passphrase, machineSalt, 65_536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // ── Encrypt / Decrypt ─────────────────────────────────────────────────────

    private fun encrypt(plaintext: String): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), GCMParameterSpec(128, iv))
        return iv to cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    private fun decrypt(iv: ByteArray, ciphertext: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    // ── Storage helpers ───────────────────────────────────────────────────────

    private fun loadEntries(): MutableList<SecretEntry> {
        if (!Files.exists(storePath)) return mutableListOf()
        return mapper.readValue(storePath.toFile())
    }

    private fun saveEntries(entries: List<SecretEntry>) {
        Files.createDirectories(storePath.parent)
        mapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), entries)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Stores (or overwrites) an encrypted secret under [name].
     */
    fun set(name: String, value: String) {
        val (iv, ciphertext) = encrypt(value)
        val b64 = Base64.getEncoder()
        val entry = SecretEntry(
            key = name,
            iv = b64.encodeToString(iv),
            value = b64.encodeToString(ciphertext),
        )
        val entries = loadEntries()
        val idx = entries.indexOfFirst { it.key == name }
        if (idx >= 0) entries[idx] = entry else entries.add(entry)
        saveEntries(entries)
    }

    /**
     * Returns the plaintext value for [name], or `null` if not found.
     */
    fun get(name: String): String? {
        val b64 = Base64.getDecoder()
        return loadEntries()
            .firstOrNull { it.key == name }
            ?.let { decrypt(b64.decode(it.iv), b64.decode(it.value)) }
    }

    /**
     * Removes the secret with [name].  Returns `true` if it existed.
     */
    fun delete(name: String): Boolean {
        val entries = loadEntries()
        val removed = entries.removeIf { it.key == name }
        if (removed) saveEntries(entries)
        return removed
    }

    /**
     * Returns all stored secret names (not values).
     */
    fun listKeys(): List<String> = loadEntries().map { it.key }
}

/** Internal JSON representation of a single encrypted secret. */
private data class SecretEntry(
    @JsonProperty("key") val key: String,
    @JsonProperty("iv") val iv: String,
    @JsonProperty("value") val value: String,
)
