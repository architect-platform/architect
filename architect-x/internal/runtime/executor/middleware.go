package executor

import (
	"context"
	"fmt"
	"time"

	"github.com/architect-platform/architect/internal/core/action"
)

// LoggingMiddleware logs action start/end.
func LoggingMiddleware(verbose bool) Middleware {
	return func(ctx context.Context, a *action.Action, next func(context.Context, *action.Action) *Result) *Result {
		if verbose {
			fmt.Printf("  [start] %s\n", a.ID)
		}
		result := next(ctx, a)
		if verbose {
			if result.Success {
				fmt.Printf("  [done]  %s (%s)\n", a.ID, formatDuration(result.Duration))
			} else {
				fmt.Printf("  [fail]  %s (%s): %v\n", a.ID, formatDuration(result.Duration), result.Error)
			}
		}
		return result
	}
}

// RetryMiddleware retries failed actions based on their retry spec.
func RetryMiddleware() Middleware {
	return func(ctx context.Context, a *action.Action, next func(context.Context, *action.Action) *Result) *Result {
		result := next(ctx, a)
		if result.Success || a.Retry == nil || a.Retry.Count == 0 {
			return result
		}

		for attempt := 1; attempt <= a.Retry.Count; attempt++ {
			delay := a.Retry.Delay
			if a.Retry.Backoff == "exponential" {
				delay = delay * time.Duration(1<<uint(attempt-1))
			}
			if delay > 0 {
				select {
				case <-ctx.Done():
					return &Result{
						ActionID: a.ID,
						Success:  false,
						Error:    ctx.Err(),
						Duration: result.Duration,
					}
				case <-time.After(delay):
				}
			}

			result = next(ctx, a)
			if result.Success {
				return result
			}
		}

		return result
	}
}

// TimeoutMiddleware enforces action timeouts.
func TimeoutMiddleware() Middleware {
	return func(ctx context.Context, a *action.Action, next func(context.Context, *action.Action) *Result) *Result {
		if a.Timeout == "" {
			return next(ctx, a)
		}

		dur, err := time.ParseDuration(a.Timeout)
		if err != nil {
			return &Result{
				ActionID: a.ID,
				Success:  false,
				Error:    fmt.Errorf("invalid timeout %q: %w", a.Timeout, err),
			}
		}

		ctx, cancel := context.WithTimeout(ctx, dur)
		defer cancel()
		return next(ctx, a)
	}
}

func formatDuration(d time.Duration) string {
	if d < time.Second {
		return fmt.Sprintf("%dms", d.Milliseconds())
	}
	return fmt.Sprintf("%.1fs", d.Seconds())
}
