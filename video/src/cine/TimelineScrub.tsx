import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack, monoStack } from "../theme";

const ACCENT = "#ffd166";

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

  const progress = interpolate(frame, [24, duration - 40], [0, 1], {
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
    <AbsoluteFill style={{ opacity }}>
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
              color: "rgba(190,208,226,0.62)",
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
            color: ACCENT,
            textAlign: "center",
            letterSpacing: -2.6,
            lineHeight: 1,
            textShadow: "0 6px 60px rgba(0,0,0,0.9)",
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
                background: "rgba(150,175,200,0.28)",
              }}
            />
            <div
              style={{
                position: "absolute",
                top: 8,
                left: 0,
                width: `${progress * 100}%`,
                height: 2,
                background: ACCENT,
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
                      background: passed ? ACCENT : "rgba(150,175,200,0.4)",
                      margin: "0 auto",
                    }}
                  />
                  <div
                    style={{
                      fontFamily: monoStack,
                      fontSize: 16,
                      color: passed ? "rgba(255,209,102,0.95)" : "rgba(150,175,200,0.5)",
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
