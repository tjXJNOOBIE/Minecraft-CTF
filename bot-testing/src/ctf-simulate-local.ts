import type { Bot } from "mineflayer";
import { goals } from "mineflayer-pathfinder";
import { Vec3 } from "vec3";
import { createBot, createRegistry, maybeJump, randomStrafe, schedule, selectKitOnOpen, stopStrafe } from "./headless-utils";
import { loadRuntimeConfig } from "./runtime-config";
import { buildArena50, createArenaLayout } from "../../tools/sim/src/arena/buildArena50";
import { scoutSite } from "../../tools/sim/src/arena/scoutSite";
import { setupPoints } from "../../tools/sim/src/game/setupPoints";
import type { ArenaLayout, BotState, Team } from "../../tools/sim/src/types";
import { createLogger } from "../../tools/sim/src/util/log";
import { createSeededRandom } from "../../tools/sim/src/util/random";
import { sleep } from "../../tools/sim/src/util/safety";
import type { RegionBounds } from "../../tools/sim/src/util/regionBounds";
import { containsPoint } from "../../tools/sim/src/util/regionBounds";
import type { WorldEditClient } from "../../tools/sim/src/util/worldedit";
import { detectWorldEdit } from "../../tools/sim/src/util/worldedit";

const { GoalNear } = goals;

type SimArgs = {
  host: string;
  port: number;
  bots: number;
  seedText: string;
};

type PreparedArena = {
  layout: ArenaLayout;
  worldEdit: WorldEditClient;
};

type SimpleRole = "DEFENDER" | "RUNNER" | "FLEX";
type SimpleGoal = "CARRY_FLAG" | "RETURN_OWN_FLAG" | "ATTACK_ENEMY_FLAG" | "DEFEND_BASE" | "PATROL";
type FlagColor = "red" | "blue";

type FlagSnapshot = {
  color: FlagColor;
  atBase: boolean;
  carrier?: string;
  droppedAt?: Vec3;
  lastEventAt: number;
};

type MatchState = {
  started: boolean;
  ended: boolean;
  overtime: boolean;
  lastMajorEventAt: number;
  redFlag: FlagSnapshot;
  blueFlag: FlagSnapshot;
};

type VisibleEnemy = {
  username: string;
  isHuman: boolean;
  position: Vec3;
  distance: number;
  isCarrier: boolean;
  nearOwnBase: boolean;
};

type DecisionDomain = {
  now: number;
  position: Vec3;
  ownFlag: FlagSnapshot;
  enemyFlag: FlagSnapshot;
  ownBase: Vec3;
  ownReturn: Vec3;
  enemyBase: Vec3;
  enemyFlagTarget: Vec3;
  visibleEnemies: VisibleEnemy[];
  forcedBlocker?: VisibleEnemy;
  lowHealth: boolean;
  carryingEnemyFlag: boolean;
  shouldReturnOwnFlag: boolean;
};

type GoalSelection = {
  goal: SimpleGoal;
  target: Vec3;
};

type BrainState = {
  bot: Bot;
  team: Team;
  role: SimpleRole;
  currentGoal: SimpleGoal;
  focusTargetId?: string;
  focusUntilMs: number;
  patrolIndex: number;
  isRanger: boolean;
  lastAbilityAt: number;
  lastRetreatEatAt: number;
  lastGoalAt: number;
};

function readArg(argv: string[], name: string): string | null {
  const flag = `--${name}`;
  const prefix = `${flag}=`;
  for (const entry of argv) {
    if (entry.startsWith(prefix)) {
      return entry.slice(prefix.length);
    }
  }

  const idx = argv.indexOf(flag);
  if (idx === -1) {
    return null;
  }
  return argv[idx + 1] ?? null;
}

function intOrDefault(raw: string | null | undefined, fallback: number): number {
  if (!raw) {
    return fallback;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? Math.floor(parsed) : fallback;
}

function parseArgs(argv: string[]): SimArgs {
  if (argv.includes("--help") || argv.includes("-h")) {
    console.log("Usage: npm run simulate -- --host <host> --port <port> --bots <bots> [--seed <seed>]");
    console.log("Defaults: host=127.0.0.1 port=25565 bots=12 seed=demo-seed");
    process.exit(0);
  }

  const host = readArg(argv, "host") || process.env.CTF_SIM_HOST || process.env.CTF_HOST || "127.0.0.1";
  const port = intOrDefault(readArg(argv, "port") || process.env.CTF_SIM_PORT || process.env.CTF_PORT, 25565);
  const bots = intOrDefault(readArg(argv, "bots") || process.env.CTF_SIM_BOTS, 12);
  const seedText = readArg(argv, "seed") || process.env.CTF_SIM_SEED || "demo-seed";

  return {
    host,
    port: port > 0 ? port : 25565,
    bots: Math.max(4, bots),
    seedText
  };
}

function makeName(prefix: string, index: number): string {
  return `${prefix}${String(index).padStart(2, "0")}`;
}

function teleport(bot: Bot, point: Vec3): void {
  bot.chat(`/tp ${bot.username} ${point.x} ${point.y} ${point.z}`);
}

function nudge(bot: Bot, durationMs: number): void {
  bot.setControlState("forward", true);
  setTimeout(() => bot.setControlState("forward", false), durationMs);
}

function isAirLike(name: string | null | undefined): boolean {
  if (!name) {
    return true;
  }
  return name.includes("air") || name === "cave_air" || name === "void_air";
}

function blockName(bot: Bot, point: Vec3): string {
  return bot.blockAt(point)?.name ?? "air";
}

function digFlag(bot: Bot, flagPoint: Vec3): void {
  const target = flagPoint.offset(0, -1, 0);
  const block = bot.blockAt(target) ?? bot.blockAt(flagPoint);
  if (!block) {
    console.log("[%s] flag block missing at %d %d %d", bot.username, flagPoint.x, flagPoint.y, flagPoint.z);
    return;
  }
  bot.dig(block, true).catch((err: any) => {
    console.log("[%s] dig error %s", bot.username, err?.message ?? err);
  });
}

function quitBots(bots: Bot[]): void {
  for (const bot of bots) {
    try {
      bot.quit();
    } catch {
      // ignore
    }
  }
}

function makeBotState(bot: Bot, team: "red" | "blue"): BotState {
  const anchor = bot.entity?.position?.floored() ?? new Vec3(0, 64, 0);
  return {
    bot,
    username: bot.username,
    id: 0,
    team,
    role: "defender",
    isSilent: false,
    isLateJoiner: false,
    hasJoinedTeam: false,
    online: true,
    reactionMs: 0,
    aimJitter: 0,
    infoLagMs: 0,
    tunnelVisionUntil: 0,
    lastSeenEnemyAt: 0,
    lastDecisionAt: 0,
    nextRoleSwapAt: 0,
    nextCommsAt: 0,
    nextThinkingPauseAt: 0,
    nextAttentionLookAt: 0,
    mistakesUntil: 0,
    lowHealthRetreatUntil: 0,
    pushAbortAt: 0,
    temporaryGrudgeUntil: 0,
    objectiveLoopCount: 0,
    misses: 0,
    hits: 0,
    deaths: 0,
    kills: 0,
    tilt: 0,
    morale: 0,
    spacingBias: 0,
    defenderAnchor: anchor
  };
}

async function runAdminCommand(bot: Bot, command: string, waitMs = 180): Promise<void> {
  bot.chat(command);
  await sleep(waitMs);
}

async function fillBox(bot: Bot, min: Vec3, max: Vec3, block: string): Promise<void> {
  await runAdminCommand(bot, `/fill ${min.x} ${min.y} ${min.z} ${max.x} ${max.y} ${max.z} ${block}`);
}

function lobbyCenter(layout: ArenaLayout): Vec3 {
  return new Vec3(layout.center.x, layout.floorY + 8, layout.bounds.minZ - 18);
}

function lobbyBounds(layout: ArenaLayout): RegionBounds {
  const center = lobbyCenter(layout);
  return {
    minX: center.x - 8,
    maxX: center.x + 8,
    minY: center.y - 2,
    maxY: center.y + 6,
    minZ: center.z - 8,
    maxZ: center.z + 8
  };
}

function arenaLooksBuilt(bot: Bot, layout: ArenaLayout, logger: ReturnType<typeof createLogger>): boolean {
  const probes = [
    new Vec3(layout.center.x, layout.floorY, layout.center.z),
    new Vec3(layout.bounds.minX, layout.floorY + 2, layout.center.z),
    new Vec3(layout.bounds.maxX, layout.floorY + 2, layout.center.z),
    new Vec3(layout.center.x, layout.floorY + 2, layout.bounds.minZ),
    new Vec3(layout.center.x, layout.floorY + 2, layout.bounds.maxZ),
    layout.redSpawn.offset(0, -1, 0),
    layout.blueSpawn.offset(0, -1, 0),
    layout.redActiveReturn.offset(0, -1, 0),
    layout.blueActiveReturn.offset(0, -1, 0)
  ];

  const solidHits = probes.reduce((count, point) => count + (isAirLike(blockName(bot, point)) ? 0 : 1), 0);
  logger.info("arena probe hits=%d/%d", solidHits, probes.length);
  return solidHits >= 7;
}

function lobbyLooksBuilt(bot: Bot, layout: ArenaLayout): boolean {
  const center = lobbyCenter(layout);
  const floorBlock = blockName(bot, center.offset(0, -1, 0));
  const edgeBlock = blockName(bot, center.offset(6, -1, 0));
  return floorBlock === "smooth_stone" && edgeBlock === "polished_andesite";
}

async function buildLobby(bot: Bot, layout: ArenaLayout, logger: ReturnType<typeof createLogger>): Promise<Vec3> {
  const center = lobbyCenter(layout);
  const clearMin = center.offset(-8, -2, -8);
  const clearMax = center.offset(8, 6, 8);
  const floorMin = center.offset(-6, 0, -6);
  const floorMax = center.offset(6, 0, 6);
  const roofMin = center.offset(-3, 4, -3);
  const roofMax = center.offset(3, 4, 3);
  const spawn = center.offset(0, 1, 0);

  logger.info("building lobby center=%d,%d,%d", center.x, center.y, center.z);

  await fillBox(bot, clearMin, clearMax, "air");
  await fillBox(bot, floorMin, floorMax, "smooth_stone");
  await fillBox(bot, center.offset(-6, 0, -6), center.offset(6, 0, -6), "polished_andesite");
  await fillBox(bot, center.offset(-6, 0, 6), center.offset(6, 0, 6), "polished_andesite");
  await fillBox(bot, center.offset(-6, 0, -6), center.offset(-6, 0, 6), "polished_andesite");
  await fillBox(bot, center.offset(6, 0, -6), center.offset(6, 0, 6), "polished_andesite");
  await fillBox(bot, center.offset(-5, 1, -5), center.offset(-5, 3, -5), "sea_lantern");
  await fillBox(bot, center.offset(-5, 1, 5), center.offset(-5, 3, 5), "sea_lantern");
  await fillBox(bot, center.offset(5, 1, -5), center.offset(5, 3, -5), "sea_lantern");
  await fillBox(bot, center.offset(5, 1, 5), center.offset(5, 3, 5), "sea_lantern");
  await fillBox(bot, roofMin, roofMax, "spruce_planks");

  teleport(bot, spawn);
  await sleep(400);
  await runAdminCommand(bot, "/ctf setlobby");
  logger.info("lobby ready spawn=%d,%d,%d", spawn.x, spawn.y, spawn.z);
  return spawn;
}

function findReusableArenaLayout(
  bot: Bot,
  layout: ArenaLayout,
  logger: ReturnType<typeof createLogger>
): ArenaLayout | null {
  const minFloorY = Math.max(120, layout.floorY - 40);
  for (let floorY = layout.floorY; floorY >= minFloorY; floorY -= 1) {
    const candidate = createArenaLayout(
      {
        center: new Vec3(layout.center.x, floorY + 1, layout.center.z),
        floorY,
        bounds: layout.bounds
      },
      layout.size
    );
    if (arenaLooksBuilt(bot, candidate, logger)) {
      return candidate;
    }
  }
  return null;
}

async function prepareArena(admin: Bot, seedText: string, logger: ReturnType<typeof createLogger>): Promise<PreparedArena> {
  const rng = createSeededRandom(seedText);
  const adminState = makeBotState(admin, "red");
  const worldEdit = await detectWorldEdit(admin, logger.child("worldedit"));
  const site = await scoutSite(adminState, 50, rng, logger.child("scout"));
  const layout = createArenaLayout(site, 50);
  const reusableLayout = findReusableArenaLayout(admin, layout, logger.child("detect"));

  if (reusableLayout) {
    logger.info(
      "reusing existing arena center=%d,%d,%d floorY=%d",
      reusableLayout.center.x,
      reusableLayout.center.y,
      reusableLayout.center.z,
      reusableLayout.floorY
    );
  } else {
    await buildArena50({
      admin: adminState,
      site,
      arenaSize: 50,
      rng,
      logger: logger.child("arena"),
      worldEdit,
      commandDelayMs: worldEdit.available ? 90 : 40
    });
  }

  const activeLayout = reusableLayout ?? layout;

  if (!lobbyLooksBuilt(admin, activeLayout)) {
    await buildLobby(admin, activeLayout, logger.child("lobby"));
  } else {
    const spawn = lobbyCenter(activeLayout).offset(0, 1, 0);
    teleport(admin, spawn);
    await sleep(400);
    await runAdminCommand(admin, "/ctf setlobby");
    logger.info("reusing lobby spawn=%d,%d,%d", spawn.x, spawn.y, spawn.z);
  }

  await setupPoints(
    adminState,
    activeLayout,
    logger.child("setup"),
    async (command: string) => runAdminCommand(admin, command, 180)
  );
  return {
    layout: activeLayout,
    worldEdit
  };
}

function useSpear(bot: Bot): void {
  try {
    bot.setQuickBarSlot(2);
    bot.activateItem();
    setTimeout(() => bot.deactivateItem(), 700);
  } catch {
    // ignore
  }
}

function useScoutTagger(bot: Bot): void {
  try {
    bot.setQuickBarSlot(0);
    bot.activateItem();
    setTimeout(() => bot.deactivateItem(), 250);
  } catch {
    // ignore
  }
}

async function clearBoundsByLayer(bot: Bot, bounds: RegionBounds): Promise<void> {
  for (let y = bounds.maxY; y >= bounds.minY; y -= 1) {
    await fillBox(
      bot,
      new Vec3(bounds.minX, y, bounds.minZ),
      new Vec3(bounds.maxX, y, bounds.maxZ),
      "air"
    );
  }
}

async function destroyTemporaryStructures(
  admin: Bot,
  preparedArena: PreparedArena,
  logger: ReturnType<typeof createLogger>
): Promise<void> {
  logger.info("destroying temporary arena and lobby");
  if (preparedArena.worldEdit.available) {
    await preparedArena.worldEdit.clearBounds(preparedArena.layout.bounds);
    await preparedArena.worldEdit.clearBounds(lobbyBounds(preparedArena.layout));
    return;
  }

  await clearBoundsByLayer(admin, preparedArena.layout.bounds);
  await clearBoundsByLayer(admin, lobbyBounds(preparedArena.layout));
}

function triggerAbility(bot: Bot, rangerBots: ReadonlySet<string>): void {
  if (rangerBots.has(bot.username)) {
    useSpear(bot);
    return;
  }
  useScoutTagger(bot);
}

function createInitialMatchState(): MatchState {
  const now = Date.now();
  return {
    started: false,
    ended: false,
    overtime: false,
    lastMajorEventAt: now,
    redFlag: {
      color: "red",
      atBase: true,
      lastEventAt: now
    },
    blueFlag: {
      color: "blue",
      atBase: true,
      lastEventAt: now
    }
  };
}

function flagForTeam(matchState: MatchState, team: Team): FlagSnapshot {
  return team === "red" ? matchState.redFlag : matchState.blueFlag;
}

function enemyFlagForTeam(matchState: MatchState, team: Team): FlagSnapshot {
  return team === "red" ? matchState.blueFlag : matchState.redFlag;
}

function ownBase(layout: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? layout.redFlag : layout.blueFlag;
}

function enemyBase(layout: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? layout.blueFlag : layout.redFlag;
}

function ownReturn(layout: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? layout.redActiveReturn : layout.blueActiveReturn;
}

function lanePath(layout: ArenaLayout, role: SimpleRole): Vec3[] {
  if (role === "DEFENDER") {
    return layout.midLane;
  }
  if (role === "RUNNER") {
    return layout.northLane.length >= layout.southLane.length ? layout.northLane : layout.southLane;
  }
  return layout.southLane.length >= layout.northLane.length ? layout.southLane : layout.northLane;
}

function patrolTarget(brain: BrainState, layout: ArenaLayout): Vec3 {
  const path = lanePath(layout, brain.role);
  const index = brain.patrolIndex % Math.max(1, path.length);
  const anchor = path[index] ?? layout.center;
  if (brain.role === "DEFENDER") {
    return ownReturn(layout, brain.team).offset(brain.team === "red" ? -3 : 3, 0, (index % 3) - 1);
  }
  return anchor.offset(brain.team === "red" ? -1 : 1, 0, (index % 5) - 2);
}

function createBrains(allBots: Bot[], redTeam: Bot[], rangerBots: ReadonlySet<string>): BrainState[] {
  return allBots.map((bot) => ({
    bot,
    team: redTeam.includes(bot) ? "red" : "blue",
    role: "FLEX",
    currentGoal: "PATROL",
    focusUntilMs: 0,
    patrolIndex: 0,
    isRanger: rangerBots.has(bot.username),
    lastAbilityAt: 0,
    lastRetreatEatAt: 0,
    lastGoalAt: 0
  }));
}

function assignRoles(brains: BrainState[], team: Team): void {
  const teamBrains = brains
    .filter((brain) => brain.team === team)
    .sort((left, right) => left.bot.username.localeCompare(right.bot.username));
  if (teamBrains.length === 0) {
    return;
  }

  const defenderCount = Math.max(1, Math.floor(teamBrains.length * 0.25));
  const flexCount = Math.max(1, Math.floor(teamBrains.length * 0.25));
  const runnerCount = Math.max(1, teamBrains.length - defenderCount - flexCount);

  teamBrains.forEach((brain, index) => {
    if (index < defenderCount) {
      brain.role = "DEFENDER";
      return;
    }
    if (index < defenderCount + runnerCount) {
      brain.role = "RUNNER";
      return;
    }
    brain.role = "FLEX";
  });
}

function maybeReassignRoles(
  brains: BrainState[],
  matchState: MatchState,
  nextReassignAt: { value: number },
  roleEventMarker: { value: number }
): void {
  const now = Date.now();
  const dueToTimer = now >= nextReassignAt.value;
  const dueToEvent = matchState.lastMajorEventAt > roleEventMarker.value;
  if (!dueToTimer && !dueToEvent) {
    return;
  }

  assignRoles(brains, "red");
  assignRoles(brains, "blue");
  nextReassignAt.value = now + 30000 + Math.floor(Math.random() * 30000);
  roleEventMarker.value = matchState.lastMajorEventAt;
}

function clearFocus(brain: BrainState): void {
  brain.focusTargetId = undefined;
  brain.focusUntilMs = 0;
}

function snapshotPlayerPosition(bots: Bot[], username: string): Vec3 | undefined {
  for (const bot of bots) {
    const position = bot.players[username]?.entity?.position?.floored();
    if (position) {
      return position;
    }
  }
  return undefined;
}

function parseFlagColor(raw: string): FlagColor | null {
  const lower = raw.toLowerCase();
  if (lower === "red" || lower === "blue") {
    return lower;
  }
  return null;
}

function updateFlagPickup(matchState: MatchState, color: FlagColor, carrier: string): void {
  const flag = color === "red" ? matchState.redFlag : matchState.blueFlag;
  flag.atBase = false;
  flag.carrier = carrier;
  flag.droppedAt = undefined;
  flag.lastEventAt = Date.now();
  matchState.lastMajorEventAt = flag.lastEventAt;
}

function updateFlagDrop(matchState: MatchState, color: FlagColor, droppedAt?: Vec3): void {
  const flag = color === "red" ? matchState.redFlag : matchState.blueFlag;
  flag.atBase = false;
  flag.carrier = undefined;
  flag.droppedAt = droppedAt;
  flag.lastEventAt = Date.now();
  matchState.lastMajorEventAt = flag.lastEventAt;
}

function updateFlagReturn(matchState: MatchState, color: FlagColor): void {
  const flag = color === "red" ? matchState.redFlag : matchState.blueFlag;
  flag.atBase = true;
  flag.carrier = undefined;
  flag.droppedAt = undefined;
  flag.lastEventAt = Date.now();
  matchState.lastMajorEventAt = flag.lastEventAt;
}

function attachMatchTracking(
  bots: Bot[],
  brains: BrainState[],
  matchState: MatchState,
  logger: ReturnType<typeof createLogger>
): void {
  for (const bot of bots) {
    bot.on("messagestr", (message) => {
      const text = message.trim();
      if (!text) {
        return;
      }

      if (text.includes("CTF match started.")) {
        matchState.started = true;
        matchState.ended = false;
        matchState.lastMajorEventAt = Date.now();
        return;
      }

      if (text.includes("OVERTIME!")) {
        matchState.overtime = true;
        matchState.lastMajorEventAt = Date.now();
        return;
      }

      if (text.includes(" wins!") || text.includes("CTF stopped:") || text === "CTF stopped.") {
        matchState.ended = true;
        matchState.lastMajorEventAt = Date.now();
        return;
      }

      if (text.includes("You are the flag carrier!")) {
        const brain = brains.find((entry) => entry.bot.username === bot.username);
        if (!brain) {
          return;
        }
        updateFlagPickup(matchState, brain.team === "red" ? "blue" : "red", bot.username);
        return;
      }

      let match = text.match(/^(.+?) picked up the (red|blue) flag\.$/i);
      if (match) {
        const carrier = match[1];
        const color = parseFlagColor(match[2]);
        if (color) {
          updateFlagPickup(matchState, color, carrier);
        }
        return;
      }

      match = text.match(/^(.+?) dropped the (red|blue) flag\.$/i);
      if (match) {
        const color = parseFlagColor(match[2]);
        if (color) {
          updateFlagDrop(matchState, color, snapshotPlayerPosition(bots, match[1]));
        }
        return;
      }

      match = text.match(/^(.+?) returned (?:your )?(red|blue) flag\.$/i);
      if (match) {
        const color = parseFlagColor(match[2]);
        if (color) {
          updateFlagReturn(matchState, color);
        }
        return;
      }

      match = text.match(/^(.+?) captured the (red|blue) flag for (.+?)\.$/i);
      if (match) {
        const color = parseFlagColor(match[2]);
        if (color) {
          updateFlagReturn(matchState, color);
        }
        matchState.lastMajorEventAt = Date.now();
        logger.info("capture event %s", text);
      }
    });
  }
}

function buildCommittedAttackers(brains: BrainState[], now: number): Map<string, number> {
  const counts = new Map<string, number>();
  for (const brain of brains) {
    if (!brain.focusTargetId || brain.focusUntilMs <= now) {
      continue;
    }
    counts.set(brain.focusTargetId, (counts.get(brain.focusTargetId) ?? 0) + 1);
  }
  return counts;
}

function enemiesVisibleToBrain(brain: BrainState, brains: BrainState[], layout: ArenaLayout, matchState: MatchState): VisibleEnemy[] {
  const self = brain.bot.entity?.position;
  if (!self) {
    return [];
  }

  const knownTeams = new Map(brains.map((entry) => [entry.bot.username, entry.team] as const));
  const ownBasePoint = ownBase(layout, brain.team);
  const ownFlag = flagForTeam(matchState, brain.team);
  const enemies: VisibleEnemy[] = [];

  for (const [username, info] of Object.entries(brain.bot.players)) {
    const entity = info?.entity;
    if (!entity || username === brain.bot.username) {
      continue;
    }

    const team = knownTeams.get(username);
    if (team === brain.team) {
      continue;
    }

    const position = entity.position.floored();
    if (!containsPoint(layout.bounds, position, 0)) {
      continue;
    }

    const distance = self.distanceTo(entity.position);
    if (distance > 26) {
      continue;
    }

    enemies.push({
      username,
      isHuman: !knownTeams.has(username),
      position,
      distance,
      isCarrier: ownFlag.carrier === username,
      nearOwnBase: position.distanceTo(ownBasePoint) <= 12
    });
  }

  return enemies.sort((left, right) => left.distance - right.distance);
}

function isOneOfClosestFlagReturners(
  brain: BrainState,
  teammates: BrainState[],
  droppedAt: Vec3 | undefined
): boolean {
  if (!brain.bot.entity || !droppedAt) {
    return false;
  }

  const ranked = teammates
    .filter((teammate) => teammate.bot.entity)
    .map((teammate) => ({
      username: teammate.bot.username,
      distance: teammate.bot.entity!.position.distanceTo(droppedAt)
    }))
    .sort((left, right) => left.distance - right.distance)
    .slice(0, Math.min(2, teammates.length));
  return ranked.some((entry) => entry.username === brain.bot.username);
}

function domainForBrain(
  brain: BrainState,
  brains: BrainState[],
  layout: ArenaLayout,
  matchState: MatchState,
  now: number
): DecisionDomain | null {
  const position = brain.bot.entity?.position?.floored();
  if (!position) {
    return null;
  }

  const ownFlag = flagForTeam(matchState, brain.team);
  const enemyFlag = enemyFlagForTeam(matchState, brain.team);
  const teammates = brains.filter((entry) => entry.team === brain.team);
  const visibleEnemies = enemiesVisibleToBrain(brain, brains, layout, matchState);

  return {
    now,
    position,
    ownFlag,
    enemyFlag,
    ownBase: ownBase(layout, brain.team),
    ownReturn: ownReturn(layout, brain.team),
    enemyBase: enemyBase(layout, brain.team),
    enemyFlagTarget: enemyFlag.droppedAt ?? enemyBase(layout, brain.team),
    visibleEnemies,
    forcedBlocker: visibleEnemies.find((enemy) => enemy.distance <= 3.5),
    lowHealth: (brain.bot.health ?? 20) <= 8,
    carryingEnemyFlag: enemyFlag.carrier === brain.bot.username,
    shouldReturnOwnFlag: !ownFlag.atBase && !ownFlag.carrier && isOneOfClosestFlagReturners(brain, teammates, ownFlag.droppedAt)
  };
}

function selectGoal(brain: BrainState, layout: ArenaLayout, domain: DecisionDomain): GoalSelection {
  if (domain.carryingEnemyFlag) {
    return {
      goal: "CARRY_FLAG",
      target: domain.ownReturn.offset(0, 1, 0)
    };
  }

  if (domain.shouldReturnOwnFlag && domain.ownFlag.droppedAt) {
    return {
      goal: "RETURN_OWN_FLAG",
      target: domain.ownFlag.droppedAt
    };
  }

  if (brain.role === "DEFENDER") {
    const threat = domain.visibleEnemies.find((enemy) => enemy.isCarrier || enemy.nearOwnBase);
    if (threat) {
      return {
        goal: "DEFEND_BASE",
        target: threat.position
      };
    }
    return {
      goal: "DEFEND_BASE",
      target: domain.ownReturn.offset(brain.team === "red" ? -3 : 3, 0, brain.patrolIndex % 5 - 2)
    };
  }

  if (brain.role === "RUNNER") {
    return {
      goal: "ATTACK_ENEMY_FLAG",
      target: domain.enemyFlagTarget
    };
  }

  const carrierThreat = domain.visibleEnemies.find((enemy) => enemy.isCarrier);
  if (carrierThreat) {
    return {
      goal: "DEFEND_BASE",
      target: carrierThreat.position
    };
  }

  if (!domain.enemyFlag.atBase && domain.enemyFlag.droppedAt) {
    return {
      goal: "ATTACK_ENEMY_FLAG",
      target: domain.enemyFlag.droppedAt
    };
  }

  return {
    goal: "PATROL",
    target: patrolTarget(brain, layout)
  };
}

function attackersCapFor(target: VisibleEnemy): number {
  if (target.isCarrier) {
    return 3;
  }
  return target.isHuman ? 2 : 3;
}

function scoreEnemyTarget(target: VisibleEnemy, selection: GoalSelection): number {
  let score = Math.max(0, 20 - target.distance);
  if (target.isCarrier) {
    score += 100;
  }
  if (target.nearOwnBase) {
    score += 30;
  }
  score += Math.max(0, 14 - selection.target.distanceTo(target.position));
  if (selection.goal === "DEFEND_BASE" && target.nearOwnBase) {
    score += 15;
  }
  return score;
}

function chooseFocusTarget(
  brain: BrainState,
  domain: DecisionDomain,
  selection: GoalSelection,
  committedAttackers: Map<string, number>
): VisibleEnemy | undefined {
  const now = domain.now;
  if (brain.focusTargetId && brain.focusUntilMs > now) {
    const existing = domain.visibleEnemies.find((enemy) => enemy.username === brain.focusTargetId);
    if (existing) {
      return existing;
    }
  }

  const ranked = domain.visibleEnemies
    .map((enemy) => ({
      enemy,
      score: scoreEnemyTarget(enemy, selection)
    }))
    .sort((left, right) => right.score - left.score);

  for (const entry of ranked) {
    const cap = attackersCapFor(entry.enemy);
    const alreadyCommitted = committedAttackers.get(entry.enemy.username) ?? 0;
    if (alreadyCommitted >= cap) {
      continue;
    }
    brain.focusTargetId = entry.enemy.username;
    brain.focusUntilMs = now + 2000 + Math.floor(Math.random() * 3000);
    committedAttackers.set(entry.enemy.username, alreadyCommitted + 1);
    return entry.enemy;
  }

  clearFocus(brain);
  return undefined;
}

function setMoveGoal(brain: BrainState, target: Vec3, range: number): void {
  if (brain.lastGoalAt > Date.now() - 250) {
    return;
  }
  brain.lastGoalAt = Date.now();
  brain.bot.setControlState("sprint", true);
  brain.bot.pathfinder.setGoal(new GoalNear(target.x, target.y, target.z, range), false);
}

function stopCombatControls(bot: Bot): void {
  bot.setControlState("left", false);
  bot.setControlState("right", false);
  bot.setControlState("back", false);
  bot.setControlState("jump", false);
  bot.setControlState("sprint", false);
  stopStrafe(bot);
}

function findFoodSlot(bot: Bot): number {
  const item = bot.inventory.items().find((entry) =>
    entry.name.includes("bread") || entry.name.includes("cooked") || entry.name.includes("apple")
  );
  if (!item) {
    return -1;
  }
  const hotbar = item.slot - 36;
  return hotbar >= 0 && hotbar <= 8 ? hotbar : -1;
}

function maybeEatAndRetreat(brain: BrainState, domain: DecisionDomain): boolean {
  if (!domain.lowHealth) {
    return false;
  }
  const slot = findFoodSlot(brain.bot);
  setMoveGoal(brain, domain.ownReturn, 2);
  brain.bot.setControlState("jump", true);
  setTimeout(() => brain.bot.setControlState("jump", false), 250);
  if (slot >= 0 && domain.now - brain.lastRetreatEatAt >= 2500) {
    brain.lastRetreatEatAt = domain.now;
    try {
      brain.bot.setQuickBarSlot(slot);
      brain.bot.activateItem();
      setTimeout(() => brain.bot.deactivateItem(), 900);
    } catch {
      // ignore
    }
  }
  return true;
}

function maybeTakeShot(brain: BrainState, target: VisibleEnemy, rangerBots: ReadonlySet<string>, now: number): boolean {
  if (target.distance < 5 || target.distance > 14) {
    return false;
  }
  if (now - brain.lastAbilityAt < 2500) {
    return false;
  }
  brain.lastAbilityAt = now;
  triggerAbility(brain.bot, rangerBots);
  return true;
}

function maybeMeleeFight(brain: BrainState, target: VisibleEnemy): boolean {
  const entity = brain.bot.players[target.username]?.entity;
  if (!entity) {
    return false;
  }

  try {
    brain.bot.lookAt(entity.position.offset(0, 1.3, 0), true);
  } catch {
    // ignore
  }

  if (target.distance > 3.2) {
    return false;
  }

  try {
    brain.bot.attack(entity);
  } catch {
    // ignore
  }
  randomStrafe(brain.bot);
  maybeJump(brain.bot);
  return true;
}

function maybePickupEnemyFlag(brain: BrainState, domain: DecisionDomain, layout: ArenaLayout): void {
  if (domain.carryingEnemyFlag) {
    return;
  }
  const target = domain.enemyFlag.droppedAt ?? enemyBase(layout, brain.team);
  if (domain.position.distanceTo(target) > 2.2) {
    return;
  }
  if (domain.enemyFlag.atBase) {
    digFlag(brain.bot, target);
  }
}

function executeBrainTick(
  brain: BrainState,
  brains: BrainState[],
  layout: ArenaLayout,
  matchState: MatchState,
  rangerBots: ReadonlySet<string>,
  committedAttackers: Map<string, number>
): void {
  // Validation
  if (!matchState.started || matchState.ended || !brain.bot.entity) {
    return;
  }

  // Domain lookup
  const domain = domainForBrain(brain, brains, layout, matchState, Date.now());
  if (!domain) {
    return;
  }

  // Goal selection
  const selection = selectGoal(brain, layout, domain);
  brain.currentGoal = selection.goal;
  const focus = chooseFocusTarget(brain, domain, selection, committedAttackers);

  // Movement / Combat actions
  if (maybeEatAndRetreat(brain, domain)) {
    return;
  }

  if (brain.currentGoal === "CARRY_FLAG" && !domain.forcedBlocker) {
    clearFocus(brain);
    setMoveGoal(brain, selection.target, 1);
    return;
  }

  if (focus) {
    if (maybeMeleeFight(brain, focus)) {
      return;
    }
    if (maybeTakeShot(brain, focus, rangerBots, domain.now)) {
      setMoveGoal(brain, selection.target, 2);
      return;
    }
    if (focus.distance <= 8 || focus.isCarrier || focus.nearOwnBase) {
      setMoveGoal(brain, focus.position, 2);
      return;
    }
  }

  if (brain.currentGoal === "RETURN_OWN_FLAG" && domain.ownFlag.droppedAt && domain.position.distanceTo(domain.ownFlag.droppedAt) <= 2.2) {
    setMoveGoal(brain, domain.ownFlag.droppedAt, 1);
    return;
  }

  if (brain.currentGoal === "ATTACK_ENEMY_FLAG") {
    maybePickupEnemyFlag(brain, domain, layout);
  }

  if (selection.goal === "PATROL" && domain.position.distanceTo(selection.target) <= 3) {
    brain.patrolIndex += 1;
  }
  setMoveGoal(brain, selection.target, selection.goal === "CARRY_FLAG" ? 1 : 2);
}

function stopAllBrains(brains: BrainState[]): void {
  for (const brain of brains) {
    brain.bot.pathfinder.setGoal(null);
    stopCombatControls(brain.bot);
  }
}

async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));
  const baseRuntime = loadRuntimeConfig();
  const runtime = {
    ...baseRuntime,
    host: args.host,
    port: args.port
  };
  const logger = createLogger("headless");

  const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
  const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
  const registry = createRegistry(runtime, viewerHost, viewerBasePort);

  const totalBots = args.bots;
  const redCount = Math.max(2, Math.ceil(totalBots / 2));
  const blueCount = Math.max(2, totalBots - redCount);
  const rangerBots = new Set<string>();

  const admin = createBot("SimDirector", runtime);
  registry.registerBot(admin, admin.username, 0);
  selectKitOnOpen(admin, "ranger");
  rangerBots.add(admin.username);

  const redTeam: Bot[] = [admin];
  const blueTeam: Bot[] = [];
  const allBots: Bot[] = [admin];
  let viewerOffset = 1;

  for (let index = 1; index < redCount; index += 1) {
    const bot = createBot(makeName("SimRed", index), runtime);
    registry.registerBot(bot, bot.username, viewerOffset++);
    const kit = index <= 2 ? "ranger" : "scout";
    selectKitOnOpen(bot, kit);
    if (kit === "ranger") {
      rangerBots.add(bot.username);
    }
    redTeam.push(bot);
    allBots.push(bot);
  }

  for (let index = 1; index <= blueCount; index += 1) {
    const bot = createBot(makeName("SimBlue", index), runtime);
    registry.registerBot(bot, bot.username, viewerOffset++);
    const kit = index <= 2 ? "ranger" : "scout";
    selectKitOnOpen(bot, kit);
    if (kit === "ranger") {
      rangerBots.add(bot.username);
    }
    blueTeam.push(bot);
    allBots.push(bot);
  }
  const brains = createBrains(allBots, redTeam, rangerBots);
  const matchState = createInitialMatchState();
  const nextRoleReassignAt = { value: 0 };
  const roleEventMarker = { value: 0 };
  let brainInterval: NodeJS.Timeout | undefined;
  let cleanupStarted = false;

  process.on("exit", () => registry.clearRegistry());
  process.on("SIGINT", () => {
    if (brainInterval) {
      clearInterval(brainInterval);
    }
    stopAllBrains(brains);
    registry.clearRegistry();
    quitBots(allBots);
    process.exit(0);
  });

  function joinTeams(): void {
    redTeam.forEach((bot, index) => {
      schedule(bot, 1800 + index * 250, () => bot.chat("/ctf join red"));
    });
    blueTeam.forEach((bot, index) => {
      schedule(bot, 2200 + index * 250, () => bot.chat("/ctf join blue"));
    });
  }

  admin.once("spawn", () => {
    void (async () => {
      logger.info("target=%s:%d bots=%d seed=%s", runtime.host, runtime.port, totalBots, args.seedText);

      const preparedArena = await prepareArena(admin, args.seedText, logger);
      const layout = preparedArena.layout;
      attachMatchTracking(allBots, brains, matchState, logger.child("match"));
      maybeReassignRoles(brains, matchState, nextRoleReassignAt, roleEventMarker);

      joinTeams();
      schedule(admin, 6000, () => admin.chat("/ctf start"));

      brainInterval = setInterval(() => {
        maybeReassignRoles(brains, matchState, nextRoleReassignAt, roleEventMarker);
        const committedAttackers = buildCommittedAttackers(brains, Date.now());
        brains.forEach((brain) => {
          executeBrainTick(brain, brains, layout, matchState, rangerBots, committedAttackers);
        });

        if (matchState.ended && !cleanupStarted) {
          cleanupStarted = true;
          if (brainInterval) {
            clearInterval(brainInterval);
            brainInterval = undefined;
          }
          stopAllBrains(brains);
          schedule(admin, 2500, () => {
            void destroyTemporaryStructures(admin, preparedArena, logger.child("teardown"));
          });
          schedule(admin, 9500, () => {
            registry.clearRegistry();
            quitBots(allBots);
            process.exit(0);
          });
        }
      }, 300);

      schedule(admin, 150000, () => {
        if (!matchState.ended) {
          admin.chat("/ctf score");
        }
      });
      schedule(admin, 190000, () => {
        if (!matchState.ended) {
          admin.chat("/ctf stop");
        }
      });
    })().catch((err) => {
      console.error("[headless]", err?.message ?? err);
      if (brainInterval) {
        clearInterval(brainInterval);
      }
      stopAllBrains(brains);
      registry.clearRegistry();
      quitBots(allBots);
      process.exit(1);
    });
  });
}

main().catch((err) => {
  console.error("[headless]", err?.message ?? err);
  process.exit(1);
});
