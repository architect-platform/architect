package io.github.architectplatform.plugins.architecture

class ImportGraphBuilder {
    fun build(files: List<SourceFileAnalysis>): ImportGraph {
        val byQualifiedName = mutableMapOf<String, SourceFileAnalysis>()
        val bySimpleName = mutableMapOf<String, MutableList<SourceFileAnalysis>>()

        files.forEach { file ->
            file.declarations.forEach { declaration ->
                val qualifiedName = listOfNotNull(file.packageName, declaration.name).joinToString(".")
                byQualifiedName[qualifiedName] = file
                bySimpleName.getOrPut(declaration.name) { mutableListOf() }.add(file)
            }
        }

        val edges = mutableListOf<ImportGraphEdge>()
        files.forEach { source ->
            source.imports.forEach { importReference ->
                resolveTarget(importReference.name, source, byQualifiedName, bySimpleName)?.takeIf { it.file != source.file }?.let { target ->
                    edges += ImportGraphEdge(source, target, importReference)
                }
            }
        }

        return ImportGraph(edges = edges, cycles = stronglyConnectedComponents(files, edges))
    }

    private fun resolveTarget(
        importName: String,
        source: SourceFileAnalysis,
        byQualifiedName: Map<String, SourceFileAnalysis>,
        bySimpleName: Map<String, List<SourceFileAnalysis>>,
    ): SourceFileAnalysis? {
        byQualifiedName[importName]?.let { return it }

        val normalized = importName.removePrefix("import ").substringAfterLast('.').substringAfterLast('/').substringAfterLast(':')
        bySimpleName[normalized]?.let { matches ->
            return matches.firstOrNull { it.moduleName == source.moduleName } ?: matches.firstOrNull()
        }

        if (importName.startsWith(".")) {
            val sourceDir = source.relativePath.substringBeforeLast('/', "")
            val resolved = (if (sourceDir.isBlank()) importName else "$sourceDir/$importName")
                .replace("/./", "/")
                .replace("../", "")
            return byQualifiedName.values.firstOrNull { it.relativePath.substringBeforeLast('.') == resolved.trimStart('/') }
        }

        return null
    }

    private fun stronglyConnectedComponents(
        files: List<SourceFileAnalysis>,
        edges: List<ImportGraphEdge>,
    ): List<List<SourceFileAnalysis>> {
        val adjacency = edges.groupBy { it.from }.mapValues { entry -> entry.value.map { it.to } }
        val indexMap = mutableMapOf<SourceFileAnalysis, Int>()
        val lowLink = mutableMapOf<SourceFileAnalysis, Int>()
        val onStack = mutableSetOf<SourceFileAnalysis>()
        val stack = ArrayDeque<SourceFileAnalysis>()
        val components = mutableListOf<List<SourceFileAnalysis>>()
        var index = 0

        fun strongConnect(node: SourceFileAnalysis) {
            indexMap[node] = index
            lowLink[node] = index
            index += 1
            stack.addLast(node)
            onStack += node

            adjacency[node].orEmpty().forEach { neighbor ->
                if (neighbor !in indexMap) {
                    strongConnect(neighbor)
                    lowLink[node] = minOf(lowLink.getValue(node), lowLink.getValue(neighbor))
                } else if (neighbor in onStack) {
                    lowLink[node] = minOf(lowLink.getValue(node), indexMap.getValue(neighbor))
                }
            }

            if (lowLink[node] == indexMap[node]) {
                val component = mutableListOf<SourceFileAnalysis>()
                do {
                    val item = stack.removeLast()
                    onStack -= item
                    component += item
                } while (item != node)

                val hasSelfLoop = adjacency[node].orEmpty().contains(node)
                if (component.size > 1 || hasSelfLoop) {
                    components += component.sortedBy { it.relativePath }
                }
            }
        }

        files.forEach { file ->
            if (file !in indexMap) {
                strongConnect(file)
            }
        }

        return components
    }
}
