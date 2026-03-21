package cli

import (
	"fmt"

	"github.com/spf13/cobra"
)

var daemonCmd = &cobra.Command{
	Use:   "daemon",
	Short: "Manage the background daemon",
	Long:  "The daemon provides persistent caching, file watching, and warm plugin processes.",
}

var daemonStartCmd = &cobra.Command{
	Use:   "start",
	Short: "Start the background daemon",
	RunE: func(cmd *cobra.Command, args []string) error {
		// TODO: implement daemon with gRPC/Unix socket
		fmt.Println("Daemon support is not yet implemented.")
		fmt.Println("All commands work in-process without the daemon.")
		return nil
	},
}

var daemonStopCmd = &cobra.Command{
	Use:   "stop",
	Short: "Stop the background daemon",
	RunE: func(cmd *cobra.Command, args []string) error {
		fmt.Println("Daemon is not running.")
		return nil
	},
}

var daemonStatusCmd = &cobra.Command{
	Use:   "status",
	Short: "Check daemon status",
	RunE: func(cmd *cobra.Command, args []string) error {
		fmt.Println("Daemon: not running")
		fmt.Println("All commands run in-process.")
		return nil
	},
}

var watchCmd = &cobra.Command{
	Use:   "watch <action>",
	Short: "Run action on file changes (requires daemon)",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		fmt.Println("Watch mode requires the daemon. Start it with: architect daemon start")
		return nil
	},
}

func init() {
	daemonCmd.AddCommand(daemonStartCmd)
	daemonCmd.AddCommand(daemonStopCmd)
	daemonCmd.AddCommand(daemonStatusCmd)
}
