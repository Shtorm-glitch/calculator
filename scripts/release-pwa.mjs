import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import path from "node:path";
import readline from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { execFileSync, spawnSync } from "node:child_process";

const root = process.cwd();
const versionFile = path.join(root, "pwa", "src", "version.ts");
const text = readFileSync(versionFile, "utf8");
const version = text.match(/APP_VERSION\s*=\s*"([^"]+)"/)?.[1];
const releasedAt = text.match(/RELEASED_AT\s*=\s*"([^"]+)"/)?.[1];

if (!version || !releasedAt) {
  throw new Error("Cannot read APP_VERSION or RELEASED_AT from pwa/src/version.ts");
}

if (hasCommand("pnpm")) {
  execFileSync("pnpm", ["--dir", "pwa", "build"], { stdio: "inherit", shell: process.platform === "win32" });
} else {
  const vite = path.join(root, "pwa", "node_modules", ".bin", process.platform === "win32" ? "vite.cmd" : "vite");
  if (process.platform === "win32") {
    execFileSync("cmd", ["/c", vite, "build", "--configLoader", "native"], { cwd: path.join(root, "pwa"), stdio: "inherit" });
  } else {
    execFileSync(vite, ["build", "--configLoader", "native"], { cwd: path.join(root, "pwa"), stdio: "inherit" });
  }
}

const dist = path.join(root, "pwa", "dist");
const docs = path.join(root, "docs");
const app = path.join(docs, "app");
const versionDir = path.join(docs, "versions", version);

if (!existsSync(dist)) throw new Error("pwa/dist was not created");
mkdirSync(path.join(docs, "versions"), { recursive: true });
rmSync(app, { recursive: true, force: true });
rmSync(versionDir, { recursive: true, force: true });
cpSync(dist, app, { recursive: true });
cpSync(dist, versionDir, { recursive: true });

const rl = readline.createInterface({ input, output });
const answer = await rl.question(`Обновить docs/latest.json до ${version} (${releasedAt})? [y/N] `);
rl.close();

if (/^(y|yes|д|да)$/i.test(answer.trim())) {
  writeFileSync(path.join(docs, "latest.json"), `${JSON.stringify({ latest: version, releasedAt })}\n`);
  console.log("docs/latest.json updated");
} else {
  console.log("docs/latest.json skipped");
}

function hasCommand(command) {
  const checker = process.platform === "win32" ? "where" : "command";
  const args = process.platform === "win32" ? [command] : ["-v", command];
  return spawnSync(checker, args, { stdio: "ignore", shell: process.platform !== "win32" }).status === 0;
}
