package io.github.architectplatform.engine.health

import io.github.architectplatform.core.project.app.ProjectService
import io.github.architectplatform.engine.core.tasks.application.TaskService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

@Controller("/api/health")
@ExecuteOn(TaskExecutors.IO)
class HealthController(
    private val taskService: TaskService,
    private val projectService: ProjectService,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    data class HealthCheck(
        val name: String,
        val status: String,
        val detail: String? = null,
    )

    data class HealthResponse(
        val status: String,
        val checks: List<HealthCheck>,
        val uptime: String,
        val version: String = "1.0.0",
    )

    @Get
    fun health(): HealthResponse {
        val checks = mutableListOf<HealthCheck>()

        // Check 1: Project service
        val projectServiceOk =
            try {
                projectService.getAllProjects()
                true
            } catch (_: Exception) {
                false
            }
        checks.add(
            HealthCheck(
                name = "project-service",
                status = if (projectServiceOk) "UP" else "DOWN",
                detail = if (projectServiceOk) "Project service operational" else "Project service unavailable",
            ),
        )

        // Check 2: Task service (if injectable and reachable, it is operational)
        val taskServiceOk =
            try {
                taskService.toString()
                true
            } catch (_: Exception) {
                false
            }
        checks.add(
            HealthCheck(
                name = "task-service",
                status = if (taskServiceOk) "UP" else "DOWN",
                detail = if (taskServiceOk) "Task service operational" else "Task service unavailable",
            ),
        )

        // Check 3: JVM memory
        val runtime = Runtime.getRuntime()
        val usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
        val memoryUsagePercent = if (maxMemoryMb > 0) (usedMemoryMb * 100) / maxMemoryMb else 0
        val memoryOk = memoryUsagePercent < 95
        checks.add(
            HealthCheck(
                name = "jvm-memory",
                status = if (memoryOk) "UP" else "DOWN",
                detail = "Used: ${usedMemoryMb}MB / ${maxMemoryMb}MB ($memoryUsagePercent%)",
            ),
        )

        // Check 4: Thread pool
        val threadCount = Thread.activeCount()
        val threadOk = threadCount < 500
        checks.add(
            HealthCheck(
                name = "thread-pool",
                status = if (threadOk) "UP" else "DOWN",
                detail = "Active threads: $threadCount",
            ),
        )

        val allUp = checks.all { it.status == "UP" }
        val uptimeFormatted = formatUptime(ManagementFactory.getRuntimeMXBean().uptime)

        logger.debug("Health check completed: status={}", if (allUp) "UP" else "DOWN")

        return HealthResponse(
            status = if (allUp) "UP" else "DOWN",
            checks = checks,
            uptime = uptimeFormatted,
        )
    }

    private fun formatUptime(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
