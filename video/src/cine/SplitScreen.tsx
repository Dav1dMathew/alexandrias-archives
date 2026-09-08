import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";
import { HeadsetView } from "./HeadsetView";

// Two panels running at once: one structure assembling, one scrubbing
// through its construction years. Shows that generation and timeline
// reconstruction are two views of the same engine rather than two
// separate features shown back to back.
export const SplitScreen: React.FC<{
  duration: number;
  left: { name: BlueprintName; label: string; caption: string };
  right: { name: BlueprintName; label: string; years: number[] };
  size?: number;
  /** Present the two panels through the headset - one per lens - instead
   *  of side by side on a flat frame. */
  headset?: boolean;
}> = ({ duration, left, right, size = 460, headset = false }) => {
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
        // Without this the panels never get their fair share: a flex item
        // defaults to min-width:auto, so each panel refuses to shrink
        // below the BlueprintHero's own box (2x the model size). Two of
        // those plus the divider overflow 1920 and shove both models off
        // their panel centres - which is exactly what was happening.
        minWidth: 0,
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
          top: headset ? 246 : 96,
          fontFamily: fontStack,
          fontSize: headset ? 34 : 19,
          fontWeight: 600,
          letterSpacing: 5,
          textTransform: "uppercase",
          color: launch.accent,
        }}
      >
        {tag}
      </div>
      {children}
      <div
        style={{
          position: "absolute",
          bottom: headset ? 244 : 108,
          textAlign: "center",
        }}
      >
        {footer}
      </div>
    </div>
  );

  const leftPanel = (
    <Panel
      tag={left.label}
      slideFrom={headset ? 0 : -70}
      footer={
        <div
          style={{
            fontFamily: fontStack,
            fontSize: headset ? 44 : 26,
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
  );

  const rightPanel = (
    <Panel
      tag={right.label}
      slideFrom={headset ? 0 : 70}
      footer={
        <div
          style={{
            fontFamily: fontStack,
            fontSize: headset ? 96 : 58,
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
  );

  // Through the headset the two panels take one lens each. A headset
  // shows both eyes the same image, so this is a deliberate lie - but a
  // split screen squeezed into a single 768px lens is unreadable, and
  // legibility wins on a beat whose whole job is "these are two views of
  // one engine".
  if (headset) {
    return (
      <AbsoluteFill style={{ opacity }}>
        <HeadsetView
          contentScale={0.58}
          focusPull={false}
          parallax={0}
          leftEye={
            <AbsoluteFill style={{ background: launch.bg }}>{leftPanel}</AbsoluteFill>
          }
          rightEye={
            <AbsoluteFill style={{ background: launch.bg }}>{rightPanel}</AbsoluteFill>
          }
        />
      </AbsoluteFill>
    );
  }

  return (
    <AbsoluteFill style={{ opacity, background: launch.bg }}>
      <AbsoluteFill style={{ flexDirection: "row" }}>
        {leftPanel}

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

        {rightPanel}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
