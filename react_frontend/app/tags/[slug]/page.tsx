import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";

import { ApiFailure } from "@/components/api-failure";
import { ArticleList } from "@/components/article-list";
import { SiteShell } from "@/components/site-shell";
import { getArticles, getTags } from "@/lib/api";
import { normalizePage, pageHref, type QueryValue } from "@/lib/navigation";
import type { ArticleSummary, PageResponse } from "@/lib/types";

type Params = Promise<{ slug: string }>;
type SearchParams = Promise<{ page?: QueryValue }>;

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { slug } = await params;
  let tag;
  try {
    tag = (await getTags()).find((item) => item.slug === slug);
  } catch {
    return { title: "符文标签" };
  }
  if (!tag) notFound();
  return { title: `#${tag.name}`, description: `浏览带有 ${tag.name} 标签的文章` };
}

export default async function TagPage({ params, searchParams }: { params: Params; searchParams: SearchParams }) {
  const { slug } = await params;
  const page = normalizePage((await searchParams).page);

  let tag;
  try {
    tag = (await getTags()).find((item) => item.slug === slug);
  } catch (error) {
    return (
      <SiteShell title="符文标签" dialogue="这枚符文暂时无法辨认，请再试一次。">
        <ApiFailure error={error} />
      </SiteShell>
    );
  }
  if (!tag) notFound();

  let result: PageResponse<ArticleSummary> | null = null;
  let failure: unknown = null;
  try {
    result = await getArticles({ page, tagId: tag.id });
  } catch (error) {
    failure = error;
  }

  if (failure || !result) {
    return (
      <SiteShell title={`#${tag.name}`} dialogue="符文的光芒突然变弱了，重新尝试即可。">
        <ApiFailure error={failure} />
      </SiteShell>
    );
  }

  if (result.totalPages > 0 && page > result.totalPages) {
    redirect(pageHref(`/tags/${tag.slug}`, result.totalPages));
  }

  return (
    <SiteShell
      eyebrow="符文标签"
      title={`#${tag.name}`}
      description={`所有被“${tag.name}”符文标记的公开技能书。`}
      dialogue={`符文“${tag.name}”已经响应，顺着光痕就能找到相关记录。`}
    >
      <ArticleList
        result={result}
        pathname={`/tags/${tag.slug}`}
        emptyTitle="符文没有找到共鸣"
        emptyDescription="这个标签已经存在，但尚未关联公开文章。"
      />
    </SiteShell>
  );
}
