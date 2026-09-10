import Link from "next/link";
import { CalendarDays, Feather } from "lucide-react";

import { formatDate } from "@/lib/navigation";
import type { ArticleSummary } from "@/lib/types";

export function ArticleCard({ article }: { article: ArticleSummary }) {
  return (
    <article className="article-card">
      <div className="article-card__ornament" aria-hidden="true" />
      <div className="article-card__taxonomy">
        {article.category ? (
          <Link href={`/categories/${article.category.slug}`}>{article.category.name}</Link>
        ) : (
          <span>未分类卷轴</span>
        )}
      </div>
      <h2>
        <Link href={`/articles/${article.slug}`}>{article.title}</Link>
      </h2>
      <p className="article-card__summary">
        {article.summary?.trim() || "这页羊皮纸尚未留下摘要，翻开正文寻找答案吧。"}
      </p>
      <div className="article-card__tags" aria-label="文章标签">
        {article.tags.slice(0, 4).map((tag) => (
          <Link href={`/tags/${tag.slug}`} key={tag.id}>
            #{tag.name}
          </Link>
        ))}
      </div>
      <footer className="article-card__meta">
        <span>
          <Feather aria-hidden="true" size={14} />
          {article.author?.nickname || article.author?.username || "匿名抄写员"}
        </span>
        <time dateTime={article.publishedAt ?? undefined}>
          <CalendarDays aria-hidden="true" size={14} />
          {formatDate(article.publishedAt)}
        </time>
      </footer>
    </article>
  );
}
