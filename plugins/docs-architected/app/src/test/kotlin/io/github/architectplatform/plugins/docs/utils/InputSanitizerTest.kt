package io.github.architectplatform.plugins.docs.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for InputSanitizer.
 */
class InputSanitizerTest {

    @Test
    fun `isValidDomain should accept valid domains`() {
        assertTrue(InputSanitizer.isValidDomain("example.com"))
        assertTrue(InputSanitizer.isValidDomain("docs.example.com"))
        assertTrue(InputSanitizer.isValidDomain("my-site.github.io"))
        assertTrue(InputSanitizer.isValidDomain("sub.domain.example.com"))
    }

    @Test
    fun `isValidDomain should reject invalid domains`() {
        assertFalse(InputSanitizer.isValidDomain("-example.com"))
        assertFalse(InputSanitizer.isValidDomain(".example.com"))
        assertFalse(InputSanitizer.isValidDomain(""))
        assertFalse(InputSanitizer.isValidDomain("a")) // Single character domain
        assertFalse(InputSanitizer.isValidDomain("ab")) // Two character domain without TLD
    }

    @Test
    fun `isValidDomain should reject domain with special characters`() {
        assertFalse(InputSanitizer.isValidDomain("example.com;ls"))
        assertFalse(InputSanitizer.isValidDomain("example.com/path"))
        assertFalse(InputSanitizer.isValidDomain("example.com:8080"))
    }

    @Test
    fun `sanitizePath should remove absolute path indicators`() {
        assertEquals("home/user/docs", InputSanitizer.sanitizePath("/home/user/docs"))
    }

    @Test
    fun `sanitizePath should remove parent directory references`() {
        assertEquals("docs/", InputSanitizer.sanitizePath("../docs/"))
        assertEquals("docs/docs/index.md", InputSanitizer.sanitizePath("docs/../docs/index.md"))
    }

    @Test
    fun `sanitizePath should remove special characters`() {
        assertEquals("docs/index.mdrm-rf/", InputSanitizer.sanitizePath("docs/index.md;rm -rf /"))
        assertEquals("docs/file.txtechohacked", InputSanitizer.sanitizePath("docs/file.txt && echo hacked"))
    }

    @Test
    fun `sanitizeBranch should allow valid branch names`() {
        assertEquals("main", InputSanitizer.sanitizeBranch("main"))
        assertEquals("feature/my-feature", InputSanitizer.sanitizeBranch("feature/my-feature"))
        assertEquals("gh-pages", InputSanitizer.sanitizeBranch("gh-pages"))
    }

    @Test
    fun `sanitizeBranch should remove special characters`() {
        assertEquals("mainrm-rf", InputSanitizer.sanitizeBranch("main;rm -rf /"))
        assertEquals("featureechohacked", InputSanitizer.sanitizeBranch("feature && echo hacked"))
    }

    @Test
    fun `sanitizeVersion should allow valid version strings`() {
        assertEquals("1.5.3", InputSanitizer.sanitizeVersion("1.5.3"))
        assertEquals("2.0.0-beta.1", InputSanitizer.sanitizeVersion("2.0.0-beta.1"))
        assertEquals("v1.2.3", InputSanitizer.sanitizeVersion("v1.2.3"))
    }

    @Test
    fun `sanitizeVersion should remove special characters`() {
        assertEquals("1.5.3rm-rf", InputSanitizer.sanitizeVersion("1.5.3;rm -rf /"))
        assertEquals("2.0.0echohacked", InputSanitizer.sanitizeVersion("2.0.0 && echo hacked"))
    }
}
