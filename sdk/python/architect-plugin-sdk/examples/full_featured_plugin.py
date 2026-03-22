from architect_plugin_sdk import InitResult, PluginServer, TaskDescriptor


class FullFeaturedPlugin:
    def __init__(self) -> None:
        self.mode = "default"

    def init(self, config):
        self.mode = str(config.get("mode", "default"))
        return InitResult(ok=True, name=f"example-python-plugin-{self.mode}", version="1.0.0")

    def list_tasks(self):
        return [
            TaskDescriptor(id="demo-prepare", description="Prepare workspace", phase="INIT"),
            TaskDescriptor(
                id="demo-build",
                description="Build demo artifacts",
                phase="BUILD",
                dependencies=["demo-prepare"],
            ),
            TaskDescriptor(
                id="demo-release",
                description="Release demo artifacts",
                phase="RELEASE",
                requires_confirmation=True,
            ),
            TaskDescriptor(id="demo-fail", description="Demonstrate error event handling"),
        ]

    def execute_task(self, request, writer):
        writer.output(f"task={request.id}")
        writer.output(f"args={request.args or []}")
        writer.output(f"profile={(request.env or {}).get('ARCHITECT_PROFILE', 'unknown')}")

        if request.id == "demo-fail":
            writer.error("intentional failure from demo-fail")
            return 1

        for progress in (0.2, 0.5, 0.8, 1.0):
            writer.progress(progress)

        writer.output(f"completed {request.id}")
        return 0


if __name__ == "__main__":
    PluginServer(FullFeaturedPlugin()).serve()
