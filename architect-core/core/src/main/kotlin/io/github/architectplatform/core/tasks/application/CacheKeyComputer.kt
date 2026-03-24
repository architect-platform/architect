package io.github.architectplatform.core.tasks.application

import io.github.architectplatform.api.core.tasks.cache.CacheDescriptor
import io.github.architectplatform.api.core.tasks.cache.CacheInput
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/**
 * Computes a deterministic cache key from a [CacheDescriptor] by hashing all inputs.
 */
object CacheKeyComputer {

  fun compute(descriptor: CacheDescriptor, projectDir: String, config: Map<String, Any?>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    for (input in descriptor.inputs.sortedBy { it.javaClass.simpleName + inputIdentity(it) }) {
      val chunk = resolveInput(input, projectDir, config)
      digest.update(chunk.toByteArray(Charsets.UTF_8))
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun inputIdentity(input: CacheInput): String = when (input) {
    is CacheInput.FileSet -> input.glob
    is CacheInput.ConfigValue -> input.key
    is CacheInput.EnvVar -> input.name
    is CacheInput.CommandOutput -> input.command
  }

  private fun resolveInput(input: CacheInput, projectDir: String, config: Map<String, Any?>): String =
    when (input) {
      is CacheInput.FileSet -> resolveFileSet(input.glob, projectDir)
      is CacheInput.ConfigValue -> resolveConfigValue(input.key, config)
      is CacheInput.EnvVar -> resolveEnvVar(input.name)
      is CacheInput.CommandOutput -> resolveCommandOutput(input.command, projectDir)
    }

  private fun resolveFileSet(glob: String, projectDir: String): String {
    val root = Path.of(projectDir)
    if (!Files.isDirectory(root)) return "empty"
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
    val digest = MessageDigest.getInstance("SHA-256")
    val matched = mutableListOf<Path>()

    Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        val relative = root.relativize(file)
        if (matcher.matches(relative)) {
          matched.add(file)
        }
        return FileVisitResult.CONTINUE
      }
    })

    matched.sortBy { it.toString() }
    for (file in matched) {
      digest.update(file.fileName.toString().toByteArray(Charsets.UTF_8))
      digest.update(Files.readAllBytes(file))
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun resolveConfigValue(key: String, config: Map<String, Any?>): String {
    val value = config[key]
    return "config:$key=${value ?: "<unset>"}"
  }

  private fun resolveEnvVar(name: String): String {
    val value = System.getenv(name)
    return "env:$name=${value ?: "<unset>"}"
  }

  private fun resolveCommandOutput(command: String, projectDir: String): String {
    return try {
      val pb = ProcessBuilder("sh", "-c", command)
      pb.directory(File(projectDir))
      pb.redirectErrorStream(true)
      val process = pb.start()
      val output = process.inputStream.bufferedReader().readText().trim()
      val exitCode = process.waitFor()
      if (exitCode == 0) "cmd:$output" else "cmd:error:$exitCode"
    } catch (_: Exception) {
      "cmd:error:exception"
    }
  }
}
