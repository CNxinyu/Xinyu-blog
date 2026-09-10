"use client";

import Image from "next/image";
import Link from "next/link";
import {
  ArrowRight,
  BookOpenText,
  FlaskConical,
  LibraryBig,
  PackageOpen,
  Search,
  SkipForward,
  Sparkles,
  ScrollText,
  Tags,
} from "lucide-react";
import { useEffect, useState } from "react";
import { AletheiaCompanion } from "./aletheia-companion";

type IntroStage = "checking" | "cover" | "flying" | "revealing" | "ready";
type MenuKey = "articles" | "categories" | "tags" | "search";
type AreaKey = "archive" | "alchemy" | "treasure";

const SESSION_KEY = "xinyu-aletheia-intro-seen";

const areaItems: Record<
  AreaKey,
  {
    title: string;
    note: string;
    status: string;
    line: string;
    icon: typeof LibraryBig;
  }
> = {
  archive: {
    title: "藏书阁",
    note: "知识与技术记录",
    status: "已开放",
    line: "藏书阁收录技术札记、实践心得和开发见闻。选一册技能书，或按分类与符文寻找线索吧。",
    icon: LibraryBig,
  },
  alchemy: {
    title: "炼金坊",
    note: "自研项目与实验产物",
    status: "筹备中",
    line: "炼金坊会陈列自己动手完成的小项目与实验产物。炉火还在调试，准备好后我会打开门。",
    icon: FlaskConical,
  },
  treasure: {
    title: "百宝箱",
    note: "实用工具与精选资源",
    status: "筹备中",
    line: "百宝箱将收纳真正好用的网站、工具和可靠下载地址。现在还在逐件整理与核验。",
    icon: PackageOpen,
  },
};

const menuItems: Record<
  MenuKey,
  {
    title: string;
    note: string;
    line: string;
    href: string;
    action: string;
    icon: typeof BookOpenText;
  }
> = {
  articles: {
    title: "技能书",
    note: "文章与术式札记",
    line: "这是我随手写下的术式与见闻。既然你找到了，就挑一本喜欢的翻开吧。",
    href: "/articles",
    action: "翻开技能书",
    icon: BookOpenText,
  },
  categories: {
    title: "分类卷轴",
    note: "按知识领域查阅",
    line: "每卷书脊上都有不同的徽记。顺着分类找，会比在书堆里迷路轻松一些。",
    href: "/categories",
    action: "查看分类卷轴",
    icon: ScrollText,
  },
  tags: {
    title: "符文标签",
    note: "从主题线索追踪",
    line: "这些符文会把相近的知识串在一起。碰一碰，也许能发现意外的联系。",
    href: "/tags",
    action: "辨认符文标签",
    icon: Tags,
  },
  search: {
    title: "星盘检索",
    note: "搜索藏书阁",
    line: "告诉星盘你在寻找什么。标题、摘要、分类和标签留下的痕迹，它都能看见。",
    href: "/search",
    action: "启动星盘检索",
    icon: Search,
  },
};

export function HomeExperience() {
  const [stage, setStage] = useState<IntroStage>("checking");
  const [area, setArea] = useState<AreaKey>("archive");
  const [selected, setSelected] = useState<MenuKey | null>(null);
  const [reducedMotion, setReducedMotion] = useState(false);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      setReducedMotion(window.matchMedia("(prefers-reduced-motion: reduce)").matches);
      setStage(sessionStorage.getItem(SESSION_KEY) ? "ready" : "cover");
    });
    return () => window.cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    if (stage === "flying") {
      const timer = window.setTimeout(() => setStage("revealing"), reducedMotion ? 0 : 760);
      return () => window.clearTimeout(timer);
    }
    if (stage === "revealing") {
      const timer = window.setTimeout(() => setStage("ready"), reducedMotion ? 0 : 880);
      return () => window.clearTimeout(timer);
    }
  }, [reducedMotion, stage]);

  const enter = () => {
    sessionStorage.setItem(SESSION_KEY, "1");
    setStage("flying");
  };

  const skip = () => {
    sessionStorage.setItem(SESSION_KEY, "1");
    setStage("ready");
  };

  if (stage === "checking") {
    return (
      <main className="intro-checking" aria-label="正在打开藏书阁">
        <Sparkles aria-hidden="true" />
      </main>
    );
  }

  const active = selected ? menuItems[selected] : null;
  const activeArea = areaItems[area];

  return (
    <main className={`home-experience stage-${stage}`}>
      <section className="workshop-scene" aria-label="Aletheia 的魔法工坊" inert={stage !== "ready"}>
        <div className="workshop-backdrop" aria-hidden="true" />
        <div className="ambient-vignette" aria-hidden="true" />
        <div className="magic-dust" aria-hidden="true" />

        <header className="home-brand">
          <span className="brand-kicker">XINYU&apos;S ARCANE WORKSHOP</span>
          <h1>Xinyu·Aletheia</h1>
          <p>藏书阁 · 炼金坊 · 百宝箱</p>
        </header>

        <div className="home-directory">
          <nav className="home-areas" aria-label="主要分区">
            {(Object.entries(areaItems) as [AreaKey, (typeof areaItems)[AreaKey]][]).map(
              ([key, item]) => {
                const Icon = item.icon;
                return (
                  <button
                    key={key}
                    type="button"
                    className={area === key ? "home-area-card is-selected" : "home-area-card"}
                    onClick={() => {
                      setArea(key);
                      setSelected(null);
                    }}
                    aria-pressed={area === key}
                  >
                    <Icon aria-hidden="true" />
                    <span>
                      <strong>{item.title}</strong>
                      <small>{item.note}</small>
                    </span>
                    <em>{item.status}</em>
                  </button>
                );
              },
            )}
          </nav>

          {area === "archive" ? (
            <nav className="home-menu" aria-label="藏书阁子入口">
              {(Object.entries(menuItems) as [MenuKey, (typeof menuItems)[MenuKey]][]).map(
                ([key, item]) => {
                  const Icon = item.icon;
                  return (
                    <button
                      key={key}
                      type="button"
                      className={selected === key ? "home-menu-card is-selected" : "home-menu-card"}
                      onClick={() => setSelected(key)}
                      aria-pressed={selected === key}
                    >
                      <span className="menu-icon"><Icon aria-hidden="true" /></span>
                      <span>
                        <strong>{item.title}</strong>
                        <small>{item.note}</small>
                      </span>
                      <ArrowRight className="menu-arrow" aria-hidden="true" />
                    </button>
                  );
                },
              )}
            </nav>
          ) : (
            <section className="future-area" aria-live="polite">
              <span>{activeArea.status}</span>
              <h2>{activeArea.title}</h2>
              <p>{activeArea.note}</p>
            </section>
          )}
        </div>

        <section className="home-mobile-dialogue" aria-live="polite">
          <span className="dialogue-name">Aletheia</span>
          <div className="dialogue-copy">
            <p>{active?.line ?? activeArea.line}</p>
            {active ? (
              <Link href={active.href} className="dialogue-action">
                {active.action}
                <ArrowRight aria-hidden="true" />
              </Link>
            ) : (
              <span className="dialogue-hint">
                {area === "archive" ? "选择藏书阁中的入口" : "这个分区正在筹备"}
              </span>
            )}
          </div>
        </section>
        <AletheiaCompanion
          variant="home"
          dialogue={active?.line ?? activeArea.line}
          action={active ? { href: active.href, label: active.action } : undefined}
          hint={area === "archive" ? "选择藏书阁中的入口" : "这个分区正在筹备"}
        />
      </section>

      {stage !== "ready" && (
        <section className="cover-scene" aria-label="开始旅程">
          <div className="cover-sky" aria-hidden="true" />
          <div className="cover-stars stars-a" aria-hidden="true" />
          <div className="cover-stars stars-b" aria-hidden="true" />
          <div className="cover-moon" aria-hidden="true" />

          <div className="cover-title">
            <span>在遗忘的星轨尽头</span>
            <h2>Xinyu·Aletheia</h2>
          </div>

          <div className="flying-witch" aria-hidden="true">
            <Image
              src="/images/aletheia/aletheia-flight.png"
              alt=""
              width={1536}
              height={1024}
              priority
            />
          </div>

          <button type="button" className="enter-button" onClick={enter} disabled={stage !== "cover"}>
            <Sparkles aria-hidden="true" />
            开始旅程
          </button>
          <button type="button" className="skip-button" onClick={skip}>
            <SkipForward aria-hidden="true" />
            跳过动画
          </button>
        </section>
      )}
    </main>
  );
}
