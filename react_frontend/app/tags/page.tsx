import type { Metadata } from "next";
import Link from "next/link";
import type { CSSProperties } from "react";
import { Sparkles } from "lucide-react";

import { ApiFailure } from "@/components/api-failure";
import { EmptyState } from "@/components/empty-state";
import { SiteShell } from "@/components/site-shell";
import { getTags } from "@/lib/api";
import type { Tag } from "@/lib/types";

export const metadata: Metadata = {
  title: "符文标签",
  description: "通过标签寻找 Xinyu·Aletheia 藏书阁中的交叉主题。",
};

export default async function TagsPage() {
  let tags: Tag[] | null = null;
  let failure: unknown = null;
  try {
    tags = await getTags();
  } catch (error) {
    failure = error;
  }

  if (failure || !tags) {
    return (
      <SiteShell title="交叉索引" dialogue="符文阵暂时暗了，重新注入一点魔力吧。">
        <ApiFailure error={failure} />
      </SiteShell>
    );
  }

  return (
    <SiteShell
      eyebrow="符文标签 · Runes"
      title="交叉索引"
      description="标签像微小符文，把不同书架上的同类线索彼此连结。"
      dialogue="符文不在意书被放在哪一层，它只会把相似的思想召集到一起。"
    >
      {tags.length ? (
        <div className="tag-cloud">
          {tags.map((tag, index) => (
            <Link
              href={`/tags/${tag.slug}`}
              key={tag.id}
              style={{ "--tag-delay": `${(index % 12) * 35}ms` } as CSSProperties}
            >
              <Sparkles aria-hidden="true" size={15} />
              {tag.name}
            </Link>
          ))}
        </div>
      ) : (
        <EmptyState title="还没有符文标签" description="标签出现后，它们会像星群一样聚集在这里。" />
      )}
    </SiteShell>
  );
}
