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
        assertTrue(output.contains("STARTED"), "Output should contain STARTED event")
        assertTrue(output.contains("test-project"), "Output should contain project name")
        assertTrue(output.contains("build"), "Output should contain task name")
        assertTrue(output.contains("COMPLETED"), "Output should contain COMPLETED event")
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
        assertTrue(output.contains("FAILED"), "Output should contain FAILED event")
        assertTrue(output.contains("ERROR DETAILS"), "Output should contain error details header")
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
        assertTrue(output.contains("→"), "Output should contain arrow indicating subproject relationship")
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
            "OUTPUT" to "📝"
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
            assertTrue(output.contains(eventType), "Output should contain event type $eventType")
            assertTrue(output.contains(expectedIcon), "Output should contain icon $expectedIcon for $eventType")
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
        
        // Verify format: icon EVENT_TYPE [project] Task: taskname message
        assertTrue(output.contains("▶️"), "Should contain icon")
        assertTrue(output.contains("STARTED"), "Should contain event type")
        assertTrue(output.contains("[my-project]"), "Should contain project in brackets")
        assertTrue(output.contains("Task: build"), "Should contain task info")
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
        assertTrue(output.contains("COMPLETED"), "Should process event with missing optional fields")
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
        
        // Verify order of execution
        val cleanStartIndex = lines.indexOfFirst { it.contains("clean") && it.contains("STARTED") }
        val cleanCompleteIndex = lines.indexOfFirst { it.contains("clean") && it.contains("COMPLETED") }
        val compileStartIndex = lines.indexOfFirst { it.contains("compile") && it.contains("STARTED") }
        val compileCompleteIndex = lines.indexOfFirst { it.contains("compile") && it.contains("COMPLETED") }
        
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
        assertTrue(output.contains("ERROR DETAILS"), "Should have error details section")
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
        assertTrue(output.contains("→"), "Should show arrow between parent and subproject")
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
        assertTrue(output.contains("FAILED"), "Output should contain FAILED")
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
        // Check that emoji is directly followed by event type (no double space)
        assertTrue(output.contains("▶️STARTED"), "Should not have space between emoji and event type")
        assertFalse(output.contains("▶️ STARTED"), "Should not have extra space after emoji")
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
        assertTrue(output.contains("COMPLETED"), "Should show completion")
        assertFalse(ui.hasFailed, "Should not be marked as failed")
        
        // Count the number of lines - should have all 4 events
        val lines = output.lines().filter { it.isNotEmpty() }
        assertTrue(lines.size >= 4, "Should have at least 4 event lines")
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
        assertTrue(output.contains("ERROR DETAILS"), "Should show error details")
    }
}
