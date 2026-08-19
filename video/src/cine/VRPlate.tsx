import React from "react";
import {
  AbsoluteFill,
  interpolate,
  OffthreadVideo,
  staticFile,
  useCurrentFrame,
} from "remotion";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

// Live-action VR footage, graded into the launch palette. The source is
// bright warm daylight at 24fps; Remotion maps it by time so the 30fps
// composition doesn't need it conformed first. Audio is muted - the
// music track carries the whole cut.
export const VRPlate: React.FC<{
  duration: number;
  headline?: string;
  caption?: string;
  /** Seconds into the source clip to begin. */
  startAtSeconds?: number;
  grade?: number;
}> = ({ duration, headline, caption, startAtSeconds = 0, grade = 1 }) => {
  const frame = useCurrentFrame();

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
          src={staticFile("vr.mp4")}
          startFrom={Math.round(startAtSeconds * 30)}
          muted
          style={{
            width: "100%",
            height: "100%",
            objectFit: "cover",
            // Pull the daylight down toward the cut's near-black ground
            // and warm it so it sits with the orange accent.
            filter: `brightness(${1 - 0.34 * grade}) contrast(${1 + 0.16 * grade}) saturate(${1 - 0.34 * grade}) sepia(${0.24 * grade})`,
          }}
        />
      </AbsoluteFill>

      {/* Warm wash + vignette to seat the plate in the palette */}
      <AbsoluteFill
        style={{
          background: `linear-gradient(180deg, rgba(255,106,26,${0.12 * grade}) 0%, rgba(10,8,6,${0.34 * grade}) 60%, rgba(5,4,3,${0.8 * grade}) 100%)`,
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
            justifyContent: "flex-start",
            paddingTop: 92,
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
