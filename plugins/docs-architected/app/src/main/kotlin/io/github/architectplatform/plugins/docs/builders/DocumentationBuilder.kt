package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.dto.BuildContext
import io.github.architectplatform.plugins.docs.dto.ComponentDocs
import java.io.File

/**
 * Base abstraction for documentation builders.
 * 
 * Each documentation framework (MkDocs, Docusaurus, VuePress) has its own implementation
 * that is responsible for:
 * - Setting up the required environment (venv for Python, node_modules for Node.js)
 * - Installing dependencies
 * - Building documentation
 * - Managing framework-specific configurations
 */
abstract class DocumentationBuilder(
    protected val context: BuildContext,
    protected val commandExecutor: CommandExecutor
) {
    
    /**
     * Sets up the environment required for building documentation.
     * 
     * For Python-based tools: Creates and activates a virtual environment
     * For Node.js-based tools: Ensures node_modules is properly set up
     * 
     * @param workingDir The directory where the build will occur
     * @return TaskResult indicating success or failure
     */
    abstract fun setupEnvironment(workingDir: File): TaskResult
    
    /**
     * Installs the dependencies required for building documentation.
     * 
     * @param workingDir The directory where dependencies should be installed
     * @return TaskResult indicating success or failure
     */
    abstract fun installDependencies(workingDir: File): TaskResult
    
    /**
     * Generates framework-specific configuration files.
     * 
     * @param workingDir The repository root directory
     * @param components List of components to include in documentation
     * @return TaskResult indicating success or failure
     */
    abstract fun generateConfiguration(workingDir: File, components: List<ComponentDocs>): TaskResult
    
    /**
     * Builds the documentation.
     * 
     * @param workingDir The directory containing the documentation source
     * @return TaskResult indicating success or failure
     */
    abstract fun build(workingDir: File): TaskResult
    
    /**
     * Cleans up temporary files and resources created during the build process.
     * 
     * @param workingDir The directory to clean up
     */
    abstract fun cleanup(workingDir: File)
    
    /**
     * Gets the name of this builder for logging purposes.
     */
    abstract fun getName(): String
    
    /**
     * Executes the full build process: setup -> install -> configure -> build.
     * 
     * @param workingDir The directory where the build should occur
     * @param components List of components to include in documentation
     * @return TaskResult indicating success or failure with all sub-results
     */
    fun executeBuild(workingDir: File, components: List<ComponentDocs>): TaskResult {
        val results = mutableListOf<TaskResult>()
        
        try {
            // Step 1: Setup environment
            val setupResult = setupEnvironment(workingDir)
            results.add(setupResult)
            if (!setupResult.success) {
                return TaskResult.failure("${getName()} environment setup failed", results)
            }
            
            // Step 2: Install dependencies if configured
            if (context.installDeps) {
                val installResult = installDependencies(workingDir)
                results.add(installResult)
                if (!installResult.success) {
                    return TaskResult.failure("${getName()} dependency installation failed", results)
                }
            }
            
            // Step 3: Generate configuration
            val configResult = generateConfiguration(workingDir, components)
            results.add(configResult)
            if (!configResult.success) {
                return TaskResult.failure("${getName()} configuration generation failed", results)
            }
            
            // Step 4: Build documentation
            val buildResult = build(workingDir)
            results.add(buildResult)
            if (!buildResult.success) {
                return TaskResult.failure("${getName()} build failed", results)
            }
            
            return TaskResult.success("Documentation built successfully with ${getName()}", results)
            
        } finally {
            // Always cleanup, even if build fails
            cleanup(workingDir)
        }
    }
}
