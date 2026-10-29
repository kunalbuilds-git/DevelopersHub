import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { useSaved } from '../saved/SavedContext.jsx'

export default function Navbar() {
  const { user, isLoggedIn, logout } = useAuth()
  const { setSavedIds } = useSaved()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    setSavedIds(new Set())
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand">
          <span className="brand-mark">{'</>'}</span> DevelopersHub
        </Link>
        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            Browse
          </NavLink>
          {isLoggedIn && (
            <NavLink to="/saved" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              Saved
            </NavLink>
          )}
        </nav>
        <div className="nav-auth">
          {isLoggedIn ? (
            <>
              <span className="nav-user" title={user.email}>
                {user.email}
              </span>
              <button className="btn btn-ghost" onClick={handleLogout}>
                Log out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost">
                Log in
              </Link>
              <Link to="/register" className="btn btn-primary">
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
