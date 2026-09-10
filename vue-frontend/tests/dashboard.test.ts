import { beforeEach, describe, expect, it, vi } from 'vitest'
import { articlesApi } from '../src/api/articles'
import { commentsApi } from '../src/api/comments'
import { loadDashboard } from '../src/api/dashboard'
import { taxonomyApi } from '../src/api/taxonomy'
import { usersApi } from '../src/api/users'
import type { ArticleSummary } from '../src/types/api'

vi.mock('../src/api/articles', () => ({ articlesApi: { page: vi.fn() } }))
vi.mock('../src/api/comments', () => ({ commentsApi: { page: vi.fn() } }))
vi.mock('../src/api/taxonomy', () => ({ taxonomyApi: { categories: vi.fn(), tags: vi.fn() } }))
vi.mock('../src/api/users', () => ({ usersApi: { page: vi.fn() } }))

const article: ArticleSummary = {
  id: 9,
  title: '魔法与秩序',
  slug: 'magic-and-order',
  summary: null,
  status: 'DRAFT',
  category: null,
  tags: [],
  author: null,
  publishedAt: null,
  createdAt: '2026-09-10T00:00:00Z',
  updatedAt: '2026-09-10T00:00:00Z',
}

function page(total: number, items: unknown[] = []) {
  return { items, page: 1, size: 1, total, totalPages: total ? 1 : 0 }
}

beforeEach(() => {
  vi.mocked(articlesApi.page).mockReset()
  vi.mocked(commentsApi.page).mockReset()
  vi.mocked(usersApi.page).mockReset()
  vi.mocked(taxonomyApi.categories).mockReset()
  vi.mocked(taxonomyApi.tags).mockReset()
})

describe('dashboard aggregation', () => {
  it('maps totals and recent articles from existing APIs', async () => {
    vi.mocked(articlesApi.page)
      .mockResolvedValueOnce(page(12, [article]) as never)
      .mockResolvedValueOnce(page(7) as never)
      .mockResolvedValueOnce(page(3) as never)
    vi.mocked(commentsApi.page).mockResolvedValue(page(4) as never)
    vi.mocked(usersApi.page).mockResolvedValue(page(26) as never)
    vi.mocked(taxonomyApi.categories).mockResolvedValue([{ id: 1 }] as never)
    vi.mocked(taxonomyApi.tags).mockResolvedValue([{ id: 1 }, { id: 2 }] as never)

    const result = await loadDashboard()

    expect(result.metrics).toEqual({
      articles: 12,
      published: 7,
      drafts: 3,
      pendingComments: 4,
      users: 26,
      categories: 1,
      tags: 2,
    })
    expect(result.recentArticles).toEqual([article])
    expect(result.failed).toEqual([])
  })

  it('keeps successful cards when one source fails', async () => {
    vi.mocked(articlesApi.page).mockResolvedValue(page(5, [article]) as never)
    vi.mocked(commentsApi.page).mockRejectedValue(new Error('offline'))
    vi.mocked(usersApi.page).mockResolvedValue(page(8) as never)
    vi.mocked(taxonomyApi.categories).mockResolvedValue([])
    vi.mocked(taxonomyApi.tags).mockResolvedValue([])

    const result = await loadDashboard()

    expect(result.metrics.articles).toBe(5)
    expect(result.metrics.pendingComments).toBeNull()
    expect(result.failed).toContain('pendingComments')
  })
})
