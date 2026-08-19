import React from "react";
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { Soundtrack } from "./Soundtrack";
import { PhotoPlate } from "./cine/PhotoPlate";
import { TypedPrompt } from "./cine/TypedPrompt";
import { TimelineScrub } from "./cine/TimelineScrub";
import { Assembly } from "./cine/Assembly";
import { fontStack, monoStack } from "./theme";

const INK = "#000000";
const ACCENT = "#ffd166";

export const FEATURE_FRAMES = 1800; // 60s at 30fps

// ---------------------------------------------------------------- type

const Line: React.FC<{
  children: React.ReactNode;
  at?: number;
  hold?: number;
  size?: number;
  weight?: number;
  bottom?: number;
  center?: boolean;
}> = ({
  children,
  at = 0,
  hold = 90,
  size = 44,
  weight = 250,
  bottom = 150,
  center = false,
}) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [at, at + 22, at + hold, at + hold + 24],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );
  const lift = interpolate(frame, [at, at + 40], [14, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: center ? "center" : "flex-end",
        paddingBottom: center ? 0 : bottom,
        opacity,
      }}
    >
      <div
        style={{
          fontFamily: fontStack,
          fontSize: size,
          fontWeight: weight,
          letterSpacing: 1.4,
          color: "#f2f6fa",
          textAlign: "center",
          maxWidth: 1400,
          lineHeight: 1.4,
          transform: `translateY(${lift}px)`,
          textShadow: "0 4px 40px rgba(0,0,0,0.9)",
        }}
      >
        {children}
      </div>
    </AbsoluteFill>
  );
};

// ------------------------------------------------------------ chapters

// Provenance chips - the project's actual trust framework, from
// VALID_PROVENANCE in ai-service/generation/llm_schema.py.
const labels = [
  { name: "documented", color: "#7ac74f" },
  { name: "corroborated", color: "#63b3ed" },
  { name: "inferred", color: ACCENT },
  { name: "ai_generated", color: "#e8873c" },
  { name: "unknown", color: "#94a3b8" },
];

const Provenance: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [0, 24, duration - 24, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill
      style={{
        opacity,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "column",
        gap: 46,
      }}
    >
      <div
        style={{
          fontFamily: fontStack,
          fontSize: 46,
          fontWeight: 250,
          color: "#f2f6fa",
          textAlign: "center",
          letterSpacing: 1.2,
          maxWidth: 1300,
          lineHeight: 1.38,
        }}
      >
        We never present a guess as history.
      </div>

      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", justifyContent: "center" }}>
        {labels.map((label, index) => {
          const chip = interpolate(frame, [30 + index * 9, 54 + index * 9], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          });
          return (
            <div
              key={label.name}
              style={{
                opacity: chip,
                transform: `translateY(${interpolate(chip, [0, 1], [14, 0])}px)`,
                fontFamily: monoStack,
                fontSize: 22,
                color: label.color,
                border: `2px solid ${label.color}66`,
                borderLeft: `6px solid ${label.color}`,
                borderRadius: 10,
                padding: "13px 22px",
                background: "rgba(12,16,22,0.8)",
              }}
            >
              {label.name}
            </div>
          );
        })}
      </div>

      <div
        style={{
          fontFamily: fontStack,
          fontSize: 24,
          fontWeight: 300,
          color: "rgba(180,200,220,0.75)",
          letterSpacing: 2,
        }}
      >
        Every element carries its evidence.
      </div>
    </AbsoluteFill>
  );
};

const Closing: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(frame, [10, 44, duration - 26, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const track = interpolate(frame, [10, 80], [18, 7], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity }}>
      <div style={{ textAlign: "center" }}>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 40,
            fontWeight: 200,
            color: "rgba(200,218,235,0.86)",
            letterSpacing: 2,
            marginBottom: 46,
          }}
        >
          Preserve the past by making it explorable.
        </div>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 56,
            fontWeight: 200,
            letterSpacing: track,
            color: "#ffffff",
          }}
        >
          ALEXANDRIA&rsquo;S <span style={{ fontWeight: 600 }}>ARCHIVES</span>
        </div>
        <div
          style={{
            fontFamily: monoStack,
            fontSize: 18,
            letterSpacing: 7,
            color: "rgba(140,160,180,0.8)",
            marginTop: 26,
          }}
        >
          PROJECT AEGIS
        </div>
      </div>
    </AbsoluteFill>
  );
};

// ------------------------------------------------------------- montage

export const Feature: React.FC = () => (
  <AbsoluteFill style={{ backgroundColor: INK }}>
    <Soundtrack />

    {/* 1 | 0-6s - cold open on real heritage */}
    <Sequence from={0} durationInFrames={190}>
      <PhotoPlate src="photos/pisa.jpg" duration={190} zoomFrom={1.04} zoomTo={1.18} dim={0.5} />
      <Line at={44} hold={104} size={46}>
        For centuries, we recorded buildings on paper.
      </Line>
    </Sequence>

    {/* 2 | 6-12s - the traditional artefacts the report names */}
    <Sequence from={180} durationInFrames={190}>
      <PhotoPlate src="photos/blueprint.jpg" duration={190} zoomFrom={1.1} zoomTo={1.24} panX={-30} dim={0.52} />
      <Line at={34} hold={110} size={42}>
        Plans, elevations, sections, scale models.
      </Line>
    </Sequence>

    {/* 3 | 12-18s - and the archives that hold them */}
    <Sequence from={360} durationInFrames={185}>
      <PhotoPlate src="photos/archive.jpg" duration={185} zoomFrom={1.12} zoomTo={1.26} panY={-24} dim={0.56} />
      <Line at={30} hold={106} size={42}>
        Brilliant &mdash; but static, and hard to read.
      </Line>
    </Sequence>

    {/* 4 | 18-24s - the loss */}
    <Sequence from={535} durationInFrames={190}>
      <PhotoPlate src="photos/colosseum.jpg" duration={190} zoomFrom={1.06} zoomTo={1.2} dim={0.58} />
      <Line at={34} hold={110} size={42}>
        And a building can be lost entirely.
      </Line>
    </Sequence>

    {/* 5 | 24-32s - the prompt being typed */}
    <Sequence from={715} durationInFrames={250}>
      <PhotoPlate src="photos/burj.jpg" duration={250} zoomFrom={1.14} zoomTo={1.26} dim={0.82} />
      <TypedPrompt
        command="/aa build the burj khalifa in 2009"
        reply="Supertall tapering tower, Y-shaped floor plan, stepped setbacks rising to a spire."
        typeStart={20}
        replyStart={150}
      />
    </Sequence>

    {/* 6 | 32-41s - it assembles, block by block */}
    <Sequence from={955} durationInFrames={275}>
      <Assembly
        name="burj"
        duration={275}
        fromSize={860}
        toSize={1020}
        caption="Built block by block, in seconds."
      />
    </Sequence>

    {/* 7 | 41-50s - the timeline feature from the report */}
    <Sequence from={1220} durationInFrames={280}>
      <TimelineScrub
        name="burj"
        duration={280}
        years={[2004, 2006, 2008, 2010]}
        size={820}
        label="Timeline reconstruction"
      />
    </Sequence>

    {/* 8 | 50-55s - provenance, the differentiator */}
    <Sequence from={1490} durationInFrames={185}>
      <Provenance duration={185} />
    </Sequence>

    {/* 9 | 55-60s - close on the logotype alone, like the reference does */}
    <Sequence from={1665} durationInFrames={135}>
      <Closing duration={135} />
    </Sequence>
  </AbsoluteFill>
);
