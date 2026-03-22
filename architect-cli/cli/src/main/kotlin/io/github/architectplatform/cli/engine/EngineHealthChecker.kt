package io.github.architectplatform.cli.engine

import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton
import java.net.HttpURLConnection
import java.net.URI

/**
 * Checks whether the Architect Engine daemon is reachable.
 *
 * Uses a short-timeout raw HTTP connection so the check is fast even when the
 * engine is down (no waiting for the 300s default timeout).
 */
@Singleton
open class EngineHealthChecker {

    @Property(name = "micronaut.http.services.engine.url", defaultValue = "http://localhost:9292")
    open var engineUrl: String = "http://localhost:9292"

    /**
     * Returns true if the engine's /health endpoint responds with HTTP 200.
     */
    open fun isRunning(): Boolean = try {
        val conn = URI("$engineUrl/health").toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 2_000
        conn.readTimeout = 2_000
        conn.requestMethod = "GET"
        conn.responseCode == 200
    } catch (_: Exception) {
        false
    }
}
