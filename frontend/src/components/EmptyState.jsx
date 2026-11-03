import { SearchX } from 'lucide-react'

export default function EmptyState({ title, children }) {
  return (
    <div className="glass grid-bg mx-auto flex max-w-xl flex-col items-center rounded-3xl px-8 py-14 text-center">
      <div className="grid size-14 place-items-center rounded-2xl border border-white/10 bg-white/5">
        <SearchX className="size-6 text-slate-400" />
      </div>
      <p className="mt-4 text-lg font-semibold text-white">{title}</p>
      <div className="mt-1 text-sm text-slate-400">{children}</div>
    </div>
  )
}
