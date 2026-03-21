package io.github.architectplatform.engine.core.history.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.engine.core.history.domain.ExecutionRecord
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Persists execution records to `~/.architect/history/` as JSON files.
 *
 * Each record is stored as `{timestamp}-{executionId}.json` so directory listings
 * are in chronological order. Reading is done by iterating the directory and
 * deserializing each file, sorted by filename descending (newest first).
 */
@Singleton
class HistoryService {

    private val logger = LoggerFactory.getLogger(HistoryService::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val historyDir: File by lazy {
        File(System.getProperty("user.home"), ".architect/history").also { it.mkdirs() }
    }
    private val timestampFmt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault())

    fun record(record: ExecutionRecord) {
        try {
            val timestamp = timestampFmt.format(Instant.ofEpochMilli(record.timestamp))
            val file = File(historyDir, "$timestamp-${record.id}.json")
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, record)
        } catch (e: Exception) {
            logger.warn("Failed to write execution record ${record.id}: ${e.message}")
        }
    }

    fun getAll(limit: Int = 50): List<ExecutionRecord> {
        return readRecords(limit) { true }
    }

    fun getByProject(project: String, limit: Int = 50): List<ExecutionRecord> {
        return readRecords(limit) { it.project == project }
    }

    private fun readRecords(limit: Int, predicate: (ExecutionRecord) -> Boolean): List<ExecutionRecord> {
        val files = historyDir.listFiles { f -> f.name.endsWith(".json") }
            ?: return emptyList()
        return files.sortedByDescending { it.name }
            .asSequence()
            .mapNotNull { file ->
                try {
                    objectMapper.readValue(file, ExecutionRecord::class.java)
                } catch (e: Exception) {
                    logger.warn("Failed to read history record ${file.name}: ${e.message}")
                    null
                }
            }
            .filter(predicate)
            .take(limit)
            .toList()
    }
}
