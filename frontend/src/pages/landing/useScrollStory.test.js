import { describe, it, expect, vi, afterEach } from 'vitest'
import { renderHook } from '@testing-library/react'
import { ease, useScrollStory } from './useScrollStory.js'

// The hook itself drives the landing page imperatively (canvas, video frames
// and ~25 required selectors), so it is exercised through its pure helpers
// here rather than through a full DOM fixture.
describe('ease', () => {
  it('returns 0 at the start of the range', () => {
    expect(ease(0)).toBe(0)
  })

  it('returns 1 at the end of the range', () => {
    expect(ease(1)).toBe(1)
  })

  it('eases in over the first half of the range', () => {
    expect(ease(0.25)).toBeCloseTo(0.0625)
  })

  it('eases out over the second half of the range', () => {
    expect(ease(0.75)).toBeCloseTo(0.9375)
  })

  it('is symmetric around the midpoint', () => {
    expect(ease(0.5)).toBeCloseTo(0.5)
    expect(ease(0.5 - 0.2)).toBeCloseTo(1 - ease(0.5 + 0.2))
  })

  it('never leaves the unit range', () => {
    for (const t of [0, 0.1, 0.25, 0.5, 0.75, 0.9, 1]) {
      expect(ease(t)).toBeGreaterThanOrEqual(0)
      expect(ease(t)).toBeLessThanOrEqual(1)
    }
  })

  it('never decreases as the progress grows', () => {
    const samples = [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1].map(ease)

    for (let i = 1; i < samples.length; i += 1) {
      expect(samples[i]).toBeGreaterThanOrEqual(samples[i - 1])
    }
  })
})

describe('useScrollStory', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('does not start the animation loop when the root element is missing', () => {
    const raf = vi.spyOn(window, 'requestAnimationFrame')

    const { unmount } = renderHook(() => useScrollStory({ current: null }, { current: 0 }))

    expect(raf).not.toHaveBeenCalled()

    unmount()
  })
})
