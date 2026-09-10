import type { ArticleSummary } from '../types/api'
import { articlesApi } from './articles'
import { commentsApi } from './comments'
import { taxonomyApi } from './taxonomy'
import { usersApi } from './users'

export type MetricKey =
  | 'articles'
  | 'published'
  | 'drafts'
  | 'pendingComments'
  | 'users'
  | 'categories'
  | 'tags'

export interface DashboardData {
  metrics: Record<MetricKey, number | null>
  recentArticles: ArticleSummary[]
  failed: MetricKey[]
}

export async function loadDashboard(): Promise<DashboardData> {
  const tasks = {
    articles: articlesApi.page({ page: 1, size: 5 }),
    published: articlesApi.page({ page: 1, size: 1, status: 'PUBLISHED' }),
    drafts: articlesApi.page({ page: 1, size: 1, status: 'DRAFT' }),
    pendingComments: commentsApi.page({ page: 1, size: 1, status: 'PENDING' }),
    users: usersApi.page({ page: 1, size: 1 }),
    categories: taxonomyApi.categories(),
    tags: taxonomyApi.tags(),
  }

  const entries = Object.entries(tasks) as [MetricKey, Promise<unknown>][]
  const settled = await Promise.allSettled(entries.map(([, promise]) => promise))
  const metrics = Object.fromEntries(entries.map(([key]) => [key, null])) as Record<MetricKey, number | null>
  const failed: MetricKey[] = []
  let recentArticles: ArticleSummary[] = []

  settled.forEach((result, index) => {
    const key = entries[index][0]
    if (result.status === 'rejected') {
      failed.push(key)
      return
    }
    if (key === 'categories' || key === 'tags') {
      metrics[key] = (result.value as unknown[]).length
      return
    }
    const page = result.value as { total: number; items: ArticleSummary[] }
    metrics[key] = page.total
    if (key === 'articles') recentArticles = page.items
  })

  return { metrics, recentArticles, failed }
}
