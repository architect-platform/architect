package cli

import (
	"github.com/spf13/cobra"
)

var (
	flagProject string
	flagFilter  string
)

var buildCmd = &cobra.Command{
	Use:   "build",
	Short: "Run the build workflow",
	Long:  "Execute the build action and all its dependencies.",
	RunE: func(cmd *cobra.Command, args []string) error {
		return runAction(cmd, []string{"build"})
	},
}

var testCmd = &cobra.Command{
	Use:   "test",
	Short: "Run the test workflow",
	Long:  "Execute the test action and all its dependencies.",
	RunE: func(cmd *cobra.Command, args []string) error {
		return runAction(cmd, []string{"test"})
	},
}

var ciCmd = &cobra.Command{
	Use:   "ci",
	Short: "Run full CI pipeline",
	Long:  "Execute the ci action (init → lint → verify → build → test).",
	RunE: func(cmd *cobra.Command, args []string) error {
		return runAction(cmd, []string{"ci"})
	},
}

var cdCmd = &cobra.Command{
	Use:   "cd",
	Short: "Run full CD pipeline",
	Long:  "Execute the cd action (init → lint → verify → build → test → release → publish).",
	RunE: func(cmd *cobra.Command, args []string) error {
		return runAction(cmd, []string{"cd"})
	},
}

func init() {
	for _, cmd := range []*cobra.Command{buildCmd, testCmd, ciCmd, cdCmd} {
		cmd.Flags().StringVar(&flagProject, "project", "", "run for a specific workspace member")
		cmd.Flags().StringVar(&flagFilter, "filter", "", "filter workspace members by glob pattern")
	}
}
