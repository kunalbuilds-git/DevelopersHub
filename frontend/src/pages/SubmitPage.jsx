import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, SendHorizonal } from 'lucide-react'
import { api } from '../api/client.js'
import { OPPORTUNITY_LEVELS } from '../components/LevelBadge.jsx'
import { OPPORTUNITY_TYPES } from '../components/TypeBadge.jsx'

const inputClass =
  'w-full rounded-xl border border-white/10 bg-white/5 px-3.5 py-2.5 text-sm text-white placeholder:text-slate-500 focus:border-accent-400/50 focus:outline-none focus:ring-2 focus:ring-accent-500/20'
const labelClass = 'text-xs font-semibold uppercase tracking-wider text-slate-400'

export default function SubmitPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    title: '',
    organization: '',
    type: 'HACKATHON',
    level: 'ANY',
    deadline: '',
    url: '',
    description: '',
    tags: '',
  })
  const [error, setError] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)

  function set(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      const body = {
        title: form.title.trim(),
        organization: form.organization.trim(),
        type: form.type,
        level: form.level,
        description: form.description.trim() || null,
        url: form.url.trim() || null,
        tags: form.tags
          .split(',')
          .map((t) => t.trim().toLowerCase())
          .filter(Boolean),
      }
      if (form.deadline) body.deadline = `${form.deadline}T23:59:00`
      await api('/opportunities/submissions', { method: 'POST', body })
      setSubmitted(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (submitted) {
    return (
      <div className="glass mx-auto mt-16 max-w-lg rounded-3xl p-10 text-center">
        <div className="mx-auto grid size-14 place-items-center rounded-2xl border border-emerald-400/30 bg-emerald-500/15">
          <CheckCircle2 className="size-7 text-emerald-300" />
        </div>
        <h1 className="mt-5 text-2xl font-bold text-white">Submission received</h1>
        <p className="mt-2 text-sm leading-relaxed text-slate-400">
          Thanks for contributing! An admin will review it shortly — once approved, it shows up in the
          public listing for everyone.
        </p>
        <div className="mt-7 flex justify-center gap-3">
          <button
            onClick={() => navigate('/')}
            className="rounded-xl bg-gradient-to-r from-accent-600 to-accent-500 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-accent-600/30 transition hover:brightness-110"
          >
            Back to browsing
          </button>
          <button
            onClick={() => {
              setSubmitted(false)
              setForm({ title: '', organization: '', type: 'HACKATHON', level: 'ANY', deadline: '', url: '', description: '', tags: '' })
            }}
            className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-medium text-slate-300 transition hover:text-white"
          >
            Submit another
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto mt-10 max-w-2xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-white">Submit an opportunity</h1>
        <p className="mt-1.5 text-slate-400">
          Found a great hackathon or internship? Share it — submissions go live once an admin approves them.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="glass flex flex-col gap-5 rounded-3xl p-7">
        <div className="grid gap-5 sm:grid-cols-2">
          <label className="flex flex-col gap-1.5">
            <span className={labelClass}>Title *</span>
            <input
              className={inputClass}
              required
              value={form.title}
              onChange={(e) => set('title', e.target.value)}
              placeholder="Spring AI Hackathon"
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className={labelClass}>Organization *</span>
            <input
              className={inputClass}
              required
              value={form.organization}
              onChange={(e) => set('organization', e.target.value)}
              placeholder="Google DSC"
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className={labelClass}>Type *</span>
            <select className={inputClass} value={form.type} onChange={(e) => set('type', e.target.value)}>
              {OPPORTUNITY_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t.charAt(0) + t.slice(1).toLowerCase().replace('_', ' ')}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5">
            <span className={labelClass}>Level</span>
            <select className={inputClass} value={form.level} onChange={(e) => set('level', e.target.value)}>
              <option value="ANY">Any level</option>
              {OPPORTUNITY_LEVELS.map((l) => (
                <option key={l} value={l}>
                  {l.charAt(0) + l.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1.5">
            <span className={labelClass}>Deadline</span>
            <input
              className={inputClass}
              type="date"
              value={form.deadline}
              onChange={(e) => set('deadline', e.target.value)}
            />
          </label>
          <label className="flex flex-col gap-1.5">
            <span className={labelClass}>Link</span>
            <input
              className={inputClass}
              type="url"
              value={form.url}
              onChange={(e) => set('url', e.target.value)}
              placeholder="https://…"
            />
          </label>
        </div>

        <label className="flex flex-col gap-1.5">
          <span className={labelClass}>Description</span>
          <textarea
            className={`${inputClass} min-h-24 resize-y`}
            value={form.description}
            onChange={(e) => set('description', e.target.value)}
            placeholder="What is it, who is it for, why is it great?"
          />
        </label>

        <label className="flex flex-col gap-1.5">
          <span className={labelClass}>Tags (comma separated)</span>
          <input
            className={inputClass}
            value={form.tags}
            onChange={(e) => set('tags', e.target.value)}
            placeholder="ai, beginners, remote"
          />
        </label>

        {error && (
          <p className="rounded-xl border border-rose-400/30 bg-rose-500/10 px-3.5 py-2.5 text-sm text-rose-300">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={busy}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-accent-600 to-accent-500 py-2.5 text-sm font-semibold text-white shadow-lg shadow-accent-600/30 transition hover:brightness-110 disabled:opacity-60"
        >
          <SendHorizonal className="size-4" />
          {busy ? 'Submitting…' : 'Submit for review'}
        </button>
      </form>
    </div>
  )
}
