import React from "react";
import {
  AbsoluteFill,
  interpolate,
  Sequence,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { BlueprintHero, BlueprintName } from "./BlueprintHero";
import { Soundtrack } from "./Soundtrack";
import { fontStack } from "./theme";

// Deliberately near-black and very sparse, matching the reference film's
// language: one subject, lots of negative space, slow moves, small type.
const INK = "#000000";
const HAZE = "#0a0d10";

const Vignette: React.FC = () => (
  <AbsoluteFill
    style={{
      background: `radial-gradient(ellipse at 50% 46%, ${HAZE} 0%, ${INK} 62%)`,
    }}
  />
);

// Small, wide-tracked caption - the reference uses type sparingly and
// never lets it compete with the subject.
const Caption: React.FC<{
  children: React.ReactNode;
  at?: number;
  hold?: number;
  bottom?: number;
  size?: number;
}> = ({ children, at = 0, hold = 90, bottom = 132, size = 27 }) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [at, at + 22, at + hold, at + hold + 24],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "flex-end",
        paddingBottom: bottom,
        opacity,
      }}
    >
      <div
        style={{
          fontFamily: fontStack,
          fontSize: size,
          fontWeight: 300,
          letterSpacing: 3.4,
          color: "#e8eef5",
          textAlign: "center",
          maxWidth: 1250,
          lineHeight: 1.55,
        }}
      >
        {children}
      </div>
    </AbsoluteFill>
  );
};

// A single subject, slowly rotating and drifting - one continuous move
// per shot rather than cuts within a shot.
const HeroShot: React.FC<{
  name: BlueprintName;
  duration: number;
  fromSize: number;
  toSize: number;
  turns?: number;
  explodeAt?: number;
  tint?: string;
}> = ({ name, duration, fromSize, toSize, turns = 0.55, explodeAt, tint }) => {
  const frame = useCurrentFrame();

  const progress = frame / duration;
  const size = interpolate(frame, [0, duration], [fromSize, toSize]);
  const rotation = progress * Math.PI * 2 * turns;

  const explode =
    explodeAt === undefined
      ? 0
      : interpolate(frame, [explodeAt, duration], [0, 1.15], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
        });

  const opacity = interpolate(
    frame,
    [0, 26, duration - 26, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
      <div style={{ transform: "translateY(-40px)" }}>
        <BlueprintHero
          name={name}
          rotation={rotation}
          size={size}
          opacity={opacity}
          explode={explode}
          tint={tint}
        />
      </div>
    </AbsoluteFill>
  );
};

// Full-bleed statement card - the reference cuts to type alone on black
// between product beats.
const Statement: React.FC<{ duration: number; children: React.ReactNode }> = ({
  duration,
  children,
}) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [0, 26, duration - 26, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );
  const drift = interpolate(frame, [0, duration], [10, -10]);

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        opacity,
      }}
    >
      <div
        style={{
          fontFamily: fontStack,
          fontSize: 60,
          fontWeight: 200,
          letterSpacing: 1.6,
          color: "#f2f6fa",
          textAlign: "center",
          maxWidth: 1350,
          lineHeight: 1.42,
          transform: `translateY(${drift}px)`,
        }}
      >
        {children}
      </div>
    </AbsoluteFill>
  );
};

const Logotype: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const opacity = interpolate(frame, [8, 40, duration - 20, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const track = interpolate(frame, [8, 70], [16, 6], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  void fps;

  return (
    <AbsoluteFill
      style={{ alignItems: "center", justifyContent: "center", opacity }}
    >
      <div style={{ textAlign: "center" }}>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 52,
            fontWeight: 200,
            letterSpacing: track,
            color: "#ffffff",
          }}
        >
          ALEXANDRIA&rsquo;S{" "}
          <span style={{ fontWeight: 600 }}>ARCHIVES</span>
        </div>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 19,
            fontWeight: 300,
            letterSpacing: 7,
            color: "#7d8998",
            marginTop: 26,
          }}
        >
          PROJECT AEGIS
        </div>
      </div>
    </AbsoluteFill>
  );
};

const shots = [
  { kind: "hero", duration: 165 },
  { kind: "statement", duration: 120 },
  { kind: "hero2", duration: 175 },
  { kind: "statement2", duration: 120 },
  { kind: "hero3", duration: 185 },
  { kind: "logo", duration: 140 },
] as const;

export const CINEMATIC_FRAMES = 990;

export const Cinematic: React.FC = () => (
  <AbsoluteFill style={{ backgroundColor: INK }}>
    <Soundtrack />
    <Vignette />

    {/* 1 - the subject arrives, very small, slowly turning */}
    <Sequence from={0} durationInFrames={200}>
      <HeroShot name="tower" duration={200} fromSize={120} toSize={210} turns={0.5} />
      <Caption at={40} hold={110}>
        Every structure carries knowledge.
      </Caption>
    </Sequence>

    {/* 2 - type alone on black */}
    <Sequence from={190} durationInFrames={135}>
      <Statement duration={135}>Some of it is already lost.</Statement>
    </Sequence>

    {/* 3 - the dome, closer, then exploding into its parts */}
    <Sequence from={315} durationInFrames={225}>
      <HeroShot
        name="dome"
        duration={225}
        fromSize={230}
        toSize={330}
        turns={0.62}
        explodeAt={130}
      />
      <Caption at={30} hold={150}>
        Reconstructed from real evidence.
      </Caption>
    </Sequence>

    {/* 4 - statement */}
    <Sequence from={530} durationInFrames={130}>
      <Statement duration={130}>From a prompt. In seconds.</Statement>
    </Sequence>

    {/* 5 - the aircraft, the most obviously "generated" subject */}
    <Sequence from={650} durationInFrames={215}>
      <HeroShot
        name="aircraft"
        duration={215}
        fromSize={250}
        toSize={340}
        turns={0.7}
      />
      <Caption at={34} hold={140}>
        Built block by block, inside Minecraft.
      </Caption>
    </Sequence>

    {/* 6 - logotype */}
    <Sequence from={855} durationInFrames={135}>
      <Logotype duration={135} />
    </Sequence>
  </AbsoluteFill>
);
