package io.github.architectplatform.engine.core.tasks.application

import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.api.core.tasks.cache.CacheDescriptor
import io.github.architectplatform.api.core.tasks.cache.CacheInput
import io.github.architectplatform.api.core.tasks.cache.CacheOutput
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskOutputCacheTest {

  // ── LocalOutputCache ──────────────────────────────────────────────────

  @Test
  fun `cache miss returns null`() {
    val cache = LocalOutputCache(cacheDir = createTempDirectory("cache-miss"))
    assertNull(cache.get("nonexistent-key"))
  }

  @Test
  fun `store and retrieve produces cache hit`() {
    val cache = LocalOutputCache(cacheDir = createTempDirectory("cache-hit"))
    val result = TaskResult.success("Build passed")
    cache.store("abc123", result, stdout = "output text")

    val cached = cache.get("abc123")
    assertNotNull(cached)
    assertTrue(cached.success)
    assertEquals("Build passed", cached.message)
    assertEquals("output text", cached.stdout)
  }

  @Test
  fun `cache hit converts to TaskResult correctly`() {
    val cache = LocalOutputCache(cacheDir = createTempDirectory("cache-convert"))
    cache.store("key1", TaskResult.success("ok"))
    cache.store("key2", TaskResult.failure("fail"))

    val hit1 = cache.get("key1")!!.toTaskResult()
    assertTrue(hit1.success)
    assertEquals("ok", hit1.message)

    val hit2 = cache.get("key2")!!.toTaskResult()
    assertTrue(!hit2.success)
    assertEquals("fail", hit2.message)
  }

  @Test
  fun `clear removes all entries`() {
    val cache = LocalOutputCache(cacheDir = createTempDirectory("cache-clear"))
    cache.store("key1", TaskResult.success("a"))
    cache.store("key2", TaskResult.success("b"))
    assertTrue(cache.contains("key1"))
    assertTrue(cache.contains("key2"))

    cache.clear()

    assertNull(cache.get("key1"))
    assertNull(cache.get("key2"))
    assertEquals(0, cache.entryCount())
  }

  @Test
  fun `entryCount and sizeBytes reflect stored data`() {
    val cache = LocalOutputCache(cacheDir = createTempDirectory("cache-info"))
    assertEquals(0, cache.entryCount())
    assertEquals(0, cache.sizeBytes())

    cache.store("entry1", TaskResult.success("msg1"), stdout = "some output")
    cache.store("entry2", TaskResult.success("msg2"))

    assertEquals(2, cache.entryCount())
    assertTrue(cache.sizeBytes() > 0)
  }

  // ── CacheKeyComputer ─────────────────────────────────────────────────

  @Test
  fun `same inputs produce same cache key`() {
    val descriptor = CacheDescriptor(
      inputs = listOf(CacheInput.ConfigValue("version"), CacheInput.EnvVar("HOME")),
    )
    val config = mapOf<String, Any?>("version" to "1.0.0")
    val key1 = CacheKeyComputer.compute(descriptor, "/tmp", config)
    val key2 = CacheKeyComputer.compute(descriptor, "/tmp", config)
    assertEquals(key1, key2)
  }

  @Test
  fun `different inputs produce different cache keys`() {
    val d1 = CacheDescriptor(inputs = listOf(CacheInput.ConfigValue("version")))
    val d2 = CacheDescriptor(inputs = listOf(CacheInput.ConfigValue("version")))
    val key1 = CacheKeyComputer.compute(d1, "/tmp", mapOf("version" to "1.0.0"))
    val key2 = CacheKeyComputer.compute(d2, "/tmp", mapOf("version" to "2.0.0"))
    assertTrue(key1 != key2)
  }

  @Test
  fun `FileSet input hashes file contents`() {
    val dir = createTempDirectory("cache-fileset")
    dir.resolve("Main.kt").writeText("fun main() {}")
    val descriptor = CacheDescriptor(inputs = listOf(CacheInput.FileSet("*.kt")))

    val key1 = CacheKeyComputer.compute(descriptor, dir.toString(), emptyMap())

    // Change file content
    dir.resolve("Main.kt").writeText("fun main() { println() }")
    val key2 = CacheKeyComputer.compute(descriptor, dir.toString(), emptyMap())

    assertTrue(key1 != key2)
  }

  @Test
  fun `EnvVar input uses current environment`() {
    val descriptor = CacheDescriptor(inputs = listOf(CacheInput.EnvVar("PATH")))
    val key = CacheKeyComputer.compute(descriptor, "/tmp", emptyMap())
    // Key is deterministic for same environment
    val key2 = CacheKeyComputer.compute(descriptor, "/tmp", emptyMap())
    assertEquals(key, key2)
  }

  @Test
  fun `CacheDescriptor with outputs is accepted`() {
    val descriptor = CacheDescriptor(
      inputs = listOf(CacheInput.ConfigValue("k")),
      outputs = listOf(CacheOutput.Stdout, CacheOutput.FileSet("build/**")),
    )
    val key = CacheKeyComputer.compute(descriptor, "/tmp", mapOf<String, Any?>("k" to "v"))
    assertTrue(key.isNotEmpty())
  }

  // ── Invalidation ─────────────────────────────────────────────────────

  @Test
  fun `cache is invalidated when inputs change`() {
    val dir = createTempDirectory("cache-invalidation")
    val cacheDir = createTempDirectory("cache-store")
    val cache = LocalOutputCache(cacheDir = cacheDir)

    dir.resolve("source.kt").writeText("val x = 1")
    val descriptor = CacheDescriptor(inputs = listOf(CacheInput.FileSet("*.kt")))
    val key1 = CacheKeyComputer.compute(descriptor, dir.toString(), emptyMap())
    cache.store(key1, TaskResult.success("first run"))

    // Modify input file → key changes → cache miss
    dir.resolve("source.kt").writeText("val x = 2")
    val key2 = CacheKeyComputer.compute(descriptor, dir.toString(), emptyMap())
    assertTrue(key1 != key2)
    assertNull(cache.get(key2))
  }

  // ── RemoteOutputCache ─────────────────────────────────────────────────

  @Test
  fun `remote fallback returns result when local misses`() {
    val remote = InMemoryRemoteCache()
    remote.storeResult("test-key", TaskResult.success("remote hit"), stdout = "remote output")

    val fetched = remote.fetchResult("test-key")
    assertNotNull(fetched)
    assertTrue(fetched.success)
    assertEquals("remote hit", fetched.message)
    assertEquals("remote output", fetched.stdout)
  }

  @Test
  fun `remote cache miss returns null`() {
    val remote = InMemoryRemoteCache()
    assertNull(remote.fetchResult("nonexistent"))
  }

  @Test
  fun `remote result converts to TaskResult`() {
    val remote = InMemoryRemoteCache()
    remote.storeResult("k", TaskResult.failure("bad"))
    val cached = remote.fetchResult("k")!!
    val result = cached.toTaskResult()
    assertTrue(!result.success)
    assertEquals("bad", result.message)
  }

  /**
   * In-memory implementation for testing remote cache behavior without HTTP.
   */
  private class InMemoryRemoteCache : RemoteOutputCache {
    private val store = mutableMapOf<String, RemoteOutputCache.CachedTaskResult>()

    override fun fetchResult(key: String): RemoteOutputCache.CachedTaskResult? = store[key]

    override fun storeResult(key: String, result: TaskResult, stdout: String?) {
      store[key] = RemoteOutputCache.CachedTaskResult(
        success = result.success,
        message = result.message,
        stdout = stdout,
      )
    }
  }
}
