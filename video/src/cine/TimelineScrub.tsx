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
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
        <div style={{ transform: "translateY(-58px)" }}>
          <BlueprintHero
            name={name}
            rotation={progress * Math.PI * 0.55}
            size={size}
            reveal={reveal}
          />
        </div>
      </AbsoluteFill>

      {/* Year read-out */}
      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "flex-end",
          paddingBottom: 150,
        }}
      >
        <div style={{ width: 980 }}>
          {label && (
            <div
              style={{
                fontFamily: fontStack,
                fontSize: 18,
                letterSpacing: 5,
                textTransform: "uppercase",
                color: "rgba(180,200,220,0.6)",
                textAlign: "center",
                marginBottom: 18,
              }}
            >
              {label}
            </div>
          )}

          <div
            style={{
              fontFamily: monoStack,
              fontSize: 60,
              fontWeight: 700,
              color: ACCENT,
              textAlign: "center",
              letterSpacing: 3,
            }}
          >
            {currentYear}
          </div>

          {/* Track with ticks for each named year */}
          <div style={{ position: "relative", marginTop: 26, height: 34 }}>
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
