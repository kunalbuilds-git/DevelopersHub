import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BookmarkX } from 'lucide-react'
import { api } from '../api/client.js'
import EmptyState from '../components/EmptyState.jsx'
import OpportunityCard from '../components/OpportunityCard.jsx'
import { SkeletonGrid } from '../components/SkeletonCard.jsx'
import { useSaved } from '../saved/SavedContext.jsx'

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
    <section className="pt-10">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-white">Saved opportunities</h1>
        <p className="mt-1.5 text-slate-400">
          Sorted by deadline — the soonest first, so nothing sneaks up on you.
        </p>
      </div>

      {error && (
        <p className="mb-6 rounded-xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-300">
          {error}
        </p>
      )}
      {loading && <SkeletonGrid count={3} />}

      {!loading && !error && bookmarks?.length === 0 && (
        <EmptyState title="Nothing saved yet">
          <p>
            Tap <strong className="text-amber-300">Save</strong> on any opportunity and it will show up here.{' '}
            <Link to="/" className="font-semibold text-accent-300 hover:text-accent-400">
              Browse opportunities →
            </Link>
          </p>
        </EmptyState>
      )}

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
        {(bookmarks || []).map((bookmark, index) => (
          <div key={bookmark.opportunityId} className="flex flex-col gap-2">
            <OpportunityCard
              index={index}
              opportunity={{
                id: bookmark.opportunityId,
                title: bookmark.title,
                organization: bookmark.organization,
                type: bookmark.type,
                level: null,
                description: null,
                url: null,
                deadline: bookmark.deadline,
                tags: [],
              }}
            />
            <button
              onClick={() => remove(bookmark.opportunityId)}
              className="self-end rounded-lg px-2 py-1 text-xs font-medium text-slate-500 transition hover:text-rose-300"
            >
              <BookmarkX className="mr-1 inline size-3.5" />
              Remove
            </button>
          </div>
        ))}
      </div>
    </section>
  )
}
