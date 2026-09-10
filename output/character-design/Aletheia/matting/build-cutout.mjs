// Deterministic extraction from the original checker-matted illustration.
// No image generation, no global brightness-to-alpha replacement.
import { createRequire } from "node:module";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { createHash } from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "../../../..");
const require = createRequire(path.join(root, "react_frontend/package.json"));
const sharp = require("sharp");
const source = process.argv[2] || path.join(here, "source-front.png");
const out = path.join(here, "inspection");
await mkdir(out, { recursive: true });
const input = await readFile(source);
const { data: rgb, info: { width: w, height: h } } = await sharp(input).removeAlpha().raw().toBuffer({ resolveWithObject: true });
if (w !== 1024 || h !== 1536) throw new Error("Source-specific trimap requires the original 1024x1536 front illustration");
const n = w * h;
const clamp = (v, lo = 0, hi = 1) => Math.max(lo, Math.min(hi, v));
const polygons = [
  // Interior constraints only: skin, ivory corset and white feather must stay opaque.
  [[450,204],[495,195],[514,222],[501,253],[477,266],[451,246]],
  [[411,325],[437,307],[448,324],[434,354],[410,355]],
  [[399,302],[426,300],[430,316],[420,339],[387,339],[389,322]],
  [[444,349],[477,368],[525,351],[558,358],[554,426],[558,476],[482,513],[443,470]],
  [[674,578],[697,561],[726,527],[720,570],[698,594],[671,604]],
  // Trace the feather's pale upper tips separately from the neighboring hair gap.
  [[723,419],[727,438],[730,451],[734,459],[731,477],[724,491],[718,470],[717,453],[714,445],[719,453],[722,459]],
];
function inside(x, y, p) {
  let yes = false;
  for (let i = 0, j = p.length - 1; i < p.length; j = i++) {
    if ((p[i][1] > y) !== (p[j][1] > y) && x < (p[j][0]-p[i][0])*(y-p[i][1])/(p[j][1]-p[i][1])+p[i][0]) yes = !yes;
  }
  return yes;
}
const protectedPixel = new Uint8Array(n);
const candidate = new Uint8Array(n);
for (let i = 0; i < n; i++) {
  const x = i % w, y = Math.floor(i / w), j = i * 3;
  const lo = Math.min(rgb[j], rgb[j+1], rgb[j+2]), hi = Math.max(rgb[j], rgb[j+1], rgb[j+2]);
  protectedPixel[i] = polygons.some(p => inside(x,y,p)) ? 1 : 0;
  // This classifies trimap candidates, not final transparency. Local topology,
  // protected interiors and an edge unmixing step determine the final alpha.
  const hairGap = y>=190 && y<=550 && (x<420 || (x>580 && y<450) || x>775);
  candidate[i] = !protectedPixel[i] && ((hi-lo <= 9 && lo >= 180) || (hairGap && hi-lo<30 && lo>185)) ? 1 : 0;
}
const queue = new Int32Array(n);
const bgLabels = new Int32Array(n);
const bgComponents = [];
let bgLabel = 0;
for(let i=0;i<n;i++) {
  if(!candidate[i] || bgLabels[i]) continue;
  bgLabel++;
  let end=1, minX=w, minY=h, maxX=0, maxY=0;
  queue[0]=i; bgLabels[i]=bgLabel;
  for(let k=0;k<end;k++) {
    const p=queue[k], x=p%w, y=Math.floor(p/w);
    minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
    for(const q of [x?p-1:-1,x<w-1?p+1:-1,p-w,p+w]) {
      if(q>=0 && q<n && candidate[q] && !bgLabels[q]) {bgLabels[q]=bgLabel;queue[end++]=q;}
    }
  }
  bgComponents.push({label:bgLabel,count:end,minX,minY,maxX,maxY});
}
await writeFile(path.join(out,"background-regions.json"),JSON.stringify(bgComponents.filter(c=>c.count>3).sort((a,b)=>b.count-a.count),null,2));
// Bounded bright regions inside the costume are opaque highlights, not backdrop.
// Preserve only the observed negative spaces among hair strands and the large
// lower cloak/boot gap. This spatial trimap prevents punching out white fabrics.
const exterior = bgComponents.reduce((a,b)=>a.count>b.count?a:b).label;
const actualBackground = new Set(bgComponents.filter(c => c.label===exterior ||
  (c.minY<560 && (c.maxX<420 || (c.minX>560 && c.minY<449) || c.minX>=750)) ||
  (c.minY>700 && c.count>80)
).map(c=>c.label));
for(let i=0;i<n;i++) if(candidate[i] && !actualBackground.has(bgLabels[i])) {
  candidate[i]=0;
  protectedPixel[i]=1;
}
const labels = new Int32Array(n);
let label = 0;
const components = [];
for (let i = 0; i < n; i++) {
  if (candidate[i] || labels[i]) continue;
  label++;
  let end = 1;
  queue[0] = i; labels[i] = label;
  for (let k = 0; k < end; k++) {
    const p = queue[k], x = p % w;
    for (const q of [x ? p-1 : -1, x<w-1 ? p+1 : -1, p-w, p+w]) {
      if (q >= 0 && q < n && !candidate[q] && !labels[q]) { labels[q] = label; queue[end++] = q; }
    }
  }
  components.push({ label, count: end });
}
const main = components.reduce((a,b) => a.count > b.count ? a : b).label;
const foreground = new Uint8Array(n);
for (let i = 0; i < n; i++) foreground[i] = labels[i] === main ? 1 : 0;

// Multi-source distance transforms provide a trimap and nearest known foreground.
function distanceTo(test) {
  const distance = new Uint16Array(n); distance.fill(65535);
  const nearest = new Int32Array(n); nearest.fill(-1);
  let end = 0;
  for (let i=0;i<n;i++) if (test(i)) { distance[i]=0; nearest[i]=i; queue[end++]=i; }
  for (let k=0;k<end;k++) {
    const p=queue[k], x=p%w;
    for (const q of [x ? p-1 : -1, x<w-1 ? p+1 : -1,p-w,p+w]) {
      if (q>=0 && q<n && distance[q]>distance[p]+1) { distance[q]=distance[p]+1; nearest[q]=nearest[p]; queue[end++]=q; }
    }
  }
  return { distance, nearest };
}
const toBackground = distanceTo(i => !foreground[i]);
const toForeground = distanceTo(i => foreground[i]);
const core = distanceTo(i => foreground[i] && (toBackground.distance[i] >= 4 || protectedPixel[i]));
const rgba = Buffer.alloc(n*4);
const trimap = Buffer.alloc(n);
let partial = 0, opaque = 0, transparent = 0;
for (let i=0;i<n;i++) {
  const x=i%w,y=Math.floor(i/w),j=i*3,k=i*4;
  let a = foreground[i] ? 1 : 0;
  let color = [rgb[j],rgb[j+1],rgb[j+2]];
  trimap[i] = protectedPixel[i] || toBackground.distance[i] >= 4 ? 255 : toForeground.distance[i] > 2 ? 0 : 128;
  if (!protectedPixel[i] && toBackground.distance[i] < 4 && toForeground.distance[i] <= 2) {
    const fIndex = core.nearest[i];
    if (fIndex >= 0 && core.distance[i] <= 48) {
      const f = [rgb[fIndex*3],rgb[fIndex*3+1],rgb[fIndex*3+2]];
      // Local checker background estimate. Match the observed luminance when the
      // pixel is near-neutral; otherwise use the closest uncontaminated sample.
      const bIndex = toBackground.nearest[i];
      let b = [rgb[bIndex*3],rgb[bIndex*3+1],rgb[bIndex*3+2]];
      const bSamples=[];
      for (let dy=-5;dy<=5;dy++) for(let dx=-5;dx<=5;dx++) {
        const xx=x+dx, yy=y+dy;
        if(xx<0||xx>=w||yy<0||yy>=h) continue;
        const p=yy*w+xx;
        if(candidate[p] && toForeground.distance[p]>=2) bSamples.push({p,d:dx*dx+dy*dy});
      }
      bSamples.sort((a,b)=>a.d-b.d);
      if(bSamples.length) { const p=bSamples[0].p*3; b=[rgb[p],rgb[p+1],rgb[p+2]]; }
      // Neutral checkerboard has no chroma; use chroma to avoid a false opaque
      // edge when neighboring checker squares have different brightness.
      const fc = f.map(v=>v-(f[0]+f[1]+f[2])/3);
      const cc = color.map(v=>v-(color[0]+color[1]+color[2])/3);
      const chromaA = clamp(cc.reduce((s,v,c)=>s+v*fc[c],0) / Math.max(1,fc.reduce((s,v)=>s+v*v,0)));
      const d = f.map((v,c)=>v-b[c]);
      const luminanceA = clamp(color.reduce((s,v,c)=>s+(v-b[c])*d[c],0) / Math.max(1,d.reduce((s,v)=>s+v*v,0)));
      const fChroma = Math.max(...f)-Math.min(...f);
      a = fChroma > 18 ? Math.min(luminanceA, chromaA) : luminanceA;
      if(candidate[i] && Math.max(...color)-Math.min(...color)<5) a=0;
      if(a < .025) a=0;
      if(a > .985) a=1;
      if(a > 0 && a < 1) {
        // Remove the white matte rather than darkening or blurring the contour.
        color=color.map((v,c)=>clamp((v-(1-a)*b[c])/Math.max(a,.08),0,255));
        // Unmixing uncertainty is largest at thin tips; use the nearest true
        // foreground color there instead of retaining neutral matte spill.
        color=color.map((v,c)=>v * a + f[c]*(1-a));
      }
    }
  }
  for(let c=0;c<3;c++) rgba[k+c]=a===0 ? 0 : Math.round(color[c]);
  rgba[k+3]=Math.round(a*255);
  if(rgba[k+3]===0) transparent++; else if(rgba[k+3]===255) opaque++; else partial++;
}
const asset=path.join(here,"aletheia-guide-front-matted.png");
await sharp(rgba,{raw:{width:w,height:h,channels:4}}).png().toFile(asset);
await sharp(trimap,{raw:{width:w,height:h,channels:1}}).png().toFile(path.join(out,"trimap.png"));
for(const [name,background] of [["black","#000000"],["purple","#151024"]]) {
  await sharp({create:{width:w,height:h,channels:3,background}}).composite([{input:asset}]).png().toFile(path.join(out,`${name}-full.png`));
  await sharp({create:{width:420,height:630,channels:3,background}}).composite([{input:await sharp(asset).resize(420,630).toBuffer()}]).png().toFile(path.join(out,`${name}-display.png`));
}
await sharp(path.join(root,"react_frontend/public/images/aletheia/workshop-background.png")).resize(w,h,{fit:"cover"}).modulate({brightness:.45}).composite([{input:asset}]).png().toFile(path.join(out,"workshop-full.png"));
await sharp(path.join(out,"purple-full.png")).extract({left:180,top:120,width:650,height:540}).png().toFile(path.join(out,"hair-and-feather.png"));
const oldAsset=path.join(root,"react_frontend/public/images/aletheia/aletheia-guide-front-cutout-v3.png");
const comparison=[];
for(const [index,file] of [oldAsset,asset].entries()) comparison.push({
  input:await sharp(file).resize(420,630).extract({left:0,top:0,width:420,height:420}).toBuffer(),left:index*420,top:0,
});
await sharp({create:{width:840,height:420,channels:3,background:"#151024"}}).composite(comparison).png().toFile(path.join(out,"before-after.png"));
let preservedInterior=0;
for(let i=0;i<n;i++) if(protectedPixel[i] || (foreground[i] && toBackground.distance[i]>=4)) {
  if(rgba[i*4+3]!==255 || [0,1,2].some(c=>rgba[i*4+c]!==rgb[i*3+c])) throw new Error(`Opaque interior changed at ${i%w},${Math.floor(i/w)}`);
  preservedInterior++;
}
const samples=[
  ["exterior",0,0,0],["hair gap left",270,350,0],["hair gap right",731,407,0],
  ["face",475,225,255],["shoulder",399,318,255],["ivory corset",480,425,255],
  ["white skirt",488,611,255],["white feather",710,533,255],
].map(([name,x,y,expected])=>{
  const alpha=rgba[(y*w+x)*4+3];
  if(alpha!==expected) throw new Error(`${name}: alpha ${alpha}, expected ${expected}`);
  return {name,x,y,alpha};
});
if(partial<1000 || transparent<n*.3 || opaque<n*.3) throw new Error("Invalid matte distribution");
const report={sourceSha256:createHash("sha256").update(input).digest("hex"),width:w,height:h,transparent,partial,opaque,preservedInterior,samples,foregroundComponents:components.length,asset};
await writeFile(path.join(out,"alpha-report.json"),JSON.stringify(report,null,2));
console.log(report);
