export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  traceId: string | null;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface Author {
  id: number;
  username: string;
  nickname: string | null;
  avatarUrl: string | null;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface Tag {
  id: number;
  name: string;
  slug: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ArticleSummary {
  id: number;
  title: string;
  slug: string;
  summary: string | null;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  category: Category | null;
  tags: Tag[];
  author: Author | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ArticleDetail extends ArticleSummary {
  contentMarkdown: string;
  contentHtml: string;
  archivedAt: string | null;
}
