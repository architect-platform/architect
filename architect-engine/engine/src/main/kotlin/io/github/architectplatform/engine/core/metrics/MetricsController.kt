package io.github.architectplatform.engine.core.metrics

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

/**
 * Exposes collected metrics via REST endpoints.
 */
@Controller("/api/metrics")
class MetricsController(private val metricsService: MetricsService) {

  /**
   * Returns metrics in Prometheus text exposition format.
   */
  @Get
  @Produces(MediaType.TEXT_PLAIN)
  fun getMetricsPrometheus(): String = metricsService.toPrometheusFormat()

  /**
   * Returns metrics as structured JSON.
   */
  @Get("/json")
  fun getMetricsJson(): Map<String, Any> = metricsService.toMap()
}
