import React from "react";
import { Composition } from "remotion";
import { AlexandriasArchives, TOTAL_FRAMES } from "./AlexandriasArchives";

export const RemotionRoot: React.FC = () => (
  <Composition
    id="AlexandriasArchives"
    component={AlexandriasArchives}
    durationInFrames={TOTAL_FRAMES}
    fps={30}
    width={1920}
    height={1080}
  />
);
