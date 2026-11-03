export function SkeletonCard() {
  return (
    <div className="glass flex flex-col gap-4 rounded-2xl p-5">
      <div className="flex gap-2">
        <div className="skeleton h-5 w-24 rounded-full" />
        <div className="skeleton ml-auto h-5 w-16 rounded-full" />
      </div>
      <div className="skeleton h-5 w-3/4" />
      <div className="skeleton h-3.5 w-1/3" />
      <div className="flex flex-col gap-2">
        <div className="skeleton h-3 w-full" />
        <div className="skeleton h-3 w-5/6" />
      </div>
      <div className="mt-2 flex gap-2">
        <div className="skeleton h-5 w-28 rounded-full" />
        <div className="skeleton h-5 w-16 rounded-full" />
      </div>
    </div>
  )
}

export function SkeletonGrid({ count = 6 }) {
  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: count }, (_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  )
}
