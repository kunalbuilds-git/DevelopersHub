/**
 * Spec glass panel, two variants:
 *  - "scene" (default): dark translucent glass for content sitting on imagery
 *    (e.g. the station card, §11).
 *  - "paper": light translucent glass for lists on pale backgrounds
 *    (e.g. the feature rows, §11).
 * Glass/blur is allowed only where the spec uses it.
 */
export default function GlassPanel({ variant = 'scene', className = '', children, ...rest }) {
  return (
    <div className={`glass-panel glass-panel--${variant} ${className}`} {...rest}>
      {children}
    </div>
  )
}
