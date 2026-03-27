package io.github.architectplatform.plugins.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.toList

object RuleSupport {
    private val sourceExtensions = setOf("kt", "java", "js", "ts", "py", "go", "rs")
    private val classPatterns = listOf(
        Regex("""(?:public\s+)?(?:data\s+class|enum\s+class|class|interface|object)\s+(\w+)"""),
        Regex("""public\s+(?:class|interface|enum)\s+(\w+)"""),
        Regex("""export\s+class\s+(\w+)"""),
        Regex("""class\s+(\w+)"""),
        Regex("""struct\s+(\w+)"""),
        Regex("""trait\s+(\w+)"""),
    )
    private val functionPatterns = listOf(
        Regex("""(?:public\s+)?fun\s+(\w+)"""),
        Regex("""public\s+[A-Za-z0-9_<>, ?\[\]]+\s+(\w+)\s*\("""),
    )

    fun analyzeProjectFiles(projectDir: Path): List<SourceFileAnalysis> =
        try {
            Files.walk(projectDir)
                .filter { it.isRegularFile() }
                .filter { sourceExtensions.contains(it.extension.lowercase()) }
                .filter { !shouldExcludePath(projectDir, it) }
                .map { analyzeFile(projectDir, it) }
                .filter { it != null }
                .map { it!! }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }

    fun matchesRule(rule: ArchitectureRule, analysis: SourceFileAnalysis): Boolean {
        if (!matchesPaths(rule.paths, analysis.relativePath)) {
            return false
        }

        val regex = runCatching { Regex(rule.pattern) }.getOrNull() ?: return false
        val fileName = analysis.file.fileName.toString().substringBeforeLast('.')
        val declarationNames = analysis.declarations.map { it.name }
        return regex.matches(fileName) ||
            declarationNames.any { regex.matches(it) } ||
            regex.matches(analysis.relativePath)
    }

    fun matchesPaths(pathPatterns: List<String>, relativePath: String): Boolean {
        if (pathPatterns.isEmpty()) {
            return true
        }

        return pathPatterns.any { pattern ->
            runCatching { Regex(pattern).matches(relativePath) }.getOrElse {
                relativePath == pattern || relativePath.startsWith(pattern.trimEnd('/'))
            }
        }
    }

    fun hasDocumentation(content: String, declarationLine: Int): Boolean {
        val lines = content.lines()
        var index = declarationLine - 2
        while (index >= 0 && (lines[index].trim().isBlank() || lines[index].trim().startsWith("@"))) {
            index--
        }
        if (index < 0) {
            return false
        }
        if (lines[index].trim().startsWith("/**")) {
            return true
        }
        if (lines[index].trim().endsWith("*/")) {
            while (index >= 0) {
                if (lines[index].trim().startsWith("/**")) {
                    return true
                }
                if (lines[index].trim().startsWith("/*")) {
                    return false
                }
                index--
            }
        }
        return false
    }

    fun moduleFor(relativePath: String): String {
        val segments = relativePath.split('/').filter { it.isNotBlank() }
        if (segments.isEmpty()) {
            return "."
        }
        return if (segments.first() == "plugins" && segments.size > 1) {
            "${segments[0]}/${segments[1]}"
        } else {
            segments.first()
        }
    }

    fun moduleBoundariesFor(boundaries: Map<String, List<String>>, moduleName: String): List<String>? =
        boundaries.entries.firstOrNull { globMatches(it.key, moduleName) }?.value

    fun globMatches(pattern: String, value: String): Boolean =
        Regex(pattern.replace(".", "\\.").replace("*", ".*")).matches(value)

    fun testFileExists(file: SourceFileAnalysis, files: List<SourceFileAnalysis>): Boolean {
        val candidates = file.declarations
            .filter { it.kind == "class" || it.kind == "interface" || it.kind == "object" }
            .flatMap { declaration -> listOf("${declaration.name}Test", "${declaration.name}Tests") }
            .toSet()

        return files.any { candidate ->
            candidate.relativePath.contains("/test/") &&
                candidate.declarations.any { it.name in candidates }
        }
    }

    private fun analyzeFile(projectDir: Path, file: Path): SourceFileAnalysis? {
        val content = runCatching { file.readText() }.getOrNull() ?: return null
        val relativePath = projectDir.relativize(file).toString().replace('\\', '/')
        return SourceFileAnalysis(
            file = file,
            relativePath = relativePath,
            packageName = extractPackageName(content),
            moduleName = moduleFor(relativePath),
            content = content,
            imports = extractImports(content),
            declarations = extractDeclarations(content),
        )
    }

    private fun extractPackageName(content: String): String? =
        content.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("package ") || it.startsWith("namespace ") }
            ?.substringAfter(' ')
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun extractImports(content: String): List<ImportReference> {
        val imports = mutableListOf<ImportReference>()
        content.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("import ") && line.contains(" from ") -> {
                    val target = line.substringAfter(" from ").trim().trim('"', '\'', ';')
                    if (target.isNotBlank()) {
                        imports += ImportReference(target, index + 1)
                    }
                }
                line.startsWith("import ") -> {
                    val target = line.removePrefix("import ").trim().substringBefore(';').trim()
                    if (target.isNotBlank()) {
                        imports += ImportReference(target, index + 1)
                    }
                }
                line.startsWith("from ") && line.contains(" import ") -> {
                    val target = line.removePrefix("from ").substringBefore(" import ").trim()
                    if (target.isNotBlank()) {
                        imports += ImportReference(target, index + 1)
                    }
                }
                line.contains("require(") -> {
                    Regex("""require\(['"]([^'"]+)['"]\)""").find(line)?.groupValues?.getOrNull(1)?.let { target ->
                        imports += ImportReference(target, index + 1)
                    }
                }
                line.startsWith("use ") -> {
                    val target = line.removePrefix("use ").substringBefore(';').trim()
                    if (target.isNotBlank()) {
                        imports += ImportReference(target, index + 1)
                    }
                }
            }
        }
        return imports.distinctBy { "${it.line}:${it.name}" }
    }

    private fun extractDeclarations(content: String): List<Declaration> {
        val declarations = mutableListOf<Declaration>()
        content.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            classPatterns.forEach { pattern ->
                pattern.find(line)?.groupValues?.getOrNull(1)?.let { name ->
                    declarations += Declaration(
                        name = name,
                        line = index + 1,
                        kind = when {
                            line.contains("interface") -> "interface"
                            line.contains("object") -> "object"
                            else -> "class"
                        },
                        visibility = if (line.contains("private ")) "private" else "public",
                    )
                }
            }
            functionPatterns.forEach { pattern ->
                pattern.find(line)?.groupValues?.getOrNull(1)?.let { name ->
                    declarations += Declaration(
                        name = name,
                        line = index + 1,
                        kind = "function",
                        visibility = if (line.contains("private ")) "private" else "public",
                    )
                }
            }
        }
        return declarations.distinctBy { "${it.kind}:${it.name}:${it.line}" }
    }

    private fun shouldExcludePath(projectDir: Path, path: Path): Boolean {
        val relativePath = projectDir.relativize(path).toString().replace('\\', '/')
        return listOf("build/", "target/", "node_modules/", ".git/", ".gradle/", "dist/", "out/", "bin/")
            .any { relativePath.contains(it) }
    }
}
