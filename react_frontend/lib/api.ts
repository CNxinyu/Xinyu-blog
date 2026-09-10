import "server-only";

import { cache } from "react";

import type {
  ApiResponse,
  ArticleDetail,
  ArticleSummary,
  Category,
  PageResponse,
  Tag,
} from "@/lib/types";

const API_BASE_URL = (process.env.API_BASE_URL ?? "http://127.0.0.1:8080").replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code: number | null,
    public readonly traceId: string | null,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function buildUrl(path: string, params?: Record<string, string | number | undefined>) {
  const url = new URL(path, `${API_BASE_URL}/`);
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined) url.searchParams.set(key, String(value));
  });
  return url;
}

async function request<T>(path: string, params?: Record<string, string | number | undefined>) {
  const url = buildUrl(path, params);
  let response: Response;

  try {
    response = await fetch(url, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
  } catch (cause) {
    throw new ApiError(
      cause instanceof Error ? `无法连接内容服务：${cause.message}` : "无法连接内容服务",
      503,
      null,
      null,
    );
  }

  let envelope: ApiResponse<T> | null = null;
  try {
    envelope = (await response.json()) as ApiResponse<T>;
  } catch {
    throw new ApiError("内容服务返回了无法识别的响应", response.status, null, null);
  }

  if (!response.ok || envelope.code !== 0) {
    throw new ApiError(
      envelope.message || `内容服务请求失败（HTTP ${response.status}）`,
      response.status,
      envelope.code,
      envelope.traceId,
    );
  }

  return envelope.data;
}

export interface ArticleQuery {
  page: number;
  size?: number;
  keyword?: string;
  categoryId?: number;
  tagId?: number;
}

export function getArticles({ page, size = 9, keyword, categoryId, tagId }: ArticleQuery) {
  return request<PageResponse<ArticleSummary>>("/api/articles", {
    page,
    size,
    keyword,
    categoryId,
    tagId,
  });
}

export const getArticle = cache((slug: string) =>
  request<ArticleDetail>(`/api/articles/${encodeURIComponent(slug)}`),
);

export const getCategories = cache(() => request<Category[]>("/api/categories"));

export const getTags = cache(() => request<Tag[]>("/api/tags"));

export function searchArticles(query: string, page: number, size = 9) {
  return request<PageResponse<ArticleSummary>>("/api/search", { q: query, page, size });
}
