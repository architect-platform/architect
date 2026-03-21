package cli

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spf13/cobra"
)

var pluginCmd = &cobra.Command{
	Use:   "plugin",
	Short: "Plugin management",
}

var pluginListCmd = &cobra.Command{
	Use:   "list",
	Short: "List loaded plugins",
	RunE: func(cmd *cobra.Command, args []string) error {
		proj, err := loadProject()
		if err != nil {
			return err
		}

		plugins := proj.PluginRegistry.All()
		if len(plugins) == 0 {
			fmt.Println("No plugins loaded.")
			return nil
		}

		for _, p := range plugins {
			ver := p.Version
			if ver == "" {
				ver = "dev"
			}
			desc := p.Description
			if desc == "" {
				desc = "-"
			}
			fmt.Printf("  %-25s %s  %s\n", p.ID, ver, desc)
		}
		return nil
	},
}

var pluginCreateCmd = &cobra.Command{
	Use:   "create <name>",
	Short: "Scaffold a new plugin",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		name := args[0]
		dir := filepath.Join("plugins", name)

		if err := os.MkdirAll(dir, 0755); err != nil {
			return err
		}

		content := fmt.Sprintf(`id: %s
version: "1.0.0"
description: "A custom plugin"

# config:
#   type: object
#   properties:
#     setting:
#       type: string
#       default: "value"

actions:
  %s-action:
    description: "An example action"
    # attach_to: build
    run:
      command: "echo 'Hello from %s plugin'"

# rules:
#   %s-rule:
#     severity: warn
#     trigger: [always]
#     check:
#       command: "test -f README.md"
`, name, name, name, name)

		pluginPath := filepath.Join(dir, "plugin.yml")
		if err := os.WriteFile(pluginPath, []byte(content), 0644); err != nil {
			return err
		}

		fmt.Printf("Created plugin scaffold at %s\n", dir)
		return nil
	},
}

func init() {
	pluginCmd.AddCommand(pluginListCmd)
	pluginCmd.AddCommand(pluginCreateCmd)
}
