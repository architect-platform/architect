package io.github.architectplatform.agent.dockercompose.application

import io.github.architectplatform.agent.dockercompose.domain.AgentConfig
import io.github.architectplatform.agent.dockercompose.domain.DeploymentCommand
import io.github.architectplatform.agent.dockercompose.domain.DeploymentResult
import io.github.architectplatform.agent.dockercompose.domain.DeploymentStatus
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service for deploying applications using Docker Compose.
 * Applies Command Pattern - encapsulates deployment as objects.
 * Follows Single Responsibility - only handles docker-compose operations.
 */
@Singleton
class DockerComposeDeploymentService(
    private val templateRenderingService: TemplateRenderingService,
    private val agentConfig: AgentConfig
) {

    private val logger = LoggerFactory.getLogger(DockerComposeDeploymentService::class.java)

    /**
     * Deploy using docker-compose.
     * Applies Template Method Pattern - defines deployment algorithm.
     */
    fun deploy(command: DeploymentCommand): DeploymentResult {
        logger.info("Starting deployment for: ${command.resourceName}")

        try {
            // Step 1: Render templates
            val composeContent = templateRenderingService.renderDockerCompose(command)
            var result = command.withRenderedComposeFile(composeContent)

            // Step 2: Create project directory
            val projectDir = prepareProjectDirectory(command.projectName)

            // Step 3: Write docker-compose.yml
            writeComposeFile(projectDir, composeContent)

            // Step 4: Execute docker-compose up
            result = result.copy(status = DeploymentStatus.DEPLOYING)
            val services = executeDockerComposeUp(projectDir, command.projectName)

            logger.info("Successfully deployed ${services.size} service(s)")
            return result.complete(
                services = services,
                message = "Successfully deployed ${services.size} service(s)"
            )

        } catch (e: Exception) {
            logger.error("Deployment failed: ${e.message}", e)
            return command.withRenderedComposeFile("")
                .fail("Deployment failed: ${e.message}")
        }
    }

    private fun prepareProjectDirectory(projectName: String): Path {
        val projectDir = Paths.get(agentConfig.workingDirectory, projectName)
        Files.createDirectories(projectDir)
        logger.debug("Created project directory: $projectDir")
        return projectDir
    }

    private fun writeComposeFile(projectDir: Path, content: String) {
        val composeFile = projectDir.resolve("docker-compose.yml")
        Files.writeString(composeFile, content)
        logger.debug("Wrote docker-compose.yml to: $composeFile")
    }

    private fun executeDockerComposeUp(projectDir: Path, projectName: String): List<String> {
        val command = listOf(
            agentConfig.dockerComposeCommand,
            "-f", "docker-compose.yml",
            "-p", projectName,
            "up", "-d"
        )

        logger.info("Executing: ${command.joinToString(" ")}")

        val process = ProcessBuilder(command)
            .directory(projectDir.toFile())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw DockerComposeException("docker-compose failed with exit code $exitCode: $output")
        }

        logger.debug("docker-compose output: $output")

        // Extract service names from output or use docker-compose ps
        return extractServiceNames(projectDir, projectName)
    }

    private fun extractServiceNames(projectDir: Path, projectName: String): List<String> {
        return try {
            val command = listOf(
                agentConfig.dockerComposeCommand,
                "-f", "docker-compose.yml",
                "-p", projectName,
                "ps", "--services"
            )

            val process = ProcessBuilder(command)
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .start()

            val services = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            services.filter { it.isNotBlank() }
        } catch (e: Exception) {
            logger.warn("Could not extract service names: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get deployment status for a project.
     */
    fun getDeploymentStatus(projectName: String): Map<String, Any> {
        val projectDir = Paths.get(agentConfig.workingDirectory, projectName)
        return mapOf(
            "projectName" to projectName,
            "exists" to Files.exists(projectDir.resolve("docker-compose.yml"))
        )
    }
}

class DockerComposeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
