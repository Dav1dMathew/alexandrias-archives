import React from "react";
import {
  AbsoluteFill,
  interpolate,
  OffthreadVideo,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

// Live-action footage graded into the launch palette. Sources arrive as
// bright warm daylight at 24fps; Remotion maps them by time so the 30fps
// composition doesn't need them conformed first. Audio is muted - the
// music track carries the whole cut.
export const FootagePlate: React.FC<{
  src: string;
  duration: number;
  headline?: string;
  caption?: string;
  /** Seconds into the source clip to begin. */
  startAtSeconds?: number;
  grade?: number;
  /** <1 slows the clip down. Used to stretch a source of known length
   *  across a slot of a different length instead of freezing on its last
   *  frame once it runs out. */
  playbackRate?: number;
  /** 0 = top of frame, 1 = bottom. Default sits in the lower third,
   *  which keeps type clear of the subject's face. */
  headlineY?: number;
}> = ({
  src,
  duration,
  headline,
  caption,
  startAtSeconds = 0,
  grade = 1,
  playbackRate = 1,
  headlineY = 0.66,
}) => {
  const frame = useCurrentFrame();
  const { height } = useVideoConfig();

  const opacity = interpolate(frame, [0, 20, duration - 22, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const headlineIn = interpolate(frame, [10, 34], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  // Slow push so the plate keeps moving even where the source is static.
  const scale = interpolate(frame, [0, duration], [1.04, 1.13]);

  return (
    <AbsoluteFill style={{ opacity, background: launch.bgDeep, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: `scale(${scale})` }}>
        <OffthreadVideo
          src={staticFile(src)}
          startFrom={Math.round(startAtSeconds * 30)}
          playbackRate={playbackRate}
          muted
          style={{
            width: "100%",
            height: "100%",
            objectFit: "cover",
            // Pull the daylight down toward the cut's near-black ground.
            // Warm plates have to be taken most of the way to mono first -
            // a hue rotation alone leaves gold reading as gold, and the
            // blue tint below can only do its job on neutral footage.
            filter: `brightness(${1 - 0.34 * grade}) contrast(${1 + 0.18 * grade}) saturate(${1 - 0.78 * grade})`,
          }}
        />
      </AbsoluteFill>

      {/* Tint the now-neutral plate to the accent, then wash and
          vignette it down into the ground. Blending on "color" keeps the
          footage's own luminance, so faces and highlights survive. */}
      <AbsoluteFill
        style={{
          background: launch.accent,
          mixBlendMode: "color",
          opacity: 0.62 * grade,
        }}
      />
      <AbsoluteFill
        style={{
          background: `linear-gradient(180deg, rgba(15,143,254,${0.16 * grade}) 0%, rgba(6,9,12,${0.36 * grade}) 60%, rgba(3,4,5,${0.8 * grade}) 100%)`,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 74% 70% at 50% 46%, transparent 26%, ${launch.bgDeep}e0 100%)`,
        }}
      />

      {headline && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            // Fraction of frame height, resolved against the composition's
            // real height in pixels. A percentage would silently resolve
            // against the *width* (CSS resolves padding percentages on the
            // inline axis), which pushed the line 1344px down a 1080px
            // frame and off screen entirely.
            justifyContent: "flex-start",
            paddingTop: headlineY * height,
            opacity: headlineIn,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 76,
              fontWeight: 600,
              letterSpacing: -1.8,
              color: launch.text,
              textAlign: "center",
              maxWidth: 1500,
              lineHeight: 1.1,
              textShadow: "0 6px 50px rgba(0,0,0,0.85)",
              transform: `translateY(${interpolate(headlineIn, [0, 1], [16, 0])}px)`,
            }}
          >
            {headline}
          </div>
        </AbsoluteFill>
      )}

      {caption && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            justifyContent: "flex-end",
            paddingBottom: 76,
            opacity: headlineIn,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 23,
              fontWeight: 600,
              letterSpacing: 4.5,
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
