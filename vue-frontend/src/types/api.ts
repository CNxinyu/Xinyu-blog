export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type CommentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type UserRole = 'USER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface User {
  id: number
  username: string
  email: string
  nickname: string | null
  avatarUrl: string | null
  bio: string | null
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface AuthPayload {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface Category {
  id: number
  name: string
  slug: string
  description: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface Tag {
  id: number
  name: string
  slug: string
  createdAt: string | null
  updatedAt: string | null
}

export interface ArticleAuthor {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
}

export interface ArticleSummary {
  id: number
  title: string
  slug: string
  summary: string | null
  status: ArticleStatus
  category: Category | null
  tags: Tag[]
  author: ArticleAuthor | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ArticleDetail extends ArticleSummary {
  contentMarkdown: string
  contentHtml: string
  archivedAt: string | null
}

export interface ArticleWritePayload {
  title: string
  slug: string
  summary: string | null
  categoryId: number
  tagIds: number[]
  contentMarkdown: string
}

export interface CommentAuthor {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
}

export interface Comment {
  id: number
  articleId: number
  parentId: number | null
  content: string
  status: CommentStatus
  author: CommentAuthor
  createdAt: string
  updatedAt: string
}

export type FieldErrors = Record<string, string>
