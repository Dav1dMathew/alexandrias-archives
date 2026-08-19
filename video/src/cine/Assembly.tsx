import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";

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
}> = ({ name, duration, fromSize = 760, toSize = 900, caption, turns = 0.45 }) => {
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

  const captionOpacity = interpolate(
    frame,
    [duration - 120, duration - 96, duration - 24, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill style={{ opacity }}>
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
        <div style={{ transform: "translateY(-46px)" }}>
          <BlueprintHero
            name={name}
            rotation={rotation}
            size={size}
            reveal={reveal}
          />
        </div>
      </AbsoluteFill>

      {caption && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            justifyContent: "flex-end",
            paddingBottom: 128,
            opacity: captionOpacity,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 27,
              fontWeight: 300,
              letterSpacing: 3.2,
              color: "#e8eef5",
            }}
          >
            {caption}
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};
