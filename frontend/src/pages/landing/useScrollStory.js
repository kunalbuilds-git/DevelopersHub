import { useEffect } from 'react'

const FPS = 24

/** §12 helpers */
function seg(p, a, b) {
  return Math.min(1, Math.max(0, (p - a) / (b - a)))
}
function ease(t) {
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
