import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { theme, fontStack } from "./theme";

// Shared background for every scene - a subtle vertical gradient plus a
// faint grid, so scene cuts read as one continuous piece rather than
// unrelated slides.
export const Backdrop: React.FC = () => (
  <div
    style={{
      position: "absolute",
      inset: 0,
      background: `linear-gradient(160deg, ${theme.bgDeep} 0%, ${theme.bgMid} 100%)`,
    }}
  >
    <div
      style={{
        position: "absolute",
        inset: 0,
        backgroundImage: `linear-gradient(${theme.border}22 1px, transparent 1px), linear-gradient(90deg, ${theme.border}22 1px, transparent 1px)`,
        backgroundSize: "60px 60px",
        maskImage: "radial-gradient(ellipse at center, black 20%, transparent 75%)",
        WebkitMaskImage:
          "radial-gradient(ellipse at center, black 20%, transparent 75%)",
      }}
    />
  </div>
);

// Fades + lifts content in, then holds. `delay` staggers elements within
// a scene so they arrive in reading order instead of all at once.
export const FadeUp: React.FC<{
  children: React.ReactNode;
  delay?: number;
  distance?: number;
}> = ({ children, delay = 0, distance = 28 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const progress = spring({
    frame: frame - delay,
    fps,
    config: { damping: 200 },
    durationInFrames: 24,
  });

  return (
    <div
      style={{
        opacity: progress,
        transform: `translateY(${interpolate(progress, [0, 1], [distance, 0])}px)`,
      }}
    >
      {children}
    </div>
  );
};

export const SceneTitle: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => (
  <h2
    style={{
      fontFamily: fontStack,
      fontSize: 62,
      fontWeight: 700,
      color: theme.text,
      margin: 0,
      letterSpacing: -1.2,
      lineHeight: 1.12,
    }}
  >
    {children}
  </h2>
);

export const SceneSubtitle: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => (
  <p
    style={{
      fontFamily: fontStack,
      fontSize: 30,
      color: theme.muted,
      margin: 0,
      lineHeight: 1.45,
      maxWidth: 1180,
    }}
  >
    {children}
  </p>
);

// Small uppercase label that sits above a scene title.
export const Eyebrow: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => (
  <div
    style={{
      fontFamily: fontStack,
      fontSize: 20,
      fontWeight: 700,
      letterSpacing: 4,
      textTransform: "uppercase",
      color: theme.gold,
    }}
  >
    {children}
  </div>
);

export const SceneLayout: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => (
  <div
    style={{
      position: "absolute",
      inset: 0,
      display: "flex",
      flexDirection: "column",
      justifyContent: "center",
      padding: "0 130px",
      gap: 26,
    }}
  >
    {children}
  </div>
);
