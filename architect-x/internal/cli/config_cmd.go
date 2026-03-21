package cli

import (
	"encoding/json"
	"fmt"

	"github.com/spf13/cobra"
	"gopkg.in/yaml.v3"
)

var configCmd = &cobra.Command{
	Use:   "config",
	Short: "Configuration management",
}

var configShowCmd = &cobra.Command{
	Use:   "show",
	Short: "Show resolved configuration",
	RunE:  runConfigShow,
}

func init() {
	configCmd.AddCommand(configShowCmd)
}

func runConfigShow(cmd *cobra.Command, args []string) error {
	proj, err := loadProject()
	if err != nil {
		return err
	}

	if flagJSON {
		data, err := json.MarshalIndent(proj.Config, "", "  ")
		if err != nil {
			return err
		}
		fmt.Println(string(data))
	} else {
		data, err := yaml.Marshal(proj.Config)
		if err != nil {
			return err
		}
		fmt.Print(string(data))
	}

	return nil
}
