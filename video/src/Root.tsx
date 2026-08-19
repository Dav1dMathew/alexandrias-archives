import React from "react";
import { Composition } from "remotion";
import { AlexandriasArchives, TOTAL_FRAMES } from "./AlexandriasArchives";
import { Cinematic, CINEMATIC_FRAMES } from "./Cinematic";

export const RemotionRoot: React.FC = () => (
  <>
    {/* Explainer cut - information-dense, for the written submission */}
    <Composition
      id="AlexandriasArchives"
      component={AlexandriasArchives}
      durationInFrames={TOTAL_FRAMES}
      fps={30}
      width={1920}
      height={1080}
    />
    {/* Product-film cut - sparse and cinematic, for opening a pitch */}
    <Composition
      id="Cinematic"
      component={Cinematic}
      durationInFrames={CINEMATIC_FRAMES}
      fps={30}
      width={1920}
      height={1080}
    />
  </>
);
