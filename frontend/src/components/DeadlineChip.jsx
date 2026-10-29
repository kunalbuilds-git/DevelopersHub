function formatDateTime(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function DeadlineChip({ deadline, daysUntilDeadline }) {
  if (!deadline) return <span className="chip chip-muted">No deadline</span>

  const label = `Due ${formatDateTime(deadline)}`
  if (daysUntilDeadline == null) return <span className="chip chip-muted">{label}</span>
  if (daysUntilDeadline < 0) return <span className="chip chip-over">{label} · passed</span>
  if (daysUntilDeadline <= 7) return <span className="chip chip-soon">{label} · in {daysUntilDeadline}d</span>
  if (daysUntilDeadline <= 30) return <span className="chip chip-near">{label} · in {daysUntilDeadline}d</span>
  return <span className="chip chip-muted">{label}</span>
}
