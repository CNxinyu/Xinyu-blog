import type { Metadata } from "next";
import "highlight.js/styles/github-dark.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Xinyu·Aletheia",
    template: "%s｜Xinyu·Aletheia",
  },
  description: "Xinyu 与 Aletheia 共建的异世界个人工坊，收录知识、项目与实用工具。",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN" className="h-full antialiased">
      <body className="min-h-full">{children}</body>
    </html>
  );
}
