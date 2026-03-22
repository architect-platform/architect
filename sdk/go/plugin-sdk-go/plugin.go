package architectplugin

import "context"

type Plugin interface {
	ListTasks(ctx context.Context) ([]TaskDescriptor, error)
	ExecuteTask(ctx context.Context, params ExecuteTaskParams, writer TaskEventWriter) (int, error)
}

type InitializablePlugin interface {
	Init(ctx context.Context, config map[string]any) (InitResult, error)
}

type TaskEventWriter interface {
	Output(text string) error
	Error(text string) error
	Progress(value float64) error
}
