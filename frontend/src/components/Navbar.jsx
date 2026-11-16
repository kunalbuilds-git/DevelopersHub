import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { useSaved } from '../saved/SavedContext.jsx'

/**
 * Spec §3 nav — fixed, three-column grid, mono links, lime "GET STARTED"
 * square. Scene variant (transparent) on the landing; solid variant on the
 * paper background for the app pages.
 */
export default function Navbar() {
  const { isLoggedIn, isAdmin, logout } = useAuth()
  const { savedIds, setSavedIds } = useSaved()
  const location = useLocation()
  const navigate = useNavigate()
  const onLanding = location.pathname === '/'

  function handleLogout() {
    logout()
    setSavedIds(new Set())
    navigate('/browse')
  }

  const menuLinks = [{ to: '/browse', label: 'Browse' }]
  if (isLoggedIn) {
    menuLinks.push({ to: '/saved', label: 'Saved' })
    menuLinks.push({ to: '/submit', label: 'Submit' })
  } else {
    menuLinks.push({ to: '/#vision', label: 'How it works' })
  }
  if (isAdmin) {
    menuLinks.push({ to: '/review', label: 'Review' })
  }

  const linkClass =
    'font-mono text-[11.5px] uppercase tracking-[0.14em] text-ink transition-colors duration-150 hover:text-lime2'

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 ${
        onLanding ? 'bg-transparent' : 'border-b border-[color:var(--line-ink)] bg-paper'
      }`}
    >
      <div className="grid grid-cols-[1fr_auto_1fr] items-center px-[34px] py-[22px] max-[820px]:grid-cols-[auto_auto] max-[820px]:gap-x-4 max-[820px]:px-[18px] max-[820px]:py-4">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-[9px] text-[21px] font-semibold tracking-[-0.02em]">
          <svg width="26" height="26" viewBox="0 0 32 32" aria-hidden="true">
            <path d="M16 2l12 7v14l-12 7-12-7V9z" fill="currentColor" />
            <path d="M16 8c-4 4-5 9-1 16 1-5 4-9 8-11-3 0-5 1-7 3 0-3 0-5 0-8z" fill="#c6f35c" />
          </svg>
          DevelopersHub
        </Link>

        {/* Centre menu (desktop) */}
        <nav className="flex items-center gap-[30px] max-[820px]:hidden" aria-label="Main">
          {menuLinks.map((link) => (
            <Link key={link.to} to={link.to} className={linkClass}>
              {link.label}
            </Link>
          ))}
        </nav>

        {/* Right */}
        <div className="flex items-center justify-end gap-4">
          {isLoggedIn ? (
            <>
              <Link
                to="/saved"
                className="flex items-center gap-[10px] font-mono text-[11.5px] uppercase tracking-[0.16em] transition-colors duration-150 hover:text-lime2"
                aria-label={`Saved list, ${savedIds.size} items`}
              >
                Saved
                <span
                  className="grid size-5 place-items-center rounded-[3px] bg-lime font-bold text-ink"
                  aria-hidden="true"
                >
                  {savedIds.size}
                </span>
              </Link>
              <button
                onClick={handleLogout}
                className="font-mono text-[11.5px] uppercase tracking-[0.16em] transition-colors duration-150 hover:text-lime2"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="font-mono text-[11.5px] uppercase tracking-[0.16em] transition-colors duration-150 hover:text-lime2"
              >
                Log in
              </Link>
              <Link
                to="/register"
                className="group flex items-center gap-3 font-mono text-[11.5px] uppercase tracking-[0.16em]"
              >
                Get started
                <span
                  aria-hidden="true"
                  className="grid size-5 place-items-center rounded-[3px] bg-lime text-ink transition-transform duration-300 group-hover:translate-x-[3px]"
                >
                  ›
                </span>
              </Link>
            </>
          )}
        </div>
      </div>

      {/* Mobile menu row (app pages only — the landing hides its menu ≤820px per §14) */}
      {!onLanding && (
        <nav
          className="flex items-center gap-6 overflow-x-auto px-[18px] pb-3 min-[821px]:hidden"
          aria-label="Main mobile"
        >
          {menuLinks.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              className={`shrink-0 font-mono text-[11px] uppercase tracking-[0.14em] ${
                location.pathname === link.to ? 'text-lime2' : 'text-ink2'
              }`}
            >
              {link.label}
            </Link>
          ))}
        </nav>
      )}
    </header>
  )
}
