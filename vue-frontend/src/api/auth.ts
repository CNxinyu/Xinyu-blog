import type { AuthPayload } from '../types/api'
import { directRequestData, ensureCsrfToken } from './client'

export const authApi = {
  async login(identifier: string, password: string) {
    await ensureCsrfToken()
    return directRequestData<AuthPayload>({
      method: 'POST',
      url: '/api/v1/auth/login',
      data: { identifier, password },
    })
  },
  async refresh() {
    await ensureCsrfToken()
    return directRequestData<AuthPayload>({ method: 'POST', url: '/api/v1/auth/refresh' })
  },
  async logout() {
    await ensureCsrfToken()
    return directRequestData<void>({ method: 'POST', url: '/api/v1/auth/logout' })
  },
}
