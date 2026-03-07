import type { BotState, MatchPhase, SimConfig, Team } from "../types";
import { createLogger, type LogLevel } from "../util/log";
import { createSeededRandom } from "../util/random";
import { runWithCleanup, sleep } from "../util/safety";
import { createMemoryModel, clearThreatAllocations, releaseExpired, setPlayerTeam } from "../bots/memory";
import { spawnBots } from "../bots/spawnBots";
import { detectWorldEdit } from "../util/worldedit";
import { scoutSite } from "../arena/scoutSite";
import { buildArena50 } from "../arena/buildArena50";
import { setupPoints } from "../game/setupPoints";
import { assignInitialRoles, maybeSwapRoles } from "../bots/roles";
import { updateObjectives } from "../bots/objectives";
import { moveByObjective, updateLobbyMovement, clearMovement } from "../bots/movement";
import { runCombatTick } from "../bots/combat";
import { maybeSendComms } from "../bots/comms";
import { teardownArena50 } from "../arena/teardownArena50";
import { clampIntoBounds, containsXZ } from "../util/regionBounds";
import { KeyedRateLimiter } from "../util/rateLimit";
import type { ArenaPalette, ArenaBuildResult, BotSpawnResult } from "../types";
import { goals } from "mineflayer-pathfinder";

const { GoalNear } = goals;

type ArgMap = Record<string, string | boolean>;

function readArgs(argv: string[]): ArgMap {
  const result: ArgMap = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) continue;
    const [k, inlineValue] = token.slice(2).split("=", 2);
    if (inlineValue !== undefined) {
      result[k] = inlineValue;
      continue;
    }
    const maybeValue = argv[i + 1];
    if (!maybeValue || maybeValue.startsWith("--")) {
      result[k] = true;
      continue;
    }
    result[k] = maybeValue;
    i += 1;
  }
  return result;
}

function parseIntArg(map: ArgMap, key: string, fallback: number): number {
  const raw = map[key];
  if (raw === undefined || raw === true) return fallback;
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? Math.floor(parsed) : fallback;
}

function parseBoolArg(map: ArgMap, key: string, fallback = false): boolean {
  const raw = map[key];
  if (raw === undefined) return fallback;
  if (raw === true) return true;
  const text = String(raw).toLowerCase();
  return text === "1" || text === "true" || text === "yes" || text === "on";
}

function parseArenaSize(raw: string | boolean | undefined): 50 | 75 | 100 {
  if (String(raw) === "50") return 50;
  if (String(raw) === "100") return 100;
  return 75;
}

function parseCsv(raw: string | boolean | undefined): string[] | undefined {
  if (!raw || raw === true) return undefined;
  return String(raw)
    .split(",")
    .map((entry) => entry.trim().toLowerCase())
    .filter((entry) => entry.length > 0);
}

function parsePalette(args: ArgMap): Partial<ArenaPalette> {
  const palette: Partial<ArenaPalette> = {};
  const primary = parseCsv(args.palettePrimary);
  const secondary = parseCsv(args.paletteSecondary);
  const trim = parseCsv(args.paletteTrim);
  const accent = parseCsv(args.paletteAccent);
  const floor = parseCsv(args.paletteFloor);
  const glass = parseCsv(args.paletteGlass);
  const lights = parseCsv(args.paletteLights);
  const markerRaw = args.paletteMarker;

  if (primary) palette.primary = primary;
  if (secondary) palette.secondary = secondary;
  if (trim) palette.trim = trim;
  if (accent) palette.accent = accent;
  if (floor) palette.floor = floor;
  if (glass) palette.glass = glass;
  if (lights) palette.lights = lights;
  if (typeof markerRaw === "string" && markerRaw.trim()) palette.marker = markerRaw.trim().toLowerCase();
  return palette;
}

function loadConfig(argv: string[]): SimConfig {
  const args = readArgs(argv);
  if (args.help || args.h) {
    console.log("Hyper-real default CTF simulation");
    console.log("Usage: tsx tools/sim/src/modes/hyperRealDefault.ts --host <host> --port <port> --bots <n> --arenaSize <50|75|100> [--seed <seed>] [--viewer]");
    console.log("Defaults: host=127.0.0.1 port=25565 bots=12 arenaSize=75 regulation=180 overtime=120");
    process.exit(0);
  }

  const seedText = String(args.seed ?? process.env.CTF_SIM_SEED ?? Date.now());
  const viewerEnabled = parseBoolArg(args, "viewer", parseBoolArg(args, "view", false));
  return {
    host: String(args.host ?? process.env.CTF_SIM_HOST ?? "127.0.0.1"),
    port: parseIntArg(args, "port", Number(process.env.CTF_SIM_PORT ?? 25565)),
    version: typeof args.version === "string" ? args.version : process.env.CTF_VERSION,
    bots: Math.max(2, parseIntArg(args, "bots", Number(process.env.CTF_SIM_BOTS ?? 12))),
    arenaSize: parseArenaSize(args.arenaSize ?? process.env.CTF_SIM_ARENA_SIZE ?? "75"),
    seedText,
    viewer: viewerEnabled,
    viewerBasePort: parseIntArg(args, "viewerBasePort", Number(process.env.CTF_VIEW_BASE_PORT ?? 8600)),
    viewerCount: Math.max(0, parseIntArg(args, "viewerCount", Number(process.env.CTF_VIEW_COUNT ?? (viewerEnabled ? 1 : 0)))),
    regulationSeconds: Math.max(60, parseIntArg(args, "regulationSeconds", 180)),
    overtimeSeconds: Math.max(30, parseIntArg(args, "overtimeSeconds", 120)),
    commandSpacingMs: Math.max(25, parseIntArg(args, "commandSpacingMs", 120)),
    debugLog: parseBoolArg(args, "debug", false)
  };
}

function isAdminCommand(command: string): boolean {
  const normalized = command.trim().toLowerCase();
  if (normalized.startsWith("//")) return true;
  return /^\/ctf\s+(set|start|stop|score|debug|canbuild)/.test(normalized);
}

function phaseAllowsAdminCommands(phase: MatchPhase): boolean {
  return phase !== "match" && phase !== "overtime";
}

function teamOf(state: BotState): Team {
  return state.team;
}

function applyTieBiasAndMorale(
  bots: BotState[],
  phase: MatchPhase,
  elapsedMs: number,
  regulationMs: number,
  redScore: number,
  blueScore: number
): void {
  const diff = redScore - blueScore;
  const nearEnd = elapsedMs >= regulationMs - 45000;
  for (const state of bots) {
    if (!state.online || !state.hasJoinedTeam) continue;
    if (nearEnd || phase === "overtime") {
      state.morale = Math.min(1.3, state.morale + 0.02);
    }
    if (diff === 0) {
      // Tie-shaping bias (not forced): alternate lane pressure windows.
      const swingWindow = Math.floor(elapsedMs / 30000) % 2 === 0 ? "red" : "blue";
      if (state.team === swingWindow) {
        state.morale = Math.min(1.25, state.morale + 0.018);
      } else {
        state.morale = Math.max(-1.0, state.morale - 0.006);
      }
      continue;
    }
    const trailing = diff > 0 ? "blue" : "red";
    if (state.team === trailing) {
      state.morale = Math.min(1.35, state.morale + 0.028);
      state.tilt = Math.max(-0.4, state.tilt - 0.01);
      if (nearEnd && state.role === "defender") {
        state.role = "escort";
      }
    } else {
      state.morale = Math.max(-1.2, state.morale - 0.02);
      state.tilt = Math.min(1.4, state.tilt + 0.01);
      if (nearEnd && state.role === "runner") {
        state.role = "ranged";
      }
    }
  }
}

function applyAttentionMoments(states: BotState[], now: number, nextFloat: () => number): void {
  for (const state of states) {
    if (!state.online || !state.bot.entity) continue;
    if (now < state.nextAttentionLookAt) continue;
    const nearestEnemy = Object.entries(state.bot.players)
      .map(([name, info]) => ({ name, entity: info?.entity }))
      .filter((entry) => Boolean(entry.entity) && entry.name !== state.username)
      .sort((a, b) => {
        const aDist = state.bot.entity!.position.distanceTo(a.entity!.position);
        const bDist = state.bot.entity!.position.distanceTo(b.entity!.position);
        return aDist - bDist;
      })[0];

    const target = nearestEnemy?.entity?.position ?? state.objective?.target;
    if (target) {
      state.bot.lookAt(target.offset(0, 1.3, 0), true).catch(() => undefined);
    }
    state.nextAttentionLookAt = now + 1200 + Math.floor(nextFloat() * 3000);
  }
}

function pullBotsIntoArena(states: BotState[], build: ArenaBuildResult): void {
  for (const state of states) {
    if (!state.online || !state.bot.entity || !state.hasJoinedTeam) continue;
    const pos = state.bot.entity.position.floored();
    if (containsXZ(build.layout.bounds, pos, 0)) continue;
    const safe = clampIntoBounds(build.layout.bounds, pos, 2);
    state.bot.pathfinder.setGoal(new GoalNear(safe.x, safe.y, safe.z, 2), false);
  }
}

async function run(): Promise<void> {
  const config = loadConfig(process.argv.slice(2));
  const logLevel: LogLevel = config.debugLog ? "debug" : "info";
  const logger = createLogger("hyper-real-default", logLevel);
  const args = readArgs(process.argv.slice(2));
  const paletteOverride = parsePalette(args);
  const rng = createSeededRandom(config.seedText);
  const memory = createMemoryModel();
  const decisionLimiter = new KeyedRateLimiter(1, 2);

  logger.info(
    "config host=%s port=%d bots=%d arenaSize=%d seed=%s viewer=%s",
    config.host,
    config.port,
    config.bots,
    config.arenaSize,
    rng.seedText,
    config.viewer ? "on" : "off"
  );

  let phase: MatchPhase = "bootstrap";
  let spawnResult: BotSpawnResult | undefined;
  let arenaBuild: ArenaBuildResult | undefined;

  const cleanup = async (reason: string): Promise<void> => {
    phase = "cleanup";
    logger.info("cleanup reason=%s", reason);
    if (spawnResult) {
      if (spawnResult.admin.online) {
        try {
          spawnResult.admin.bot.chat("/ctf stop");
          await sleep(config.commandSpacingMs);
        } catch {
          // ignore
        }
      }
      if (arenaBuild) {
        try {
          await teardownArena50({
            admin: spawnResult.admin,
            build: arenaBuild,
            logger: logger.child("teardown"),
            commandDelayMs: config.commandSpacingMs
          });
        } catch (err: any) {
          logger.warn("teardown failed: %s", err?.message ?? err);
        }
      }
      await spawnResult.disconnectAll();
    }
    phase = "done";
  };

  await runWithCleanup(
    logger,
    async () => {
      spawnResult = await spawnBots(config, rng, logger.child("spawn"), memory);
      for (const state of spawnResult.bots) {
        setPlayerTeam(memory, state.username, teamOf(state));
      }
      setPlayerTeam(memory, spawnResult.admin.username, "red");

      const runAdminCommand = async (command: string): Promise<void> => {
        if (isAdminCommand(command) && !phaseAllowsAdminCommands(phase)) {
          throw new Error(`admin command blocked during ${phase}: ${command}`);
        }
        spawnResult!.admin.bot.chat(command);
        await sleep(config.commandSpacingMs);
      };

      phase = "lobby";
      const worldEdit = await detectWorldEdit(spawnResult.admin.bot, logger.child("worldedit"));

      let requestedSize: 50 | 75 | 100 = config.arenaSize;
      if (requestedSize === 100 && !worldEdit.available) {
        logger.warn("arenaSize 100 requested without WorldEdit; using 75");
        requestedSize = 75;
      }

      const site = await scoutSite(spawnResult.admin, requestedSize, rng, logger.child("scout"));
      phase = "building";
      arenaBuild = await buildArena50({
        admin: spawnResult.admin,
        site,
        arenaSize: requestedSize,
        rng,
        logger: logger.child("build"),
        worldEdit,
        commandDelayMs: config.commandSpacingMs,
        paletteOverride
      });

      phase = "setup";
      await setupPoints(spawnResult.admin, arenaBuild.layout, logger.child("setup"), runAdminCommand);
      await runAdminCommand(`/ctf setgametime ${config.regulationSeconds}`);
      await runAdminCommand("/ctf setscorelimit 5");

      await spawnResult.joinInitialTeams();
      assignInitialRoles(spawnResult.bots, "red", rng, Date.now());
      assignInitialRoles(spawnResult.bots, "blue", rng, Date.now());

      // Lobby realism before match start.
      const preStartUntil = Date.now() + 8000;
      while (Date.now() < preStartUntil) {
        for (const state of spawnResult.bots) {
          updateLobbyMovement(state, rng, Date.now());
        }
        await sleep(260);
      }

      await runAdminCommand("/ctf start");
      phase = "match";

      const startedAt = Date.now();
      const regulationEndsAt = startedAt + config.regulationSeconds * 1000;
      const overtimeEndsAt = regulationEndsAt + config.overtimeSeconds * 1000;
      let lastRoleSwapTick = 0;

      while (Date.now() < overtimeEndsAt) {
        const now = Date.now();
        const elapsed = now - startedAt;
        if (now >= regulationEndsAt && phase === "match") {
          phase = "overtime";
          logger.info("entering overtime");
        }

        spawnResult.pulsePresence(phase, elapsed);
        clearThreatAllocations(memory);
        releaseExpired(memory, now);

        if (now - lastRoleSwapTick >= 3200) {
          maybeSwapRoles(spawnResult.bots, memory, phase, rng, now);
          lastRoleSwapTick = now;
        }

        applyTieBiasAndMorale(
          spawnResult.bots,
          phase,
          elapsed,
          config.regulationSeconds * 1000,
          memory.score.red,
          memory.score.blue
        );

        updateObjectives(
          spawnResult.bots,
          arenaBuild.layout,
          memory,
          phase,
          rng,
          now,
          regulationEndsAt
        );

        for (const state of spawnResult.bots) {
          if (!state.online) continue;
          if (!state.hasJoinedTeam && phase !== "lobby") continue;
          if (decisionLimiter.allow(`move:${state.username}`, now)) {
            moveByObjective(
              state,
              spawnResult.bots.filter((entry) => entry.team === state.team && entry.online),
              arenaBuild.layout,
              phase,
              rng,
              now
            );
          }
          maybeSendComms(state, memory, phase, rng, now);
        }

        applyAttentionMoments(spawnResult.bots, now, () => rng.float());
        runCombatTick(spawnResult.bots, arenaBuild.layout, memory, phase, rng, now);
        pullBotsIntoArena(spawnResult.bots, arenaBuild);

        await sleep(280);
      }

      phase = "cleanup";
      const cleanupUntil = Date.now() + 3500;
      while (Date.now() < cleanupUntil) {
        for (const state of spawnResult.bots) {
          clearMovement(state);
          updateLobbyMovement(state, rng, Date.now());
        }
        await sleep(260);
      }

      await runAdminCommand("/ctf stop");
      logger.info(
        "simulation complete scoreModel red=%d blue=%d events=%d",
        memory.score.red,
        memory.score.blue,
        memory.events.length
      );
    },
    cleanup
  );
}

run().catch((err: any) => {
  console.error("[hyper-real-default] fatal:", err?.message ?? err);
  process.exit(1);
});
