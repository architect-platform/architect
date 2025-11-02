package io.github.architectplatform.plugins.pipelines

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorkflowStepTest {
    
    @Test
    fun `test WorkflowStep basic creation`() {
        val step = WorkflowStep(
            name = "build",
            task = "gradle-build"
        )
        
        assertEquals("build", step.name)
        assertEquals("gradle-build", step.task)
        assertTrue(step.args.isEmpty())
        assertTrue(step.dependsOn.isEmpty())
        assertFalse(step.continueOnError)
        assertNull(step.condition)
    }
    
    @Test
    fun `test WorkflowStep with dependencies`() {
        val step = WorkflowStep(
            name = "test",
            task = "gradle-test",
            dependsOn = listOf("build", "lint")
        )
        
        assertEquals(2, step.dependsOn.size)
        assertTrue(step.dependsOn.contains("build"))
        assertTrue(step.dependsOn.contains("lint"))
    }
    
    @Test
    fun `test WorkflowStep with args`() {
        val step = WorkflowStep(
            name = "custom",
            task = "custom-task",
            args = listOf("--flag", "value")
        )
        
        assertEquals(2, step.args.size)
        assertEquals("--flag", step.args[0])
        assertEquals("value", step.args[1])
    }
    
    @Test
    fun `test WorkflowStep with condition`() {
        val step = WorkflowStep(
            name = "conditional",
            task = "conditional-task",
            condition = "ENV == production"
        )
        
        assertEquals("ENV == production", step.condition)
    }
    
    @Test
    fun `test WorkflowStep with continueOnError`() {
        val step = WorkflowStep(
            name = "optional",
            task = "optional-task",
            continueOnError = true
        )
        
        assertTrue(step.continueOnError)
    }
}
