package io.github.architectplatform.engine.core.project.infra

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode

/**
 * Parses YAML and builds a map of dot-notation key paths to 1-based line numbers.
 *
 * Example: given `project:\n  name: foo`, the map will contain
 * `"project" → 1`, `"project.name" → 2`.
 */
object YamlLineTracker {

  /**
   * @return map of key-path → 1-based line number
   */
  fun trackLines(yamlContent: String): Map<String, Int> {
    val yaml = Yaml()
    val root: Node = yaml.compose(java.io.StringReader(yamlContent)) ?: return emptyMap()
    val result = mutableMapOf<String, Int>()
    walk(root, "", result)
    return result
  }

  private fun walk(node: Node, prefix: String, out: MutableMap<String, Int>) {
    when (node) {
      is MappingNode -> {
        for (tuple in node.value) {
          val keyNode = tuple.keyNode
          val keyName = (keyNode as? ScalarNode)?.value ?: continue
          val fullPath = if (prefix.isEmpty()) keyName else "$prefix.$keyName"
          // Line numbers in SnakeYAML marks are 0-based
          out[fullPath] = keyNode.startMark.line + 1
          walk(tuple.valueNode, fullPath, out)
        }
      }
      is SequenceNode -> {
        for ((index, item) in node.value.withIndex()) {
          val itemPath = "$prefix.$index"
          out[itemPath] = item.startMark.line + 1
          walk(item, itemPath, out)
        }
      }
      // ScalarNode — leaf value, nothing to recurse into
      else -> {}
    }
  }
}
