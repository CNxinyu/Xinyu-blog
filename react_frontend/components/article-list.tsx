import { ArticleCard } from "@/components/article-card";
import { EmptyState } from "@/components/empty-state";
import { Pagination } from "@/components/pagination";
import type { PageResponse, ArticleSummary } from "@/lib/types";

interface ArticleListProps {
  result: PageResponse<ArticleSummary>;
  pathname: string;
  query?: Record<string, string>;
  emptyTitle?: string;
  emptyDescription?: string;
}

export function ArticleList({
  result,
  pathname,
  query,
  emptyTitle = "卷轴架暂时空着",
  emptyDescription = "这里还没有公开的文章，过些时候再来看看吧。",
}: ArticleListProps) {
  if (result.items.length === 0) {
    return <EmptyState title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <>
      <div className="article-grid">
        {result.items.map((article) => (
          <ArticleCard article={article} key={article.id} />
        ))}
      </div>
      <Pagination
        currentPage={result.page}
        totalPages={result.totalPages}
        pathname={pathname}
        query={query}
      />
    </>
  );
}
