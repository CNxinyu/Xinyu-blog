"use client";

import { isValidElement, type ReactNode, useState } from "react";
import { Check, Copy } from "lucide-react";

function nodeText(node: ReactNode): string {
  if (typeof node === "string" || typeof node === "number") return String(node);
  if (Array.isArray(node)) return node.map(nodeText).join("");
  if (isValidElement<{ children?: ReactNode }>(node)) return nodeText(node.props.children);
  return "";
}

function languageOf(node: ReactNode): string {
  if (Array.isArray(node)) return node.map(languageOf).find(Boolean) ?? "code";
  if (isValidElement<{ className?: string; children?: ReactNode }>(node)) {
    const match = node.props.className?.match(/language-([\w-]+)/);
    return match?.[1] ?? languageOf(node.props.children);
  }
  return "";
}

export function CodeBlock({ children }: { children?: ReactNode }) {
  const [copied, setCopied] = useState(false);
  const language = languageOf(children) || "code";

  async function copyCode() {
    await navigator.clipboard.writeText(nodeText(children).replace(/\n$/, ""));
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <div className="code-block">
      <div className="code-block__bar">
        <span>{language}</span>
        <button type="button" onClick={copyCode} aria-label="复制代码">
          {copied ? <Check aria-hidden="true" size={14} /> : <Copy aria-hidden="true" size={14} />}
          {copied ? "已复制" : "复制"}
        </button>
      </div>
      <pre>{children}</pre>
    </div>
  );
}
