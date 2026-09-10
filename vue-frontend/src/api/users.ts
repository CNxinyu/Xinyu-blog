import type { PageResponse, User, UserRole, UserStatus } from '../types/api'
import { requestData } from './client'

export interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: UserStatus
  role?: UserRole
}

export const usersApi = {
  page: (params: UserQuery) =>
    requestData<PageResponse<User>>({ method: 'GET', url: '/api/v1/admin/users', params }),
  updateStatus: (id: number, status: UserStatus) =>
    requestData<User>({ method: 'PATCH', url: `/api/v1/admin/users/${id}/status`, data: { status } }),
  updateRole: (id: number, role: UserRole) =>
    requestData<User>({ method: 'PATCH', url: `/api/v1/admin/users/${id}/role`, data: { role } }),
}
