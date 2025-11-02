package io.github.architectplatform.plugins.pipelines

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorkflowDefinitionTest {
    
    @Test
    fun `test WorkflowDefinition basic creation`() {
        val workflow = WorkflowDefinition(
            name = "test",
            description = "Test workflow"
        )
        
        assertEquals("test", workflow.name)
        assertEquals("Test workflow", workflow.description)
        assertNull(workflow.extends)
        assertTrue(workflow.steps.isEmpty())
        assertTrue(workflow.env.isEmpty())
    }
    
    @Test
    fun `test WorkflowDefinition with steps`() {
        val steps = listOf(
            WorkflowStep(name = "build", task = "gradle-build"),
            WorkflowStep(name = "test", task = "gradle-test", dependsOn = listOf("build"))
        )
        
        val workflow = WorkflowDefinition(
            name = "ci",
            steps = steps
        )
        
        assertEquals(2, workflow.steps.size)
        assertEquals("build", workflow.steps[0].name)
        assertEquals("test", workflow.steps[1].name)
        assertEquals(listOf("build"), workflow.steps[1].dependsOn)
    }
    
    @Test
    fun `test WorkflowDefinition with template extension`() {
        val workflow = WorkflowDefinition(
            name = "custom-ci",
            extends = "ci-standard",
            steps = listOf(
                WorkflowStep(name = "deploy", task = "deploy-task")
            )
        )
        
        assertEquals("ci-standard", workflow.extends)
        assertEquals(1, workflow.steps.size)
    }
}
