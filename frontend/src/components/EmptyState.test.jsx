import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import EmptyState from './EmptyState.jsx'

describe('EmptyState', () => {
  it('renders the title', () => {
    render(<EmptyState title="No opportunities found">nothing here</EmptyState>)

    expect(screen.getByText('No opportunities found')).toBeInTheDocument()
  })

  it('renders its children', () => {
    render(
      <EmptyState title="No opportunities found">
        <p>Try clearing the filters.</p>
      </EmptyState>,
    )

    expect(screen.getByText('Try clearing the filters.')).toBeInTheDocument()
  })
})
