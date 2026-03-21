package plugin

import (
	"fmt"
	"os"
	"path/filepath"

	"gopkg.in/yaml.v3"
)

const pluginFileName = "plugin.yml"

// Loader handles finding and loading plugins.
type Loader struct {
	SearchPaths []string // directories to search for plugins
}

// NewLoader creates a plugin loader.
func NewLoader() *Loader {
	return &Loader{}
}

// LoadFromDir loads a plugin from a directory containing plugin.yml.
func (l *Loader) LoadFromDir(dir string) (*Plugin, error) {
	path := filepath.Join(dir, pluginFileName)
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("reading plugin at %s: %w", path, err)
	}

	var p Plugin
	if err := yaml.Unmarshal(data, &p); err != nil {
		return nil, fmt.Errorf("parsing plugin %s: %w", path, err)
	}

	// Assign IDs from map keys
	for id, a := range p.Actions {
		if a.ID == "" {
			a.ID = id
		}
	}
	for id, r := range p.Rules {
		if r.ID == "" {
			r.ID = id
		}
	}

	return &p, nil
}

// Resolve finds a plugin by its reference string.
// Supports:
//   - ./path/to/plugin (local directory)
//   - plugin-name (search in search paths)
func (l *Loader) Resolve(ref string) (string, error) {
	// Local path
	if ref[0] == '.' || ref[0] == '/' {
		abs, err := filepath.Abs(ref)
		if err != nil {
			return "", err
		}
		if _, err := os.Stat(filepath.Join(abs, pluginFileName)); err != nil {
			return "", fmt.Errorf("no plugin.yml found at %s", abs)
		}
		return abs, nil
	}

	// Search in search paths
	for _, searchDir := range l.SearchPaths {
		candidate := filepath.Join(searchDir, ref)
		if _, err := os.Stat(filepath.Join(candidate, pluginFileName)); err == nil {
			return candidate, nil
		}
	}

	// Check cache directory
	home, err := os.UserHomeDir()
	if err == nil {
		cacheDir := filepath.Join(home, ".cache", "architect", "plugins", ref)
		if _, err := os.Stat(filepath.Join(cacheDir, pluginFileName)); err == nil {
			return cacheDir, nil
		}
	}

	return "", fmt.Errorf("plugin %q not found in search paths", ref)
}
