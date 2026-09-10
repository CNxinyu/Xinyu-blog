"use client";

import { useEffect } from "react";
import { RefreshCw, TriangleAlert } from "lucide-react";

export default function ErrorPage({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="state-page">
      <TriangleAlert aria-hidden="true" size={40} />
      <span>Xinyu·Aletheia</span>
      <h1>魔法回路出现了意外</h1>
      <p>页面没有正确展开。你可以重新尝试，未完成的检索条件会被保留。</p>
      <button className="primary-action" type="button" onClick={reset}>
        <RefreshCw aria-hidden="true" size={16} />
        重新尝试
      </button>
    </div>
  );
}
