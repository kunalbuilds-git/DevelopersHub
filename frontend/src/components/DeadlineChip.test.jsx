import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import DeadlineChip from './DeadlineChip.jsx'

function formatExpected(value) {
  return new Date(value).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

const DEADLINE = '2027-03-12T00:00:00.000Z'

describe('DeadlineChip', () => {
  it('renders "Rolling" when there is no deadline', () => {
    render(<DeadlineChip deadline={null} daysUntilDeadline={null} />)

    expect(screen.getByText('Rolling')).toBeInTheDocument()
  })

  it('renders "Closes today" when the deadline is today', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={0} />)

    expect(screen.getByText('Closes today')).toBeInTheDocument()
    expect(screen.queryByText(formatExpected(DEADLINE))).not.toBeInTheDocument()
  })

  it('renders "1d left" when one day is left', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={1} />)

    expect(screen.getByText('1d left')).toBeInTheDocument()
  })

  it('renders "7d left" at the edge of the urgent window', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={7} />)

    expect(screen.getByText('7d left')).toBeInTheDocument()
  })

  it('renders "8d left" just outside the urgent window', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={8} />)

    expect(screen.getByText('8d left')).toBeInTheDocument()
  })

  it('renders "30d left" at the edge of the near window', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={30} />)

    expect(screen.getByText('30d left')).toBeInTheDocument()
  })

  it('renders the formatted date when more than 30 days are left', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={31} />)

    expect(screen.getByText(formatExpected(DEADLINE))).toBeInTheDocument()
  })

  it('renders the formatted date and strikes it through once the deadline passed', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={-3} />)

    const chip = screen.getByText(formatExpected(DEADLINE))
    expect(chip).toBeInTheDocument()
    expect(chip.className).toContain('line-through')
  })

  it('renders the formatted date when no day count is known', () => {
    render(<DeadlineChip deadline={DEADLINE} daysUntilDeadline={null} />)

    expect(screen.getByText(formatExpected(DEADLINE))).toBeInTheDocument()
    expect(screen.queryByText('Rolling')).not.toBeInTheDocument()
  })

  it('renders the raw value when the deadline cannot be parsed', () => {
    render(<DeadlineChip deadline="sometime next week" daysUntilDeadline={null} />)

    expect(screen.getByText('sometime next week')).toBeInTheDocument()
  })
})
