package io.github.architectplatform.plugins.docs.publishers

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.dto.PublishContext
import io.github.architectplatform.plugins.docs.utils.SecurityUtils
import java.io.File

/**
 * Publisher for GitHub Pages.
 * 
 * This publisher:
 * - Validates that the output directory exists
 * - Creates CNAME file for custom domains
 * - Creates .nojekyll file to bypass Jekyll processing
 * - Commits documentation to gh-pages branch
 * - Pushes to remote repository
 */
class GitHubPagesPublisher(
    context: PublishContext,
    commandExecutor: CommandExecutor
) : PublishStrategy(context, commandExecutor) {
    
    private var tempDir: File? = null
    
    override fun getName(): String = "GitHub Pages"
    
    /**
     * Validates that the output directory exists and contains files.
     */
    override fun validateEnvironment(workingDir: File, outputDir: File): TaskResult {
        return if (!outputDir.exists()) {
            TaskResult.failure("Output directory not found: ${outputDir.path}. Please run docs-build first.")
        } else if (outputDir.listFiles()?.isEmpty() != false) {
            TaskResult.failure("Output directory is empty: ${outputDir.path}")
        } else {
            TaskResult.success("Output directory validated")
        }
    }
    
    /**
     * Creates CNAME file for custom domain if configured.
     */
    override fun prePublish(outputDir: File): TaskResult {
        val results = mutableListOf<String>()
        
        // Create CNAME file if custom domain is specified
        if (context.cname && context.domain.isNotEmpty()) {
            if (!SecurityUtils.isValidDomain(context.domain)) {
                return TaskResult.failure("Invalid domain format: ${context.domain}")
            } else {
                val cnameFile = File(outputDir, "CNAME")
                cnameFile.writeText(context.domain)
                results.add("Created CNAME file for domain: ${context.domain}")
            }
        }
        
        // Create .nojekyll file to bypass Jekyll processing
        val nojekyllFile = File(outputDir, ".nojekyll")
        if (!nojekyllFile.exists()) {
            nojekyllFile.writeText("")
            results.add("Created .nojekyll file")
        }
        
        return if (results.isNotEmpty()) {
            TaskResult.success(results.joinToString(", "))
        } else {
            TaskResult.success("Pre-publish operations completed")
        }
    }
    
    /**
     * Publishes documentation to GitHub Pages by committing to gh-pages branch.
     */
    override fun publish(workingDir: File, outputDir: File): TaskResult {
        val results = mutableListOf<String>()
        val sanitizedBranch = SecurityUtils.sanitizeBranch(context.branch)
        
        try {
            // Step 1: Get current branch
            commandExecutor.execute("git rev-parse --abbrev-ref HEAD", workingDir.toString())
            results.add("Saved current branch reference")
            
            // Step 2: Create temporary directory for docs
            tempDir = File(workingDir, ".docs-publish-temp")
            tempDir?.mkdirs()
            
            // Step 3: Copy built documentation to temp directory
            outputDir.copyRecursively(tempDir!!, overwrite = true)
            results.add("Copied documentation to temporary directory")
            
            // Step 4: Stash current changes
            try {
                commandExecutor.execute("git stash push -m 'Stashing changes before gh-pages deployment'", workingDir.toString())
                results.add("Stashed current changes")
            } catch (e: Exception) {
                // No changes to stash - this is okay
            }
            
            // Step 5: Check if gh-pages branch exists and checkout
            try {
                commandExecutor.execute("git show-ref --verify --quiet refs/heads/$sanitizedBranch", workingDir.toString())
                // Branch exists
                commandExecutor.execute("git checkout $sanitizedBranch", workingDir.toString())
                results.add("Checked out existing $sanitizedBranch branch")
            } catch (e: Exception) {
                // Branch doesn't exist, create orphan branch
                commandExecutor.execute("git checkout --orphan $sanitizedBranch", workingDir.toString())
                commandExecutor.execute("git rm -rf .", workingDir.toString())
                results.add("Created new $sanitizedBranch branch")
            }
            
            // Step 6: Remove all files except .git and temp directory
            val filesToRemove = workingDir.listFiles()?.filter { 
                it.name != ".git" && it.name != ".docs-publish-temp"
            } ?: emptyList()
            filesToRemove.forEach { it.deleteRecursively() }
            
            // Step 7: Copy files from temp directory
            tempDir?.listFiles()?.forEach { file ->
                file.copyRecursively(File(workingDir, file.name), overwrite = true)
            }
            results.add("Copied documentation to $sanitizedBranch branch")
            
            // Step 8: Add and commit
            commandExecutor.execute("git add .", workingDir.toString())
            try {
                commandExecutor.execute("git commit -m 'docs: update documentation'", workingDir.toString())
                results.add("Committed documentation changes")
            } catch (e: Exception) {
                // No changes to commit - this is okay
                results.add("No documentation changes to commit")
            }
            
            // Step 9: Push to remote
            commandExecutor.execute("git push origin $sanitizedBranch", workingDir.toString())
            results.add("Pushed to $sanitizedBranch branch")
            
            // Step 10: Return to original branch
            commandExecutor.execute("git checkout -", workingDir.toString())
            results.add("Returned to original branch")
            
            // Step 11: Restore stashed changes
            try {
                commandExecutor.execute("git stash pop", workingDir.toString())
                results.add("Restored stashed changes")
            } catch (e: Exception) {
                // No stashed changes - this is okay
            }
            
            return TaskResult.success(results.joinToString("; "))
            
        } catch (e: Exception) {
            return TaskResult.failure("Failed to publish to GitHub Pages: ${e.message}")
        }
    }
    
    /**
     * Cleans up temporary directory.
     */
    override fun cleanup(workingDir: File) {
        tempDir?.let { 
            if (it.exists()) {
                it.deleteRecursively()
            }
        }
    }
}
