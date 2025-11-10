package io.github.architectplatform.server.api

/**
 * Central definition of all API endpoints for Architect Server.
 * This ensures consistency between server and agent implementations.
 */
object ApiEndpoints {
    
    // Base API path
    const val API_BASE = "/api"
    
    // Agent endpoints
    object Agents {
        const val BASE = "$API_BASE/agents"
        const val REGISTER = "$BASE/register"
        const val HEARTBEAT = "$BASE/heartbeat"
        const val COMMANDS = "$BASE/{agentId}/commands"
        const val STATUS = "$BASE/{agentId}/status"
    }
    
    // Deployment endpoints
    object Deployments {
        const val BASE = "$API_BASE/deployments"
        const val RESULT = "$BASE/result"
        const val STATUS = "$BASE/{commandId}/status"
        const val HISTORY = "$BASE/history"
    }
    
    // Template endpoints
    object Templates {
        const val BASE = "$API_BASE/templates"
        const val BY_ID = "$BASE/{templateId}"
        const val BY_TYPE = "$BASE/type/{templateType}"
        const val VALIDATE = "$BASE/validate"
    }
    
    // Resource definition endpoints
    object Resources {
        const val BASE = "$API_BASE/resources"
        const val BY_ID = "$BASE/{resourceId}"
        const val BY_NAMESPACE = "$BASE/namespace/{namespace}"
        const val DEPLOY = "$BASE/{resourceId}/deploy"
    }
}

/**
 * HTTP header constants
 */
object ApiHeaders {
    const val AUTHORIZATION = "Authorization"
    const val AGENT_ID = "X-Agent-ID"
    const val API_VERSION = "X-API-Version"
    const val CONTENT_TYPE = "Content-Type"
}

/**
 * API version constants
 */
object ApiVersions {
    const val V1 = "v1"
    const val CURRENT = V1
}
