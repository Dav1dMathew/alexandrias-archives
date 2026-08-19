import React from "react";
import {
  AbsoluteFill,
  interpolate,
  Sequence,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Soundtrack } from "./Soundtrack";
import { TypedPrompt } from "./cine/TypedPrompt";
import { TimelineScrub } from "./cine/TimelineScrub";
import { Assembly } from "./cine/Assembly";
import { Lineup } from "./cine/Lineup";
import { PhotoPlate } from "./cine/PhotoPlate";
import { fontStack } from "./theme";
import { light } from "./lightTheme";

export const LAUNCH_FRAMES = 1800; // 60s at 30fps

// --------------------------------------------------------- beat grid
//
// Measured from the track itself rather than eyeballed. Starting the
// audio at 2:01, an RMS envelope shows the mix bottoming out at ~0.03
// around +14.8s and jumping to ~0.30 at +15.10s - that step is the drop.
// Autocorrelating the onset envelope over the post-drop groove gives
// 127.75 BPM (beat 0.4697s, bar 1.8787s); the grid's 4th beat lands on
// +16.98s, which independently matches the next big transient at +17.0s.
export const AUDIO_START_SECONDS = 121; // 2:01
const DROP_FRAME = 453; // +15.10s at 30fps
const BAR_FRAMES = 56.36; // 1.8787s at 30fps

/** Frame of the nth bar line after the drop; negative reaches back. */
const bar = (n: number) => Math.round(DROP_FRAME + n * BAR_FRAMES);

// Warm paper ground with a soft orange bloom - the launch-ad register,
// as opposed to the near-black film cut.
const Paper: React.FC<{ bloom?: number }> = ({ bloom = 0.5 }) => (
  <AbsoluteFill style={{ background: light.paper }}>
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse at 50% 42%, rgba(255,138,45,${bloom * 0.28}) 0%, transparent 62%)`,
      }}
    />
  </AbsoluteFill>
);

// Big statement type. Words animate in individually so the line lands
// with some snap rather than simply fading up.
const Headline: React.FC<{
  text: string;
  duration: number;
  size?: number;
  accentFrom?: number;
  at?: number;
}> = ({ text, duration, size = 92, accentFrom, at = 0 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const words = text.split(" ");

  const out = interpolate(frame, [duration - 22, duration], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        padding: "0 150px",
        opacity: out,
      }}
    >
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          justifyContent: "center",
          gap: `0 ${size * 0.26}px`,
          maxWidth: 1560,
        }}
      >
        {words.map((word, index) => {
          const pop = spring({
            frame: frame - at - index * 4,
            fps,
            config: { damping: 200, mass: 0.6 },
            durationInFrames: 26,
          });
          const accent = accentFrom !== undefined && index >= accentFrom;
          return (
            <span
              key={`${word}-${index}`}
              style={{
                fontFamily: fontStack,
                fontSize: size,
                fontWeight: 600,
                letterSpacing: -size * 0.026,
                lineHeight: 1.08,
                color: accent ? light.orange : light.ink,
                opacity: pop,
                transform: `translateY(${interpolate(pop, [0, 1], [26, 0])}px)`,
                display: "inline-block",
              }}
            >
              {word}
            </span>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

const trustLabels = [
  "documented",
  "corroborated",
  "inferred",
  "ai generated",
  "unknown",
];

const Trust: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const opacity = interpolate(frame, [0, 20, duration - 24, duration], [0, 1, 1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill
      style={{
        opacity,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "column",
        gap: 44,
      }}
    >
      <div
        style={{
          fontFamily: fontStack,
          fontSize: 76,
          fontWeight: 600,
          letterSpacing: -1.9,
          color: light.ink,
          textAlign: "center",
          maxWidth: 1400,
          lineHeight: 1.1,
        }}
      >
        Every element carries its evidence.
      </div>

      <div style={{ display: "flex", gap: 14, flexWrap: "wrap", justifyContent: "center" }}>
        {trustLabels.map((label, index) => {
          const chip = interpolate(frame, [26 + index * 7, 50 + index * 7], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          });
          return (
            <div
              key={label}
              style={{
                opacity: chip,
                transform: `translateY(${interpolate(chip, [0, 1], [12, 0])}px)`,
                fontFamily: fontStack,
                fontSize: 25,
                fontWeight: 500,
                letterSpacing: -0.2,
                color: light.ink,
                background: light.orangeSoft,
                borderRadius: 999,
                padding: "13px 26px",
              }}
            >
              {label}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

const Close: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const pop = spring({ frame, fps, config: { damping: 200 }, durationInFrames: 34 });
  const out = interpolate(frame, [duration - 24, duration], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  const track = interpolate(frame, [8, 70], [16, 6], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill
      style={{ alignItems: "center", justifyContent: "center", opacity: out }}
    >
      <div
        style={{
          textAlign: "center",
          opacity: pop,
          transform: `translateY(${interpolate(pop, [0, 1], [22, 0])}px)`,
        }}
      >
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 46,
            fontWeight: 300,
            letterSpacing: track,
            color: light.ink,
          }}
        >
          ALEXANDRIA&rsquo;S <span style={{ fontWeight: 700 }}>ARCHIVES</span>
        </div>
        <div
          style={{
            width: 74,
            height: 4,
            background: light.orange,
            borderRadius: 2,
            margin: "34px auto 0",
          }}
        />
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 27,
            fontWeight: 400,
            color: light.inkSoft,
            marginTop: 34,
            letterSpacing: -0.2,
          }}
        >
          Describe it. Explore it.
        </div>
      </div>
    </AbsoluteFill>
  );
};

// The reveal that lands on the drop. "Introducing" is already settled a
// beat early; the name itself slams in exactly on frame 0 of this
// sequence, with a flash and a scale overshoot.
const Introducing: React.FC<{ duration: number }> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const hit = spring({
    frame,
    fps,
    config: { damping: 26, mass: 0.9, stiffness: 190 },
    durationInFrames: 40,
  });

  // Single-frame-ish white bloom on the transient.
  const flash = interpolate(frame, [0, 7], [0.9, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const out = interpolate(frame, [duration - 20, duration], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  const eyebrow = interpolate(frame, [0, 12], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ opacity: out }}>
      <AbsoluteFill style={{ background: `rgba(255,255,255,${flash})` }} />
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
        <div style={{ textAlign: "center" }}>
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 30,
              fontWeight: 600,
              letterSpacing: 9,
              textTransform: "uppercase",
              color: light.orange,
              opacity: eyebrow,
              marginBottom: 30,
            }}
          >
            Introducing
          </div>
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 118,
              fontWeight: 700,
              letterSpacing: -3.4,
              lineHeight: 1.02,
              color: light.ink,
              transform: `scale(${interpolate(hit, [0, 1], [1.22, 1])})`,
            }}
          >
            Alexandria&rsquo;s Archives
          </div>
          <div
            style={{
              width: interpolate(hit, [0, 1], [0, 210]),
              height: 5,
              background: light.orange,
              borderRadius: 3,
              margin: "36px auto 0",
            }}
          />
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

export const Launch: React.FC = () => (
  <AbsoluteFill style={{ backgroundColor: light.paper }}>
    {/* Starts at 2:01 so the drop lands on DROP_FRAME. Short fade-in only -
        a long one would soften the build into the drop. */}
    <Soundtrack
      src="launch.mp3"
      startAtSeconds={AUDIO_START_SECONDS}
      fadeInFrames={16}
      fadeOutFrames={54}
    />
    <Paper />

    {/* ============ ACT 1 - THE PROBLEM (0 to the drop at 453) ============
        The track is breaking down through here, so the visuals hold long
        and get quieter as the mix thins out. */}

    <Sequence from={0} durationInFrames={168}>
      <PhotoPlate src="photos/pisa.jpg" duration={168} zoomFrom={1.05} zoomTo={1.17} dim={0.62} variant="light" />
      <Headline text="For centuries, we recorded buildings on paper." duration={168} size={74} />
    </Sequence>

    <Sequence from={158} durationInFrames={158}>
      <PhotoPlate src="photos/blueprint.jpg" duration={158} zoomFrom={1.1} zoomTo={1.22} panX={-24} dim={0.6} variant="light" />
      <Headline text="Plans. Elevations. Records." duration={158} size={80} />
    </Sequence>

    {/* Last statement lands into the breakdown and clears just before
        the drop, leaving a beat of near-empty frame. */}
    <Sequence from={306} durationInFrames={150}>
      <PhotoPlate src="photos/colosseum.jpg" duration={150} zoomFrom={1.06} zoomTo={1.2} dim={0.66} variant="light" />
      <Headline text="And a building can be lost entirely." duration={150} size={78} accentFrom={3} />
    </Sequence>

    {/* ================= THE DROP - PRODUCT REVEAL (453) ================= */}
    <Sequence from={bar(0)} durationInFrames={bar(3) - bar(0)}>
      <Introducing duration={bar(3) - bar(0)} />
    </Sequence>

    {/* ==================== ACT 3 - FEATURES, ON BARS ==================== */}

    {/* 3 bars in - describe it */}
    <Sequence from={bar(3)} durationInFrames={bar(7) - bar(3)}>
      <TypedPrompt
        command="/aa build the burj khalifa in 2009"
        reply="Supertall tapering tower, Y-shaped floor plan, stepped setbacks rising to a spire."
        typeStart={12}
        charsPerFrame={0.62}
        replyStart={108}
        replyCharsPerFrame={1.05}
      />
    </Sequence>

    {/* 7 bars - it builds itself */}
    <Sequence from={bar(7)} durationInFrames={bar(11) - bar(7)}>
      <Assembly
        name="burj"
        duration={bar(11) - bar(7)}
        fromSize={880}
        toSize={1040}
        caption="Built block by block, in seconds."
        variant="light"
      />
    </Sequence>

    {/* 11 bars - the timeline, holding on the finished tower */}
    <Sequence from={bar(11)} durationInFrames={bar(16) - bar(11)}>
      <TimelineScrub
        name="burj"
        duration={bar(16) - bar(11)}
        years={[2004, 2006, 2008, 2010]}
        size={900}
        label="Timeline reconstruction"
      />
    </Sequence>

    {/* 16 bars - the range */}
    <Sequence from={bar(16)} durationInFrames={bar(20) - bar(16)}>
      <Lineup
        duration={bar(20) - bar(16)}
        headline="Not one shape. Any shape."
        stagger={14}
        entries={[
          { name: "concorde", label: "Concorde", size: 380 },
          { name: "boeing747", label: "Boeing 747", size: 380 },
          { name: "whitehouse", label: "The White House", size: 380 },
          { name: "eye", label: "London Eye", size: 380 },
        ]}
      />
    </Sequence>

    {/* 20 bars - trust */}
    <Sequence from={bar(20)} durationInFrames={bar(22) - bar(20)}>
      <Trust duration={bar(22) - bar(20)} />
    </Sequence>

    {/* 22 bars - close out the remaining frames */}
    <Sequence from={bar(22)} durationInFrames={LAUNCH_FRAMES - bar(22)}>
      <Close duration={LAUNCH_FRAMES - bar(22)} />
    </Sequence>
  </AbsoluteFill>
);
