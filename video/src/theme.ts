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

// San Francisco - Apple's system typeface, present on this machine as
// SFNS.ttf and reached through -apple-system/system-ui. The previous
// stack led with "Segoe UI", which is a Windows font: on macOS it never
// resolved and everything silently fell back to a generic sans, which is
// a large part of why the type looked generic.
export const fontStack =
  '-apple-system, system-ui, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", Helvetica, sans-serif';

export const monoStack =
  'ui-monospace, "SF Mono", SFMono-Regular, Menlo, monospace';
