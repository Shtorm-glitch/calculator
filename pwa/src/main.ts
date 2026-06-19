import {
  Calculator,
  Check,
  ChevronRight,
  Download,
  Edit3,
  List,
  Moon,
  Palette,
  Save,
  Settings,
  Sun,
  Trash2,
  X
} from "lucide";
import "./styles.css";
import { APP_VERSION } from "./version";
import {
  calculate,
  cleanDigits,
  defaultSaveName,
  formatGroupedDigits,
  formatRub,
  hasErrors,
  isValidKpi3Plan,
  mediumScaleCoefficient,
  scaleCoefficient,
  talkTimeRows,
  validate
} from "./calc";
import { defaultInput, type AccentTheme, type CalcInput, type CalcResult, type FieldErrors, type SavedCalc, type TabId } from "./model";
import {
  applyActivationFromUrl,
  ensureLocalActivation,
  loadAccent,
  loadActivatedVersion,
  loadDarkMode,
  loadInput,
  loadLatestKnown,
  loadSaved,
  migrateSchema,
  saveAccent,
  saveDarkMode,
  saveInput,
  saveLatestKnown,
  saveSaved
} from "./storage";

type LatestPayload = { latest: string; releasedAt?: string };

const accentColors: Record<AccentTheme, string> = {
  Mint: "#1FA67A",
  Ocean: "#1677B8",
  Graphite: "#7A8491",
  Berry: "#C13F7A"
};

const state = {
  tab: "calc" as TabId,
  input: loadInput(),
  saved: loadSaved(),
  darkMode: loadDarkMode(),
  accent: loadAccent(),
  latest: loadLatestKnown(),
  stale: false,
  modal: null as null | string,
  toast: "",
  installPrompt: null as any,
  installBannerDismissed: sessionStorage.getItem("farm.installBannerDismissed") === "true",
  lastValidKpi3Plan: loadInput().plan3 || "30"
};

const appRoot = document.querySelector<HTMLDivElement>("#app");
if (!appRoot) throw new Error("App root not found");
const app = appRoot;

localStorage.removeItem("farm.installBannerDismissed");
migrateSchema();
applyActivationFromUrl();
ensureLocalActivation();
registerServiceWorker();
window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  state.installPrompt = event;
  render();
});
window.addEventListener("appinstalled", () => {
  state.installPrompt = null;
  state.installBannerDismissed = true;
  sessionStorage.setItem("farm.installBannerDismissed", "true");
  render();
});
window.matchMedia("(display-mode: standalone)").addEventListener("change", () => render());
window.addEventListener("resize", () => fitToViewport());

checkVersion();
render();

function setInput(patch: Partial<CalcInput>): void {
  state.input = { ...state.input, ...patch };
  if (isValidKpi3Plan(state.input.plan3)) state.lastValidKpi3Plan = state.input.plan3;
  saveInput(state.input);
  render();
}

function updateInputSilently(patch: Partial<CalcInput>): void {
  state.input = { ...state.input, ...patch };
  if (isValidKpi3Plan(state.input.plan3)) state.lastValidKpi3Plan = state.input.plan3;
  saveInput(state.input);
}

function setTab(tab: TabId): void {
  state.tab = tab;
  render();
}

function errors(): FieldErrors {
  return validate(state.input);
}

function result(): CalcResult | null {
  const currentErrors = errors();
  return hasErrors(currentErrors) ? null : calculate(state.input);
}

function render(): void {
  document.documentElement.dataset.theme = state.darkMode ? "dark" : "light";
  document.documentElement.style.setProperty("--accent", accentColors[state.accent]);
  const content = activatedView() ?? appView();
  app.innerHTML = content;
  mountIcons();
  bindEvents();
  fitToViewport();
}

function fitToViewport(): void {
  window.requestAnimationFrame(() => {
    const shell = document.querySelector<HTMLElement>(".shell");
    if (!shell) return;
    const shouldScale = isStandaloneMode() && window.matchMedia("(min-width: 720px)").matches && !state.modal;
    shell.style.setProperty("--app-scale", "1");
    document.body.classList.remove("is-scaled");
    if (!shouldScale) return;

    const height = shell.scrollHeight;
    const available = window.innerHeight - 4;
    if (height <= available) return;

    const scale = Math.min(1, available / height);
    shell.style.setProperty("--app-scale", scale.toFixed(3));
    document.body.classList.add("is-scaled");
  });
}

function activatedView(): string | null {
  if (loadActivatedVersion()) return null;
  return `
    <main class="activation">
      <img class="activation__icon" src="./icons/icon-192.png" alt="" />
      <h1>Farm</h1>
      <p>Калькулятор не активирован - обратись к TL</p>
    </main>
  `;
}

function appView(): string {
  return `
    <div class="shell">
      ${state.stale ? `<div class="stale-banner">Калькулятор устарел - обратись к TL</div>` : ""}
      ${installBanner()}
      <header class="topbar">
        <div>
          <p class="eyebrow">Farm</p>
          <h1>${tabTitle(state.tab)}</h1>
        </div>
        <img class="app-icon" src="./icons/icon-180.png" alt="" />
      </header>
      <section class="content">${screen()}</section>
      <nav class="bottom-nav">
        ${navButton("calc", "Расчёт", "calculator")}
        ${navButton("saved", "Сохранённые", "list")}
        ${navButton("settings", "Настройки", "settings")}
      </nav>
      ${modalView()}
      ${state.toast ? `<div class="toast">${escapeHtml(state.toast)}</div>` : ""}
    </div>
  `;
}

function installBanner(): string {
  if (state.installBannerDismissed || isStandaloneMode()) return "";
  return `
    <div class="install-banner">
      <button type="button" data-action="install-pwa">Установить как приложение</button>
      <button type="button" class="install-banner__close" data-action="dismiss-install-banner" aria-label="Скрыть"><i data-icon="x"></i></button>
    </div>
  `;
}

function screen(): string {
  if (state.tab === "saved") return savedScreen();
  if (state.tab === "settings") return settingsScreen();
  return calcScreen();
}

function calcScreen(): string {
  const currentErrors = errors();
  const currentResult = hasErrors(currentErrors) ? null : calculate(state.input);
  return `
    <div class="stack">
      <button class="salary-card" data-modal="salary">
        <span>Оклад</span>
        <strong>${formatRub(Number(state.input.salary || 0))}</strong>
        <i data-icon="chevron-right"></i>
      </button>
      <button class="weights-card" data-modal="weights">
        <span>Веса KPI</span>
        <div class="weights-grid">
          ${weightCell("KPI 1", state.input.weight1)}
          ${weightCell("KPI 2", state.input.weight2)}
          ${weightCell("KPI 3", state.input.weight3)}
        </div>
      </button>
      <div class="kpi-grid">
        ${kpiCard("KPI 1 AV", "1", state.input.plan1, state.input.fact1, currentErrors.plan1 || currentErrors.fact1)}
        ${kpiCard("KPI 2 total", "2", state.input.plan2, state.input.fact2, currentErrors.plan2 || currentErrors.fact2)}
        ${kpiCard("KPI 3 talk time", "3", state.input.plan3, state.input.fact3, currentErrors.plan3 || currentErrors.fact3)}
      </div>
      ${currentErrors.weight1 ? `<div class="error-card">${currentErrors.weight1}</div>` : ""}
      ${resultPanel(currentResult)}
    </div>
  `;
}

function savedScreen(): string {
  if (state.saved.length === 0) {
    return `<div class="empty">Сохранённых расчётов пока нет</div>`;
  }
  return `
    <div class="saved-list">
      ${state.saved.map((item, index) => `
        <button class="saved-row" data-open-saved="${index}">
          <span>${escapeHtml(item.name)}</span>
          <i data-icon="chevron-right"></i>
        </button>
      `).join("")}
    </div>
  `;
}

function settingsScreen(): string {
  return `
    <div class="settings-list">
      <section class="settings-group">
        <div class="settings-row">
          <span>Тема</span>
          <button class="icon-toggle" data-action="toggle-theme" aria-label="Переключить тему">
            <i data-icon="${state.darkMode ? "moon" : "sun"}"></i>
          </button>
        </div>
        <div class="accent-grid">
          ${(["Mint", "Ocean", "Graphite", "Berry"] as AccentTheme[]).map((theme) => `
            <button class="accent-choice ${state.accent === theme ? "is-active" : ""}" data-accent="${theme}">
              <span style="background:${accentColors[theme]}"></span>${theme}
            </button>
          `).join("")}
        </div>
      </section>
      <section class="settings-group">
        <div class="settings-note">Чтобы установить Farm на iPhone: Поделиться → На экран «Домой»</div>
        ${state.installPrompt ? `
          <div class="settings-note install-note">
            <span>Farm можно установить как приложение на этом устройстве</span>
            <button type="button" class="primary small" data-action="install-pwa">Установить Farm</button>
          </div>
        ` : ""}
      </section>
      <section class="settings-group">
        <div class="settings-row"><span>Версия</span><strong>${APP_VERSION}</strong></div>
      </section>
    </div>
  `;
}

function resultPanel(current: CalcResult | null): string {
  if (!current) {
    return `<section class="result-card muted">Заполните поля корректно, чтобы увидеть расчёт</section>`;
  }
  return `
    <section class="result-card">
      ${resultLine("KPI 1", `${current.percent1}%`, formatRub(current.bonus1), false, {
        scale: { label: "Шкала", modal: "scale-soft" },
        result: { modal: "result-soft" }
      })}
      ${resultLine("KPI 2", `${current.percent2}%`, formatRub(current.bonus2), false, {
        scale: { label: "Шкала", modal: "scale-medium" },
        result: { modal: "result-medium" }
      })}
      ${resultLine("KPI 3", `${current.percent3}%`, formatRub(current.bonus3), false, {
        scale: { label: "Шкала", modal: "scale-soft" },
        result: { modal: "result-kpi3" }
      })}
      <hr />
      ${resultLine("Премия до налога", "", formatRub(current.beforeTax), true)}
      ${resultLine("За вычетом 13%", "", formatRub(current.afterTax), true)}
      <button class="primary save-button" data-modal="save"><i data-icon="save"></i>Сохранить расчёт</button>
    </section>
  `;
}

function modalView(): string {
  if (!state.modal) return "";
  if (state.modal === "salary") return inputModal("Оклад", "salary", state.input.salary, errors().salary);
  if (state.modal === "weights") return weightsModal();
  if (state.modal === "kpi1") return kpiModal("KPI 1 AV", "plan1", "fact1");
  if (state.modal === "kpi2") return kpiModal("KPI 2 total", "plan2", "fact2");
  if (state.modal === "kpi3") return kpi3Modal();
  if (state.modal === "scale-soft") return scaleModal("Шкала пологая", softScaleRows);
  if (state.modal === "scale-medium") return scaleModal("Средняя шкала мотивации", mediumScaleRows);
  if (state.modal === "result-soft") return resultScaleModal("KPI 1 AV", "result-soft");
  if (state.modal === "result-medium") return resultScaleModal("KPI 2 total", "result-medium");
  if (state.modal === "result-kpi3") return resultScaleModal("KPI 3", "result-kpi3");
  if (state.modal === "save") return saveModal();
  if (state.modal.startsWith("saved:")) return savedDetailsModal(Number(state.modal.split(":")[1]));
  if (state.modal.startsWith("delete:")) return deleteModal(Number(state.modal.split(":")[1]));
  if (state.modal === "replace") return replaceModal();
  return "";
}

function inputModal(title: string, field: keyof CalcInput, value: string, error?: string): string {
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header><h2>${title}</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        <label class="field">
          <input inputmode="numeric" data-field="${field}" value="${formatGroupedDigits(value)}" autocomplete="off" />
          ${error ? `<small>${escapeHtml(error)}</small>` : ""}
        </label>
        <button class="primary" data-close>Готово</button>
      </section>
    </div>
  `;
}

function weightsModal(): string {
  const currentErrors = errors();
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header><h2>Веса KPI</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        ${numberField("KPI 1 AV, %", "weight1", state.input.weight1, currentErrors.weight1)}
        ${numberField("KPI 2 total, %", "weight2", state.input.weight2, currentErrors.weight2)}
        ${numberField("KPI 3 talk time, %", "weight3", state.input.weight3, currentErrors.weight3)}
        <button class="primary" data-close>Готово</button>
      </section>
    </div>
  `;
}

function kpiModal(title: string, planField: keyof CalcInput, factField: keyof CalcInput): string {
  const currentErrors = errors();
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header><h2>${title}</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        ${numberField("План", planField, state.input[planField], currentErrors[planField])}
        ${numberField("Факт", factField, state.input[factField], currentErrors[factField])}
        <button class="primary" data-close>Готово</button>
      </section>
    </div>
  `;
}

function kpi3Modal(): string {
  const currentErrors = errors();
  const planInvalid = Boolean(currentErrors.plan3);
  return `
    <div class="modal-backdrop">
      <section class="sheet tall">
        <header><h2>KPI 3 talk time</h2><button class="icon-button" data-close-kpi3><i data-icon="x"></i></button></header>
        ${numberField("План", "plan3", state.input.plan3, currentErrors.plan3, false)}
        <div class="talk-list">
          ${talkTimeRows.slice().reverse().map(({ calls, minutes }) => `
            <button ${planInvalid ? "disabled" : ""} class="${state.input.fact3 === String(calls) ? "is-active" : ""}" data-kpi3-fact="${calls}">
              <span>${calls}</span><strong>${minutes}</strong>
            </button>
          `).join("")}
          <button ${planInvalid ? "disabled" : ""} data-manual-kpi3>Ввести вручную</button>
        </div>
      </section>
    </div>
  `;
}

function saveModal(): string {
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header><h2>Название</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        <label class="field">
          <input data-save-name maxlength="30" value="${escapeAttr(defaultSaveName())}" autocomplete="off" />
          <small data-save-error></small>
        </label>
        <button class="primary" data-action="confirm-save">Сохранить</button>
      </section>
    </div>
  `;
}

function replaceModal(): string {
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header><h2>Выберите расчёт для замены</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        <div class="replace-list">
          ${state.saved.map((item, index) => `<button data-replace-index="${index}">${escapeHtml(item.name)}</button>`).join("")}
        </div>
        <button class="secondary" data-modal="save">Назад</button>
      </section>
    </div>
  `;
}

function savedDetailsModal(index: number): string {
  const item = state.saved[index];
  if (!item) return "";
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header>
          <h2>${escapeHtml(item.name)}</h2>
          <button class="icon-button" data-close><i data-icon="x"></i></button>
        </header>
        <div class="details">
          ${resultLine("KPI 1 AV", `${item.result.percent1}%`, formatRub(item.result.bonus1))}
          ${resultLine("KPI 2 тотал revenue", `${item.result.percent2}%`, formatRub(item.result.bonus2))}
          ${resultLine("KPI 3 talk time", `${item.result.percent3}%`, formatRub(item.result.bonus3))}
          <hr />
          ${resultLine("Премия до налога", "", formatRub(item.result.beforeTax), true)}
          ${resultLine("За вычетом 13%", "", formatRub(item.result.afterTax), true)}
        </div>
        <div class="modal-actions">
          <button class="primary" data-edit-saved="${index}"><i data-icon="edit-3"></i>Изменить</button>
          <button class="danger" data-modal="delete:${index}"><i data-icon="trash-2"></i>Удалить</button>
        </div>
      </section>
    </div>
  `;
}

function deleteModal(index: number): string {
  const item = state.saved[index];
  if (!item) return "";
  return `
    <div class="modal-backdrop">
      <section class="sheet">
        <header><h2>Удалить расчёт?</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        <p class="confirm-name">${escapeHtml(item.name)}</p>
        <div class="modal-actions">
          <button class="danger" data-delete-saved="${index}">Удалить</button>
          <button class="secondary" data-close>Отмена</button>
        </div>
      </section>
    </div>
  `;
}

type ScaleRow = {
  range: string;
  target: string;
  tone: "positive" | "neutral" | "negative";
};

const softScaleRows: ScaleRow[] = [
  { range: "121% и более", target: "0,5% за каждый % перевыполнения", tone: "positive" },
  { range: "115-120%", target: "2% за каждый % перевыполнения", tone: "positive" },
  { range: "101-114%", target: "3% за каждый % перевыполнения", tone: "positive" },
  { range: "100%", target: "100%", tone: "neutral" },
  { range: "90-99%", target: "-3% за каждый % недовыполнения", tone: "negative" },
  { range: "85-89%", target: "-2% за каждый % недовыполнения", tone: "negative" },
  { range: "80-84%", target: "35%", tone: "negative" },
  { range: "75-79%", target: "25%", tone: "negative" },
  { range: "70-74%", target: "20%", tone: "negative" },
  { range: "69% и менее", target: "0%", tone: "negative" }
];

const mediumScaleRows: ScaleRow[] = [
  { range: "130% и более", target: "0,5% за каждый % перевыполнения", tone: "positive" },
  { range: "121-129%", target: "1% за каждый % перевыполнения", tone: "positive" },
  { range: "111-120%", target: "3% за каждый % перевыполнения", tone: "positive" },
  { range: "106-110%", target: "4% за каждый % перевыполнения", tone: "positive" },
  { range: "101-105%", target: "5% за каждый % перевыполнения", tone: "positive" },
  { range: "100%", target: "100%", tone: "neutral" },
  { range: "95-99%", target: "-5% за каждый % недовыполнения", tone: "negative" },
  { range: "90-94%", target: "-4% за каждый % недовыполнения", tone: "negative" },
  { range: "85-89%", target: "30%", tone: "negative" },
  { range: "80-84%", target: "20%", tone: "negative" },
  { range: "79% и менее", target: "0%", tone: "negative" }
];

function scaleModal(title: string, rows: ScaleRow[]): string {
  return `
    <div class="modal-backdrop">
      <section class="sheet scale-sheet">
        <header><h2>${title}</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        <div class="scale-table" role="table" aria-label="${escapeAttr(title)}">
          <div class="scale-table__head" role="row">
            <span role="columnheader">% выполнения</span>
            <span role="columnheader">% от целевой</span>
          </div>
          ${rows.map((row) => `
            <div class="scale-table__row is-${row.tone}" role="row">
              <span role="cell">${row.range}</span>
              <span role="cell">${row.target}</span>
            </div>
          `).join("")}
        </div>
      </section>
    </div>
  `;
}

type ResultScaleKind = "result-soft" | "result-medium" | "result-kpi3";
type ResultScaleRow = {
  percent: number | null;
  target: string;
  bonus: string;
  current: boolean;
  tone: "positive" | "neutral" | "negative" | "gap";
};

function resultScaleModal(title: string, kind: ResultScaleKind): string {
  const current = result();
  if (!current) return "";
  const isSoft = kind === "result-soft";
  const isKpi3 = kind === "result-kpi3";
  const currentPercent = isKpi3 ? current.percent3 : isSoft ? current.percent1 : current.percent2;
  const weight = isKpi3 ? state.input.weight3 : isSoft ? state.input.weight1 : state.input.weight2;
  const base = Number(state.input.salary || 0) * Number(weight) / 100;
  const rows = buildResultScaleRows(currentPercent, base, isSoft || isKpi3 ? scaleCoefficient : mediumScaleCoefficient);

  return `
    <div class="modal-backdrop">
      <section class="sheet scale-sheet">
        <header><h2>${title}: мой результат</h2><button class="icon-button" data-close><i data-icon="x"></i></button></header>
        <div class="result-scale-table" role="table" aria-label="${escapeAttr(title)} мой результат">
          <div class="result-scale-table__head" role="row">
            <span role="columnheader">% выполнения</span>
            <span role="columnheader">% от целевой</span>
            <span role="columnheader">Сумма премии</span>
          </div>
          ${rows.map((row) => row.tone === "gap" ? `
            <div class="result-scale-table__gap" role="row"><span role="cell">...</span></div>
          ` : `
            <div class="result-scale-table__row is-${row.tone} ${row.current ? "is-current" : ""}" role="row" data-result-current="${row.current}">
              <span role="cell">${row.percent}%</span>
              <span role="cell">${row.target}</span>
              <span role="cell">${row.bonus}</span>
            </div>
          `).join("")}
        </div>
      </section>
    </div>
  `;
}

function buildResultScaleRows(
  currentPercent: number,
  base: number,
  coefficient: (ratio: number) => number
): ResultScaleRow[] {
  const standard = Array.from({ length: 53 }, (_, index) => 121 - index).map((percent) =>
    makeResultScaleRow(percent, base, coefficient, percent === currentPercent)
  );

  if (currentPercent > 121) {
    return [
      makeResultScaleRow(currentPercent, base, coefficient, true),
      { percent: null, target: "", bonus: "", current: false, tone: "gap" },
      ...standard
    ];
  }

  if (currentPercent < 69) {
    return [
      ...standard,
      { percent: null, target: "", bonus: "", current: false, tone: "gap" },
      makeResultScaleRow(currentPercent, base, coefficient, true)
    ];
  }

  return standard;
}

function makeResultScaleRow(
  percent: number,
  base: number,
  coefficient: (ratio: number) => number,
  current: boolean
): ResultScaleRow {
  const target = Math.floor(coefficient(percent / 100) * 100);
  return {
    percent,
    target: `${target}%`,
    bonus: formatRub(Math.floor(base * coefficient(percent / 100))),
    current,
    tone: percent > 100 ? "positive" : percent === 100 ? "neutral" : "negative"
  };
}

function bindEvents(): void {
  document.querySelectorAll<HTMLElement>(".modal-backdrop").forEach((el) => {
    el.addEventListener("click", (event) => {
      if (event.target === el) closeActiveModal();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-tab]").forEach((el) => {
    el.addEventListener("click", () => setTab(el.dataset.tab as TabId));
  });
  document.querySelectorAll<HTMLElement>("[data-modal]").forEach((el) => {
    el.addEventListener("click", () => {
      state.modal = el.dataset.modal || null;
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-close]").forEach((el) => {
    el.addEventListener("click", closeModal);
  });
  document.querySelectorAll<HTMLInputElement>("[data-field]").forEach((el) => {
    el.addEventListener("input", () => {
      updateInputSilently({ [el.dataset.field as keyof CalcInput]: cleanDigits(el.value) } as Partial<CalcInput>);
    });
    el.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        closeActiveModal();
      }
    });
  });
  document.querySelectorAll<HTMLElement>("[data-close-kpi3]").forEach((el) => {
    el.addEventListener("click", () => {
      if (!isValidKpi3Plan(state.input.plan3)) setInput({ plan3: state.lastValidKpi3Plan || "30" });
      closeModal();
    });
  });
  document.querySelectorAll<HTMLButtonElement>("[data-kpi3-fact]").forEach((el) => {
    el.addEventListener("click", () => setInput({ fact3: el.dataset.kpi3Fact || state.input.fact3 }));
  });
  document.querySelectorAll<HTMLElement>("[data-manual-kpi3]").forEach((el) => {
    el.addEventListener("click", () => {
      const value = prompt("Факт KPI 3", state.input.fact3);
      if (value != null) setInput({ fact3: cleanDigits(value) });
    });
  });
  document.querySelectorAll<HTMLElement>("[data-open-saved]").forEach((el) => {
    el.addEventListener("click", () => {
      state.modal = `saved:${el.dataset.openSaved}`;
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-edit-saved]").forEach((el) => {
    el.addEventListener("click", () => {
      const item = state.saved[Number(el.dataset.editSaved)];
      if (!item) return;
      state.input = item.input;
      saveInput(state.input);
      state.tab = "calc";
      state.modal = null;
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-delete-saved]").forEach((el) => {
    el.addEventListener("click", () => {
      state.saved = state.saved.filter((_, index) => index !== Number(el.dataset.deleteSaved));
      saveSaved(state.saved);
      state.modal = null;
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-replace-index]").forEach((el) => {
    el.addEventListener("click", () => replaceSaved(Number(el.dataset.replaceIndex)));
  });
  document.querySelectorAll<HTMLElement>("[data-action='confirm-save']").forEach((el) => {
    el.addEventListener("click", confirmSave);
  });
  document.querySelectorAll<HTMLElement>("[data-action='toggle-theme']").forEach((el) => {
    el.addEventListener("click", () => {
      state.darkMode = !state.darkMode;
      saveDarkMode(state.darkMode);
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-accent]").forEach((el) => {
    el.addEventListener("click", () => {
      state.accent = el.dataset.accent as AccentTheme;
      saveAccent(state.accent);
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-action='install-pwa']").forEach((el) => {
    el.addEventListener("click", async (event) => {
      event.preventDefault();
      event.stopPropagation();
      if (!state.installPrompt) {
        showToast(installFallbackMessage());
        return;
      }
      state.installPrompt.prompt();
      await state.installPrompt.userChoice;
      state.installPrompt = null;
      state.installBannerDismissed = true;
      sessionStorage.setItem("farm.installBannerDismissed", "true");
      render();
    });
  });
  document.querySelectorAll<HTMLElement>("[data-action='dismiss-install-banner']").forEach((el) => {
    el.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      state.installBannerDismissed = true;
      sessionStorage.setItem("farm.installBannerDismissed", "true");
      render();
    });
  });
  window.requestAnimationFrame(() => {
    document.querySelector<HTMLElement>("[data-result-current='true']")?.scrollIntoView({
      block: "center",
      inline: "nearest"
    });
  });
}

function confirmSave(): void {
  const nameInput = document.querySelector<HTMLInputElement>("[data-save-name]");
  const errorEl = document.querySelector<HTMLElement>("[data-save-error]");
  const name = (nameInput?.value || "").trim();
  const message = !name
    ? "Введите название"
    : state.saved.some((item) => item.name === name)
      ? "Такое название уже есть"
      : "";
  if (message) {
    if (errorEl) errorEl.textContent = message;
    return;
  }
  const current = result();
  if (!current) return;
  const item = { name, savedAt: Date.now(), input: state.input, result: current };
  if (state.saved.length < 3) {
    state.saved = [...state.saved, item];
    saveSaved(state.saved);
    state.modal = null;
    showToast("Расчёт сохранён");
  } else {
    sessionStorage.setItem("farm.pendingSave", JSON.stringify(item));
    state.modal = "replace";
  }
  render();
}

function replaceSaved(index: number): void {
  const raw = sessionStorage.getItem("farm.pendingSave");
  if (!raw) return;
  const item = JSON.parse(raw) as SavedCalc;
  state.saved = state.saved.map((saved, savedIndex) => savedIndex === index ? item : saved);
  saveSaved(state.saved);
  sessionStorage.removeItem("farm.pendingSave");
  state.modal = null;
  showToast("Расчёт сохранён");
  render();
}

function closeModal(): void {
  state.modal = null;
  render();
}

function closeActiveModal(): void {
  if (state.modal === "kpi3" && !isValidKpi3Plan(state.input.plan3)) {
    updateInputSilently({ plan3: state.lastValidKpi3Plan || "30" });
  }
  closeModal();
}

function showToast(message: string): void {
  state.toast = message;
  window.setTimeout(() => {
    state.toast = "";
    render();
  }, 1800);
}

async function checkVersion(): Promise<void> {
  try {
    const response = await fetch("../latest.json", { cache: "no-store" });
    if (!response.ok) throw new Error("latest unavailable");
    const latest = await response.json() as LatestPayload;
    state.latest = { ...latest, checkedAt: new Date().toISOString() };
    saveLatestKnown(state.latest);
  } catch {
    state.latest = loadLatestKnown();
  }
  state.stale = state.latest ? isMajorMinorNewer(APP_VERSION, state.latest.latest) : false;
  if (state.latest && isPatchNewer(APP_VERSION, state.latest.latest)) {
    await reloadForPatch();
  }
  render();
}

async function reloadForPatch(): Promise<void> {
  try {
    const registrations = await navigator.serviceWorker?.getRegistrations?.() ?? [];
    await Promise.all(registrations.map((registration) => registration.update()));
    location.reload();
  } catch {
    // Keep current version and retry on next launch.
  }
}

function isMajorMinorNewer(current: string, latest: string): boolean {
  const c = parseVersion(current);
  const l = parseVersion(latest);
  return l.major > c.major || (l.major === c.major && l.minor > c.minor);
}

function isPatchNewer(current: string, latest: string): boolean {
  const c = parseVersion(current);
  const l = parseVersion(latest);
  return l.major === c.major && l.minor === c.minor && l.patch > c.patch;
}

function parseVersion(version: string): { major: number; minor: number; patch: number } {
  const [, major = "0", minor = "0", patch = "0"] = version.match(/^v?(\d+)\.(\d+)\.(\d+)$/) || [];
  return { major: Number(major), minor: Number(minor), patch: Number(patch) };
}

function registerServiceWorker(): void {
  if ("serviceWorker" in navigator && location.protocol !== "file:") {
    window.addEventListener("load", () => navigator.serviceWorker.register("./service-worker.js"));
  }
}

function isStandaloneMode(): boolean {
  return window.matchMedia("(display-mode: standalone)").matches || (navigator as Navigator & { standalone?: boolean }).standalone === true;
}

function mountIcons(): void {
  const icons = { Calculator, List, Settings, Save, Trash2, Edit3, X, ChevronRight, Moon, Sun, Palette, Check, Download };
  document.querySelectorAll<HTMLElement>("[data-icon]").forEach((el) => {
    const name = (el.dataset.icon || "").replace(/-([a-z0-9])/g, (_, letter) => letter.toUpperCase());
    const icon = icons[name.charAt(0).toUpperCase() + name.slice(1) as keyof typeof icons];
    if (icon) el.innerHTML = iconToSvg(icon);
  });
}

function iconToSvg(icon: unknown): string {
  const [, attrs, children] = icon as [string, Record<string, string | number>, Array<[string, Record<string, string | number>]>];
  const merged = { ...attrs, width: 20, height: 20, "stroke-width": 2.1 };
  const attrText = Object.entries(merged).map(([key, value]) => `${key}="${String(value)}"`).join(" ");
  const childText = children.map(([tag, childAttrs]) => {
    const childAttrText = Object.entries(childAttrs).map(([key, value]) => `${key}="${String(value)}"`).join(" ");
    return `<${tag} ${childAttrText}></${tag}>`;
  }).join("");
  return `<svg ${attrText}>${childText}</svg>`;
}

function navButton(tab: TabId, label: string, icon: string): string {
  return `<button class="${state.tab === tab ? "is-active" : ""}" data-tab="${tab}"><i data-icon="${icon}"></i><span>${label}</span></button>`;
}

function tabTitle(tab: TabId): string {
  return tab === "calc" ? "Расчёт" : tab === "saved" ? "Сохранённые" : "Настройки";
}

function weightCell(label: string, value: string): string {
  return `<div><small>${label}</small><strong>${escapeHtml(value)}%</strong></div>`;
}

function kpiCard(title: string, index: string, plan: string, fact: string, error?: string): string {
  return `
    <button class="kpi-card ${error ? "has-error" : ""}" data-modal="kpi${index}">
      <strong>${title}</strong>
      <div><span>План</span><b>${formatGroupedDigits(plan)}</b></div>
      <div><span>Факт</span><b>${formatGroupedDigits(fact)}</b></div>
      ${error ? `<small>${escapeHtml(error)}</small>` : ""}
    </button>
  `;
}

function numberField(label: string, field: keyof CalcInput, value: string, error?: string, grouped = true): string {
  return `
    <label class="field">
      <span>${label}</span>
      <input inputmode="numeric" data-field="${field}" value="${grouped ? formatGroupedDigits(value) : escapeAttr(value)}" autocomplete="off" />
      ${error ? `<small>${escapeHtml(error)}</small>` : ""}
    </label>
  `;
}

function resultLine(
  label: string,
  middle: string,
  value: string,
  strong = false,
  actions?: {
    scale?: { label: string; modal: string };
    result?: { modal: string };
  }
): string {
  return `
    <div class="result-line ${strong ? "strong" : ""} ${actions ? "has-actions" : ""}">
      ${actions ? `
        <button class="result-kpi-button" data-modal="${actions.result?.modal || ""}">
          <span>${label}</span><strong>$</strong>
        </button>
        <span class="result-actions">
          ${actions.scale ? `<button class="scale-link" data-modal="${actions.scale.modal}">${actions.scale.label}</button>` : ""}
        </span>
      ` : `<span>${label}</span><span></span>`}
      <em>${middle}</em>
      <b>${value}</b>
    </div>
  `;
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char] || char);
}

function escapeAttr(value: string): string {
  return escapeHtml(value);
}

function installFallbackMessage(): string {
  const ua = navigator.userAgent;
  if (/Android/i.test(ua)) return "В Chrome: меню ⋮ → Добавить на главный экран";
  return "Чтобы установить Farm на iPhone: Поделиться → На экран «Домой»";
}
