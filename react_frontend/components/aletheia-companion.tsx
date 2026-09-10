"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowRight, ChevronDown, MessageCircle, X } from "lucide-react";
import { useEffect, useId, useRef, useState, useSyncExternalStore } from "react";

const COMPACT_QUERY = "(max-width: 1023px), (max-height: 559px)";
// Non-generative matte: preserved costume interiors and soft, decontaminated edges.
const GUIDE_IMAGE = "/images/aletheia/aletheia-guide-front-matted-v4.png";
function subscribe(callback: () => void) {
  const query = window.matchMedia(COMPACT_QUERY);
  query.addEventListener("change", callback);
  return () => query.removeEventListener("change", callback);
}

interface CompanionProps {
  dialogue: string;
  variant?: "home" | "content";
  action?: { href: string; label: string };
  hint?: string;
}

export function AletheiaCompanion({ dialogue, variant = "content", action, hint }: CompanionProps) {
  const compact = useSyncExternalStore(subscribe, () => window.matchMedia(COMPACT_QUERY).matches, () => false);
  const [expanded, setExpanded] = useState(false);
  const [hidden, setHidden] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(84);
  const trigger = useRef<HTMLButtonElement>(null);
  const restore = useRef<HTMLButtonElement>(null);
  const close = useRef<HTMLButtonElement>(null);
  const companion = useRef<HTMLDivElement>(null);
  const id = useId();

  useEffect(() => {
    if (variant !== "content") return;
    const header = document.querySelector(".site-header");
    if (!header) return;
    const observer = new ResizeObserver(([entry]) => setHeaderHeight(entry.target.getBoundingClientRect().height));
    observer.observe(header);
    return () => observer.disconnect();
  }, [variant]);

  useEffect(() => {
    if (!expanded || !compact) return;
    close.current?.focus();
    const escape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setExpanded(false);
        trigger.current?.focus();
      }
    };
    window.addEventListener("keydown", escape);
    return () => window.removeEventListener("keydown", escape);
  }, [expanded, compact]);

  useEffect(() => {
    const root = companion.current;
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const finePointer = window.matchMedia("(pointer: fine)");
    if (!root || compact || reducedMotion.matches || !finePointer.matches) return;

    let frame = 0;
    const setOffset = (x: number, y: number) => {
      window.cancelAnimationFrame(frame);
      frame = window.requestAnimationFrame(() => {
        root.style.setProperty("--companion-look-x", `${x}px`);
        root.style.setProperty("--companion-look-y", `${y}px`);
      });
    };
    const followPointer = (event: PointerEvent) => {
      setOffset((event.clientX / window.innerWidth - 0.5) * 10, (event.clientY / window.innerHeight - 0.5) * 6);
    };
    const resetPointer = () => setOffset(0, 0);
    window.addEventListener("pointermove", followPointer, { passive: true });
    document.documentElement.addEventListener("pointerleave", resetPointer);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener("pointermove", followPointer);
      document.documentElement.removeEventListener("pointerleave", resetPointer);
    };
  }, [compact]);

  const collapse = () => {
    setExpanded(false);
    trigger.current?.focus();
  };

  return (
    <div ref={companion} className={`companion companion--${variant}`} style={{ "--companion-header": `${variant === "home" ? 0 : headerHeight}px` } as React.CSSProperties}>
      <aside id={id} className={`companion__panel${expanded && !hidden ? " is-expanded" : ""}`} aria-label="Aletheia 的引导">
        <button ref={close} type="button" className="companion__close" onClick={collapse} aria-label="关闭人物对话"><X size={18} /></button>
        <div className="companion__portrait" aria-hidden="true">
          <div className="companion__motion">
            <Image src={GUIDE_IMAGE} alt="" width={1024} height={1536} sizes="(max-width: 1023px) 320px, 400px" priority />
          </div>
        </div>
        <div className="companion__dialogue" aria-live="polite">
          <span className="companion__name">Aletheia</span>
          <p>{dialogue}</p>
          {action ? <Link href={action.href} className="dialogue-action">{action.label}<ArrowRight aria-hidden="true" size={16} /></Link> : hint ? <small>{hint}</small> : null}
        </div>
      </aside>
      <div className="companion__launcher">
        {hidden ? (
          <button ref={restore} type="button" className="companion__restore" aria-label="恢复 Aletheia 头像" onClick={() => {
            setHidden(false);
            requestAnimationFrame(() => trigger.current?.focus());
          }}><MessageCircle size={16} aria-hidden="true" /></button>
        ) : (
          <>
            <button ref={trigger} type="button" className="companion__avatar" aria-label="与 Aletheia 对话" aria-expanded={expanded} aria-controls={id} onClick={() => setExpanded(!expanded)}>
              <Image src={GUIDE_IMAGE} alt="" width={1024} height={1536} sizes="160px" />
            </button>
            <button type="button" className="companion__hide" aria-label="收起 Aletheia 头像" onClick={() => {
              setExpanded(false);
              setHidden(true);
              requestAnimationFrame(() => restore.current?.focus());
            }}><ChevronDown aria-hidden="true" size={16} /></button>
          </>
        )}
      </div>
    </div>
  );
}
