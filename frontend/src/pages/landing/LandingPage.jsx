import { Link } from 'react-router-dom'
import Navbar from '../../components/Navbar.jsx'

/**
 * Placeholder shell for step 1 — the pinned Mossline story lands here in
 * steps 3-6 (hero, vision, accounting, station, solutions).
 */
export default function LandingPage() {
  return (
    <>
      <Navbar />
      <main
        style={{
          minHeight: '100vh',
          background: 'var(--mist)',
          display: 'grid',
          placeItems: 'center',
          padding: '24px',
        }}
      >
        <div style={{ textAlign: 'center' }}>
          <span className="chip">Student opportunities</span>
          <h1
            style={{
              margin: '18px 0 10px',
              fontSize: 'clamp(44px, 6vw, 96px)',
              lineHeight: 0.95,
              letterSpacing: '-0.04em',
              fontWeight: 500,
            }}
          >
            Never miss a deadline
          </h1>
          <p style={{ margin: '0 0 28px', color: 'var(--ink2)', fontSize: '16px' }}>
            Hackathons, internships and open source — one searchable place.
          </p>
          <Link
            to="/browse"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '10px',
              background: 'var(--lime)',
              color: 'var(--ink)',
              padding: '12px 18px',
              borderRadius: '6px',
              fontWeight: 600,
            }}
          >
            Browse opportunities
            <span aria-hidden="true">›</span>
          </Link>
        </div>
      </main>
    </>
  )
}
