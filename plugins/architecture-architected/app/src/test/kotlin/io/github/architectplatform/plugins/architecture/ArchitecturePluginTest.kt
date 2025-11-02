package io.github.architectplatform.plugins.architecture

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArchitecturePluginTest {

    @Test
    fun `test plugin id and context key`() {
        val plugin = ArchitecturePlugin()
        
        assertEquals("architecture-plugin", plugin.id)
        assertEquals("architecture", plugin.contextKey)
        assertEquals(ArchitectureContext::class.java, plugin.ctxClass)
    }

    @Test
    fun `test plugin context initialization`() {
        val plugin = ArchitecturePlugin()
        
        assertNotNull(plugin.context)
        assertTrue(plugin.context.enabled)
    }

    @Test
    fun `test plugin context can be set`() {
        val plugin = ArchitecturePlugin()
        val customContext = ArchitectureContext(
            enabled = false,
            onViolation = "fail"
        )
        
        plugin.context = customContext
        
        assertFalse(plugin.context.enabled)
        assertEquals("fail", plugin.context.onViolation)
    }
}
