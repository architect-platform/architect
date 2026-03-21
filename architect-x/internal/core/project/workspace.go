package project

import (
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"

	"github.com/architect-platform/architect/internal/core/config"
)

// WorkspaceMember represents a project within a workspace.
type WorkspaceMember struct {
	Name         string
	Dir          string
	Config       *config.Config
	Dependencies []string
}

// Workspace represents a monorepo with multiple projects.
type Workspace struct {
	RootDir string
	Config  *config.Config
	Members []*WorkspaceMember
}

// LoadWorkspace loads a workspace and discovers all members.
func LoadWorkspace(rootDir string, cfg *config.Config) (*Workspace, error) {
	if cfg.Workspace == nil {
		return nil, fmt.Errorf("not a workspace project (no workspace config)")
	}

	ws := &Workspace{
		RootDir: rootDir,
		Config:  cfg,
	}

	// Discover members via glob patterns
	for _, pattern := range cfg.Workspace.Members {
		matches, err := filepath.Glob(filepath.Join(rootDir, pattern))
		if err != nil {
			return nil, fmt.Errorf("invalid workspace member pattern %q: %w", pattern, err)
		}

		for _, dir := range matches {
			// Check if excluded
			if isExcluded(dir, rootDir, cfg.Workspace.Exclude) {
				continue
			}

			// Check for architect.yml
			configPath := filepath.Join(dir, config.DefaultConfigFile)
			if _, err := os.Stat(configPath); os.IsNotExist(err) {
				continue
			}

			loader := config.NewLoader(dir)
			memberCfg, err := loader.Load()
			if err != nil {
				return nil, fmt.Errorf("loading member at %s: %w", dir, err)
			}

			ws.Members = append(ws.Members, &WorkspaceMember{
				Name:         memberCfg.Project.Name,
				Dir:          dir,
				Config:       memberCfg,
				Dependencies: memberCfg.Project.Dependencies,
			})
		}
	}

	return ws, nil
}

// TopologicalOrder returns members sorted by dependency order.
func (ws *Workspace) TopologicalOrder() ([]*WorkspaceMember, error) {
	byName := make(map[string]*WorkspaceMember)
	for _, m := range ws.Members {
		byName[m.Name] = m
	}

	// Kahn's algorithm
	inDegree := make(map[string]int)
	children := make(map[string][]string)
	for _, m := range ws.Members {
		inDegree[m.Name] = 0
	}
	for _, m := range ws.Members {
		for _, dep := range m.Dependencies {
			if _, ok := byName[dep]; ok {
				children[dep] = append(children[dep], m.Name)
				inDegree[m.Name]++
			}
		}
	}

	var queue []string
	for name, deg := range inDegree {
		if deg == 0 {
			queue = append(queue, name)
		}
	}
	sort.Strings(queue) // deterministic order

	var result []*WorkspaceMember
	for len(queue) > 0 {
		name := queue[0]
		queue = queue[1:]
		result = append(result, byName[name])

		var newReady []string
		for _, child := range children[name] {
			inDegree[child]--
			if inDegree[child] == 0 {
				newReady = append(newReady, child)
			}
		}
		sort.Strings(newReady)
		queue = append(queue, newReady...)
	}

	if len(result) != len(ws.Members) {
		return nil, fmt.Errorf("circular dependency detected among workspace members")
	}

	return result, nil
}

// Filter returns members matching the given pattern.
func (ws *Workspace) Filter(pattern string) []*WorkspaceMember {
	var result []*WorkspaceMember
	for _, m := range ws.Members {
		matched, _ := filepath.Match(pattern, m.Name)
		if matched {
			result = append(result, m)
			continue
		}
		// Also try matching relative dir
		rel, _ := filepath.Rel(ws.RootDir, m.Dir)
		if matched, _ := filepath.Match(pattern, rel); matched {
			result = append(result, m)
		}
	}
	return result
}

// FindMember returns a member by name.
func (ws *Workspace) FindMember(name string) *WorkspaceMember {
	for _, m := range ws.Members {
		if m.Name == name {
			return m
		}
	}
	return nil
}

func isExcluded(dir, rootDir string, excludes []string) bool {
	rel, err := filepath.Rel(rootDir, dir)
	if err != nil {
		return false
	}
	for _, pattern := range excludes {
		if matched, _ := filepath.Match(pattern, rel); matched {
			return true
		}
		// Also match base name
		if matched, _ := filepath.Match(pattern, filepath.Base(dir)); matched {
			return true
		}
		// Match with prefix stripped
		if strings.HasPrefix(rel, pattern) {
			return true
		}
	}
	return false
}
