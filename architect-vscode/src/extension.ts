import * as vscode from "vscode";
import { TaskTreeProvider, TaskItem } from "./taskTreeProvider";

let outputChannel: vscode.OutputChannel;

export function activate(context: vscode.ExtensionContext) {
  outputChannel = vscode.window.createOutputChannel("Architect");

  const taskProvider = new TaskTreeProvider();
  vscode.window.registerTreeDataProvider("architectTasks", taskProvider);

  context.subscriptions.push(
    vscode.commands.registerCommand("architect.runTask", (item?: TaskItem) => {
      const taskId = item?.taskId;
      if (!taskId) {
        vscode.window
          .showInputBox({ prompt: "Task ID to run" })
          .then((id) => id && runArchitectCommand(["--embedded", id]));
        return;
      }
      runArchitectCommand(["--embedded", taskId]);
    }),

    vscode.commands.registerCommand("architect.planTask", (item?: TaskItem) => {
      const taskId = item?.taskId;
      if (!taskId) {
        vscode.window
          .showInputBox({ prompt: "Task ID to plan" })
          .then((id) => id && runArchitectCommand(["--embedded", "plan", id]));
        return;
      }
      runArchitectCommand(["--embedded", "plan", taskId]);
    }),

    vscode.commands.registerCommand("architect.validate", () => {
      runArchitectCommand(["--embedded", "validate"]);
    }),

    vscode.commands.registerCommand("architect.refreshTasks", () => {
      taskProvider.refresh();
    }),

    outputChannel
  );

  // Refresh tasks on activation
  taskProvider.refresh();
}

function runArchitectCommand(args: string[]) {
  const config = vscode.workspace.getConfiguration("architect");
  const executable = config.get<string>("executablePath", "architect");

  outputChannel.show(true);
  outputChannel.appendLine(`> ${executable} ${args.join(" ")}`);

  const workspaceFolder = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
  if (!workspaceFolder) {
    outputChannel.appendLine("Error: no workspace folder open");
    return;
  }

  const cp = require("child_process");
  const proc = cp.spawn(executable, args, {
    cwd: workspaceFolder,
    env: { ...process.env },
  });

  proc.stdout?.on("data", (data: Buffer) => {
    outputChannel.append(data.toString());
  });

  proc.stderr?.on("data", (data: Buffer) => {
    outputChannel.append(data.toString());
  });

  proc.on("close", (code: number) => {
    outputChannel.appendLine(`\nProcess exited with code ${code}`);
  });
}

export function deactivate() {
  outputChannel?.dispose();
}
