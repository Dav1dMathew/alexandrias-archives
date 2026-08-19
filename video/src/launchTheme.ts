// Black / orange palette for the launch cut. Warm near-black rather than
// pure #000 so the grain and the orange bloom have something to sit on -
// pure black crushes both and reads flat.
export const launch = {
  bg: "#0a0806",
  bgDeep: "#050403",
  surface: "#15100c",

  text: "#f7f2ec",
  textSoft: "#b3a698",
  textFaint: "#6d6256",

  accent: "#ff6a1a",
  accentBright: "#ff9147",
  accentDim: "#7a3208",
  // Backing for chips/pills - a dark ember rather than a pale fill.
  accentSoft: "#2a1408",

  // Models render light-on-dark in this cut.
  model: "#f2ece4",
  modelEdge: "#ff6a1a",
} as const;
