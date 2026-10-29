export default function Pagination({ page, totalPages, totalElements, onPage }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements != null ? (
      <p className="pagination-info">
        {totalElements} {totalElements === 1 ? 'opportunity' : 'opportunities'}
      </p>
    ) : null
  }

  return (
    <div className="pagination">
      <button className="btn btn-ghost" disabled={page === 0} onClick={() => onPage(page - 1)}>
        ← Previous
      </button>
      <span className="pagination-info">
        Page {page + 1} of {totalPages} · {totalElements} opportunities
      </span>
      <button className="btn btn-ghost" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>
        Next →
      </button>
    </div>
  )
}
