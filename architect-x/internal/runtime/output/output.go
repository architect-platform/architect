package output

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/architect-platform/architect/internal/runtime/executor"
)

// Mode determines the output format.
type Mode int

const (
	ModeRich  Mode = iota // tree view with colors
	ModePlain             // CI-friendly
	ModeJSON              // structured JSON
	ModeQuiet             // errors only
)

// Printer handles formatted output for action execution.
type Printer struct {
	mu     sync.Mutex
	w      io.Writer
	mode   Mode
	start  time.Time
	counts struct {
		total     int
		completed int
		failed    int
	}
}

// NewPrinter creates an output printer.
func NewPrinter(mode Mode) *Printer {
	return &Printer{
		w:     os.Stdout,
		mode:  mode,
		start: time.Now(),
	}
}

// SetTotal sets the total number of actions to track.
func (p *Printer) SetTotal(n int) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.counts.total = n
}

// ActionStart prints when an action begins.
func (p *Printer) ActionStart(actionID string) {
	p.mu.Lock()
	defer p.mu.Unlock()

	switch p.mode {
	case ModeRich:
		fmt.Fprintf(p.w, "  %s %s\n", colorize("[run] ", colorYellow), actionID)
	case ModePlain:
		fmt.Fprintf(p.w, "[run]  %s\n", actionID)
	case ModeJSON:
		p.jsonEvent("start", actionID, nil)
	case ModeQuiet:
		// no output
	}
}

// ActionDone prints when an action completes.
func (p *Printer) ActionDone(result *executor.Result) {
	p.mu.Lock()
	defer p.mu.Unlock()

	p.counts.completed++
	if !result.Success {
		p.counts.failed++
	}

	switch p.mode {
	case ModeRich:
		p.printRichResult(result)
	case ModePlain:
		p.printPlainResult(result)
	case ModeJSON:
		p.jsonEvent("done", result.ActionID, result)
	case ModeQuiet:
		if !result.Success && result.Error != nil {
			fmt.Fprintf(p.w, "ERROR: %s: %v\n", result.ActionID, result.Error)
		}
	}
}

// Summary prints the final execution summary.
func (p *Printer) Summary(results []*executor.Result) {
	p.mu.Lock()
	defer p.mu.Unlock()

	elapsed := time.Since(p.start)

	switch p.mode {
	case ModeRich:
		fmt.Fprintln(p.w)
		completed := 0
		failed := 0
		for _, r := range results {
			if r.Success {
				completed++
			} else if !r.Skipped {
				failed++
			}
		}
		status := colorize("SUCCESS", colorGreen)
		if failed > 0 {
			status = colorize("FAILED", colorRed)
		}
		fmt.Fprintf(p.w, "%s  %d actions | %d succeeded | %d failed | %s\n",
			status, len(results), completed, failed, formatDur(elapsed))

	case ModePlain:
		fmt.Fprintf(p.w, "\n%d actions completed in %s\n", len(results), formatDur(elapsed))

	case ModeJSON:
		data := map[string]interface{}{
			"total":    len(results),
			"duration": elapsed.String(),
			"results":  results,
		}
		b, _ := json.Marshal(data)
		fmt.Fprintln(p.w, string(b))

	case ModeQuiet:
		if p.counts.failed > 0 {
			fmt.Fprintf(p.w, "%d action(s) failed\n", p.counts.failed)
		}
	}
}

// PrintPlan outputs a dry-run execution plan.
func (p *Printer) PrintPlan(results []*executor.Result) {
	fmt.Fprintln(p.w, "Plan:")
	for i, r := range results {
		prefix := "  "
		if i == len(results)-1 {
			prefix = "  "
		}
		if r.Output != "" {
			fmt.Fprintf(p.w, "%s%d. %s\n", prefix, i+1, r.Output)
		} else {
			fmt.Fprintf(p.w, "%s%d. %s (no-op)\n", prefix, i+1, r.ActionID)
		}
	}
	fmt.Fprintf(p.w, "\n%d action(s) in plan.\nRun without --dry-run to execute.\n", len(results))
}

// PrintTree outputs actions as a tree view.
func PrintTree(w io.Writer, actionID string, results []*executor.Result) {
	fmt.Fprintf(w, "\n%s\n", actionID)
	resultMap := make(map[string]*executor.Result)
	for _, r := range results {
		resultMap[r.ActionID] = r
	}
	for i, r := range results {
		prefix := "├── "
		if i == len(results)-1 {
			prefix = "└── "
		}
		status := colorize("[done]", colorGreen)
		if !r.Success {
			if r.Skipped {
				status = colorize("[skip]", colorYellow)
			} else {
				status = colorize("[fail]", colorRed)
			}
		}
		dur := ""
		if r.Duration > 0 {
			dur = fmt.Sprintf(" (%s)", formatDur(r.Duration))
		}
		fmt.Fprintf(w, "  %s%s %s%s\n", prefix, status, r.ActionID, dur)
	}
}

func (p *Printer) printRichResult(r *executor.Result) {
	if r.Skipped {
		fmt.Fprintf(p.w, "  %s %s (skipped)\n", colorize("[skip]", colorYellow), r.ActionID)
		return
	}
	if r.Success {
		fmt.Fprintf(p.w, "  %s %s (%s)\n", colorize("[done]", colorGreen), r.ActionID, formatDur(r.Duration))
		if r.Output != "" {
			for _, line := range strings.Split(strings.TrimSpace(r.Output), "\n") {
				fmt.Fprintf(p.w, "         %s\n", line)
			}
		}
	} else {
		fmt.Fprintf(p.w, "  %s %s (%s)\n", colorize("[fail]", colorRed), r.ActionID, formatDur(r.Duration))
		if r.Error != nil {
			fmt.Fprintf(p.w, "         %s\n", colorize(r.Error.Error(), colorRed))
		}
		if r.Output != "" {
			for _, line := range strings.Split(strings.TrimSpace(r.Output), "\n") {
				fmt.Fprintf(p.w, "         %s\n", line)
			}
		}
	}
}

func (p *Printer) printPlainResult(r *executor.Result) {
	if r.Skipped {
		fmt.Fprintf(p.w, "[skip] %s\n", r.ActionID)
		return
	}
	status := "[done]"
	if !r.Success {
		status = "[fail]"
	}
	fmt.Fprintf(p.w, "%s %s (%s)\n", status, r.ActionID, formatDur(r.Duration))
	if r.Output != "" {
		fmt.Fprint(p.w, r.Output)
	}
}

func (p *Printer) jsonEvent(event, actionID string, result *executor.Result) {
	data := map[string]interface{}{
		"event":  event,
		"action": actionID,
	}
	if result != nil {
		data["success"] = result.Success
		data["duration"] = result.Duration.String()
		if result.Error != nil {
			data["error"] = result.Error.Error()
		}
	}
	b, _ := json.Marshal(data)
	fmt.Fprintln(p.w, string(b))
}

// Color codes
const (
	colorReset  = "\033[0m"
	colorRed    = "\033[31m"
	colorGreen  = "\033[32m"
	colorYellow = "\033[33m"
)

func colorize(s, color string) string {
	return color + s + colorReset
}

func formatDur(d time.Duration) string {
	if d < time.Second {
		return fmt.Sprintf("%dms", d.Milliseconds())
	}
	return fmt.Sprintf("%.1fs", d.Seconds())
}
