# Xinyu·Aletheia

第四阶段技术博客前台，基于 Next.js 16、React 19、TypeScript、Tailwind CSS 4 与安全 Markdown 渲染构建。

## 本地运行

项目要求 Node.js 24.19.0。复制 `.env.example` 为 `.env.local`，并让 `API_BASE_URL` 指向第三阶段 Spring Boot 服务：

```bash
npm install
npm run dev
```

默认前台地址为 `http://127.0.0.1:3000`，默认 API 地址为 `http://127.0.0.1:8080`。API 不可用时页面会显示真实错误，不会回退到模拟内容。

## 验证

```bash
npm run lint
npm run build
```

本阶段仅提供本地仓库与预览，不包含部署配置、评论、认证或后台管理功能。
