import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { api, getStoredAuth, storeAuth, clearStoredAuth } from './client.js'

const TOKEN_KEY = 'dh.token'
const REFRESH_KEY = 'dh.refreshToken'
const EMAIL_KEY = 'dh.email'

function jsonResponse(payload, status = 200) {
  const text = JSON.stringify(payload)
  return {
    ok: status >= 200 && status < 300,
    status,
    text: async () => text,
    json: async () => JSON.parse(text),
  }
}

function emptyResponse(status = 201) {
  return {
    ok: status >= 200 && status < 300,
    status,
    text: async () => '',
    json: async () => {
      throw new SyntaxError('Unexpected end of JSON input')
    },
  }
}

function nonJsonResponse(body = 'boom', status = 500) {
  return {
    ok: false,
    status,
    text: async () => body,
    json: async () => {
      throw new SyntaxError('Unexpected token in JSON')
    },
  }
}

let fetchMock

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  localStorage.clear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('getStoredAuth', () => {
  it('returns the stored token, refresh token and email', () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-123')
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    localStorage.setItem(EMAIL_KEY, 'dev@example.com')

    expect(getStoredAuth()).toEqual({
      token: 'jwt-123',
      refreshToken: 'refresh-123',
      email: 'dev@example.com',
    })
  })

  it('returns null for every key when nothing is stored', () => {
    expect(getStoredAuth()).toEqual({ token: null, refreshToken: null, email: null })
  })

  it('returns null for the keys that are missing', () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-123')

    expect(getStoredAuth()).toEqual({
      token: 'jwt-123',
      refreshToken: null,
      email: null,
    })
  })
})

describe('storeAuth', () => {
  it('writes the token, refresh token and email to localStorage', () => {
    storeAuth({ token: 'jwt-123', refreshToken: 'refresh-123', email: 'dev@example.com' })

    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-123')
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-123')
    expect(localStorage.getItem(EMAIL_KEY)).toBe('dev@example.com')
  })

  it('leaves an existing refresh token untouched when no new one is given', () => {
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    localStorage.setItem(EMAIL_KEY, 'old@example.com')

    storeAuth({ token: 'jwt-456' })

    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-456')
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-123')
    expect(localStorage.getItem(EMAIL_KEY)).toBe('old@example.com')
  })

  it('ignores empty and missing values', () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-123')

    storeAuth({ token: '', refreshToken: undefined, email: null })

    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-123')
    expect(localStorage.getItem(REFRESH_KEY)).toBeNull()
    expect(localStorage.getItem(EMAIL_KEY)).toBeNull()
  })
})

describe('clearStoredAuth', () => {
  it('removes every stored auth key', () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-123')
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    localStorage.setItem(EMAIL_KEY, 'dev@example.com')

    clearStoredAuth()

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(REFRESH_KEY)).toBeNull()
    expect(localStorage.getItem(EMAIL_KEY)).toBeNull()
  })
})

describe('api request building', () => {
  it('prefixes the path with /api and defaults to GET', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await api('/opportunities')

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/opportunities')
    expect(options.method).toBe('GET')
  })

  it('attaches the stored JWT as a bearer token', async () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-123')
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await api('/opportunities')

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer jwt-123')
  })

  it('omits the Authorization header when no token is stored', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await api('/opportunities')

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined()
  })

  it('does not attach the Authorization header when auth is false', async () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-123')
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await api('/auth/login', { auth: false })

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined()
  })

  it('sends no body and no Content-Type header when there is no body', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await api('/opportunities')

    const options = fetchMock.mock.calls[0][1]
    expect(options.body).toBeUndefined()
    expect(options.headers['Content-Type']).toBeUndefined()
  })

  it('stringifies a JSON body and sets the Content-Type header', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await api('/opportunities', { method: 'POST', body: { keyword: 'rust' } })

    const options = fetchMock.mock.calls[0][1]
    expect(options.method).toBe('POST')
    expect(options.body).toBe(JSON.stringify({ keyword: 'rust' }))
    expect(options.headers['Content-Type']).toBe('application/json')
  })
})

describe('api responses', () => {
  it('parses a successful JSON body', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ content: [{ id: 7 }], totalElements: 1 }))

    const data = await api('/opportunities')

    expect(data).toEqual({ content: [{ id: 7 }], totalElements: 1 })
  })

  it('resolves to null for an empty response body', async () => {
    fetchMock.mockResolvedValue(emptyResponse(201))

    await expect(api('/bookmarks/7', { method: 'POST' })).resolves.toBeNull()
  })

  it('throws the ProblemDetail detail as the error message and attaches the status', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ title: 'Forbidden', detail: 'Not your call.' }, 403))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Not your call.',
      status: 403,
    })
  })

  it('falls back to the ProblemDetail title when detail is missing', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ title: 'Bad Request' }, 400))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Bad Request',
      status: 400,
    })
  })

  it('falls back to a default message when the error body is not JSON', async () => {
    fetchMock.mockResolvedValue(nonJsonResponse('<html>500</html>', 500))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Request failed (500)',
      status: 500,
    })
  })

  it('falls back to a default message when the JSON error body has neither detail nor title', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ type: 'about:blank' }, 422))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Request failed (422)',
      status: 422,
    })
  })
})

describe('api refresh and retry', () => {
  it('refreshes the access token and retries the original request once on a 401', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired-token')
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ detail: 'Token expired' }, 401))
      .mockResolvedValueOnce(jsonResponse({ token: 'fresh-token', refreshToken: 'refresh-456' }))
      .mockResolvedValueOnce(jsonResponse({ content: [] }))

    const data = await api('/opportunities')

    expect(data).toEqual({ content: [] })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh')
    expect(fetchMock.mock.calls[1][1]).toEqual({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: 'refresh-123' }),
    })
    expect(fetchMock.mock.calls[2][0]).toBe('/api/opportunities')
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer fresh-token')
  })

  it('stores the new auth returned by the refresh call', async () => {
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ detail: 'Token expired' }, 401))
      .mockResolvedValueOnce(jsonResponse({ token: 'fresh-token', refreshToken: 'refresh-456' }))
      .mockResolvedValueOnce(jsonResponse({ content: [] }))

    await api('/opportunities')

    expect(localStorage.getItem(TOKEN_KEY)).toBe('fresh-token')
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-456')
  })

  it('retries the original request exactly once and throws the second 401', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired-token')
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ detail: 'Token expired' }, 401))
      .mockResolvedValueOnce(jsonResponse({ token: 'fresh-token' }))
      .mockResolvedValueOnce(jsonResponse({ detail: 'Still expired' }, 401))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Still expired',
      status: 401,
    })

    // original + refresh + one retry - the retry must not refresh again
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls.filter(([url]) => url === '/api/opportunities')).toHaveLength(2)
    expect(fetchMock.mock.calls.filter(([url]) => url === '/api/auth/refresh')).toHaveLength(1)
  })

  it('throws the original 401 without retrying when the refresh call fails', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired-token')
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ detail: 'Token expired' }, 401))
      .mockResolvedValueOnce(jsonResponse({ detail: 'Refresh token revoked' }, 401))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Token expired',
      status: 401,
    })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('does not attempt a refresh when no refresh token is stored', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired-token')
    fetchMock.mockResolvedValue(jsonResponse({ detail: 'Token expired' }, 401))

    await expect(api('/opportunities')).rejects.toMatchObject({
      message: 'Token expired',
      status: 401,
    })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('does not refresh a 401 when auth is false', async () => {
    localStorage.setItem(REFRESH_KEY, 'refresh-123')
    fetchMock.mockResolvedValue(jsonResponse({ detail: 'Bad credentials' }, 401))

    await expect(api('/auth/login', { auth: false })).rejects.toMatchObject({
      message: 'Bad credentials',
      status: 401,
    })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
