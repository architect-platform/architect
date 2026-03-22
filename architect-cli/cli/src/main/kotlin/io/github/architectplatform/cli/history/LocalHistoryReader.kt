package io.github.architectplatform.cli.history

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.architectplatform.cli.dto.HistoryRecordDTO
import jakarta.inject.Singleton
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Reads execution history records directly from `~/.architect/history/` without
 * requiring the engine daemon to be running.
 *
 * Files are written by the engine's `HistoryService` as `{timestamp}-{executionId}.json`.
 * This reader parses them in descending filename order (newest first).
 */
@Singleton
class LocalHistoryReader {

    private val logger = LoggerFactory.getLogger(LocalHistoryReader::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val historyDir: File by lazy {
        File(System.getProperty("user.home"), ".architect/history")
    }

    fun getAll(limit: Int = 50): List<HistoryRecordDTO> = readRecords(limit) { true }

    fun getByProject(project: String, limit: Int = 50): List<HistoryRecordDTO> =
        readRecords(limit) { it.project == project }

    private fun readRecords(limit: Int, predicate: (HistoryRecordDTO) -> Boolean): List<HistoryRecordDTO> {
        if (!historyDir.exists()) return emptyList()
        val files = historyDir.listFiles { f -> f.name.endsWith(".json") }
            ?: return emptyList()
        return files.sortedByDescending { it.name }
            .asSequence()
            .mapNotNull { file ->
                try {
                    objectMapper.readValue(file, HistoryRecordDTO::class.java)
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
