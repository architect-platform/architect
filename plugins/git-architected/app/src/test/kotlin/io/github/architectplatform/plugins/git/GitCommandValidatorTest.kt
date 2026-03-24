package io.github.architectplatform.plugins.git

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for GitCommandValidator utility functions.
 */
class GitCommandValidatorTest {

  @Test
  fun `escapeShellArg should not escape safe arguments`() {
    val safeArgs = listOf(
        "simple",
        "path/to/file",
        "file.txt",
        "tag_v1.0.0"
    )
    
    for (arg in safeArgs) {
      assertEquals(arg, GitCommandValidator.escapeShellArg(arg), "Safe argument '$arg' should not be escaped")
    }
  }

  @Test
  fun `escapeShellArg should escape arguments with colons`() {
    // Colons should be escaped to prevent URL/network parsing issues
    assertEquals("'192.168.1.1:8080'", GitCommandValidator.escapeShellArg("192.168.1.1:8080"))
    assertEquals("'https://github.com'", GitCommandValidator.escapeShellArg("https://github.com"))
  }

  @Test
  fun `escapeShellArg should escape arguments with hyphens`() {
    // Arguments with hyphens should be escaped to prevent flag interpretation
    assertEquals("'branch-name'", GitCommandValidator.escapeShellArg("branch-name"))
    assertEquals("'-m'", GitCommandValidator.escapeShellArg("-m"))
    assertEquals("'--force'", GitCommandValidator.escapeShellArg("--force"))
  }

  @Test
  fun `escapeShellArg should escape arguments with spaces`() {
    assertEquals("'hello world'", GitCommandValidator.escapeShellArg("hello world"))
    assertEquals("'commit message'", GitCommandValidator.escapeShellArg("commit message"))
  }

  @Test
  fun `escapeShellArg should escape arguments with single quotes`() {
    assertEquals("'it'\\''s working'", GitCommandValidator.escapeShellArg("it's working"))
    assertEquals("'user'\\''s branch'", GitCommandValidator.escapeShellArg("user's branch"))
  }

  @Test
  fun `escapeShellArg should escape arguments with special characters`() {
    assertEquals("'test;rm -rf'", GitCommandValidator.escapeShellArg("test;rm -rf"))
    assertEquals("'test\$var'", GitCommandValidator.escapeShellArg("test\$var"))
    assertEquals("'test`cmd`'", GitCommandValidator.escapeShellArg("test`cmd`"))
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
      assertTrue(GitCommandValidator.isValidGitConfigKey(key), "Valid key '$key' should be accepted")
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
        "user.name\$var",
        "user-name.email",  // hyphens not allowed
        "section-name.key"  // hyphens not allowed
    )
    
    for (key in invalidKeys) {
      assertFalse(GitCommandValidator.isValidGitConfigKey(key), "Invalid key '$key' should be rejected")
    }
  }

  @Test
  fun `isValidGitConfigKey should handle multi-level keys`() {
    assertTrue(GitCommandValidator.isValidGitConfigKey("a.b"))
    assertTrue(GitCommandValidator.isValidGitConfigKey("a.b.c"))
    assertTrue(GitCommandValidator.isValidGitConfigKey("a.b.c.d"))
  }

  @Test
  fun `isValidGitCommand should accept allowed commands`() {
    val allowedCommands = listOf(
        "status", "add", "commit", "push", "pull", "fetch",
        "checkout", "branch", "log", "diff", "merge", "reset",
        "stash", "tag", "remote", "clone", "init"
    )
    
    for (cmd in allowedCommands) {
      assertTrue(GitCommandValidator.isValidGitCommand(cmd), "Command '$cmd' should be allowed")
    }
  }

  @Test
  fun `isValidGitCommand should reject disallowed commands`() {
    val disallowedCommands = listOf(
        "",
        "rm",
        "cat",
        "ls",
        "echo",
        "bash",
        "sh",
        "git",
        "unknown",
        "status; rm -rf",
        "status && ls"
    )
    
    for (cmd in disallowedCommands) {
      assertFalse(GitCommandValidator.isValidGitCommand(cmd), "Command '$cmd' should be rejected")
    }
  }
}
