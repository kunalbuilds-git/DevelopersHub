const LEVEL_STYLES = {
  BEGINNER: 'bg-emerald-500/10 text-emerald-300',
  INTERMEDIATE: 'bg-sky-500/10 text-sky-300',
  ADVANCED: 'bg-fuchsia-500/10 text-fuchsia-300',
}

const LEVEL_LABELS = {
  BEGINNER: 'Beginner',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
}

export const OPPORTUNITY_LEVELS = Object.keys(LEVEL_LABELS)

export default function LevelBadge({ level }) {
  if (!level || level === 'ANY') return null
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-[0.7rem] font-semibold ${LEVEL_STYLES[level] || 'bg-white/10 text-slate-300'}`}>
      {LEVEL_LABELS[level] || level}
    </span>
  )
}
