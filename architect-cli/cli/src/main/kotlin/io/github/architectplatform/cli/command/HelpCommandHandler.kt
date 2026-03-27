package io.github.architectplatform.cli.command

/**
 * Handles `architect help` and provides structured documentation for all commands.
 *
 * Supports:
 * - `architect help` — overview of all commands
 * - `architect help <command>` — detailed help for a specific command
 * - `architect help tasks` — explains the task system
 * - `architect help plugins` — explains the plugin ecosystem
 */
class HelpCommandHandler {

  fun handle(args: List<String>) {
    val topic = args.getOrNull(1)
    when (topic) {
      null, "" -> printOverview()
      "tasks" -> printTasksHelp()
      "plugins" -> printPluginsHelp()
      "init" -> printCommandHelp("init", INIT_HELP)
      "engine" -> printCommandHelp("engine", ENGINE_HELP)
      "plugin" -> printCommandHelp("plugin", PLUGIN_HELP)
      "cache" -> printCommandHelp("cache", CACHE_HELP)
      "check" -> printCommandHelp("check", CHECK_HELP)
      "plan" -> printCommandHelp("plan", PLAN_HELP)
      "graph" -> printCommandHelp("graph", GRAPH_HELP)
      "history" -> printCommandHelp("history", HISTORY_HELP)
      "affected" -> printCommandHelp("affected", AFFECTED_HELP)
      "validate" -> printCommandHelp("validate", VALIDATE_HELP)
      "info" -> printCommandHelp("info", INFO_HELP)
      "config" -> printCommandHelp("config", CONFIG_HELP)
      "doctor" -> printCommandHelp("doctor", DOCTOR_HELP)
      "completion" -> printCommandHelp("completion", COMPLETION_HELP)
      "upgrade" -> printCommandHelp("upgrade", UPGRADE_HELP)
      else -> {
        println("Unknown topic: $topic")
        println()
        printOverview()
      }
    }
  }

  private fun printOverview() {
    println(
      """
      |ARCHITECT — Plugin-based task execution framework
      |
      |USAGE
      |  architect [options] <command> [args...]
      |  architect [options] <task-name> [task-args...]
      |
      |COMMANDS
      |  help [topic]       Show help (this screen) or help for a topic
      |  tasks              List all registered tasks
      |  info               Show project information
      |  plan <task>        Show execution plan for a task
      |  graph [task]       Generate dependency graph (DOT format)
      |  validate           Validate project configuration
      |  check              Run pre-flight checks
      |  history            Show task execution history
      |  affected           List projects affected by recent changes
      |  engine             Manage the Architect engine daemon
      |  plugin             Manage plugins (validate, create, search, install)
      |  cache              Manage the task output cache
      |  completion         Generate shell completions
      |  upgrade            Upgrade the Architect CLI
      |
      |GLOBAL OPTIONS
      |  --plain            Plain output (no colors, no spinners)
      |  --json             JSON output mode
      |  --no-daemon        Skip engine daemon (embedded execution)
      |  --embedded         Force embedded execution mode
      |  --env <profile>    Environment profile (e.g., staging, ci)
      |  --watch            Re-run task on file changes
      |  --no-cache         Disable output caching
      |  --no-color         Disable colored output
      |  --version          Print version and exit
      |  --filter <glob>    Filter tasks by pattern
      |  --affected         Only run on affected projects (monorepo)
      |
      |EXAMPLES
      |  architect tasks                    List all available tasks
      |  architect build                    Run the 'build' task
      |  architect test --watch             Run tests, re-run on changes
      |  architect --env ci build           Run build with CI profile
      |  architect plan build --tree        Show build execution plan as tree
      |  architect help tasks               Learn about the task system
      |  architect help plugins             Learn about the plugin ecosystem
      |
      |HELP TOPICS
      |  architect help tasks               How the task system works
      |  architect help plugins             How plugins work
      |  architect help <command>           Detailed help for any command
      """.trimMargin()
    )
  }

  private fun printTasksHelp() {
    println(
      """
      |ARCHITECT TASK SYSTEM
      |
      |Tasks are the fundamental building blocks of Architect. Each task represents
      |a discrete unit of work that can be executed as part of a project's lifecycle.
      |
      |TASK TYPES
      |  SimpleTask          Basic task with no arguments
      |  TaskWithArgs         Task that accepts command-line arguments
      |  CompositeTask        Parent task that orchestrates child tasks
      |  ConfigurableTask     Task with key-value configuration
      |
      |LIFECYCLE PHASES
      |  Tasks can belong to lifecycle phases that define execution order:
      |
      |  INIT → LINT → VERIFY → BUILD → TEST → RUN → RELEASE → PUBLISH
      |
      |  Tasks in the same phase run after all tasks in earlier phases complete.
      |
      |DEPENDENCIES
      |  Tasks can declare explicit dependencies on other tasks:
      |  - Phase dependencies: inherited from the task's phase
      |  - Custom dependencies: explicitly declared in task definition
      |  - Transitive: if A depends on B and B depends on C, C runs first
      |
      |TASK FEATURES
      |  shouldExecute()     Runtime condition — skip tasks dynamically
      |  onFailure()         Failure strategy — ABORT, CONTINUE, or RETRY
      |  timeout()           Per-task timeout override
      |  cacheDescriptor()   Content-hash output caching
      |  requiredPermissions()  Declare required permissions
      |
      |EXECUTION
      |  Tasks are resolved into a dependency graph, sorted topologically,
      |  grouped into batches, and executed. Tasks in the same batch run
      |  in parallel when parallel execution is enabled.
      |
      |EXAMPLES
      |  architect tasks                    List all tasks
      |  architect build                    Execute the 'build' task
      |  architect test -- --verbose        Pass args to test task
      |  architect plan build               Show execution plan
      |  architect plan build --tree        Show plan as dependency tree
      """.trimMargin()
    )
  }

  private fun printPluginsHelp() {
    println(
      """
      |ARCHITECT PLUGIN SYSTEM
      |
      |Plugins extend Architect with new tasks, phases, and integrations.
      |They are declared in architect.yml and loaded at project registration time.
      |
      |PLUGIN DECLARATION (architect.yml)
      |  plugins:
      |    - name: git-architected
      |      repo: architect-platform/architect
      |    - name: gradle-architected
      |      repo: architect-platform/architect
      |      version: "1.2.0"
      |
      |PLUGIN TYPES
      |  github     Downloaded from GitHub Releases (default)
      |  local      Loaded from local filesystem
      |  maven      Downloaded from Maven repository
      |
      |OFFICIAL PLUGINS
      |  git-architected          Git operations (status, commit, push)
      |  github-architected       GitHub CI/CD and release automation
      |  gradle-architected       Gradle build integration
      |  javascript-architected   npm/yarn/pnpm integration
      |  security-architected     Security scanning, dependency audits, and SBOM generation
      |  testing-architected      Cross-language testing and coverage orchestration
      |  docs-architected         Documentation (MkDocs, Docusaurus, VuePress)
      |  scripts-architected      Custom shell script execution
      |  pipelines-architected    Pipeline/workflow management
      |  architecture-architected Architecture validation rules
      |
      |PLUGIN MANAGEMENT
      |  architect plugin search <query>    Search plugin registry
      |  architect plugin install <name>    Install a plugin
      |  architect plugin validate <path>   Validate a plugin JAR
      |  architect plugin test <path>       Run plugin contract checks on a JAR
      |  architect plugin create <name>     Scaffold a new plugin
      |  architect plugin docs <path>       Generate plugin documentation
      |
      |DEVELOPING PLUGINS
      |  1. Implement ArchitectPlugin<ContextType> interface
      |  2. Register tasks in register(registry: TaskRegistry)
      |  3. Create META-INF/services SPI entry
      |  4. Build as JAR and publish to GitHub Releases
      |
      |  See: architect plugin create my-plugin  (scaffolds a new plugin)
      """.trimMargin()
    )
  }

  private fun printCommandHelp(command: String, help: String) {
    println(help)
  }

  companion object {
    private val INIT_HELP = """
      |architect init — Initialize a new Architect project
      |
      |USAGE
      |  architect init [options]
      |
      |DESCRIPTION
      |  Interactively scaffolds a new architect.yml configuration file.
      |  Detects the project stack (Gradle, npm, Cargo, etc.) and suggests
      |  appropriate plugins.
      |
      |OPTIONS
      |  --yes              Accept all defaults (non-interactive)
      |
      |EXAMPLES
      |  architect init                     Interactive setup
      |  architect init --yes               Auto-detect and generate config
    """.trimMargin()

    private val ENGINE_HELP = """
      |architect engine — Manage the Architect engine daemon
      |
      |USAGE
      |  architect engine <subcommand>
      |
      |SUBCOMMANDS
      |  start              Start the engine daemon
      |  stop               Stop the engine daemon
      |  restart            Restart the engine daemon
      |  status             Show engine status
      |  logs               Stream engine logs
      |
      |EXAMPLES
      |  architect engine start             Start the daemon
      |  architect engine status            Check if engine is running
      |  architect engine logs              Stream live engine logs
    """.trimMargin()

    private val PLUGIN_HELP = """
      |architect plugin — Manage Architect plugins
      |
      |USAGE
      |  architect plugin <subcommand> [args...]
      |
      |SUBCOMMANDS
      |  search <query>     Search the plugin registry
      |  install <name>     Install a plugin
      |  validate <path>    Validate a plugin JAR file
      |  test <path>        Run contract checks against a plugin JAR
      |  create <name>      Scaffold a new plugin project
      |  docs <path>        Generate plugin documentation
      |
      |EXAMPLES
      |  architect plugin search git        Search for git-related plugins
      |  architect plugin validate ./my-plugin.jar
      |  architect plugin test ./my-plugin.jar
      |  architect plugin create my-plugin  Scaffold new plugin
    """.trimMargin()

    private val CACHE_HELP = """
      |architect cache — Manage the task output cache
      |
      |USAGE
      |  architect cache <subcommand>
      |
      |SUBCOMMANDS
      |  clear              Clear all cached task outputs
      |  stats              Show cache statistics
      |
      |EXAMPLES
      |  architect cache clear              Clear the cache
      |  architect cache stats              Show hit/miss statistics
    """.trimMargin()

    private val CHECK_HELP = """
      |architect check — Run pre-flight checks
      |
      |USAGE
      |  architect check [options]
      |
      |DESCRIPTION
      |  Validates the project configuration, checks tool availability,
      |  and verifies plugin compatibility.
      |
      |EXAMPLES
      |  architect check                    Run all pre-flight checks
    """.trimMargin()

    private val PLAN_HELP = """
      |architect plan — Show execution plan for a task
      |
      |USAGE
      |  architect plan <task> [--tree]
      |
      |DESCRIPTION
      |  Displays the resolved execution plan including all dependencies
      |  without actually executing anything.
      |
      |OPTIONS
      |  --tree             Display plan as a dependency tree
      |
      |EXAMPLES
      |  architect plan build               Show build execution plan
      |  architect plan build --tree        Show plan as tree
      |  architect plan test                Show test execution plan
    """.trimMargin()

    private val GRAPH_HELP = """
      |architect graph — Generate dependency graph
      |
      |USAGE
      |  architect graph [task] [--open]
      |
      |DESCRIPTION
      |  Generates a DOT-format dependency graph for tasks.
      |  If a task is specified, shows only that task's subgraph.
      |
      |OPTIONS
      |  --open             Open graph in default viewer
      |
      |EXAMPLES
      |  architect graph                    Full task graph
      |  architect graph build              Graph for build task
      |  architect graph --open             Open in viewer
    """.trimMargin()

    private val HISTORY_HELP = """
      |architect history — Show task execution history
      |
      |USAGE
      |  architect history
      |
      |DESCRIPTION
      |  Displays recent task execution results including timestamps,
      |  durations, and success/failure status.
      |
      |EXAMPLES
      |  architect history                  Show recent executions
    """.trimMargin()

    private val AFFECTED_HELP = """
      |architect affected — List affected projects
      |
      |USAGE
      |  architect affected [--base <ref>]
      |
      |DESCRIPTION
      |  In monorepo setups, determines which projects are affected
      |  by recent changes (compared to base ref).
      |
      |OPTIONS
      |  --base <ref>       Git ref to compare against (default: main)
      |
      |EXAMPLES
      |  architect affected                 List affected projects
      |  architect affected --base develop  Compare against develop
    """.trimMargin()

    private val VALIDATE_HELP = """
      |architect validate — Validate project configuration
      |
      |USAGE
      |  architect validate
      |
      |DESCRIPTION
      |  Validates the architect.yml configuration against the schema,
      |  checks plugin references, and verifies task dependencies.
      |
      |EXAMPLES
      |  architect validate                 Validate current project
    """.trimMargin()

    private val INFO_HELP = """
      |architect info — Show project information
      |
      |USAGE
      |  architect info
      |
      |DESCRIPTION
      |  Displays project metadata, loaded plugins, registered tasks,
      |  and configuration summary.
      |
      |EXAMPLES
      |  architect info                     Show project details
    """.trimMargin()

    private val CONFIG_HELP = """
      |architect config — Manage project configuration
      |
      |USAGE
      |  architect config <subcommand> [args...]
      |
      |SUBCOMMANDS
      |  show               Display resolved configuration
      |  get <key>          Read a specific config value
      |  set <key> <value>  Set a config value in architect.yml
      |  diff               Show diff between profiles
      |  validate           Validate config against schema
      |
      |EXAMPLES
      |  architect config show              Show merged config
      |  architect config get project.name  Read project name
      |  architect config diff              Compare profiles
    """.trimMargin()

    private val DOCTOR_HELP = """
      |architect doctor — Diagnose project setup
      |
      |USAGE
      |  architect doctor [--fix]
      |
      |DESCRIPTION
      |  Runs diagnostic checks: engine status, plugin health, config
      |  validity, required tools, version compatibility.
      |
      |OPTIONS
      |  --fix              Attempt auto-remediation of issues
      |
      |EXAMPLES
      |  architect doctor                   Run diagnostics
      |  architect doctor --fix             Fix detected issues
    """.trimMargin()

    private val COMPLETION_HELP = """
      |architect completion — Generate shell completions
      |
      |USAGE
      |  architect completion <shell>
      |
      |DESCRIPTION
      |  Generates auto-completion scripts for your shell.
      |
      |SHELLS
      |  bash               Bash completion script
      |  zsh                Zsh completion script
      |  fish               Fish completion script
      |
      |EXAMPLES
      |  architect completion bash >> ~/.bashrc
      |  architect completion zsh >> ~/.zshrc
      |  architect completion fish > ~/.config/fish/completions/architect.fish
    """.trimMargin()

    private val UPGRADE_HELP = """
      |architect upgrade — Upgrade the Architect CLI
      |
      |USAGE
      |  architect upgrade
      |
      |DESCRIPTION
      |  Checks for and installs the latest version of the Architect CLI.
      |
      |EXAMPLES
      |  architect upgrade                  Upgrade to latest version
    """.trimMargin()
  }
}
