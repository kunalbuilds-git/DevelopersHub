import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

const { useAuth, useSaved, api } = vi.hoisted(() => ({
  useAuth: vi.fn(),
  useSaved: vi.fn(),
  api: vi.fn(),
}))

vi.mock('../auth/AuthContext.jsx', () => ({ useAuth }))
vi.mock('../saved/SavedContext.jsx', () => ({ useSaved }))
vi.mock('../api/client.js', () => ({ api }))

import BookmarkButton from './BookmarkButton.jsx'

const OPPORTUNITY_ID = 42

let savedIds
let markSaved

function signIn() {
  useAuth.mockReturnValue({ isLoggedIn: true })
}

function signOut() {
  useAuth.mockReturnValue({ isLoggedIn: false })
}

beforeEach(() => {
  savedIds = new Set()
  markSaved = vi.fn((id, saved) => {
    if (saved) savedIds.add(id)
    else savedIds.delete(id)
  })
  useSaved.mockImplementation(() => ({
    savedIds,
    markSaved,
    isSaved: (id) => savedIds.has(id),
  }))
  api.mockReset()
})

afterEach(() => {
  // jsdom cannot navigate, so the guest redirect is captured instead.
  vi.unstubAllGlobals()
})

describe('BookmarkButton', () => {
  it('sends a guest to the login page', async () => {
    const user = userEvent.setup()
    signOut()
    vi.stubGlobal('location', { href: '' })
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    const button = screen.getByRole('button', { name: 'Save' })
    expect(button).toHaveAttribute('title', 'Log in to save opportunities')

    await user.click(button)

    expect(window.location.href).toBe('/login')
    expect(api).not.toHaveBeenCalled()
  })

  it('saves an opportunity that is not saved yet', async () => {
    const user = userEvent.setup()
    signIn()
    api.mockResolvedValue(null)
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(api).toHaveBeenCalledTimes(1)
    expect(api).toHaveBeenCalledWith(`/bookmarks/${OPPORTUNITY_ID}`, { method: 'POST' })
    expect(markSaved).toHaveBeenCalledWith(OPPORTUNITY_ID, true)
    expect(await screen.findByRole('button', { name: 'Saved' })).toBeInTheDocument()
  })

  it('removes an opportunity that is already saved', async () => {
    const user = userEvent.setup()
    signIn()
    savedIds.add(OPPORTUNITY_ID)
    api.mockResolvedValue(null)
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    await user.click(screen.getByRole('button', { name: 'Saved' }))

    expect(api).toHaveBeenCalledTimes(1)
    expect(api).toHaveBeenCalledWith(`/bookmarks/${OPPORTUNITY_ID}`, { method: 'DELETE' })
    expect(markSaved).toHaveBeenCalledWith(OPPORTUNITY_ID, false)
    expect(await screen.findByRole('button', { name: 'Save' })).toBeInTheDocument()
  })

  it('disables the button while the request is in flight', async () => {
    const user = userEvent.setup()
    signIn()
    api.mockImplementation(() => new Promise(() => {}))
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
    })
  })

  it('reconciles a 409 with the server state and stays quiet', async () => {
    const user = userEvent.setup()
    signIn()
    const conflict = new Error('Already bookmarked')
    conflict.status = 409
    api.mockRejectedValue(conflict)
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(markSaved).toHaveBeenCalledWith(OPPORTUNITY_ID, true)
    })
    expect(screen.queryByText('Already bookmarked')).not.toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Saved' })).toBeInTheDocument()
  })

  it('reconciles a 404 with the server state and stays quiet', async () => {
    const user = userEvent.setup()
    signIn()
    savedIds.add(OPPORTUNITY_ID)
    const missing = new Error('Bookmark not found')
    missing.status = 404
    api.mockRejectedValue(missing)
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    await user.click(screen.getByRole('button', { name: 'Saved' }))

    await waitFor(() => {
      expect(markSaved).toHaveBeenCalledWith(OPPORTUNITY_ID, false)
    })
    expect(screen.queryByText('Bookmark not found')).not.toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Save' })).toBeInTheDocument()
  })

  it('shows the error message for any other failure', async () => {
    const user = userEvent.setup()
    signIn()
    api.mockRejectedValue(new Error('Network unreachable'))
    render(<BookmarkButton opportunityId={OPPORTUNITY_ID} />)

    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Network unreachable')).toBeInTheDocument()
    expect(markSaved).not.toHaveBeenCalled()
  })
})
