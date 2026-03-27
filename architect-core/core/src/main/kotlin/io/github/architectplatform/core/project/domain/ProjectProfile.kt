package io.github.architectplatform.core.project.domain

data class ProjectProfile(
  val languages: Set<String> = emptySet(),
  val buildTools: Set<String> = emptySet(),
  val testFrameworks: Set<String> = emptySet(),
  val ciSystems: Set<String> = emptySet(),
  val containerization: Set<String> = emptySet(),
  val markers: Set<String> = emptySet(),
) {
  fun isEmpty(): Boolean =
    languages.isEmpty() &&
      buildTools.isEmpty() &&
      testFrameworks.isEmpty() &&
      ciSystems.isEmpty() &&
      containerization.isEmpty() &&
      markers.isEmpty()
}
