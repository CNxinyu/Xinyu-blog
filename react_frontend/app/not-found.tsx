import Link from "next/link";
import { Compass, Home } from "lucide-react";

export default function NotFound() {
  return (
    <div className="state-page">
      <Compass aria-hidden="true" size={44} />
      <span>404 · LOST SCROLL</span>
      <h1>这页卷轴并不存在</h1>
      <p>它可能已被收起、改名，或从未进入过藏书阁。</p>
      <Link className="primary-action" href="/">
        <Home aria-hidden="true" size={16} />
        返回工坊
      </Link>
    </div>
  );
}
