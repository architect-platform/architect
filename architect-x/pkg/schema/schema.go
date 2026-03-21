package schema

// Schema represents a JSON Schema definition for config validation.
// This is a simplified representation; full JSON Schema support will
// use a dedicated library in later phases.
type Schema struct {
	Type       string                `json:"type"`
	Properties map[string]*Property  `json:"properties,omitempty"`
	Required   []string              `json:"required,omitempty"`
}

// Property defines a schema property.
type Property struct {
	Type        string      `json:"type"`
	Description string      `json:"description,omitempty"`
	Default     interface{} `json:"default,omitempty"`
	Enum        []string    `json:"enum,omitempty"`
}
