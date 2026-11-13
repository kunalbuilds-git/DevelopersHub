import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../../components/Navbar.jsx'
import Chip from '../../components/ui/Chip.jsx'
import GlassPanel from '../../components/ui/GlassPanel.jsx'
import { api } from '../../api/client.js'
import { useAuth } from '../../auth/AuthContext.jsx'
import { HAS_DEADLINE_SORT } from './constants'
import { ease, useScrollStory } from './useScrollStory'
import { OPPORTUNITY_TYPES } from '../../components/TypeBadge.jsx'
import './landing.css'

/** §10 hotspots — positions relative to the stage, values link to filtered browse. */
const HOTSPOTS = [
  { left: '12%', top: '58%', label: 'Hackathons', type: 'HACKATHON' },
  { left: '33%', top: '41%', label: 'Internships', type: 'INTERNSHIP' },
  { left: '70%', top: '66%', label: 'Programs', type: 'PROGRAM' },
  { left: '83%', top: '47%', label: 'Open source', type: 'OPEN_SOURCE' },
]

const FEATURES = [
  { n: '01', label: 'Search by title or organization', to: '/browse' },
  { n: '02', label: 'Filter by type, level and tag', to: '/browse' },
  { n: '03', label: 'Bookmark and sort by deadline', to: '/saved' },
  { n: '04', label: 'Deadline urgency at a glance', to: '/browse' },
  { n: '05', label: 'Submit an opportunity for review', to: '/submit' },
]

const SOLUTION_CARDS = [
  {
    label: 'Hackathons',
    title: ['Build something', 'with a deadline'],
    body: 'Find hackathons you can enter as a beginner and see when registration closes.',
    to: '/browse?type=HACKATHON',
    img: '/landing/mast.webp',
    pos: '22% 70%',
    size: '260%',
  },
  {
    label: 'Internships',
    title: ['Your first', 'real experience'],
    body: 'Internships and company programs filtered by level, so first-years see what they can actually apply to.',
    to: '/browse?type=INTERNSHIP',
    img: '/landing/island.webp',
    pos: '50% 50%',
    size: '190%',
  },
  {
    label: 'Open source and programs',
    title: ['Contribute', 'early'],
    body: 'GSoC-style programs and beginner-friendly open source, with timelines you can plan around.',
    to: '/browse?type=OPEN_SOURCE',
    img: '/landing/mast.webp',
    pos: '62% 30%',
    size: '230%',
  },
]

const ACCOUNTING_TEXT =
  'Every listing shows what it is, who it is for and when it closes, so you can apply early and stop scrambling the night before the deadline.'

export default function LandingPage() {
  const { isLoggedIn } = useAuth()
  const [savedList, setSavedList] = useState(null)
  const [weekly, setWeekly] = useState({ buckets: new Array(8).fill(0), thisWeek: 0 })
  const [hotspotOn, setHotspotOn] = useState(null)
  const heroCopyRef = useRef(null)
  const heroLineRef = useRef(null)
  const storyRef = useRef(null)
  const savedCountRef = useRef(0)
  useScrollStory(storyRef, savedCountRef)
  const solStatsRef = useRef(null)
  const statNumRefs = useRef([null, null, null])
  const countedRef = useRef(false)
  const camRef = useRef(null)
  const camFRef = useRef(null)
  const canvasRef = useRef(null)
  const videoRef = useRef(null)
  const fogRef = useRef(null)
  const progRef = useRef(null)

  // The full saved list — the endpoint returns it unpaginated, so the station
  // card's weekly buckets and "+N this week" are computed from real data.
  useEffect(() => {
    const computeWeekly = (list) => {
      const buckets = new Array(8).fill(0)
      for (const b of list) {
        if (!b.deadline) continue
        const week = Math.floor((new Date(b.deadline).getTime() - Date.now()) / 86400000)
        if (week >= 0 && week < 8) buckets[week]++
      }
      return { buckets, thisWeek: buckets[0] }
    }
    const empty = { buckets: new Array(8).fill(0), thisWeek: 0 }
    if (!isLoggedIn) {
      setSavedList([])
      setWeekly(empty)
      return
    }
    api('/bookmarks')
      .then((list) => {
        setSavedList(list)
        setWeekly(computeWeekly(list))
      })
      .catch(() => {
        setSavedList([])
        setWeekly(empty)
      })
  }, [isLoggedIn])

  // §7 layoutHero — the island's fern tips must sit on the lower half of the
  // headline letters at any viewport shape. Runs on load, resize and after fonts.
  useEffect(() => {
    const layout = () => {
      const block = heroCopyRef.current
      const line = heroLineRef.current
      if (!block || !line) return
      const W = window.innerWidth
      const H = window.innerHeight
      if (W <= 820) {
        block.style.top = ''
        line.style.fontSize = ''
        return
      }
      const bw = W * 1.06
      const bh = H * 1.12
      const k = Math.max(bw / 1600, bh / 900)
      const fernTop = -0.09 * H + (bh - 900 * k) / 2 + 245 * k + 0.06 * H
      let fs = Math.min(104, 72 * k)
      line.style.fontSize = fs + 'px'
      const lineWidth = line.getBoundingClientRect().width
      if (lineWidth > W * 0.86) {
        fs = fs * ((W * 0.86) / lineWidth)
        line.style.fontSize = fs + 'px'
      }
      block.style.top = fernTop + 0.5 * fs - block.offsetHeight + 'px'
    }
    layout()
    window.addEventListener('resize', layout)
    if (document.fonts?.ready) {
      document.fonts.ready.then(layout)
    }
    return () => window.removeEventListener('resize', layout)
  }, [])

  // §13 stats — live totals from the API, counted up once on first view
  useEffect(() => {
    const onScroll = () => {
      if (countedRef.current || !solStatsRef.current) return
      if (solStatsRef.current.getBoundingClientRect().top > window.innerHeight * 0.6) return
      countedRef.current = true
      window.removeEventListener('scroll', onScroll)
      Promise.allSettled([
        api('/opportunities?size=1&includeExpired=true'),
        api('/opportunities?size=1'),
      ]).then(([allResult, openResult]) => {
        const targets = [
          allResult.status === 'fulfilled' ? allResult.value.totalElements : null,
          openResult.status === 'fulfilled' ? openResult.value.totalElements : null,
          OPPORTUNITY_TYPES.length,
        ]
        const start = performance.now()
        const tick = (now) => {
          const t = Math.min(1, (now - start) / 1600)
          const e = ease(t)
          targets.forEach((target, i) => {
            const el = statNumRefs.current[i]
            if (!el) return
            el.textContent = target == null ? '—' : Math.round(target * e).toLocaleString('en-IN')
          })
          if (t < 1) requestAnimationFrame(tick)
        }
        requestAnimationFrame(tick)
      })
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const buckets = weekly.buckets
  const thisWeek = weekly.thisWeek
  const savedCount = savedList?.length ?? 0
  useEffect(() => {
    savedCountRef.current = savedCount
  }, [savedCount])
  const hasSaved = savedCount > 0
  const maxBucket = Math.max(1, ...buckets)
  const sparkPoints = buckets
    .map((c, i) => `${Math.round((i * 214) / 7)},${Math.round(60 - (c / maxBucket) * 50)}`)
    .join(' ')

  return (
    <>
      <Navbar />
      <main>
        {/* Pinned scroll story (§4) — the controller lands in steps 4-5 */}
        <div className="landing-story" id="story" ref={storyRef}>
          <div className="landing-stage">
            {/* Camera box: canvas + high-res plate + mist sheen */}
            <div className="landing-cam" ref={camRef}>
              <canvas id="cv" ref={canvasRef} />
              <img className="plate" src="/landing/hero.webp" alt="" />
              <div className="landing-mist" aria-hidden="true" />
            </div>

            {/* Hidden scrub video (§5) */}
            <video
              className="landing-video"
              ref={videoRef}
              src="/landing/video.mp4"
              muted
              playsInline
              preload="auto"
              crossOrigin="anonymous"
            />

            {/* Fog bank (§8) */}
            <div className="landing-fog" ref={fogRef} aria-hidden="true">
              <div className="f0" />
              <div className="f1" />
              <div className="f2" />
            </div>

            {/* L0 — hero headline */}
            <div className="landing-layer landing-layer--l0">
              <div className="landing-hero-copy" ref={heroCopyRef}>
                <Chip>Student opportunities</Chip>
                <h1>
                  <span className="hero-line">
                    <span ref={heroLineRef}>Never miss a deadline</span>
                  </span>
                </h1>
              </div>
            </div>

            {/* Front cut-out — the big island in front of the headline (§4) */}
            <div className="landing-cam landing-camF" ref={camFRef} aria-hidden="true">
              <img src="/landing/cutout.webp" alt="" />
            </div>

            {/* Floating rock sprites (§6) */}
            <div className="landing-rock rk1" data-x="-0.3" data-y="-0.6" data-depth="1.3" aria-hidden="true">
              <div className="bob">
                <img
                  src="/landing/rocks.webp"
                  alt=""
                  style={{ width: (1200 / 260) * 100 + '%', left: (-30 / 260) * 100 + '%', top: (-200 / 295) * 100 + '%' }}
                />
              </div>
            </div>
            <div className="landing-rock rk3" data-x="1" data-y="0.55" data-depth="1.8" aria-hidden="true">
              <div className="bob">
                <img
                  src="/landing/rocks.webp"
                  alt=""
                  style={{ width: (1200 / 265) * 100 + '%', left: (-895 / 265) * 100 + '%', top: (-240 / 270) * 100 + '%' }}
                />
              </div>
            </div>

            {/* L1 — hero HUD, caption, scroll hint */}
            <div className="landing-layer landing-layer--l1">
              <div className="landing-hud">
                <span className="hud-crosshair" aria-hidden="true" />
                <div className="hud-box">
                  <span className="hud-corner tl" aria-hidden="true" />
                  <span className="hud-corner tr" aria-hidden="true" />
                  <span className="hud-corner bl" aria-hidden="true" />
                  <span className="hud-corner br" aria-hidden="true" />
                  <Chip>Closing soon</Chip>
                  {HAS_DEADLINE_SORT && (
                    <pre className="hud-pre">{`NEXT  —\nCLOSES  —\nIN  — DAYS`}</pre>
                  )}
                </div>
              </div>
              <div className="landing-caption">
                Hackathons, internships and open source.
                <br />
                Built for first- and second-year
                <br />
                CS students in India.
              </div>
              <div className="landing-hint">
                Scroll to explore
                <span className="hint-circle" aria-hidden="true">
                  <span className="hint-chevron" />
                </span>
              </div>
            </div>

            {/* L2 — vision (§9) */}
            <div className="landing-layer landing-layer--l2" id="vision">
              <div className="landing-vision">
                <Chip>Why it exists</Chip>
                <h2>
                  <span>From</span> <span>Scattered</span> <span>Links</span> <span>to</span>
                  <br />
                  <span>Deadline-Sorted,</span>
                  <br />
                  <span>Saved</span> <span>Opportunities</span>
                </h2>
                <p>
                  Hackathons, internships and open source programs
                  <br />
                  in one searchable place, built for students.
                </p>
              </div>
            </div>

            {/* L3 — accounting + hotspots (§10) */}
            <div className="landing-layer landing-layer--l3">
              <p className="landing-acct">
                {ACCOUNTING_TEXT.split(' ').map((word, i) => (
                  <span key={i}>{word} </span>
                ))}
              </p>
              {HOTSPOTS.map((hotspot, i) => (
                <div
                  key={hotspot.label}
                  className={`landing-hotspot${hotspotOn === i ? ' on' : ''}`}
                  style={{ left: hotspot.left, top: hotspot.top }}
                >
                  <div className="hotspot-row">
                    <button
                      type="button"
                      className="hotspot-dot"
                      onClick={() => setHotspotOn(i === hotspotOn ? null : i)}
                      aria-label={hotspot.label}
                    />
                    <span className="hotspot-label">{hotspot.label}</span>
                  </div>
                  <em className="hotspot-value">
                    <Link to={`/browse?type=${hotspot.type}`}>Browse →</Link>
                  </em>
                </div>
              ))}
            </div>

            {/* L4 — station (§11) */}
            <div className="landing-layer landing-layer--l4">
              <div className="landing-cred">
                <span className="station-label">✕ Your saved list</span>
                <h2>A deadline tracker built for how students actually apply</h2>
              </div>

              <GlassPanel variant="scene" className="landing-card">
                <h4>Saved opportunities</h4>
                <p className="card-note">Next 8 weeks</p>
                <div className="card-number">
                  <span className="num">{savedCount}</span> <span className="card-unit">saved</span>
                </div>
                {thisWeek > 0 && <span className="chip lime-tag">+{thisWeek} this week</span>}
                {hasSaved ? (
                  <svg width="220" height="70" viewBox="0 0 220 70" aria-hidden="true">
                    <polyline
                      points={sparkPoints}
                      fill="none"
                      stroke="var(--lime)"
                      strokeWidth="1.6"
                    />
                    <circle cx={Math.round((3 * 214) / 7)} cy={Math.round(60 - (buckets[3] / maxBucket) * 50)} r="2.4" fill="#ffffff" />
                    <circle cx={Math.round((5 * 214) / 7)} cy={Math.round(60 - (buckets[5] / maxBucket) * 50)} r="2.4" fill="#ffffff" />
                    <circle cx={214} cy={Math.round(60 - (buckets[7] / maxBucket) * 50)} r="3.4" fill="var(--lime)" />
                  </svg>
                ) : (
                  <div className="spark-empty" aria-hidden="true" />
                )}
                <p className="card-footer">W1 · W2 · W4 · W8</p>
              </GlassPanel>

              <GlassPanel variant="paper" className="landing-feats">
                {FEATURES.map((feature) => (
                  <Link key={feature.n} to={feature.to} className="feats-row">
                    <span className="idx">{feature.n}</span>
                    <span>{feature.label}</span>
                    <span className="arrow" aria-hidden="true">
                      →
                    </span>
                  </Link>
                ))}
              </GlassPanel>
            </div>

            {/* Progress bar */}
            <div className="landing-prog" ref={progRef} aria-hidden="true">
              <i />
            </div>
          </div>
        </div>

        {/* Solutions panel (§13) */}
        <section className="landing-sol">
          <div className="sol-head">
            <div>
              <Chip>What you can do</Chip>
              <h2>Find the right thing, then apply on time</h2>
            </div>
            <p>
              Search by type, level and tag, save what you care about, and see exactly how long you
              have left. Spotted something we are missing? Submit it and we will review it.
            </p>
          </div>

          <div className="sol-cards">
            {SOLUTION_CARDS.map((card) => (
              <Link
                key={card.label}
                to={card.to}
                className="sol-card"
                onPointerMove={(event) => {
                  const rect = event.currentTarget.getBoundingClientRect()
                  const x = (event.clientX - rect.left) / rect.width - 0.5
                  const y = (event.clientY - rect.top) / rect.height - 0.5
                  event.currentTarget.style.transform = `perspective(1000px) rotateY(${(x * 6).toFixed(2)}deg) rotateX(${(-y * 6).toFixed(2)}deg)`
                }}
                onPointerLeave={(event) => {
                  event.currentTarget.style.transform = ''
                }}
              >
                <div
                  className="sol-card-bg"
                  style={{ backgroundImage: `url(${card.img})`, backgroundPosition: card.pos, backgroundSize: card.size }}
                />
                <div className="sol-card-overlay" aria-hidden="true" />
                <div className="sol-card-top">
                  <span className="mono">[{card.label}]</span>
                  <span className="sol-card-arrow" aria-hidden="true">→</span>
                </div>
                <div className="sol-card-bottom">
                  <h3>
                    {card.title[0]}
                    <br />
                    {card.title[1]}
                  </h3>
                  <p>{card.body}</p>
                </div>
              </Link>
            ))}
          </div>

          <div className="sol-stats" ref={solStatsRef}>
            <div className="sol-stat">
              <span className="num" ref={(el) => (statNumRefs.current[0] = el)}>—</span>
              <span className="label">Opportunities listed</span>
            </div>
            <div className="sol-stat">
              <span className="num" ref={(el) => (statNumRefs.current[1] = el)}>—</span>
              <span className="label">Open right now</span>
            </div>
            <div className="sol-stat">
              <span className="num" ref={(el) => (statNumRefs.current[2] = el)}>—</span>
              <span className="label">Opportunity types</span>
            </div>
          </div>

          <div className="sol-cta">
            <Link to="/browse" className="sol-cta-btn">
              Browse all opportunities
              <span aria-hidden="true">›</span>
            </Link>
          </div>
        </section>
      </main>
    </>
  )
}
