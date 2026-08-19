import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { BlueprintHero, BlueprintName } from "../BlueprintHero";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

type Entry = { name: BlueprintName; label: string; size: number };

// Several real generated models on screen at once, arriving one after
// another - the "look at the range of this thing" beat every product
// launch has. Each is the actual archetype output, so the silhouettes
// genuinely differ rather than being restyled copies of one shape.
export const Lineup: React.FC<{
  duration: number;
  entries: Entry[];
  headline?: string;
  stagger?: number;
}> = ({ duration, entries, headline, stagger = 16 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const opacity = interpolate(frame, [0, 20, duration - 26, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const headlineIn = interpolate(frame, [6, 30], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ opacity, background: launch.bg }}>
      {headline && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            justifyContent: "flex-start",
            paddingTop: 96,
            opacity: headlineIn,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 72,
              fontWeight: 600,
              letterSpacing: -1.7,
              color: launch.text,
              textAlign: "center",
              maxWidth: 1500,
              lineHeight: 1.1,
              transform: `translateY(${interpolate(headlineIn, [0, 1], [14, 0])}px)`,
            }}
          >
            {headline}
          </div>
        </AbsoluteFill>
      )}

      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "center",
          gap: 10,
          flexDirection: "row",
          paddingTop: 70,
        }}
      >
        {entries.map((entry, index) => {
          const at = 20 + index * stagger;
          const pop = spring({
            frame: frame - at,
            fps,
            config: { damping: 200, mass: 0.7 },
            durationInFrames: 30,
          });

          return (
            <div
              key={entry.name}
              style={{
                flex: 1,
                minWidth: 0,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 10,
                opacity: pop,
                transform: `translateY(${interpolate(pop, [0, 1], [46, 0])}px)`,
              }}
            >
              {/* BlueprintHero's SVG canvas is twice its `size`, which is
                  far wider than the model itself. In a flex row that
                  overflow squeezed its neighbours and clipped the labels,
                  so the model lives in a fixed-height box and is allowed
                  to overhang harmlessly against the flat background. */}
              <div
                style={{
                  height: 330,
                  width: "100%",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  overflow: "visible",
                }}
              >
                <BlueprintHero
                  name={entry.name}
                  // Each turns at a slightly different rate so the row
                  // doesn't read as one rigid animation.
                  rotation={(frame / duration) * Math.PI * (0.9 + index * 0.16)}
                  size={entry.size}
                  tint={launch.model}
                />
              </div>
              <div
                style={{
                  fontFamily: fontStack,
                  fontSize: 24,
                  fontWeight: 500,
                  letterSpacing: -0.2,
                  color: launch.textSoft,
                  textAlign: "center",
                  whiteSpace: "nowrap",
                }}
              >
                {entry.label}
              </div>
            </div>
          );
        })}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
