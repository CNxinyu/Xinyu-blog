# 人物布局回归

使用 Node 24.19.0，先执行 `npm run build`，然后执行 `node tests/companion-layout.mjs`。
脚本通过 `PLAYWRIGHT_MODULE` 指定已有 Playwright 安装，默认使用本机 Edge；可用 `BROWSER_CHANNEL` 改为 `chrome`。
不自动安装浏览器或项目依赖。

脚本自行启动仅监听 127.0.0.1 随机端口的只读 Mock API 和独立 Next production preview，结束时关闭自己启动的服务。不访问 Java/PostgreSQL，不更改正常开发服务器的 API 配置。
结果与截图默认写入 `.next/companion-validation/`，可通过 `VISUAL_OUTPUT` 指定另一个目录。

覆盖 12 个视口 × 首页/列表/长文章/搜索共 48 组，以及移动菜单、空列表、3 个缩放等效场景、两种动画偏好下的开场/返回/跳过，共 55 项。
断言滚动前后固定容器坐标、头肩区域高度、正文与伴随区间距、横向溢出、头像展开边界和 Escape 焦点返回。
缩放测试采用 CSS 视口与设备比例组合，不声称操作过浏览器菜单的真实缩放。
几何断言不能替代素材边缘检查；当前已接入用户授权的非生成式精细抠图版本，过程与背景叠加检查见 `output/character-design/Aletheia/matting/README.md`。

## 2026-09-10 素材修复记录：生成式尝试失败，后续非生成式处理已完成

用户授权非生成式精细抠图后，已从第一次原始正面图重建遮罩与边缘 Alpha，保留衣料内部高光并去除边缘白底污染。最终资源为 `aletheia-guide-front-matted-v4.png`，包含 41,606 个半透明像素；原图、副本脚本与检查图保留在 `output/character-design/Aletheia/matting/`。以下为此前失败尝试的历史记录。

按 imagegen 技能使用内置 ImageGen，对原正面图做背景提取及一次定向修正。两个结果均为 `Format24bppRgb`，角落 Alpha 为 255，且肉眼可见棋盘格被画入图片。未通过真透明门槛，未复制到 public、未替换现有素材。没有调用 CLI，也没有执行颜色阈值抠图。

原图：`C:/Users/87773/.codex/generated_images/01a0891f-5113-74e0-a357-59dafd61c53a/exec-b8c0f773-f071-4586-9769-e57aef2ce8eb.png`

失败输出保留在同一目录：

- `exec-321e283d-8edc-4204-a29d-513e6a878a09.png`
- `exec-c0066446-04fb-41c0-b33b-9b8bff1c15f7.png`

第一次提示词：

> Use case: background-extraction. Edit this exact front-facing Aletheia character image for a website overlay. Remove ALL the baked white/gray checkerboard background including the enclosed spaces between strands of hair and costume. Output a genuinely transparent RGBA PNG, empty pixels alpha=0, naturally antialiased partially transparent edge pixels. Do NOT paint a checkerboard or solid background to simulate transparency. Remove white matte contamination and white halos at hair and clothing edges. Preserve the exact face, front-facing pose, brown hair, black feather hat with purple rune, bronze accessories, ivory corset and layered dark mauve ragged outfit and white waist feather; preserve white details inside the character opaque. Keep original full-body framing with complete hat and feet. No new objects, text, glow or decorative particles. This is only clean background extraction, not a redesign.

一次定向修正提示词：

> CORRECTION, background extraction only: The previous result was an RGB image with an actual painted checkerboard, not transparency. Deliver this exact character cutout with a REAL PNG ALPHA CHANNEL. All checkerboard squares must be removed, including between hair strands and ragged costume. Empty background pixels must be transparent, not white, gray or black; output RGBA PNG with soft antialiased transparent edges. Preserve the character artwork unchanged: face, brown hair, black feathered hat purple rune, ivory corset, bronze ornaments, layered black/mauve outfit, white waist feather, full pose. No checkerboard pattern, no matte fringe, no isolated background speckles. Do not replace transparency with a simulated transparency preview.
