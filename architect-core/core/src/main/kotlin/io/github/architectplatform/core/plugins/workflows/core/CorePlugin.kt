package io.github.architectplatform.core.plugins.workflows.core

import io.github.architectplatform.api.components.workflows.core.CoreWorkflow
import io.github.architectplatform.core.plugins.workflows.WorkflowPlugin

// ------------
// CorePlugin
// ------------
class CorePlugin : WorkflowPlugin(id = "core-phases", phases = CoreWorkflow.entries)

