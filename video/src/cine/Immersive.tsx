import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";
import { HeadsetView } from "./HeadsetView";

// The "you're inside it" beat. Shot from behind the optics: one continuous
// scene through the binocular opening, with the camera pushing in the whole
// time. Two separate circles read as a diagram of a headset - this reads as
// wearing one.
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

  return (
    <AbsoluteFill style={{ opacity }}>
      <HeadsetView caption={caption} contentScale={0.62}>
        {/* The panels are the only light source in the shot, so the world
            has to sit clearly above the black shell or the opening
            disappears. */}
        <AbsoluteFill
          style={{
            background:
              `radial-gradient(ellipse 70% 76% at 50% 44%, #16344d 0%, ` +
              `#081420 58%, ${launch.bgDeep} 100%)`,
          }}
        />

        {/* Floor plane, so the structure is standing in a space rather
            than floating in a void */}
        <AbsoluteFill
          style={{
            background:
              `linear-gradient(to top, ${launch.accent}12 0%, transparent 32%)`,
          }}
        />

        <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
          <BlueprintHero
            name={name}
            rotation={orbit}
            size={push}
            tint={launch.model}
          />
        </AbsoluteFill>

        {headline && (
          <AbsoluteFill
            style={{ alignItems: "center", justifyContent: "flex-start", paddingTop: 150 }}
          >
            <div
              style={{
                fontFamily: fontStack,
                fontSize: 66,
                fontWeight: 600,
                letterSpacing: -1.6,
                color: launch.text,
                textAlign: "center",
                maxWidth: 1180,
                lineHeight: 1.1,
                textShadow: "0 6px 44px rgba(0,0,0,0.85)",
              }}
            >
              {headline}
            </div>
          </AbsoluteFill>
        )}
      </HeadsetView>
    </AbsoluteFill>
  );
};
