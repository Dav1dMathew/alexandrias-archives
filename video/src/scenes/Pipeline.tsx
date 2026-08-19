import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { theme, fontStack, monoStack } from "../theme";
import { Eyebrow, FadeUp, SceneLayout, SceneTitle } from "../components";

const stages = [
  { icon: "\u{1F4AC}", label: "Prompt", detail: "/aa build …" },
  { icon: "\u{1F9E0}", label: "AI service", detail: "shape + facts" },
  { icon: "\u{1F4D0}", label: "Blueprint", detail: "validated JSON" },
  { icon: "\u{1F3DB}", label: "Minecraft", detail: "placed blocks" },
];

const NODE_WIDTH = 250;
const GAP = 60;

export const Pipeline: React.FC = () => {
  const frame = useCurrentFrame();

  // The travelling packet crosses one gap per 26 frames, starting once
  // every node has appeared.
  const travel = interpolate(frame, [46, 150], [0, stages.length - 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });

  return (
    <SceneLayout>
      <FadeUp>
        <Eyebrow>How it works</Eyebrow>
      </FadeUp>

      <FadeUp delay={6}>
        <SceneTitle>Evidence in, explorable structure out.</SceneTitle>
      </FadeUp>

      <div
        style={{
          marginTop: 46,
          position: "relative",
          display: "flex",
          alignItems: "stretch",
          gap: GAP,
          // Sized to exactly the nodes it holds and centred, so the rail
          // below terminates at the last node instead of running on to
          // the edge of the (much wider) scene padding box.
          width: stages.length * NODE_WIDTH + (stages.length - 1) * GAP,
          marginLeft: "auto",
          marginRight: "auto",
        }}
      >
        {/* Connecting rail, drawn behind the node cards */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: NODE_WIDTH / 2,
            right: NODE_WIDTH / 2,
            height: 3,
            background: `${theme.border}`,
            transform: "translateY(-50%)",
          }}
        />

        {/* Packet travelling along the rail */}
        <div
          style={{
            position: "absolute",
            top: "50%",
            left: NODE_WIDTH / 2 + travel * (NODE_WIDTH + GAP),
            width: 20,
            height: 20,
            marginLeft: -10,
            borderRadius: "50%",
            background: theme.gold,
            boxShadow: `0 0 26px ${theme.gold}`,
            transform: "translateY(-50%)",
            opacity: frame > 40 ? 1 : 0,
          }}
        />

        {stages.map((stage, index) => {
          const arrived = travel >= index - 0.35;
          return (
            <FadeUp key={stage.label} delay={index * 9} distance={22}>
              <div
                style={{
                  width: NODE_WIDTH,
                  padding: "30px 22px",
                  borderRadius: 18,
                  background: theme.panel,
                  border: `2px solid ${arrived ? theme.gold : theme.border}`,
                  boxShadow: arrived ? `0 0 36px ${theme.gold}30` : "none",
                  textAlign: "center",
                  position: "relative",
                }}
              >
                <div style={{ fontSize: 46, lineHeight: 1 }}>{stage.icon}</div>
                <div
                  style={{
                    fontFamily: fontStack,
                    fontSize: 27,
                    fontWeight: 700,
                    color: theme.text,
                    marginTop: 14,
                  }}
                >
                  {stage.label}
                </div>
                <div
                  style={{
                    fontFamily: monoStack,
                    fontSize: 17,
                    color: theme.muted,
                    marginTop: 8,
                  }}
                >
                  {stage.detail}
                </div>
              </div>
            </FadeUp>
          );
        })}
      </div>

      <FadeUp delay={120}>
        <div
          style={{
            fontFamily: fontStack,
            fontSize: 26,
            color: theme.muted,
            marginTop: 40,
            textAlign: "center",
          }}
        >
          Typical round trip:{" "}
          <span style={{ color: theme.green, fontWeight: 700 }}>under 3 seconds</span>
        </div>
      </FadeUp>
    </SceneLayout>
  );
};
