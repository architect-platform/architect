package io.github.architectplatform.core.secrets

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

internal object PlatformKeychainSecretStore {
    fun create(
        indexPath: Path,
        osName: String = System.getProperty("os.name"),
        executor: SecretProcessExecutor = ProcessSecretProcessExecutor(),
    ): SecretStoreBackend? = when {
        osName.lowercase().contains("mac") -> MacOsKeychainSecretStore(indexPath, executor)
        osName.lowercase().contains("linux") -> LinuxSecretToolSecretStore(indexPath, executor)
        else -> null
    }
}

internal interface SecretProcessExecutor {
    fun run(command: List<String>, input: String? = null): SecretCommandResult
}

internal class ProcessSecretProcessExecutor : SecretProcessExecutor {
    override fun run(command: List<String>, input: String?): SecretCommandResult {
        val process = try {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        } catch (exception: IOException) {
            return SecretCommandResult(exitCode = -1, output = exception.message.orEmpty())
        }

        process.outputStream.bufferedWriter().use { writer ->
            if (input != null) {
                writer.write(input)
                writer.newLine()
            }
        }

        if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return SecretCommandResult(exitCode = 124, output = "")
        }

        return SecretCommandResult(
            exitCode = process.exitValue(),
            output = process.inputStream.bufferedReader().readText().trim(),
        )
    }
}

internal abstract class IndexedNativeSecretStore(
    private val index: SecretNameIndex,
    protected val executor: SecretProcessExecutor,
) : SecretStoreBackend {
    private val available by lazy {
        val result = availabilityCheck()
        result.exitCode >= 0 && result.exitCode != 124
    }

    override fun isAvailable(): Boolean = available

    override fun set(name: String, value: String) {
        val result = executor.run(setCommand(name, value), inputForSet(value))
        check(result.exitCode == 0) {
            "Failed to store secret '$name' in native keychain: ${result.output.ifBlank { "exit ${result.exitCode}" }}"
        }
        index.add(name)
    }

    override fun get(name: String): String? {
        val result = executor.run(getCommand(name))
        return result.output.takeIf { result.exitCode == 0 && it.isNotBlank() }
    }

    override fun delete(name: String): Boolean {
        val result = executor.run(deleteCommand(name))
        if (result.exitCode == 0 || result.exitCode in missingSecretExitCodes()) {
            index.remove(name)
        }
        return result.exitCode == 0
    }

    override fun listKeys(): List<String> {
        val existingKeys = index.list().filter { get(it) != null }
        index.replace(existingKeys)
        return existingKeys
    }

    protected open fun inputForSet(value: String): String? = null

    protected open fun missingSecretExitCodes(): Set<Int> = emptySet()

    protected abstract fun availabilityCheck(): SecretCommandResult

    protected abstract fun setCommand(name: String, value: String): List<String>

    protected abstract fun getCommand(name: String): List<String>

    protected abstract fun deleteCommand(name: String): List<String>
}

internal class MacOsKeychainSecretStore(
    indexPath: Path,
    executor: SecretProcessExecutor = ProcessSecretProcessExecutor(),
    private val serviceName: String = "architect",
) : IndexedNativeSecretStore(SecretNameIndex(indexPath), executor) {
    override fun availabilityCheck(): SecretCommandResult = executor.run(listOf("security", "help"))

    override fun setCommand(name: String, value: String): List<String> =
        listOf("security", "add-generic-password", "-U", "-a", name, "-s", serviceName, "-w", value)

    override fun getCommand(name: String): List<String> =
        listOf("security", "find-generic-password", "-a", name, "-s", serviceName, "-w")

    override fun deleteCommand(name: String): List<String> =
        listOf("security", "delete-generic-password", "-a", name, "-s", serviceName)

    override fun missingSecretExitCodes(): Set<Int> = setOf(44)
}

internal class LinuxSecretToolSecretStore(
    indexPath: Path,
    executor: SecretProcessExecutor = ProcessSecretProcessExecutor(),
    private val serviceName: String = "architect",
) : IndexedNativeSecretStore(SecretNameIndex(indexPath), executor) {
    override fun availabilityCheck(): SecretCommandResult = executor.run(listOf("secret-tool", "--help"))

    override fun setCommand(name: String, value: String): List<String> =
        listOf("secret-tool", "store", "--label=Architect secret $name", "architect-service", serviceName, "name", name)

    override fun inputForSet(value: String): String = value

    override fun getCommand(name: String): List<String> =
        listOf("secret-tool", "lookup", "architect-service", serviceName, "name", name)

    override fun deleteCommand(name: String): List<String> =
        listOf("secret-tool", "clear", "architect-service", serviceName, "name", name)

    override fun missingSecretExitCodes(): Set<Int> = setOf(1)
}

internal class SecretNameIndex(
    private val indexPath: Path,
) {
    private val mapper = jacksonObjectMapper()

    fun list(): List<String> {
        if (!Files.exists(indexPath)) {
            return emptyList()
        }
        return runCatching { mapper.readValue<List<String>>(indexPath.toFile()) }
            .getOrElse { emptyList() }
            .distinct()
    }

    fun add(name: String) {
        update { it.add(name) }
    }

    fun remove(name: String): Boolean {
        var removed = false
        update {
            removed = it.remove(name)
        }
        return removed
    }

    fun replace(names: Collection<String>) {
        persist(names.toSortedSet())
    }

    private fun update(mutator: (MutableSet<String>) -> Unit) {
        val names = list().toMutableSet()
        mutator(names)
        persist(names)
    }

    private fun persist(names: Collection<String>) {
        if (names.isEmpty()) {
            Files.deleteIfExists(indexPath)
            return
        }
        Files.createDirectories(indexPath.parent)
        mapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), names.toList())
    }
}
