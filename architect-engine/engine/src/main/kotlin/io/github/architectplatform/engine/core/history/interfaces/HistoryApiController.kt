package io.github.architectplatform.engine.core.history.interfaces

import io.github.architectplatform.engine.core.history.app.HistoryService
import io.github.architectplatform.engine.core.history.domain.ExecutionRecord
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@Controller("/api/history")
@ExecuteOn(TaskExecutors.IO)
class HistoryApiController(private val historyService: HistoryService) {

    @Get
    fun getAll(@QueryValue(defaultValue = "50") limit: Int): List<ExecutionRecord> =
        historyService.getAll(limit)

    @Get("/{project}")
    fun getByProject(
        @PathVariable project: String,
        @QueryValue(defaultValue = "50") limit: Int,
    ): List<ExecutionRecord> = historyService.getByProject(project, limit)
}
