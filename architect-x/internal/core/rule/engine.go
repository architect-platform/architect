package rule

import (
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

// Engine evaluates rules against a project.
type Engine struct {
	ProjectDir string
	Rules      map[string]*Rule
}

// NewEngine creates a rule engine.
func NewEngine(projectDir string, rules map[string]*Rule) *Engine {
	return &Engine{
		ProjectDir: projectDir,
		Rules:      rules,
	}
}

// CheckAll evaluates all rules matching the given trigger.
func (e *Engine) CheckAll(trigger string) []Result {
	var results []Result
	for _, r := range e.Rules {
		if !matchesTrigger(r.Trigger, trigger) {
			continue
		}
		result := e.Evaluate(r)
		results = append(results, result)
	}
	return results
}

// Evaluate runs a single rule check.
func (e *Engine) Evaluate(r *Rule) Result {
	// Pattern-based check (file existence)
	if r.Pattern != nil {
		return e.evaluatePattern(r)
	}

	// Command-based check
	if r.Check != nil {
		return e.evaluateCommand(r)
	}

	return Result{
		RuleID:   r.ID,
		Passed:   true,
		Severity: r.Severity,
		Message:  "no check defined",
	}
}

// Fix attempts to auto-fix a rule violation.
func (e *Engine) Fix(r *Rule) error {
	if r.Fix == nil {
		return fmt.Errorf("rule %q has no fix defined", r.ID)
	}

	dir := r.Fix.WorkingDir
	if dir == "" {
		dir = e.ProjectDir
	}

	cmd := exec.Command("sh", "-c", r.Fix.Command)
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func (e *Engine) evaluatePattern(r *Rule) Result {
	if r.Pattern.Exists != nil {
		var missing []string
		for _, path := range r.Pattern.Exists {
			fullPath := filepath.Join(e.ProjectDir, path)
			if _, err := os.Stat(fullPath); os.IsNotExist(err) {
				missing = append(missing, path)
			}
		}
		if len(missing) > 0 {
			return Result{
				RuleID:   r.ID,
				Passed:   false,
				Severity: r.Severity,
				Message:  fmt.Sprintf("missing required files: %s", strings.Join(missing, ", ")),
			}
		}
		return Result{
			RuleID:   r.ID,
			Passed:   true,
			Severity: r.Severity,
		}
	}

	return Result{RuleID: r.ID, Passed: true, Severity: r.Severity}
}

func (e *Engine) evaluateCommand(r *Rule) Result {
	dir := r.Check.WorkingDir
	if dir == "" {
		dir = e.ProjectDir
	}

	cmd := exec.Command("sh", "-c", r.Check.Command)
	cmd.Dir = dir

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	err := cmd.Run()
	output := strings.TrimSpace(stdout.String())
	if stderr.Len() > 0 {
		if output != "" {
			output += "\n"
		}
		output += strings.TrimSpace(stderr.String())
	}

	if err != nil {
		msg := output
		if msg == "" {
			msg = r.Description
		}
		return Result{
			RuleID:   r.ID,
			Passed:   false,
			Severity: r.Severity,
			Message:  msg,
			Fixable:  r.Fix != nil,
		}
	}

	return Result{
		RuleID:   r.ID,
		Passed:   true,
		Severity: r.Severity,
		Message:  output,
	}
}

func matchesTrigger(triggers []string, trigger string) bool {
	for _, t := range triggers {
		if t == trigger || t == "always" {
			return true
		}
	}
	return false
}
