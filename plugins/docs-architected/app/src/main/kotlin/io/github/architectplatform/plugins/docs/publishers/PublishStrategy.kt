package io.github.architectplatform.plugins.docs.publishers

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.dto.PublishContext
import java.io.File

/**
 * Base abstraction for documentation publishing strategies.
 * 
 * Different publishing targets (GitHub Pages, S3, Netlify, etc.) can have their own implementation.
 */
abstract class PublishStrategy(
    protected val context: PublishContext,
    protected val commandExecutor: CommandExecutor
) {
    
    /**
     * Validates that the environment is ready for publishing.
     * 
     * @param workingDir The directory containing built documentation
     * @param outputDir The directory containing the built documentation to publish
     * @return TaskResult indicating success or failure
     */
    abstract fun validateEnvironment(workingDir: File, outputDir: File): TaskResult
    
    /**
     * Performs pre-publish operations (e.g., creating CNAME files, .nojekyll, etc.).
     * 
     * @param outputDir The directory containing the built documentation
     * @return TaskResult indicating success or failure
     */
    abstract fun prePublish(outputDir: File): TaskResult
    
    /**
     * Publishes the documentation to the target destination.
     * 
     * @param workingDir The working directory
     * @param outputDir The directory containing the built documentation
     * @return TaskResult indicating success or failure
     */
    abstract fun publish(workingDir: File, outputDir: File): TaskResult
    
    /**
     * Performs post-publish cleanup operations.
     * 
     * @param workingDir The working directory
     */
    abstract fun cleanup(workingDir: File)
    
    /**
     * Gets the name of this publishing strategy for logging purposes.
     */
    abstract fun getName(): String
    
    /**
     * Executes the full publish process: validate -> pre-publish -> publish.
     * 
     * @param workingDir The working directory
     * @param outputDir The directory containing the built documentation
     * @return TaskResult indicating success or failure with all sub-results
     */
    fun executePublish(workingDir: File, outputDir: File): TaskResult {
        val results = mutableListOf<TaskResult>()
        
        try {
            // Step 1: Validate environment
            val validateResult = validateEnvironment(workingDir, outputDir)
            results.add(validateResult)
            if (!validateResult.success) {
                return TaskResult.failure("${getName()} validation failed", results)
            }
            
            // Step 2: Pre-publish operations
            val prePublishResult = prePublish(outputDir)
            results.add(prePublishResult)
            if (!prePublishResult.success) {
                return TaskResult.failure("${getName()} pre-publish failed", results)
            }
            
            // Step 3: Publish
            val publishResult = publish(workingDir, outputDir)
            results.add(publishResult)
            if (!publishResult.success) {
                return TaskResult.failure("${getName()} publish failed", results)
            }
            
            return TaskResult.success("Documentation published successfully with ${getName()}", results)
            
        } finally {
            // Always cleanup
            cleanup(workingDir)
        }
    }
}
