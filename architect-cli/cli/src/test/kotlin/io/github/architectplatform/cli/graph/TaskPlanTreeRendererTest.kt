package io.github.architectplatform.cli.graph

import io.github.architectplatform.cli.dto.TaskPlanDTO
import io.github.architectplatform.cli.dto.TaskPlanStepDTO
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TaskPlanTreeRendererTest {

  private val renderer = TaskPlanTreeRenderer()

  @Test
  fun `renders dependency tree for linear chain`() {
    val plan = TaskPlanDTO(
      task = "deploy",
      project = "my-project",
      totalSteps = 3,
      parallelBatches = 3,
      steps = listOf(
        TaskPlanStepDTO(
          id = "build", description = "Compile sources",
          phase = "BUILD", depends = emptyList(), batch = 0,
        ),
        TaskPlanStepDTO(id = "test", description = "Run tests", phase = "TEST", depends = listOf("build"), batch = 1),
        TaskPlanStepDTO(
          id = "deploy", description = "Ship release",
          phase = "PUBLISH", depends = listOf("test"), batch = 2,
        ),
      ),
    )

    val output = renderer.render(plan)

    assertTrue(output.contains("🌳 Dependency Tree: deploy"))
    assertTrue(output.contains("📦 Project: my-project"))
    assertTrue(output.contains("3 task(s) across 3 parallel batch(es)"))
    // Root should be deploy
    assertTrue(output.contains("deploy [PUBLISH] — Ship release (batch 2)"))
    // test is a child of deploy
    assertTrue(output.contains("└── test [TEST] — Run tests (batch 1)"))
    // build is a child of test
    assertTrue(output.contains("└── build [BUILD] — Compile sources (batch 0)"))
  }

  @Test
  fun `renders single task with no dependencies`() {
    val plan = TaskPlanDTO(
      task = "lint",
      project = "api",
      totalSteps = 1,
      parallelBatches = 1,
      steps = listOf(
        TaskPlanStepDTO(
          id = "lint", description = "Check code style",
          phase = "LINT", depends = emptyList(), batch = 0,
        ),
      ),
    )

    val output = renderer.render(plan)

    assertTrue(output.contains("lint [LINT] — Check code style (batch 0)"))
    assertFalse(output.contains("└──"))
  }

  @Test
  fun `renders diamond dependency graph`() {
    val plan = TaskPlanDTO(
      task = "package",
      project = "app",
      totalSteps = 4,
      parallelBatches = 3,
      steps = listOf(
        TaskPlanStepDTO(id = "init", description = "Initialize", phase = null, depends = emptyList(), batch = 0),
        TaskPlanStepDTO(
          id = "compile", description = "Compile code",
          phase = "BUILD", depends = listOf("init"), batch = 1,
        ),
        TaskPlanStepDTO(id = "lint", description = "Lint code", phase = "LINT", depends = listOf("init"), batch = 1),
        TaskPlanStepDTO(
          id = "package", description = "Package artifact",
          phase = "PUBLISH", depends = listOf("compile", "lint"), batch = 2,
        ),
      ),
    )

    val output = renderer.render(plan)

    assertTrue(output.contains("package [PUBLISH] — Package artifact (batch 2)"))
    assertTrue(output.contains("├── compile [BUILD] — Compile code (batch 1)"))
    assertTrue(output.contains("└── lint [LINT] — Lint code (batch 1)"))
    // init appears as dependency of both compile and lint
    assertTrue(output.contains("init"))
  }
}
