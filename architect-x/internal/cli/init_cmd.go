package cli

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spf13/cobra"
)

var initCmd = &cobra.Command{
	Use:   "init",
	Short: "Initialize a new architect project",
	Long:  "Create an architect.yml file in the current directory.",
	RunE:  runInit,
}

func runInit(cmd *cobra.Command, args []string) error {
	dir, err := os.Getwd()
	if err != nil {
		return err
	}

	configPath := filepath.Join(dir, "architect.yml")
	if _, err := os.Stat(configPath); err == nil {
		return fmt.Errorf("architect.yml already exists in %s", dir)
	}

	projectName := filepath.Base(dir)

	content := fmt.Sprintf(`project:
  name: %s
  type: application

actions:
  build:
    description: "Build the project"
    steps: []

  test:
    description: "Run tests"
    steps: []

  lint:
    description: "Run linters"
    steps: []
`, projectName)

	if err := os.WriteFile(configPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("writing architect.yml: %w", err)
	}

	fmt.Printf("Created architect.yml for project '%s'\n", projectName)
	return nil
}
