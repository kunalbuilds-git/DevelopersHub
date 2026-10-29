import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import { useSaved } from '../saved/SavedContext.jsx'
import OpportunityCard from '../components/OpportunityCard.jsx'

export default function SavedPage() {
  const [bookmarks, setBookmarks] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const { markSaved } = useSaved()

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    api('/bookmarks')
      .then(setBookmarks)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  async function remove(opportunityId) {
    try {
      await api(`/bookmarks/${opportunityId}`, { method: 'DELETE' })
    } catch (err) {
      if (err.status !== 404) {
        setError(err.message)
        return
      }
    }
    // Keep the shared saved-state in sync so card toggles stay correct.
    markSaved(opportunityId, false)
    setBookmarks((current) => current.filter((b) => b.opportunityId !== opportunityId))
  }

  return (
    <section>
      <div className="page-head">
        <h1>Saved opportunities</h1>
        <p className="page-subtitle">Sorted by deadline — the soonest first, so nothing sneaks up on you.</p>
      </div>

      {error && <p className="alert alert-error">{error}</p>}
      {loading && <p className="muted">Loading your saved list…</p>}

      {!loading && !error && bookmarks?.length === 0 && (
        <div className="empty-state">
          <p className="empty-title">Nothing saved yet</p>
          <p className="muted">
            Tap <strong>☆ Save</strong> on any opportunity and it will show up here.{' '}
            <Link to="/">Browse opportunities →</Link>
          </p>
        </div>
      )}

      <div className="card-grid">
        {(bookmarks || []).map((bookmark) => (
          <div key={bookmark.opportunityId} className="saved-item">
            <OpportunityCard
              opportunity={{
                id: bookmark.opportunityId,
                title: bookmark.title,
                organization: bookmark.organization,
                type: bookmark.type,
                description: null,
                url: null,
                deadline: bookmark.deadline,
                tags: [],
              }}
            />
            <button className="btn btn-ghost saved-remove" onClick={() => remove(bookmark.opportunityId)}>
              Remove
            </button>
          </div>
        ))}
      </div>
    </section>
  )
}
