package io.github.architectplatform.plugins.docs.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for SecurityUtils.
 */
class SecurityUtilsTest {

    @Test
    fun `isValidDomain should accept valid domains`() {
        assertTrue(SecurityUtils.isValidDomain("example.com"))
        assertTrue(SecurityUtils.isValidDomain("docs.example.com"))
        assertTrue(SecurityUtils.isValidDomain("my-site.github.io"))
        assertTrue(SecurityUtils.isValidDomain("sub.domain.example.com"))
    }

    @Test
    fun `isValidDomain should reject invalid domains`() {
        assertFalse(SecurityUtils.isValidDomain("-example.com"))
        assertFalse(SecurityUtils.isValidDomain(".example.com"))
        assertFalse(SecurityUtils.isValidDomain(""))
        assertFalse(SecurityUtils.isValidDomain("a")) // Single character domain
        assertFalse(SecurityUtils.isValidDomain("ab")) // Two character domain without TLD
    }

    @Test
    fun `isValidDomain should reject domain with special characters`() {
        assertFalse(SecurityUtils.isValidDomain("example.com;ls"))
        assertFalse(SecurityUtils.isValidDomain("example.com/path"))
        assertFalse(SecurityUtils.isValidDomain("example.com:8080"))
    }

    @Test
    fun `sanitizePath should remove absolute path indicators`() {
        assertEquals("home/user/docs", SecurityUtils.sanitizePath("/home/user/docs"))
    }

    @Test
    fun `sanitizePath should remove parent directory references`() {
        assertEquals("docs/", SecurityUtils.sanitizePath("../docs/"))
        assertEquals("docs/index.md", SecurityUtils.sanitizePath("docs/../docs/index.md"))
    }

    @Test
    fun `sanitizePath should remove special characters`() {
        assertEquals("docs/index.md", SecurityUtils.sanitizePath("docs/index.md;rm -rf /"))
        assertEquals("docs/file.txt", SecurityUtils.sanitizePath("docs/file.txt && echo hacked"))
    }

    @Test
    fun `sanitizeBranch should allow valid branch names`() {
        assertEquals("main", SecurityUtils.sanitizeBranch("main"))
        assertEquals("feature/my-feature", SecurityUtils.sanitizeBranch("feature/my-feature"))
        assertEquals("gh-pages", SecurityUtils.sanitizeBranch("gh-pages"))
    }

    @Test
    fun `sanitizeBranch should remove special characters`() {
        assertEquals("mainrm-rf", SecurityUtils.sanitizeBranch("main;rm -rf /"))
        assertEquals("feature", SecurityUtils.sanitizeBranch("feature && echo hacked"))
    }

    @Test
    fun `sanitizeVersion should allow valid version strings`() {
        assertEquals("1.5.3", SecurityUtils.sanitizeVersion("1.5.3"))
        assertEquals("2.0.0-beta.1", SecurityUtils.sanitizeVersion("2.0.0-beta.1"))
        assertEquals("v1.2.3", SecurityUtils.sanitizeVersion("v1.2.3"))
    }

    @Test
    fun `sanitizeVersion should remove special characters`() {
        assertEquals("1.5.3", SecurityUtils.sanitizeVersion("1.5.3;rm -rf /"))
        assertEquals("2.0.0", SecurityUtils.sanitizeVersion("2.0.0 && echo hacked"))
    }
}
