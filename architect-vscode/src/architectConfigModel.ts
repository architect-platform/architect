import { parseDocument } from "yaml";

export interface ArchitectTaskDefinition {
  id: string;
  description: string;
  phase?: string;
  source: "tasks" | "scripts";
}

export interface ArchitectConfigModel {
  tasks: ArchitectTaskDefinition[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readOptionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

function extractTaskDefinitions(
  value: unknown,
  source: ArchitectTaskDefinition["source"],
): ArchitectTaskDefinition[] {
  if (!isRecord(value)) {
    return [];
  }

  return Object.entries(value).flatMap(([id, rawTask]) => {
    if (!isRecord(rawTask)) {
      return [];
    }

    return [
      {
        id,
        description: readOptionalString(rawTask.description) ?? "",
        phase: readOptionalString(rawTask.phase),
        source,
      },
    ];
  });
}

export function parseArchitectConfig(yamlContent: string): ArchitectConfigModel {
  const document = parseDocument(yamlContent);

  if (document.errors.length > 0) {
    throw new Error(document.errors.map((error) => error.message).join("\n"));
  }

  const parsed = document.toJS();
  if (!isRecord(parsed)) {
    return { tasks: [] };
  }

  const taskMap = new Map<string, ArchitectTaskDefinition>();

  for (const task of extractTaskDefinitions(parsed.tasks, "tasks")) {
    taskMap.set(task.id, task);
  }

  const scriptsSection = isRecord(parsed.scripts) ? parsed.scripts : undefined;
  for (const task of extractTaskDefinitions(scriptsSection?.scripts, "scripts")) {
    if (!taskMap.has(task.id)) {
      taskMap.set(task.id, task);
    }
  }

  return {
    tasks: Array.from(taskMap.values()),
  };
}
