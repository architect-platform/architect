package io.github.architectplatform.agent.dockercompose

import io.micronaut.runtime.Micronaut.run

/**
 * Main entry point for Architect Docker Compose Agent.
 * 
 * The agent is responsible for:
 * - Connecting to Architect Server
 * - Receiving deployment commands
 * - Rendering Docker Compose templates
 * - Deploying using docker-compose
 * - Reporting deployment status
 */
fun main(args: Array<String>) {
    run(*args)
}
