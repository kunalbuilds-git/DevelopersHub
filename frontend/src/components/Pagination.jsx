import { ChevronLeft, ChevronRight } from 'lucide-react'

export default function Pagination({ page, totalPages, totalElements, onPage }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements != null ? (
      <p className="text-center text-sm text-slate-500">
        {totalElements} {totalElements === 1 ? 'opportunity' : 'opportunities'}
      </p>
    ) : null
  }

  return (
    <div className="mt-10 flex items-center justify-center gap-4">
      <button
        disabled={page === 0}
        onClick={() => onPage(page - 1)}
        className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-300 transition enabled:hover:border-accent-400/40 enabled:hover:text-white disabled:opacity-40"
      >
        <ChevronLeft className="size-4" /> Prev
      </button>
      <span className="font-mono text-sm text-slate-400">
        {page + 1} / {totalPages} · {totalElements} total
      </span>
      <button
        disabled={page >= totalPages - 1}
        onClick={() => onPage(page + 1)}
        className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-300 transition enabled:hover:border-accent-400/40 enabled:hover:text-white disabled:opacity-40"
      >
        Next <ChevronRight className="size-4" />
      </button>
    </div>
  )
}
