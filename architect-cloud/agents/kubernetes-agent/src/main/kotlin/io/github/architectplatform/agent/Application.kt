package io.github.architectplatform.agent

import io.micronaut.runtime.Micronaut.run

/**
 * Main entry point for the Architect Kubernetes Agent.
 * 
 * The agent is responsible for:
 * - Connecting to Architect Server
 * - Receiving deployment commands
 * - Rendering Kubernetes templates
 * - Applying manifests to the local cluster
 * - Reporting deployment status back to the server
 */
fun main(args: Array<String>) {
    run(*args)
}
