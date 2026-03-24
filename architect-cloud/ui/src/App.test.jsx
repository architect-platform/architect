import { createElement } from 'react'
import { render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function jsonResponse(body) {
  return {
    ok: true,
    json: vi.fn().mockResolvedValue(body)
  }
}

describe('App', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('fetches cloud data and renders the summary cards', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse([{ id: 'engine-1', status: 'ACTIVE' }]))
      .mockResolvedValueOnce(jsonResponse([{ id: 'project-1' }]))
      .mockResolvedValueOnce(
        jsonResponse([{ id: 'exec-1', status: 'RUNNING', startedAt: '2026-03-24T00:00:00Z' }])
      )

    vi.stubGlobal('fetch', fetchMock)

    render(createElement(App))

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
  })

  it('shows a retryable error banner when the engine request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false
      })
    )

    render(createElement(App))

    expect(await screen.findByText(/Failed to fetch engines/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })
})
