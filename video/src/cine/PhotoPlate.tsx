import React from "react";
import { AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame } from "remotion";

// Real landmark photography from Wikimedia Commons with a slow push and a
// heavy dark grade, so the stills sit in the same near-black world as the
// generated block models rather than looking like pasted-in stock.
export const PhotoPlate: React.FC<{
  src: string;
  duration: number;
  zoomFrom?: number;
  zoomTo?: number;
  panX?: number;
  panY?: number;
  dim?: number;
  fade?: number;
}> = ({
  src,
  duration,
  zoomFrom = 1.06,
  zoomTo = 1.2,
  panX = 0,
  panY = 0,
  dim = 0.55,
  fade = 26,
}) => {
  const frame = useCurrentFrame();

  const scale = interpolate(frame, [0, duration], [zoomFrom, zoomTo]);
  const shiftX = interpolate(frame, [0, duration], [0, panX]);
  const shiftY = interpolate(frame, [0, duration], [0, panY]);
  const opacity = interpolate(
    frame,
    [0, fade, duration - fade, duration],
    [0, 1, 1, 0],
    { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
  );

  return (
    <AbsoluteFill style={{ opacity, overflow: "hidden" }}>
      <Img
        src={staticFile(src)}
        style={{
          position: "absolute",
          width: "100%",
          height: "100%",
          objectFit: "cover",
          transform: `scale(${scale}) translate(${shiftX}px, ${shiftY}px)`,
          filter: "saturate(0.6) contrast(1.08)",
        }}
      />
      {/* Grade: flat dim plus a vignette, keeping type legible over any frame */}
      <AbsoluteFill style={{ background: `rgba(2,4,7,${dim})` }} />
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(ellipse at 50% 45%, transparent 28%, rgba(0,0,0,0.82) 92%)",
        }}
      />
    </AbsoluteFill>
  );
};
