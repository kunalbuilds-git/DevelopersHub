const TYPE_LABELS = {
  HACKATHON: 'Hackathon',
  INTERNSHIP: 'Internship',
  PROGRAM: 'Program',
  OPEN_SOURCE: 'Open Source',
}

export default function TypeBadge({ type }) {
  return <span className={`type-badge type-${String(type).toLowerCase()}`}>{TYPE_LABELS[type] || type}</span>
}
