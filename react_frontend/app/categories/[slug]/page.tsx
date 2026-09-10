import type { Metadata } from "next";
import { notFound, redirect } from "next/navigation";

import { ApiFailure } from "@/components/api-failure";
import { ArticleList } from "@/components/article-list";
import { SiteShell } from "@/components/site-shell";
import { getArticles, getCategories } from "@/lib/api";
import { normalizePage, pageHref, type QueryValue } from "@/lib/navigation";
import type { ArticleSummary, PageResponse } from "@/lib/types";

type Params = Promise<{ slug: string }>;
type SearchParams = Promise<{ page?: QueryValue }>;

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { slug } = await params;
  let category;
  try {
    category = (await getCategories()).find((item) => item.slug === slug);
  } catch {
    return { title: "分类卷轴" };
  }
  if (!category) notFound();
  return { title: category.name, description: category.description || `浏览 ${category.name} 分类文章` };
}

export default async function CategoryPage({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: SearchParams;
}) {
  const { slug } = await params;
  const page = normalizePage((await searchParams).page);

  let category;
  try {
    category = (await getCategories()).find((item) => item.slug === slug);
  } catch (error) {
    return (
      <SiteShell title="分类卷轴" dialogue="这张卷轴暂时无法显形，请再试一次。">
        <ApiFailure error={error} />
      </SiteShell>
    );
  }
  if (!category) notFound();

  let result: PageResponse<ArticleSummary> | null = null;
  let failure: unknown = null;
  try {
    result = await getArticles({ page, categoryId: category.id });
  } catch (error) {
    failure = error;
  }

  if (failure || !result) {
    return (
      <SiteShell title={category.name} dialogue="卷轴展开到一半停住了，重新尝试即可。">
        <ApiFailure error={failure} />
      </SiteShell>
    );
  }

  if (result.totalPages > 0 && page > result.totalPages) {
    redirect(pageHref(`/categories/${category.slug}`, result.totalPages));
  }

  return (
    <SiteShell
      eyebrow="分类卷轴"
      title={category.name}
      description={category.description || "沿着这卷分类继续阅读。"}
      dialogue={`“${category.name}”的卷轴已经展开，相关技能书都在这里。`}
    >
      <ArticleList
        result={result}
        pathname={`/categories/${category.slug}`}
        emptyTitle="卷轴内还没有文章"
        emptyDescription="这个分类已经建立，但尚未收录公开内容。"
      />
    </SiteShell>
  );
}
