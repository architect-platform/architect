package io.github.architectplatform.intellij

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.Icon
import com.intellij.icons.AllIcons
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import java.awt.BorderLayout

class ArchitectRunConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = "Architect Task"
  override fun getConfigurationTypeDescription(): String = "Run an Architect task"
  override fun getIcon(): Icon = AllIcons.Actions.Execute
  override fun getId(): String = "ArchitectRunConfiguration"

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(ArchitectConfigurationFactory(this))
  }
}

class ArchitectConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return ArchitectRunConfiguration(project, this, "Architect Task")
  }

  override fun getId(): String = "ArchitectConfigurationFactory"
}

class ArchitectRunConfiguration(
  project: Project,
  factory: ConfigurationFactory,
  name: String
) : RunConfigurationBase<RunConfigurationOptions>(project, factory, name) {

  var taskId: String = ""

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return ArchitectSettingsEditor()
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return object : CommandLineState(environment) {
      override fun startProcess(): ProcessHandler {
        val cmd = GeneralCommandLine("architect", "--embedded", taskId)
        cmd.withWorkDirectory(project.basePath)
        return ProcessHandlerFactory.getInstance().createProcessHandler(cmd)
      }
    }
  }
}

class ArchitectSettingsEditor : SettingsEditor<ArchitectRunConfiguration>() {
  private val taskField = JTextField()

  override fun resetEditorFrom(s: ArchitectRunConfiguration) {
    taskField.text = s.taskId
  }

  override fun applyEditorTo(s: ArchitectRunConfiguration) {
    s.taskId = taskField.text
  }

  override fun createEditor(): JComponent {
    val panel = JPanel(BorderLayout())
    panel.add(taskField, BorderLayout.CENTER)
    return panel
  }
}
