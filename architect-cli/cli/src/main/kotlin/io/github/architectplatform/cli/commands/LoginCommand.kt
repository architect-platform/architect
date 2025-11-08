package io.github.architectplatform.cli.commands

import io.github.architectplatform.cli.client.EngineCommandClient
import io.github.architectplatform.cli.dto.LoginRequest
import jakarta.inject.Inject
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

/**
 * Command for managing authentication with external services.
 * 
 * Provides subcommands for GitHub authentication, status checking, and logout.
 */
@Command(
    name = "login",
    description = ["Manage authentication with external services"],
    subcommands = [
        LoginCommand.GitHub::class,
        LoginCommand.Status::class,
        LoginCommand.Logout::class
    ]
)
class LoginCommand : Callable<Int> {
    
    override fun call(): Int {
        println("Use 'architect login <command>' to manage authentication")
        println("Available commands:")
        println("  github - Authenticate with GitHub")
        println("  status - Check authentication status")
        println("  logout - Remove stored authentication")
        return 0
    }
    
    /**
     * GitHub login command - authenticates with GitHub using a Personal Access Token.
     */
    @Command(name = "github", description = ["Authenticate with GitHub"])
    class GitHub : Callable<Int> {
        
        @Inject
        lateinit var engineCommandClient: EngineCommandClient
        
        override fun call(): Int {
            println()
            println("━".repeat(80))
            println("🔐 GitHub Authentication")
            println("━".repeat(80))
            println()
            println("To authenticate with GitHub, you need a Personal Access Token (PAT).")
            println("This token will be used to avoid rate limiting when fetching plugins.")
            println()
            println("To create a token:")
            println("1. Go to https://github.com/settings/tokens")
            println("2. Click 'Generate new token' → 'Generate new token (classic)'")
            println("3. Give it a name (e.g., 'Architect CLI')")
            println("4. Select scopes: 'repo' (for private repos) or 'public_repo' (for public only)")
            println("5. Click 'Generate token' and copy the token")
            println()
            print("Enter your GitHub token (input will be hidden): ")
            
            // Read token securely (without echoing to console)
            val token = System.console()?.readPassword()?.let { String(it) }
                ?: readLine()?.trim() // Fallback for non-console environments
            
            if (token.isNullOrBlank()) {
                println()
                println("❌ No token provided. Authentication cancelled.")
                return 1
            }
            
            println()
            println("Storing token securely...")
            
            return try {
                val request = LoginRequest(token = token)
                val response = engineCommandClient.login("github", request)
                
                if (response.success) {
                    println()
                    println("✅ ${response.message}")
                    println()
                    println("You can now use Architect without GitHub rate limits!")
                    println("Your token is stored securely in ~/.architect-engine/config.yml")
                    0
                } else {
                    println()
                    println("❌ Failed to store token: ${response.message}")
                    1
                }
            } catch (e: Exception) {
                println()
                println("❌ Failed to communicate with Architect Engine")
                println("Error: ${e.message}")
                println()
                println("Make sure the engine is running: architect engine start")
                1
            }
        }
    }
    
    /**
     * Status command - checks the current authentication status.
     */
    @Command(name = "status", description = ["Check authentication status"])
    class Status : Callable<Int> {
        
        @Inject
        lateinit var engineCommandClient: EngineCommandClient
        
        override fun call(): Int {
            return try {
                val status = engineCommandClient.getStatus("github")
                println()
                if (status.authenticated) {
                    println("✅ Authenticated with GitHub")
                } else {
                    println("❌ Not authenticated with GitHub")
                    println()
                    println("Run 'architect login github' to authenticate")
                }
                println()
                0
            } catch (e: Exception) {
                println()
                println("❌ Failed to check authentication status")
                println("Error: ${e.message}")
                println()
                println("Make sure the engine is running: architect engine start")
                1
            }
        }
    }
    
    /**
     * Logout command - removes stored authentication.
     */
    @Command(name = "logout", description = ["Remove stored authentication"])
    class Logout : Callable<Int> {
        
        @Inject
        lateinit var engineCommandClient: EngineCommandClient
        
        override fun call(): Int {
            return try {
                val response = engineCommandClient.logout("github")
                println()
                if (response.success) {
                    println("✅ ${response.message}")
                } else {
                    println("❌ ${response.message}")
                }
                println()
                0
            } catch (e: Exception) {
                println()
                println("❌ Failed to logout")
                println("Error: ${e.message}")
                println()
                println("Make sure the engine is running: architect engine start")
                1
            }
        }
    }
}
