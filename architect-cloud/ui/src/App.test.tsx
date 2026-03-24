import { render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function jsonResponse<T>(body: T): Response {
  return {
    ok: true,
    json: vi.fn().mockResolvedValue(body)
  } as unknown as Response
}

describe('App', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('fetches cloud data through the typed API layer and renders summary cards', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse([
          {
            id: 'engine-1',
            hostname: 'localhost',
            port: 8080,
            version: '1.0.0',
            status: 'ACTIVE',
            createdAt: '2026-03-24T00:00:00Z',
            lastHeartbeat: '2026-03-24T00:00:00Z'
          }
        ])
      )
      .mockResolvedValueOnce(
        jsonResponse([
          {
            id: 'project-1',
            name: 'project',
            path: '/tmp/project',
            engineId: 'engine-1',
            description: null,
            createdAt: '2026-03-24T00:00:00Z'
          }
        ])
      )
      .mockResolvedValueOnce(
        jsonResponse([
          {
            id: 'exec-1',
            projectId: 'project-1',
            engineId: 'engine-1',
            taskId: 'build',
            status: 'RUNNING',
            message: null,
            errorDetails: null,
            startedAt: '2026-03-24T00:00:00Z',
            completedAt: null
          }
        ])
      )

    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    expect(screen.getByText('Loading cloud monitoring data...')).toBeInTheDocument()

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(3)
    })

    expect(fetchMock).toHaveBeenNthCalledWith(1, 'http://localhost:8080/api/engines')
    expect(fetchMock).toHaveBeenNthCalledWith(2, 'http://localhost:8080/api/projects')
    expect(fetchMock).toHaveBeenNthCalledWith(3, 'http://localhost:8080/api/executions/engine/engine-1')

    expect(await screen.findByText('Active engines')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-active-engines')).getByText('1')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-total-projects')).getByText('1')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-running-executions')).getByText('1')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-total-executions')).getByText('1')).toBeInTheDocument()
    expect(screen.getByText(/Last updated:/)).toBeInTheDocument()
  })

  it('shows a retryable error banner when the typed engine request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false
      } as Response)
    )

    render(<App />)

    expect(await screen.findByText(/Failed to fetch engines/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })
})
