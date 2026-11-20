import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import OpportunityCard from './OpportunityCard.jsx'

const { bookmarkSpy } = vi.hoisted(() => ({ bookmarkSpy: vi.fn() }))

// The card only forwards the opportunity id - the button itself needs the
// auth and saved contexts, which are covered by BookmarkButton's own test.
vi.mock('./BookmarkButton.jsx', () => ({
  default: (props) => {
    bookmarkSpy(props.opportunityId)
    return null
  },
}))

const opportunity = {
  id: 42,
  title: 'Build a Rocket',
  organization: 'DevsHub Labs',
  type: 'HACKATHON',
  level: 'BEGINNER',
  deadline: '2027-03-12T00:00:00.000Z',
  daysUntilDeadline: 5,
  description: 'Ship something wild.',
  url: 'https://example.com/rocket',
  tags: [{ id: 1, name: 'rust' }],
}

beforeEach(() => {
  bookmarkSpy.mockClear()
})

describe('OpportunityCard', () => {
  it('renders the title as a heading', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByRole('heading', { name: /Build a Rocket/ })).toBeInTheDocument()
  })

  it('renders the title as a plain text when there is no url', () => {
    render(<OpportunityCard opportunity={{ ...opportunity, url: null }} />)

    expect(screen.getByRole('heading', { name: 'Build a Rocket' })).toBeInTheDocument()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('links the title to the opportunity url', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByRole('link', { name: /Build a Rocket/ })).toHaveAttribute(
      'href',
      'https://example.com/rocket',
    )
  })

  it('renders the organization', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByText('DevsHub Labs')).toBeInTheDocument()
  })

  it('renders the type badge', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByText('Hackathon')).toBeInTheDocument()
  })

  it('renders the level badge', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByText('Beginner')).toBeInTheDocument()
  })

  it('renders the deadline chip', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByText('5d left')).toBeInTheDocument()
  })

  it('renders the description', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByText('Ship something wild.')).toBeInTheDocument()
  })

  it('renders the tags', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(screen.getByText('#rust')).toBeInTheDocument()
  })

  it('renders tags that carry no id', () => {
    render(<OpportunityCard opportunity={{ ...opportunity, tags: [{ name: 'go' }] }} />)

    expect(screen.getByText('#go')).toBeInTheDocument()
  })

  it('renders without a description or tags', () => {
    render(
      <OpportunityCard
        opportunity={{ ...opportunity, description: null, tags: undefined }}
      />,
    )

    expect(screen.getByRole('heading', { name: /Build a Rocket/ })).toBeInTheDocument()
    expect(screen.queryByText('#rust')).not.toBeInTheDocument()
  })

  it('passes the opportunity id to the bookmark button', () => {
    render(<OpportunityCard opportunity={opportunity} />)

    expect(bookmarkSpy).toHaveBeenCalledWith(42)
  })
})
