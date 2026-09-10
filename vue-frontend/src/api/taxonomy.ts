import type { Category, Tag } from '../types/api'
import { requestData } from './client'

export const taxonomyApi = {
  categories: () => requestData<Category[]>({ method: 'GET', url: '/api/categories' }),
  tags: () => requestData<Tag[]>({ method: 'GET', url: '/api/tags' }),
  createCategory: (data: { name: string; slug: string; description: string | null }) =>
    requestData<Category>({ method: 'POST', url: '/api/admin/categories', data }),
  updateCategory: (id: number, data: { name: string; slug: string; description: string | null }) =>
    requestData<Category>({ method: 'PUT', url: `/api/admin/categories/${id}`, data }),
  deleteCategory: (id: number) =>
    requestData<void>({ method: 'DELETE', url: `/api/admin/categories/${id}` }),
  createTag: (data: { name: string; slug: string }) =>
    requestData<Tag>({ method: 'POST', url: '/api/admin/tags', data }),
  updateTag: (id: number, data: { name: string; slug: string }) =>
    requestData<Tag>({ method: 'PUT', url: `/api/admin/tags/${id}`, data }),
  deleteTag: (id: number) =>
    requestData<void>({ method: 'DELETE', url: `/api/admin/tags/${id}` }),
}
