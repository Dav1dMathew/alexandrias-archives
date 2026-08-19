import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { fontStack, monoStack } from "../theme";

const ACCENT = "#ffd166";
const PANEL = "rgba(14,19,26,0.92)";
const BORDER = "rgba(120,150,180,0.34)";

// The in-game build prompt being typed, character by character, with a
// blinking caret - then the AI's reply streaming in underneath.
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
        Math.min(
          reply.length,
          Math.floor((frame - replyStart) * replyCharsPerFrame),
        ),
      )
    : 0;

  // Caret blinks only while the field is idle; solid while typing, which
  // is how a real text field behaves.
  const blink = doneTyping ? (Math.floor(frame / 15) % 2 === 0 ? 1 : 0) : 1;

  const panelIn = interpolate(frame, [0, 18], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
      <div
        style={{
          width: 1180,
          opacity: panelIn,
          transform: `translateY(${interpolate(panelIn, [0, 1], [24, 0])}px)`,
        }}
      >
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 17,
            letterSpacing: 5,
            textTransform: "uppercase",
            color: "rgba(180,200,220,0.62)",
            marginBottom: 20,
            paddingLeft: 4,
          }}
        >
          Minecraft &mdash; Alexandria&rsquo;s Archives
        </div>

        <div
          style={{
            background: PANEL,
            border: `2px solid ${BORDER}`,
            borderRadius: 14,
            padding: "30px 34px",
            display: "flex",
            alignItems: "center",
            gap: 14,
            boxShadow: "0 30px 90px rgba(0,0,0,0.6)",
          }}
        >
          <span
            style={{
              fontFamily: monoStack,
              fontSize: 33,
              color: ACCENT,
              flexShrink: 0,
            }}
          >
            &gt;
          </span>
          <span
            style={{
              fontFamily: monoStack,
              fontSize: 33,
              color: "#f2f6fa",
              whiteSpace: "pre",
            }}
          >
            {command.slice(0, typed)}
          </span>
          <span
            style={{
              width: 15,
              height: 34,
              background: ACCENT,
              opacity: blink,
              display: "inline-block",
            }}
          />
        </div>

        {reply && replyTyped > 0 && (
          <div
            style={{
              marginTop: 26,
              paddingLeft: 6,
              fontFamily: fontStack,
              fontSize: 25,
              lineHeight: 1.55,
              color: "rgba(200,218,235,0.9)",
              minHeight: 90,
            }}
          >
            <span style={{ color: ACCENT, fontFamily: monoStack, fontSize: 21 }}>
              ANALYSING&nbsp;&nbsp;
            </span>
            {reply.slice(0, replyTyped)}
          </div>
        )}
      </div>
    </AbsoluteFill>
  );
};
