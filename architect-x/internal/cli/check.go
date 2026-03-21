package cli

import (
	"fmt"
	"os"

	"github.com/architect-platform/architect/internal/core/rule"
	"github.com/spf13/cobra"
)

var checkCmd = &cobra.Command{
	Use:   "check [rule-id]",
	Short: "Validate project against convention rules",
	Long:  "Run convention checks defined in rules. Use --fix to auto-fix fixable violations.",
	RunE:  runCheck,
}

var flagFix bool

func init() {
	checkCmd.Flags().BoolVar(&flagFix, "fix", false, "auto-fix fixable rule violations")
}

func runCheck(cmd *cobra.Command, args []string) error {
	proj, err := loadProject()
	if err != nil {
		return err
	}

	if proj.Config.Rules == nil || len(proj.Config.Rules) == 0 {
		fmt.Println("No rules defined.")
		return nil
	}

	engine := rule.NewEngine(proj.Dir, proj.Config.Rules)

	// If a specific rule is requested
	if len(args) > 0 {
		r, ok := proj.Config.Rules[args[0]]
		if !ok {
			return fmt.Errorf("rule %q not found", args[0])
		}
		result := engine.Evaluate(r)
		printRuleResult(result, r)
		if !result.Passed && result.Severity == rule.SeverityError {
			os.Exit(1)
		}
		return nil
	}

	// Check all rules with "always" or "manual" trigger
	results := engine.CheckAll("always")

	hasErrors := false
	fixable := 0
	for _, result := range results {
		r := proj.Config.Rules[result.RuleID]
		printRuleResult(result, r)

		if !result.Passed {
			if result.Fixable {
				fixable++
				if flagFix {
					fmt.Printf("  Fixing %s...\n", result.RuleID)
					if err := engine.Fix(r); err != nil {
						fmt.Printf("  Fix failed: %v\n", err)
					} else {
						fmt.Printf("  Fixed!\n")
					}
				}
			}
			if result.Severity == rule.SeverityError {
				hasErrors = true
			}
		}
	}

	fmt.Println()
	passed := 0
	failed := 0
	for _, r := range results {
		if r.Passed {
			passed++
		} else {
			failed++
		}
	}
	fmt.Printf("%d rules checked: %d passed, %d failed\n", len(results), passed, failed)

	if fixable > 0 && !flagFix {
		fmt.Printf("%d fixable violation(s). Run with --fix to auto-fix.\n", fixable)
	}

	if hasErrors {
		os.Exit(1)
	}

	return nil
}

func printRuleResult(result rule.Result, r *rule.Rule) {
	icon := "\033[32m✓\033[0m" // green check
	if !result.Passed {
		switch result.Severity {
		case rule.SeverityError:
			icon = "\033[31m✗\033[0m" // red x
		case rule.SeverityWarn:
			icon = "\033[33m⚠\033[0m" // yellow warning
		default:
			icon = "\033[34mℹ\033[0m" // blue info
		}
	}

	desc := r.Description
	if desc == "" {
		desc = r.ID
	}
	fmt.Printf("  %s %s", icon, desc)

	if !result.Passed && result.Message != "" {
		fmt.Printf(": %s", result.Message)
	}
	fmt.Println()

	if !result.Passed && r.Rationale != "" {
		fmt.Printf("    Rationale: %s\n", r.Rationale)
	}
}
