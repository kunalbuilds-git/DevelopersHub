import { describe, it, expect } from 'vitest'
import { HAS_DEADLINE_SORT } from './constants.js'

describe('landing constants', () => {
  it('reports that the listing is not sorted by deadline yet', () => {
    // The flag gates the hero HUD and the hotspot "soonest" titles; it flips
    // to true once the backend query gains an ORDER BY on the deadline.
    expect(HAS_DEADLINE_SORT).toBe(false)
  })
})
