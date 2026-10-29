import { useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useSaved } from '../saved/SavedContext.jsx'

/**
 * Bookmark toggle. Guests are sent to the login page; a 409 from the backend
 * ("already bookmarked") is reconciled to the saved state.
 */
export default function BookmarkButton({ opportunityId }) {
  const { isLoggedIn } = useAuth()
  const { isSaved, markSaved } = useSaved()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function toggle() {
    if (busy) return
    setError(null)
    setBusy(true)
    const currentlySaved = isSaved(opportunityId)
    try {
      if (currentlySaved) {
        await api(`/bookmarks/${opportunityId}`, { method: 'DELETE' })
        markSaved(opportunityId, false)
      } else {
        await api(`/bookmarks/${opportunityId}`, { method: 'POST' })
        markSaved(opportunityId, true)
      }
    } catch (err) {
      // 409 = already saved, 404 = already removed: reconcile to the server's truth.
      if (err.status === 409) {
        markSaved(opportunityId, true)
      } else if (err.status === 404) {
        markSaved(opportunityId, false)
      } else {
        setError(err.message)
      }
    } finally {
      setBusy(false)
    }
  }

  if (!isLoggedIn) {
    return (
      <button
        className="btn btn-ghost btn-bookmark"
        title="Log in to save opportunities"
        onClick={() => {
          window.location.href = '/login'
        }}
      >
        ☆ Save
      </button>
    )
  }

  const saved = isSaved(opportunityId)
  return (
    <button
      className={`btn btn-bookmark ${saved ? 'btn-saved' : 'btn-ghost'}`}
      onClick={toggle}
      disabled={busy}
      title={saved ? 'Remove from saved list' : 'Save this opportunity'}
    >
      {saved ? '★ Saved' : '☆ Save'}
      {error && <span className="bookmark-error">{error}</span>}
    </button>
  )
}
