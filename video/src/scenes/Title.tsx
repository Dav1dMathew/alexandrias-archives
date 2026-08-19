import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { theme, fontStack, monoStack } from "../theme";
import { FadeUp } from "../components";

export const Title: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const glow = spring({
    frame,
    fps,
    config: { damping: 200 },
    durationInFrames: 40,
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
        gap: 30,
      }}
    >
      {/* Soft gold bloom behind the wordmark */}
      <div
        style={{
          position: "absolute",
          width: 1100,
          height: 460,
          borderRadius: "50%",
          background: `radial-gradient(ellipse, ${theme.gold}22 0%, transparent 68%)`,
          opacity: glow,
        }}
      />

      <FadeUp>
        <div
          style={{
            fontFamily: monoStack,
            fontSize: 22,
            letterSpacing: 7,
            textTransform: "uppercase",
            color: theme.blue,
            textAlign: "center",
          }}
        >
          Project AEGIS
        </div>
      </FadeUp>

      <FadeUp delay={8}>
        <h1
          style={{
            fontFamily: fontStack,
            fontSize: 118,
            fontWeight: 800,
            color: theme.text,
            margin: 0,
            letterSpacing: -3.5,
            textAlign: "center",
            textShadow: `0 0 70px ${theme.gold}40`,
          }}
        >
          Alexandria&rsquo;s Archives
        </h1>
      </FadeUp>

      <FadeUp delay={18}>
        <p
          style={{
            fontFamily: fontStack,
            fontSize: 36,
            color: theme.muted,
            margin: 0,
            textAlign: "center",
            maxWidth: 1180,
            lineHeight: 1.4,
          }}
        >
          AI-reconstructed history, rebuilt block by block.
        </p>
      </FadeUp>

      {/* Rule that draws itself outward from the centre */}
      <div
        style={{
          marginTop: 14,
          height: 3,
          width: interpolate(glow, [0, 1], [0, 460]),
          background: `linear-gradient(90deg, transparent, ${theme.gold}, transparent)`,
          borderRadius: 2,
        }}
      />
    </div>
  );
};
