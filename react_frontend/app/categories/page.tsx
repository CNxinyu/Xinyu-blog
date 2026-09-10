import type { Metadata } from "next";
import Link from "next/link";
import { ScrollText } from "lucide-react";

import { ApiFailure } from "@/components/api-failure";
import { EmptyState } from "@/components/empty-state";
import { SiteShell } from "@/components/site-shell";
import { getCategories } from "@/lib/api";
import type { Category } from "@/lib/types";

export const metadata: Metadata = {
  title: "分类卷轴",
  description: "按主题浏览 Aletheia 的技术文章。",
};

export default async function CategoriesPage() {
  let categories: Category[] | null = null;
  let failure: unknown = null;
  try {
    categories = await getCategories();
  } catch (error) {
    failure = error;
  }

  if (failure || !categories) {
    return (
      <SiteShell title="知识谱系" dialogue="卷轴上的文字暂时隐去了，让我重新点亮它。">
        <ApiFailure error={failure} />
      </SiteShell>
    );
  }

  return (
    <SiteShell
      eyebrow="分类卷轴 · Scrolls"
      title="知识谱系"
      description="每卷分类都沿着一条清晰的知识脉络展开。"
      dialogue="分类是地图，不是围墙。挑一张卷轴，我们沿着它继续深入。"
    >
      {categories.length ? (
        <div className="taxonomy-grid">
          {categories.map((category) => (
            <Link className="taxonomy-card" href={`/categories/${category.slug}`} key={category.id}>
              <ScrollText aria-hidden="true" size={24} />
              <div>
                <h2>{category.name}</h2>
                <p>{category.description?.trim() || "这卷分类尚未写下引言。"}</p>
              </div>
              <span>展开卷轴 →</span>
            </Link>
          ))}
        </div>
      ) : (
        <EmptyState title="还没有分类卷轴" description="分类建立后，它们会出现在这面卷轴墙上。" />
      )}
    </SiteShell>
  );
}
