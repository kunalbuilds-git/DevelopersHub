import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client.js'
import OpportunityCard from '../components/OpportunityCard.jsx'
import Pagination from '../components/Pagination.jsx'

const TYPES = ['HACKATHON', 'INTERNSHIP', 'PROGRAM', 'OPEN_SOURCE']

export default function BrowsePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') || ''
  const type = searchParams.get('type') || ''
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

  // Keep the input in sync when the URL changes (back button, filters reset).
  useEffect(() => setKeywordInput(keyword), [keyword])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    const params = new URLSearchParams({ page: String(page), size: '9' })
    if (keyword) params.set('keyword', keyword)
    if (type) params.set('type', type)
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
  }, [page, keyword, type, tag])

  const content = result?.content || []
  // The backend used two pagination shapes across v0.1/v0.2; read both.
  const pageNumber = result ? (result.page ?? result.number ?? 0) : 0

  return (
    <section>
      <div className="page-head">
        <h1>Find your next opportunity</h1>
        <p className="page-subtitle">
          Hackathons, internships, programs, and beginner-friendly open source — in one place.
        </p>
      </div>

      <form
        className="filters"
        onSubmit={(event) => {
          event.preventDefault()
          updateParams({ keyword: keywordInput.trim(), page: '' })
        }}
      >
        <input
          className="input filter-search"
          type="search"
          placeholder="Search by title or organization…"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
          aria-label="Search opportunities"
        />
        <select
          className="input filter-type"
          value={type}
          onChange={(event) => updateParams({ type: event.target.value, page: '' })}
          aria-label="Filter by type"
        >
          <option value="">All types</option>
          {TYPES.map((t) => (
            <option key={t} value={t}>
              {t.charAt(0) + t.slice(1).toLowerCase().replace('_', ' ')}
            </option>
          ))}
        </select>
        <input
          className="input filter-tag"
          type="text"
          placeholder="Tag (e.g. java)"
          value={tag}
          onChange={(event) => updateParams({ tag: event.target.value.trim().toLowerCase(), page: '' })}
          aria-label="Filter by tag"
        />
        <button className="btn btn-primary" type="submit">
          Search
        </button>
        {(keyword || type || tag) && (
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => {
              setKeywordInput('')
              setSearchParams(new URLSearchParams())
            }}
          >
            Clear
          </button>
        )}
      </form>

      {error && <p className="alert alert-error">{error}</p>}
      {loading && <p className="muted">Loading opportunities…</p>}

      {!loading && !error && content.length === 0 && (
        <div className="empty-state">
          <p className="empty-title">No opportunities found</p>
          <p className="muted">Try clearing the filters or checking back later.</p>
        </div>
      )}

      <div className="card-grid">
        {content.map((opportunity) => (
          <OpportunityCard key={opportunity.id} opportunity={opportunity} />
        ))}
      </div>

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
