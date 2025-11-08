package io.github.architectplatform.cli.commands

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.util.concurrent.Callable

/**
 * Command for managing the Architect Engine lifecycle.
 * 
 * Provides subcommands for installing, starting, stopping, and cleaning the engine.
 */
@Command(
    name = "engine",
    description = ["Manage the Architect Engine"],
    subcommands = [
        EngineCommand.Install::class,
        EngineCommand.InstallCI::class,
        EngineCommand.Start::class,
        EngineCommand.Stop::class,
        EngineCommand.Clean::class
    ]
)
class EngineCommand : Callable<Int> {
    
    override fun call(): Int {
        println("Use 'architect engine <command>' to manage the engine")
        println("Available commands:")
        println("  install    - Install the Architect Engine")
        println("  install-ci - Install the engine for CI environments")
        println("  start      - Start the Architect Engine")
        println("  stop       - Stop the Architect Engine")
        println("  clean      - Remove all engine data")
        return 0
    }
    
    /**
     * Install command - downloads and installs the Architect Engine.
     */
    @Command(name = "install", description = ["Install the Architect Engine"])
    class Install : Callable<Int> {
        override fun call(): Int {
            println("Installing Architect Engine...")
            val command = "curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-cli/.installers/bash | bash"
            return executeShellCommand(command)
        }
    }
    
    /**
     * Install-CI command - installs the engine optimized for CI environments.
     */
    @Command(name = "install-ci", description = ["Install the Architect Engine for CI environments"])
    class InstallCI : Callable<Int> {
        override fun call(): Int {
            println("Installing Architect Engine for CI...")
            val command = "curl -sSL https://raw.githubusercontent.com/architect-platform/architect/main/architect-engine/.installers/bash-ci | bash"
            return executeShellCommand(command)
        }
    }
    
    /**
     * Start command - starts the Architect Engine as a background process.
     */
    @Command(name = "start", description = ["Start the Architect Engine"])
    class Start : Callable<Int> {
        override fun call(): Int {
            println("Starting Architect Engine...")
            val command = "architect-engine"
            return executeShellCommand(command, wait = false)
        }
    }
    
    /**
     * Stop command - stops any running Architect Engine processes.
     */
    @Command(name = "stop", description = ["Stop the Architect Engine"])
    class Stop : Callable<Int> {
        override fun call(): Int {
            println("Stopping Architect Engine...")
            val command = "pkill -f architect-engine"
            return executeShellCommand(command)
        }
    }
    
    /**
     * Clean command - removes all Architect Engine data.
     */
    @Command(name = "clean", description = ["Remove all Architect Engine data"])
    class Clean : Callable<Int> {
        override fun call(): Int {
            println("Cleaning Architect Engine...")
            val command = "rm -rf ~/.architect-engine"
            return executeShellCommand(command)
        }
    }
    
    companion object {
        /**
         * Executes a shell command using the system's runtime.
         *
         * @param command The shell command to execute
         * @param wait If true, waits for the command to complete before returning
         * @return Exit code (0 for success, non-zero for failure)
         */
        private fun executeShellCommand(command: String, wait: Boolean = true): Int {
            return try {
                val process = Runtime.getRuntime().exec(command)
                if (wait) {
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        println("✅ Command executed successfully")
                    } else {
                        println("❌ Command exited with code $exitCode")
                    }
                    exitCode
                } else {
                    println("✅ Command started in background")
                    0
                }
            } catch (e: Exception) {
                println("❌ Failed to execute command: ${e.message}")
                1
            }
        }
    }
}
