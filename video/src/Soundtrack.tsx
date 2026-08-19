import React from "react";
import { Audio, interpolate, staticFile, useVideoConfig } from "remotion";

// The source track is ~4:25, far longer than either cut. Remotion only
// renders the composition's own frame range, so the tail is trimmed
// automatically - all this needs to add is a fade in at the head and a
// fade out over the final beat so it doesn't stop dead on the last frame.
export const Soundtrack: React.FC<{
  fadeInFrames?: number;
  fadeOutFrames?: number;
  gain?: number;
  startAtSeconds?: number;
}> = ({
  fadeInFrames = 34,
  fadeOutFrames = 70,
  gain = 1,
  // The track opens sparse and builds - measured mean loudness rises from
  // about -19.9 dB over the first 33s to -16.9 dB around the 90s mark. So
  // start from the developed section rather than the intro, which is both
  // fuller and a better match for the reference film's energy.
  startAtSeconds = 90,
}) => {
  const { durationInFrames, fps } = useVideoConfig();

  return (
    <Audio
      src={staticFile("destiny.mp3")}
      trimBefore={Math.round(startAtSeconds * fps)}
      volume={(frame) =>
        gain *
        interpolate(
          frame,
          [
            0,
            fadeInFrames,
            durationInFrames - fadeOutFrames,
            durationInFrames,
          ],
          [0, 1, 1, 0],
          { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
        )
      }
    />
  );
};
