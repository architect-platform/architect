package io.github.architectplatform.engine.audit

import jakarta.inject.Singleton
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-memory audit log for task executions.
 *
 * Records every task execution with timestamp, project, task, result, and duration.
 * Supports configurable retention (max entries) and query filtering by project,
 * task, status, and time range.
 */
@Singleton
class AuditService {

    data class AuditEntry(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Instant = Instant.now(),
        val projectName: String,
        val taskName: String,
        val status: String,
        val durationMs: Long,
        val executionId: String? = null,
        val message: String? = null,
    )

    private val entries = ConcurrentLinkedDeque<AuditEntry>()
    private val maxEntries = 10_000

    fun record(entry: AuditEntry) {
        entries.addFirst(entry)
        while (entries.size > maxEntries) {
            entries.removeLast()
        }
    }

    fun record(
        projectName: String,
        taskName: String,
        status: String,
        durationMs: Long,
        executionId: String? = null,
        message: String? = null,
    ) {
        record(
            AuditEntry(
                projectName = projectName,
                taskName = taskName,
                status = status,
                durationMs = durationMs,
                executionId = executionId,
                message = message,
            )
        )
    }

    fun getEntries(
        project: String? = null,
        task: String? = null,
        status: String? = null,
        since: Instant? = null,
        until: Instant? = null,
        limit: Int = 100,
    ): List<AuditEntry> {
        return entries.asSequence()
            .filter { project == null || it.projectName == project }
            .filter { task == null || it.taskName == task }
            .filter { status == null || it.status == status }
            .filter { since == null || it.timestamp >= since }
            .filter { until == null || it.timestamp <= until }
            .take(limit)
            .toList()
    }

    fun clear() {
        entries.clear()
    }

    fun count(): Int = entries.size
}
