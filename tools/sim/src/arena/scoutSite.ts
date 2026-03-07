import { Vec3 } from "vec3";
import type { BotState } from "../types";
import type { Logger } from "../util/log";
import type { SeededRandom } from "../util/random";
import type { RegionBounds } from "../util/regionBounds";
import { makeBounds } from "../util/regionBounds";

export interface ArenaSite {
  center: Vec3;
  floorY: number;
  bounds: RegionBounds;
}

function isAirLike(name: string): boolean {
  return name.includes("air") || name === "cave_air" || name === "void_air";
}

function sampleSkyClearScore(bot: BotState["bot"], center: Vec3, arenaSize: number): number {
  let score = 0;
  const half = Math.floor(arenaSize / 2);
  const step = Math.max(4, Math.floor(arenaSize / 6));

  for (let x = center.x - half; x <= center.x + half; x += step) {
    for (let z = center.z - half; z <= center.z + half; z += step) {
      for (let y = center.y; y <= center.y + 12; y += 3) {
        const block = bot.blockAt(new Vec3(x, y, z));
        if (!block) continue;
        score += isAirLike(block.name) ? 2 : -4;
      }
      const floorBlock = bot.blockAt(new Vec3(x, center.y - 1, z));
      if (floorBlock) {
        score += isAirLike(floorBlock.name) ? 1 : -2;
      }
    }
  }

  return score;
}

export async function scoutSite(
  scout: BotState,
  arenaSize: 50 | 75 | 100,
  rng: SeededRandom,
  logger: Logger
): Promise<ArenaSite> {
  const base = scout.bot.entity?.position?.floored() ?? new Vec3(0, 100, 0);
  const candidateY = [
    Math.max(120, base.y + 55),
    Math.max(132, base.y + 70),
    Math.max(145, base.y + 88)
  ];
  const offsets = [
    new Vec3(0, 0, 0),
    new Vec3(90, 0, 90),
    new Vec3(-90, 0, 90),
    new Vec3(90, 0, -90),
    new Vec3(-90, 0, -90),
    new Vec3(150, 0, 0),
    new Vec3(-150, 0, 0),
    new Vec3(0, 0, 150),
    new Vec3(0, 0, -150)
  ];

  let bestCenter = new Vec3(base.x, candidateY[0], base.z);
  let bestScore = Number.NEGATIVE_INFINITY;

  for (const y of candidateY) {
    for (const offset of rng.shuffle(offsets)) {
      const center = new Vec3(base.x + offset.x, y, base.z + offset.z);
      const score = sampleSkyClearScore(scout.bot, center, arenaSize);
      if (score > bestScore) {
        bestScore = score;
        bestCenter = center;
      }
    }
  }

  const floorY = bestCenter.y - 1;
  const bounds = makeBounds(bestCenter, arenaSize, floorY, 24);
  logger.info(
    "scouted build site center=%d,%d,%d size=%d score=%d",
    bestCenter.x,
    bestCenter.y,
    bestCenter.z,
    arenaSize,
    bestScore
  );

  return { center: bestCenter, floorY, bounds };
}
