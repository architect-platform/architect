package io.github.architectplatform.plugins.docker

data class DockerContext(
  val image: String = "",
  val registry: String = "",
  val platforms: List<String> = emptyList(),
  val dockerfile: String = "Dockerfile",
  val buildArgs: Map<String, String> = emptyMap(),
  val composeFile: String = "docker-compose.yml",
  val enabled: Boolean = true,
)
