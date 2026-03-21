package cli

import (
	"fmt"
	"sort"
	"strings"

	"github.com/spf13/cobra"
)

var actionsCmd = &cobra.Command{
	Use:   "actions",
	Short: "List all available actions",
	RunE:  runActions,
}

func runActions(cmd *cobra.Command, args []string) error {
	proj, err := loadProject()
	if err != nil {
		return err
	}

	actions := proj.Registry.All()
	if len(actions) == 0 {
		fmt.Println("No actions defined.")
		return nil
	}

	// Sort by ID
	sort.Slice(actions, func(i, j int) bool {
		return actions[i].ID < actions[j].ID
	})

	for _, a := range actions {
		desc := a.Description
		if desc == "" {
			if a.Run != nil {
				desc = a.Run.Command
				if len(desc) > 60 {
					desc = desc[:57] + "..."
				}
			} else if len(a.Steps) > 0 {
				desc = fmt.Sprintf("steps: [%s]", strings.Join(a.Steps, ", "))
			}
		}
		fmt.Printf("  %-20s %s\n", a.ID, desc)
	}

	return nil
}
