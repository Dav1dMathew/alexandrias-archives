import React from "react";
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { Soundtrack } from "./Soundtrack";
import { PhotoPlate } from "./cine/PhotoPlate";
import { TypedPrompt } from "./cine/TypedPrompt";
import { TimelineScrub } from "./cine/TimelineScrub";
import { Assembly } from "./cine/Assembly";
import { fontStack } from "./theme";

const INK = "#000000";
const ACCENT = "#ffd166";

export const FEATURE_FRAMES = 1800; // 60s at 30fps

// ---------------------------------------------------------------- type

// Apple-style headline: large, semibold, tight tracking, centred in the
// frame. Earlier versions used thin wide-tracked type pinned to the
// bottom edge, which reads as a subtitle rather than a statement.
const Line: React.FC<{
  children: React.ReactNode;
  at?: number;
  hold?: number;
  size?: number;
  weight?: number;
  /** Nudge off centre; negative sits high, positive low. */
  offsetY?: number;
  muted?: boolean;
}> = ({
  children,
  at = 0,
  hold = 90,
  size = 78,
  weight = 600,
  offsetY = 0,
  muted = false,
}) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [at, at + 24, at + hold, at + hold + 26],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );
  // Long, slow drift rather than a quick settle - Apple films keep type
  // moving almost imperceptibly for the whole time it's on screen.
  const drift = interpolate(frame, [at, at + hold + 26], [16, -16]);

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        opacity,
        padding: "0 140px",
      }}
    >
      <div
        style={{
          fontFamily: fontStack,
          fontSize: size,
          fontWeight: weight,
          letterSpacing: -size * 0.022,
          color: muted ? "rgba(214,228,240,0.92)" : "#f5f8fb",
          textAlign: "center",
          maxWidth: 1500,
          lineHeight: 1.14,
          // Distributes words evenly across lines instead of leaving a
          // single-word widow on the last line.
          textWrap: "balance",
          transform: `translateY(${offsetY + drift}px)`,
          textShadow: "0 6px 60px rgba(0,0,0,0.95)",
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
          fontSize: 78,
          fontWeight: 600,
          color: "#f5f8fb",
          textAlign: "center",
          letterSpacing: -1.7,
          maxWidth: 1400,
          lineHeight: 1.14,
        }}
      >
        We never present a guess as history.
      </div>

      {/* A plain typographic row with a small colour dot each. The earlier
          version used bordered, rounded, filled pills, which reads as a
          software dashboard rather than a film. */}
      <div
        style={{
          display: "flex",
          gap: 52,
          flexWrap: "wrap",
          justifyContent: "center",
          alignItems: "center",
        }}
      >
        {labels.map((label, index) => {
          const chip = interpolate(frame, [30 + index * 8, 56 + index * 8], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          });
          return (
            <div
              key={label.name}
              style={{
                opacity: chip,
                transform: `translateY(${interpolate(chip, [0, 1], [10, 0])}px)`,
                display: "flex",
                alignItems: "center",
                gap: 12,
              }}
            >
              <span
                style={{
                  width: 9,
                  height: 9,
                  borderRadius: "50%",
                  background: label.color,
                }}
              />
              <span
                style={{
                  fontFamily: fontStack,
                  fontSize: 27,
                  fontWeight: 450,
                  letterSpacing: -0.3,
                  color: "rgba(226,236,245,0.94)",
                }}
              >
                {label.name.replace("_", " ")}
              </span>
            </div>
          );
        })}
      </div>

      <div
        style={{
          fontFamily: fontStack,
          fontSize: 30,
          fontWeight: 400,
          color: "rgba(190,208,226,0.8)",
          letterSpacing: -0.2,
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
            fontSize: 72,
            fontWeight: 600,
            color: "#f5f8fb",
            letterSpacing: -1.6,
            marginBottom: 60,
            lineHeight: 1.14,
            maxWidth: 1400,
          }}
        >
          Preserve the past by making it explorable.
        </div>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 46,
            fontWeight: 300,
            letterSpacing: track,
            color: "#ffffff",
          }}
        >
          ALEXANDRIA&rsquo;S <span style={{ fontWeight: 600 }}>ARCHIVES</span>
        </div>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 19,
            fontWeight: 500,
            letterSpacing: 6,
            color: "rgba(150,170,190,0.82)",
            marginTop: 28,
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
      <Line at={44} hold={104} size={80}>
        For centuries, we recorded buildings on paper.
      </Line>
    </Sequence>

    {/* 2 | 6-12s - the traditional artefacts the report names */}
    <Sequence from={180} durationInFrames={190}>
      <PhotoPlate src="photos/blueprint.jpg" duration={190} zoomFrom={1.1} zoomTo={1.24} panX={-30} dim={0.52} />
      <Line at={34} hold={110} size={74}>
        Plans, elevations, sections, scale models.
      </Line>
    </Sequence>

    {/* 3 | 12-18s - and the archives that hold them */}
    <Sequence from={360} durationInFrames={185}>
      <PhotoPlate src="photos/archive.jpg" duration={185} zoomFrom={1.12} zoomTo={1.26} panY={-24} dim={0.56} />
      <Line at={30} hold={106} size={74}>
        Brilliant &mdash; but static, and hard to read.
      </Line>
    </Sequence>

    {/* 4 | 18-24s - the loss */}
    <Sequence from={535} durationInFrames={190}>
      <PhotoPlate src="photos/colosseum.jpg" duration={190} zoomFrom={1.06} zoomTo={1.2} dim={0.58} />
      <Line at={34} hold={110} size={74}>
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
