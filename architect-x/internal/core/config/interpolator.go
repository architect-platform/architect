package config

import (
	"fmt"
	"os"
	"os/exec"
	"regexp"
	"runtime"
	"strings"
)

var interpolatePattern = regexp.MustCompile(`\{\{\s*([a-zA-Z0-9_.]+)\s*\}\}`)

// Interpolator resolves {{ variable }} expressions in strings.
type Interpolator struct {
	Project *ProjectConfig
	Config  *Config
	gitInfo map[string]string
}

// NewInterpolator creates an interpolator for the given config.
func NewInterpolator(cfg *Config) *Interpolator {
	return &Interpolator{
		Project: &cfg.Project,
		Config:  cfg,
	}
}

// Interpolate resolves all {{ }} expressions in the given string.
func (i *Interpolator) Interpolate(s string) string {
	return interpolatePattern.ReplaceAllStringFunc(s, func(match string) string {
		sub := interpolatePattern.FindStringSubmatch(match)
		if len(sub) < 2 {
			return match
		}
		key := sub[1]
		val, ok := i.resolve(key)
		if !ok {
			return match
		}
		return val
	})
}

func (i *Interpolator) resolve(key string) (string, bool) {
	parts := strings.SplitN(key, ".", 2)
	if len(parts) < 2 {
		return "", false
	}

	namespace := parts[0]
	field := parts[1]

	switch namespace {
	case "project":
		return i.resolveProject(field)
	case "env":
		val, ok := os.LookupEnv(field)
		return val, ok
	case "git":
		return i.resolveGit(field)
	case "arch":
		return i.resolveArch(field)
	default:
		return "", false
	}
}

func (i *Interpolator) resolveProject(field string) (string, bool) {
	switch field {
	case "name":
		return i.Project.Name, i.Project.Name != ""
	case "version":
		return i.Project.Version, i.Project.Version != ""
	case "type":
		return i.Project.Type, i.Project.Type != ""
	case "description":
		return i.Project.Description, i.Project.Description != ""
	default:
		return "", false
	}
}

func (i *Interpolator) resolveGit(field string) (string, bool) {
	if i.gitInfo == nil {
		i.gitInfo = loadGitInfo()
	}
	val, ok := i.gitInfo[field]
	return val, ok
}

func (i *Interpolator) resolveArch(field string) (string, bool) {
	switch field {
	case "os":
		return runtime.GOOS, true
	case "cpu":
		return runtime.GOARCH, true
	default:
		return "", false
	}
}

func loadGitInfo() map[string]string {
	info := make(map[string]string)
	if branch, err := gitCmd("rev-parse", "--abbrev-ref", "HEAD"); err == nil {
		info["branch"] = branch
	}
	if commit, err := gitCmd("rev-parse", "--short", "HEAD"); err == nil {
		info["commit"] = commit
	}
	if tag, err := gitCmd("describe", "--tags", "--abbrev=0"); err == nil {
		info["tag"] = tag
	}
	return info
}

func gitCmd(args ...string) (string, error) {
	cmd := exec.Command("git", args...)
	out, err := cmd.Output()
	if err != nil {
		return "", fmt.Errorf("git %s: %w", strings.Join(args, " "), err)
	}
	return strings.TrimSpace(string(out)), nil
}
