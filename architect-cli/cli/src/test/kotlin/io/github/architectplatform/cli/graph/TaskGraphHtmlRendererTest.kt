package io.github.architectplatform.cli.graph

import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskGraphHtmlRendererTest {
  private val renderer = TaskGraphHtmlRenderer()

  @Test
  fun `renders mermaid html graph from task plans`() {
    val html = renderer.render(
      graphName = "sample-project",
      plans = listOf(
        TaskPlanDTO(
          task = "test",
          project = "sample-project",
          totalSteps = 2,
          parallelBatches = 2,
          steps = listOf(
            TaskPlanStepDTO(
              id = "build",
              description = "Compile sources",
              phase = "BUILD",
              depends = emptyList(),
              batch = 0,
            ),
            TaskPlanStepDTO(
              id = "test",
              description = "Run tests",
              phase = "TEST",
              depends = listOf("build"),
              batch = 1,
            ),
          ),
        ),
      ),
    )

    assertTrue(html.contains("<pre class=\"mermaid\">"))
    assertTrue(html.contains("flowchart LR"))
    assertTrue(html.contains("task_0 --> task_1") || html.contains("task_1 --> task_0"))
    assertTrue(html.contains("Compile sources"))
    assertTrue(html.contains("mermaid.initialize"))
  }
}
