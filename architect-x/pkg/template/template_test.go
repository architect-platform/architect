package template

import "testing"

func TestRender(t *testing.T) {
	vars := map[string]string{
		"project.name": "my-app",
		"env.HOME":     "/home/user",
	}

	resolve := func(key string) (string, bool) {
		v, ok := vars[key]
		return v, ok
	}

	tests := []struct {
		input    string
		expected string
	}{
		{"Hello {{ project.name }}", "Hello my-app"},
		{"{{ env.HOME }}/bin", "/home/user/bin"},
		{"no vars", "no vars"},
		{"{{ missing }}", "{{ missing }}"},
		{"{{project.name}}", "my-app"},
		{"{{ project.name  }}", "my-app"},
	}

	for _, tt := range tests {
		got := Render(tt.input, resolve)
		if got != tt.expected {
			t.Errorf("Render(%q) = %q, want %q", tt.input, got, tt.expected)
		}
	}
}
