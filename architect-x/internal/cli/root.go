package cli

import (
	"fmt"
	"os"

	"github.com/spf13/cobra"
)

var (
	flagDryRun   bool
	flagVerbose  bool
	flagQuiet    bool
	flagPlain    bool
	flagJSON     bool
	flagEnv      string
	flagParallel int
	flagSet      []string
)

var rootCmd = &cobra.Command{
	Use:   "architect",
	Short: "Architect - A powerful, plugin-based project automation tool",
	Long: `Architect is a single-binary project automation tool that uses
a plugin-based action system with DAG execution for building,
testing, and deploying software projects.`,
	SilenceUsage:  true,
	SilenceErrors: true,
}

func init() {
	rootCmd.PersistentFlags().BoolVar(&flagDryRun, "dry-run", false, "show plan without executing")
	rootCmd.PersistentFlags().BoolVarP(&flagVerbose, "verbose", "v", false, "verbose output")
	rootCmd.PersistentFlags().BoolVarP(&flagQuiet, "quiet", "q", false, "errors only")
	rootCmd.PersistentFlags().BoolVar(&flagPlain, "plain", false, "CI-friendly plain output")
	rootCmd.PersistentFlags().BoolVar(&flagJSON, "json", false, "structured JSON output")
	rootCmd.PersistentFlags().StringVar(&flagEnv, "env", "", "select environment")
	rootCmd.PersistentFlags().IntVar(&flagParallel, "parallel", 0, "max concurrent actions")
	rootCmd.PersistentFlags().StringSliceVar(&flagSet, "set", nil, "override config (key=value)")

	rootCmd.AddCommand(runCmd)
	rootCmd.AddCommand(validateCmd)
	rootCmd.AddCommand(actionsCmd)
	rootCmd.AddCommand(buildCmd)
	rootCmd.AddCommand(testCmd)
	rootCmd.AddCommand(ciCmd)
	rootCmd.AddCommand(cdCmd)
	rootCmd.AddCommand(configCmd)
	rootCmd.AddCommand(initCmd)
	rootCmd.AddCommand(versionCmd)
	rootCmd.AddCommand(graphCmd)
	rootCmd.AddCommand(checkCmd)
	rootCmd.AddCommand(pluginCmd)
	rootCmd.AddCommand(daemonCmd)
	rootCmd.AddCommand(watchCmd)
}

// Execute runs the CLI.
func Execute() error {
	if err := rootCmd.Execute(); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		return err
	}
	return nil
}

// parseOverrides converts --set key=value flags to a map.
func parseOverrides() map[string]string {
	m := make(map[string]string)
	for _, s := range flagSet {
		for i := 0; i < len(s); i++ {
			if s[i] == '=' {
				m[s[:i]] = s[i+1:]
				break
			}
		}
	}
	return m
}
