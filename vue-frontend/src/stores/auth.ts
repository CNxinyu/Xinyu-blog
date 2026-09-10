import { defineStore } from 'pinia'
import { authApi } from '../api/auth'
import { ApiError, setAccessToken } from '../api/client'
import type { AuthPayload, User } from '../types/api'

let refreshPromise: Promise<boolean> | null = null

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    bootstrapped: false,
    authenticating: false,
  }),
  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN' && state.user.status === 'ACTIVE',
  },
  actions: {
    applySession(payload: AuthPayload) {
      if (payload.user.role !== 'ADMIN' || payload.user.status !== 'ACTIVE') {
        throw new ApiError('仅管理员可访问管理台', 403, 40300)
      }
      this.user = payload.user
      setAccessToken(payload.accessToken)
    },
    clearSession() {
      this.user = null
      setAccessToken(null)
    },
    async bootstrap() {
      if (this.bootstrapped) return this.isAdmin
      await this.refreshSession()
      this.bootstrapped = true
      return this.isAdmin
    },
    async refreshSession(): Promise<boolean> {
      if (refreshPromise) return refreshPromise
      refreshPromise = (async () => {
        try {
          const payload = await authApi.refresh()
          this.applySession(payload)
          return true
        } catch {
          this.clearSession()
          return false
        } finally {
          refreshPromise = null
        }
      })()
      return refreshPromise
    },
    async login(identifier: string, password: string) {
      this.authenticating = true
      try {
        const payload = await authApi.login(identifier, password)
        if (payload.user.role !== 'ADMIN' || payload.user.status !== 'ACTIVE') {
          try {
            await authApi.logout()
          } finally {
            this.clearSession()
          }
          throw new ApiError('仅管理员可访问管理台', 403, 40300)
        }
        this.applySession(payload)
        this.bootstrapped = true
      } finally {
        this.authenticating = false
      }
    },
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.clearSession()
        this.bootstrapped = true
      }
    },
  },
})
