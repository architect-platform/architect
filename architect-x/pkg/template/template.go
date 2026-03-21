package template

import (
	"regexp"
	"strings"
)

var pattern = regexp.MustCompile(`\{\{\s*([a-zA-Z0-9_.]+)\s*\}\}`)

// Render replaces {{ key }} expressions using the provided resolver function.
// The resolver receives the dot-separated key (e.g., "project.name") and
// returns the value and whether it was found.
func Render(input string, resolve func(key string) (string, bool)) string {
	return pattern.ReplaceAllStringFunc(input, func(match string) string {
		sub := pattern.FindStringSubmatch(match)
		if len(sub) < 2 {
			return match
		}
		key := strings.TrimSpace(sub[1])
		if val, ok := resolve(key); ok {
			return val
		}
		return match
	})
}
