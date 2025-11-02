package io.github.architectplatform.engine.core.tasks.domain

import io.github.architectplatform.api.core.tasks.Task
import io.github.architectplatform.api.core.tasks.TaskRegistry

/**
 * Resolves task dependencies and determines execution order.
 * 
 * This class is responsible for:
 * - Resolving all transitive dependencies of a task
 * - Resolving child tasks (subtasks) of composite tasks
 * - Performing topological sort to determine correct execution order
 * - Detecting circular dependencies in both dependencies and child relationships
 */
class TaskDependencyResolver {

    /**
     * Resolves all transitive dependencies and children of a task.
     * 
     * This method handles both:
     * - Dependencies: Tasks that must complete before this task starts
     * - Children: Subtasks that are executed as part of this task
     * 
     * @param root The task whose dependencies and children should be resolved
     * @param taskRegistry Registry containing all available tasks
     * @return Map of task IDs to Task instances, including all dependencies and children
     * @throws IllegalArgumentException if a dependency or child is not found in the registry
     */
    fun resolveAllDependencies(root: Task, taskRegistry: TaskRegistry): Map<String, Task> {
        val resolvedTasks = mutableMapOf<String, Task>()
        val visited = mutableSetOf<String>()

        fun visit(task: Task) {
            if (!visited.add(task.id)) return
            
            // First, resolve all dependencies
            for (dependencyId in task.depends()) {
                val dependencyTask = taskRegistry.get(dependencyId)
                    ?: throw IllegalArgumentException(
                        "Task dependency '$dependencyId' not found in registry"
                    )
                visit(dependencyTask)
                resolvedTasks[dependencyId] = dependencyTask
            }
            
            // Then, resolve all children
            for (childId in task.children()) {
                val childTask = taskRegistry.get(childId)
                    ?: throw IllegalArgumentException(
                        "Child task '$childId' not found in registry"
                    )
                visit(childTask)
                resolvedTasks[childId] = childTask
            }
            
            resolvedTasks[task.id] = task
        }

        visit(root)
        return resolvedTasks
    }

    /**
     * Performs topological sort on tasks to determine execution order.
     * 
     * Tasks are ordered such that:
     * - All dependencies of a task appear before the task itself
     * - Children of a task appear after the task but before tasks that depend on the parent
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
            
            // Visit dependencies first
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

    /**
     * Resolves and orders child tasks for execution.
     * 
     * Child tasks are resolved with their own dependencies, creating a complete
     * execution plan for the composite task's children.
     * 
     * @param parentTask The parent task whose children should be resolved
     * @param taskRegistry Registry containing all available tasks
     * @return List of child tasks in execution order
     * @throws IllegalArgumentException if a child task is not found
     * @throws IllegalStateException if circular dependencies are detected
     */
    fun resolveChildren(parentTask: Task, taskRegistry: TaskRegistry): List<Task> {
        val children = parentTask.children()
        if (children.isEmpty()) {
            return emptyList()
        }

        val allChildTasks = mutableMapOf<String, Task>()
        
        // Resolve each child and its dependencies
        for (childId in children) {
            val childTask = taskRegistry.get(childId)
                ?: throw IllegalArgumentException(
                    "Child task '$childId' not found in registry (parent: ${parentTask.id})"
                )
            
            val childDeps = resolveAllDependencies(childTask, taskRegistry)
            allChildTasks.putAll(childDeps)
        }

        // Sort children and their dependencies topologically
        return topologicalSort(allChildTasks)
    }
}
