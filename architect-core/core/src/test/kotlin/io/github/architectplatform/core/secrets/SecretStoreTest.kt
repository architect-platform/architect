package io.github.architectplatform.core.secrets

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecretStoreTest {

    private fun storeAt(dir: Path) = SecretStore(storePath = dir.resolve("secrets.enc"))

    @Test
    fun `set and get roundtrip returns original value`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        store.set("DB_PASSWORD", "super-secret-123")
        assertEquals("super-secret-123", store.get("DB_PASSWORD"))
    }

    @Test
    fun `get nonexistent key returns null`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        assertNull(store.get("MISSING_KEY"))
    }

    @Test
    fun `delete removes key`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        store.set("TOKEN", "abc123")
        assertTrue(store.delete("TOKEN"))
        assertNull(store.get("TOKEN"))
    }

    @Test
    fun `delete returns false for nonexistent key`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        assertFalse(store.delete("NONEXISTENT"))
    }

    @Test
    fun `list shows all key names`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        store.set("API_KEY", "key-value")
        store.set("DB_URL", "jdbc:postgres://localhost/mydb")
        store.set("SECRET_TOKEN", "token-xyz")
        val keys = store.listKeys()
        assertEquals(setOf("API_KEY", "DB_URL", "SECRET_TOKEN"), keys.toSet())
    }

    @Test
    fun `set overwrites existing key`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        store.set("MY_SECRET", "original")
        store.set("MY_SECRET", "updated")
        assertEquals("updated", store.get("MY_SECRET"))
        assertEquals(1, store.listKeys().count { it == "MY_SECRET" })
    }

    @Test
    fun `multiple secrets are stored independently`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        store.set("KEY_A", "value-a")
        store.set("KEY_B", "value-b")
        assertEquals("value-a", store.get("KEY_A"))
        assertEquals("value-b", store.get("KEY_B"))
    }

    @Test
    fun `list is empty when no secrets stored`(@TempDir tempDir: Path) {
        val store = storeAt(tempDir)
        assertTrue(store.listKeys().isEmpty())
    }
}
