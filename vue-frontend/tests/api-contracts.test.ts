import { beforeEach, describe, expect, it, vi } from 'vitest'
import { articlesApi } from '../src/api/articles'
import { commentsApi } from '../src/api/comments'
import { requestData } from '../src/api/client'
import { taxonomyApi } from '../src/api/taxonomy'
import { usersApi } from '../src/api/users'

vi.mock('../src/api/client', () => ({ requestData: vi.fn() }))

beforeEach(() => vi.mocked(requestData).mockReset().mockResolvedValue(undefined as never))

describe('admin API contracts', () => {
  it('uses the article lifecycle endpoints', async () => {
    await articlesApi.page({ page: 2, status: 'DRAFT' })
    await articlesApi.changeStatus(7, 'publish')
    await articlesApi.preview('# title')

    expect(requestData).toHaveBeenNthCalledWith(1, {
      method: 'GET', url: '/api/admin/articles', params: { page: 2, status: 'DRAFT' },
    })
    expect(requestData).toHaveBeenNthCalledWith(2, {
      method: 'POST', url: '/api/admin/articles/7/publish',
    })
    expect(requestData).toHaveBeenNthCalledWith(3, {
      method: 'POST', url: '/api/admin/articles/preview', data: { contentMarkdown: '# title' },
    })
  })

  it('uses taxonomy, comment and user mutation endpoints', async () => {
    await taxonomyApi.deleteCategory(2)
    await taxonomyApi.updateTag(3, { name: 'Vue', slug: 'vue' })
    await commentsApi.updateStatus(4, 'APPROVED')
    await usersApi.updateRole(5, 'ADMIN')

    expect(requestData).toHaveBeenNthCalledWith(1, { method: 'DELETE', url: '/api/admin/categories/2' })
    expect(requestData).toHaveBeenNthCalledWith(2, {
      method: 'PUT', url: '/api/admin/tags/3', data: { name: 'Vue', slug: 'vue' },
    })
    expect(requestData).toHaveBeenNthCalledWith(3, {
      method: 'PUT', url: '/api/admin/comments/4/status', data: { status: 'APPROVED' },
    })
    expect(requestData).toHaveBeenNthCalledWith(4, {
      method: 'PATCH', url: '/api/v1/admin/users/5/role', data: { role: 'ADMIN' },
    })
  })
})
