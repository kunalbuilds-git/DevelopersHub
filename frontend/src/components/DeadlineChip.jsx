import { CalendarClock } from 'lucide-react'

function formatDateTime(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function DeadlineChip({ deadline, daysUntilDeadline }) {
  if (!deadline) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-2.5 py-0.5 text-[0.72rem] font-medium text-slate-400">
        <CalendarClock className="size-3" /> Rolling
      </span>
    )
  }

  const label = formatDateTime(deadline)

  if (daysUntilDeadline == null) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-2.5 py-0.5 text-[0.72rem] font-medium text-slate-400">
        <CalendarClock className="size-3" /> {label}
      </span>
    )
  }
  if (daysUntilDeadline < 0) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-2.5 py-0.5 text-[0.72rem] font-medium text-slate-500 line-through">
        <CalendarClock className="size-3" /> {label}
      </span>
    )
  }
  if (daysUntilDeadline <= 7) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-rose-400/30 bg-rose-500/15 px-2.5 py-0.5 text-[0.72rem] font-semibold text-rose-300">
        <CalendarClock className="size-3" /> {daysUntilDeadline === 0 ? 'Closes today' : `${daysUntilDeadline}d left`}
      </span>
    )
  }
  if (daysUntilDeadline <= 30) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-amber-400/25 bg-amber-500/10 px-2.5 py-0.5 text-[0.72rem] font-semibold text-amber-300">
        <CalendarClock className="size-3" /> {daysUntilDeadline}d left
      </span>
    )
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-2.5 py-0.5 text-[0.72rem] font-medium text-slate-400">
      <CalendarClock className="size-3" /> {label}
    </span>
  )
}
