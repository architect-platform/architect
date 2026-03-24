package io.github.architectplatform.api.core.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShellUtilsTest {

  // --- escapeShellArg ---

  @Test
  fun `simple alphanumeric arg is wrapped in single quotes`() {
    assertEquals("'hello'", ShellUtils.escapeShellArg("hello"))
  }

  @Test
  fun `arg with spaces is wrapped in single quotes`() {
    assertEquals("'hello world'", ShellUtils.escapeShellArg("hello world"))
  }

  @Test
  fun `arg with embedded single quote uses escape sequence`() {
    assertEquals("'it'\\''s'", ShellUtils.escapeShellArg("it's"))
  }

  @Test
  fun `arg with shell metacharacters semicolon is neutralized`() {
    val escaped = ShellUtils.escapeShellArg("; rm -rf /")
    assertEquals("'; rm -rf /'", escaped)
  }

  @Test
  fun `arg with dollar sign command substitution is neutralized`() {
    assertEquals("'\$(evil)'", ShellUtils.escapeShellArg("\$(evil)"))
  }

  @Test
  fun `arg with backtick command substitution is neutralized`() {
    assertEquals("'`evil`'", ShellUtils.escapeShellArg("`evil`"))
  }

  @Test
  fun `arg with pipe is neutralized`() {
    assertEquals("'a|b'", ShellUtils.escapeShellArg("a|b"))
  }

  @Test
  fun `arg with newline does not break escaping`() {
    assertEquals("'line1\nline2'", ShellUtils.escapeShellArg("line1\nline2"))
  }

  @Test
  fun `empty arg produces empty single-quoted string`() {
    assertEquals("''", ShellUtils.escapeShellArg(""))
  }

  @Test
  fun `arg starting with hyphen is safely quoted`() {
    assertEquals("'--force'", ShellUtils.escapeShellArg("--force"))
  }

  @Test
  fun `arg with multiple single quotes is fully escaped`() {
    assertEquals("'a'\\''b'\\''c'", ShellUtils.escapeShellArg("a'b'c"))
  }

  // --- escapeShellArgs ---

  @Test
  fun `empty list produces empty string`() {
    assertEquals("", ShellUtils.escapeShellArgs(emptyList()))
  }

  @Test
  fun `multiple args are escaped and joined with spaces`() {
    val result = ShellUtils.escapeShellArgs(listOf("foo", "bar baz", "it's"))
    assertEquals("'foo' 'bar baz' 'it'\\''s'", result)
  }

  // --- requireSafeIdentifier ---

  @Test
  fun `valid alphanumeric identifier returns unchanged`() {
    assertEquals("release", ShellUtils.requireSafeIdentifier("release", "profile"))
  }

  @Test
  fun `identifier with hyphen and dot is accepted`() {
    assertEquals("x86_64-unknown-linux-gnu", ShellUtils.requireSafeIdentifier("x86_64-unknown-linux-gnu", "target"))
  }

  @Test
  fun `identifier with semicolon throws`() {
    assertThrows(IllegalArgumentException::class.java) {
      ShellUtils.requireSafeIdentifier("release; rm -rf /", "profile")
    }
  }

  @Test
  fun `identifier with space throws`() {
    assertThrows(IllegalArgumentException::class.java) {
      ShellUtils.requireSafeIdentifier("my profile", "profile")
    }
  }

  @Test
  fun `identifier with dollar sign throws`() {
    assertThrows(IllegalArgumentException::class.java) {
      ShellUtils.requireSafeIdentifier("\$evil", "profile")
    }
  }
}
