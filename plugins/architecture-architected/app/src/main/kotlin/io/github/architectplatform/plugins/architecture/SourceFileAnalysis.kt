package io.github.architectplatform.plugins.architecture

import java.nio.file.Path

data class ImportReference(
    val name: String,
    val line: Int,
)

data class Declaration(
    val name: String,
    val line: Int,
    val kind: String,
    val visibility: String = "public",
) {
    val isPublic: Boolean
        get() = visibility == "public"
}

data class SourceFileAnalysis(
    val file: Path,
    val relativePath: String,
    val packageName: String?,
    val moduleName: String,
    val content: String,
    val imports: List<ImportReference>,
    val declarations: List<Declaration>,
)

data class ImportGraphEdge(
    val from: SourceFileAnalysis,
    val to: SourceFileAnalysis,
    val importReference: ImportReference,
)

data class ImportGraph(
    val edges: List<ImportGraphEdge>,
    val cycles: List<List<SourceFileAnalysis>>,
)
