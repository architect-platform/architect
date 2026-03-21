package githooks

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

// Supported git hook names.
var SupportedHooks = []string{
	"pre-commit", "commit-msg", "pre-push",
	"post-commit", "post-merge", "pre-rebase",
}

// Install writes git hook scripts that delegate to architect.
func Install(projectDir string, hooks map[string][]string) error {
	gitDir := filepath.Join(projectDir, ".git")
	if _, err := os.Stat(gitDir); os.IsNotExist(err) {
		return fmt.Errorf("not a git repository (no .git directory)")
	}

	hooksDir := filepath.Join(gitDir, "hooks")
	if err := os.MkdirAll(hooksDir, 0755); err != nil {
		return fmt.Errorf("creating hooks directory: %w", err)
	}

	for hookName, actions := range hooks {
		if !isSupported(hookName) {
			return fmt.Errorf("unsupported hook: %s", hookName)
		}

		script := generateHookScript(hookName, actions)
		hookPath := filepath.Join(hooksDir, hookName)

		if err := os.WriteFile(hookPath, []byte(script), 0755); err != nil {
			return fmt.Errorf("writing hook %s: %w", hookName, err)
		}
	}

	return nil
}

// Uninstall removes architect-managed git hooks.
func Uninstall(projectDir string) error {
	hooksDir := filepath.Join(projectDir, ".git", "hooks")

	for _, hookName := range SupportedHooks {
		hookPath := filepath.Join(hooksDir, hookName)
		data, err := os.ReadFile(hookPath)
		if err != nil {
			continue
		}
		// Only remove hooks we created
		if strings.Contains(string(data), "# managed by architect") {
			os.Remove(hookPath)
		}
	}

	return nil
}

func generateHookScript(hookName string, actions []string) string {
	var sb strings.Builder
	sb.WriteString("#!/bin/sh\n")
	sb.WriteString("# managed by architect - do not edit manually\n\n")

	for _, actionID := range actions {
		if hookName == "commit-msg" {
			// Pass commit message file as env var
			sb.WriteString(fmt.Sprintf("COMMIT_MSG_FILE=\"$1\" architect run %s\n", actionID))
		} else {
			sb.WriteString(fmt.Sprintf("architect run %s\n", actionID))
		}
		sb.WriteString("if [ $? -ne 0 ]; then\n")
		sb.WriteString("  exit 1\n")
		sb.WriteString("fi\n\n")
	}

	return sb.String()
}

func isSupported(name string) bool {
	for _, h := range SupportedHooks {
		if h == name {
			return true
		}
	}
	return false
}
