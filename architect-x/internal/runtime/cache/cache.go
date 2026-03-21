package cache

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
)

// Cache provides content-addressable caching for action results.
type Cache struct {
	Dir string
}

// New creates a cache in the given directory.
func New(dir string) (*Cache, error) {
	if err := os.MkdirAll(dir, 0755); err != nil {
		return nil, fmt.Errorf("creating cache dir: %w", err)
	}
	return &Cache{Dir: dir}, nil
}

// DefaultDir returns the default cache directory.
func DefaultDir() string {
	home, err := os.UserHomeDir()
	if err != nil {
		return filepath.Join(os.TempDir(), "architect-cache")
	}
	return filepath.Join(home, ".cache", "architect", "cache")
}

// Entry represents a cached action result.
type Entry struct {
	ActionID string `json:"action_id"`
	Success  bool   `json:"success"`
	Output   string `json:"output"`
}

// Get retrieves a cached result by key.
func (c *Cache) Get(key string) (*Entry, bool) {
	path := c.path(key)
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, false
	}

	var entry Entry
	if err := json.Unmarshal(data, &entry); err != nil {
		return nil, false
	}
	return &entry, true
}

// Put stores a result in the cache.
func (c *Cache) Put(key string, entry *Entry) error {
	data, err := json.Marshal(entry)
	if err != nil {
		return err
	}
	return os.WriteFile(c.path(key), data, 0644)
}

// Key computes a cache key from inputs.
func Key(parts ...string) string {
	h := sha256.New()
	for _, p := range parts {
		h.Write([]byte(p))
	}
	return hex.EncodeToString(h.Sum(nil))[:16]
}

func (c *Cache) path(key string) string {
	return filepath.Join(c.Dir, key+".json")
}

// Clear removes all cached entries.
func (c *Cache) Clear() error {
	entries, err := os.ReadDir(c.Dir)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		os.Remove(filepath.Join(c.Dir, entry.Name()))
	}
	return nil
}
