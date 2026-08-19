import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { launch } from "../launchTheme";

// Animated film grain. The turbulence seed advances every couple of
// frames so the grain crawls the way real stock does - a static noise
// layer reads as a dirty screen instead of as film.
export const Grain: React.FC<{ opacity?: number; scale?: number }> = ({
  opacity = 0.2,
  scale = 0.9,
}) => {
  const frame = useCurrentFrame();
  // Cycling a small set of seeds keeps it from looking periodic while
  // avoiding a new filter definition on literally every frame.
  const seed = Math.floor(frame / 2) % 8;

  return (
    <AbsoluteFill
      style={{
        pointerEvents: "none",
        mixBlendMode: "overlay",
        opacity,
      }}
    >
      <svg width="100%" height="100%">
        <filter id={`grain-${seed}`} x="0" y="0" width="100%" height="100%">
          <feTurbulence
            type="fractalNoise"
            baseFrequency={scale}
            numOctaves={3}
            seed={seed}
            stitchTiles="stitch"
          />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <rect width="100%" height="100%" filter={`url(#grain-${seed})`} />
      </svg>
    </AbsoluteFill>
  );
};

// Warm bloom plus a heavy vignette - the "modern but vintage" part. The
// bloom drifts slowly so backgrounds never sit perfectly still.
export const Glow: React.FC<{
  intensity?: number;
  x?: number;
  y?: number;
}> = ({ intensity = 1, x = 50, y = 46 }) => {
  const frame = useCurrentFrame();
  const drift = Math.sin(frame / 90) * 2.4;

  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 62% 54% at ${x + drift}% ${y}%, rgba(255,106,26,${0.3 * intensity}) 0%, rgba(255,106,26,${0.09 * intensity}) 34%, transparent 68%)`,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 42% 36% at ${x - drift}% ${y + 10}%, rgba(255,145,71,${0.16 * intensity}) 0%, transparent 60%)`,
        }}
      />
      {/* Vignette last so it darkens the bloom's edges too */}
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 78% 74% at 50% 48%, transparent 30%, ${launch.bgDeep}e6 100%)`,
        }}
      />
    </AbsoluteFill>
  );
};
