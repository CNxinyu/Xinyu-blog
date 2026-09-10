import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { ApiFailure } from "@/components/api-failure";
import { ArticleList } from "@/components/article-list";
import { SiteShell } from "@/components/site-shell";
import { getArticles } from "@/lib/api";
import { normalizePage, pageHref, type QueryValue } from "@/lib/navigation";
import type { ArticleSummary, PageResponse } from "@/lib/types";

export const metadata: Metadata = {
  title: "技能书",
  description: "浏览 Aletheia 收藏的技术文章与实践手记。",
};

export default async function ArticlesPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: QueryValue }>;
}) {
  const page = normalizePage((await searchParams).page);
  let result: PageResponse<ArticleSummary> | null = null;
  let failure: unknown = null;

  try {
    result = await getArticles({ page });
  } catch (error) {
    failure = error;
  }

  if (failure || !result) {
    return (
      <SiteShell title="公开藏书" dialogue="书架的魔法回路似乎断开了，我正在重新校准。">
        <ApiFailure error={failure} />
      </SiteShell>
    );
  }

  if (result.totalPages > 0 && page > result.totalPages) {
    redirect(pageHref("/articles", result.totalPages));
  }

  return (
    <SiteShell
      eyebrow="技能书 · Grimoire"
      title="公开藏书"
      description="代码、工程实践与沿途发现，被整理成可以反复翻阅的技能书。"
      dialogue="每本书都封存着一次求真的实验。先从最吸引你的标题开始吧。"
    >
      <ArticleList result={result} pathname="/articles" />
    </SiteShell>
  );
}
