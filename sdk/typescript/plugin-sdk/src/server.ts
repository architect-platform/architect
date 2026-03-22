import { createInterface } from "node:readline";
import { once } from "node:events";
import { Writable, Readable } from "node:stream";
import { ArchitectProcessPlugin, TaskEventWriter } from "./plugin";
import {
  APP_VERSION,
  EVENT_TYPES,
  ExecuteTaskParams,
  InitParams,
  JSON_RPC_ERROR,
  JSON_RPC_VERSION,
  JsonObject,
  JsonRpcRequest,
  JsonRpcResponse,
  METHODS,
  TaskEvent,
} from "./protocol";

export interface PluginServerIO {
  input: Readable;
  output: Writable;
  error?: Writable;
  exit?: (code?: number) => void;
}

export class PluginServer {
  private readonly io: PluginServerIO;
  private shuttingDown = false;

  constructor(
    private readonly plugin: ArchitectProcessPlugin,
    io?: Partial<PluginServerIO>,
  ) {
    this.io = {
      input: io?.input ?? process.stdin,
      output: io?.output ?? process.stdout,
      error: io?.error ?? process.stderr,
      exit: io?.exit ?? ((code?: number) => {
        process.exitCode = code ?? 0;
      }),
    };
  }

  async listen(): Promise<void> {
    const lines = createInterface({
      input: this.io.input,
      crlfDelay: Infinity,
    });

    for await (const line of lines) {
      if (!line.trim()) {
        continue;
      }

      const request = this.parseRequest(line);
      await this.handleRequest(request);

      if (this.shuttingDown) {
        lines.close();
        break;
      }
    }
  }

  async handleRequest(request: JsonRpcRequest): Promise<void> {
    if (request.jsonrpc !== JSON_RPC_VERSION || typeof request.id !== "number" || typeof request.method !== "string") {
      await this.writeResponse({
        jsonrpc: JSON_RPC_VERSION,
        id: request.id ?? null,
        error: {
          code: JSON_RPC_ERROR.invalidRequest,
          message: "Invalid JSON-RPC request",
        },
      });
      return;
    }

    switch (request.method) {
      case METHODS.init:
        await this.handleInit(request as JsonRpcRequest<InitParams>);
        return;
      case METHODS.listTasks:
        await this.handleListTasks(request);
        return;
      case METHODS.executeTask:
        await this.handleExecuteTask(request as JsonRpcRequest<ExecuteTaskParams>);
        return;
      case METHODS.shutdown:
        await this.handleShutdown(request);
        return;
      default:
        await this.writeResponse({
          jsonrpc: JSON_RPC_VERSION,
          id: request.id,
          error: {
            code: JSON_RPC_ERROR.methodNotFound,
            message: `Unknown method: ${request.method}`,
          },
        });
    }
  }

  private parseRequest(line: string): JsonRpcRequest {
    try {
      return JSON.parse(line) as JsonRpcRequest;
    } catch {
      return {
        jsonrpc: JSON_RPC_VERSION,
        id: null as unknown as number,
        method: "",
      };
    }
  }

  private async handleInit(request: JsonRpcRequest<InitParams>): Promise<void> {
    try {
      const params = request.params;
      if (!params || params.protocol_version !== APP_VERSION) {
        await this.writeResponse({
          jsonrpc: JSON_RPC_VERSION,
          id: request.id,
          error: {
            code: JSON_RPC_ERROR.invalidRequest,
            message: `Unsupported protocol version: ${params?.protocol_version ?? "missing"}`,
          },
        });
        return;
      }

      const result = await this.plugin.init?.((params.config ?? {}) as JsonObject);
      await this.writeResponse({
        jsonrpc: JSON_RPC_VERSION,
        id: request.id,
        result: {
          ok: true,
          ...(result ?? {}),
        },
      });
    } catch (error) {
      await this.writeResponse(this.internalError(request.id, error));
    }
  }

  private async handleListTasks(request: JsonRpcRequest): Promise<void> {
    try {
      const tasks = await this.plugin.listTasks();
      await this.writeResponse({
        jsonrpc: JSON_RPC_VERSION,
        id: request.id,
        result: tasks,
      });
    } catch (error) {
      await this.writeResponse(this.internalError(request.id, error));
    }
  }

  private async handleExecuteTask(request: JsonRpcRequest<ExecuteTaskParams>): Promise<void> {
    const params = request.params;
    if (!params?.id) {
      await this.writeResponse({
        jsonrpc: JSON_RPC_VERSION,
        id: request.id,
        error: {
          code: JSON_RPC_ERROR.invalidRequest,
          message: "executeTask requires an id",
        },
      });
      return;
    }

    await this.writeResponse({
      jsonrpc: JSON_RPC_VERSION,
      id: request.id,
      result: null,
    });

    const writer = this.createTaskEventWriter();

    try {
      const exitCode = (await this.plugin.executeTask(params, writer)) ?? 0;
      await this.writeEvent({
        type: EVENT_TYPES.completed,
        exitCode,
      });
    } catch (error) {
      await writer.error(this.errorMessage(error));
      await this.writeEvent({
        type: EVENT_TYPES.completed,
        exitCode: 1,
      });
    }
  }

  private async handleShutdown(request: JsonRpcRequest): Promise<void> {
    this.shuttingDown = true;
    await this.writeResponse({
      jsonrpc: JSON_RPC_VERSION,
      id: request.id,
      result: null,
    });
    this.io.exit?.(0);
  }

  private createTaskEventWriter(): TaskEventWriter {
    return {
      output: (data) => this.writeEvent({ type: EVENT_TYPES.output, data }),
      error: (data) => this.writeEvent({ type: EVENT_TYPES.error, data }),
      progress: (progress) => this.writeEvent({ type: EVENT_TYPES.progress, progress }),
    };
  }

  private async writeResponse(response: JsonRpcResponse): Promise<void> {
    await this.writeLine(JSON.stringify(response));
  }

  private async writeEvent(event: TaskEvent): Promise<void> {
    await this.writeLine(JSON.stringify(event));
  }

  private async writeLine(line: string): Promise<void> {
    if (!this.io.output.write(`${line}\n`)) {
      await once(this.io.output, "drain");
    }
  }

  private internalError(id: number, error: unknown): JsonRpcResponse {
    return {
      jsonrpc: JSON_RPC_VERSION,
      id,
      error: {
        code: JSON_RPC_ERROR.internalError,
        message: this.errorMessage(error),
      },
    };
  }

  private errorMessage(error: unknown): string {
    return error instanceof Error ? error.message : String(error);
  }
}

export async function runPlugin(plugin: ArchitectProcessPlugin): Promise<void> {
  const server = new PluginServer(plugin);
  await server.listen();
}