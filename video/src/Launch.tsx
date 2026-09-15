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
import { PhotoPlate } from "./cine/PhotoPlate";
import { Grain, Glow } from "./cine/Grain";
import { HeadsetView } from "./cine/HeadsetView";
import { FootagePlate } from "./cine/FootagePlate";
import { fontStack } from "./theme";
import { launch } from "./launchTheme";

// The in-VR shot plays its source clip end to end at a touch over 1x -
// five bars (9.39s) keeps the cut under a minute and on the beat grid,
// which is measured off the track, not eyeballed.
export const VR_SHOT_BARS = 5;
const VR_CLIP_SECONDS = 10;
const STATUE_CLIP_SECONDS = 26.37;
export const LAUNCH_FRAMES = 1736; // 57.9s at 30fps

// --------------------------------------------------------- beat grid
//
// Measured from the track itself rather than eyeballed. Starting the
// audio at 2:01, an RMS envelope shows the mix bottoming out at ~0.03
// around +14.8s and jumping to ~0.30 at +15.10s - that step is the drop.
// Autocorrelating the onset envelope over the post-drop groove gives
// 127.75 BPM (beat 0.4697s, bar 1.8787s); the grid's 4th beat lands on
// +16.98s, which independently matches the next big transient at +17.0s.
// The intro is 4s tighter than that measurement, so the audio starts 4s
// later to keep the drop on DROP_FRAME.
export const AUDIO_START_SECONDS = 125; // 2:05
const DROP_FRAME = 333; // +11.10s at 30fps
const BAR_FRAMES = 56.36; // 1.8787s at 30fps

/** Frame of the nth bar line after the drop; negative reaches back. */
const bar = (n: number) => Math.round(DROP_FRAME + n * BAR_FRAMES);

// Warm near-black ground with the orange bloom behind everything.
const Ground: React.FC = () => (
  <AbsoluteFill style={{ background: launch.bg }}>
    <Glow intensity={0.85} />
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
                color: accent ? launch.accent : launch.text,
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
          color: launch.text,
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
                color: launch.text,
                background: launch.accentSoft,
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
            color: launch.text,
          }}
        >
          ALEXANDRIA&rsquo;S <span style={{ fontWeight: 700 }}>ARCHIVES</span>
        </div>
        <div
          style={{
            width: 74,
            height: 4,
            background: launch.accent,
            borderRadius: 2,
            margin: "34px auto 0",
          }}
        />
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 27,
            fontWeight: 400,
            color: launch.textSoft,
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
const Introducing: React.FC<{ duration: number; tagline?: string }> = ({
  duration,
  // NOTE: "world's first" is a factual claim about the market, not about
  // the software - swap this line if it can't be supported.
  tagline = "The world’s first evidence-aware world builder.",
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  // Hands the eyebrow over to the positioning line about a bar in.
  const swap = interpolate(frame, [34, 54], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

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
      {/* Scrim: the particle plate behind this is bright in the centre,
          which was washing out the tagline. Darkens just the type area
          without flattening the embers around it. */}
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(ellipse 46% 30% at 50% 50%, rgba(6,4,3,0.62) 0%, transparent 72%)",
        }}
      />
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
        <div style={{ textAlign: "center" }}>
          {/* Eyebrow slot: "Introducing" hands over to the positioning
              line in place, so the name never leaves frame. */}
          <div style={{ position: "relative", height: 40, marginBottom: 30 }}>
            <div
              style={{
                position: "absolute",
                inset: 0,
                fontFamily: fontStack,
                fontSize: 30,
                fontWeight: 600,
                letterSpacing: 9,
                textTransform: "uppercase",
                color: launch.accent,
                opacity: eyebrow * (1 - swap),
              }}
            >
              Introducing
            </div>
          </div>
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 118,
              fontWeight: 700,
              letterSpacing: -3.4,
              lineHeight: 1.02,
              color: launch.text,
              transform: `scale(${interpolate(hit, [0, 1], [1.22, 1])})`,
            }}
          >
            Alexandria&rsquo;s Archives
          </div>
          <div
            style={{
              width: interpolate(hit, [0, 1], [0, 210]),
              height: 5,
              background: launch.accent,
              borderRadius: 3,
              margin: "34px auto 0",
            }}
          />
          {/* The claim arrives under the rule once the name has landed. */}
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 34,
              fontWeight: 500,
              letterSpacing: -0.4,
              // Brighter than textSoft: this line sits over the particle
              // plate's brightest area, where the muted tone disappeared.
              color: "#e6ddd2",
              textShadow: "0 3px 26px rgba(0,0,0,0.9)",
              marginTop: 30,
              opacity: swap,
              transform: `translateY(${interpolate(swap, [0, 1], [14, 0])}px)`,
            }}
          >
            {tagline}
          </div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

export const Launch: React.FC = () => (
  <AbsoluteFill style={{ backgroundColor: launch.bg }}>
    {/* Starts at 2:05 so the drop lands on DROP_FRAME. Short fade-in only -
        a long one would soften the build into the drop. */}
    <Soundtrack
      src="launch.mp3"
      startAtSeconds={AUDIO_START_SECONDS}
      fadeInFrames={16}
      fadeOutFrames={54}
    />
    <Ground />

    {/* ============ ACT 1 - THE PROBLEM (0 to the drop at 453) ============
        Real footage now carries this act. The track is breaking down
        through here, so shots hold long and get quieter as the mix thins.
        Beats: how we recorded buildings -> why that fails to teach ->
        what happens when the building itself is gone. */}

    <Sequence from={0} durationInFrames={132}>
      <FootagePlate
        src="historian.mp4"
        duration={132}
        headline="For centuries, we recorded buildings on paper."
        headlineY={0.7}
        startAtSeconds={0}
      />
    </Sequence>

    <Sequence from={122} durationInFrames={126}>
      <FootagePlate
        src="teaching.mp4"
        duration={126}
        headline="But you can’t walk through a diagram."
        headlineY={0.7}
        startAtSeconds={2}
      />
    </Sequence>

    {/* Lands into the breakdown and clears just before the drop, leaving
        a beat of near-empty frame. */}
    <Sequence from={238} durationInFrames={100}>
      <PhotoPlate src="photos/colosseum.jpg" duration={100} zoomFrom={1.06} zoomTo={1.2} dim={0.66} variant="dark" />
      <Headline text="And the building itself can be lost." duration={100} size={76} accentFrom={3} />
    </Sequence>

    {/* ================= THE DROP - PRODUCT REVEAL (453) =================
        Tighter than before: the name lands, then the eyebrow hands over
        to the positioning line in place, all inside 2 bars. */}
    <Sequence from={bar(0)} durationInFrames={bar(2) - bar(0)}>
      {/* Particle plate sits behind the wordmark. Graded light so the
          embers stay visible - the type is composited over it rather
          than generated into it, which keeps the name razor sharp. */}
      <FootagePlate
        src="reveal.mp4"
        duration={bar(2) - bar(0)}
        grade={0.4}
        startAtSeconds={1.5}
      />
      <Introducing duration={bar(2) - bar(0)} />
    </Sequence>

    {/* ==================== ACT 3 - FEATURES, ON BARS ==================== */}

    {/* 2 bars - she puts the headset on */}
    <Sequence from={bar(2)} durationInFrames={bar(4) - bar(2)}>
      <FootagePlate
        src="vr.mp4"
        duration={bar(4) - bar(2)}
        headline="Then step inside it."
        headlineY={0.7}
        startAtSeconds={0}
      />
    </Sequence>

    {/* 4 bars - standing in the space, reacting to something enormous.
        The virtual content is deliberately never shown here; the next
        shot is what she's looking at. */}
    <Sequence from={bar(4)} durationInFrames={bar(6) - bar(4)}>
      <FootagePlate
        src="tajmahal.mp4"
        duration={bar(6) - bar(4)}
        startAtSeconds={1}
      />
    </Sequence>

    {/* 7 bars - describe it, still wearing the headset, so the prompt
        reads as a panel floating in the space rather than a cut to a
        laptop. Types fast and clears as soon as the reply has landed. */}
    <Sequence from={bar(6)} durationInFrames={bar(8) - bar(6)}>
      <HeadsetView
        continuous
        contentScale={0.84}
        contentOffsetX={-135}
        contentOffsetY={-96}
        warp={130}
        focusPull={false}
      >
        <AbsoluteFill
          style={{
            background:
              `radial-gradient(ellipse 68% 74% at 50% 44%, #16344d 0%, ` +
              `#081420 58%, ${launch.bgDeep} 100%)`,
          }}
        />
        <TypedPrompt
          command="/aa build the burj khalifa in 2009"
          reply="Supertall tapering tower, Y-shaped plan, stepped setbacks rising to a spire."
          typeStart={4}
          charsPerFrame={1.3}
          replyStart={36}
          replyCharsPerFrame={2.4}
          replyAlign="left"
          replyMaxWidth={400}
        />
      </HeadsetView>
    </Sequence>

    {/* 10 bars - the payoff for the prompt: the structure it just
        described, standing in the world, still seen from inside the
        headset. Continuous rather than per-eye so it reads as one place
        rather than two screens. The grade is deliberately light - this
        beat's whole point is that it's the game, so washing it fully into
        the palette would throw away the thing being shown. */}
    <Sequence from={bar(8)} durationInFrames={bar(8 + VR_SHOT_BARS) - bar(8)}>
      <HeadsetView
        continuous
        contentScale={1.16}
        warp={110}
        focusPull={false}
        caption="Generated in-world"
        // The headset dissolves back once you've registered you're wearing
        // it, so the beat ends on the world rather than on the hardware.
        plateOpacity={1}
        plateOpacityTo={0.42}
        plateFadeStart={20}
        plateFadeEnd={100}
      >
        <FootagePlate
          src="vr_build.mp4"
          duration={bar(8 + VR_SHOT_BARS) - bar(8)}
          startAtSeconds={0}
          // 10s of source stretched across 11.27s of slot. The slight
          // slow motion fills the bars exactly rather than freezing on the
          // last frame for the remainder.
          playbackRate={VR_CLIP_SECONDS / ((bar(8 + VR_SHOT_BARS) - bar(8)) / 30)}
          grade={0.3}
        />
      </HeadsetView>
    </Sequence>

    {/* 16 bars - the Statue of Liberty weathering, seen from inside the
        headset. The shell fades almost fully out so the copper turning
        green is what the viewer is actually watching. */}
    <Sequence from={bar(13)} durationInFrames={bar(19) - bar(13)}>
      <HeadsetView
        continuous
        contentScale={1.02}
        warp={70}
        focusPull={false}
        caption="Weathering in real time"
        plateOpacity={0.7}
        plateOpacityTo={0.1}
        plateFadeStart={8}
        plateFadeEnd={60}
      >
        <FootagePlate
          src="statue_age.mp4"
          duration={bar(19) - bar(13)}
          startAtSeconds={0}
          // The 26s recording runs ~2.3x so the whole brown-to-green
          // oxidation lands inside the slot.
          playbackRate={STATUE_CLIP_SECONDS / ((bar(19) - bar(13)) / 30)}
          grade={0.08}
        />
      </HeadsetView>
    </Sequence>

    {/* 18 bars - trust */}
    <Sequence from={bar(19)} durationInFrames={bar(22) - bar(19)}>
      <Trust duration={bar(22) - bar(19)} />
    </Sequence>

    {/* 21 bars - close out the remaining frames */}
    <Sequence from={bar(22)} durationInFrames={LAUNCH_FRAMES - bar(22)}>
      <Close duration={LAUNCH_FRAMES - bar(22)} />
    </Sequence>

    {/* Grain sits above everything so it films the whole frame */}
    <Grain opacity={0.17} />
  </AbsoluteFill>
);
