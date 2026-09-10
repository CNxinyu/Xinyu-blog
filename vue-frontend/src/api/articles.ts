import type {
  ArticleDetail,
  ArticleStatus,
  ArticleSummary,
  ArticleWritePayload,
  PageResponse,
} from '../types/api'
import { requestData } from './client'

export interface ArticleQuery {
  page?: number
  size?: number
  status?: ArticleStatus
  keyword?: string
  categoryId?: number
  tagId?: number
}

export const articlesApi = {
  page: (params: ArticleQuery) =>
    requestData<PageResponse<ArticleSummary>>({ method: 'GET', url: '/api/admin/articles', params }),
  detail: (id: number) =>
    requestData<ArticleDetail>({ method: 'GET', url: `/api/admin/articles/${id}` }),
  create: (data: ArticleWritePayload) =>
    requestData<ArticleDetail>({ method: 'POST', url: '/api/admin/articles', data }),
  update: (id: number, data: ArticleWritePayload) =>
    requestData<ArticleDetail>({ method: 'PUT', url: `/api/admin/articles/${id}`, data }),
  changeStatus: (id: number, action: 'draft' | 'publish' | 'archive') =>
    requestData<ArticleDetail>({ method: 'POST', url: `/api/admin/articles/${id}/${action}` }),
  delete: (id: number) =>
    requestData<void>({ method: 'DELETE', url: `/api/admin/articles/${id}` }),
  preview: (contentMarkdown: string) =>
    requestData<{ contentHtml: string }>({
      method: 'POST',
      url: '/api/admin/articles/preview',
      data: { contentMarkdown },
    }),
}
