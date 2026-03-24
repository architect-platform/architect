package io.github.architectplatform.plugins.python

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.api.core.plugins.ArchitectPlugin
import io.github.architectplatform.api.core.tasks.TaskRegistry
import io.github.architectplatform.api.core.utils.ShellUtils

class PythonPlugin : ArchitectPlugin<PythonContext> {
  override val id = "python-plugin"
  override val contextKey: String = "python"
  override val ctxClass: Class<PythonContext> = PythonContext::class.java
  override var context: PythonContext = PythonContext()

  override fun register(registry: TaskRegistry) {
    registry.add(PythonTask(
      id = "py-install",
      phase = CoreWorkflow.INIT,
      ctx = context,
      buildCommand = { ctx, args ->
        when (ctx.tool) {
          "uv" -> "uv sync${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          "poetry" -> "poetry install${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          else -> "pip install -r requirements.txt${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
        }
      },
    ))

    registry.add(PythonTask(
      id = "py-lint",
      phase = CoreWorkflow.LINT,
      ctx = context,
      buildCommand = { ctx, args ->
        val target = if (args.isNotEmpty()) ShellUtils.escapeShellArgs(args) else ShellUtils.escapeShellArg(".")
        when (ctx.linter) {
          "ruff" -> "ruff check $target"
          "flake8" -> "flake8 $target"
          else -> "ruff check $target"
        }
      },
    ))

    registry.add(PythonTask(
      id = "py-test",
      phase = CoreWorkflow.TEST,
      ctx = context,
      buildCommand = { ctx, args ->
        when (ctx.testRunner) {
          "pytest" -> "pytest${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          "unittest" -> "python -m unittest${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          else -> "pytest${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
        }
      },
    ))

    registry.add(PythonTask(
      id = "py-build",
      phase = CoreWorkflow.BUILD,
      ctx = context,
      buildCommand = { ctx, args ->
        when (ctx.tool) {
          "uv" -> "uv build${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          "poetry" -> "poetry build${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          else -> "python -m build${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
        }
      },
    ))

    registry.add(PythonTask(
      id = "py-publish",
      phase = CoreWorkflow.PUBLISH,
      ctx = context,
      buildCommand = { ctx, args ->
        when (ctx.tool) {
          "uv" -> "uv publish${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          "poetry" -> "poetry publish${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
          else -> "twine upload dist/*${if (args.isNotEmpty()) " ${ShellUtils.escapeShellArgs(args)}" else ""}"
        }
      },
    ))
  }
}
