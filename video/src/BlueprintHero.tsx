import React from "react";
import blueprints from "./data/blueprints.json";

export type BlueprintName = keyof typeof blueprints;

type Blueprint = {
  width: number;
  height: number;
  depth: number;
  blocks: number[][];
};

const ISO_COS = Math.cos(Math.PI / 6); // 30deg
const ISO_SIN = Math.sin(Math.PI / 6);

// Renders a real generated blueprint as a rotating isometric block model.
// The geometry is the actual output of ai-service/generation/archetypes.py
// (exported to data/blueprints.json), so these are literally the structures
// /aa build places in game - not a redrawn illustration of them.
export const BlueprintHero: React.FC<{
  name: BlueprintName;
  rotation: number;
  size: number;
  opacity?: number;
  tint?: string;
  explode?: number;
}> = ({ name, rotation, size, opacity = 1, tint = "#e8eef5", explode = 0 }) => {
  const blueprint = blueprints[name] as Blueprint;
  const { width, height, depth, blocks } = blueprint;

  const cx = width / 2;
  const cy = height / 2;
  const cz = depth / 2;

  const cos = Math.cos(rotation);
  const sin = Math.sin(rotation);

  // Project every block, keeping a depth key so we can paint back-to-front.
  const projected = blocks.map(([bx, by, bz]) => {
    const ox = bx - cx;
    const oy = by - cy;
    const oz = bz - cz;

    // Explode pushes blocks radially outward from the centre, echoing the
    // classic product "exploded view" beat.
    const spread = 1 + explode;
    const ex = ox * spread;
    const ez = oz * spread;
    const ey = oy * (1 + explode * 0.6);

    const rx = ex * cos - ez * sin;
    const rz = ex * sin + ez * cos;

    return {
      sx: (rx - rz) * ISO_COS,
      sy: (rx + rz) * ISO_SIN - ey,
      depthKey: rx + rz - ey,
      // Height and facing drive a simple lambert-ish shade so the model
      // reads as lit rather than as a flat silhouette.
      shade: Math.min(
        1,
        Math.max(0.18, 0.42 + (oy / Math.max(height, 1)) * 0.75 + (rx / Math.max(width, 1)) * 0.28),
      ),
    };
  });

  projected.sort((a, b) => a.depthKey - b.depthKey);

  const extent = Math.max(width, height, depth);
  const scale = size / (extent * 1.9);

  return (
    <svg
      width={size * 2}
      height={size * 2}
      viewBox={`${-size} ${-size} ${size * 2} ${size * 2}`}
      style={{ opacity, overflow: "visible" }}
      shapeRendering="crispEdges"
    >
      <g transform={`scale(${scale})`}>
        {projected.map((point, index) => (
          <rect
            key={index}
            x={point.sx - 0.62}
            y={point.sy - 0.62}
            width={1.24}
            height={1.24}
            fill={tint}
            opacity={point.shade}
          />
        ))}
      </g>
    </svg>
  );
};
