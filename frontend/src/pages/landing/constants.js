/**
 * The backend does not yet sort the listing by deadline (the query has no
 * ORDER BY). Flip to true once the deadline-ascending sort lands on the
 * backend; it enables the hero HUD pre and the hotspot soonest-item titles.
 * Until then: HUD pre hidden, hotspots show "Browse →" links.
 */
export const HAS_DEADLINE_SORT = false
