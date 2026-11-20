import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import LevelBadge, { OPPORTUNITY_LEVELS } from './LevelBadge.jsx'

describe('LevelBadge', () => {
  it('renders nothing when the level is missing', () => {
    const { container } = render(<LevelBadge />)

    expect(container).toBeEmptyDOMElement()
  })

  it('renders nothing for the ANY level', () => {
    const { container } = render(<LevelBadge level="ANY" />)

    expect(container).toBeEmptyDOMElement()
  })

  it('renders the Beginner label', () => {
    render(<LevelBadge level="BEGINNER" />)

    expect(screen.getByText('Beginner')).toBeInTheDocument()
  })

  it('renders the Intermediate label', () => {
    render(<LevelBadge level="INTERMEDIATE" />)

    expect(screen.getByText('Intermediate')).toBeInTheDocument()
  })

  it('renders the Advanced label', () => {
    render(<LevelBadge level="ADVANCED" />)

    expect(screen.getByText('Advanced')).toBeInTheDocument()
  })

  it('falls back to the raw value for an unknown level', () => {
    render(<LevelBadge level="EXPERT" />)

    expect(screen.getByText('EXPERT')).toBeInTheDocument()
  })

  it('exports the three known levels', () => {
    expect(OPPORTUNITY_LEVELS).toEqual(['BEGINNER', 'INTERMEDIATE', 'ADVANCED'])
  })
})
