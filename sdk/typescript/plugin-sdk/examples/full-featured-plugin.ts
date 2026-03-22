import { runPlugin, type ArchitectProcessPlugin } from "@architect-platform/plugin-sdk";

const plugin: ArchitectProcessPlugin = {
  async init(config) {
    const mode = String(config["mode"] ?? "default");
    return {
      ok: true,
      name: `example-ts-plugin-${mode}`,
      version: "1.0.0",
    };
  },

  async listTasks() {
    return [
      {
        id: "demo-build",
        description: "Build demo artifacts",
        phase: "BUILD",
        dependencies: ["demo-prepare"],
      },
      {
        id: "demo-prepare",
        description: "Prepare workspace",
        phase: "INIT",
      },
      {
        id: "demo-release",
        description: "Release demo artifacts",
        phase: "RELEASE",
        requires_confirmation: true,
      },
      {
        id: "demo-fail",
        description: "Demonstrate error event handling",
      },
    ];
  },

  async executeTask(request, writer) {
    await writer.output(`task=${request.id}`);
    await writer.output(`args=${JSON.stringify(request.args ?? [])}`);
    await writer.output(`profile=${request.env?.ARCHITECT_PROFILE ?? "unknown"}`);

    if (request.id === "demo-fail") {
      await writer.error("intentional failure from demo-fail");
      return 1;
    }

    for (const progress of [0.2, 0.5, 0.8, 1.0]) {
      await writer.progress(progress);
    }

    await writer.output(`completed ${request.id}`);
    return 0;
  },
};

void runPlugin(plugin);
