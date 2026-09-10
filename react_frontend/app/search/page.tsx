import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { Search } from "lucide-react";

import { ApiFailure } from "@/components/api-failure";
import { ArticleList } from "@/components/article-list";
import { SiteShell } from "@/components/site-shell";
import { firstValue, normalizePage, pageHref, type QueryValue } from "@/lib/navigation";
import { searchArticles } from "@/lib/api";
import type { ArticleSummary, PageResponse } from "@/lib/types";

export const metadata: Metadata = {
  title: "星盘检索",
  description: "搜索 Xinyu·Aletheia 藏书阁中的文章、分类和标签。",
};

type SearchParams = Promise<{ q?: QueryValue; page?: QueryValue }>;

export default async function SearchPage({ searchParams }: { searchParams: SearchParams }) {
  const values = await searchParams;
  const query = (firstValue(values.q) ?? "").trim();
  const page = normalizePage(values.page);
  const invalid = query.length > 100;

  let content;
  if (!query) {
    content = (
      <div className="search-prompt">
        <Search aria-hidden="true" size={34} />
        <h2>转动星盘，寻找线索</h2>
        <p>输入文章标题、摘要、分类名或标签名。空关键词不会请求内容服务。</p>
      </div>
    );
  } else if (invalid) {
    content = (
      <div className="validation-message" role="alert">
        <h2>关键词太长了</h2>
        <p>请将检索词控制在 100 个字符以内。</p>
      </div>
    );
  } else {
    let result: PageResponse<ArticleSummary> | null = null;
    let failure: unknown = null;
    try {
      result = await searchArticles(query, page);
    } catch (error) {
      failure = error;
    }

    if (failure || !result) {
      content = <ApiFailure error={failure} />;
    } else {
      if (result.totalPages > 0 && page > result.totalPages) {
        redirect(pageHref("/search", result.totalPages, { q: query }));
      }
      content = (
        <ArticleList
          result={result}
          pathname="/search"
          query={{ q: query }}
          emptyTitle="星盘没有发现记录"
          emptyDescription="换一个关键词，或减少限定词后再试试。"
        />
      );
    }
  }

  return (
    <SiteShell
      eyebrow="星盘检索 · Search"
      title="寻找知识坐标"
      description={query && !invalid ? `正在检索“${query}”` : "用关键词定位藏书阁中的内容。"}
      dialogue="给我一个关键词。标题、摘要、分类和标签留下的星光，我都能看见。"
    >
      <form className="search-panel" action="/search" method="get" role="search">
        <label htmlFor="search-query">检索关键词</label>
        <div>
          <input
            id="search-query"
            name="q"
            defaultValue={query}
            maxLength={101}
            aria-invalid={invalid}
            placeholder="例如：Next.js、Spring Boot、架构…"
          />
          <button type="submit">
            <Search aria-hidden="true" size={17} />
            开始检索
          </button>
        </div>
      </form>
      {content}
    </SiteShell>
  );
}
