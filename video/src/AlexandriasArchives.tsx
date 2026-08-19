import React from "react";
import { AbsoluteFill, interpolate, Sequence, useCurrentFrame } from "remotion";
import { Backdrop } from "./components";
import { Soundtrack } from "./Soundtrack";
import { Title } from "./scenes/Title";
import { Problem } from "./scenes/Problem";
import { Pipeline } from "./scenes/Pipeline";
import { Archetypes } from "./scenes/Archetypes";
import { Provenance } from "./scenes/Provenance";
import { Status } from "./scenes/Status";
import { Closing } from "./scenes/Closing";

const FADE = 14;

// Scenes overlap by FADE frames so one cross-dissolves into the next
// rather than hard-cutting.
const scenes = [
  { Component: Title, duration: 120 },
  { Component: Problem, duration: 175 },
  { Component: Pipeline, duration: 195 },
  { Component: Archetypes, duration: 180 },
  { Component: Provenance, duration: 178 },
  { Component: Status, duration: 178 },
  { Component: Closing, duration: 132 },
];

export const TOTAL_FRAMES = scenes.reduce(
  (total, scene) => total + scene.duration - FADE,
  FADE,
);

const Dissolve: React.FC<{ duration: number; children: React.ReactNode }> = ({
  duration,
  children,
}) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(
    frame,
    [0, FADE, duration - FADE, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return <AbsoluteFill style={{ opacity }}>{children}</AbsoluteFill>;
};

export const AlexandriasArchives: React.FC = () => {
  let cursor = 0;

  return (
    <AbsoluteFill style={{ backgroundColor: "#0d1b2a" }}>
      <Soundtrack />
      <Backdrop />
      {scenes.map(({ Component, duration }, index) => {
        const from = cursor;
        cursor += duration - FADE;
        return (
          <Sequence key={index} from={from} durationInFrames={duration}>
            <Dissolve duration={duration}>
              <Component />
            </Dissolve>
          </Sequence>
        );
      })}
    </AbsoluteFill>
  );
};
