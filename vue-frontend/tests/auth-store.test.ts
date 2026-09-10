import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '../src/api/auth'
import { ApiError } from '../src/api/client'
import { useAuthStore } from '../src/stores/auth'
import type { AuthPayload, User } from '../src/types/api'

vi.mock('../src/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  },
}))

function user(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    username: 'admin',
    email: 'admin@example.com',
    nickname: '馆长',
    avatarUrl: null,
    bio: null,
    role: 'ADMIN',
    status: 'ACTIVE',
    createdAt: '2026-09-10T00:00:00Z',
    updatedAt: '2026-09-10T00:00:00Z',
    ...overrides,
  }
}

function session(overrides: Partial<User> = {}): AuthPayload {
  return { accessToken: 'access-token', tokenType: 'Bearer', expiresIn: 900, user: user(overrides) }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(authApi.login).mockReset()
  vi.mocked(authApi.refresh).mockReset()
  vi.mocked(authApi.logout).mockReset().mockResolvedValue(undefined)
})

describe('auth store', () => {
  it('accepts an active administrator session without persisting browser state', async () => {
    vi.mocked(authApi.login).mockResolvedValue(session())
    const store = useAuthStore()

    await store.login('admin', 'secret')

    expect(store.isAdmin).toBe(true)
    expect(store.user?.username).toBe('admin')
    expect(window.localStorage.length).toBe(0)
  })

  it('revokes a refresh session when a non-admin logs in', async () => {
    vi.mocked(authApi.login).mockResolvedValue(session({ role: 'USER' }))
    const store = useAuthStore()

    await expect(store.login('reader', 'secret')).rejects.toBeInstanceOf(ApiError)
    expect(authApi.logout).toHaveBeenCalledOnce()
    expect(store.user).toBeNull()
  })

  it('coalesces concurrent refresh attempts into one rotation', async () => {
    let resolveRefresh!: (payload: AuthPayload) => void
    vi.mocked(authApi.refresh).mockImplementation(
      () => new Promise<AuthPayload>((resolve) => { resolveRefresh = resolve }),
    )
    const store = useAuthStore()

    const first = store.refreshSession()
    const second = store.refreshSession()
    expect(authApi.refresh).toHaveBeenCalledOnce()
    resolveRefresh(session())

    await expect(Promise.all([first, second])).resolves.toEqual([true, true])
    expect(store.isAdmin).toBe(true)
  })

  it('clears the session when refresh fails', async () => {
    vi.mocked(authApi.refresh).mockRejectedValue(new ApiError('expired', 401, 40101))
    const store = useAuthStore()

    await expect(store.refreshSession()).resolves.toBe(false)
    expect(store.user).toBeNull()
  })
})
