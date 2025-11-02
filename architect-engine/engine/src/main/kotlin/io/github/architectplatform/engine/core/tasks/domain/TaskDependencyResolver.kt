package io.github.architectplatform.engine.core.tasks.domain

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Resolves task dependencies and determines execution order.
 * 
 * This class is responsible for:
 * - Resolving all transitive dependencies of a task
 * - Performing topological sort to determine correct execution order
 * - Detecting circular dependencies
 */
class TaskDependencyResolver {

    /**
     * Resolves all transitive dependencies of a task.
     * 
     * @param root The task whose dependencies should be resolved
     * @param taskRegistry Registry containing all available tasks
     * @return Map of task IDs to Task instances, including all dependencies
     * @throws IllegalArgumentException if a dependency is not found in the registry
     */
    fun resolveAllDependencies(root: Task, taskRegistry: TaskRegistry): Map<String, Task> {
        val resolvedTasks = mutableMapOf<String, Task>()
        val visited = mutableSetOf<String>()

        fun visit(task: Task) {
            if (!visited.add(task.id)) return
            
            for (dependencyId in task.depends()) {
                val dependencyTask = taskRegistry.get(dependencyId)
                    ?: throw IllegalArgumentException(
                        "Task dependency '$dependencyId' not found in registry"
                    )
                visit(dependencyTask)
                resolvedTasks[dependencyId] = dependencyTask
            }
            resolvedTasks[task.id] = task
        }

        visit(root)
        return resolvedTasks
    }

    /**
     * Performs topological sort on tasks to determine execution order.
     * 
     * Tasks are ordered such that all dependencies of a task appear before
     * the task itself in the resulting list.
     * 
     * @param tasks Map of task IDs to Task instances
     * @return List of tasks in execution order
     * @throws IllegalStateException if a circular dependency is detected
     */
    fun topologicalSort(tasks: Map<String, Task>): List<Task> {
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val result = mutableListOf<Task>()

        fun dfs(current: Task) {
            if (current.id in visiting) {
                throw IllegalStateException(
                    "Circular dependency detected involving task: ${current.id}"
                )
            }
            
            if (!visited.add(current.id)) return
            
            visiting.add(current.id)
            
            for (dependencyId in current.depends()) {
                val dependency = tasks[dependencyId]
                    ?: throw IllegalStateException("Missing task for id $dependencyId")
                dfs(dependency)
            }
            
            visiting.remove(current.id)
            result.add(current)
        }

        tasks.values.forEach { dfs(it) }

        return result
    }
}
