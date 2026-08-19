// Palette deliberately matches the rest of the project - docs/architecture-diagram.svg
// and the in-game BuildScreen GUI - so the video reads as the same product.
export const theme = {
  bgDeep: "#0d1b2a",
  bgMid: "#132638",
  panel: "#16263a",
  border: "#3f6b8a",
  blue: "#5fa8d3",
  gold: "#ffd166",
  green: "#7ac74f",
  text: "#f1faee",
  muted: "#8fb3c9",
} as const;

export const fontStack =
  '"Segoe UI", -apple-system, BlinkMacSystemFont, "Helvetica Neue", Arial, sans-serif';

export const monoStack =
  '"SF Mono", "Consolas", "Menlo", "Roboto Mono", monospace';
