import React from "react";
import { theme, fontStack } from "../theme";
import { Eyebrow, FadeUp, SceneLayout, SceneSubtitle, SceneTitle } from "../components";

// Simple line glyphs, one per real generator in ai-service/generation/archetypes.py.
const glyphs: { name: string; path: React.ReactNode }[] = [
  {
    name: "house",
    path: (
      <>
        <path d="M8 26 L28 10 L48 26" />
        <path d="M13 26 V44 H43 V26" />
        <path d="M24 44 V34 H32 V44" />
      </>
    ),
  },
  {
    name: "tower",
    path: (
      <>
        <path d="M18 46 V14 H38 V46" />
        <path d="M18 14 V9 H23 V14 M28 14 V9 H33 V14 M38 14 V9" />
        <path d="M25 46 V36 H31 V46" />
      </>
    ),
  },
  {
    name: "temple",
    path: (
      <>
        <path d="M8 20 L28 9 L48 20" />
        <path d="M12 20 V42 M22 20 V42 M34 20 V42 M44 20 V42" />
        <path d="M8 42 H48" />
      </>
    ),
  },
  {
    name: "wheel",
    path: (
      <>
        <circle cx="28" cy="27" r="18" />
        <circle cx="28" cy="27" r="4" />
        <path d="M28 9 V45 M10 27 H46 M15 14 L41 40 M41 14 L15 40" />
      </>
    ),
  },
  {
    name: "dome",
    path: (
      <>
        <path d="M14 30 Q14 12 28 12 Q42 12 42 30" />
        <path d="M28 12 V6" />
        <path d="M14 30 H42 V44 H14 Z" />
      </>
    ),
  },
  {
    name: "aircraft",
    path: (
      <>
        <path d="M28 8 Q32 20 32 30 L32 42 L28 48 L24 42 L24 30 Q24 20 28 8 Z" />
        <path d="M24 26 L6 38 L6 42 L24 36" />
        <path d="M32 26 L50 38 L50 42 L32 36" />
      </>
    ),
  },
  {
    name: "vehicle",
    path: (
      <>
        <path d="M8 34 V26 H18 L24 17 H38 L44 26 H50 V34" />
        <circle cx="18" cy="38" r="5" />
        <circle cx="40" cy="38" r="5" />
        <path d="M23 34 H35" />
      </>
    ),
  },
  {
    name: "ship",
    path: (
      <>
        <path d="M8 34 H48 L42 44 H14 Z" />
        <path d="M28 34 V8" />
        <path d="M28 14 H42 L28 22 Z" />
      </>
    ),
  },
  {
    name: "bridge",
    path: (
      <>
        <path d="M6 36 H50" />
        <path d="M16 36 V10 M40 36 V10" />
        <path d="M6 22 Q16 10 28 26 Q40 10 50 22" />
      </>
    ),
  },
];

export const Archetypes: React.FC = () => (
  <SceneLayout>
    <FadeUp>
      <Eyebrow>Real geometry, not templates</Eyebrow>
    </FadeUp>

    <FadeUp delay={6}>
      <SceneTitle>Nine structural archetypes.</SceneTitle>
    </FadeUp>

    <FadeUp delay={12}>
      <SceneSubtitle>
        The model picks a real shape family and its parameters &mdash; a dome&rsquo;s
        curve, a tower&rsquo;s lean, a wing&rsquo;s span &mdash; then geometry is
        generated procedurally.
      </SceneSubtitle>
    </FadeUp>

    <div
      style={{
        marginTop: 34,
        display: "grid",
        // One row of 9 - a 5-wide grid leaves an orphan row of 4.
        gridTemplateColumns: "repeat(9, 1fr)",
        gap: 14,
      }}
    >
      {glyphs.map((glyph, index) => (
        <FadeUp key={glyph.name} delay={20 + index * 5} distance={16}>
          <div
            style={{
              background: theme.panel,
              border: `2px solid ${theme.border}`,
              borderRadius: 16,
              padding: "18px 6px 12px",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 6,
            }}
          >
            <svg
              width="76"
              height="76"
              viewBox="0 0 56 56"
              fill="none"
              stroke={theme.blue}
              strokeWidth={2.4}
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              {glyph.path}
            </svg>
            <span
              style={{
                fontFamily: fontStack,
                fontSize: 17,
                color: theme.muted,
                letterSpacing: 0.4,
              }}
            >
              {glyph.name}
            </span>
          </div>
        </FadeUp>
      ))}
    </div>
  </SceneLayout>
);
