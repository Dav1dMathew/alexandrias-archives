import React from "react";
import { theme, fontStack } from "../theme";
import { Eyebrow, FadeUp, SceneLayout, SceneSubtitle, SceneTitle } from "../components";

const points = [
  "Buildings are lost to conflict, disaster and redevelopment.",
  "Plans and elevations take training to read.",
  "Text and photographs can't convey scale or circulation.",
];

export const Problem: React.FC = () => (
  <SceneLayout>
    <FadeUp>
      <Eyebrow>The problem</Eyebrow>
    </FadeUp>

    <FadeUp delay={6}>
      <SceneTitle>
        A building can disappear.
        <br />
        Its knowledge doesn&rsquo;t have to.
      </SceneTitle>
    </FadeUp>

    <FadeUp delay={14}>
      <SceneSubtitle>
        Architectural knowledge lives in blueprints, photographs and records
        that are hard to access &mdash; and easy to lose.
      </SceneSubtitle>
    </FadeUp>

    <div style={{ display: "flex", flexDirection: "column", gap: 18, marginTop: 16 }}>
      {points.map((point, index) => (
        <FadeUp key={point} delay={24 + index * 8} distance={18}>
          <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
            <div
              style={{
                width: 10,
                height: 10,
                borderRadius: "50%",
                background: theme.gold,
                flexShrink: 0,
              }}
            />
            <span
              style={{
                fontFamily: fontStack,
                fontSize: 28,
                color: theme.text,
                opacity: 0.9,
              }}
            >
              {point}
            </span>
          </div>
        </FadeUp>
      ))}
    </div>
  </SceneLayout>
);
