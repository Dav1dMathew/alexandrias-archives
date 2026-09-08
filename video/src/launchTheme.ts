// Black / blue palette for the launch cut. Near-black rather than pure
// #000 so the grain and the bloom have something to sit on - pure black
// crushes both and reads flat.
//
// The accent is sampled off the headset plate's edge piping
// (public/headset.png reads #0195fe at its brightest, #133e99 through the
// mid-tones), so the VR beats and the rest of the film are one palette.
export const launch = {
  bg: "#06080a",
  bgDeep: "#030405",
  surface: "#0c1218",

  text: "#eef4fa",
  textSoft: "#9dadbd",
  textFaint: "#5b6873",

  accent: "#0f8ffe",
  accentBright: "#66c2ff",
  accentDim: "#133e99",
  // Backing for chips/pills - a deep cold fill rather than a pale one.
  accentSoft: "#0a1c33",

  // Models render light-on-dark in this cut.
  model: "#e8eff6",
  modelEdge: "#0f8ffe",
} as const;
