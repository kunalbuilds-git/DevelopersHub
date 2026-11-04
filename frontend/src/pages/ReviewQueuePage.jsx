import { useCallback, useEffect, useState } from 'react'
import { Check, ExternalLink, Inbox, X } from 'lucide-react'
import { api } from '../api/client.js'
import EmptyState from '../components/EmptyState.jsx'
import LevelBadge from '../components/LevelBadge.jsx'
import { SkeletonGrid } from '../components/SkeletonCard.jsx'
import TypeBadge from '../components/TypeBadge.jsx'

const STATUS_TABS = ['PENDING', 'APPROVED', 'REJECTED']

const TAB_STYLES = {
  PENDING: 'text-amber-300 border-amber-400/30 bg-amber-500/10',
  APPROVED: 'text-emerald-300 border-emerald-400/30 bg-emerald-500/10',
  REJECTED: 'text-rose-300 border-rose-400/30 bg-rose-500/10',
}

export default function ReviewQueuePage() {
  const [status, setStatus] = useState('PENDING')
  const [items, setItems] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    api(`/opportunities/submissions?status=${status}&size=50`)
      .then((page) => setItems(page.content || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [status])

  useEffect(load, [load])

  async function review(id, action) {
    setBusyId(id)
    setError(null)
    try {
      await api(`/opportunities/submissions/${id}/${action}`, { method: 'POST' })
      setItems((current) => current.filter((item) => item.id !== id))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="pt-10">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight text-white">Review queue</h1>
        <p className="mt-1.5 text-slate-400">
          User submissions waiting to go live. Approving publishes them to everyone.
        </p>
      </div>

      <div className="mb-6 flex gap-2">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setStatus(tab)}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              status === tab
                ? TAB_STYLES[tab]
                : 'border border-white/10 bg-white/5 text-slate-400 hover:text-slate-200'
            }`}
          >
            {tab.charAt(0) + tab.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {error && (
        <p className="mb-6 rounded-xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-300">
          {error}
        </p>
      )}
      {loading && <SkeletonGrid count={3} />}

      {!loading && !error && items?.length === 0 && (
        <EmptyState title={`No ${status.toLowerCase()} submissions`}>
          <p>The queue is clear — new submissions from users will appear here.</p>
        </EmptyState>
      )}

      <div className="flex flex-col gap-4">
        {(items || []).map((item) => (
          <article key={item.id} className="glass glass-hover flex flex-col gap-4 rounded-2xl p-5 sm:flex-row sm:items-center">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <TypeBadge type={item.type} />
                <LevelBadge level={item.level} />
                {item.deadline && (
                  <span className="font-mono text-xs text-slate-500">
                    due {new Date(item.deadline).toLocaleDateString()}
                  </span>
                )}
              </div>
              <h3 className="mt-2 flex items-center gap-1.5 font-semibold text-white">
                {item.title}
                {item.url && <ExternalLink className="size-3.5 text-slate-500" />}
              </h3>
              <p className="font-mono text-xs text-accent-300/80">{item.organization}</p>
              {item.description && <p className="mt-1.5 line-clamp-2 text-sm text-slate-400">{item.description}</p>}
              {(item.tags || []).length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {item.tags.map((tag) => (
                    <span key={tag.id ?? tag.name} className="rounded-full border border-white/10 bg-white/5 px-2 py-0.5 font-mono text-[0.7rem] text-slate-400">
                      #{tag.name}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {item.status === 'PENDING' && (
              <div className="flex shrink-0 gap-2">
                <button
                  disabled={busyId === item.id}
                  onClick={() => review(item.id, 'approve')}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-400/30 bg-emerald-500/15 px-4 py-2 text-sm font-semibold text-emerald-300 transition hover:bg-emerald-500/25 disabled:opacity-50"
                >
                  <Check className="size-4" /> Approve
                </button>
                <button
                  disabled={busyId === item.id}
                  onClick={() => review(item.id, 'reject')}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-rose-400/30 bg-rose-500/10 px-4 py-2 text-sm font-semibold text-rose-300 transition hover:bg-rose-500/20 disabled:opacity-50"
                >
                  <X className="size-4" /> Reject
                </button>
              </div>
            )}
          </article>
        ))}
      </div>

      {!loading && items?.length > 0 && (
        <p className="mt-6 flex items-center justify-center gap-2 text-sm text-slate-500">
          <Inbox className="size-4" /> end of the {status.toLowerCase()} queue
        </p>
      )}
    </section>
  )
}
