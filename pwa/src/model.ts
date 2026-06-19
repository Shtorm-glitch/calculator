export type AccentTheme = "Mint" | "Ocean" | "Graphite" | "Berry";
export type TabId = "calc" | "saved" | "settings";

export type CalcInput = {
  salary: string;
  weight1: string;
  weight2: string;
  weight3: string;
  plan1: string;
  plan2: string;
  plan3: string;
  fact1: string;
  fact2: string;
  fact3: string;
};

export type CalcResult = {
  percent1: number;
  percent2: number;
  percent3: number;
  bonus1: number;
  bonus2: number;
  bonus3: number;
  beforeTax: number;
  afterTax: number;
};

export type SavedCalc = {
  name: string;
  savedAt: number;
  input: CalcInput;
  result: CalcResult;
};

export type FieldErrors = Partial<Record<keyof CalcInput, string>>;

export const defaultInput = (): CalcInput => ({
  salary: "10000",
  weight1: "60",
  weight2: "20",
  weight3: "20",
  plan1: "100000",
  plan2: "300000",
  plan3: "30",
  fact1: "100000",
  fact2: "300000",
  fact3: "30"
});
