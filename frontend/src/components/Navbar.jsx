import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Bookmark, Code2, FileCheck2, LogOut, ShieldCheck, Sparkles } from 'lucide-react'
import { useAuth } from '../auth/AuthContext.jsx'
import { useSaved } from '../saved/SavedContext.jsx'

export default function Navbar() {
  const { user, isLoggedIn, isAdmin, logout } = useAuth()
  const { savedIds, setSavedIds } = useSaved()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    setSavedIds(new Set())
    navigate('/')
  }

  const linkClass = ({ isActive }) =>
    `rounded-lg px-3 py-1.5 text-sm font-medium transition ${
      isActive ? 'bg-white/10 text-white' : 'text-slate-400 hover:bg-white/5 hover:text-slate-200'
    }`

  return (
    <header className="sticky top-0 z-40 border-b border-white/10 bg-ink-950/70 backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-5">
        <Link to="/" className="group flex items-center gap-2 font-bold tracking-tight text-white">
          <span className="grid size-8 place-items-center rounded-lg bg-gradient-to-br from-accent-500 to-fuchsia-glow text-white shadow-lg shadow-accent-500/30">
            <Code2 className="size-4.5" strokeWidth={2.5} />
          </span>
          <span className="text-[1.05rem]">
            Developers<span className="text-gradient">Hub</span>
          </span>
        </Link>

        <nav className="ml-4 hidden items-center gap-1 sm:flex">
          <NavLink to="/" end className={linkClass}>
            Browse
          </NavLink>
          {isLoggedIn && (
            <NavLink to="/saved" className={linkClass}>
              <span className="inline-flex items-center gap-1.5">
                <Bookmark className="size-3.5" />
                Saved
                {savedIds.size > 0 && (
                  <span className="rounded-full bg-accent-500/20 px-1.5 text-xs font-semibold text-accent-300">
                    {savedIds.size}
                  </span>
                )}
              </span>
            </NavLink>
          )}
          {isLoggedIn && (
            <NavLink to="/my-submissions" className={linkClass}>
              <span className="inline-flex items-center gap-1.5">
                <FileCheck2 className="size-3.5" />
                My submissions
              </span>
            </NavLink>
          )}
          {isLoggedIn && (
            <NavLink to="/submit" className={linkClass}>
              Submit
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/review" className={linkClass}>
              <span className="inline-flex items-center gap-1.5">
                <ShieldCheck className="size-3.5" />
                Review
              </span>
            </NavLink>
          )}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          {isLoggedIn ? (
            <>
              <span
                className="hidden max-w-44 items-center gap-1.5 truncate text-sm text-slate-400 md:inline-flex"
                title={user.email}
              >
                <Sparkles className="size-3.5 text-accent-300" />
                {user.email}
              </span>
              <button
                onClick={handleLogout}
                className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-1.5 text-sm font-medium text-slate-300 transition hover:border-white/25 hover:text-white"
              >
                <LogOut className="size-3.5" />
                Log out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-300 transition hover:text-white"
              >
                Log in
              </Link>
              <Link
                to="/register"
                className="rounded-lg bg-gradient-to-r from-accent-600 to-accent-500 px-3.5 py-1.5 text-sm font-semibold text-white shadow-lg shadow-accent-600/30 transition hover:brightness-110"
              >
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
