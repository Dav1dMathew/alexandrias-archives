import React from "react";
import { theme, fontStack } from "../theme";
import { Eyebrow, FadeUp, SceneLayout, SceneTitle } from "../components";

const working = [
  "Prompt → structure, live in Minecraft",
  "Real footprints from OpenStreetMap",
  "Reference photos from Wikipedia",
  "In-game build menu and progress bar",
];

const next = [
  "Photogrammetry for true 3D reconstruction",
  "Browser and VR viewer",
  "Curator review and versioning",
];

const Column: React.FC<{
  heading: string;
  accent: string;
  items: string[];
  delayBase: number;
}> = ({ heading, accent, items, delayBase }) => (
  <div style={{ flex: 1 }}>
    <FadeUp delay={delayBase}>
      <div
        style={{
          fontFamily: fontStack,
          fontSize: 25,
          fontWeight: 700,
          color: accent,
          marginBottom: 20,
          letterSpacing: 0.4,
        }}
      >
        {heading}
      </div>
    </FadeUp>
    <div style={{ display: "flex", flexDirection: "column", gap: 15 }}>
      {items.map((item, index) => (
        <FadeUp key={item} delay={delayBase + 8 + index * 6} distance={14}>
          <div style={{ display: "flex", gap: 14, alignItems: "flex-start" }}>
            <div
              style={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                background: accent,
                marginTop: 11,
                flexShrink: 0,
              }}
            />
            <span
              style={{
                fontFamily: fontStack,
                fontSize: 25,
                color: theme.text,
                opacity: 0.88,
                lineHeight: 1.35,
              }}
            >
              {item}
            </span>
          </div>
        </FadeUp>
      ))}
    </div>
  </div>
);

export const Status: React.FC = () => (
  <SceneLayout>
    <FadeUp>
      <Eyebrow>Where it stands</Eyebrow>
    </FadeUp>

    <FadeUp delay={6}>
      <SceneTitle>A working prototype, honestly scoped.</SceneTitle>
    </FadeUp>

    <div style={{ display: "flex", gap: 90, marginTop: 40 }}>
      <Column
        heading="Working today"
        accent={theme.green}
        items={working}
        delayBase={16}
      />
      <div style={{ width: 2, background: theme.border, opacity: 0.5 }} />
      <Column heading="Next" accent={theme.blue} items={next} delayBase={30} />
    </div>
  </SceneLayout>
);
