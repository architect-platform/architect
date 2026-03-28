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

class SecretStore(
    storePath: Path = defaultStorePath(),
    private val backends: List<SecretStoreBackend> = defaultBackends(storePath),
) {
    fun set(name: String, value: String) {
        val primaryBackend = availableBackends().first()
        primaryBackend.set(name, value)
        availableBackends()
            .filter { it !== primaryBackend }
            .forEach { it.delete(name) }
    }

    fun get(name: String): String? = availableBackends().firstNotNullOfOrNull { it.get(name) }

    fun delete(name: String): Boolean = availableBackends().fold(false) { removed, backend ->
        backend.delete(name) || removed
    }

    fun listKeys(): List<String> = availableBackends().flatMap { it.listKeys() }.toSortedSet().toList()

    private fun availableBackends(): List<SecretStoreBackend> = backends.filter { it.isAvailable() }

    companion object {
        internal fun defaultStorePath(): Path = Path.of(System.getProperty("user.home"), ".architect", "secrets.enc")

        internal fun defaultBackends(storePath: Path): List<SecretStoreBackend> {
            val fallbackStore = EncryptedFileSecretStoreBackend(storePath)
            val indexPath = (storePath.parent ?: defaultStorePath().parent).resolve("secrets.keychain-index.json")
            return listOfNotNull(
                PlatformKeychainSecretStore.create(indexPath),
                fallbackStore,
            )
        }
    }
}

interface SecretStoreBackend {
    fun isAvailable(): Boolean

    fun set(name: String, value: String)

    fun get(name: String): String?

    fun delete(name: String): Boolean

    fun listKeys(): List<String>
}

/**
 * Local encrypted secret store backed by a JSON file on disk.
 *
 * Each entry is encrypted with AES/GCM/NoPadding using a key derived from a
 * machine-specific passphrase via PBKDF2WithHmacSHA256. A fresh 12-byte IV is
 * generated per entry so every write is unique.
 */
internal class EncryptedFileSecretStoreBackend(
    private val storePath: Path,
) : SecretStoreBackend {
    private val mapper = jacksonObjectMapper()
    private val random = SecureRandom()

    private val machineSalt: ByteArray by lazy {
        val hostname = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("localhost")
        val username = System.getProperty("user.name", "user")
        "$hostname:$username".toByteArray(Charsets.UTF_8)
    }

    override fun isAvailable(): Boolean = true

    override fun set(name: String, value: String) {
        val (iv, ciphertext) = encrypt(value)
        val b64 = Base64.getEncoder()
        val entry = SecretEntry(
            key = name,
            iv = b64.encodeToString(iv),
            value = b64.encodeToString(ciphertext),
        )
        val entries = loadEntries()
        val idx = entries.indexOfFirst { it.key == name }
        if (idx >= 0) {
            entries[idx] = entry
        } else {
            entries.add(entry)
        }
        saveEntries(entries)
    }

    override fun get(name: String): String? {
        val b64 = Base64.getDecoder()
        return loadEntries()
            .firstOrNull { it.key == name }
            ?.let { decrypt(b64.decode(it.iv), b64.decode(it.value)) }
    }

    override fun delete(name: String): Boolean {
        val entries = loadEntries()
        val removed = entries.removeIf { it.key == name }
        if (removed) {
            saveEntries(entries)
        }
        return removed
    }

    override fun listKeys(): List<String> = loadEntries().map { it.key }

    private fun deriveKey(): SecretKeySpec {
        val passphrase = (machineSalt.toString(Charsets.UTF_8) + "-architect-secrets").toCharArray()
        val spec = PBEKeySpec(passphrase, machineSalt, 65_536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

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

    private fun loadEntries(): MutableList<SecretEntry> {
        if (!Files.exists(storePath)) {
            return mutableListOf()
        }
        return mapper.readValue(storePath.toFile())
    }

    private fun saveEntries(entries: List<SecretEntry>) {
        Files.createDirectories(storePath.parent)
        if (entries.isEmpty()) {
            Files.deleteIfExists(storePath)
            return
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), entries)
    }
}

internal data class SecretEntry(
    @JsonProperty("key") val key: String,
    @JsonProperty("iv") val iv: String,
    @JsonProperty("value") val value: String,
)
