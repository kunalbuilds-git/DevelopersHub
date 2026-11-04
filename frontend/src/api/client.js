const TOKEN_KEY = 'dh.token'
const REFRESH_KEY = 'dh.refreshToken'
const EMAIL_KEY = 'dh.email'

export function getStoredAuth() {
  return {
    token: localStorage.getItem(TOKEN_KEY),
    refreshToken: localStorage.getItem(REFRESH_KEY),
    email: localStorage.getItem(EMAIL_KEY),
  }
}

export function storeAuth({ token, refreshToken, email }) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
  if (email) localStorage.setItem(EMAIL_KEY, email)
}

export function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(EMAIL_KEY)
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem(REFRESH_KEY)
  if (!refreshToken) return false
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  if (!res.ok) return false
  const data = await res.json()
  storeAuth(data)
  return true
}

/**
 * Thin fetch wrapper for the DevelopersHub API.
 * - attaches the stored JWT
 * - on 401, tries one refresh-and-retry when a refresh token exists
 * - turns RFC 7807 ProblemDetail error bodies into readable Error messages
 */
export async function api(path, { method = 'GET', body, auth = true, canRetry = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const { token } = getStoredAuth()
  if (auth && token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401 && auth && canRetry && (await refreshAccessToken())) {
    return api(path, { method, body, auth, canRetry: false })
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const problem = await res.json()
      message = problem.detail || problem.title || message
    } catch {
      // non-JSON error body - keep the default message
    }
    const error = new Error(message)
    error.status = res.status
    throw error
  }

  // 201/200 responses can legitimately have an empty body (e.g. bookmark creation).
  const text = await res.text()
  if (!text) return null
  return JSON.parse(text)
}
