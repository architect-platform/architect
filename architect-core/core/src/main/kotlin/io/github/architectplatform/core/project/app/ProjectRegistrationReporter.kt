package io.github.architectplatform.core.project.app

/**
 * Optional host-provided reporter for project registration side effects.
 *
 * Core must not depend on cloud implementations directly.
 */
fun interface ProjectRegistrationReporter {
  fun reportProject(name: String, path: String, description: String?)
}
