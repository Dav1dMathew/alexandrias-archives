import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { theme, fontStack, monoStack } from "../theme";
import { FadeUp } from "../components";

const team = [
  "David Mathew",
  "Maayan Hazra",
  "Jyotishk Ajmera",
  "Akshat Barjatya",
];

export const Closing: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const rule = spring({
    frame: frame - 10,
    fps,
    config: { damping: 200 },
    durationInFrames: 30,
  });

  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 26,
      }}
    >
      <FadeUp>
        <h2
          style={{
            fontFamily: fontStack,
            fontSize: 66,
            fontWeight: 800,
            color: theme.text,
            margin: 0,
            textAlign: "center",
            letterSpacing: -1.6,
            maxWidth: 1300,
            lineHeight: 1.2,
          }}
        >
          Preserve the past by making it explorable.
        </h2>
      </FadeUp>

      <div
        style={{
          height: 3,
          width: interpolate(rule, [0, 1], [0, 380]),
          background: `linear-gradient(90deg, transparent, ${theme.gold}, transparent)`,
          borderRadius: 2,
        }}
      />

      <FadeUp delay={22}>
        <div
          style={{
            fontFamily: monoStack,
            fontSize: 24,
            color: theme.blue,
            letterSpacing: 5,
            textTransform: "uppercase",
            textAlign: "center",
          }}
        >
          Alexandria&rsquo;s Archives &middot; Project AEGIS
        </div>
      </FadeUp>

      <FadeUp delay={32}>
        <div
          style={{
            display: "flex",
            gap: 34,
            marginTop: 10,
            flexWrap: "wrap",
            justifyContent: "center",
          }}
        >
          {team.map((member) => (
            <span
              key={member}
              style={{
                fontFamily: fontStack,
                fontSize: 22,
                color: theme.muted,
              }}
            >
              {member}
            </span>
          ))}
        </div>
      </FadeUp>
    </div>
  );
};
