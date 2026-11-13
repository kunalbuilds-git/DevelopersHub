import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import BrowsePage from './pages/BrowsePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import SavedPage from './pages/SavedPage.jsx'
import MySubmissionsPage from './pages/MySubmissionsPage.jsx'
import SubmitPage from './pages/SubmitPage.jsx'
import ReviewQueuePage from './pages/ReviewQueuePage.jsx'
import LandingPage from './pages/landing/LandingPage.jsx'
import { useAuth } from './auth/AuthContext.jsx'

function RequireAuth({ children, admin = false }) {
  const { isLoggedIn, isAdmin } = useAuth()
  const location = useLocation()
  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  if (admin && !isAdmin) {
    return <Navigate to="/browse" replace />
  }
  return children
}

/** Shared shell for the app pages — the landing route sits outside it. */
function AppShell() {
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <main className="mx-auto w-full max-w-6xl flex-1 px-5 pb-20 pt-[90px] max-[820px]:pt-[110px]">
        <Routes>
          <Route path="/browse" element={<BrowsePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/saved"
            element={
              <RequireAuth>
                <SavedPage />
              </RequireAuth>
            }
          />
          <Route
            path="/my-submissions"
            element={
              <RequireAuth>
                <MySubmissionsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/submit"
            element={
              <RequireAuth>
                <SubmitPage />
              </RequireAuth>
            }
          />
          <Route
            path="/review"
            element={
              <RequireAuth admin>
                <ReviewQueuePage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/browse" replace />} />
        </Routes>
      </main>
      <footer className="border-t border-[color:var(--line-ink)] bg-paper">
        <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-2 px-5 py-6 font-mono text-[11px] uppercase tracking-[0.14em] text-ink2 sm:flex-row sm:items-center">
          <p>
            DevelopersHub — <span className="normal-case tracking-normal">never miss a deadline</span>
          </p>
          <p>
            <a
              href="https://github.com/kunalbuilds-git/DevelopersHub"
              target="_blank"
              rel="noopener noreferrer"
              className="transition-colors duration-150 hover:text-lime2"
            >
              GitHub
            </a>
            <span aria-hidden="true" className="mx-2">
              ·
            </span>
            Built by Kunal
          </p>
        </div>
      </footer>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      {/* Landing: full-viewport pinned story, outside the app shell */}
      <Route path="/" element={<LandingPage />} />
      <Route path="*" element={<AppShell />} />
    </Routes>
  )
}
