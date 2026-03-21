package config

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v3"
)

// ValidationError represents a config validation issue.
type ValidationError struct {
	File     string
	Line     int
	Level    string // error, warning
	Message  string
	Suggest  string
}

func (e *ValidationError) String() string {
	loc := e.File
	if e.Line > 0 {
		loc = fmt.Sprintf("%s:%d", e.File, e.Line)
	}
	level := e.Level
	if len(level) > 0 {
		level = strings.ToUpper(level[:1]) + level[1:]
	}
	s := fmt.Sprintf("%s - %s: %s", loc, level, e.Message)
	if e.Suggest != "" {
		s += fmt.Sprintf(" (did you mean '%s'?)", e.Suggest)
	}
	return s
}

// Validator checks config files for errors.
type Validator struct {
	knownTopLevel []string
	knownActionFields []string
}

// NewValidator creates a config validator.
func NewValidator() *Validator {
	return &Validator{
		knownTopLevel: []string{
			"project", "plugins", "actions", "rules", "hooks",
			"environments", "workspace", "cloud", "middleware",
		},
		knownActionFields: []string{
			"id", "description", "plugin", "run", "steps",
			"depends_on", "attach_to", "parallel", "condition",
			"continue_on_error", "retry", "timeout", "cache",
			"inputs", "env",
		},
	}
}

// ValidateFile checks a YAML config file for issues.
func (v *Validator) ValidateFile(path string) ([]ValidationError, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("reading %s: %w", path, err)
	}

	var errors []ValidationError

	// Parse as raw YAML node for field-level checking
	var node yaml.Node
	if err := yaml.Unmarshal(data, &node); err != nil {
		errors = append(errors, ValidationError{
			File:    filepath.Base(path),
			Level:   "error",
			Message: fmt.Sprintf("invalid YAML: %v", err),
		})
		return errors, nil
	}

	if node.Kind == yaml.DocumentNode && len(node.Content) > 0 {
		root := node.Content[0]
		if root.Kind == yaml.MappingNode {
			errors = append(errors, v.checkMapping(filepath.Base(path), root, v.knownTopLevel)...)
		}
	}

	// Also try to unmarshal to Config to catch type errors
	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		errors = append(errors, ValidationError{
			File:    filepath.Base(path),
			Level:   "error",
			Message: fmt.Sprintf("config structure error: %v", err),
		})
	}

	return errors, nil
}

// ValidateProject runs validation for a project directory.
func (v *Validator) ValidateProject(dir string) ([]ValidationError, error) {
	var allErrors []ValidationError

	// Check main config
	mainPath := filepath.Join(dir, DefaultConfigFile)
	if _, err := os.Stat(mainPath); err == nil {
		errs, err := v.ValidateFile(mainPath)
		if err != nil {
			return nil, err
		}
		allErrors = append(allErrors, errs...)
	}

	// Check fragments
	fragDir := filepath.Join(dir, FragmentDir)
	if entries, err := os.ReadDir(fragDir); err == nil {
		for _, entry := range entries {
			if entry.IsDir() || !isYAML(entry.Name()) {
				continue
			}
			errs, err := v.ValidateFile(filepath.Join(fragDir, entry.Name()))
			if err != nil {
				return nil, err
			}
			allErrors = append(allErrors, errs...)
		}
	}

	return allErrors, nil
}

func (v *Validator) checkMapping(file string, node *yaml.Node, known []string) []ValidationError {
	var errors []ValidationError
	for i := 0; i < len(node.Content)-1; i += 2 {
		keyNode := node.Content[i]
		key := keyNode.Value
		if !contains(known, key) {
			suggestion := findClosest(key, known)
			errors = append(errors, ValidationError{
				File:    file,
				Line:    keyNode.Line,
				Level:   "error",
				Message: fmt.Sprintf("unknown property '%s'", key),
				Suggest: suggestion,
			})
		}
	}
	return errors
}

func contains(ss []string, s string) bool {
	for _, v := range ss {
		if v == s {
			return true
		}
	}
	return false
}

// findClosest returns the closest match using simple edit distance.
func findClosest(input string, candidates []string) string {
	best := ""
	bestDist := len(input) + 1
	for _, c := range candidates {
		d := levenshtein(input, c)
		if d < bestDist && d <= 3 {
			bestDist = d
			best = c
		}
	}
	return best
}

func levenshtein(a, b string) int {
	la, lb := len(a), len(b)
	dp := make([][]int, la+1)
	for i := range dp {
		dp[i] = make([]int, lb+1)
		dp[i][0] = i
	}
	for j := 0; j <= lb; j++ {
		dp[0][j] = j
	}
	for i := 1; i <= la; i++ {
		for j := 1; j <= lb; j++ {
			cost := 0
			if a[i-1] != b[j-1] {
				cost = 1
			}
			dp[i][j] = min(dp[i-1][j]+1, min(dp[i][j-1]+1, dp[i-1][j-1]+cost))
		}
	}
	return dp[la][lb]
}
