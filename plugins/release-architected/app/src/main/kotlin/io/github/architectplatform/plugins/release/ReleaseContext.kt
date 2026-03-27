package io.github.architectplatform.plugins.release

data class ReleaseContext(
  val enabled: Boolean = true,
  val workingDirectory: String = ".",
  val strategy: String = "semantic",
  val changelog: Boolean = true,
  val changelogPath: String = "CHANGELOG.md",
  val releaseNotesPath: String = "build/release-notes.md",
  val versionFiles: List<String> = listOf("VERSION"),
  val tagPrefix: String = "v",
  val currentVersion: String? = null,
  val manualVersion: String? = null,
  val artifacts: List<ReleaseArtifact> = emptyList(),
  val publish: ReleasePublish = ReleasePublish(),
  val rollback: ReleaseRollback = ReleaseRollback(),
) {
  fun normalizedStrategy(): String = strategy.trim().lowercase()
}

data class ReleaseArtifact(
  val type: String,
  val enabled: Boolean = true,
  val registry: String? = null,
  val directory: String? = null,
  val image: String? = null,
  val context: String? = null,
  val dockerfile: String? = null,
  val assets: List<String> = emptyList(),
  val notesPath: String? = null,
  val tags: List<String> = emptyList(),
) {
  fun normalizedType(): String = type.trim().lowercase()
}

data class ReleasePublish(
  val enabled: Boolean = true,
  val beforeCommands: List<String> = emptyList(),
  val afterCommands: List<String> = emptyList(),
)

data class ReleaseRollback(
  val enabled: Boolean = true,
  val deleteTag: Boolean = true,
  val restoreVersionFiles: Boolean = true,
  val restoreChangelog: Boolean = true,
  val beforeCommands: List<String> = emptyList(),
  val afterCommands: List<String> = emptyList(),
)
