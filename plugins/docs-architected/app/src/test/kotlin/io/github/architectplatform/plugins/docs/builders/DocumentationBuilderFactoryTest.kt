package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.plugins.docs.dto.BuildContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Tests for DocumentationBuilderFactory.
 */
class DocumentationBuilderFactoryTest {

    private val mockExecutor = mock(CommandExecutor::class.java)

    @Test
    fun `createBuilder should return MkDocsBuilder for mkdocs framework`() {
        val context = BuildContext(framework = "mkdocs")
        val builder = DocumentationBuilderFactory.createBuilder(context, mockExecutor)
        assertTrue(builder is MkDocsBuilder)
        assertEquals("MkDocs", builder.getName())
    }

    @Test
    fun `createBuilder should return DocusaurusBuilder for docusaurus framework`() {
        val context = BuildContext(framework = "docusaurus")
        val builder = DocumentationBuilderFactory.createBuilder(context, mockExecutor)
        assertTrue(builder is DocusaurusBuilder)
        assertEquals("Docusaurus", builder.getName())
    }

    @Test
    fun `createBuilder should return VuePressBuilder for vuepress framework`() {
        val context = BuildContext(framework = "vuepress")
        val builder = DocumentationBuilderFactory.createBuilder(context, mockExecutor)
        assertTrue(builder is VuePressBuilder)
        assertEquals("VuePress", builder.getName())
    }

    @Test
    fun `createBuilder should be case insensitive`() {
        val context1 = BuildContext(framework = "MkDocs")
        val builder1 = DocumentationBuilderFactory.createBuilder(context1, mockExecutor)
        assertTrue(builder1 is MkDocsBuilder)

        val context2 = BuildContext(framework = "DOCUSAURUS")
        val builder2 = DocumentationBuilderFactory.createBuilder(context2, mockExecutor)
        assertTrue(builder2 is DocusaurusBuilder)
    }

    @Test
    fun `createBuilder should throw IllegalArgumentException for unsupported framework`() {
        val context = BuildContext(framework = "unsupported")
        assertThrows(IllegalArgumentException::class.java) {
            DocumentationBuilderFactory.createBuilder(context, mockExecutor)
        }
    }

    @Test
    fun `createBuilder should throw IllegalArgumentException for empty framework`() {
        val context = BuildContext(framework = "")
        assertThrows(IllegalArgumentException::class.java) {
            DocumentationBuilderFactory.createBuilder(context, mockExecutor)
        }
    }
}
