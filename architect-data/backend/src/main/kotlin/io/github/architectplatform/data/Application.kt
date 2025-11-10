package io.github.architectplatform.data

import io.micronaut.runtime.Micronaut

/**
 * Architect Data - Workflow Execution Tracking Service
 * 
 * This service tracks and monitors:
 * - Engine instances and their health
 * - Project registrations across engines
 * - Workflow execution events and history
 * - Real-time event streaming
 */
fun main(args: Array<String>) {
    Micronaut.run(Application::class.java, *args)
}

class Application
