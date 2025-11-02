package io.github.architectplatform.plugins.git

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for GitUtils utility functions.
 */
class GitUtilsTest {

  @Test
  fun `escapeShellArg should not escape safe arguments`() {
    val safeArgs = listOf(
        "simple",
        "path/to/file",
        "file.txt",
        "branch-name",
        "tag_v1.0.0",
        "192.168.1.1:8080"
    )
    
    for (arg in safeArgs) {
      assertEquals(arg, GitUtils.escapeShellArg(arg), "Safe argument '$arg' should not be escaped")
    }
  }

  @Test
  fun `escapeShellArg should escape arguments with spaces`() {
    assertEquals("'hello world'", GitUtils.escapeShellArg("hello world"))
    assertEquals("'commit message'", GitUtils.escapeShellArg("commit message"))
  }

  @Test
  fun `escapeShellArg should escape arguments with single quotes`() {
    assertEquals("'it'\\''s working'", GitUtils.escapeShellArg("it's working"))
    assertEquals("'user'\\''s branch'", GitUtils.escapeShellArg("user's branch"))
  }

  @Test
  fun `escapeShellArg should escape arguments with special characters`() {
    assertEquals("'test;rm -rf'", GitUtils.escapeShellArg("test;rm -rf"))
    assertEquals("'test\$var'", GitUtils.escapeShellArg("test\$var"))
    assertEquals("'test`cmd`'", GitUtils.escapeShellArg("test`cmd`"))
  }

  @Test
  fun `isValidGitConfigKey should accept valid config keys`() {
    val validKeys = listOf(
        "user.name",
        "user.email",
        "core.editor",
        "core.autocrlf",
        "remote.origin.url",
        "branch.main.remote",
        "commit.gpgsign",
        "pull.rebase",
        "init.defaultBranch"
    )
    
    for (key in validKeys) {
      assertTrue(GitUtils.isValidGitConfigKey(key), "Valid key '$key' should be accepted")
    }
  }

  @Test
  fun `isValidGitConfigKey should reject invalid config keys`() {
    val invalidKeys = listOf(
        "",
        ".",
        ".user.name",
        "user.",
        "user..name",
        "user name",
        "user;name",
        "123.name",
        "user.123name",
        "user.name;rm -rf",
        "user.name\$var"
    )
    
    for (key in invalidKeys) {
      assertFalse(GitUtils.isValidGitConfigKey(key), "Invalid key '$key' should be rejected")
    }
  }

  @Test
  fun `isValidGitConfigKey should handle multi-level keys`() {
    assertTrue(GitUtils.isValidGitConfigKey("a.b"))
    assertTrue(GitUtils.isValidGitConfigKey("a.b.c"))
    assertTrue(GitUtils.isValidGitConfigKey("a.b.c.d"))
    assertTrue(GitUtils.isValidGitConfigKey("section-name.key"))
  }
}
