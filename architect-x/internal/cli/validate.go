package cli

import (
	"fmt"

	"github.com/architect-platform/architect/internal/core/config"
	"github.com/spf13/cobra"
)

var validateCmd = &cobra.Command{
	Use:   "validate",
	Short: "Validate project configuration",
	Long:  "Check architect.yml and fragment configs for errors and typos.",
	RunE:  runValidate,
}

func runValidate(cmd *cobra.Command, args []string) error {
	dir, err := config.FindProjectDir()
	if err != nil {
		return err
	}

	validator := config.NewValidator()
	errors, err := validator.ValidateProject(dir)
	if err != nil {
		return err
	}

	if len(errors) == 0 {
		fmt.Println("Configuration is valid.")
		return nil
	}

	hasErrors := false
	for _, e := range errors {
		fmt.Println(e.String())
		if e.Level == "error" {
			hasErrors = true
		}
	}

	if hasErrors {
		return fmt.Errorf("validation failed with %d error(s)", len(errors))
	}

	return nil
}
