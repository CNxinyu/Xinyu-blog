import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";

import { pageHref } from "@/lib/navigation";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  pathname: string;
  query?: Record<string, string>;
}

export function Pagination({ currentPage, totalPages, pathname, query }: PaginationProps) {
  if (totalPages <= 1) return null;

  const candidates = [1, currentPage - 1, currentPage, currentPage + 1, totalPages]
    .filter((page) => page >= 1 && page <= totalPages)
    .filter((page, index, pages) => pages.indexOf(page) === index)
    .sort((a, b) => a - b);

  return (
    <nav className="pagination" aria-label="文章分页">
      {currentPage > 1 ? (
        <Link href={pageHref(pathname, currentPage - 1, query)} aria-label="上一页">
          <ChevronLeft aria-hidden="true" size={17} />
        </Link>
      ) : (
        <span aria-hidden="true" className="pagination__disabled">
          <ChevronLeft size={17} />
        </span>
      )}

      {candidates.map((page, index) => (
        <span className="pagination__item" key={page}>
          {index > 0 && page - candidates[index - 1] > 1 ? (
            <span className="pagination__ellipsis" aria-hidden="true">
              …
            </span>
          ) : null}
          <Link
            href={pageHref(pathname, page, query)}
            aria-current={page === currentPage ? "page" : undefined}
          >
            {page}
          </Link>
        </span>
      ))}

      {currentPage < totalPages ? (
        <Link href={pageHref(pathname, currentPage + 1, query)} aria-label="下一页">
          <ChevronRight aria-hidden="true" size={17} />
        </Link>
      ) : (
        <span aria-hidden="true" className="pagination__disabled">
          <ChevronRight size={17} />
        </span>
      )}
    </nav>
  );
}
