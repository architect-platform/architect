import { ExecuteTaskParams, InitResult, JsonObject, TaskDescriptor } from "./protocol";

export interface TaskEventWriter {
  output(data: string): Promise<void>;
  error(data: string): Promise<void>;
  progress(progress: number): Promise<void>;
}

export interface ArchitectProcessPlugin {
  init?(config: JsonObject): Promise<InitResult | void> | InitResult | void;
  listTasks(): Promise<TaskDescriptor[]> | TaskDescriptor[];
  executeTask(
    request: ExecuteTaskParams,
    writer: TaskEventWriter,
  ): Promise<number | void> | number | void;
}