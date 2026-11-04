import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Clock, FileCheck2, ShieldX } from 'lucide-react'
import { api } from '../api/client.js'
import EmptyState from '../components/EmptyState.jsx'
import Pagination from '../components/Pagination.jsx'
import { SkeletonGrid } from '../components/SkeletonCard.jsx'
import TypeBadge from '../components/TypeBadge.jsx'
import LevelBadge from '../components/LevelBadge.jsx'

const STATUS_TABS = [
  { value: '', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
]

const STATUS_STYLES = {
  PENDING: 'border-amber-400/30 bg-amber-500/10 text-amber-300',
  APPROVED: 'border-emerald-400/30 bg-emerald-500/10 text-emerald-300',
  REJECTED: 'border-rose-400/30 bg-rose-500/10 text-rose-300',
}

const STATUS_ICONS = {
  PENDING: Clock,
  APPROVED: FileCheck2,
  REJECTED: ShieldX,
}

export default function MySubmissionsPage() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    const params = new URLSearchParams({ page: String(page), size: '9' })
    if (status) params.set('status', status)
    api(`/opportunities/submissions/mine?${params.toString()}`)
      .then(setResult)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [status, page])

  useEffect(load, [load])

  const content = result?.content || []

  return (
    <section className="pt-10">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight text-white">My submissions</h1>
        <p className="mt-1.5 text-slate-400">
          Opportunities you shared, and where they stand with the review team.
        </p>
      </div>

      <div className="mb-6 flex flex-wrap gap-2">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value || 'all'}
            onClick={() => {
              setStatus(tab.value)
              setPage(0)
            }}
            className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
              status === tab.value
                ? 'border border-accent-400/40 bg-accent-500/15 text-accent-300'
                : 'border border-white/10 bg-white/5 text-slate-400 hover:text-slate-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {error && (
        <p className="mb-6 rounded-xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-300">
          {error}
        </p>
      )}
      {loading && <SkeletonGrid count={3} />}

      {!loading && !error && content.length === 0 && (
        <EmptyState title="No submissions yet">
          <p>
            Found a great hackathon or internship? <Link to="/submit" className="font-semibold text-accent-300 hover:text-accent-400">Submit it</Link>{' '}
            and track its status here.
          </p>
        </EmptyState>
      )}

      <div className="flex flex-col gap-4">
        {content.map((item, index) => {
          const Icon = STATUS_ICONS[item.status] || Clock
          return (
            <motion.article
              key={item.id}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: Math.min(index * 0.05, 0.3) }}
              className="glass glass-hover flex flex-col gap-3 rounded-2xl p-5 sm:flex-row sm:items-center"
            >
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <TypeBadge type={item.type} />
                  <LevelBadge level={item.level} />
                  <span
                    className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[0.7rem] font-semibold ${
                      STATUS_STYLES[item.status] || 'border-white/10 bg-white/5 text-slate-300'
                    }`}
                  >
                    <Icon className="size-3" />
                    {item.status?.charAt(0) + item.status?.slice(1).toLowerCase()}
                  </span>
                </div>
                <h3 className="mt-2 font-semibold text-white">{item.title}</h3>
                <p className="font-mono text-xs text-accent-300/80">{item.organization}</p>
                {item.description && <p className="mt-1.5 line-clamp-2 text-sm text-slate-400">{item.description}</p>}
              </div>
            </motion.article>
          )
        })}
      </div>

      {!loading && !error && (
        <Pagination
          page={result?.page ?? 0}
          totalPages={result?.totalPages}
          totalElements={result?.totalElements}
          onPage={setPage}
        />
      )}
    </section>
  )
}
