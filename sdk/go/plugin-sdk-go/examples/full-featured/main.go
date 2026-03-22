package main

import (
	"context"
	"fmt"

	architectplugin "github.com/architect-platform/plugin-sdk-go"
)

type fullFeaturedPlugin struct {
	mode string
}

func (p *fullFeaturedPlugin) Init(_ context.Context, config map[string]any) (architectplugin.InitResult, error) {
	if rawMode, ok := config["mode"].(string); ok && rawMode != "" {
		p.mode = rawMode
	}
	if p.mode == "" {
		p.mode = "default"
	}
	return architectplugin.InitResult{
		OK:      true,
		Name:    "example-go-plugin-" + p.mode,
		Version: "1.0.0",
	}, nil
}

func (p *fullFeaturedPlugin) ListTasks(context.Context) ([]architectplugin.TaskDescriptor, error) {
	return []architectplugin.TaskDescriptor{
		{
			ID:           "demo-prepare",
			Description:  "Prepare workspace",
			Phase:        "INIT",
			Dependencies: []string{},
		},
		{
			ID:           "demo-build",
			Description:  "Build demo artifacts",
			Phase:        "BUILD",
			Dependencies: []string{"demo-prepare"},
		},
		{
			ID:                   "demo-release",
			Description:          "Release demo artifacts",
			Phase:                "RELEASE",
			RequiresConfirmation: true,
		},
		{
			ID:          "demo-fail",
			Description: "Demonstrate error event handling",
		},
	}, nil
}

func (p *fullFeaturedPlugin) ExecuteTask(_ context.Context, request architectplugin.ExecuteTaskParams, writer architectplugin.TaskEventWriter) (int, error) {
	if err := writer.Output("task=" + request.ID); err != nil {
		return 1, err
	}
	if err := writer.Output(fmt.Sprintf("args=%v", request.Args)); err != nil {
		return 1, err
	}
	if request.Env != nil {
		if err := writer.Output("profile=" + request.Env["ARCHITECT_PROFILE"]); err != nil {
			return 1, err
		}
	}

	if request.ID == "demo-fail" {
		if err := writer.Error("intentional failure from demo-fail"); err != nil {
			return 1, err
		}
		return 1, nil
	}

	for _, progress := range []float64{0.2, 0.5, 0.8, 1.0} {
		if err := writer.Progress(progress); err != nil {
			return 1, err
		}
	}

	if err := writer.Output("completed " + request.ID); err != nil {
		return 1, err
	}
	return 0, nil
}

func main() {
	plugin := &fullFeaturedPlugin{}
	if err := architectplugin.Run(context.Background(), plugin); err != nil {
		panic(err)
	}
}
