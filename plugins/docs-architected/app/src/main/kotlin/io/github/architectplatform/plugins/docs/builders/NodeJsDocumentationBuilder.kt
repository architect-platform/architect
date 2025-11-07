package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.dto.BuildContext
import java.io.File

/**
 * Base class for Node.js-based documentation builders (Docusaurus, VuePress).
 * 
 * Handles common Node.js environment setup including:
 * - Checking for package.json
 * - Running npm/yarn/pnpm install
 * - Managing node_modules
 */
abstract class NodeJsDocumentationBuilder(
    context: BuildContext,
    commandExecutor: CommandExecutor
) : DocumentationBuilder(context, commandExecutor) {
    
    /**
     * Sets up Node.js environment by ensuring package.json exists.
     */
    override fun setupEnvironment(workingDir: File): TaskResult {
        val packageJson = File(workingDir, "package.json")
        
        return if (packageJson.exists()) {
            TaskResult.success("Node.js environment ready - package.json found")
        } else {
            TaskResult.failure("package.json not found in $workingDir. Please initialize a Node.js project first.")
        }
    }
    
    /**
     * Installs Node.js dependencies using npm, yarn, or pnpm.
     */
    override fun installDependencies(workingDir: File): TaskResult {
        return try {
            val packageLock = File(workingDir, "package-lock.json")
            val yarnLock = File(workingDir, "yarn.lock")
            val pnpmLock = File(workingDir, "pnpm-lock.yaml")
            
            val command = when {
                pnpmLock.exists() -> "pnpm install --frozen-lockfile"
                yarnLock.exists() -> "yarn install --frozen-lockfile"
                packageLock.exists() -> "npm ci"
                else -> "npm install"
            }
            
            commandExecutor.execute(command, workingDir.toString())
            TaskResult.success("Installed Node.js dependencies")
        } catch (e: Exception) {
            TaskResult.failure("Failed to install Node.js dependencies: ${e.message}")
        }
    }
    
    /**
     * Cleans up temporary files (default: no-op for Node.js builders).
     */
    override fun cleanup(workingDir: File) {
        // Node.js builders typically don't need cleanup
    }
}
