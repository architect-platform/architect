package rule

// Severity levels for rules.
type Severity string

const (
	SeverityError Severity = "error"
	SeverityWarn  Severity = "warn"
	SeverityInfo  Severity = "info"
)

// Rule defines a convention check.
type Rule struct {
	ID          string   `yaml:"id"`
	Description string   `yaml:"description"`
	Severity    Severity `yaml:"severity"`
	Trigger     []string `yaml:"trigger"` // pre-commit, build, always, manual
	Check       *RunSpec `yaml:"check,omitempty"`
	Fix         *RunSpec `yaml:"fix,omitempty"`
	Condition   string   `yaml:"condition,omitempty"`
	Rationale   string   `yaml:"rationale,omitempty"`
	Pattern     *Pattern `yaml:"pattern,omitempty"`
}

// RunSpec defines a shell command (shared concept with action).
type RunSpec struct {
	Command    string `yaml:"command"`
	WorkingDir string `yaml:"working_dir,omitempty"`
}

// Pattern defines declarative checks.
type Pattern struct {
	Exists []string `yaml:"exists,omitempty"`
}

// Result holds the outcome of evaluating a rule.
type Result struct {
	RuleID   string
	Passed   bool
	Message  string
	Severity Severity
	Fixable  bool
}
