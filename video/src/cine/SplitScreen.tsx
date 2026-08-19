import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

// Two panels running at once: one structure assembling, one scrubbing
// through its construction years. Shows that generation and timeline
// reconstruction are two views of the same engine rather than two
// separate features shown back to back.
export const SplitScreen: React.FC<{
  duration: number;
  left: { name: BlueprintName; label: string; caption: string };
  right: { name: BlueprintName; label: string; years: number[] };
  size?: number;
}> = ({ duration, left, right, size = 460 }) => {
  const frame = useCurrentFrame();

  const opacity = interpolate(frame, [0, 22, duration - 24, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  // Panels slide in from their own edges.
  const enter = interpolate(frame, [0, 30], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  // Left builds from nothing; right scrubs its year range.
  const buildRaw = interpolate(frame, [22, duration - 70], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const build = 1 - Math.pow(1 - buildRaw, 2);

  const scrub = interpolate(frame, [30, duration - 60], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const firstYear = right.years[0];
  const lastYear = right.years[right.years.length - 1];
  const currentYear = Math.round(interpolate(scrub, [0, 1], [firstYear, lastYear]));

  const Panel: React.FC<{
    children: React.ReactNode;
    tag: string;
    footer: React.ReactNode;
    slideFrom: number;
  }> = ({ children, tag, footer, slideFrom }) => (
    <div
      style={{
        flex: 1,
        height: "100%",
        position: "relative",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        transform: `translateX(${interpolate(enter, [0, 1], [slideFrom, 0])}px)`,
        opacity: enter,
      }}
    >
      <div
        style={{
          position: "absolute",
          top: 96,
          fontFamily: fontStack,
          fontSize: 19,
          fontWeight: 600,
          letterSpacing: 5,
          textTransform: "uppercase",
          color: launch.accent,
        }}
      >
        {tag}
      </div>
      {children}
      <div style={{ position: "absolute", bottom: 108, textAlign: "center" }}>
        {footer}
      </div>
    </div>
  );

  return (
    <AbsoluteFill style={{ opacity, background: launch.bg }}>
      <AbsoluteFill style={{ flexDirection: "row" }}>
        <Panel
          tag={left.label}
          slideFrom={-70}
          footer={
            <div
              style={{
                fontFamily: fontStack,
                fontSize: 26,
                fontWeight: 500,
                color: launch.textSoft,
                letterSpacing: -0.2,
              }}
            >
              {left.caption}
            </div>
          }
        >
          <BlueprintHero
            name={left.name}
            rotation={(frame / duration) * Math.PI * 0.9}
            size={size}
            reveal={build}
            tint={launch.model}
            edgeTint={launch.accent}
          />
        </Panel>

        {/* Divider */}
        <div
          style={{
            width: 2,
            height: "58%",
            alignSelf: "center",
            background: `linear-gradient(180deg, transparent, ${launch.accent}66, transparent)`,
            opacity: enter,
          }}
        />

        <Panel
          tag={right.label}
          slideFrom={70}
          footer={
            <div
              style={{
                fontFamily: fontStack,
                fontSize: 58,
                fontWeight: 600,
                color: launch.accent,
                letterSpacing: -1.4,
                lineHeight: 1,
              }}
            >
              {currentYear}
            </div>
          }
        >
          <BlueprintHero
            name={right.name}
            rotation={scrub * Math.PI * 0.5}
            size={size}
            reveal={interpolate(scrub, [0, 1], [0.14, 1])}
            tint={launch.model}
            edgeTint={launch.accent}
          />
        </Panel>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
