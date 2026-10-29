import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { api, clearStoredAuth, getStoredAuth, storeAuth } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const { token, email } = getStoredAuth()
    return token ? { email } : null
  })

  const login = useCallback(async (email, password) => {
    const data = await api('/auth/login', { method: 'POST', body: { email, password }, auth: false })
    storeAuth({ ...data, email })
    setUser({ email })
  }, [])

  const register = useCallback(async (email, password) => {
    const data = await api('/auth/register', { method: 'POST', body: { email, password }, auth: false })
    storeAuth({ ...data, email })
    setUser({ email })
  }, [])

  const logout = useCallback(() => {
    const { refreshToken } = getStoredAuth()
    // Best effort: tell the backend to revoke the refresh token, then drop
    // the local credentials either way.
    if (refreshToken) {
      api('/auth/logout', { method: 'POST', body: { refreshToken }, auth: false }).catch(() => {})
    }
    clearStoredAuth()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, login, register, logout, isLoggedIn: user !== null }),
    [user, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
