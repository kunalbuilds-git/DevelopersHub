import { useState } from 'react'
import { Star } from 'lucide-react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useSaved } from '../saved/SavedContext.jsx'

/**
 * Bookmark toggle. Guests are sent to the login page; a 409 ("already
 * bookmarked") or 404 ("already removed") is reconciled to the server's truth.
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
        className="inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-semibold text-slate-300 transition hover:border-accent-400/40 hover:text-accent-300"
        title="Log in to save opportunities"
        onClick={() => {
          window.location.href = '/login'
        }}
      >
        <Star className="size-3.5" /> Save
      </button>
    )
  }

  const saved = isSaved(opportunityId)
  return (
    <span className="relative inline-flex">
      <button
        onClick={toggle}
        disabled={busy}
        title={saved ? 'Remove from saved list' : 'Save this opportunity'}
        className={`inline-flex items-center gap-1 rounded-full border px-3 py-1 text-xs font-semibold transition disabled:opacity-50 ${
          saved
            ? 'border-amber-400/40 bg-amber-400/15 text-amber-300 hover:bg-amber-400/25'
            : 'border-white/10 bg-white/5 text-slate-300 hover:border-accent-400/40 hover:text-accent-300'
        }`}
      >
        <Star className={`size-3.5 ${saved ? 'fill-amber-300' : ''}`} />
        {saved ? 'Saved' : 'Save'}
      </button>
      {error && (
        <span className="absolute right-0 top-full z-10 mt-1 whitespace-nowrap rounded-md bg-rose-500/90 px-2 py-1 text-[0.68rem] font-medium text-white">
          {error}
        </span>
      )}
    </span>
  )
}
