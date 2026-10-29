import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'

const SavedContext = createContext(null)

/**
 * Tracks which opportunity ids the logged-in user has bookmarked, so every
 * card can render the right toggle state without extra requests.
 */
export function SavedProvider({ children }) {
  const { isLoggedIn } = useAuth()
  const [savedIds, setSavedIds] = useState(() => new Set())

  useEffect(() => {
    let cancelled = false
    if (!isLoggedIn) {
      setSavedIds(new Set())
      return
    }
    api('/bookmarks')
      .then((bookmarks) => {
        if (cancelled) return
        setSavedIds(new Set(bookmarks.map((b) => b.opportunityId)))
      })
      .catch(() => {
        // A failed load just means unknown state; toggles will reconcile.
      })
    return () => {
      cancelled = true
    }
  }, [isLoggedIn])

  const markSaved = useCallback((id, saved) => {
    setSavedIds((current) => {
      const next = new Set(current)
      if (saved) {
        next.add(id)
      } else {
        next.delete(id)
      }
      return next
    })
  }, [])

  const value = useMemo(
    () => ({ savedIds, markSaved, isSaved: (id) => savedIds.has(id) }),
    [savedIds, markSaved],
  )

  return <SavedContext.Provider value={value}>{children}</SavedContext.Provider>
}

export function useSaved() {
  return useContext(SavedContext)
}
