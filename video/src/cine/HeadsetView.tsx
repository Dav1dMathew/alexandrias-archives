import React from "react";
import { AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame } from "remotion";
import { fontStack } from "../theme";
import { launch } from "../launchTheme";

const W = 1920;
const H = 1080;

// public/headset.png is a real headset interior with the two lens windows
// cut out to full transparency, so it drops straight over the world as a
// matte. Measured off its alpha channel:
//
//   left window   x 0.053 - 0.453   y 0.270 - 0.739
//   right window  x 0.547 - 0.947   y 0.271 - 0.740
//
// The plate is 1672x941 - 16:9 to within a rounding error - so it maps
// 1:1 onto the composition with no cropping.
const LENS_W = Math.round(0.4 * W); // 768
const LENS_H = Math.round(0.469 * H); // 506
const LENS_Y = Math.round(0.2705 * H); // 292
const LEFT_X = Math.round(0.053 * W); // 102
const RIGHT_X = Math.round(0.547 * W); // 1050

// ----------------------------------------------------------- lens warp
//
// Barrel distortion, the way a headset pre-warps its render so the lens
// pulls it straight again: straight lines bow out from the lens axis and
// the corners stretch.
//
// The displacement field is baked as a coarse grid of flat-coloured cells
// - R carries the x offset, G the y - then smoothed by a blur inside the
// filter. Cheap and deterministic, and it avoids trying to do signed
// arithmetic across half a dozen filter primitives in a clamped 0-1 space.
const GRID = 40;

const warpMap = (boxW: number, boxH: number): string => {
  const cells: string[] = [];
  const cw = boxW / GRID;
  const chh = boxH / GRID;

  for (let i = 0; i < GRID; i++) {
    for (let j = 0; j < GRID; j++) {
      const u = ((i + 0.5) * cw - boxW / 2) / (boxW / 2);
      const v = ((j + 0.5) * chh - boxH / 2) / (boxH / 2);
      const r2 = Math.min(1, u * u + v * v);

      const r = Math.round(255 * clamp01(0.5 + 0.5 * clampUnit(u * r2)));
      const g = Math.round(255 * clamp01(0.5 + 0.5 * clampUnit(v * r2)));

      cells.push(
        `<rect x='${(i * cw).toFixed(2)}' y='${(j * chh).toFixed(2)}' ` +
          `width='${(cw + 0.6).toFixed(2)}' height='${(chh + 0.6).toFixed(2)}' ` +
          `fill='rgb(${r},${g},128)'/>`,
      );
    }
  }

  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' width='${boxW}' height='${boxH}' ` +
      `viewBox='0 0 ${boxW} ${boxH}'>${cells.join("")}</svg>`,
  )}`;
};

const WARP_MAP = warpMap(LENS_W, LENS_H);
const WARP_MAP_FULL = warpMap(W, H);

function clamp01(n: number) {
  return Math.max(0, Math.min(1, n));
}
function clampUnit(n: number) {
  return Math.max(-1, Math.min(1, n));
}

// First-person "you are wearing it" frame: the shot is what your eyes see
// through the optics, not a picture of a headset. Children are authored
// against the full 1920x1080 frame and drawn once per lens.
//
// Honest about what it is: a representation of the intended VR product
// from the report's roadmap, not a capture of a working headset build,
// which doesn't exist yet.
export const HeadsetView: React.FC<{
  children?: React.ReactNode;
  caption?: string;
  /** Scales the world inside each lens. */
  contentScale?: number;
  /** Nudges the world up (negative) within the lens. */
  contentOffsetY?: number;
  /** Nudges the world sideways. In continuous mode this is how you keep
   *  type clear of the nose bridge, which is opaque through the middle. */
  contentOffsetX?: number;
  /** Micro head movement. Off for beats where the content must sit still. */
  drift?: boolean;
  /** Optics waking up. Only the first headset shot of a run should pull
   *  focus - repeating it on a later cut reads as the wearer taking the
   *  headset off and putting it back on. */
  focusPull?: boolean;
  /** Barrel distortion, in px at the lens edge. 0 disables it. */
  warp?: number;
  /** Horizontal parallax between the eyes, in px. */
  parallax?: number;
  /** Per-eye content, overriding `children` for that lens. A real headset
   *  shows the same image to both eyes, so this is a deliberate lie -
   *  it buys two legible panels where one split across a single lens
   *  would be too small to read. Use it only where that trade is worth
   *  it, and pass `children` everywhere else. */
  leftEye?: React.ReactNode;
  rightEye?: React.ReactNode;
  /** Draw the world once across the whole frame instead of once per lens.
   *  A single element spanning both windows then reads as one object seen
   *  through the optics, with the nose bridge crossing it - which is what
   *  a continuous scene actually looks like from inside a headset. Use it
   *  when the beat is one thing; leave it off when the beat is a world
   *  each eye should see its own view of. */
  continuous?: boolean;
  /** Opacity of the headset plate itself. Below 1 the world bleeds through
   *  the shell, which reads as the hardware receding rather than as a
   *  translucent object - useful on a beat whose subject is the world, not
   *  the headset. */
  plateOpacity?: number;
  /** Ease the plate to this opacity between plateFadeStart and
   *  plateFadeEnd (frames, sequence-relative). */
  plateOpacityTo?: number;
  plateFadeStart?: number;
  plateFadeEnd?: number;
}> = ({
  children,
  caption,
  contentScale = 1,
  contentOffsetY = 0,
  contentOffsetX = 0,
  drift = true,
  focusPull = true,
  warp = 70,
  parallax = 7,
  leftEye,
  rightEye,
  continuous = false,
  plateOpacity = 1,
  plateOpacityTo,
  plateFadeStart = 0,
  plateFadeEnd = 40,
}) => {
  const frame = useCurrentFrame();

  // Head never holds perfectly still. A couple of pixels of sway on a long
  // period is what separates "worn" from "screenshot"; any more and the
  // frame starts to swim.
  const swayX = drift ? Math.sin(frame / 74) * 5 : 0;
  const swayY = drift ? Math.cos(frame / 58) * 3.4 : 0;
  const swayR = drift ? Math.sin(frame / 96) * 0.16 : 0;

  const plate =
    plateOpacityTo === undefined
      ? plateOpacity
      : interpolate(frame, [plateFadeStart, plateFadeEnd], [plateOpacity, plateOpacityTo], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
        });

  const focus = focusPull
    ? interpolate(frame, [0, 20], [1, 0], {
        extrapolateLeft: "clamp",
        extrapolateRight: "clamp",
      })
    : 0;

  // One eye. The 1920x1080 authoring frame is centred inside the lens box
  // so children can lay themselves out as if they owned the whole screen.
  const Eye: React.FC<{
    x: number;
    shift: number;
    content: React.ReactNode;
  }> = ({ x, shift, content }) => (
    <div
      style={{
        position: "absolute",
        left: x,
        top: LENS_Y,
        width: LENS_W,
        height: LENS_H,
        overflow: "hidden",
        filter: warp > 0 ? "url(#aa-lens-warp)" : undefined,
      }}
    >
      <div
        style={{
          position: "absolute",
          left: (LENS_W - W) / 2,
          top: (LENS_H - H) / 2,
          width: W,
          height: H,
          transform:
            `translate(${swayX + shift + contentOffsetX}px, ${swayY + contentOffsetY}px) ` +
            `rotate(${swayR}deg) scale(${contentScale})`,
          filter: focus > 0.01 ? `blur(${focus * 13}px)` : undefined,
        }}
      >
        {content}
      </div>

      {/* Panels dim toward the barrel wall */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background:
            `radial-gradient(ellipse 62% 66% at 50% 48%, transparent 46%, rgba(2,2,4,0.72) 100%)`,
        }}
      />
    </div>
  );

  return (
    <AbsoluteFill style={{ background: "#000000" }}>
      {warp > 0 && (
        <svg width={0} height={0} style={{ position: "absolute" }}>
          <defs>
            <filter
              id="aa-lens-warp"
              x="0"
              y="0"
              width={LENS_W}
              height={LENS_H}
              filterUnits="userSpaceOnUse"
              primitiveUnits="userSpaceOnUse"
            >
              <feImage
                href={WARP_MAP}
                x={0}
                y={0}
                width={LENS_W}
                height={LENS_H}
                preserveAspectRatio="none"
                result="map"
              />
              <feGaussianBlur in="map" stdDeviation={7} result="smooth" />
              <feDisplacementMap
                in="SourceGraphic"
                in2="smooth"
                scale={warp * 2}
                xChannelSelector="R"
                yChannelSelector="G"
              />
            </filter>

            <filter
              id="aa-lens-warp-full"
              x="0"
              y="0"
              width={W}
              height={H}
              filterUnits="userSpaceOnUse"
              primitiveUnits="userSpaceOnUse"
            >
              <feImage
                href={WARP_MAP_FULL}
                x={0}
                y={0}
                width={W}
                height={H}
                preserveAspectRatio="none"
                result="mapFull"
              />
              <feGaussianBlur in="mapFull" stdDeviation={12} result="smoothFull" />
              <feDisplacementMap
                in="SourceGraphic"
                in2="smoothFull"
                scale={warp * 2}
                xChannelSelector="R"
                yChannelSelector="G"
              />
            </filter>
          </defs>
        </svg>
      )}

      {continuous ? (
        <>
          <AbsoluteFill
            style={{
              filter: warp > 0 ? "url(#aa-lens-warp-full)" : undefined,
            }}
          >
            <AbsoluteFill
              style={{
                transform:
                  `translate(${swayX + contentOffsetX}px, ${swayY + contentOffsetY}px) ` +
                  `rotate(${swayR}deg) scale(${contentScale})`,
                filter: focus > 0.01 ? `blur(${focus * 13}px)` : undefined,
              }}
            >
              {children}
            </AbsoluteFill>
          </AbsoluteFill>

          {/* Each lens still falls off toward its own barrel wall, even
              though one image spans both. These are barrel shading, not
              content, so they fade with the plate - left at full strength
              behind a faded shell they read as two hard-edged boxes sitting
              on top of the footage. */}
          {[LEFT_X, RIGHT_X].map((x) => (
            <div
              key={x}
              style={{
                position: "absolute",
                left: x,
                top: LENS_Y,
                width: LENS_W,
                height: LENS_H,
                opacity: plate,
                background:
                  `radial-gradient(ellipse 62% 66% at 50% 48%, transparent 46%, rgba(2,2,4,0.72) 100%)`,
              }}
            />
          ))}
        </>
      ) : (
        <>
          <Eye x={LEFT_X} shift={parallax} content={leftEye ?? children} />
          <Eye x={RIGHT_X} shift={-parallax} content={rightEye ?? children} />
        </>
      )}

      {/* The headset itself, matted over the top */}
      <Img
        src={staticFile("headset.png")}
        style={{
          position: "absolute",
          inset: 0,
          width: W,
          height: H,
          objectFit: "fill",
          opacity: plate,
        }}
      />

      {caption && (
        <AbsoluteFill
          style={{
            alignItems: "center",
            justifyContent: "flex-end",
            paddingBottom: 26,
          }}
        >
          <div
            style={{
              fontFamily: fontStack,
              fontSize: 24,
              fontWeight: 500,
              letterSpacing: 4,
              textTransform: "uppercase",
              color: launch.text,
              textShadow: "0 2px 30px rgba(0,0,0,0.95)",
            }}
          >
            {caption}
          </div>
        </AbsoluteFill>
      )}
    </AbsoluteFill>
  );
};
