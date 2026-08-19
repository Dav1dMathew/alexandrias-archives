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
}> = ({ fadeInFrames = 34, fadeOutFrames = 70, gain = 0.82 }) => {
  const { durationInFrames } = useVideoConfig();

  return (
    <Audio
      src={staticFile("destiny.mp3")}
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
