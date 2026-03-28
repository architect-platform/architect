package io.github.architectplatform.cli.command

import io.github.architectplatform.core.secrets.SecretStore

/**
 * Handles `architect secret` — local encrypted secret management.
 *
 * Subcommands:
 * - `architect secret set <name> <value>`  — stores encrypted secret
 * - `architect secret set <name>`          — prompts securely for value
 * - `architect secret get <name>`          — prints plaintext value
 * - `architect secret list`                — lists all secret names
 * - `architect secret delete <name>`       — removes a secret
 */
class SecretCommandHandler(
    private val store: SecretStore = SecretStore(),
) {
    fun handle(args: List<String>) {
        val subcommand = args.getOrNull(1) ?: run {
            printUsage()
            return
        }
        when (subcommand) {
            "set" -> handleSet(args.getOrNull(2), args.getOrNull(3))
            "get" -> handleGet(args.getOrNull(2))
            "list" -> handleList()
            "delete", "rm", "remove" -> handleDelete(args.getOrNull(2))
            else -> {
                println("Unknown secret subcommand: $subcommand")
                printUsage()
            }
        }
    }

    private fun handleSet(name: String?, value: String?) {
        if (name == null) {
            println("Usage: architect secret set <name> [value]")
            return
        }
        val secretValue = when {
            value != null -> value
            else -> {
                // Prompt securely — hide input when a real console is available
                val console = System.console()
                if (console != null) {
                    print("Enter value for '$name': ")
                    console.readPassword()?.let { String(it) } ?: run {
                        println("No input provided.")
                        return
                    }
                } else {
                    // Fallback for environments without a real console (e.g., piped input)
                    print("Enter value for '$name': ")
                    readLine() ?: run {
                        println("No input provided.")
                        return
                    }
                }
            }
        }
        store.set(name, secretValue)
        println("✅ Secret '$name' stored successfully.")
    }

    private fun handleGet(name: String?) {
        if (name == null) {
            println("Usage: architect secret get <name>")
            return
        }
        val value = store.get(name)
        if (value != null) {
            println(value)
        } else {
            println("Secret '$name' not found.")
        }
    }

    private fun handleList() {
        val keys = store.listKeys()
        if (keys.isEmpty()) {
            println("No secrets stored.")
            return
        }
        println()
        println("🔑 Stored secrets (${keys.size}):")
        println("━".repeat(40))
        keys.sorted().forEach { key ->
            println("   $key")
        }
        println()
    }

    private fun handleDelete(name: String?) {
        if (name == null) {
            println("Usage: architect secret delete <name>")
            return
        }
        if (store.delete(name)) {
            println("✅ Secret '$name' deleted.")
        } else {
            println("Secret '$name' not found.")
        }
    }

    private fun printUsage() {
        println()
        println("Usage: architect secret <subcommand> [args]")
        println()
        println("Subcommands:")
        println("   set <name> [value]   Store an encrypted secret (prompts if value omitted)")
        println("   get <name>           Print the plaintext value of a secret")
        println("   list                 List all stored secret names")
        println("   delete <name>        Delete a secret")
        println()
    }
}
