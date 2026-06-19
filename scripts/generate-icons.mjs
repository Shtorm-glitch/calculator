import { deflateSync } from "node:zlib";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";

const root = process.cwd();
const iconsDir = path.join(root, "pwa", "public", "icons");
const splashDir = path.join(root, "pwa", "public", "splash");
mkdirSync(iconsDir, { recursive: true });
mkdirSync(splashDir, { recursive: true });

for (const [name, size, maskable] of [
  ["icon-180.png", 180, false],
  ["icon-192.png", 192, false],
  ["icon-512.png", 512, false],
  ["icon-maskable-512.png", 512, true]
]) {
  writeFileSync(path.join(iconsDir, name), png(size, size, (x, y) => iconPixel(x, y, size, maskable)));
}

writeFileSync(path.join(splashDir, "splash-iphone-portrait.png"), png(1290, 2796, splashPixel));

function splashPixel(x, y, width, height) {
  const cx = width / 2;
  const cy = height / 2 - 80;
  const iconSize = 260;
  const localX = x - (cx - iconSize / 2);
  const localY = y - (cy - iconSize / 2);
  let pixel = [32, 35, 39, 255];
  if (localX >= 0 && localY >= 0 && localX < iconSize && localY < iconSize) {
    pixel = iconPixel(localX, localY, iconSize, false);
  }
  const textY = cy + 205;
  if (Math.abs(y - textY) < 42 && Math.abs(x - cx) < 138) {
    if (farmTextPixel(x - (cx - 138), y - (textY - 42))) return [244, 246, 248, 255];
  }
  return pixel;
}

function iconPixel(x, y, size, maskable) {
  const s = size / 108;
  const px = x / s;
  const py = y / s;
  const inset = maskable ? 0 : 6;
  let color = [32, 35, 39, 255];
  if (roundRect(px, py, inset, inset, 108 - inset * 2, 108 - inset * 2, 22)) color = [32, 35, 39, 255];
  if (poly(px, py, [[31,25],[78,18],[86,23],[91,45],[86,53],[38,61],[30,56],[25,34]])) color = [118,199,91,255];
  if (poly(px, py, [[36,31],[75,25],[80,28],[84,43],[81,48],[42,55],[37,52],[33,36]])) color = [46,125,50,255];
  if (poly(px, py, [[43,34],[72,29],[76,43],[47,49]])) color = [166,227,111,255];
  if (circle(px, py, 59, 42, 9)) color = [46,125,50,255];
  if (roundRect(px, py, 17, 31, 74, 58, 9)) color = [141,63,22,255];
  if (roundRect(px, py, 20, 37, 68, 47, 8)) color = [184,96,37,255];
  if (roundRect(px, py, 60, 55, 36, 26, 7)) color = [122,50,20,255];
  if (roundRect(px, py, 65, 60, 27, 16, 4)) color = [169,80,32,255];
  if (circle(px, py, 75, 69, 7)) color = [255,200,61,255];
  if (line(px, py, 25, 43, 82, 43, 1.1) || line(px, py, 24, 79, 81, 79, 1.1) || line(px, py, 70, 60, 88, 60, 1.1) || line(px, py, 70, 76, 88, 76, 1.1)) color = [255,178,45,255];
  return color;
}

function png(width, height, pixel) {
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) {
    const row = y * (width * 4 + 1);
    raw[row] = 0;
    for (let x = 0; x < width; x++) {
      const [r, g, b, a] = pixel(x, y, width, height);
      const i = row + 1 + x * 4;
      raw[i] = r; raw[i + 1] = g; raw[i + 2] = b; raw[i + 3] = a;
    }
  }
  return Buffer.concat([
    Buffer.from([137,80,78,71,13,10,26,10]),
    chunk("IHDR", Buffer.concat([u32(width), u32(height), Buffer.from([8, 6, 0, 0, 0])])),
    chunk("IDAT", deflateSync(raw)),
    chunk("IEND", Buffer.alloc(0))
  ]);
}

function chunk(type, data) {
  const typeBuffer = Buffer.from(type);
  return Buffer.concat([u32(data.length), typeBuffer, data, u32(crc32(Buffer.concat([typeBuffer, data])) >>> 0)]);
}

function u32(value) {
  const buffer = Buffer.alloc(4);
  buffer.writeUInt32BE(value >>> 0);
  return buffer;
}

function crc32(buffer) {
  let crc = ~0;
  for (const byte of buffer) {
    crc ^= byte;
    for (let k = 0; k < 8; k++) crc = crc & 1 ? 0xedb88320 ^ (crc >>> 1) : crc >>> 1;
  }
  return ~crc;
}

function roundRect(x, y, rx, ry, w, h, r) {
  const cx = Math.max(rx + r, Math.min(x, rx + w - r));
  const cy = Math.max(ry + r, Math.min(y, ry + h - r));
  return (x - cx) ** 2 + (y - cy) ** 2 <= r ** 2 && x >= rx && y >= ry && x <= rx + w && y <= ry + h;
}

function circle(x, y, cx, cy, r) {
  return (x - cx) ** 2 + (y - cy) ** 2 <= r ** 2;
}

function line(x, y, x1, y1, x2, y2, width) {
  const a = x - x1;
  const b = y - y1;
  const c = x2 - x1;
  const d = y2 - y1;
  const dot = a * c + b * d;
  const len = c * c + d * d;
  const t = Math.max(0, Math.min(1, dot / len));
  const px = x1 + t * c;
  const py = y1 + t * d;
  return (x - px) ** 2 + (y - py) ** 2 <= width ** 2;
}

function poly(x, y, points) {
  let inside = false;
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const [xi, yi] = points[i];
    const [xj, yj] = points[j];
    const intersect = yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}

function farmTextPixel(x, y) {
  const scale = 7;
  const letters = {
    F: ["11111","10000","10000","11110","10000","10000","10000"],
    a: ["00000","01110","00001","01111","10001","10011","01101"],
    r: ["00000","10110","11001","10000","10000","10000","10000"],
    m: ["00000","11010","10101","10101","10101","10101","10101"]
  };
  const text = "Farm";
  let offset = 0;
  for (const ch of text) {
    const glyph = letters[ch];
    const gx = Math.floor((x - offset) / scale);
    const gy = Math.floor(y / scale);
    if (gx >= 0 && gx < 5 && gy >= 0 && gy < 7 && glyph[gy][gx] === "1") return true;
    offset += 6 * scale;
  }
  return false;
}
