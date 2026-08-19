import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { fontStack } from "../theme";

const SURFACE = "#f4f2ee";
const INK = "#17181a";
const MUTED = "#a3a09a";

// Four-point sparkle drawn as a path. Deliberately not an emoji - emoji
// glyphs in a product shot are one of the clearest "made by a bot" tells.
const Sparkle: React.FC<{ size?: number; color?: string }> = ({
  size = 17,
  color = "#ffffff",
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <path
      d="M12 2 C12.6 7.4 16.6 11.4 22 12 C16.6 12.6 12.6 16.6 12 22 C11.4 16.6 7.4 12.6 2 12 C7.4 11.4 11.4 7.4 12 2 Z"
      fill={color}
    />
  </svg>
);

// A modern AI input field: rounded surface, leading avatar, character
// count, and a submit button that shifts to a working state once the
// prompt is committed.
export const TypedPrompt: React.FC<{
  command: string;
  reply?: string;
  typeStart?: number;
  charsPerFrame?: number;
  replyStart?: number;
  replyCharsPerFrame?: number;
}> = ({
  command,
  reply,
  typeStart = 14,
  charsPerFrame = 0.42,
  replyStart = 130,
  replyCharsPerFrame = 0.75,
}) => {
  const frame = useCurrentFrame();

  const typed = Math.max(
    0,
    Math.min(command.length, Math.floor((frame - typeStart) * charsPerFrame)),
  );
  const doneTyping = typed >= command.length;

  const replyTyped = reply
    ? Math.max(
        0,
        Math.min(reply.length, Math.floor((frame - replyStart) * replyCharsPerFrame)),
      )
    : 0;
  const working = doneTyping && replyTyped > 0;

  // Caret is solid while keystrokes are landing and only blinks once the
  // field goes idle, which is how a real text input behaves.
  const caret = doneTyping ? (Math.floor(frame / 16) % 2 === 0 ? 1 : 0) : 1;

  const rise = interpolate(frame, [0, 22], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
      <div
        style={{
          width: 1240,
          opacity: rise,
          transform: `translateY(${interpolate(rise, [0, 1], [26, 0])}px)`,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 22,
            background: SURFACE,
            borderRadius: 999,
            padding: "16px 16px 16px 22px",
            boxShadow:
              "0 40px 90px rgba(0,0,0,0.55), 0 2px 0 rgba(255,255,255,0.5) inset",
          }}
        >
          {/* Leading avatar */}
          <div
            style={{
              width: 54,
              height: 54,
              borderRadius: "50%",
              background: INK,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
          >
            <Sparkle size={22} />
          </div>

          {/* Field */}
          <div
            style={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              minWidth: 0,
            }}
          >
            <span
              style={{
                fontFamily: fontStack,
                fontSize: 31,
                fontWeight: 400,
                letterSpacing: -0.4,
                color: INK,
                whiteSpace: "pre",
              }}
            >
              {command.slice(0, typed)}
            </span>
            <span
              style={{
                width: 2,
                height: 34,
                background: INK,
                opacity: caret,
                marginLeft: 2,
                borderRadius: 1,
              }}
            />
          </div>

          {/* Character count */}
          <span
            style={{
              fontFamily: fontStack,
              fontSize: 21,
              fontWeight: 400,
              color: MUTED,
              flexShrink: 0,
              fontVariantNumeric: "tabular-nums",
            }}
          >
            {typed}/{command.length}
          </span>

          {/* Submit */}
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 11,
              background: INK,
              color: "#ffffff",
              borderRadius: 999,
              padding: "18px 30px",
              flexShrink: 0,
            }}
          >
            <Sparkle size={17} />
            <span
              style={{
                fontFamily: fontStack,
                fontSize: 23,
                fontWeight: 500,
                letterSpacing: -0.2,
              }}
            >
              {working ? "Generating" : "Generate"}
            </span>
          </div>
        </div>

        {/* Response */}
        {reply && replyTyped > 0 && (
          <div
            style={{
              marginTop: 34,
              paddingLeft: 34,
              paddingRight: 34,
              fontFamily: fontStack,
              fontSize: 27,
              fontWeight: 400,
              letterSpacing: -0.3,
              lineHeight: 1.5,
              color: "rgba(232,239,246,0.9)",
              textAlign: "center",
              minHeight: 84,
              textShadow: "0 4px 40px rgba(0,0,0,0.9)",
            }}
          >
            {reply.slice(0, replyTyped)}
          </div>
        )}
      </div>
    </AbsoluteFill>
  );
};
