const TYPE_STYLES = {
  HACKATHON: 'bg-indigo-500/15 text-indigo-300 border-indigo-400/30',
  INTERNSHIP: 'bg-emerald-500/15 text-emerald-300 border-emerald-400/30',
  PROGRAM: 'bg-amber-500/15 text-amber-300 border-amber-400/30',
  OPEN_SOURCE: 'bg-pink-500/15 text-pink-300 border-pink-400/30',
  EVENT: 'bg-sky-500/15 text-sky-300 border-sky-400/30',
}

const TYPE_LABELS = {
  HACKATHON: 'Hackathon',
  INTERNSHIP: 'Internship',
  PROGRAM: 'Program',
  OPEN_SOURCE: 'Open Source',
  EVENT: 'Company Event',
}

export const OPPORTUNITY_TYPES = Object.keys(TYPE_LABELS)

export default function TypeBadge({ type }) {
  const style = TYPE_STYLES[type] || 'bg-white/10 text-slate-300 border-white/20'
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-[0.7rem] font-semibold tracking-wide ${style}`}>
      {TYPE_LABELS[type] || type}
    </span>
  )
}
