/**
 * Spec §1 .chip — the lime tag used across the whole product (landing scene
 * labels, type/level/tag badges, active filters). Lime fill, ink text.
 */
export default function Chip({ children, className = '' }) {
  return <span className={`chip ${className}`}>{children}</span>
}
