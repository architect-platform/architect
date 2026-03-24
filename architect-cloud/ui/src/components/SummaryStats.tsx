import type { CloudDashboardStats } from '../types/cloud'

interface SummaryStatsProps {
  stats: CloudDashboardStats
  lastUpdated: string | null
}

const STAT_CARDS: Array<{
  id: string
  label: string
  getValue: (stats: CloudDashboardStats) => number
}> = [
  {
    id: 'active-engines',
    label: 'Active engines',
    getValue: (stats) => stats.activeEngines
  },
  {
    id: 'total-projects',
    label: 'Tracked projects',
    getValue: (stats) => stats.totalProjects
  },
  {
    id: 'running-executions',
    label: 'Running executions',
    getValue: (stats) => stats.runningExecutions
  },
  {
    id: 'total-executions',
    label: 'Total executions',
    getValue: (stats) => stats.totalExecutions
  }
]

function formatLastUpdated(lastUpdated: string | null): string {
  if (!lastUpdated) {
    return 'Waiting for first successful refresh'
  }

  return new Date(lastUpdated).toLocaleString()
}

export function SummaryStats({ stats, lastUpdated }: SummaryStatsProps) {
  return (
    <>
      <section className="stats-grid" aria-label="Cloud monitoring summary">
        {STAT_CARDS.map((card) => (
          <article className="stat-card" data-testid={`stat-${card.id}`} key={card.id}>
            <span className="stat-label">{card.label}</span>
            <strong className="stat-value">{card.getValue(stats)}</strong>
          </article>
        ))}
      </section>

      <p className="last-updated">Last updated: {formatLastUpdated(lastUpdated)}</p>
    </>
  )
}
