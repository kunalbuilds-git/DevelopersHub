import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { FlaskConical, Rocket, Search, Tag, X } from 'lucide-react'
import { api } from '../api/client.js'
import EmptyState from '../components/EmptyState.jsx'
import OpportunityCard from '../components/OpportunityCard.jsx'
import Pagination from '../components/Pagination.jsx'
import { SkeletonGrid } from '../components/SkeletonCard.jsx'
import { OPPORTUNITY_LEVELS } from '../components/LevelBadge.jsx'
import { OPPORTUNITY_TYPES } from '../components/TypeBadge.jsx'

const TYPE_ICONS = {
  HACKATHON: Rocket,
  INTERNSHIP: FlaskConical,
  PROGRAM: null,
  OPEN_SOURCE: null,
  EVENT: null,
}

const TYPE_PILL_LABELS = {
  ALL: 'All',
  HACKATHON: 'Hackathons',
  INTERNSHIP: 'Internships',
  PROGRAM: 'Programs',
  OPEN_SOURCE: 'Open Source',
  EVENT: 'Events',
}

export default function BrowsePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') || ''
  const type = searchParams.get('type') || ''
  const level = searchParams.get('level') || ''
  const tag = searchParams.get('tag') || ''
  const page = Math.max(0, parseInt(searchParams.get('page') || '0', 10) || 0)

  const [keywordInput, setKeywordInput] = useState(keyword)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  function updateParams(changes) {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(changes)) {
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
    }
    setSearchParams(next)
  }

  useEffect(() => setKeywordInput(keyword), [keyword])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    const params = new URLSearchParams({ page: String(page), size: '9' })
    if (keyword) params.set('keyword', keyword)
    if (type) params.set('type', type)
    if (level) params.set('level', level)
    if (tag) params.set('tag', tag)
    api(`/opportunities?${params.toString()}`, { auth: false })
      .then((data) => {
        if (!cancelled) setResult(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [page, keyword, type, level, tag])

  const content = result?.content || []
  const pageNumber = result ? (result.page ?? result.number ?? 0) : 0
  const hasFilters = keyword || type || level || tag

  return (
    <section>
      {/* Hero */}
      <div className="relative overflow-hidden">
        <div className="grid-bg pointer-events-none absolute inset-0" />
        <div className="pointer-events-none absolute -top-24 left-1/4 size-72 animate-float rounded-full bg-accent-600/20 blur-3xl" />
        <div className="pointer-events-none absolute -top-10 right-10 size-56 animate-pulse-slow rounded-full bg-fuchsia-glow/10 blur-3xl" />
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
          className="relative py-12 text-center"
        >
          <p className="mx-auto mb-4 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3.5 py-1 font-mono text-xs text-accent-300">
            <span className="size-1.5 animate-pulse rounded-full bg-emerald-400" />
            built for student developers
          </p>
          <h1 className="mx-auto max-w-2xl text-4xl font-extrabold tracking-tight text-white sm:text-5xl">
            Never miss a <span className="text-gradient">deadline</span> again.
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-balance text-[0.98rem] leading-relaxed text-slate-400">
            Hackathons, internships, company events, and beginner-friendly open source —
            searchable, filterable by level, and saved in one place.
          </p>
        </motion.div>
      </div>

      {/* Filters */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, delay: 0.1, ease: 'easeOut' }}
        className="glass sticky top-16 z-30 mb-8 space-y-3 rounded-2xl p-3"
      >
        <div className="flex flex-wrap items-center gap-2">
          {['ALL', ...OPPORTUNITY_TYPES].map((t) => {
            const Icon = TYPE_ICONS[t]
            const active = (t === 'ALL' && !type) || type === t
            return (
              <button
                key={t}
                onClick={() => updateParams({ type: t === 'ALL' ? '' : t, page: '' })}
                className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-sm font-medium transition ${
                  active
                    ? 'bg-gradient-to-r from-accent-600 to-accent-500 text-white shadow-md shadow-accent-600/30'
                    : 'border border-white/10 bg-white/5 text-slate-400 hover:text-slate-200'
                }`}
              >
                {Icon && <Icon className="size-3.5" />}
                {TYPE_PILL_LABELS[t]}
              </button>
            )
          })}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Level</span>
          {['', ...OPPORTUNITY_LEVELS].map((l) => {
            const active = level === l
            return (
              <button
                key={l || 'any'}
                onClick={() => updateParams({ level: l, page: '' })}
                className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
                  active
                    ? 'bg-white/15 text-white'
                    : 'text-slate-500 hover:text-slate-300'
                }`}
              >
                {l === '' ? 'Any' : l.charAt(0) + l.slice(1).toLowerCase()}
              </button>
            )
          })}

          <div className="relative ml-auto min-w-52 flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-500" />
            <input
              className="w-full rounded-xl border border-white/10 bg-white/5 py-2 pl-9 pr-3 text-sm text-slate-200 placeholder:text-slate-500 focus:border-accent-400/50 focus:outline-none focus:ring-2 focus:ring-accent-500/20"
              type="search"
              placeholder="Search by title or organization…"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
              aria-label="Search opportunities"
            />
          </div>

          <div className="relative">
            <Tag className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-slate-500" />
            <input
              className="w-36 rounded-xl border border-white/10 bg-white/5 py-2 pl-8 pr-3 text-sm text-slate-200 placeholder:text-slate-500 focus:border-accent-400/50 focus:outline-none"
              type="text"
              placeholder="tag: java"
              value={tag}
              onChange={(event) => updateParams({ tag: event.target.value.trim().toLowerCase(), page: '' })}
              aria-label="Filter by tag"
            />
          </div>

          {hasFilters && (
            <button
              type="button"
              onClick={() => {
                setKeywordInput('')
                setSearchParams(new URLSearchParams())
              }}
              className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-slate-400 transition hover:text-white"
            >
              <X className="size-3.5" /> Clear
            </button>
          )}
        </div>
      </motion.div>

      {error && (
        <p className="mb-6 rounded-xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-300">
          {error}
        </p>
      )}

      {loading && <SkeletonGrid count={6} />}

      {!loading && !error && content.length === 0 && (
        <EmptyState title="No opportunities found">
          <p>Try clearing the filters or checking back later — new stuff lands all the time.</p>
        </EmptyState>
      )}

      {!loading && !error && content.length > 0 && (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
          {content.map((opportunity, index) => (
            <OpportunityCard key={opportunity.id} opportunity={opportunity} index={index} />
          ))}
        </div>
      )}

      {!loading && !error && (
        <Pagination
          page={pageNumber}
          totalPages={result?.totalPages}
          totalElements={result?.totalElements}
          onPage={(next) => updateParams({ page: String(next) })}
        />
      )}
    </section>
  )
}
