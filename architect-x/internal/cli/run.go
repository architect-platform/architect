package cli

import (
	"context"
	"fmt"
	"os"

	"github.com/architect-platform/architect/internal/core/config"
	"github.com/architect-platform/architect/internal/core/project"
	"github.com/architect-platform/architect/internal/runtime/executor"
	"github.com/architect-platform/architect/internal/runtime/output"
	"github.com/architect-platform/architect/pkg/events"
	"github.com/spf13/cobra"
)

var runCmd = &cobra.Command{
	Use:   "run <action>",
	Short: "Run an action",
	Long:  "Execute a specific action defined in your project configuration.",
	Args:  cobra.ExactArgs(1),
	RunE:  runAction,
}

func runAction(cmd *cobra.Command, args []string) error {
	actionID := args[0]

	proj, err := loadProject()
	if err != nil {
		return err
	}

	// Apply interpolation
	interpolator := config.NewInterpolator(proj.Config)
	for _, a := range proj.Registry.All() {
		if a.Run != nil {
			a.Run.Command = interpolator.Interpolate(a.Run.Command)
		}
	}

	// Build executor
	exec := executor.New(proj.Registry)
	exec.DryRun = flagDryRun
	if flagParallel > 0 {
		exec.MaxParallel = flagParallel
	}

	// Add middleware
	exec.Middleware = append(exec.Middleware, executor.TimeoutMiddleware())
	exec.Middleware = append(exec.Middleware, executor.RetryMiddleware())
	if flagVerbose {
		exec.Middleware = append(exec.Middleware, executor.LoggingMiddleware(true))
	}

	// Output mode
	mode := output.ModeRich
	if flagPlain {
		mode = output.ModePlain
	} else if flagJSON {
		mode = output.ModeJSON
	} else if flagQuiet {
		mode = output.ModeQuiet
	}
	printer := output.NewPrinter(mode)

	// Subscribe to events for live output
	exec.EventBus.Subscribe(func(e events.Event) {
		switch e.Type {
		case events.ActionStarted:
			printer.ActionStart(e.ActionID)
		}
	})

	ctx := context.Background()
	results, err := exec.Execute(ctx, actionID)
	if err != nil {
		return err
	}

	if flagDryRun {
		printer.PrintPlan(results)
		return nil
	}

	// Print results for completed actions
	for _, r := range results {
		printer.ActionDone(r)
	}
	printer.Summary(results)

	// Check for failures
	for _, r := range results {
		if !r.Success && !r.Skipped {
			os.Exit(1)
		}
	}

	return nil
}

func loadProject() (*project.Project, error) {
	dir, err := config.FindProjectDir()
	if err != nil {
		return nil, err
	}
	return project.Load(dir, flagEnv, parseOverrides())
}

// graphCmd shows the action dependency graph.
var graphCmd = &cobra.Command{
	Use:   "graph [action]",
	Short: "Show action dependency graph",
	Args:  cobra.MaximumNArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		proj, err := loadProject()
		if err != nil {
			return err
		}

		actionID := "build"
		if len(args) > 0 {
			actionID = args[0]
		}

		dag, err := executor.BuildDAG(proj.Registry, actionID)
		if err != nil {
			return err
		}

		dot, _ := cmd.Flags().GetBool("dot")
		if dot {
			return printDOT(dag)
		}

		// Default: print topological order
		order := dag.TopologicalOrder()
		for i, a := range order {
			deps := ""
			if len(a.DependsOn) > 0 {
				deps = fmt.Sprintf(" (depends on: %v)", a.DependsOn)
			}
			fmt.Printf("  %d. %s%s\n", i+1, a.ID, deps)
		}
		return nil
	},
}

func init() {
	graphCmd.Flags().Bool("dot", false, "output as DOT for visualization")
}

func printDOT(dag *executor.DAG) error {
	fmt.Println("digraph architect {")
	fmt.Println("  rankdir=LR;")
	for _, node := range dag.Nodes {
		for _, child := range node.Children {
			fmt.Printf("  %q -> %q;\n", node.Action.ID, child.Action.ID)
		}
	}
	fmt.Println("}")
	return nil
}
