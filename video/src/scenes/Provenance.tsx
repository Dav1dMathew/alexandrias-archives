import React from "react";
import { theme, fontStack, monoStack } from "../theme";
import { Eyebrow, FadeUp, SceneLayout, SceneSubtitle, SceneTitle } from "../components";

// Mirrors the labels the AI service actually emits per extracted fact
// (VALID_PROVENANCE in ai-service/generation/llm_schema.py).
const labels = [
  { name: "documented", color: theme.green, note: "Well-established fact" },
  { name: "corroborated", color: "#63b3ed", note: "Supported, not certain" },
  { name: "inferred", color: theme.gold, note: "Reasoned from type/era" },
  { name: "ai_generated", color: "#e8873c", note: "Filled a gap" },
  { name: "unknown", color: "#94a3b8", note: "Not guessing" },
];

export const Provenance: React.FC = () => (
  <SceneLayout>
    <FadeUp>
      <Eyebrow>Trust by design</Eyebrow>
    </FadeUp>

    <FadeUp delay={6}>
      <SceneTitle>Every fact is labelled by confidence.</SceneTitle>
    </FadeUp>

    <FadeUp delay={12}>
      <SceneSubtitle>
        Reconstruction is interpretation. The model states what is documented,
        what is inferred, and what it simply doesn&rsquo;t know &mdash; rather
        than presenting a guess as history.
      </SceneSubtitle>
    </FadeUp>

    <div
      style={{
        marginTop: 34,
        display: "flex",
        flexWrap: "wrap",
        gap: 16,
      }}
    >
      {labels.map((label, index) => (
        <FadeUp key={label.name} delay={22 + index * 7} distance={16}>
          <div
            style={{
              background: theme.panel,
              border: `2px solid ${label.color}55`,
              borderLeft: `6px solid ${label.color}`,
              borderRadius: 12,
              padding: "16px 24px",
              minWidth: 268,
            }}
          >
            <div
              style={{
                fontFamily: monoStack,
                fontSize: 23,
                fontWeight: 700,
                color: label.color,
              }}
            >
              {label.name}
            </div>
            <div
              style={{
                fontFamily: fontStack,
                fontSize: 19,
                color: theme.muted,
                marginTop: 6,
              }}
            >
              {label.note}
            </div>
          </div>
        </FadeUp>
      ))}
    </div>
  </SceneLayout>
);
