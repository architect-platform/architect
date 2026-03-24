import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SummaryStats } from './SummaryStats'

describe('SummaryStats', () => {
  it('renders typed dashboard stat values and last updated text', () => {
    render(
      <SummaryStats
        lastUpdated="2026-03-24T20:00:00Z"
        stats={{
          activeEngines: 2,
          totalProjects: 4,
          runningExecutions: 1,
          totalExecutions: 9
        }}
      />
    )

    expect(within(screen.getByTestId('stat-active-engines')).getByText('2')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-total-projects')).getByText('4')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-running-executions')).getByText('1')).toBeInTheDocument()
    expect(within(screen.getByTestId('stat-total-executions')).getByText('9')).toBeInTheDocument()
    expect(screen.getByText(/Last updated:/)).toBeInTheDocument()
  })
})
