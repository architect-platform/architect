package io.github.architectplatform.data.application.domain

import java.time.Instant

/**
 * Domain model representing an engine instance that reports execution data.
 * Pure domain object without infrastructure concerns.
 */
data class EngineInstance(
    val id: String,
    val hostname: String,
    val port: Int,
    val version: String?,
    val status: EngineStatus = EngineStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val lastHeartbeat: Instant = Instant.now()
) {
    fun updateHeartbeat(): EngineInstance {
        return copy(lastHeartbeat = Instant.now())
    }
    
    fun markInactive(): EngineInstance {
        return copy(status = EngineStatus.INACTIVE)
    }
    
    fun markOffline(): EngineInstance {
        return copy(status = EngineStatus.OFFLINE)
    }
    
    fun reactivate(): EngineInstance {
        return copy(status = EngineStatus.ACTIVE, lastHeartbeat = Instant.now())
    }
}

enum class EngineStatus {
    ACTIVE,
    INACTIVE,
    OFFLINE
}
