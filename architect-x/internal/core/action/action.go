package action

import (
	"time"
)

// Action is the single primitive that replaces Task, Phase, Workflow, and CompositeTask.
type Action struct {
	ID          string            `yaml:"id"`
	Description string            `yaml:"description,omitempty"`
	Plugin      string            `yaml:"plugin,omitempty"`

	// Execution (pick one)
	Run   *RunSpec  `yaml:"run,omitempty"`
	Steps []string  `yaml:"steps,omitempty"`

	// Graph
	DependsOn []string `yaml:"depends_on,omitempty"`
	AttachTo  string   `yaml:"attach_to,omitempty"`

	// Behavior
	Parallel        bool       `yaml:"parallel,omitempty"`
	Condition       string     `yaml:"condition,omitempty"`
	ContinueOnError bool       `yaml:"continue_on_error,omitempty"`
	Retry           *RetrySpec `yaml:"retry,omitempty"`
	Timeout         string     `yaml:"timeout,omitempty"`
	Cache           *CacheSpec `yaml:"cache,omitempty"`

	// Config
	Inputs map[string]InputSpec `yaml:"inputs,omitempty"`
	Env    map[string]string    `yaml:"env,omitempty"`
}

// RunSpec defines a shell command to execute.
type RunSpec struct {
	Command    string `yaml:"command"`
	WorkingDir string `yaml:"working_dir,omitempty"`
	Shell      string `yaml:"shell,omitempty"`
}

// RetrySpec defines retry behavior for an action.
type RetrySpec struct {
	Count   int           `yaml:"count"`
	Delay   time.Duration `yaml:"delay,omitempty"`
	Backoff string        `yaml:"backoff,omitempty"` // linear, exponential
}

// CacheSpec defines caching behavior for an action.
type CacheSpec struct {
	Key   string   `yaml:"key,omitempty"`
	Paths []string `yaml:"paths,omitempty"`
}

// InputSpec defines an input parameter for an action.
type InputSpec struct {
	Type        string      `yaml:"type"`
	Description string      `yaml:"description,omitempty"`
	Default     interface{} `yaml:"default,omitempty"`
	Required    bool        `yaml:"required,omitempty"`
}
