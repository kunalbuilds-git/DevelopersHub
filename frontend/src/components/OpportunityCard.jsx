import DeadlineChip from './DeadlineChip.jsx'
import TypeBadge from './TypeBadge.jsx'
import BookmarkButton from './BookmarkButton.jsx'

export default function OpportunityCard({ opportunity }) {
  return (
    <article className="card opportunity-card">
      <div className="card-head">
        <TypeBadge type={opportunity.type} />
        <BookmarkButton opportunityId={opportunity.id} />
      </div>
      <h3 className="card-title">
        {opportunity.url ? (
          <a href={opportunity.url} target="_blank" rel="noreferrer">
            {opportunity.title}
          </a>
        ) : (
          opportunity.title
        )}
      </h3>
      <p className="card-org">{opportunity.organization}</p>
      {opportunity.description && <p className="card-description">{opportunity.description}</p>}
      <div className="card-meta">
        <DeadlineChip deadline={opportunity.deadline} daysUntilDeadline={opportunity.daysUntilDeadline} />
        {(opportunity.tags || []).map((tag) => (
          <span key={tag.id ?? tag.name} className="chip chip-tag">
            #{tag.name}
          </span>
        ))}
      </div>
    </article>
  )
}
