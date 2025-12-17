<template>
  <div style="position:fixed; inset:0;">
    <canvas ref="canvasRef" style="width:100vw; height:100vh; display:block;"></canvas>
    <!-- 分数面板 -->
    <div style="position:absolute; left:12px; top:10px; color:#fff; font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto; font-weight:700; background:rgba(0,0,0,.5); padding:6px 10px; border-radius:8px; z-index:10; text-shadow:0 1px 2px rgba(0,0,0,.3); backdrop-filter:blur(4px);">
      分数：{{ score }}
    </div>
    <!-- 加分提示 -->
    <div v-for="(pop, idx) in scorePops" :key="idx" style="position:absolute; left:0; top:0; color:#ffd700; font-weight:700; pointer-events:none; z-index:10; text-shadow:0 1px 3px rgba(0,0,0,.5);" :style="{left: pop.x + 'px', top: pop.y + 'px', opacity: pop.opacity}">
      +{{ pop.value }}
    </div>
    <!-- 开始/结束弹窗 -->
    <div v-if="showOverlay" style="position:absolute; inset:0; display:flex; align-items:center; justify-content:center; z-index:20; background:rgba(0,0,0,.2);">
      <div style="background:rgba(255,255,255,.95); padding:22px 24px; border-radius:16px; text-align:center; width:min(92%,420px); box-shadow:0 8px 24px rgba(0,0,0,.15);">
        <div v-if="!gameStarted" style="font-size:22px; font-weight:800; margin-bottom:10px; color:#1e40af;">按空格或点击开始</div>
        <div v-else style="font-size:20px; font-weight:800; margin-bottom:10px; color:#dc2626;">游戏结束</div>
        <div v-if="lastScore !== null" style="margin:8px 0 16px; color:#333; font-size:18px;">最终得分：{{ lastScore }}</div>
        <button @click="startGame" style="cursor:pointer; background:#1e40af; color:white; border:none; padding:12px 20px; border-radius:12px; font-weight:700; font-size:16px;">重新开始</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'

const canvasRef = ref(null)
const ctxRef = ref(null)
let width = 860
let height = 420
let groundY = height - 80

// 物理参数
let gravity = 0.45
let jumpVelocity = -14
const gameSpeedBase = 4.4

const score = ref(0)
const lastScore = ref(null)
const showOverlay = ref(true)
const gameStarted = ref(false)
const scorePops = ref([])
let rafId = 0
let running = false
let lastTs = 0

// 水流相关
let bgOffsetX = 0
let waveIntensity = 1

// 人物（帧动画+精准碰撞盒）
const player = {
  x: 80,
  y: groundY - 68,
  w: 48,
  h: 68,
  vy: 0,
  onGround: true,
  jumpsRemaining: 2,
  frameIndex: 0,
  frameTimer: 0,
  // 碰撞盒优化：偏移+缩小，贴合人物实际轮廓（避免透明区域触发碰撞）
  collisionBox: {
    offsetX: 8,   // 碰撞盒X轴偏移（向右移，避开左侧透明区域）
    offsetY: 10,  // 碰撞盒Y轴偏移（向下移，避开顶部透明区域）
    width: 32,    // 碰撞盒宽度（缩小为原宽度的2/3）
    height: 56    // 碰撞盒高度（缩小为原高度的4/5）
  }
}

const obstacles = []
const leeks = []
const bubbles = []

let spawnObstacleTimer = 0
let spawnLeekTimer = 0
let spawnBubbleTimer = 0
let timeSinceLastObstacle = 0
let scoreTimeAccumulator = 0

// 背景图（修复缓存）
const bgImage = new Image()
bgImage.src = `/游戏背景.jpg?timestamp=${new Date().getTime()}`
let bgLoaded = false
bgImage.onload = () => { bgLoaded = true }
bgImage.onerror = () => { 
  console.warn('背景图加载失败，使用备用色')
  bgLoaded = false 
}

// 初音未来帧动画配置（透明背景PNG直接适配）
const playerFrames = [
  { src: '/miku-run-cycle1.png', w: 48, h: 68, image: null },
  { src: '/miku-run-cycle2.png', w: 48, h: 68, image: null },
  { src: '/miku-run-cycle3.png', w: 48, h: 68, image: null },
  { src: '/miku-run-cycle4.png', w: 48, h: 68, image: null },
  { src: '/miku-run-cycle5.png', w: 48, h: 68, image: null }
]

// 预加载帧图片
const loadPlayerFrames = () => {
  playerFrames.forEach(frame => {
    if (frame.src) {
      const img = new Image()
      img.crossOrigin = 'anonymous'
      img.src = frame.src + `?timestamp=${new Date().getTime()}`
      img.onload = () => { frame.image = img }
      img.onerror = () => { console.warn(`帧图片${frame.src}加载失败，请检查路径和图片格式`) }
    }
  })
}
loadPlayerFrames()

// 重置游戏
function resetGameState() {
  score.value = 0
  scorePops.value = []
  bubbles.length = 0
  bgOffsetX = 0
  waveIntensity = 1
  player.x = 80
  player.y = groundY - player.h
  player.vy = 0
  player.onGround = true
  player.jumpsRemaining = 2
  player.frameIndex = 0
  player.frameTimer = 0
  obstacles.length = 0
  leeks.length = 0
  spawnObstacleTimer = 0
  spawnLeekTimer = 0
  spawnBubbleTimer = 0
  timeSinceLastObstacle = 0
  scoreTimeAccumulator = 0
}

// 开始游戏
function startGame() {
  resetGameState()
  showOverlay.value = false
  gameStarted.value = true
  running = true
  lastTs = 0
  loop()
}

// 结束游戏
function endGame() {
  running = false
  cancelAnimationFrame(rafId)
  lastScore.value = score.value
  showOverlay.value = true
}

// 碰撞检测优化：使用人物精准碰撞盒
function rectsOverlap(a, b) {
  return a.x + 2 < b.x + b.w &&  // 缩小碰撞判定范围（+2/-2）
         a.x + a.w - 2 > b.x && 
         a.y + 2 < b.y + b.h && 
         a.y + a.h - 2 > b.y
}

// 生成障碍（缩小碰撞盒，避免误碰）
function spawnObstacle() {
  const typeList = ['eiffel', 'pyramid', 'pearl']
  const type = typeList[Math.floor(Math.random() * typeList.length)]
  let w, h
  if (type === 'pyramid') {
    h = 40 + Math.random() * 50
    w = h * 0.7  // 原宽度h → 缩小为0.7h
  } else if (type === 'pearl') {
    h = 60 + Math.random() * 60
    w = Math.max(25, h * 0.3)  // 原0.4 → 缩小为0.3
  } else { // eiffel
    h = 70 + Math.random() * 70
    w = Math.max(24, h * 0.28) // 原0.35 → 缩小为0.28
  }
  obstacles.push({ 
    x: width + 20, 
    y: groundY - h, 
    w, 
    h, 
    passed: false, 
    type,
    // 障碍物碰撞盒偏移（向上移，避免底部无效区域触发碰撞）
    collisionBox: {
      offsetY: 5,
      height: h - 5
    }
  })
}

// 生成大葱
function spawnLeek() {
  const airborne = Math.random() < 0.55
  const size = 22
  const baseY = airborne ? groundY - (80 + Math.random() * 80) : groundY - size - 6
  leeks.push({ x: width + 10, y: baseY, w: size, h: size, collected: false })
}

// 生成大透明水泡
function spawnBubble() {
  const x = Math.random() * width
  const y = groundY - 20 - Math.random() * (height - 120)
  const radius = 8 + Math.random() * 10
  const speedY = -0.3 - Math.random() * 0.8
  const opacity = 0.2 + Math.random() * 0.4
  bubbles.push({ x, y, radius, speedY, opacity, life: 0 })
}

// 绘制背景（水流滚动）
function drawBackground(ctx) {
  if (bgLoaded) {
    const imgW = bgImage.width || width
    const imgH = bgImage.height || height
    const scale = Math.max(width / imgW, height / imgH)
    const dw = imgW * scale
    const dh = imgH * scale
    const dx = (width - dw) / 2 - bgOffsetX
    const dy = (height - dh) / 2

    ctx.drawImage(bgImage, dx, dy, dw, dh)
    if (dx + dw < width) ctx.drawImage(bgImage, dx + dw, dy, dw, dh)
    ctx.fillStyle = 'rgba(0, 30, 60, 0.25)'
    ctx.fillRect(0, 0, width, height)
  } else {
    const bgGrd = ctx.createLinearGradient(0, 0, 0, height)
    bgGrd.addColorStop(0, '#0f172a')
    bgGrd.addColorStop(0.5, '#1e3a8a')
    bgGrd.addColorStop(1, '#3b82f6')
    ctx.fillStyle = bgGrd
    ctx.fillRect(0, 0, width, height)
  }

  // 水下道路
  const roadHeight = height - groundY
  const roadGrd = ctx.createLinearGradient(0, groundY, 0, groundY + roadHeight)
  roadGrd.addColorStop(0, '#1e3a8a')
  roadGrd.addColorStop(0.5, '#0f172a')
  roadGrd.addColorStop(1, '#030712')
  ctx.fillStyle = roadGrd
  ctx.fillRect(0, groundY, width, roadHeight)

  // 道路边缘光晕
  const edgeGrd = ctx.createLinearGradient(0, groundY - 10, 0, groundY)
  edgeGrd.addColorStop(0, 'rgba(59, 130, 246, 0.6)')
  edgeGrd.addColorStop(1, 'transparent')
  ctx.fillStyle = edgeGrd
  ctx.fillRect(0, groundY - 10, width, 10)

  // 动态波纹
  ctx.strokeStyle = 'rgba(147, 197, 253, 0.5)'
  ctx.lineWidth = 1.5
  for (let i = 0; i < 3; i++) {
    const y = groundY - 5 - Math.random() * 3
    ctx.beginPath()
    ctx.moveTo(0, y)
    for (let x = 0; x < width; x += 15) {
      const offset = Math.sin(x / 40 + i * Math.PI + score.value * 0.01) * waveIntensity
      ctx.lineTo(x, y + offset)
    }
    ctx.lineTo(width, y)
    ctx.stroke()
  }
}

// 绘制人物（透明背景自动适配，无需额外代码）
function drawMiku(ctx, x, y, w, h) {
  // 帧动画播放逻辑（每8帧切换一次，流畅度适中）
  player.frameTimer += 1
  if (player.frameTimer >= 8) {
    player.frameIndex = (player.frameIndex + 1) % playerFrames.length
    player.frameTimer = 0
  }

  const currentFrame = playerFrames[player.frameIndex]
  if (currentFrame.image) {
    // 直接绘制透明背景PNG（Canvas自动支持透明度）
    ctx.drawImage(currentFrame.image, x, y, currentFrame.w, currentFrame.h)
  } else {
    // 备用占位动画（跑步姿态）
    ctx.fillStyle = '#f9d3c2'
    ctx.beginPath(); ctx.roundRect?.(x, y, w, h, 8) || ctx.rect(x, y, w, h); ctx.fill()
    
    // 腿部动画
    ctx.fillStyle = '#f9f9f9'
    if (player.frameIndex === 0) {
      ctx.fillRect(x + w/2 - 12, y + h - 18, 6, 18)
      ctx.fillRect(x + w/2 + 6, y + h - 22, 6, 14)
    } else if (player.frameIndex === 1) {
      ctx.fillRect(x + w/2 - 12, y + h - 22, 6, 14)
      ctx.fillRect(x + w/2 + 6, y + h - 18, 6, 18)
    } else {
      ctx.fillRect(x + w/2 - 12, y + h - 20, 6, 16)
      ctx.fillRect(x + w/2 + 6, y + h - 20, 6, 16)
    }
    
    // 头发和服装细节
    ctx.fillStyle = '#00a86b' // 初音绿头发
    ctx.beginPath(); ctx.arc(x + 12, y + 15, 8, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(x + 36, y + 15, 8, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#374151'
    ctx.fillRect(x + 8, y + h/2, w - 16, h/2 - 4)
  }

  // 【调试用】绘制人物碰撞盒（需要调试时取消注释）
  // ctx.strokeStyle = 'rgba(255, 0, 0, 0.5)'
  // ctx.lineWidth = 1
  // ctx.strokeRect(
  //   x + player.collisionBox.offsetX,
  //   y + player.collisionBox.offsetY,
  //   player.collisionBox.width,
  //   player.collisionBox.height
  // )
}

// 绘制障碍
function drawObstacle(ctx, o) {
  if (o.type === 'pyramid') {
    ctx.fillStyle = '#94a3b8'
    ctx.beginPath()
    ctx.moveTo(o.x + o.w / 2, o.y)
    ctx.lineTo(o.x + o.w, o.y + o.h)
    ctx.lineTo(o.x, o.y + o.h)
    ctx.closePath()
    ctx.fill()
    ctx.strokeStyle = '#64748b'
    ctx.lineWidth = 2
    ctx.stroke()
  } else if (o.type === 'pearl') {
    ctx.fillStyle = '#64748b'
    ctx.fillRect(o.x + o.w * 0.45, o.y + o.h * 0.15, o.w * 0.1, o.h * 0.85)
    ctx.fillStyle = '#bfdbfe'
    ctx.beginPath(); ctx.arc(o.x + o.w * 0.5, o.y + o.h * 0.35, Math.min(o.w, o.h) * 0.18, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(o.x + o.w * 0.5, o.y + o.h * 0.75, Math.min(o.w, o.h) * 0.22, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = 'rgba(255,255,255,0.5)'
    ctx.beginPath(); ctx.arc(o.x + o.w * 0.45, o.y + o.h * 0.32, Math.min(o.w, o.h) * 0.05, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(o.x + o.w * 0.45, o.y + o.h * 0.72, Math.min(o.w, o.h) * 0.06, 0, Math.PI * 2); ctx.fill()
  } else { // eiffel
    ctx.fillStyle = '#475569'
    ctx.beginPath()
    ctx.moveTo(o.x + o.w / 2, o.y)
    ctx.lineTo(o.x + o.w, o.y + o.h)
    ctx.lineTo(o.x, o.y + o.h)
    ctx.closePath()
    ctx.fill()
    ctx.strokeStyle = '#334155'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(o.x + o.w * 0.2, o.y + o.h * 0.6)
    ctx.lineTo(o.x + o.w * 0.8, o.y + o.h * 0.6)
    ctx.moveTo(o.x + o.w * 0.25, o.y + o.h * 0.8)
    ctx.lineTo(o.x + o.w * 0.75, o.y + o.h * 0.8)
    ctx.stroke()
  }

  // 【调试用】绘制障碍物碰撞盒（需要调试时取消注释）
  // ctx.strokeStyle = 'rgba(0, 255, 0, 0.5)'
  // ctx.lineWidth = 1
  // ctx.strokeRect(
  //   o.x,
  //   o.y + o.collisionBox.offsetY,
  //   o.w,
  //   o.collisionBox.height
  // )
}

// 绘制大葱
function drawLeek(ctx, c) {
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(c.x + 6, c.y + 6, 8, c.h - 12)
  ctx.fillStyle = '#10b981'
  ctx.beginPath(); ctx.roundRect?.(c.x, c.y, c.w, 10, 3) || ctx.rect(c.x, c.y, c.w, 10); ctx.fill()
  ctx.beginPath(); ctx.roundRect?.(c.x + 12, c.y - 6, 8, 12, 2) || ctx.rect(c.x + 12, c.y - 6, 8, 12); ctx.fill()
  ctx.fillStyle = 'rgba(255,255,255,0.4)'
  ctx.fillRect(c.x + 8, c.y + 8, 4, c.h - 16)
}

// 绘制水泡
function drawBubbles(ctx) {
  bubbles.forEach(bubble => {
    const bubbleGrd = ctx.createRadialGradient(
      bubble.x, bubble.y, 0,
      bubble.x, bubble.y, bubble.radius
    )
    bubbleGrd.addColorStop(0, `rgba(255, 255, 255, ${bubble.opacity})`)
    bubbleGrd.addColorStop(1, `rgba(200, 230, 255, ${bubble.opacity * 0.2})`)
    ctx.fillStyle = bubbleGrd
    ctx.beginPath()
    ctx.arc(bubble.x, bubble.y, bubble.radius, 0, Math.PI * 2)
    ctx.fill()

    ctx.fillStyle = `rgba(255, 255, 255, ${bubble.opacity * 0.8})`
    ctx.beginPath()
    ctx.arc(bubble.x - bubble.radius * 0.3, bubble.y - bubble.radius * 0.3, bubble.radius * 0.25, 0, Math.PI * 2)
    ctx.fill()
  })
}

// 加分提示
function addScorePop(x, y, value) {
  scorePops.value.push({
    x: x + 24,
    y: y - 20,
    value,
    opacity: 1,
    timer: 0
  })
}

// 更新加分提示
function updateScorePops(delta) {
  scorePops.value = scorePops.value.map(pop => {
    pop.timer += delta
    pop.y -= 0.5
    pop.opacity = 1 - (pop.timer / 1000)
    return pop
  }).filter(pop => pop.opacity > 0)
}

// 更新水泡
function updateBubbles(delta) {
  spawnBubbleTimer -= delta
  if (spawnBubbleTimer <= 0) {
    spawnBubble()
    spawnBubbleTimer = 200 + Math.random() * 400
  }

  for (let i = bubbles.length - 1; i >= 0; i--) {
    const bubble = bubbles[i]
    bubble.y += bubble.speedY
    bubble.life += delta
    bubble.x += Math.sin(bubble.life / 250) * 0.5

    if (bubble.y + bubble.radius < 0 || bubble.life > 4000) {
      bubbles.splice(i, 1)
    }
  }
}

// 更新水流
function updateWaterFlow(delta) {
  const speed = gameSpeedBase + Math.min(5, score.value / 150)
  bgOffsetX += speed * 0.08
  waveIntensity = 1 + Math.min(2, score.value / 200)
}

// 游戏更新
function update(delta) {
  const ctx = ctxRef.value
  const speed = gameSpeedBase + Math.min(5, score.value / 150)

  // 人物物理
  player.vy += gravity
  player.y += player.vy
  if (player.y + player.h >= groundY) {
    player.y = groundY - player.h
    player.vy = 0
    player.onGround = true
    player.jumpsRemaining = 2
  }

  // 计时得分
  scoreTimeAccumulator += delta
  while (scoreTimeAccumulator >= 1000) {
    score.value += 1
    scoreTimeAccumulator -= 1000
  }

  // 更新障碍
  timeSinceLastObstacle += delta
  for (let i = obstacles.length - 1; i >= 0; i--) {
    const o = obstacles[i]
    o.x -= speed
    if (!o.passed && o.x + o.w < player.x) {
      o.passed = true
      score.value += 1
      addScorePop(o.x, o.y, 1)
    }
    if (o.x + o.w < -20) obstacles.splice(i, 1)
  }

  // 更新大葱
  for (let i = leeks.length - 1; i >= 0; i--) {
    const c = leeks[i]
    c.x -= speed
    if (c.x + c.w < -20) leeks.splice(i, 1)
  }

  // 精准碰撞检测：使用人物和障碍物的优化碰撞盒
  const pRect = {
    x: player.x + player.collisionBox.offsetX,
    y: player.y + player.collisionBox.offsetY,
    w: player.collisionBox.width,
    h: player.collisionBox.height
  }
  for (const o of obstacles) {
    const oRect = {
      x: o.x,
      y: o.y + o.collisionBox.offsetY,
      w: o.w,
      h: o.collisionBox.height
    }
    if (rectsOverlap(pRect, oRect)) {
      endGame()
      return
    }
  }

  // 收集大葱
  for (let i = leeks.length - 1; i >= 0; i--) {
    const c = leeks[i]
    if (!c.collected && rectsOverlap(pRect, c)) {
      c.collected = true
      score.value += 3
      addScorePop(c.x, c.y, 3)
      leeks.splice(i, 1)
    }
  }

  // 生成逻辑
  spawnObstacleTimer -= delta
  if (spawnObstacleTimer <= 0) {
    spawnObstacle()
    spawnObstacleTimer = 700 + Math.random() * 600
    timeSinceLastObstacle = 0
  }
  if (timeSinceLastObstacle > 1600) {
    spawnObstacle()
    spawnObstacleTimer = 700 + Math.random() * 600
    timeSinceLastObstacle = 0
  }

  spawnLeekTimer -= delta
  if (spawnLeekTimer <= 0) {
    spawnLeek()
    spawnLeekTimer = 800 + Math.random() * 1000
  }

  // 更新动态效果
  updateScorePops(delta)
  updateBubbles(delta)
  updateWaterFlow(delta)

  // 绘制顺序：背景 → 障碍 → 大葱 → 人物 → 水泡 → 雾效 → 加分提示
  drawBackground(ctx)
  for (const o of obstacles) drawObstacle(ctx, o)
  for (const c of leeks) drawLeek(ctx, c)
  drawMiku(ctx, player.x, player.y, player.w, player.h)
  drawBubbles(ctx)

  // 雾效
  const grd = ctx.createLinearGradient(0, groundY - 20, 0, groundY + 60)
  grd.addColorStop(0, 'rgba(0, 30, 60, 0)')
  grd.addColorStop(1, 'rgba(0, 30, 60, 0.4)')
  ctx.fillStyle = grd
  ctx.fillRect(0, groundY - 20, width, 80)

  // 加分提示
  scorePops.value.forEach(pop => {
    ctx.fillStyle = `rgba(255, 215, 0, ${pop.opacity})`
    ctx.font = 'bold 16px ui-sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(`+${pop.value}`, pop.x, pop.y)
  })
}

// 游戏循环
function loop(ts = 0) {
  if (!running) return
  const delta = Math.min(48, ts - lastTs || 16)
  lastTs = ts
  update(delta)
  rafId = requestAnimationFrame(loop)
}

// 跳跃
function jump() {
  if (!running) return
  if (player.onGround || player.jumpsRemaining > 0) {
    player.vy = jumpVelocity
    player.onGround = false
    if (player.jumpsRemaining > 0) player.jumpsRemaining -= 1
  }
}

// 键盘控制
function handleKey(e) {
  if (e.code === 'Space') {
    e.preventDefault()
    if (showOverlay.value) startGame()
    else jump()
  }
}

// 点击控制
function handlePointer() {
  if (showOverlay.value) startGame()
  else jump()
}

// 自适应屏幕
function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  width = Math.floor(window.innerWidth)
  height = Math.floor(window.innerHeight)
  groundY = Math.floor(height - Math.max(80, height * 0.14))
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  const ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctxRef.value = ctx
  if (!running) {
    drawBackground(ctx)
    drawMiku(ctx, player.x, player.y, player.w, player.h)
    drawBubbles(ctx)
  }
}

// 挂载
onMounted(() => {
  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)
  window.addEventListener('keydown', handleKey)
  const canvas = canvasRef.value
  canvas.addEventListener('pointerdown', handlePointer)
  
  // 加载提示
  if (!bgLoaded || !playerFrames[0].image) {
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#bfdbfe'
    ctx.font = '18px ui-sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('加载水下世界...', width/2, height/2)
    drawBubbles(ctx)
  }
})

// 卸载
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKey)
  window.removeEventListener('resize', resizeCanvas)
  const canvas = canvasRef.value
  if (canvas) canvas.removeEventListener('pointerdown', handlePointer)
  cancelAnimationFrame(rafId)
})
</script>

<style scoped>
button:hover {
  background: #1e40af !important;
  transform: scale(1.05);
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.3);
}
button:active {
  transform: scale(0.98);
}
</style>