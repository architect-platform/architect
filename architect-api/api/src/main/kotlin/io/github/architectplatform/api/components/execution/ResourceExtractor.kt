package io.github.architectplatform.api.components.execution

import java.nio.file.Path

/**
 * Interface for extracting and managing classpath resources.
 *
 * ResourceExtractor provides utilities for working with files embedded in JAR resources,
 * allowing tasks to extract template files, configuration files, or other resources
 * to the filesystem.
 *
 * Example usage:
 * ```kotlin
 * val extractor = environment.service(ResourceExtractor::class.java)
 * val classLoader = MyPlugin::class.java.classLoader
 * 
 * // Copy a single file from resources
 * extractor.copyFileFromResources(
 *   classLoader,
 *   "templates/config.yml",
 *   projectDir.resolve("config"),
 *   "config.yml"
 * )
 * 
 * // Copy an entire directory
 * extractor.copyDirectoryFromResources(
 *   classLoader,
 *   "templates/project",
 *   projectDir
 * )
 * 
 * // Read a resource file's content
 * val content = extractor.getResourceFileContent(classLoader, "templates/readme.md")
 * 
 * // List all files in a resource directory
 * val files = extractor.listResourceFiles(classLoader, "templates")
 * ```
 */
interface ResourceExtractor {
  /**
   * Copies a single file from classpath resources to the filesystem.
   *
   * @param classLoader The class loader to use for loading resources
   * @param resourcePath Path to the resource file within the classpath
   * @param targetDir Directory where the file should be copied
   * @param targetFileName Optional custom filename for the target file.
   *                       If null, uses the original filename from resourcePath.
   */
  fun copyFileFromResources(
      classLoader: ClassLoader,
      resourcePath: String,
      targetDir: Path,
      targetFileName: String? = null,
  )

  /**
   * Copies an entire directory from classpath resources to the filesystem.
   *
   * This method recursively copies all files and subdirectories from the specified
   * resource directory to the target location.
   *
   * @param classLoader The class loader to use for loading resources
   * @param resourceRoot Root path of the resource directory within the classpath
   * @param targetDirectory Directory where the resources should be copied
   */
  fun copyDirectoryFromResources(
      classLoader: ClassLoader,
      resourceRoot: String,
      targetDirectory: Path
  )

  /**
   * Reads the content of a resource file as a string.
   *
   * @param classLoader The class loader to use for loading the resource
   * @param resourcePath Path to the resource file within the classpath
   * @return The content of the file as a string
   */
  fun getResourceFileContent(classLoader: ClassLoader, resourcePath: String): String

  /**
   * Lists all files in a resource directory.
   *
   * @param classLoader The class loader to use for loading resources
   * @param resourceRoot Root path of the resource directory within the classpath
   * @return List of relative paths to all files in the directory and its subdirectories
   */
  fun listResourceFiles(classLoader: ClassLoader, resourceRoot: String): List<String>
}
