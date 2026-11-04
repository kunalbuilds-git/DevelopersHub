import { Link } from 'react-router-dom'
import { Code2 } from 'lucide-react'

/** Shared split layout for the login and register pages. */
export default function AuthLayout({ title, subtitle, children, switchText, switchTo, switchLabel }) {
  return (
    <div className="mx-auto mt-10 grid max-w-4xl overflow-hidden rounded-3xl border border-white/10 md:grid-cols-2">
      {/* Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-accent-600/30 via-violet-glow/20 to-fuchsia-glow/10 p-10 md:flex">
        <div className="grid-bg pointer-events-none absolute inset-0" />
        <div className="pointer-events-none absolute -bottom-16 -left-16 size-56 rounded-full bg-accent-500/30 blur-3xl" />
        <Link to="/" className="relative flex items-center gap-2 font-bold text-white">
          <span className="grid size-8 place-items-center rounded-lg bg-white/15">
            <Code2 className="size-4.5" strokeWidth={2.5} />
          </span>
          DevelopersHub
        </Link>
        <div className="relative">
          <p className="text-2xl font-bold leading-tight text-white">
            One account.
            <br />
            Every <span className="text-gradient">opportunity</span>.
          </p>
          <ul className="mt-6 space-y-2.5 text-sm text-slate-300">
            <li className="flex items-center gap-2">
              <span className="size-1.5 rounded-full bg-accent-300" /> Save hackathons, internships, and events
            </li>
            <li className="flex items-center gap-2">
              <span className="size-1.5 rounded-full bg-accent-300" /> Deadlines sorted so nothing sneaks up on you
            </li>
            <li className="flex items-center gap-2">
              <span className="size-1.5 rounded-full bg-accent-300" /> Filter by your experience level
            </li>
          </ul>
        </div>
        <p className="relative font-mono text-xs text-slate-400">free · student-built · no spam</p>
      </div>

      {/* Form panel */}
      <div className="glass border-0 p-8 sm:p-10">
        <h1 className="text-2xl font-bold text-white">{title}</h1>
        <p className="mt-1.5 text-sm text-slate-400">{subtitle}</p>
        {children}
        <p className="mt-6 text-sm text-slate-400">
          {switchText}{' '}
          <Link to={switchTo} className="font-semibold text-accent-300 hover:text-accent-400">
            {switchLabel}
          </Link>
        </p>
      </div>
    </div>
  )
}
