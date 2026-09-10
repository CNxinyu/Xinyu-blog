import type { Comment, CommentStatus, PageResponse } from '../types/api'
import { requestData } from './client'

export interface CommentQuery {
  page?: number
  size?: number
  status?: CommentStatus
  articleId?: number
  keyword?: string
}

export const commentsApi = {
  page: (params: CommentQuery) =>
    requestData<PageResponse<Comment>>({ method: 'GET', url: '/api/admin/comments', params }),
  updateStatus: (id: number, status: CommentStatus) =>
    requestData<Comment>({ method: 'PUT', url: `/api/admin/comments/${id}/status`, data: { status } }),
  delete: (id: number) =>
    requestData<void>({ method: 'DELETE', url: `/api/admin/comments/${id}` }),
}
