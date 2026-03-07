import mineflayer from "mineflayer";
import type { Bot } from "mineflayer";
import { Movements, pathfinder } from "mineflayer-pathfinder";
import { Vec3 } from "vec3";
import type { BotSpawnResult, BotState, MatchPhase, MemoryModel, SimConfig, Team } from "../types";
import type { SeededRandom } from "../util/random";
import type { Logger } from "../util/log";
import { assignSilenceProfile } from "./comms";
import { observeServerMessage, reportDeath } from "./memory";
import { sleep } from "../util/safety";

type ViewerFactory = (bot: Bot, options: { port: number; firstPerson: boolean; viewDistance?: number }) => void;

function loadViewerFactory(): ViewerFactory | null {
  try {
    const moduleRef = require("prismarine-viewer");
    return typeof moduleRef?.mineflayer === "function" ? moduleRef.mineflayer : null;
  } catch {
    return null;
  }
}

function configureMovements(bot: Bot): void {
  try {
    const mcData = require("minecraft-data")(bot.version);
    const movements = new Movements(bot, mcData);
    movements.allowParkour = true;
    movements.allowSprinting = true;
    movements.canOpenDoors = true;
    movements.canDig = false;
    bot.pathfinder.setMovements(movements);
  } catch {
    // keep defaults
  }
}

function makeState(
  bot: Bot,
  id: number,
  username: string,
  team: Team,
  rng: SeededRandom
): BotState {
  const anchor = bot.entity?.position?.floored?.() ?? new Vec3(0, 64, 0);
  const state: BotState = {
    bot,
    username,
    id,
    team,
    role: "flex",
    isSilent: false,
    isLateJoiner: false,
    hasJoinedTeam: false,
    online: true,
    reactionMs: rng.int(120, 420),
    aimJitter: Math.max(0.02, Math.abs(rng.gaussian(0.14, 0.08))),
    infoLagMs: rng.int(180, 900),
    tunnelVisionUntil: 0,
    lastSeenEnemyAt: 0,
    lastDecisionAt: 0,
    nextRoleSwapAt: 0,
    nextCommsAt: rng.int(6000, 14000),
    nextThinkingPauseAt: rng.int(1200, 4200),
    nextAttentionLookAt: rng.int(1800, 6200),
    mistakesUntil: 0,
    lowHealthRetreatUntil: 0,
    pushAbortAt: 0,
    temporaryGrudgeUntil: 0,
    objectiveLoopCount: 0,
    misses: 0,
    hits: 0,
    deaths: 0,
    kills: 0,
    tilt: rng.jitter(0, 0.15),
    morale: rng.jitter(0, 0.18),
    spacingBias: rng.jitter(0, 0.35),
    defenderAnchor: anchor
  };
  assignSilenceProfile(state, rng);
  return state;
}

function createBot(username: string, config: SimConfig): Bot {
  const timeoutMs = Number(process.env.CTF_BOT_TIMEOUT_MS || 60000);
  const checkTimeoutInterval = Number.isFinite(timeoutMs) && timeoutMs > 0 ? Math.floor(timeoutMs) : 60000;
  const bot = mineflayer.createBot({
    host: config.host,
    port: config.port,
    username,
    auth: "offline",
    version: config.version,
    checkTimeoutInterval
  });
  bot.loadPlugin(pathfinder);
  bot.once("spawn", () => configureMovements(bot));
  return bot;
}

function startViewerIfEnabled(bot: Bot, config: SimConfig, logger: Logger, offset: number): void {
  if (!config.viewer || offset >= config.viewerCount) return;
  const factory = loadViewerFactory();
  if (!factory) return;
  try {
    const port = config.viewerBasePort + offset;
    factory(bot, { port, firstPerson: true, viewDistance: 6 });
    logger.info("viewer %s on %d", bot.username, port);
  } catch (err: any) {
    logger.warn("viewer failed for %s: %s", bot.username, err?.message ?? err);
  }
}

function setupMessageHooks(bot: Bot, memory: MemoryModel): void {
  bot.on("messagestr", (msg) => observeServerMessage(memory, msg, Date.now()));
}

export async function spawnBots(
  config: SimConfig,
  rng: SeededRandom,
  logger: Logger,
  memory: MemoryModel
): Promise<BotSpawnResult> {
  const adminBot = createBot("SimDirector", config);
  const admin = makeState(adminBot, 0, "SimDirector", "red", rng);
  admin.isSilent = true;
  admin.role = "defender";
  setupMessageHooks(adminBot, memory);
  adminBot.on("error", (err) => logger.warn("%s error %s", admin.username, err.message));
  adminBot.on("kicked", (reason) => logger.warn("%s kicked %s", admin.username, String(reason)));

  const bots: BotState[] = [];
  const rosterCount = Math.max(2, config.bots);
  const redCount = Math.ceil(rosterCount / 2);
  const blueCount = rosterCount - redCount;
  let viewerIndex = 0;

  const attachCoreHooks = (state: BotState): void => {
    state.bot.on("spawn", () => {
      state.online = true;
      if (!Number.isFinite(state.defenderAnchor.x)) {
        state.defenderAnchor = state.bot.entity?.position.floored() ?? state.defenderAnchor;
      }
    });
    state.bot.on("end", () => {
      state.online = false;
      state.hasJoinedTeam = false;
    });
    state.bot.on("death", () => reportDeath(memory, state, Date.now()));
    state.bot.on("error", (err) => logger.warn("%s error %s", state.username, err.message));
    state.bot.on("kicked", (reason) => logger.warn("%s kicked %s", state.username, String(reason)));
    setupMessageHooks(state.bot, memory);
  };

  for (let i = 0; i < redCount; i += 1) {
    const username = `SimRed${String(i + 1).padStart(2, "0")}`;
    const bot = createBot(username, config);
    const state = makeState(bot, bots.length + 1, username, "red", rng);
    if (i >= redCount - 2) state.isLateJoiner = true;
    attachCoreHooks(state);
    startViewerIfEnabled(bot, config, logger, viewerIndex++);
    bots.push(state);
  }

  for (let i = 0; i < blueCount; i += 1) {
    const username = `SimBlue${String(i + 1).padStart(2, "0")}`;
    const bot = createBot(username, config);
    const state = makeState(bot, bots.length + 1, username, "blue", rng);
    if (i >= blueCount - 2) state.isLateJoiner = true;
    attachCoreHooks(state);
    startViewerIfEnabled(bot, config, logger, viewerIndex++);
    bots.push(state);
  }

  startViewerIfEnabled(adminBot, config, logger, viewerIndex++);

  const waitForSpawns = async (): Promise<void> => {
    await Promise.all(
      [admin, ...bots].map((state) => new Promise<void>((resolve) => {
        if (state.bot.entity) {
          resolve();
          return;
        }
        const timer = setTimeout(() => resolve(), 15000);
        state.bot.once("spawn", () => {
          clearTimeout(timer);
          resolve();
        });
      }))
    );
  };
  await waitForSpawns();

  const joinInitialTeams = async (): Promise<void> => {
    for (const state of bots.filter((entry) => !entry.isLateJoiner)) {
      state.bot.chat(`/ctf join ${state.team}`);
      state.hasJoinedTeam = true;
      await sleep(rng.int(120, 340));
    }
  };

  const guaranteedPresent = bots.filter((entry) => !entry.isLateJoiner);
  if (guaranteedPresent.length === 0 && bots.length > 0) {
    bots[0].isLateJoiner = false;
    guaranteedPresent.push(bots[0]);
  }
  const lateLeaveTarget = rng.pick(guaranteedPresent);
  let lateLeaveDone = false;
  let lateJoinIssued = false;
  let lateRejoinIssued = false;

  const pulsePresence = (phase: MatchPhase, now: number): void => {
    if (phase === "match" && !lateJoinIssued && now % 30000 < 250) {
      for (const state of bots.filter((entry) => entry.isLateJoiner && !entry.hasJoinedTeam && entry.online)) {
        state.bot.chat(`/ctf join ${state.team}`);
        state.hasJoinedTeam = true;
      }
      lateJoinIssued = true;
    }

    if (phase === "match" && !lateLeaveDone && now % 45000 < 250) {
      if (lateLeaveTarget.online && lateLeaveTarget.hasJoinedTeam) {
        lateLeaveTarget.bot.chat("/ctf leave");
        lateLeaveTarget.hasJoinedTeam = false;
        lateLeaveDone = true;
      }
    }

    if (phase === "overtime" && lateLeaveDone && !lateRejoinIssued && lateLeaveTarget.online) {
      lateLeaveTarget.bot.chat(`/ctf join ${lateLeaveTarget.team}`);
      lateLeaveTarget.hasJoinedTeam = true;
      lateRejoinIssued = true;
    }
  };

  const disconnectAll = async (): Promise<void> => {
    const shutdown = async (state: BotState): Promise<void> => {
      try {
        state.bot.quit();
      } catch {
        try {
          state.bot.end();
        } catch {
          // ignore
        }
      }
    };
    await Promise.all([shutdown(admin), ...bots.map((state) => shutdown(state))]);
  };

  return { bots, admin, joinInitialTeams, pulsePresence, disconnectAll };
}
