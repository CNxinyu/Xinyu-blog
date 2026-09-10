// Local-only visual regression. Requires an existing production build and Playwright.
// PLAYWRIGHT_MODULE may point to a bundled installation; no backend/database is started.
import assert from "node:assert/strict";
import { createServer } from "node:http";
import { spawn } from "node:child_process";
import { createRequire } from "node:module";
import { once } from "node:events";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const output = process.env.VISUAL_OUTPUT || path.join(root, ".next", "companion-validation");
const stamp = "2026-09-10T08:00:00Z";
const category = { id: 1, name: "工程实践", slug: "engineering", description: "布局验收", createdAt: stamp, updatedAt: stamp };
const tag = { id: 1, name: "TypeScript", slug: "typescript", createdAt: stamp, updatedAt: stamp };
const articles = Array.from({ length: 9 }, (_, i) => ({
  id: i + 1, slug: `layout-${i + 1}`, title: i === 0 ? "长标题验收：以稳定的视口坐标组织人物、正文和台词，保持阅读空间" : `工坊笔记 ${i + 1}`,
  summary: i === 1 ? null : "用于响应式布局的临时只读测试数据，不进入正式 API 或数据库。", status: "PUBLISHED", category, tags: [tag],
  author: { id: 1, username: "tester", nickname: "测试作者", avatarUrl: null }, publishedAt: stamp, createdAt: stamp, updatedAt: stamp,
}));
const markdown = "## 表格与代码\n\n| 名称 | 说明 |\n| --- | --- |\n| Aletheia | 固定伴随区 |\n\n```typescript\nconst greeting: string = 'Aletheia';\n```\n\n" + Array.from({ length: 35 }, (_, i) => `## 阅读段落 ${i + 1}\n\n正文滚动时，人物与台词应保持在相同的视口坐标。帽子、脸部和肩部可见，正文不被遮挡。\n\n`).join("");
let empty = false;
const mock = createServer((req, res) => {
  const url = new URL(req.url, "http://127.0.0.1");
  if (req.method !== "GET") { res.writeHead(405).end(); return; }
  let data;
  if (url.pathname === "/api/articles" || url.pathname === "/api/search") data = { items: empty ? [] : articles, page: 1, size: 9, total: empty ? 0 : 18, totalPages: empty ? 0 : 2 };
  else if (url.pathname === "/api/articles/layout-1") data = { ...articles[0], contentMarkdown: markdown, contentHtml: "", archivedAt: null };
  else if (url.pathname === "/api/categories") data = [category];
  else if (url.pathname === "/api/tags") data = [tag];
  else { res.writeHead(404).end(); return; }
  res.writeHead(200, { "Content-Type": "application/json" }).end(JSON.stringify({ code: 0, message: "OK", data, traceId: "layout-test" }));
});
await mkdir(output, { recursive: true });
mock.listen(0, "127.0.0.1");
await once(mock, "listening");
// Let the OS choose a free local preview port, without touching the user's dev server.
const reserve = createServer();
reserve.listen(0, "127.0.0.1");
await once(reserve, "listening");
const port = reserve.address().port;
await new Promise(resolve => reserve.close(resolve));
const preview = spawn(process.execPath, [path.join(root, "node_modules/next/dist/bin/next"), "start", "-H", "127.0.0.1", "-p", String(port)], {
  cwd: root, windowsHide: true, env: { ...process.env, API_BASE_URL: `http://127.0.0.1:${mock.address().port}` }, stdio: ["ignore", "pipe", "pipe"],
});
let logs = "";
preview.stdout.on("data", data => { logs += data; });
preview.stderr.on("data", data => { logs += data; });
const base = `http://127.0.0.1:${port}`;
const checks = [];
let browser;
try {
  for (let n = 0; n < 100; n++) {
    try { if ((await fetch(base)).ok) break; } catch { /* wait for this preview only */ }
    if (n === 99) throw new Error(`Preview failed: ${logs}`);
    await new Promise(resolve => setTimeout(resolve, 200));
  }
  browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || "msedge" });
  const context = await browser.newContext();
  await context.addInitScript(() => sessionStorage.setItem("xinyu-aletheia-intro-seen", "1"));
  const page = await context.newPage();
  const errors = [];
  page.on("pageerror", error => errors.push(error.message));
  const sizes = [[1920,1080], [1366,768], [1280,600], [1024,768], [390,844], [360,640], [844,390], [1023,768], [1024,559], [1024,560], [1280,720], [960,540]];
  for (const [width, height] of sizes) {
    await page.setViewportSize({ width, height });
    for (const route of ["/", "/articles", "/articles/layout-1", "/search"]) {
      await page.goto(base + route);
      await page.locator(".companion").waitFor();
      await page.waitForFunction(() => [...document.querySelectorAll(".companion img")].filter(img => img.getClientRects().length).every(img => img.complete && img.naturalWidth > 0));
      await page.waitForTimeout(120);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `horizontal overflow: ${width} ${route}`);
      const desktop = width >= 1024 && height >= 560;
      const panel = page.locator(".companion__panel");
      if (desktop) {
        const before = await panel.boundingBox();
        assert(before && before.y >= 0 && before.y + before.height <= height + 1);
        const metrics = await page.evaluate(() => {
          const image = document.querySelector(".companion__portrait img").getBoundingClientRect();
          const portrait = document.querySelector(".companion__portrait").getBoundingClientRect();
          const main = document.querySelector(".site-main, .home-directory").getBoundingClientRect();
          const panel = document.querySelector(".companion__panel").getBoundingClientRect();
          const dialogue = document.querySelector(".companion__dialogue").getBoundingClientRect();
          // Current source face occupies y≈12%-19%; head/shoulders through 30%.
          return {
            headInside: image.y >= portrait.y && image.y + image.height * .3 <= portrait.bottom,
            clear: main.right + 8 <= portrait.left,
            dialogueWide: dialogue.width >= innerWidth * .8,
            dialogueAnchored: dialogue.bottom <= innerHeight && dialogue.bottom >= innerHeight - 24,
            portraitAboveDialogue: portrait.bottom <= dialogue.top,
            panelInViewport: panel.top >= 0 && panel.bottom <= innerHeight,
          };
        });
        assert(Object.values(metrics).every(Boolean), `portrait framing, dialogue, or content overlap: ${width} ${height} ${route}`);
        for (const fraction of [.5, 1]) {
          await page.evaluate(f => window.scrollTo(0, (document.documentElement.scrollHeight - innerHeight) * f), fraction);
          await page.waitForTimeout(80);
          const after = await panel.boundingBox();
          assert(Math.abs(before.y - after.y) < 1 && Math.abs(before.x - after.x) < 1, `anchor moved: ${width} ${route}`);
          if (route !== "/") assert(Math.abs((await page.locator(".site-header").boundingBox()).y) < 1, "header must remain sticky");
        }
      } else {
        assert(!(await panel.isVisible()));
        const avatar = page.getByRole("button", { name: "与 Aletheia 对话" });
        const avatarStart = await avatar.boundingBox();
        await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
        const avatarEnd = await avatar.boundingBox();
        assert(Math.abs(avatarStart.y - avatarEnd.y) < 1, "mobile launcher moved with scroll");
        await avatar.click();
        assert(await panel.isVisible());
        const box = await panel.boundingBox();
        assert(box.width <= 320 && box.x >= 0 && box.y >= 0 && box.y + box.height <= height);
        assert(await panel.evaluate(el => {
          const portrait = el.querySelector(".companion__portrait").getBoundingClientRect();
          const image = el.querySelector("img").getBoundingClientRect();
          const faceY = image.y + image.height * .15;
          const hit = document.elementFromPoint(portrait.x + portrait.width / 2, faceY);
          return faceY >= portrait.top && faceY < portrait.bottom && el.contains(hit);
        }), "expanded face is covered by navigation or clipped");
        if (route === "/search" && [390,844].includes(width)) await page.screenshot({ path: path.join(output, `${width}x${height}-expanded.png`) });
        await page.keyboard.press("Escape");
        assert(!(await panel.isVisible()));
        assert(await avatar.evaluate(el => el === document.activeElement));
        await page.getByRole("button", { name: "收起 Aletheia 头像" }).click();
        assert(!(await avatar.isVisible()));
        await page.getByRole("button", { name: "恢复 Aletheia 头像" }).click();
        assert(await avatar.isVisible());
      }
      await page.evaluate(() => window.scrollTo(0, 0));
      if ([1920,1366,1280,1024,390,360].includes(width)) await page.screenshot({ path: path.join(output, `${width}x${height}-${route.replaceAll("/", "_") || "home"}.png`) });
      checks.push(`${width}x${height} ${route}: framing, scroll, overflow, controls OK`);
    }
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(base);
  await page.getByRole("button", { name: /^技能书/ }).click();
  assert(await page.locator(".home-mobile-dialogue").getByRole("link", { name: "翻开技能书" }).isVisible());
  await page.getByRole("button", { name: /炼金坊/ }).click();
  assert(await page.locator(".future-area").isVisible());
  checks.push("Mobile home menu keeps destination links available while avatar is collapsed");
  empty = true;
  await page.goto(base + "/articles");
  assert(await page.getByText("卷轴架暂时空着").isVisible());
  checks.push("Empty article state OK");
  for (const scale of [1.25, 1.5, 2]) {
    // Browser zoom equivalent: fewer CSS pixels plus a matching device scale.
    const zoomContext = await browser.newContext({ viewport: { width: Math.round(1920 / scale), height: Math.round(1080 / scale) }, deviceScaleFactor: scale });
    const zoomPage = await zoomContext.newPage();
    await zoomPage.goto(base + "/search");
    assert(await zoomPage.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    const box = await zoomPage.locator(scale === 2 ? ".companion__avatar" : ".companion__panel").boundingBox();
    assert(box && box.y >= 0 && box.x >= 0 && box.y + box.height <= Math.round(1080 / scale));
    await zoomContext.close();
    checks.push(`${scale * 100}% zoom equivalent: CSS viewport + device scale OK`);
  }
  for (const reducedMotion of ["reduce", "no-preference"]) {
    const introContext = await browser.newContext({ viewport: { width: 1366, height: 768 }, reducedMotion });
    const intro = await introContext.newPage();
    await intro.goto(base);
    await intro.getByRole("button", { name: "开始旅程", exact: true }).waitFor();
    assert(await intro.locator(".workshop-scene").evaluate(el => el.inert));
    await intro.getByRole("button", { name: "开始旅程", exact: true }).click();
    await intro.locator(".stage-ready").waitFor();
    await intro.reload();
    await intro.locator(".stage-ready").waitFor();
    await intro.evaluate(() => sessionStorage.clear());
    await intro.reload();
    await intro.getByRole("button", { name: "跳过动画" }).click();
    await intro.locator(".stage-ready").waitFor();
    await introContext.close();
    checks.push(`Intro: ${reducedMotion}, entry, session return and skip OK`);
  }
  assert.deepEqual(errors, []);
  console.log(checks.join("\n"));
} finally {
  await writeFile(path.join(output, "results.json"), JSON.stringify({ checks, logs }, null, 2));
  await browser?.close();
  preview.kill();
  await new Promise(resolve => mock.close(resolve));
}
