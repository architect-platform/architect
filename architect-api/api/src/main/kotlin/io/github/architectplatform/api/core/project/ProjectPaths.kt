package io.github.architectplatform.api.core.project

import java.nio.file.Path

fun resolvePathWithinRoot(root: Path, candidate: String, description: String = "Path"): Path {
  val relativePath = Path.of(candidate)
  require(!relativePath.isAbsolute) {
    "$description must be relative to the project root: $candidate"
  }

  return resolvePathWithinRoot(root, relativePath, description)
}

fun resolvePathWithinRoot(root: Path, candidate: Path, description: String = "Path"): Path {
  val normalizedRoot = root.toAbsolutePath().normalize()
  val resolvedPath = normalizedRoot.resolve(candidate).normalize()

  require(resolvedPath.startsWith(normalizedRoot)) {
    "$description must stay within the project root: $candidate"
  }

  return resolvedPath
}

fun ProjectContext.resolvePath(candidate: String, description: String = "Path"): Path =
  resolvePathWithinRoot(dir, candidate, description)
