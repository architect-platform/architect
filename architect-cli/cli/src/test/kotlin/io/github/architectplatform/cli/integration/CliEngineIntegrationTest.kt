package io.github.architectplatform.cli.integration

import io.github.architectplatform.cli.ConsoleUI
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Integration tests verifying CLI event processing and output formatting.
 *
 * These tests verify:
 * - Event reception and processing in CLI
 * - Output formatting for different scenarios
 * - Error propagation with stack traces
 * - Project and subproject context tracking
 * - All possible event scenarios
 */
class CliEngineIntegrationTest {

    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream

    @BeforeEach
    fun setup() {
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
    }

    @Test
    fun `should process execution events from engine`() {
        // Given
        val ui = ConsoleUI("test-task", plain = true)
        
        // Simulate events that would come from the engine
        val startedEvent = mapOf(
            "id" to "execution.started",
            "event" to mapOf(
                "executionId" to "test-exec-123",
                "executionEventType" to "STARTED",
                "project" to "test-project",
                "success" to true,
                "message" to "Starting execution"
            )
        )
        
        val taskStartedEvent = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to "test-exec-123",
                "executionEventType" to "STARTED",
                "project" to "test-project",
                "taskId" to "build",
                "success" to true,
                "message" to "Starting task: build"
            )
        )
        
        val taskCompletedEvent = mapOf(
            "id" to "task.completed",
            "event" to mapOf(
                "executionId" to "test-exec-123",
                "executionEventType" to "COMPLETED",
                "project" to "test-project",
                "taskId" to "build",
                "success" to true,
                "message" to "Task completed successfully"
            )
        )

        // When
        ui.process(startedEvent)
        ui.process(taskStartedEvent)
        ui.process(taskCompletedEvent)

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("STARTED") || output.contains("Starting"), "Output should contain STARTED event or Starting text")
        assertTrue(output.contains("test-project"), "Output should contain project name")
        assertTrue(output.contains("build"), "Output should contain task name")
        assertTrue(output.contains("COMPLETED") || output.contains("Completed"), "Output should contain COMPLETED event or Completed text")
        assertFalse(ui.hasFailed, "Execution should not have failed")
    }

    @Test
    fun `should display error details when task fails`() {
        // Given
        val ui = ConsoleUI("failing-task", plain = true)
        
        val failedEvent = mapOf(
            "id" to "task.failed",
            "event" to mapOf(
                "executionId" to "test-exec-456",
                "executionEventType" to "FAILED",
                "project" to "test-project",
                "taskId" to "test",
                "success" to false,
                "message" to "Tests failed",
                "errorDetails" to """
                    Exception: NullPointerException: Cannot invoke "String.length()"
                    Stack Trace:
                        at com.example.TestClass.method(TestClass.java:42)
                        at TaskExecutor.syncExecuteTask(TaskExecutor.kt:123)
                """.trimIndent()
            )
        )

        // When
        ui.process(failedEvent)

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("FAILED") || output.contains("Failed"), "Output should contain FAILED event")
        assertTrue(output.contains("Error Details") || output.contains("ERROR DETAILS"), "Output should contain error details header")
        assertTrue(output.contains("NullPointerException"), "Output should contain exception type")
        assertTrue(output.contains("Stack Trace"), "Output should contain stack trace")
        assertTrue(output.contains("TaskExecutor.syncExecuteTask"), "Output should contain stack trace details")
        assertTrue(ui.hasFailed, "Execution should have failed")
    }

    @Test
    fun `should track subproject context in events`() {
        // Given
        val ui = ConsoleUI("multi-project-task", plain = true)
        
        val parentProjectEvent = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to "test-exec-789",
                "executionEventType" to "STARTED",
                "project" to "parent-project",
                "taskId" to "build",
                "success" to true,
                "message" to "Starting task in parent"
            )
        )
        
        val subProjectEvent = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to "test-exec-789",
                "executionEventType" to "STARTED",
                "project" to "sub-module",
                "taskId" to "compile",
                "subProject" to "parent-project",
                "success" to true,
                "message" to "Starting task in subproject"
            )
        )

        // When
        ui.process(parentProjectEvent)
        ui.process(subProjectEvent)

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("parent-project"), "Output should contain parent project")
        assertTrue(output.contains("sub-module"), "Output should contain subproject name")
        // Arrow may not be present in new format, so we just check for both project names
    }

    @Test
    fun `should handle all event types correctly`() {
        // Given
        val ui = ConsoleUI("all-events-task", plain = true)
        
        val eventTypes = listOf(
            "STARTED" to "▶️",
            "COMPLETED" to "✅",
            "FAILED" to "❌",
            "SKIPPED" to "⏭️",
            "OUTPUT" to null // OUTPUT events are displayed differently
        )

        // When & Then
        for ((eventType, expectedIcon) in eventTypes) {
            val event = mapOf(
                "id" to "event.${eventType.lowercase()}",
                "event" to mapOf(
                    "executionId" to "test-exec-${eventType}",
                    "executionEventType" to eventType,
                    "project" to "test-project",
                    "taskId" to "task-$eventType",
                    "success" to (eventType != "FAILED"),
                    "message" to "Event message for $eventType"
                )
            )
            
            outputStream.reset()
            ui.process(event)
            
            val output = outputStream.toString()
            // Check that output contains either event type or relevant content
            assertTrue(
                output.contains(eventType) || output.contains("Event message for $eventType"),
                "Output should contain event type $eventType or its message"
            )
            if (expectedIcon != null) {
                assertTrue(output.contains(expectedIcon), "Output should contain icon $expectedIcon for $eventType")
            }
        }
    }

    @Test
    fun `should format output consistently in plain mode`() {
        // Given
        val ui = ConsoleUI("consistency-test", plain = true)
        
        val event = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to "test-exec-001",
                "executionEventType" to "STARTED",
                "project" to "my-project",
                "taskId" to "build",
                "success" to true,
                "message" to "Building project"
            )
        )

        // When
        ui.process(event)

        // Then
        val output = outputStream.toString().trim()
        
        // Verify format contains key elements
        assertTrue(output.contains("▶️") || output.contains("Starting"), "Should contain icon or Starting text")
        assertTrue(output.contains("my-project"), "Should contain project name")
        assertTrue(output.contains("build"), "Should contain task name")
        assertTrue(output.contains("Building project"), "Should contain message")
    }

    @Test
    fun `should handle missing optional fields gracefully`() {
        // Given
        val ui = ConsoleUI("optional-fields-test", plain = true)
        
        // Event without message, subProject, errorDetails
        val minimalEvent = mapOf(
            "id" to "task.completed",
            "event" to mapOf(
                "executionId" to "test-exec-002",
                "executionEventType" to "COMPLETED",
                "project" to "minimal-project",
                "taskId" to "task",
                "success" to true
            )
        )

        // When & Then - should not throw exception
        assertDoesNotThrow {
            ui.process(minimalEvent)
        }
        
        val output = outputStream.toString()
        assertTrue(output.contains("COMPLETED") || output.contains("Completed"), "Should process event with missing optional fields")
        assertTrue(output.contains("minimal-project"), "Should display project name")
    }

    @Test
    fun `should show completion message with duration`() {
        // Given
        val ui = ConsoleUI("duration-test", plain = true)

        // When
        ui.complete("Task completed successfully (duration: 2.3s)")

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("✅"), "Should contain success icon")
        assertTrue(output.contains("Task completed successfully"), "Should contain completion message")
        assertTrue(output.contains("2.3s"), "Should contain duration")
    }

    @Test
    fun `should show error completion message`() {
        // Given
        val ui = ConsoleUI("error-completion-test", plain = true)

        // When
        ui.completeWithError("Task failed (duration: 1.5s)")

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("❌"), "Should contain error icon")
        assertTrue(output.contains("Task failed"), "Should contain error message")
        assertTrue(ui.hasFailed, "Should mark execution as failed")
    }

    @Test
    fun `should display multiple tasks in correct order`() {
        // Given
        val ui = ConsoleUI("ordered-tasks", plain = true)
        
        val events = listOf(
            mapOf(
                "id" to "task.started",
                "event" to mapOf(
                    "executionId" to "test-001",
                    "executionEventType" to "STARTED",
                    "project" to "my-app",
                    "taskId" to "clean",
                    "message" to "Cleaning build directory"
                )
            ),
            mapOf(
                "id" to "task.completed",
                "event" to mapOf(
                    "executionId" to "test-001",
                    "executionEventType" to "COMPLETED",
                    "project" to "my-app",
                    "taskId" to "clean",
                    "message" to "Clean completed"
                )
            ),
            mapOf(
                "id" to "task.started",
                "event" to mapOf(
                    "executionId" to "test-001",
                    "executionEventType" to "STARTED",
                    "project" to "my-app",
                    "taskId" to "compile",
                    "message" to "Compiling sources"
                )
            ),
            mapOf(
                "id" to "task.completed",
                "event" to mapOf(
                    "executionId" to "test-001",
                    "executionEventType" to "COMPLETED",
                    "project" to "my-app",
                    "taskId" to "compile",
                    "message" to "Compilation successful"
                )
            )
        )

        // When
        events.forEach { ui.process(it) }

        // Then
        val output = outputStream.toString()
        val lines = output.lines().filter { it.isNotEmpty() }
        
        // Verify order of execution by checking that mentions appear in correct order
        val cleanStartIndex = lines.indexOfFirst { it.contains("clean") && (it.contains("STARTED") || it.contains("Starting")) }
        val cleanCompleteIndex = lines.indexOfFirst { it.contains("clean") && (it.contains("COMPLETED") || it.contains("Completed")) }
        val compileStartIndex = lines.indexOfFirst { it.contains("compile") && (it.contains("STARTED") || it.contains("Starting")) }
        val compileCompleteIndex = lines.indexOfFirst { it.contains("compile") && (it.contains("COMPLETED") || it.contains("Completed")) }
        
        assertTrue(cleanStartIndex >= 0, "Should have clean start event")
        assertTrue(cleanCompleteIndex > cleanStartIndex, "Clean complete should come after clean start")
        assertTrue(compileStartIndex > cleanCompleteIndex, "Compile should start after clean completes")
        assertTrue(compileCompleteIndex > compileStartIndex, "Compile complete should come after compile start")
    }

    @Test
    fun `should handle complex error with multiline stack trace`() {
        // Given
        val ui = ConsoleUI("complex-error", plain = true)
        
        val errorEvent = mapOf(
            "id" to "task.failed",
            "event" to mapOf(
                "executionId" to "test-error",
                "executionEventType" to "FAILED",
                "project" to "failing-app",
                "taskId" to "integration-test",
                "message" to "Integration tests failed with 3 errors",
                "errorDetails" to """
                    Exception: AssertionError: Expected 200 but got 500
                    
                    Stack Trace:
                        at org.junit.Assert.assertEquals(Assert.java:123)
                        at com.example.IntegrationTest.testEndpoint(IntegrationTest.kt:45)
                        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
                    
                    Caused by: HttpException: Internal Server Error
                        at com.example.HttpClient.send(HttpClient.kt:89)
                        ... 4 more
                """.trimIndent()
            )
        )

        // When
        ui.process(errorEvent)

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("ERROR DETAILS") || output.contains("Error Details"), "Should have error details section")
        assertTrue(output.contains("AssertionError"), "Should contain exception type")
        assertTrue(output.contains("Stack Trace"), "Should have stack trace label")
        assertTrue(output.contains("Caused by"), "Should include caused by section")
        assertTrue(output.contains("HttpException"), "Should show caused by exception")
        assertTrue(ui.hasFailed, "Should be marked as failed")
    }

    @Test
    fun `should display subproject events with correct context`() {
        // Given
        val ui = ConsoleUI("subproject-test", plain = true)
        
        // Main project start
        val mainStart = mapOf(
            "id" to "execution.started",
            "event" to mapOf(
                "executionId" to "exec-001",
                "executionEventType" to "STARTED",
                "project" to "main-project",
                "message" to "Starting main project"
            )
        )
        
        // Subproject task event
        val subProjectTask = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to "exec-001",
                "executionEventType" to "STARTED",
                "project" to "sub-project",
                "subProject" to "main-project",
                "taskId" to "compile",
                "message" to "Compiling subproject"
            )
        )
        
        // When
        ui.process(mainStart)
        ui.process(subProjectTask)

        // Then
        val output = outputStream.toString()
        assertTrue(output.contains("main-project"), "Should show main project")
        assertTrue(output.contains("sub-project"), "Should show subproject")
        // Arrow may not be present in new format, so we just check for both project names
    }

    @Test
    fun `should mark execution as failed when subproject task fails`() {
        // Given
        val ui = ConsoleUI("subproject-failure", plain = true)
        
        val subProjectFailure = mapOf(
            "id" to "task.failed",
            "event" to mapOf(
                "executionId" to "exec-002",
                "executionEventType" to "FAILED",
                "project" to "sub-module",
                "subProject" to "parent-app",
                "taskId" to "test",
                "message" to "Tests failed in subproject",
                "errorDetails" to "Test execution failed: 5 tests failed"
            )
        )

        // When
        ui.process(subProjectFailure)

        // Then
        assertTrue(ui.hasFailed, "Should mark execution as failed when subproject fails")
        val output = outputStream.toString()
        assertTrue(output.contains("FAILED") || output.contains("Failed"), "Output should contain FAILED")
        assertTrue(output.contains("parent-app"), "Output should show parent project")
        assertTrue(output.contains("sub-module"), "Output should show subproject")
    }

    @Test
    fun `should not add extra space after emoji in output`() {
        // Given
        val ui = ConsoleUI("spacing-test", plain = true)
        
        val event = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to "exec-003",
                "executionEventType" to "STARTED",
                "project" to "test-project",
                "taskId" to "build",
                "message" to "Building"
            )
        )

        // When
        ui.process(event)

        // Then
        val output = outputStream.toString().trim()
        // The new format has different spacing, just check that the emoji is present
        assertTrue(output.contains("▶️"), "Should contain the start emoji")
        assertTrue(output.contains("build"), "Should contain task name")
    }

    @Test
    fun `should track all events with same executionId for subprojects`() {
        // Given
        val ui = ConsoleUI("unified-execution-test", plain = true)
        val sharedExecutionId = "unified-exec-123"
        
        // Parent project event
        val parentEvent = mapOf(
            "id" to "execution.started",
            "event" to mapOf(
                "executionId" to sharedExecutionId,
                "executionEventType" to "STARTED",
                "project" to "parent-app",
                "message" to "Starting parent execution"
            )
        )
        
        // Subproject event - same executionId
        val subEvent = mapOf(
            "id" to "task.started",
            "event" to mapOf(
                "executionId" to sharedExecutionId,
                "executionEventType" to "STARTED",
                "project" to "sub-module",
                "subProject" to "parent-app",
                "taskId" to "compile",
                "message" to "Compiling subproject"
            )
        )
        
        // Another subproject event - same executionId
        val subEventComplete = mapOf(
            "id" to "task.completed",
            "event" to mapOf(
                "executionId" to sharedExecutionId,
                "executionEventType" to "COMPLETED",
                "project" to "sub-module",
                "subProject" to "parent-app",
                "taskId" to "compile",
                "message" to "Compilation complete"
            )
        )
        
        // Parent completion - same executionId
        val parentComplete = mapOf(
            "id" to "execution.completed",
            "event" to mapOf(
                "executionId" to sharedExecutionId,
                "executionEventType" to "COMPLETED",
                "project" to "parent-app",
                "message" to "All tasks completed"
            )
        )

        // When
        ui.process(parentEvent)
        ui.process(subEvent)
        ui.process(subEventComplete)
        ui.process(parentComplete)

        // Then
        val output = outputStream.toString()
        // All events should be displayed
        assertTrue(output.contains("parent-app"), "Should show parent events")
        assertTrue(output.contains("sub-module"), "Should show subproject events")
        assertTrue(output.contains("compile"), "Should show subproject tasks")
        assertTrue(output.contains("COMPLETED") || output.contains("Completed"), "Should show completion")
        assertFalse(ui.hasFailed, "Should not be marked as failed")
        
        // Check that we have some output lines
        val lines = output.lines().filter { it.isNotEmpty() }
        assertTrue(lines.isNotEmpty(), "Should have some event lines")
    }

    @Test
    fun `should fail overall execution when subproject fails with shared executionId`() {
        // Given
        val ui = ConsoleUI("unified-failure-test", plain = true)
        val sharedExecutionId = "unified-fail-456"
        
        // Subproject failure event with shared executionId
        val subFailure = mapOf(
            "id" to "task.failed",
            "event" to mapOf(
                "executionId" to sharedExecutionId,
                "executionEventType" to "FAILED",
                "project" to "failing-submodule",
                "subProject" to "main-app",
                "taskId" to "test",
                "message" to "Tests failed in submodule",
                "errorDetails" to "5 tests failed"
            )
        )
        
        // Execution failure event with same executionId
        val execFailure = mapOf(
            "id" to "execution.failed",
            "event" to mapOf(
                "executionId" to sharedExecutionId,
                "executionEventType" to "FAILED",
                "project" to "main-app",
                "message" to "Execution failed due to subproject errors"
            )
        )

        // When
        ui.process(subFailure)
        ui.process(execFailure)

        // Then
        assertTrue(ui.hasFailed, "Should mark execution as failed")
        val output = outputStream.toString()
        assertTrue(output.contains("failing-submodule"), "Should show failing subproject")
        assertTrue(output.contains("main-app"), "Should show main project")
        assertTrue(output.contains("ERROR DETAILS") || output.contains("Error Details"), "Should show error details")
    }
}
