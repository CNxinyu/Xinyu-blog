"use client";

import { useRouter } from "next/navigation";
import { RefreshCw } from "lucide-react";

export function RetryButton() {
  const router = useRouter();
  return (
    <button className="primary-action" type="button" onClick={() => router.refresh()}>
      <RefreshCw aria-hidden="true" size={16} />
      重新尝试
    </button>
  );
}
