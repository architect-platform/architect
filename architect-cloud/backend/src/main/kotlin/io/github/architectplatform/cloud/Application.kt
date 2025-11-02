package io.github.architectplatform.cloud

import io.micronaut.runtime.Micronaut.run

/**
 * Architect Cloud Backend Application
 * 
 * This application provides a centralized backend where all architect engine instances
 * can report their status, execution data, and events for tracking and visualization.
 */
fun main(args: Array<String>) {
    run(*args)
}
