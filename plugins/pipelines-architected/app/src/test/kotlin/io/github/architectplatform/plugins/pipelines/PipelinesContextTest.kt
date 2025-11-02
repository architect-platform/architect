package io.github.architectplatform.plugins.pipelines

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PipelinesContextTest {
    
    @Test
    fun `test PipelinesContext default values`() {
        val context = PipelinesContext()
        
        assertTrue(context.enabled)
        assertTrue(context.workflows.isEmpty())
    }
    
    @Test
    fun `test PipelinesContext with workflows`() {
        val workflow = WorkflowDefinition(
            name = "test-workflow",
            description = "Test workflow",
            steps = listOf(
                WorkflowStep(name = "step1", task = "task1")
            )
        )
        
        val context = PipelinesContext(
            workflows = listOf(workflow),
            enabled = true
        )
        
        assertEquals(1, context.workflows.size)
        assertEquals("test-workflow", context.workflows[0].name)
    }
}
