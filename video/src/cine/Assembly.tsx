import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

// The payoff beat: the structure builds itself from the ground up, big in
// frame, while slowly turning. Uses the same real geometry as every other
// shot - only the reveal line moves.
export const Assembly: React.FC<{
  name: BlueprintName;
  duration: number;
  fromSize?: number;
  toSize?: number;
  caption?: string;
  turns?: number;
  /** Light launch cut renders dark-on-paper; night cut is the inverse. */
  variant?: "dark" | "light";
}> = ({
  name,
  duration,
  fromSize = 760,
  toSize = 900,
  caption,
  turns = 0.45,
  variant = "dark",
}) => {
  const isLight = variant === "light";
  const frame = useCurrentFrame();

  // Build fast at first, easing out near the top - a linear reveal makes
  // the last few courses feel like they stall.
  const raw = interpolate(frame, [10, duration - 46], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const reveal = 1 - Math.pow(1 - raw, 2.1);

  const size = interpolate(frame, [0, duration], [fromSize, toSize]);
  const rotation = (frame / duration) * Math.PI * 2 * turns;

  const opacity = interpolate(
    frame,
    [0, 22, duration - 24, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  // As a headline it should be present for most of the shot, not arrive
  // at the end the way a trailing subtitle would.
  const captionOpacity = interpolate(
    frame,
    [26, 54, duration - 34, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill style={{ opacity, background: isLight ? launch.bg : undefined }}>
      {/* Model sits low so the headline owns the upper third - the Apple
          arrangement, rather than type pinned under the subject. */}
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
        <div style={{ transform: "translateY(78px)" }}>
          <BlueprintHero
            name={name}
            rotation={rotation}
            size={size}
            reveal={reveal}
            tint={isLight ? launch.model : undefined}
            edgeTint={isLight ? launch.accent : undefined}
          />
        </div>
      </AbsoluteFill>

      {caption && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            justifyContent: "flex-start",
            paddingTop: 118,
            opacity: captionOpacity,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 68,
              fontWeight: 600,
              letterSpacing: -1.5,
              color: isLight ? launch.text : "#f5f8fb",
              textAlign: "center",
              maxWidth: 1400,
              lineHeight: 1.14,
              textShadow: isLight ? "none" : "0 6px 60px rgba(0,0,0,0.95)",
            }}
          >
            {caption}
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};
