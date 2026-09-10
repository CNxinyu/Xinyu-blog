import { AletheiaCompanion } from "./aletheia-companion";
import Link from "next/link";
import type { ReactNode } from "react";
import { BookOpenText, Home, Search, ScrollText, Tags } from "lucide-react";

const navigation = [
  { href: "/", label: "工坊", icon: Home },
  { href: "/articles", label: "技能书", icon: BookOpenText },
  { href: "/categories", label: "分类卷轴", icon: ScrollText },
  { href: "/tags", label: "符文标签", icon: Tags },
];

interface SiteShellProps {
  eyebrow?: string;
  title: string;
  description?: string;
  dialogue: string;
  children: ReactNode;
  compact?: boolean;
}

export function SiteShell({
  eyebrow = "Xinyu·Aletheia · 藏书阁",
  title,
  description,
  dialogue,
  children,
  compact = false,
}: SiteShellProps) {
  return (
    <div className="site-shell">
      <div className="site-shell__backdrop" aria-hidden="true" />
      <header className="site-header">
        <Link className="site-header__brand" href="/" aria-label="返回 Xinyu·Aletheia 首页">
          <span className="site-header__sigil">A</span>
          <span>
            <strong>Xinyu·Aletheia</strong>
            <small>藏书阁</small>
          </span>
        </Link>

        <nav className="site-nav" aria-label="主导航">
          {navigation.map(({ href, label, icon: Icon }) => (
            <Link href={href} key={href} aria-label={label}>
              <Icon aria-hidden="true" size={16} />
              <span>{label}</span>
            </Link>
          ))}
        </nav>

        <form className="header-search" action="/search" method="get" role="search">
          <label className="sr-only" htmlFor="header-query">
            搜索藏书
          </label>
          <input id="header-query" name="q" maxLength={100} placeholder="检索藏书…" />
          <button type="submit" aria-label="搜索">
            <Search aria-hidden="true" size={17} />
          </button>
        </form>
      </header>

      <div className={`site-layout${compact ? " site-layout--compact" : ""}`}>
        <main className="site-main">
          <section className="page-heading">
            <span className="page-heading__eyebrow">{eyebrow}</span>
            <h1>{title}</h1>
            {description ? <p>{description}</p> : null}
          </section>
          {children}
        </main>

        <AletheiaCompanion dialogue={dialogue} />
      </div>

      <footer className="site-footer">
        <span>以知识作星火，以真实为书签。</span>
        <span>© {new Date().getFullYear()} Xinyu·Aletheia</span>
      </footer>
    </div>
  );
}
