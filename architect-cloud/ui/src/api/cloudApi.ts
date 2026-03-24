import type { CloudDashboardData, EngineInstance, Execution, Project } from '../types/cloud'

const API_BASE_URL = 'http://localhost:8080/api'

async function fetchJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`)
  if (!response.ok) {
    throw new Error(`Failed to fetch ${path.replace('/api/', '').replace(/^\//, '')}`)
  }

  return (await response.json()) as T
}

function normalizeError(path: string, error: unknown): Error {
  if (error instanceof Error) {
    if (error.message.startsWith('Failed to fetch')) {
      const segment = path.split('/').filter(Boolean)[0] ?? 'resource'
      return new Error(`Failed to fetch ${segment}`)
    }

    return error
  }

  return new Error(`Failed to fetch ${path}`)
}

async function fetchEngines(): Promise<EngineInstance[]> {
  try {
    return await fetchJson<EngineInstance[]>('/engines')
  } catch (error) {
    throw normalizeError('engines', error)
  }
}

async function fetchProjects(): Promise<Project[]> {
  try {
    return await fetchJson<Project[]>('/projects')
  } catch (error) {
    throw normalizeError('projects', error)
  }
}

async function fetchExecutionsForEngine(engineId: string): Promise<Execution[]> {
  try {
    return await fetchJson<Execution[]>(`/executions/engine/${engineId}`)
  } catch (error) {
    throw normalizeError(`executions/engine/${engineId}`, error)
  }
}

export async function fetchCloudDashboardData(): Promise<CloudDashboardData> {
  const engines = await fetchEngines()
  const projects = await fetchProjects()

  const executionsByEngine = await Promise.all(
    engines.map(async (engine) => {
      try {
        return await fetchExecutionsForEngine(engine.id)
      } catch (error) {
        console.error(`Failed to fetch executions for engine ${engine.id}:`, error)
        return []
      }
    })
  )

  const executions = executionsByEngine
    .flat()
    .sort((left, right) => new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime())

  return {
    engines,
    projects,
    executions
  }
}
