import { APP_VERSION, DATA_SCHEMA_VERSION } from "./version";
import { defaultInput, type AccentTheme, type CalcInput, type SavedCalc } from "./model";

const keys = {
  input: "farm.input",
  saved: "farm.saved",
  dark: "farm.dark",
  accent: "farm.accent",
  activation: "farm.activatedVersion",
  latest: "farm.latestKnown",
  schema: "farm.dataSchema",
  pendingClearSaved: "farm.pendingClearSaved"
};

export type LatestKnown = {
  latest: string;
  releasedAt?: string;
  checkedAt: string;
};

export function loadInput(): CalcInput {
  return readJson(keys.input, defaultInput());
}

export function saveInput(input: CalcInput): void {
  writeJson(keys.input, input);
}

export function loadSaved(): SavedCalc[] {
  return readJson(keys.saved, []);
}

export function saveSaved(items: SavedCalc[]): void {
  writeJson(keys.saved, items);
}

export function loadDarkMode(): boolean {
  return localStorage.getItem(keys.dark) !== "false";
}

export function saveDarkMode(value: boolean): void {
  localStorage.setItem(keys.dark, String(value));
}

export function loadAccent(): AccentTheme {
  const value = localStorage.getItem(keys.accent);
  return value === "Mint" || value === "Ocean" || value === "Berry" || value === "Graphite" ? value : "Graphite";
}

export function saveAccent(value: AccentTheme): void {
  localStorage.setItem(keys.accent, value);
}

export function ensureLocalActivation(): void {
  if (location.hostname === "localhost" || location.hostname === "127.0.0.1" || location.protocol === "file:") {
    localStorage.setItem(keys.activation, APP_VERSION);
  }
}

export function applyActivationFromUrl(): boolean {
  const params = new URLSearchParams(location.search);
  const version = params.get("activate") || activationVersionFromPath();
  if (!version) return false;

  const previousVersion = localStorage.getItem(keys.activation);
  if (previousVersion !== version) {
    localStorage.setItem(keys.activation, version);
  }
  if (!shouldKeepActivationUrl()) {
    history.replaceState(null, "", location.pathname + location.hash);
  }
  return true;
}

export function loadActivatedVersion(): string | null {
  return localStorage.getItem(keys.activation);
}

export function saveLatestKnown(value: LatestKnown): void {
  writeJson(keys.latest, value);
}

export function loadLatestKnown(): LatestKnown | null {
  return readJson<LatestKnown | null>(keys.latest, null);
}

export function migrateSchema(): void {
  const stored = Number(localStorage.getItem(keys.schema) || DATA_SCHEMA_VERSION);
  if (stored !== DATA_SCHEMA_VERSION) {
    saveSaved([]);
  }
  localStorage.setItem(keys.schema, String(DATA_SCHEMA_VERSION));

  if (localStorage.getItem(keys.pendingClearSaved) === "true") {
    saveSaved([]);
    localStorage.removeItem(keys.pendingClearSaved);
  }
}

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) as T : fallback;
  } catch {
    return fallback;
  }
}

function writeJson(key: string, value: unknown): void {
  localStorage.setItem(key, JSON.stringify(value));
}

function shouldKeepActivationUrl(): boolean {
  const standalone = window.matchMedia("(display-mode: standalone)").matches || (navigator as Navigator & { standalone?: boolean }).standalone === true;
  const isiOS = /iPad|iPhone|iPod/i.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  return isiOS && !standalone;
}

function activationVersionFromPath(): string | null {
  const match = location.pathname.match(/\/app\/a\/(v\d+\.\d+\.\d+)\/?$/);
  return match?.[1] || null;
}
