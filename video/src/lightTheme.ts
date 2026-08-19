// Light / orange palette for the launch cut, in the register of a SaaS
// product-launch ad: warm paper ground, near-black type, one hot accent
// used sparingly so it still lands when it appears.
export const light = {
  // Warm off-white rather than pure #fff - pure white clips under the
  // vignette and looks like an unstyled page.
  paper: "#f7f3ec",
  paperDeep: "#efe8dd",
  ink: "#14110d",
  inkSoft: "#4a443b",
  inkFaint: "#8e857a",

  orange: "#f4610f",
  orangeBright: "#ff7a29",
  orangeSoft: "#ffd9bf",

  // Models are rendered dark-on-light here, the inverse of the night cut.
  model: "#1c1814",
  modelEdge: "#f4610f",
} as const;
