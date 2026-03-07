import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA } from "./scenario-positions";
import { createBot, createRegistry, engageTarget, isReady, pathTo, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const redNames = ["RedLeader", ...Array.from({ length: 11 }, (_, i) => `Red${String(i + 1).padStart(2, "0")}`)];
const blueNames = ["BlueLeader", ...Array.from({ length: 11 }, (_, i) => `Blue${String(i + 1).padStart(2, "0")}`)];

const bots = new Map<string, any>();
const teamByBot = new Map<string, "red" | "blue">();
const totalBots = redNames.length + blueNames.length;
const viewerCount = Math.max(0, Number(process.env.CTF_VIEW_COUNT || totalBots));

function registerBot(name: string, team: "red" | "blue", viewerOffset?: number): void {
  const bot = createBot(name, runtime);
  bots.set(name, bot);
  teamByBot.set(name, team);
  registry.registerBot(bot, name, viewerOffset);
  selectKitOnOpen(bot, name.endsWith("Leader") ? "ranger" : "scout");
}

let viewerIndex = 0;
for (const name of redNames) {
  registerBot(name, "red", viewerIndex < viewerCount ? viewerIndex++ : -1);
}
for (const name of blueNames) {
  registerBot(name, "blue", viewerIndex < viewerCount ? viewerIndex++ : -1);
}

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function randomPoint(): Vec3 {
  const min = ARENA.center.x - (ARENA.halfSize - 2);
  const max = ARENA.center.x + (ARENA.halfSize - 2);
  const minZ = ARENA.center.z - (ARENA.halfSize - 2);
  const maxZ = ARENA.center.z + (ARENA.halfSize - 2);
  const x = Math.floor(min + Math.random() * (max - min + 1));
  const z = Math.floor(minZ + Math.random() * (maxZ - minZ + 1));
  return new Vec3(x, ARENA.center.y, z);
}

function findNearestEnemy(bot: any, enemyNames: string[]): string | null {
  let nearest: { name: string; dist: number } | null = null;
  for (const enemyName of enemyNames) {
    const entity = bot.players[enemyName]?.entity;
    if (!entity) {
      continue;
    }
    const dist = bot.entity.position.distanceTo(entity.position);
    if (!nearest || dist < nearest.dist) {
      nearest = { name: enemyName, dist };
    }
  }
  return nearest ? nearest.name : null;
}

function startBehavior(botName: string, enemyNames: string[]): void {
  const bot = bots.get(botName);
  if (!bot) return;
  let busy = false;

  setInterval(async () => {
    if (!isReady(bot) || busy || !bot.entity) {
      return;
    }
    const target = findNearestEnemy(bot, enemyNames);
    if (target) {
      busy = true;
      engageTarget(bot, target, 7000);
      setTimeout(() => {
        busy = false;
      }, 7200);
      return;
    }

    busy = true;
    if (!bot.entity) {
      busy = false;
      return;
    }
    bot.setControlState("sprint", true);
    await pathTo(bot, randomPoint(), 2.5, 15000);
    bot.setControlState("sprint", false);
    busy = false;
  }, 8000);
}

function joinTeams(): void {
  for (const name of redNames) {
    const bot = bots.get(name);
    if (!bot) continue;
    schedule(bot, 2000 + Math.random() * 1000, () => bot.chat("/ctf join red"));
  }
  for (const name of blueNames) {
    const bot = bots.get(name);
    if (!bot) continue;
    schedule(bot, 2000 + Math.random() * 1000, () => bot.chat("/ctf join blue"));
  }
}

const admin = bots.get("RedLeader");
if (admin) {
  admin.once("spawn", () => {
    joinTeams();
    schedule(admin, 8000, () => admin.chat("/ctf start"));

    schedule(admin, 25000, () => {
      for (const name of redNames) {
        startBehavior(name, blueNames);
      }
      for (const name of blueNames) {
        startBehavior(name, redNames);
      }
    });

    // Run full 10 minute match.
    schedule(admin, 620000, () => {
      console.log("[long24] match complete");
      for (const bot of bots.values()) {
        if (bot.quit) bot.quit();
      }
      registry.clearRegistry();
      process.exit(0);
    });
  });
}
