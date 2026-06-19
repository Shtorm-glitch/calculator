import type { CalcInput, CalcResult, FieldErrors } from "./model";

const MAX_NUMBER = 1_000_000_000;

export const talkTimeRows = Array.from({ length: 16 }, (_, index) => {
  const calls = 20 + index;
  return { calls, minutes: 130 + index * 5 };
});

export function cleanDigits(text: string): string {
  const digits = text.replace(/\D/g, "").slice(0, 10);
  const number = Number(digits || "0");
  return number > MAX_NUMBER ? String(MAX_NUMBER) : digits;
}

export function formatGroupedDigits(value: string): string {
  const digits = value.replace(/\D/g, "");
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
}

export function formatRub(value: number): string {
  return `${Math.floor(value).toLocaleString("ru-RU")} ₽`;
}

export function defaultSaveName(date = new Date()): string {
  const formatter = new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
  return formatter.format(date).replace(",", "");
}

export function kpi3PlanError(value: string): string | undefined {
  const number = Number(value);
  return Number.isInteger(number) && number >= 1 && number <= 99
    ? undefined
    : "Введите целое число от 1 до 99";
}

export function isValidKpi3Plan(value: string): boolean {
  return !kpi3PlanError(value);
}

export function validate(input: CalcInput): FieldErrors {
  const positive = (value: string, label: string): string | undefined => {
    if (!/^\d+$/.test(value)) return `${label}: заполните поле`;
    const number = Number(value);
    if (number <= 0) return `${label}: должно быть больше 0`;
    if (number > MAX_NUMBER) return `${label}: максимум 1 000 000 000`;
    return undefined;
  };

  const weightRange = (value: string, label: string): string | undefined => {
    if (!/^\d+$/.test(value)) return `${label}: заполните поле`;
    const number = Number(value);
    return number >= 1 && number <= 100 ? undefined : `${label}: от 1 до 100%`;
  };

  const weight1 = positive(input.weight1, "Вес KPI 1") ?? weightRange(input.weight1, "Вес KPI 1");
  const weight2 = positive(input.weight2, "Вес KPI 2") ?? weightRange(input.weight2, "Вес KPI 2");
  const weight3 = positive(input.weight3, "Вес KPI 3") ?? weightRange(input.weight3, "Вес KPI 3");
  const sum = Number(input.weight1 || 0) + Number(input.weight2 || 0) + Number(input.weight3 || 0);
  const sumError = !weight1 && !weight2 && !weight3 && sum !== 100 ? "Сумма весов должна быть 100%" : undefined;

  return {
    salary: positive(input.salary, "Оклад"),
    weight1: weight1 ?? sumError,
    weight2: weight2 ?? sumError,
    weight3: weight3 ?? sumError,
    plan1: positive(input.plan1, "План KPI 1"),
    plan2: positive(input.plan2, "План KPI 2"),
    plan3: kpi3PlanError(input.plan3),
    fact1: positive(input.fact1, "Факт KPI 1"),
    fact2: positive(input.fact2, "Факт KPI 2"),
    fact3: positive(input.fact3, "Факт KPI 3")
  };
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.values(errors).some(Boolean);
}

export function calculate(input: CalcInput): CalcResult {
  const salary = Number(input.salary);
  const ratio = (fact: string, plan: string) => Math.floor((Number(fact) / Number(plan)) * 100) / 100;

  const ratio1 = ratio(input.fact1, input.plan1);
  const ratio2 = ratio(input.fact2, input.plan2);
  const ratio3 = ratio(input.fact3, input.plan3);
  const bonus1 = Math.floor((salary * Number(input.weight1)) / 100 * scaleCoefficient(ratio1));
  const bonus2 = Math.floor((salary * Number(input.weight2)) / 100 * mediumScaleCoefficient(ratio2));
  const bonus3 = Math.floor((salary * Number(input.weight3)) / 100 * scaleCoefficient(ratio3));
  const beforeTax = bonus1 + bonus2 + bonus3;

  return {
    percent1: Math.floor(ratio1 * 100),
    percent2: Math.floor(ratio2 * 100),
    percent3: Math.floor(ratio3 * 100),
    bonus1,
    bonus2,
    bonus3,
    beforeTax,
    afterTax: Math.floor(beforeTax * 0.87)
  };
}

export function scaleCoefficient(ratio: number): number {
  if (ratio <= 0.69) return 0.0;
  if (ratio >= 0.70 && ratio <= 0.74) return 0.20;
  if (ratio >= 0.75 && ratio <= 0.79) return 0.25;
  if (ratio >= 0.80 && ratio <= 0.84) return 0.35;
  if (ratio >= 0.85 && ratio <= 0.89) return 1.0 - (1.0 - ratio) * 2.0;
  if (ratio >= 0.90 && ratio <= 0.99) return 1.0 - (1.0 - ratio) * 3.0;
  if (ratio >= 1.00 && ratio <= 1.14) return 1.0 + (ratio - 1.0) * 3.0;
  if (ratio >= 1.15 && ratio <= 1.20) return 1.42 + (ratio - 1.14) * 2.0;
  return 1.54 + (ratio - 1.20) * 0.5;
}

export function mediumScaleCoefficient(ratio: number): number {
  if (ratio <= 0.79) return 0.0;
  if (ratio >= 0.80 && ratio <= 0.84) return 0.20;
  if (ratio >= 0.85 && ratio <= 0.89) return 0.30;
  if (ratio >= 0.90 && ratio <= 0.94) return 1.0 - (1.0 - ratio) * 4.0;
  if (ratio >= 0.95 && ratio <= 0.99) return 1.0 - (1.0 - ratio) * 5.0;
  if (ratio >= 1.00 && ratio <= 1.05) return 1.0 + (ratio - 1.0) * 5.0;
  if (ratio >= 1.06 && ratio <= 1.10) return 1.25 + (ratio - 1.05) * 4.0;
  if (ratio >= 1.11 && ratio <= 1.20) return 1.45 + (ratio - 1.10) * 3.0;
  if (ratio >= 1.21 && ratio <= 1.29) return 1.75 + (ratio - 1.20);
  return 1.84 + (ratio - 1.29) * 0.5;
}
