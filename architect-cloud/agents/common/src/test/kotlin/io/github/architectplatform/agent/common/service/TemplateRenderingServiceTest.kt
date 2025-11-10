package io.github.architectplatform.agent.common.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for TemplateRenderingService.
 * Tests Jinja2 template rendering functionality.
 */
class TemplateRenderingServiceTest {
    
    private val service = TemplateRenderingService()
    
    @Test
    fun `renderTemplate should render simple template`() {
        val template = "Hello {{ name }}!"
        val variables = mapOf("name" to "World")
        
        val result = service.renderTemplate(template, variables)
        
        assertEquals("Hello World!", result)
    }
    
    @Test
    fun `renderTemplate should render template with multiple variables`() {
        val template = "{{ greeting }} {{ name }}, you are {{ age }} years old."
        val variables = mapOf(
            "greeting" to "Hello",
            "name" to "Alice",
            "age" to 30
        )
        
        val result = service.renderTemplate(template, variables)
        
        assertEquals("Hello Alice, you are 30 years old.", result)
    }
    
    @Test
    fun `renderTemplate should render template with conditional`() {
        val template = """
            {% if enabled %}
            Feature is enabled
            {% else %}
            Feature is disabled
            {% endif %}
        """.trimIndent()
        
        val resultEnabled = service.renderTemplate(template, mapOf("enabled" to true))
        val resultDisabled = service.renderTemplate(template, mapOf("enabled" to false))
        
        assertTrue(resultEnabled.contains("Feature is enabled"))
        assertTrue(resultDisabled.contains("Feature is disabled"))
    }
    
    @Test
    fun `renderTemplate should render template with loop`() {
        val template = """
            Items:
            {% for item in items %}
            - {{ item }}
            {% endfor %}
        """.trimIndent()
        
        val variables = mapOf("items" to listOf("apple", "banana", "cherry"))
        
        val result = service.renderTemplate(template, variables)
        
        assertTrue(result.contains("- apple"))
        assertTrue(result.contains("- banana"))
        assertTrue(result.contains("- cherry"))
    }
    
    @Test
    fun `renderTemplate should render nested variables`() {
        val template = "{{ user.name }} ({{ user.email }})"
        val variables = mapOf(
            "user" to mapOf(
                "name" to "John Doe",
                "email" to "john@example.com"
            )
        )
        
        val result = service.renderTemplate(template, variables)
        
        assertEquals("John Doe (john@example.com)", result)
    }
    
    @Test
    fun `renderTemplate should throw exception for unknown token when configured`() {
        val template = "{{ unknown_var }}"
        val variables = emptyMap<String, Any>()
        
        assertThrows<TemplateRenderingException> {
            service.renderTemplate(template, variables)
        }
    }
    
    @Test
    fun `renderTemplates should render multiple templates`() {
        val templates = listOf(
            "Hello {{ name }}",
            "Goodbye {{ name }}"
        )
        val variables = mapOf("name" to "World")
        
        val results = service.renderTemplates(templates, variables)
        
        assertEquals(2, results.size)
        assertEquals("Hello World", results[0])
        assertEquals("Goodbye World", results[1])
    }
    
    @Test
    fun `renderAndMerge should merge rendered templates with separator`() {
        val templates = listOf(
            "apiVersion: v1",
            "kind: Service"
        )
        val variables = emptyMap<String, Any>()
        
        val result = service.renderAndMerge(templates, variables, "\n---\n")
        
        assertTrue(result.contains("apiVersion: v1"))
        assertTrue(result.contains("---"))
        assertTrue(result.contains("kind: Service"))
    }
    
    @Test
    fun `renderAndMerge should use custom separator`() {
        val templates = listOf("Part 1", "Part 2", "Part 3")
        val variables = emptyMap<String, Any>()
        
        val result = service.renderAndMerge(templates, variables, " | ")
        
        assertEquals("Part 1 | Part 2 | Part 3", result)
    }
    
    @Test
    fun `validateTemplate should return true for valid template`() {
        val template = "Hello {{ name }}"
        
        val isValid = service.validateTemplate(template)
        
        assertTrue(isValid)
    }
    
    @Test
    fun `validateTemplate should return false for invalid template`() {
        val template = "Hello {{ unclosed"
        
        val isValid = service.validateTemplate(template)
        
        assertFalse(isValid)
    }
}
