# 正面立绘非生成式精细抠图

2026-09-10：用户明确允许改用非生成式精细抠图。未调用 ImageGen、未下载模型、未使用外部 API。

## 文件与复现

- `source-front.png`：第一次正面立绘的未抠图副本；原始文件未修改。
- `build-cutout.mjs`：针对这张 1024×1536 原图的确定性遮罩与边缘处理脚本。它不是适用于任意人物图片的通用抠图器。
- `aletheia-guide-front-matted.png`：最终 RGBA PNG；前台副本为 `react_frontend/public/images/aletheia/aletheia-guide-front-matted-v4.png`。
- `inspection/alpha-report.json`：源图 SHA-256、Alpha 分布、内部像素不变断言与关键部位采样。
- `inspection/before-after.png`：左侧旧 v3，右侧精细抠图版。
- `inspection/black-full.png`、`purple-full.png`、`workshop-full.png`：原尺寸背景叠加检查；`*-display.png` 为 420px 宽显示检查；`hair-and-feather.png` 为细节检查。
- `inspection/trimap.png`：白色为确定前景、灰色为边缘求解区、黑色为确定背景。

在仓库根目录用 Node 24.19.0 执行：

```powershell
node output/character-design/Aletheia/matting/build-cutout.mjs
```

脚本复用当前前台安装的 Sharp，不新增运行时依赖。执行只更新此目录的派生图和检查报告，不自动替换前台素材。

## 处理方式

1. 从未硬抠过的原始图建立背景候选与连通区域，利用人物部位约束保护脸部、肩部、束身衣、裙片与白羽；区分衣料内部高光和发丝间真实空隙。
2. 在前景与背景交界处建立窄边缘求解区，根据相邻前景颜色与背景颜色估计透明度，并移除白底混色污染。
3. 保留核心区域原始 RGB；边缘输出渐变 Alpha，完全透明像素 RGB 清零。不改变人物姿态、面容和服装设计，不使用 CSS 发光或模糊掩盖边缘。

此方法包含图像专用的空间遮罩、色彩候选分类和边缘反混色；不是将全图的白色或灰色直接删除。烘焙背景没有原始 Alpha，重建的细发丝透明度属于估计值，不宣称恢复了原作者的原始分层。

## 最终检查

- 836,842 个完全透明像素、41,606 个半透明像素、694,416 个完全不透明像素。
- 688,458 个确定内部像素与源图 RGB 完全一致且 Alpha=255。
- 外部与左右发丝空隙采样 Alpha=0；脸部、肩部、象牙束身衣、白裙片、白羽采样 Alpha=255。
- 已检查黑色、暗紫和工坊背景、全身及头发/白羽特写，未见原 v3 的明显棋盘格、连续白边和浅色衣料误删穿孔。
- 原始正面图 SHA-256：`1a3737b22f302082d1d317e2e6e7ce893cdc016bdddabf4070475bf5fc2488f0`。
