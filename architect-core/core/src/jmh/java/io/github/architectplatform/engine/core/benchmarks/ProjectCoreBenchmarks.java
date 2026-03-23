package io.github.architectplatform.engine.core.benchmarks;

import io.github.architectplatform.api.core.project.ProjectContext;
import io.github.architectplatform.api.core.tasks.Environment;
import io.github.architectplatform.api.core.tasks.Task;
import io.github.architectplatform.api.core.tasks.TaskResult;
import io.github.architectplatform.api.core.tasks.cache.CacheDescriptor;
import io.github.architectplatform.api.core.tasks.phase.Phase;
import io.github.architectplatform.engine.core.project.app.ConfigValidator;
import io.github.architectplatform.engine.core.project.app.ValidationResult;
import io.github.architectplatform.engine.core.tasks.domain.TaskDependencyResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
public class ProjectCoreBenchmarks {

  @Benchmark
  public List<Task> topologicalSort100Tasks(TaskGraphState state) {
    return state.resolver.topologicalSort(state.tasks);
  }

  @Benchmark
  public ValidationResult validateLargeConfig(ConfigValidationState state) {
    return state.validator.validate(state.config, state.pluginContextKeys, state.plugins, Map.of());
  }

  @State(Scope.Thread)
  public static class TaskGraphState {
    TaskDependencyResolver resolver;
    Map<String, Task> tasks;

    @Setup(Level.Trial)
    public void setup() {
      resolver = new TaskDependencyResolver();
      tasks = new LinkedHashMap<>();

      for (int index = 0; index < 100; index++) {
        List<String> dependencies = new ArrayList<>();
        if (index > 0) {
          dependencies.add("task-" + (index - 1));
        }
        if (index > 4 && index % 5 == 0) {
          dependencies.add("task-" + (index - 5));
        }
        tasks.put("task-" + index, new BenchmarkTask("task-" + index, dependencies));
      }
    }
  }

  @State(Scope.Thread)
  public static class ConfigValidationState {
    ConfigValidator validator;
    Map<String, Object> config;
    List<io.github.architectplatform.api.core.plugins.ArchitectPlugin<?>> plugins;
    java.util.Set<String> pluginContextKeys;

    @Setup(Level.Trial)
    public void setup() {
      validator = new ConfigValidator();
      plugins = List.of();
      pluginContextKeys = java.util.Set.of();

      Map<String, Object> tasks = new LinkedHashMap<>();
      for (int index = 0; index < 500; index++) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("description", "Benchmark task " + index);
        task.put("run", "echo task-" + index);
        if (index > 0) {
          task.put("depends", List.of("task-" + (index - 1)));
        }
        tasks.put("task-" + index, task);
      }

      Map<String, Object> profiles = new LinkedHashMap<>();
      for (int index = 0; index < 10; index++) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("watch", Map.of("enabled", index % 2 == 0));
        profile.put("tasks", Map.of("profile-task-" + index, Map.of("run", "echo profile-" + index)));
        profiles.put("profile-" + index, profile);
      }

      config = new LinkedHashMap<>();
      config.put("$schema", "https://architect-platform.dev/schema/architect.schema.json");
      config.put("project", Map.of("name", "benchmark-project", "description", "Benchmark project"));
      config.put("tasks", tasks);
      config.put("profiles", profiles);
      config.put("watch", Map.of("paths", List.of("src", "docs")));
    }
  }

  private static final class BenchmarkTask implements Task {
    private final String id;
    private final List<String> depends;

    private BenchmarkTask(String id, List<String> depends) {
      this.id = id;
      this.depends = depends;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String description() {
      return "Benchmark task " + id;
    }

    @Override
    public Phase phase() {
      return null;
    }

    @Override
    public List<String> depends() {
      return depends;
    }

    @Override
    public List<String> children() {
      return Collections.emptyList();
    }

    @Override
    public CacheDescriptor cacheDescriptor() {
      return null;
    }

    @Override
    public boolean requiresConfirmation() {
      return false;
    }

    @Override
    public TaskResult execute(Environment environment, ProjectContext projectContext, List<String> args) {
      return TaskResult.Companion.success("ok", Collections.emptyList());
    }
  }
}