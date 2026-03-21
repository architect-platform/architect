package main

import (
	"os"

	"github.com/architect-platform/architect/internal/cli"
)

func main() {
	if err := cli.Execute(); err != nil {
		os.Exit(1)
	}
}
