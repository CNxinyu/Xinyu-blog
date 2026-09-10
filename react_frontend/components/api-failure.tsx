import { TriangleAlert } from "lucide-react";

import { RetryButton } from "@/components/retry-button";
import { ApiError } from "@/lib/api";

export function ApiFailure({ error }: { error: unknown }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <div className="api-failure" role="alert">
      <TriangleAlert aria-hidden="true" size={34} />
      <div>
        <h2>魔法回路暂时中断</h2>
        <p>{apiError?.message ?? "内容服务暂时无法响应，请稍后重试。"}</p>
        {apiError?.traceId ? (
          <p className="api-failure__trace">
            TraceId: <code>{apiError.traceId}</code>
          </p>
        ) : null}
        <RetryButton />
      </div>
    </div>
  );
}
