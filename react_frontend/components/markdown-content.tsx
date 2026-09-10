import type { ComponentPropsWithoutRef, ReactNode } from "react";
import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";

import { CodeBlock } from "@/components/code-block";

function plainText(children: ReactNode): string {
  if (typeof children === "string" || typeof children === "number") return String(children);
  if (Array.isArray(children)) return children.map(plainText).join("");
  return "";
}

function headingId(children: ReactNode) {
  return plainText(children)
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, "-")
    .replace(/^-|-$/g, "");
}

function heading(level: 1 | 2 | 3 | 4) {
  const Heading = `h${level}` as const;
  return function MarkdownHeading({ children, ...props }: ComponentPropsWithoutRef<typeof Heading>) {
    const id = headingId(children);
    return (
      <Heading id={id} {...props}>
        <a href={`#${id}`}>{children}</a>
      </Heading>
    );
  };
}

export function MarkdownContent({ markdown }: { markdown: string }) {
  return (
    <article className="markdown-body">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        skipHtml
        components={{
          h1: heading(1),
          h2: heading(2),
          h3: heading(3),
          h4: heading(4),
          a({ href = "", children, ...props }) {
            const external = /^https?:\/\//i.test(href);
            return (
              <a
                href={href}
                {...props}
                target={external ? "_blank" : undefined}
                rel={external ? "noreferrer noopener" : undefined}
              >
                {children}
              </a>
            );
          },
          pre({ children }) {
            return <CodeBlock>{children}</CodeBlock>;
          },
          table({ children, ...props }) {
            return (
              <div className="markdown-table">
                <table {...props}>{children}</table>
              </div>
            );
          },
          img({ alt = "", ...props }) {
            // Markdown images have arbitrary remote dimensions, so a native image is the correct renderer here.
            // eslint-disable-next-line @next/next/no-img-element
            return <img alt={alt} loading="lazy" {...props} />;
          },
        }}
      >
        {markdown}
      </ReactMarkdown>
    </article>
  );
}
