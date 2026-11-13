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
      <main className="mx-auto w-full max-w-6xl flex-1 px-5 pb-20">
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
      <footer className="border-t border-white/10 bg-ink-900/50">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 px-5 py-6 text-sm text-slate-500 sm:flex-row">
          <p>
            <span className="font-mono font-semibold text-slate-400">DevelopersHub</span> — one place for
            hackathons, internships, and company events.
          </p>
          <p className="font-mono text-xs">built by students, for students</p>
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
