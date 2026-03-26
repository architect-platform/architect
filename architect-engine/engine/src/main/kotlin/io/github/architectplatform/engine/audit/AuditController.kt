package io.github.architectplatform.engine.audit

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import java.time.Instant

/**
 * REST controller exposing the execution audit log.
 *
 * Provides a query endpoint for audit entries with optional filters
 * for project, task, status, and time range.
 */
@Controller("/api/audit")
@ExecuteOn(TaskExecutors.IO)
class AuditController(private val auditService: AuditService) {

    data class AuditResponse(
        val total: Int,
        val entries: List<AuditService.AuditEntry>,
    )

    @Get
    fun getAuditLog(
        @QueryValue(defaultValue = "") project: String?,
        @QueryValue(defaultValue = "") task: String?,
        @QueryValue(defaultValue = "") status: String?,
        @QueryValue(defaultValue = "") since: String?,
        @QueryValue(defaultValue = "") until: String?,
        @QueryValue(defaultValue = "100") limit: Int,
    ): AuditResponse {
        val sinceInstant = since?.takeIf { it.isNotBlank() }?.let {
            try { Instant.parse(it) } catch (_: Exception) { null }
        }
        val untilInstant = until?.takeIf { it.isNotBlank() }?.let {
            try { Instant.parse(it) } catch (_: Exception) { null }
        }
        val entries = auditService.getEntries(
            project = project?.takeIf { it.isNotBlank() },
            task = task?.takeIf { it.isNotBlank() },
            status = status?.takeIf { it.isNotBlank() },
            since = sinceInstant,
            until = untilInstant,
            limit = limit,
        )
        return AuditResponse(total = entries.size, entries = entries)
    }
}
