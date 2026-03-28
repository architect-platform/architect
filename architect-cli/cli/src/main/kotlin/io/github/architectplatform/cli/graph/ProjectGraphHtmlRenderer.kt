package io.github.architectplatform.cli.graph

import io.github.architectplatform.core.project.domain.ProjectDependencyGraph
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class ProjectGraphHtmlRenderer {

  fun render(
    graphName: String,
    graph: ProjectDependencyGraph,
    affectedProjects: Set<String> = emptySet(),
  ): String {
    val projects = graph.projects.sorted()
    val cycles = graph.detectCycles()
    val cycleNodes = cycles.flatten().toSet()
    val cycleEdgePairs = cycles
      .flatMap { cycle -> cycle.zipWithNext() }
      .map { (a, b) -> setOf(a, b) }
      .toSet()
    val sharedDeps = graph.sharedDependencies()

    val nodesJson = projects.joinToString(",\n        ") { project ->
      val isCycle = project in cycleNodes
      val isAffected = project in affectedProjects
      val isShared = project in sharedDeps
      val color = when {
        isCycle -> """{ background: "#e53935", border: "#b71c1c", highlight: { background: "#ef9a9a", border: "#b71c1c" } }"""
        isAffected -> """{ background: "#f57c00", border: "#e65100", highlight: { background: "#ffcc80", border: "#e65100" } }"""
        isShared -> """{ background: "#1565c0", border: "#0d47a1", highlight: { background: "#90caf9", border: "#0d47a1" } }"""
        else -> """{ background: "#2e7d32", border: "#1b5e20", highlight: { background: "#a5d6a7", border: "#1b5e20" } }"""
      }
      val tooltipLines = mutableListOf(escapeJs(project))
      if (isCycle) tooltipLines += "in cycle"
      if (isAffected) tooltipLines += "affected"
      if (isShared) tooltipLines += "shared dep"
      val deps = graph.dependenciesOf(project)
      if (deps.isNotEmpty()) tooltipLines += "Deps: ${deps.sorted().joinToString(", ")}"
      val dependents = graph.dependentsOf(project)
      if (dependents.isNotEmpty()) tooltipLines += "Used by: ${dependents.sorted().joinToString(", ")}"
      val title = tooltipLines.joinToString("\\n")
      """{ id: "${escapeJs(project)}", label: "${escapeJs(project)}", color: $color, font: { color: "#ffffff", size: 14 }, title: "$title" }"""
    }

    val edgesJson = graph.dependencies
      .flatMap { (project, deps) -> deps.map { dep -> dep to project } }
      .distinct()
      .sortedWith(compareBy({ it.first }, { it.second }))
      .joinToString(",\n        ") { (dep, project) ->
        val isCycleEdge = setOf(dep, project) in cycleEdgePairs
        val edgeColor = if (isCycleEdge) """{ color: "#e53935", highlight: "#e53935" }""" else """{ color: "#555", highlight: "#aaa" }"""
        val width = if (isCycleEdge) 3 else 1
        """{ from: "${escapeJs(dep)}", to: "${escapeJs(project)}", color: $edgeColor, arrows: "to", width: $width }"""
      }

    val legendItems = mutableListOf(
      """<span class="legend-item"><span class="dot" style="background:#2e7d32"></span>Normal</span>""",
      """<span class="legend-item"><span class="dot" style="background:#1565c0"></span>Shared dep</span>""",
    )
    if (affectedProjects.isNotEmpty()) legendItems += """<span class="legend-item"><span class="dot" style="background:#f57c00"></span>Affected</span>"""
    if (cycleNodes.isNotEmpty()) legendItems += """<span class="legend-item"><span class="dot" style="background:#e53935"></span>In cycle</span>"""
    val legendHtml = """<div class="legend">${legendItems.joinToString("")}</div>"""

    val cycleWarning = if (cycles.isNotEmpty()) {
      val cycleList = cycles.joinToString("") { cycle ->
        "<li>${cycle.joinToString(" -> ") { "<code>${escapeHtml(it)}</code>" }}</li>"
      }
      """<div class="cycle-warning">Circular dependencies detected:<ul>$cycleList</ul></div>"""
    } else ""

    val statsHtml = buildString {
      append("""<div class="stats">""")
      append("""<span>${projects.size} project(s)</span>""")
      append("""<span>${graph.dependencies.values.sumOf { it.size }} dep(s)</span>""")
      if (sharedDeps.isNotEmpty()) append("""<span>${sharedDeps.size} shared</span>""")
      if (affectedProjects.isNotEmpty()) append("""<span>${affectedProjects.size} affected</span>""")
      if (cycles.isNotEmpty()) append("""<span>${cycles.size} cycle(s)</span>""")
      append("""</div>""")
    }

    return buildString {
      appendLine("""<!doctype html>""")
      appendLine("""<html lang="en">""")
      appendLine("""<head>""")
      appendLine("""  <meta charset="utf-8"/>""")
      appendLine("""  <meta name="viewport" content="width=device-width, initial-scale=1"/>""")
      appendLine("""  <title>${escapeHtml(graphName)} Project Graph</title>""")
      appendLine("""  <script src="https://cdn.jsdelivr.net/npm/vis-network@9.1.9/dist/vis-network.min.js"></script>""")
      appendLine("""  <link href="https://cdn.jsdelivr.net/npm/vis-network@9.1.9/dist/dist/vis-network.min.css" rel="stylesheet"/>""")
      appendLine("""  <style>""")
      appendLine("""    * { box-sizing: border-box; margin: 0; padding: 0; }""")
      appendLine("""    body { font-family: "IBM Plex Sans", system-ui, sans-serif; background: #0f1a14; color: #e0ede5; min-height: 100vh; display: flex; flex-direction: column; }""")
      appendLine("""    header { padding: 20px 32px 12px; border-bottom: 1px solid #1e3228; }""")
      appendLine("""    header h1 { font-size: 22px; font-weight: 600; color: #a5d6a7; }""")
      appendLine("""    header p { font-size: 13px; color: #6b8f78; margin-top: 4px; }""")
      appendLine("""    .meta { display: flex; gap: 16px; align-items: center; padding: 8px 32px; background: #0d1710; border-bottom: 1px solid #1e3228; flex-wrap: wrap; }""")
      appendLine("""    .stats { display: flex; gap: 16px; font-size: 12px; color: #8ab39a; }""")
      appendLine("""    .stats span::before { content: "* "; }""")
      appendLine("""    .legend { display: flex; gap: 14px; font-size: 12px; color: #8ab39a; }""")
      appendLine("""    .legend-item { display: flex; align-items: center; gap: 5px; }""")
      appendLine("""    .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }""")
      appendLine("""    .cycle-warning { background: #3b1a1a; border: 1px solid #e53935; border-radius: 8px; padding: 12px 16px; margin: 10px 32px; font-size: 13px; color: #ef9a9a; }""")
      appendLine("""    .cycle-warning ul { margin-top: 6px; padding-left: 20px; }""")
      appendLine("""    .cycle-warning li { margin-top: 3px; }""")
      appendLine("""    code { background: rgba(229,57,53,0.2); padding: 1px 4px; border-radius: 3px; font-family: monospace; }""")
      appendLine("""    #graph { flex: 1; min-height: 500px; }""")
      appendLine("""    .controls { position: fixed; bottom: 20px; right: 20px; display: flex; flex-direction: column; gap: 8px; }""")
      appendLine("""    .controls button { background: #1b3326; border: 1px solid #2e7d32; color: #a5d6a7; padding: 8px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; }""")
      appendLine("""    .controls button:hover { background: #2e7d32; }""")
      appendLine("""    #node-info { position: fixed; top: 80px; right: 20px; background: #1b3326; border: 1px solid #2e7d32; padding: 12px; border-radius: 8px; font-size: 13px; max-width: 260px; display: none; white-space: pre-wrap; }""")
      appendLine("""  </style>""")
      appendLine("""</head>""")
      appendLine("""<body>""")
      appendLine("""  <header>""")
      appendLine("""    <h1>${escapeHtml(graphName)} Project Dependency Graph</h1>""")
      appendLine("""    <p>Click a node to highlight neighbours. Scroll to zoom. Drag to pan.</p>""")
      appendLine("""  </header>""")
      appendLine("""  <div class="meta">$statsHtml$legendHtml</div>""")
      if (cycleWarning.isNotEmpty()) appendLine("  $cycleWarning")
      appendLine("""  <div id="graph"></div>""")
      appendLine("""  <div id="node-info"></div>""")
      appendLine("""  <div class="controls">""")
      appendLine("""    <button onclick="network.fit()">Fit All</button>""")
      appendLine("""    <button onclick="relayout()">Re-layout</button>""")
      appendLine("""  </div>""")
      appendLine("""  <script>""")
      appendLine("""    const nodesData = [$nodesJson];""")
      appendLine("""    const edgesData = [$edgesJson];""")
      appendLine("""    const nodes = new vis.DataSet(nodesData);""")
      appendLine("""    const edges = new vis.DataSet(edgesData);""")
      appendLine("""    const container = document.getElementById("graph");""")
      appendLine("""    const network = new vis.Network(container, { nodes, edges }, {""")
      appendLine("""      physics: { enabled: true, stabilization: { iterations: 200 }, barnesHut: { gravitationalConstant: -8000, springLength: 180 } },""")
      appendLine("""      interaction: { hover: true, navigationButtons: false, keyboard: true },""")
      appendLine("""      edges: { smooth: { type: "cubicBezier", roundness: 0.4 } },""")
      appendLine("""      nodes: { shape: "box", borderWidth: 2, margin: 8 }""")
      appendLine("""    });""")
      appendLine("""    network.once("stabilized", () => network.setOptions({ physics: { enabled: false } }));""")
      appendLine("""    const nodeInfo = document.getElementById("node-info");""")
      appendLine("""    network.on("click", function(params) {""")
      appendLine("""      if (params.nodes.length === 0) {""")
      appendLine("""        nodes.update(nodes.get().map(n => ({ id: n.id, opacity: 1 })));""")
      appendLine("""        nodeInfo.style.display = "none";""")
      appendLine("""        return;""")
      appendLine("""      }""")
      appendLine("""      const selected = params.nodes[0];""")
      appendLine("""      const connected = new Set(network.getConnectedNodes(selected));""")
      appendLine("""      connected.add(selected);""")
      appendLine("""      nodes.update(nodes.get().map(n => ({ id: n.id, opacity: connected.has(n.id) ? 1 : 0.2 })));""")
      appendLine("""      const node = nodes.get(selected);""")
      appendLine("""      if (node && node.title) {""")
      appendLine("""        nodeInfo.textContent = node.title.replace(/\n/g, "
");""")
      appendLine("""        nodeInfo.style.display = "block";""")
      appendLine("""      }""")
      appendLine("""    });""")
      appendLine("""    function relayout() {""")
      appendLine("""      network.setOptions({ physics: { enabled: true } });""")
      appendLine("""      setTimeout(() => network.setOptions({ physics: { enabled: false } }), 2000);""")
      appendLine("""    }""")
      appendLine("""  </script>""")
      appendLine("""</body>""")
      append("""</html>""")
    }
  }

  fun writeTempFile(
    graphName: String,
    graph: ProjectDependencyGraph,
    affectedProjects: Set<String> = emptySet(),
  ): Path {
    val outputPath = Files.createTempFile("architect-project-graph-", ".html")
    outputPath.writeText(render(graphName, graph, affectedProjects))
    return outputPath
  }

  private fun escapeJs(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")

  private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
}
