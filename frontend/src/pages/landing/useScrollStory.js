import { useEffect } from 'react'

const FPS = 24

/** §12 helpers */
function seg(p, a, b) {
  return Math.min(1, Math.max(0, (p - a) / (b - a)))
}
export function ease(t) {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
}

/**
 * §5 video frame cache + §12 per-frame scroll controller.
 * No React state — everything is written straight to DOM nodes inside one
 * requestAnimationFrame loop. Cleans up the loop, listeners, the video and
 * all cached bitmaps on unmount.
 */
export function useScrollStory(rootRef, savedCountRef) {
  useEffect(() => {
    const root = rootRef.current
    if (!root) return

    const q = (sel) => root.querySelector(sel)
    const qa = (sel) => Array.from(root.querySelectorAll(sel))

    const story = q('.landing-story')
    const cam = q('.landing-cam')
    const camF = q('.landing-camF')
    const plate = q('.landing-cam .plate')
    const canvas = q('#cv')
    const ctx = canvas.getContext('2d')
    const video = q('.landing-video')
    const prog = q('.landing-prog i')
    const l0 = q('.landing-layer--l0')
    const l1 = q('.landing-layer--l1')
    const heroCopy = q('.landing-hero-copy')
    const hud = q('.landing-hud')
    const caption = q('.landing-caption')
    const hint = q('.landing-hint')
    const fog = q('.landing-fog')
    const l2 = q('.landing-layer--l2')
    const l3 = q('.landing-layer--l3')
    const l4 = q('.landing-layer--l4')
    const visionChip = q('.landing-vision .chip')
    const visionWords = qa('.landing-vision h2 span')
    const visionP = q('.landing-vision p')
    const acctWords = qa('.landing-acct span')
    const hotspots = qa('.landing-hotspot')
    const cred = q('.landing-cred')
    const card = q('.landing-card')
    const numberEl = q('.card-number .num')
    const sparkPolyline = q('.landing-card polyline')
    const featsRows = qa('.feats-row')
    const rocks = qa('.landing-rock')

    // Sparkline draw-on length (§11)
    let sparkLen = 0
    if (sparkPolyline) {
      try {
        sparkLen = sparkPolyline.getTotalLength()
        sparkPolyline.style.strokeDasharray = String(sparkLen)
        sparkPolyline.style.strokeDashoffset = String(sparkLen)
      } catch {
        sparkLen = 0
      }
    }

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    // -- §5 video frame cache -------------------------------------------------
    let NF = 2
    const frames = []
    const liveBitmaps = new Set()
    let shown = -1
    let want = 0
    let busy = false
    let stopped = false
    let seekHandler = null

    const grab = document.createElement('canvas')
    grab.width = 1600
    grab.height = 900
    const grabCtx = grab.getContext('2d')

    const sizeCanvas = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 1.5)
      canvas.width = canvas.clientWidth * dpr
      canvas.height = canvas.clientHeight * dpr
    }
    sizeCanvas()
    window.addEventListener('resize', sizeCanvas)

    const drawWant = () => {
      if (busy || reduced) return
      let k = 0
      let idx = -1
      for (; want - k >= 0 || want + k < NF; k++) {
        if (want - k >= 0 && frames[want - k]) {
          idx = want - k
          break
        }
        if (want + k < NF && frames[want + k]) {
          idx = want + k
          break
        }
      }
      if (idx < 0 || idx === shown) return
      busy = true
      createImageBitmap(frames[idx]).then((bm) => {
        if (stopped) {
          bm.close()
          return
        }
        const s = Math.max(canvas.width / bm.width, canvas.height / bm.height)
        const w = bm.width * s
        const h = bm.height * s
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        ctx.drawImage(bm, (canvas.width - w) / 2, (canvas.height - h) / 2, w, h)
        shown = idx
        busy = false
        liveBitmaps.delete(bm)
        bm.close()
        if (idx !== want) drawWant()
      })
    }

    const setupVideo = () => {
      if (stopped || reduced) return
      NF = Math.max(2, Math.round(video.duration * FPS))
      frames.length = NF
      // Frame 0 first — the hero hand-off depends on it.
      grabCtx.drawImage(video, 0, 0, grab.width, grab.height)
      grab.toBlob(
        (b) => {
          if (b && !stopped) frames[0] = b
          drawWant()
        },
        'image/jpeg',
        0.9,
      )
      video.play()

      if (video.requestVideoFrameCallback) {
        const onFrame = () => {
          if (stopped) return
          const i = Math.min(NF - 1, Math.round(video.currentTime * FPS))
          if (i > 0 && !frames[i]) {
            grabCtx.drawImage(video, 0, 0, grab.width, grab.height)
            grab.toBlob(
              (b) => {
                if (b && !stopped) frames[i] = b
                if (i === want) drawWant()
              },
              'image/jpeg',
              0.86,
            )
          } else if (i === want) {
            drawWant()
          }
          if (!video.ended) video.requestVideoFrameCallback(onFrame)
        }
        video.requestVideoFrameCallback(onFrame)
      } else {
        let i = 1
        seekHandler = () => {
          if (stopped) return
          grabCtx.drawImage(video, 0, 0, grab.width, grab.height)
          grab.toBlob(
            (b) => {
              if (b && !stopped) frames[i] = b
              if (i === want) drawWant()
            },
            'image/jpeg',
            0.86,
          )
          i++
          if (i < NF) video.currentTime = Math.min(i / FPS, video.duration - 0.01)
        }
        video.addEventListener('seeked', seekHandler)
        video.currentTime = Math.min(i / FPS, video.duration - 0.01)
      }
    }
    if (video.readyState >= 2) setupVideo()
    else video.addEventListener('loadeddata', setupVideo, { once: true })

    // -- §12 pointer ----------------------------------------------------------
    let mx = 0
    let my = 0
    let sx = 0
    let sy = 0
    const onPointer = (e) => {
      mx = e.clientX / window.innerWidth - 0.5
      my = e.clientY / window.innerHeight - 0.5
    }
    window.addEventListener('pointermove', onPointer)

    // -- §12 rAF loop ----------------------------------------------------------
    let raf = 0
    let sp = 0
    let last = performance.now()
    const tick = (now) => {
      if (stopped) return
      const dt = Math.min(64, now - last)
      last = now

      const rect = story.getBoundingClientRect()
      const p = Math.min(1, Math.max(0, -rect.top / (story.offsetHeight - window.innerHeight)))
      sp += (p - sp) * (1 - Math.pow(0.86, dt / 16.7))
      if (Math.abs(p - sp) < 0.0002) sp = p

      sx += (mx - sx) * 0.06
      sy += (my - sy) * 0.06

      // video scrub
      const vp = ease(seg(sp, 0.05, 0.7))
      want = Math.round(vp * (NF - 1))
      drawWant()

      // camera
      const camT = `translate3d(${(-sx * 18).toFixed(2)}px, ${(-sy * 12 + (1 - vp) * window.innerHeight * 0.06).toFixed(2)}px, 0) scale(${(1 + 0.02 * (1 - vp)).toFixed(4)})`
      cam.style.transform = camT
      camF.style.transform = camT

      // hero hand-off
      const ho = shown < 0 ? 0 : shown === 0 ? seg(sp, 0.06, 0.105) : 1
      plate.style.opacity = String(1 - ho)
      plate.style.visibility = ho >= 1 ? 'hidden' : 'visible'
      camF.style.opacity = String(1 - ho)
      camF.style.visibility = ho >= 1 ? 'hidden' : 'visible'

      // hero copy
      const h = seg(sp, 0.02, 0.1)
      heroCopy.style.transform = `translate3d(${(-sx * 10).toFixed(2)}px, ${(-h * 80 - sy * 6).toFixed(2)}px, 0) scale(${(1 + h * 0.12).toFixed(4)})`
      heroCopy.style.filter = `blur(${(h * 14).toFixed(2)}px)`
      heroCopy.style.opacity = String(1 - h)
      l0.style.visibility = 1 - h <= 0 ? 'hidden' : 'visible'

      hud.style.transform = `translate3d(${(sx * 26).toFixed(2)}px, ${(sy * 18 - h * 40).toFixed(2)}px, 0)`
      hud.style.opacity = String(1 - seg(sp, 0.02, 0.08))
      caption.style.opacity = String(1 - seg(sp, 0.01, 0.05))
      hint.style.opacity = String(1 - seg(sp, 0.01, 0.05))
      l1.style.visibility = 1 - seg(sp, 0.02, 0.08) <= 0 ? 'hidden' : 'visible'

      // fog roll-in (§8)
      const fg = ease(seg(sp, 0.38, 0.47))
      fog.style.visibility = fg <= 0 ? 'hidden' : 'visible'
      fog.style.opacity = String(fg)
      fog.style.transform = `translate3d(${((fg - 1) * 18 - sx * 10).toFixed(2)}vw, ${(-sy * 6).toFixed(2)}px, 0)`

      // vision words (§9)
      const vIn = seg(sp, 0.16, 0.27)
      const vOut = seg(sp, 0.34, 0.42)
      visionWords.forEach((w, i) => {
        const k = Math.min(1, Math.max(0, vIn * 1.6 - i * 0.08))
        w.style.transform = `translate3d(0, ${((1 - k) * 30 - vOut * 60).toFixed(2)}px, 0)`
        w.style.filter = `blur(${((1 - k) * 12 + vOut * 14).toFixed(2)}px)`
        w.style.opacity = String(k * (1 - vOut))
      })
      const vAll = Math.min(1, Math.max(0, vIn * 2 - 0.6)) * (1 - vOut)
      visionChip.style.opacity = String(vAll)
      visionChip.style.transform = `translate3d(0, ${(-vOut * 50).toFixed(2)}px, 0)`
      visionP.style.opacity = String(vAll)
      visionP.style.transform = `translate3d(0, ${(-vOut * 50).toFixed(2)}px, 0)`
      l2.style.visibility = vAll <= 0 ? 'hidden' : 'visible'

      // accounting write-in + hotspots (§10)
      const aIn = seg(sp, 0.45, 0.55)
      const aOut = seg(sp, 0.63, 0.68)
      const aCount = acctWords.length
      acctWords.forEach((w, i) => {
        const k = Math.min(1, Math.max(0, aIn * 2.2 - (i / aCount) * 1.2))
        w.style.opacity = String((0.15 + 0.85 * k) * (1 - aOut))
        w.style.filter = `blur(${((1 - k) * 6 + aOut * 10).toFixed(2)}px)`
      })
      hotspots.forEach((hotspot, i) => {
        const k = seg(sp, 0.49 + i * 0.02, 0.53 + i * 0.02)
        hotspot.style.visibility = k <= 0 || aOut >= 1 ? 'hidden' : 'visible'
        hotspot.style.transform = `translate3d(${(-sx * 30).toFixed(2)}px, ${(-sy * 20).toFixed(2)}px, 0) scale(${(k * (1 - aOut)).toFixed(4)})`
      })
      l3.style.visibility = aIn <= 0 ? 'hidden' : 'visible'

      // station (§11)
      const sIn = seg(sp, 0.72, 0.82)
      cred.style.opacity = String(sIn)
      cred.style.transform = `translate3d(0, ${((1 - sIn) * 30).toFixed(2)}px, 0)`
      cred.style.filter = `blur(${((1 - sIn) * 8).toFixed(2)}px)`
      const cIn = ease(seg(sp, 0.74, 0.86))
      card.style.opacity = String(cIn)
      card.style.transform = `translate3d(${(-sx * 14).toFixed(2)}px, ${((1 - cIn) * 60 - sy * 10).toFixed(2)}px, 0) rotateY(${(sx * 8).toFixed(2)}deg) rotateX(${(-sy * 8).toFixed(2)}deg)`
      const num = Math.round((savedCountRef?.current ?? 0) * seg(sp, 0.76, 0.9))
      if (numberEl) numberEl.textContent = String(num)
      if (sparkPolyline && sparkLen) {
        sparkPolyline.style.strokeDashoffset = String(sparkLen * (1 - seg(sp, 0.77, 0.92)))
      }
      featsRows.forEach((row, i) => {
        const k = ease(seg(sp, 0.76 + i * 0.025, 0.82 + i * 0.025))
        row.style.opacity = String(k)
        row.style.transform = `translateX(${((1 - k) * 40).toFixed(2)}px)`
      })
      l4.style.visibility = sIn <= 0 ? 'hidden' : 'visible'

      // rock fly-past (§6)
      rocks.forEach((rock) => {
        const x = parseFloat(rock.dataset.x)
        const y = parseFloat(rock.dataset.y)
        const depth = parseFloat(rock.dataset.depth)
        const fp = seg(sp, 0.02, 0.2)
        const fe = fp * fp
        rock.style.transform = `translate3d(${(x * fp * 70 + -sx * 24 * depth).toFixed(2)}vw, ${(y * fp * 60 + -sy * 16 * depth).toFixed(2)}vh, 0) scale(${(1 + fe * 1.6 * depth).toFixed(4)})`
        rock.style.filter = `blur(${(fe * 5 * depth).toFixed(2)}px)`
        rock.style.visibility = fp >= 1 ? 'hidden' : 'visible'
      })

      // progress
      prog.style.transform = `scaleX(${sp.toFixed(4)})`

      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)

    // -- cleanup ----------------------------------------------------------------
    return () => {
      stopped = true
      cancelAnimationFrame(raf)
      window.removeEventListener('resize', sizeCanvas)
      window.removeEventListener('pointermove', onPointer)
      if (seekHandler) video.removeEventListener('seeked', seekHandler)
      video.pause()
      video.removeAttribute('src')
      video.load()
      liveBitmaps.forEach((b) => b.close())
      liveBitmaps.clear()
    }
  }, [rootRef, savedCountRef])
}
