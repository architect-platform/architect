package io.github.architectplatform.api.core.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShellArgumentSanitizerTest {

  // --- escapeShellArg ---

  @Test
  fun `simple alphanumeric arg is wrapped in single quotes`() {
    assertEquals("'hello'", ShellArgumentSanitizer.escapeShellArg("hello"))
  }

  @Test
  fun `arg with spaces is wrapped in single quotes`() {
    assertEquals("'hello world'", ShellArgumentSanitizer.escapeShellArg("hello world"))
  }

  @Test
  fun `arg with embedded single quote uses escape sequence`() {
    assertEquals("'it'\\''s'", ShellArgumentSanitizer.escapeShellArg("it's"))
  }

  @Test
  fun `arg with shell metacharacters semicolon is neutralized`() {
    val escaped = ShellArgumentSanitizer.escapeShellArg("; rm -rf /")
    assertEquals("'; rm -rf /'", escaped)
  }

  @Test
  fun `arg with dollar sign command substitution is neutralized`() {
    assertEquals("'\$(evil)'", ShellArgumentSanitizer.escapeShellArg("\$(evil)"))
  }

  @Test
  fun `arg with backtick command substitution is neutralized`() {
    assertEquals("'`evil`'", ShellArgumentSanitizer.escapeShellArg("`evil`"))
  }

  @Test
  fun `arg with pipe is neutralized`() {
    assertEquals("'a|b'", ShellArgumentSanitizer.escapeShellArg("a|b"))
  }

  @Test
  fun `arg with newline does not break escaping`() {
    assertEquals("'line1\nline2'", ShellArgumentSanitizer.escapeShellArg("line1\nline2"))
  }

  @Test
  fun `empty arg produces empty single-quoted string`() {
    assertEquals("''", ShellArgumentSanitizer.escapeShellArg(""))
  }

  @Test
  fun `arg starting with hyphen is safely quoted`() {
    assertEquals("'--force'", ShellArgumentSanitizer.escapeShellArg("--force"))
  }

  @Test
  fun `arg with multiple single quotes is fully escaped`() {
    assertEquals("'a'\\''b'\\''c'", ShellArgumentSanitizer.escapeShellArg("a'b'c"))
  }

  // --- escapeShellArgs ---

  @Test
  fun `empty list produces empty string`() {
    assertEquals("", ShellArgumentSanitizer.escapeShellArgs(emptyList()))
  }

  @Test
  fun `multiple args are escaped and joined with spaces`() {
    val result = ShellArgumentSanitizer.escapeShellArgs(listOf("foo", "bar baz", "it's"))
    assertEquals("'foo' 'bar baz' 'it'\\''s'", result)
  }

  // --- requireSafeIdentifier ---

  @Test
  fun `valid alphanumeric identifier returns unchanged`() {
    assertEquals("release", ShellArgumentSanitizer.requireSafeIdentifier("release", "profile"))
  }

  @Test
  fun `identifier with hyphen and dot is accepted`() {
    assertEquals("x86_64-unknown-linux-gnu", ShellArgumentSanitizer.requireSafeIdentifier("x86_64-unknown-linux-gnu", "target"))
  }

  @Test
  fun `identifier with semicolon throws`() {
    assertThrows(IllegalArgumentException::class.java) {
      ShellArgumentSanitizer.requireSafeIdentifier("release; rm -rf /", "profile")
    }
  }

  @Test
  fun `identifier with space throws`() {
    assertThrows(IllegalArgumentException::class.java) {
      ShellArgumentSanitizer.requireSafeIdentifier("my profile", "profile")
    }
  }

  @Test
  fun `identifier with dollar sign throws`() {
    assertThrows(IllegalArgumentException::class.java) {
      ShellArgumentSanitizer.requireSafeIdentifier("\$evil", "profile")
    }
  }
}
