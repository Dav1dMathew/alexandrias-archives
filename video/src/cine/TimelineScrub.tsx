import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

// The report's "timeline growth across historical periods" feature: the
// same structure re-rendered at successive points in its construction,
// driven by BlueprintHero's `reveal` prop, with a scrubbing year read-out.
export const TimelineScrub: React.FC<{
  name: BlueprintName;
  duration: number;
  years: number[];
  size?: number;
  label?: string;
}> = ({ name, duration, years, size = 700, label }) => {
  const frame = useCurrentFrame();

  // Reaches full height with time to spare, so the shot ends holding on
  // the finished structure rather than cutting the moment it tops out.
  const progress = interpolate(frame, [24, duration - 96], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const first = years[0];
  const last = years[years.length - 1];
  const currentYear = Math.round(interpolate(progress, [0, 1], [first, last]));

  // Never fully empty - a structure with zero courses reads as a bug.
  const reveal = interpolate(progress, [0, 1], [0.12, 1]);

  const opacity = interpolate(
    frame,
    [0, 24, duration - 24, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill style={{ opacity, background: launch.bg }}>
      {/* Label and year lead at the top; the model sits below them and the
          track anchors the bottom. Previously all three were stacked at the
          bottom edge, which crowded the frame and buried the year. */}
      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "flex-start",
          paddingTop: 104,
        }}
      >
        {label && (
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 21,
              fontWeight: 500,
              letterSpacing: 5,
              textTransform: "uppercase",
              color: launch.textFaint,
              textAlign: "center",
              marginBottom: 20,
            }}
          >
            {label}
          </div>
        )}
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 104,
            fontWeight: 600,
            color: launch.accent,
            textAlign: "center",
            letterSpacing: -2.6,
            lineHeight: 1,
            
          }}
        >
          {currentYear}
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
        <div style={{ transform: "translateY(84px)" }}>
          <BlueprintHero
            name={name}
            rotation={progress * Math.PI * 0.55}
            size={size}
            reveal={reveal}
            // Explicit tint so the model stays consistent with the
            // launch palette rather than the default film-cut white.
            tint={launch.model}
            edgeTint={launch.accent}
          />
        </div>
      </AbsoluteFill>

      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "flex-end",
          paddingBottom: 92,
        }}
      >
        <div style={{ width: 1080 }}>
          {/* Track with ticks for each named year */}
          <div style={{ position: "relative", height: 34 }}>
            <div
              style={{
                position: "absolute",
                top: 8,
                left: 0,
                right: 0,
                height: 2,
                background: "rgba(247,242,236,0.16)",
              }}
            />
            <div
              style={{
                position: "absolute",
                top: 8,
                left: 0,
                width: `${progress * 100}%`,
                height: 2,
                background: launch.accent,
              }}
            />
            {years.map((year) => {
              const at = (year - first) / Math.max(1, last - first);
              const passed = currentYear >= year;
              return (
                <div
                  key={year}
                  style={{
                    position: "absolute",
                    left: `${at * 100}%`,
                    top: 0,
                    transform: "translateX(-50%)",
                    textAlign: "center",
                  }}
                >
                  <div
                    style={{
                      width: 3,
                      height: 18,
                      background: passed ? launch.accent : "rgba(247,242,236,0.22)",
                      margin: "0 auto",
                    }}
                  />
                  <div
                    style={{
                      fontFamily: fontStack,
                      fontSize: 16,
                      color: passed ? launch.accent : launch.textFaint,
                      marginTop: 6,
                    }}
                  >
                    {year}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
