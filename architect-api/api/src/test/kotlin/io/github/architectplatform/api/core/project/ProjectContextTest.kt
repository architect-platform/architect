package io.github.architectplatform.api.core.project

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Tests for [ProjectContext] — getKey extension, missing key, nested key, wrong type.
 */
class ProjectContextTest {

  @Test
  fun `context exposes dir and config`(@TempDir tmpDir: Path) {
    val config: Config = mapOf("name" to "my-project")
    val ctx = ProjectContext(dir = tmpDir, config = config)

    assertEquals(tmpDir, ctx.dir)
    assertEquals("my-project", ctx.config.getKey<String>("name"))
  }

  @Test
  fun `getKey returns null for missing key`(@TempDir tmpDir: Path) {
    val ctx = ProjectContext(dir = tmpDir, config = emptyMap())

    assertNull(ctx.config.getKey<String>("nonexistent"))
  }

  @Test
  fun `getKey traverses nested config`(@TempDir tmpDir: Path) {
    val config: Config = mapOf(
      "plugins" to mapOf(
        "git" to mapOf(
          "enabled" to true,
          "branch" to "main",
        ),
      ),
    )
    val ctx = ProjectContext(dir = tmpDir, config = config)

    assertEquals(true, ctx.config.getKey<Boolean>("plugins.git.enabled"))
    assertEquals("main", ctx.config.getKey<String>("plugins.git.branch"))
  }

  @Test
  fun `getKey returns null for partially missing nested path`(@TempDir tmpDir: Path) {
    val config: Config = mapOf("a" to mapOf("b" to 1))
    val ctx = ProjectContext(dir = tmpDir, config = config)

    assertNull(ctx.config.getKey<Int>("a.c"))
  }

  @Test
  fun `getKey returns value even when generic type differs due to JVM erasure`(@TempDir tmpDir: Path) {
    val config: Config = mapOf("count" to 42)
    val ctx = ProjectContext(dir = tmpDir, config = config)

    // Due to JVM type erasure, getKey<String> on an Int value returns the Int (unchecked cast)
    val result = ctx.config.getKey<Any>("count")
    assertEquals(42, result)
  }

  @Test
  fun `getKey throws when traversing through primitive`(@TempDir tmpDir: Path) {
    val config: Config = mapOf("name" to "hello")
    val ctx = ProjectContext(dir = tmpDir, config = config)

    assertThrows(IllegalStateException::class.java) {
      ctx.config.getKey<String>("name.sub")
    }
  }

  @Test
  fun `context with list config elements`(@TempDir tmpDir: Path) {
    val config: Config = mapOf(
      "tasks" to listOf(
        mapOf("id" to "build"),
        mapOf("id" to "test"),
      ),
    )
    val ctx = ProjectContext(dir = tmpDir, config = config)

    assertEquals("build", ctx.config.getKey<String>("tasks.0.id"))
    assertEquals("test", ctx.config.getKey<String>("tasks.1.id"))
  }

  @Test
  fun `data class equality`(@TempDir tmpDir: Path) {
    val config: Config = mapOf("k" to "v")
    val a = ProjectContext(dir = tmpDir, config = config)
    val b = ProjectContext(dir = tmpDir, config = config)

    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
  }

  @Test
  fun `resolvePath keeps paths within project root`(@TempDir tmpDir: Path) {
    val ctx = ProjectContext(dir = tmpDir, config = emptyMap())

    val resolved = ctx.resolvePath("nested/project")

    assertEquals(tmpDir.resolve("nested/project").toAbsolutePath().normalize(), resolved)
  }

  @Test
  fun `resolvePath rejects absolute paths`(@TempDir tmpDir: Path) {
    val ctx = ProjectContext(dir = tmpDir, config = emptyMap())

    assertThrows(IllegalArgumentException::class.java) {
      ctx.resolvePath("/tmp/outside")
    }
  }

  @Test
  fun `resolvePath rejects traversal outside project root`(@TempDir tmpDir: Path) {
    val ctx = ProjectContext(dir = tmpDir, config = emptyMap())

    assertThrows(IllegalArgumentException::class.java) {
      ctx.resolvePath("../outside")
    }
  }
}
