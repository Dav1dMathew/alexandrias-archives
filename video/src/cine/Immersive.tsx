import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

// The "you're inside it" beat. A stereo pair of viewports - the shape a
// headset actually presents - each showing the same structure from a
// slightly offset angle, with the camera pushing in the whole time.
//
// Honest about what it is: a representation of the intended VR product
// from the report's roadmap, not a capture of a working headset build,
// which doesn't exist yet.
export const Immersive: React.FC<{
  duration: number;
  name: BlueprintName;
  headline?: string;
  caption?: string;
}> = ({ duration, name, headline, caption }) => {
  const frame = useCurrentFrame();

  const opacity = interpolate(frame, [0, 24, duration - 26, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  // Continuous push-in plus a slow orbit - reads as moving through the
  // space rather than watching an object spin.
  const push = interpolate(frame, [0, duration], [430, 620]);
  const orbit = (frame / duration) * Math.PI * 0.85;

  const lensIn = interpolate(frame, [4, 34], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const Lens: React.FC<{ eyeOffset: number }> = ({ eyeOffset }) => (
    <div
      style={{
        position: "relative",
        width: 660,
        height: 660,
        borderRadius: "50%",
        overflow: "hidden",
        background: `radial-gradient(circle at 50% 44%, ${launch.surface} 0%, ${launch.bgDeep} 78%)`,
        border: `2px solid ${launch.accent}3a`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        transform: `scale(${interpolate(lensIn, [0, 1], [0.86, 1])})`,
      }}
    >
      <BlueprintHero
        name={name}
        // A few degrees of parallax between the eyes.
        rotation={orbit + eyeOffset}
        size={push}
        tint={launch.model}
      />
      {/* Lens falloff */}
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 50% 50%, transparent 52%, ${launch.bgDeep}f2 92%)`,
        }}
      />
    </div>
  );

  return (
    <AbsoluteFill style={{ opacity, background: launch.bg }}>
      {headline && (
        <AbsoluteFill
          style={{ alignItems: "center", justifyContent: "flex-start", paddingTop: 86 }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 72,
              fontWeight: 600,
              letterSpacing: -1.7,
              color: launch.text,
              textAlign: "center",
              maxWidth: 1500,
              lineHeight: 1.1,
            }}
          >
            {headline}
          </div>
        </AbsoluteFill>
      )}

      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "center",
          flexDirection: "row",
          gap: 34,
          paddingTop: 40,
        }}
      >
        <Lens eyeOffset={-0.05} />
        <Lens eyeOffset={0.05} />
      </AbsoluteFill>

      {caption && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            justifyContent: "flex-end",
            paddingBottom: 74,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 24,
              fontWeight: 500,
              letterSpacing: 4,
              textTransform: "uppercase",
              color: launch.accent,
            }}
          >
            {caption}
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};
