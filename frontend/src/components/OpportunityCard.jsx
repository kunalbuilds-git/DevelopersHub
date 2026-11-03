import { motion } from 'framer-motion'
import { ExternalLink } from 'lucide-react'
import BookmarkButton from './BookmarkButton.jsx'
import DeadlineChip from './DeadlineChip.jsx'
import LevelBadge from './LevelBadge.jsx'
import TypeBadge from './TypeBadge.jsx'

export default function OpportunityCard({ opportunity, index = 0 }) {
  return (
    <motion.article
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay: Math.min(index * 0.05, 0.4), ease: 'easeOut' }}
      className="glass glass-hover group relative flex flex-col gap-3 overflow-hidden rounded-2xl p-5"
    >
      <div className="pointer-events-none absolute -right-12 -top-12 size-32 rounded-full bg-accent-500/10 blur-2xl transition group-hover:bg-accent-500/20" />
      <div className="flex flex-wrap items-center gap-2">
        <TypeBadge type={opportunity.type} />
        <LevelBadge level={opportunity.level} />
        <span className="ml-auto">
          <BookmarkButton opportunityId={opportunity.id} />
        </span>
      </div>

      <h3 className="text-[1.05rem] font-semibold leading-snug text-white">
        {opportunity.url ? (
          <a href={opportunity.url} target="_blank" rel="noreferrer" className="transition hover:text-accent-300">
            {opportunity.title}
            <ExternalLink className="mb-0.5 ml-1 inline size-3.5 text-slate-500" />
          </a>
        ) : (
          opportunity.title
        )}
      </h3>

      <p className="font-mono text-[0.78rem] font-medium text-accent-300/80">{opportunity.organization}</p>

      {opportunity.description && (
        <p className="line-clamp-3 text-sm leading-relaxed text-slate-400">{opportunity.description}</p>
      )}

      <div className="mt-auto flex flex-wrap items-center gap-1.5 pt-2">
        <DeadlineChip deadline={opportunity.deadline} daysUntilDeadline={opportunity.daysUntilDeadline} />
        {(opportunity.tags || []).map((tag) => (
          <span
            key={tag.id ?? tag.name}
            className="rounded-full border border-white/10 bg-white/5 px-2.5 py-0.5 font-mono text-[0.72rem] text-slate-400"
          >
            #{tag.name}
          </span>
        ))}
      </div>
    </motion.article>
  )
}
