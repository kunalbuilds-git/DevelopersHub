import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import TypeBadge, { OPPORTUNITY_TYPES } from './TypeBadge.jsx'

describe('TypeBadge', () => {
  it('renders the Hackathon label', () => {
    render(<TypeBadge type="HACKATHON" />)

    expect(screen.getByText('Hackathon')).toBeInTheDocument()
  })

  it('renders the Internship label', () => {
    render(<TypeBadge type="INTERNSHIP" />)

    expect(screen.getByText('Internship')).toBeInTheDocument()
  })

  it('renders the Program label', () => {
    render(<TypeBadge type="PROGRAM" />)

    expect(screen.getByText('Program')).toBeInTheDocument()
  })

  it('renders the Open Source label', () => {
    render(<TypeBadge type="OPEN_SOURCE" />)

    expect(screen.getByText('Open Source')).toBeInTheDocument()
  })

  it('renders the Company Event label for the EVENT type', () => {
    render(<TypeBadge type="EVENT" />)

    expect(screen.getByText('Company Event')).toBeInTheDocument()
  })

  it('falls back to the raw value for an unknown type', () => {
    render(<TypeBadge type="CONFERENCE" />)

    expect(screen.getByText('CONFERENCE')).toBeInTheDocument()
  })

  it('exports the five known types', () => {
    expect(OPPORTUNITY_TYPES).toEqual([
      'HACKATHON',
      'INTERNSHIP',
      'PROGRAM',
      'OPEN_SOURCE',
      'EVENT',
    ])
  })
})
