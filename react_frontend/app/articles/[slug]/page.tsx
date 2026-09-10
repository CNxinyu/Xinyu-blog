import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, CalendarDays, Feather } from "lucide-react";

import { ApiFailure } from "@/components/api-failure";
import { MarkdownContent } from "@/components/markdown-content";
import { SiteShell } from "@/components/site-shell";
import { ApiError, getArticle } from "@/lib/api";
import { formatDate } from "@/lib/navigation";

type ArticleParams = Promise<{ slug: string }>;

export async function generateMetadata({ params }: { params: ArticleParams }): Promise<Metadata> {
  const { slug } = await params;
  try {
    const article = await getArticle(slug);
    return {
      title: article.title,
      description: article.summary || `阅读《${article.title}》`,
    };
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    return { title: "未找到技能书" };
  }
}

export default async function ArticleDetailPage({ params }: { params: ArticleParams }) {
  const { slug } = await params;
  let article;

  try {
    article = await getArticle(slug);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) notFound();
    return (
      <SiteShell title="读取技能书" dialogue="封印没有松动，再试一次，或许只是星轨短暂偏移。">
        <ApiFailure error={error} />
      </SiteShell>
    );
  }

  return (
    <SiteShell
      eyebrow={article.category?.name ?? "技能书"}
      title={article.title}
      description={article.summary || undefined}
      dialogue="正文已经展开。慢慢读，真正有用的咒文从不怕被反复推敲。"
      compact
    >
      <div className="article-detail__meta">
        <span>
          <Feather aria-hidden="true" size={15} />
          {article.author?.nickname || article.author?.username || "匿名抄写员"}
        </span>
        <time dateTime={article.publishedAt ?? undefined}>
          <CalendarDays aria-hidden="true" size={15} />
          {formatDate(article.publishedAt)}
        </time>
      </div>

      <div className="article-detail__links">
        {article.category ? (
          <Link href={`/categories/${article.category.slug}`}>分类：{article.category.name}</Link>
        ) : null}
        {article.tags.map((tag) => (
          <Link href={`/tags/${tag.slug}`} key={tag.id}>
            #{tag.name}
          </Link>
        ))}
      </div>

      <MarkdownContent markdown={article.contentMarkdown} />

      <Link className="back-link" href="/articles">
        <ArrowLeft aria-hidden="true" size={16} />
        返回全部技能书
      </Link>
    </SiteShell>
  );
}
