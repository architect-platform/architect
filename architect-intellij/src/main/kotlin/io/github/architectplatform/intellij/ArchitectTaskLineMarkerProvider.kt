package io.github.architectplatform.intellij

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

class ArchitectTaskLineMarkerProvider : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    // Only operate on leaf elements that are keys in YAML key-value pairs
    val parent = element.parent ?: return null
    if (parent !is YAMLKeyValue) return null
    if (element != parent.key?.firstChild) return null

    // Check if this key-value is directly under the "tasks" mapping
    val grandparent = parent.parent ?: return null
    if (grandparent !is YAMLMapping) return null
    val greatGrandparent = grandparent.parent ?: return null
    if (greatGrandparent !is YAMLKeyValue) return null
    if (greatGrandparent.keyText != "tasks") return null

    // Check it's in an architect.yml file
    val file = element.containingFile?.virtualFile ?: return null
    if (file.name != "architect.yml" && file.name != "architect.yaml") return null

    val taskId = parent.keyText
    val actions = ExecutorAction.getActions(0)
    return Info(
      AllIcons.RunConfigurations.TestState.Run,
      { "Run task '$taskId'" },
      *actions
    )
  }
}
