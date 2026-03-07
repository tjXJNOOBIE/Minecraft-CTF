import { Vec3 } from "vec3";
import type { ArenaBuildResult, ArenaLayout, ArenaPalette, BotState, PlacementRecord, PlacementTracker, Team } from "../types";
import type { SeededRandom } from "../util/random";
import type { Logger } from "../util/log";
import type { WorldEditClient } from "../util/worldedit";
import type { RegionBounds } from "../util/regionBounds";
import { makeBounds, mirrorAcrossX } from "../util/regionBounds";
import type { ArenaSite } from "./scoutSite";
import { sleep } from "../util/safety";

type BuildContext = {
  admin: BotState;
  logger: Logger;
  rng: SeededRandom;
  worldEdit: WorldEditClient;
  layout: ArenaLayout;
  palette: ArenaPalette;
  tracker: PlacementTracker;
  usedWorldEdit: boolean;
  commandDelayMs: number;
};

class PlacementTrackerImpl implements PlacementTracker {
  private readonly map = new Map<string, PlacementRecord>();

  track(pos: Vec3, originalBlock: string, placedBlock: string): void {
    const key = `${pos.x},${pos.y},${pos.z}`;
    const existing = this.map.get(key);
    if (existing) {
      this.map.set(key, {
        ...existing,
        placedBlock
      });
      return;
    }
    this.map.set(key, {
      key,
      pos: pos.floored(),
      originalBlock,
      placedBlock
    });
  }

  list(): PlacementRecord[] {
    return [...this.map.values()];
  }

  count(): number {
    return this.map.size;
  }
}

function defaultPalette(): ArenaPalette {
  return {
    primary: ["stone_bricks", "cracked_stone_bricks", "andesite"],
    secondary: ["cobblestone", "mossy_cobblestone", "mossy_stone_bricks"],
    trim: ["polished_andesite", "smooth_stone_slab"],
    accent: ["spruce_fence", "spruce_trapdoor", "chain", "lantern"],
    floor: ["stone", "andesite", "coarse_dirt"],
    glass: ["tinted_glass", "black_stained_glass"],
    lights: ["lantern"],
    marker: "polished_andesite"
  };
}

function mergePalette(override?: Partial<ArenaPalette>): ArenaPalette {
  const base = defaultPalette();
  if (!override) return base;
  return {
    primary: override.primary?.length ? override.primary : base.primary,
    secondary: override.secondary?.length ? override.secondary : base.secondary,
    trim: override.trim?.length ? override.trim : base.trim,
    accent: override.accent?.length ? override.accent : base.accent,
    floor: override.floor?.length ? override.floor : base.floor,
    glass: override.glass?.length ? override.glass : base.glass,
    lights: override.lights?.length ? override.lights : base.lights,
    marker: override.marker || base.marker
  };
}

function detailLevelForSize(size: 50 | 75 | 100): "medium" | "high" {
  if (size === 75) return "high";
  return "medium";
}

export function createArenaLayout(site: ArenaSite, arenaSize: 50 | 75 | 100): ArenaLayout {
  const half = Math.floor(arenaSize / 2);
  const center = site.center.floored();
  const floorY = site.floorY;
  const laneOffset = Math.max(8, Math.floor(arenaSize * 0.24));
  const baseInset = Math.max(6, Math.floor(arenaSize * 0.16));
  const returnInset = Math.max(8, Math.floor(arenaSize * 0.2));

  const redSpawn = center.offset(-half + baseInset, 1, 0);
  const blueSpawn = center.offset(half - baseInset, 1, 0);
  // Keep the capture block on the floor so pathfinding bots can reliably touch it.
  const redFlag = center.offset(-half + baseInset + 4, 1, 0);
  const blueFlag = center.offset(half - baseInset - 4, 1, 0);

  const redReturnPoints: [Vec3, Vec3] = [
    center.offset(-half + returnInset, 1, Math.floor(laneOffset * 0.8)),
    center.offset(-half + returnInset, 1, -Math.floor(laneOffset * 0.8))
  ];
  const blueReturnPoints: [Vec3, Vec3] = [
    center.offset(half - returnInset, 1, Math.floor(laneOffset * 0.8)),
    center.offset(half - returnInset, 1, -Math.floor(laneOffset * 0.8))
  ];

  const laneSteps = Math.max(8, Math.floor(arenaSize / 4));
  const midLane: Vec3[] = [];
  const northLane: Vec3[] = [];
  const southLane: Vec3[] = [];
  for (let i = 0; i <= laneSteps; i += 1) {
    const t = i / laneSteps;
    const x = Math.floor((center.x - half + 4) * (1 - t) + (center.x + half - 4) * t);
    midLane.push(new Vec3(x, floorY + 1, center.z));
    northLane.push(new Vec3(x, floorY + 1, center.z - laneOffset));
    southLane.push(new Vec3(x, floorY + 1, center.z + laneOffset));
  }

  return {
    size: arenaSize,
    detailLevel: detailLevelForSize(arenaSize),
    center,
    floorY,
    bounds: makeBounds(center, arenaSize, floorY, 26),
    redSpawn,
    blueSpawn,
    redFlag,
    blueFlag,
    redReturnPoints,
    blueReturnPoints,
    redActiveReturn: redReturnPoints[0],
    blueActiveReturn: blueReturnPoints[0],
    midLane,
    northLane,
    southLane,
    safeInteriorRadius: Math.floor(arenaSize * 0.42)
  };
}

async function chatCommand(ctx: BuildContext, command: string): Promise<void> {
  ctx.admin.bot.chat(command);
  await sleep(ctx.commandDelayMs);
}

function pick(list: string[], rng: SeededRandom): string {
  return list[Math.max(0, Math.min(list.length - 1, rng.int(0, list.length - 1)))];
}

async function placeBlock(ctx: BuildContext, pos: Vec3, block: string): Promise<void> {
  const original = ctx.admin.bot.blockAt(pos)?.name ?? "air";
  ctx.tracker.track(pos, original, block);
  await chatCommand(ctx, `/setblock ${pos.x} ${pos.y} ${pos.z} ${block}`);
}

async function placeSymmetric(ctx: BuildContext, leftPos: Vec3, leftBlock: string, rightBlock = leftBlock): Promise<void> {
  await placeBlock(ctx, leftPos, leftBlock);
  const mirrored = mirrorAcrossX(ctx.layout.center.x, leftPos);
  if (mirrored.x === leftPos.x && mirrored.z === leftPos.z && mirrored.y === leftPos.y) {
    return;
  }
  await placeBlock(ctx, mirrored, rightBlock);
}

function trackVolume(ctx: BuildContext, min: Vec3, max: Vec3, placedBlock: string): void {
  for (let x = min.x; x <= max.x; x += 1) {
    for (let y = min.y; y <= max.y; y += 1) {
      for (let z = min.z; z <= max.z; z += 1) {
        const pos = new Vec3(x, y, z);
        const original = ctx.admin.bot.blockAt(pos)?.name ?? "air";
        ctx.tracker.track(pos, original, placedBlock);
      }
    }
  }
}

async function fillWithTracking(
  ctx: BuildContext,
  min: Vec3,
  max: Vec3,
  block: string
): Promise<void> {
  trackVolume(ctx, min, max, block);
  await chatCommand(ctx, `/fill ${min.x} ${min.y} ${min.z} ${max.x} ${max.y} ${max.z} ${block}`);
}

async function fillBoxWorldEditOrManual(
  ctx: BuildContext,
  min: Vec3,
  max: Vec3,
  block: string
): Promise<void> {
  const volume = (Math.abs(max.x - min.x) + 1) * (Math.abs(max.y - min.y) + 1) * (Math.abs(max.z - min.z) + 1);
  if (ctx.worldEdit.available && volume >= 1200) {
    ctx.usedWorldEdit = true;
    await ctx.worldEdit.setPos1(min);
    await ctx.worldEdit.setPos2(max);
    await ctx.worldEdit.set(block);
    return;
  }

  for (let x = min.x; x <= max.x; x += 1) {
    for (let y = min.y; y <= max.y; y += 1) {
      for (let z = min.z; z <= max.z; z += 1) {
        await placeBlock(ctx, new Vec3(x, y, z), block);
      }
    }
  }
}

async function applyWallDamage(ctx: BuildContext, bounds: RegionBounds): Promise<void> {
  const baseCrackPasses = ctx.layout.detailLevel === "high" ? 95 : 52;
  const crackScale = ctx.worldEdit.available ? 1 : 0.4;
  const crackPasses = Math.max(20, Math.floor(baseCrackPasses * crackScale));
  const leftMaxX = ctx.layout.center.x;
  for (let i = 0; i < crackPasses; i += 1) {
    const side = ctx.rng.pick(["north", "south", "west"] as const);
    let x = ctx.rng.int(bounds.minX, leftMaxX);
    let z = ctx.rng.int(bounds.minZ, bounds.maxZ);
    const y = ctx.rng.int(bounds.minY + 2, bounds.minY + 7);
    if (side === "north") z = bounds.minZ;
    if (side === "south") z = bounds.maxZ;
    if (side === "west") x = bounds.minX;
    const block = ctx.rng.chance(0.6) ? "cracked_stone_bricks" : "mossy_stone_bricks";
    await placeSymmetric(ctx, new Vec3(x, y, z), block);
  }
}

export async function buildPlatformTemplate(
  ctx: BuildContext,
  center: Vec3,
  size: number,
  palette: ArenaPalette
): Promise<void> {
  const half = Math.floor(size / 2);
  const y = ctx.layout.floorY;

  // Fast base platform.
  await fillWithTracking(
    ctx,
    new Vec3(center.x - half, y, center.z - half),
    new Vec3(center.x + half, y, center.z + half),
    palette.floor[0]
  );

  // Sparse mirrored floor detail pass.
  const basePatchAttempts = size === 75 ? 220 : size === 100 ? 180 : 120;
  const patchScale = ctx.worldEdit.available ? 1 : 0.35;
  const patchAttempts = Math.max(40, Math.floor(basePatchAttempts * patchScale));
  for (let i = 0; i < patchAttempts; i += 1) {
    const relX = ctx.rng.int(-half, 0);
    const z = center.z + ctx.rng.int(-half, half);
    const patchSize = ctx.rng.chance(0.7) ? 1 : 2;
    const block = ctx.rng.chance(0.35) ? palette.floor[2] : palette.floor[1];
    for (let dx = -patchSize; dx <= patchSize; dx += 1) {
      for (let dz = -patchSize; dz <= patchSize; dz += 1) {
        const left = new Vec3(center.x + relX + dx, y, z + dz);
        await placeSymmetric(ctx, left, block);
      }
    }
  }

  // Outer trim ring for strong silhouette.
  for (let x = center.x - half; x <= center.x; x += 1) {
    const northBlock = pick(palette.trim, ctx.rng);
    const southBlock = pick(palette.trim, ctx.rng);
    await placeSymmetric(ctx, new Vec3(x, y + 1, center.z - half), northBlock);
    await placeSymmetric(ctx, new Vec3(x, y + 1, center.z + half), southBlock);
  }
  for (let z = center.z - half; z <= center.z + half; z += 1) {
    await placeBlock(ctx, new Vec3(center.x - half, y + 1, z), pick(palette.trim, ctx.rng));
    await placeBlock(ctx, new Vec3(center.x + half, y + 1, z), pick(palette.trim, ctx.rng));
    // Centerline trims stay identical for both teams.
    if (ctx.rng.chance(0.15)) {
      await placeBlock(ctx, new Vec3(center.x, y + 1, z), pick(palette.trim, ctx.rng));
    }
  }
}

export async function buildCornerTowerTemplate(
  ctx: BuildContext,
  corner: Vec3,
  height: number
): Promise<void> {
  for (let y = corner.y; y <= corner.y + height; y += 1) {
    for (let dx = -2; dx <= 2; dx += 1) {
      for (let dz = -2; dz <= 2; dz += 1) {
        const edge = Math.abs(dx) === 2 || Math.abs(dz) === 2;
        if (!edge) continue;
        await placeBlock(ctx, corner.offset(dx, y - corner.y, dz), pick(ctx.palette.primary, ctx.rng));
      }
    }
    if ((y - corner.y) % 3 === 0) {
      await placeBlock(ctx, corner.offset(0, y - corner.y, 0), "lantern");
    }
  }
  await placeBlock(ctx, corner.offset(0, height + 1, 0), "spruce_fence");
}

export async function buildOuterWallTemplate(
  ctx: BuildContext,
  bounds: RegionBounds,
  wallHeight: number,
  towerInterval: number
): Promise<void> {
  const min = new Vec3(bounds.minX, bounds.minY + 1, bounds.minZ);
  const max = new Vec3(bounds.maxX, bounds.minY + wallHeight, bounds.maxZ);
  if (ctx.worldEdit.available) {
    ctx.usedWorldEdit = true;
    await ctx.worldEdit.setPos1(min);
    await ctx.worldEdit.setPos2(max);
    await ctx.worldEdit.walls("stone_bricks");
    await ctx.worldEdit.setPos1(new Vec3(bounds.minX, bounds.minY + wallHeight + 1, bounds.minZ));
    await ctx.worldEdit.setPos2(new Vec3(bounds.maxX, bounds.minY + wallHeight + 1, bounds.maxZ));
    await ctx.worldEdit.walls("smooth_stone_slab");
  } else {
    for (let y = bounds.minY + 1; y <= bounds.minY + wallHeight; y += 1) {
      await fillWithTracking(ctx, new Vec3(bounds.minX, y, bounds.minZ), new Vec3(bounds.maxX, y, bounds.minZ), "stone_bricks");
      await fillWithTracking(ctx, new Vec3(bounds.minX, y, bounds.maxZ), new Vec3(bounds.maxX, y, bounds.maxZ), "stone_bricks");
      await fillWithTracking(ctx, new Vec3(bounds.minX, y, bounds.minZ), new Vec3(bounds.minX, y, bounds.maxZ), "stone_bricks");
      await fillWithTracking(ctx, new Vec3(bounds.maxX, y, bounds.minZ), new Vec3(bounds.maxX, y, bounds.maxZ), "stone_bricks");
    }
  }

  // Tower cadence along walls.
  const y = bounds.minY + 1;
  for (let z = bounds.minZ + towerInterval; z < bounds.maxZ; z += towerInterval) {
    await placeBlock(ctx, new Vec3(bounds.minX, y, z), "polished_andesite");
    await placeBlock(ctx, new Vec3(bounds.maxX, y, z), "polished_andesite");
  }
  await applyWallDamage(ctx, bounds);
}

export async function buildSpawnPocketTemplate(
  ctx: BuildContext,
  teamSide: Team,
  spawnAreaBounds: RegionBounds,
  antiBowCampCover = true
): Promise<void> {
  const spawnX = teamSide === "red" ? spawnAreaBounds.minX + 3 : spawnAreaBounds.maxX - 3;
  const y = spawnAreaBounds.minY + 1;
  const zMid = Math.floor((spawnAreaBounds.minZ + spawnAreaBounds.maxZ) / 2);

  // Arch + roof cover.
  await fillBoxWorldEditOrManual(
    ctx,
    new Vec3(spawnX - 2, y + 3, zMid - 4),
    new Vec3(spawnX + 2, y + 4, zMid + 4),
    "stone_bricks"
  );

  // Side and mid exits.
  const dir = teamSide === "red" ? 1 : -1;
  for (let i = 0; i <= 5; i += 1) {
    await placeBlock(ctx, new Vec3(spawnX + dir * i, y + 1, zMid - 1), "air");
    await placeBlock(ctx, new Vec3(spawnX + dir * i, y + 1, zMid + 1), "air");
    await placeBlock(ctx, new Vec3(spawnX + dir * i, y + 1, zMid - 4), "air");
  }

  if (antiBowCampCover) {
    // S-bend line-of-sight breaker.
    for (let step = 0; step <= 4; step += 1) {
      const z = step % 2 === 0 ? zMid + 2 : zMid - 2;
      const x = spawnX + dir * (step + 1);
      await placeBlock(ctx, new Vec3(x, y + 1, z), "stone_bricks");
      await placeBlock(ctx, new Vec3(x, y + 2, z), pick(ctx.palette.secondary, ctx.rng));
    }
  }

  const banner = teamSide === "red" ? "red_banner" : "blue_banner";
  await placeBlock(ctx, new Vec3(spawnX, y + 2, zMid), banner);
}

export async function buildFlagPedestalTemplate(
  ctx: BuildContext,
  teamSide: Team,
  pedestalHeight: number,
  coverBlocks: string[]
): Promise<void> {
  const flag = teamSide === "red" ? ctx.layout.redFlag : ctx.layout.blueFlag;
  const base = new Vec3(flag.x + (teamSide === "red" ? -2 : 2), ctx.layout.floorY + 1, flag.z);

  await fillBoxWorldEditOrManual(
    ctx,
    base.offset(-1, 0, -2),
    base.offset(1, pedestalHeight - 1, 2),
    "stone_bricks"
  );

  await placeBlock(ctx, flag, ctx.palette.marker);

  // Ramps + cover.
  const dir = teamSide === "red" ? 1 : -1;
  for (let i = 1; i <= pedestalHeight + 1; i += 1) {
    const stepY = Math.max(0, i - 2);
    await placeBlock(ctx, base.offset(dir * i, stepY, 0), "smooth_stone_slab");
    await placeBlock(ctx, base.offset(dir * i, stepY, 1), "smooth_stone_slab");
    await placeBlock(ctx, base.offset(dir * i, stepY, -1), "smooth_stone_slab");
  }
  await placeBlock(ctx, base.offset(0, pedestalHeight, 2), pick(coverBlocks, ctx.rng));
  await placeBlock(ctx, base.offset(0, pedestalHeight, -2), pick(coverBlocks, ctx.rng));
}

export async function buildLaneCoverTemplate(
  ctx: BuildContext,
  lanePath: Vec3[],
  coverFrequency: number
): Promise<void> {
  for (let i = 0; i < lanePath.length; i += 1) {
    const point = lanePath[i];
    if (i % coverFrequency !== 0) continue;
    if (point.x > ctx.layout.center.x) continue;
    await placeSymmetric(ctx, point.offset(0, 1, 0), pick(ctx.palette.secondary, ctx.rng));
    await placeSymmetric(ctx, point.offset(0, 2, 0), pick(ctx.palette.secondary, ctx.rng));
    if (ctx.rng.chance(0.42)) {
      await placeSymmetric(ctx, point.offset(0, 3, 0), "spruce_fence");
      await placeSymmetric(ctx, point.offset(0, 4, 0), "lantern");
    }
  }
}

export async function buildReturnPointTemplate(
  ctx: BuildContext,
  location: Vec3,
  signText = "Return Point",
  markerBlock = "polished_andesite"
): Promise<void> {
  const y = ctx.layout.floorY + 1;
  for (let dx = -1; dx <= 1; dx += 1) {
    for (let dz = -1; dz <= 1; dz += 1) {
      await placeBlock(ctx, new Vec3(location.x + dx, y, location.z + dz), markerBlock);
    }
  }
  const signPos = new Vec3(location.x + 1, y + 1, location.z);
  await placeBlock(ctx, signPos, "spruce_sign");
  await chatCommand(
    ctx,
    `/data merge block ${signPos.x} ${signPos.y} ${signPos.z} {Text1:'{"text":"${signText}"}'}`
  );
}

async function buildMidRuins(ctx: BuildContext): Promise<void> {
  const arches = ctx.layout.detailLevel === "high" ? 4 : 2;
  const half = Math.floor(ctx.layout.size / 2);
  const xStep = Math.max(5, Math.floor(half / (arches + 1)));
  for (let i = 1; i <= arches; i += 1) {
    const x = ctx.layout.center.x - half + i * xStep;
    const z = ctx.layout.center.z + ctx.rng.pick([-3, 3]);
    for (let h = 0; h <= 4; h += 1) {
      await placeSymmetric(ctx, new Vec3(x, ctx.layout.floorY + 1 + h, z - 1), "mossy_stone_bricks");
      await placeSymmetric(ctx, new Vec3(x, ctx.layout.floorY + 1 + h, z + 1), "mossy_stone_bricks");
    }
    await placeSymmetric(ctx, new Vec3(x, ctx.layout.floorY + 5, z), "cracked_stone_bricks");
  }
}

async function buildMirroredHalfSetPieces(ctx: BuildContext): Promise<void> {
  const half = Math.floor(ctx.layout.size / 2);
  const y = ctx.layout.floorY + 1;
  const leftMaxX = ctx.layout.center.x;
  const leftMinX = ctx.layout.center.x - half;

  for (let x = leftMinX + 4; x <= leftMaxX; x += Math.max(3, Math.floor(ctx.layout.size / 10))) {
    const z = ctx.layout.center.z + ctx.rng.pick([-ctx.layout.size / 4, ctx.layout.size / 4]).valueOf();
    const pos = new Vec3(x, y, Math.floor(z));
    await placeSymmetric(ctx, pos, pick(ctx.palette.secondary, ctx.rng));
    if (ctx.rng.chance(0.35)) {
      await placeSymmetric(ctx, pos.offset(0, 1, 0), "vine");
    }
  }
}

export type BuildArenaOptions = {
  admin: BotState;
  site: ArenaSite;
  arenaSize: 50 | 75 | 100;
  rng: SeededRandom;
  logger: Logger;
  worldEdit: WorldEditClient;
  commandDelayMs: number;
  paletteOverride?: Partial<ArenaPalette>;
};

export async function buildArena50(options: BuildArenaOptions): Promise<ArenaBuildResult> {
  const layout = createArenaLayout(options.site, options.arenaSize);
  const tracker = new PlacementTrackerImpl();
  const ctx: BuildContext = {
    admin: options.admin,
    logger: options.logger,
    rng: options.rng,
    worldEdit: options.worldEdit,
    layout,
    palette: mergePalette(options.paletteOverride),
    tracker,
    usedWorldEdit: false,
    commandDelayMs: Math.max(30, options.commandDelayMs)
  };

  ctx.logger.info("building arena size=%d detail=%s", layout.size, layout.detailLevel);

  // Clear build volume for deterministic setup.
  if (ctx.worldEdit.available) {
    ctx.usedWorldEdit = true;
    await ctx.worldEdit.clearBounds(layout.bounds);
  }

  await buildPlatformTemplate(ctx, layout.center, layout.size, ctx.palette);
  await buildOuterWallTemplate(ctx, layout.bounds, 6, Math.max(8, Math.floor(layout.size / 5)));

  const corners = [
    new Vec3(layout.bounds.minX + 1, layout.floorY + 1, layout.bounds.minZ + 1),
    new Vec3(layout.bounds.minX + 1, layout.floorY + 1, layout.bounds.maxZ - 1),
    new Vec3(layout.bounds.maxX - 1, layout.floorY + 1, layout.bounds.minZ + 1),
    new Vec3(layout.bounds.maxX - 1, layout.floorY + 1, layout.bounds.maxZ - 1)
  ];
  for (const corner of corners) {
    await buildCornerTowerTemplate(ctx, corner, 9);
  }

  const redSpawnPocketBounds: RegionBounds = {
    minX: layout.bounds.minX + 1,
    maxX: layout.bounds.minX + Math.max(8, Math.floor(layout.size * 0.24)),
    minY: layout.floorY,
    maxY: layout.floorY + 8,
    minZ: layout.center.z - Math.max(8, Math.floor(layout.size * 0.18)),
    maxZ: layout.center.z + Math.max(8, Math.floor(layout.size * 0.18))
  };
  const blueSpawnPocketBounds: RegionBounds = {
    minX: layout.bounds.maxX - Math.max(8, Math.floor(layout.size * 0.24)),
    maxX: layout.bounds.maxX - 1,
    minY: layout.floorY,
    maxY: layout.floorY + 8,
    minZ: layout.center.z - Math.max(8, Math.floor(layout.size * 0.18)),
    maxZ: layout.center.z + Math.max(8, Math.floor(layout.size * 0.18))
  };
  await buildSpawnPocketTemplate(ctx, "red", redSpawnPocketBounds, true);
  await buildSpawnPocketTemplate(ctx, "blue", blueSpawnPocketBounds, true);

  await buildFlagPedestalTemplate(ctx, "red", 3, ctx.palette.secondary);
  await buildFlagPedestalTemplate(ctx, "blue", 3, ctx.palette.secondary);

  const coverFrequency = layout.detailLevel === "high" ? 2 : 3;
  await buildLaneCoverTemplate(ctx, layout.midLane, coverFrequency);
  await buildLaneCoverTemplate(ctx, layout.northLane, coverFrequency + 1);
  await buildLaneCoverTemplate(ctx, layout.southLane, coverFrequency + 1);

  await buildReturnPointTemplate(ctx, layout.redReturnPoints[0], "Return Point", ctx.palette.marker);
  await buildReturnPointTemplate(ctx, layout.redReturnPoints[1], "Return Point", ctx.palette.marker);
  await buildReturnPointTemplate(ctx, layout.blueReturnPoints[0], "Return Point", ctx.palette.marker);
  await buildReturnPointTemplate(ctx, layout.blueReturnPoints[1], "Return Point", ctx.palette.marker);

  await buildMidRuins(ctx);
  await buildMirroredHalfSetPieces(ctx);

  options.logger.info(
    "arena build done usedWorldEdit=%s trackedBlocks=%d",
    ctx.usedWorldEdit ? "yes" : "no",
    tracker.count()
  );

  return {
    layout,
    tracker,
    usedWorldEdit: ctx.usedWorldEdit,
    worldEdit: options.worldEdit
  };
}
