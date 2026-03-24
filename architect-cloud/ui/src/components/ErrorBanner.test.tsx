import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ErrorBanner } from './ErrorBanner'

describe('ErrorBanner', () => {
  it('renders the error message and calls retry', () => {
    const onRetry = vi.fn()

    render(<ErrorBanner message="Failed to fetch engines" onRetry={onRetry} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Failed to fetch engines')
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
