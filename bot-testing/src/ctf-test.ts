import mineflayer from "mineflayer";
import { Vec3 } from "vec3";
import type { Bot } from "mineflayer";
import { updateRegistry } from "./bot-registry";
import { startBotViewer } from "./bot-viewer";
import { addressOf, loadRuntimeConfig } from "./runtime-config";

const runtime = loadRuntimeConfig();
const host = runtime.host;
const port = runtime.port;
const version = runtime.version;

const redBase = runtime.redBase;
const blueBase = runtime.blueBase;

function createBot(username: string): Bot {
  const bot = mineflayer.createBot({ host, port, username, auth: "offline", version });
  bot.on("messagestr", (msg) => console.log(`[${username}] ${msg}`));
  bot.on("error", (err) => console.log(username, "error", err.message));
  bot.on("kicked", (reason) => console.log(username, "kicked", reason));
  return bot;
}

const redLeader = createBot("RedLeader");
const redTwo = createBot("RedTwo");
const redThree = createBot("RedThree");
const blueLeader = createBot("BlueLeader");
const blueTwo = createBot("BlueTwo");
const blueThree = createBot("BlueThree");

const activeBots = new Set<string>();
const viewUrlsByBot = new Map<string, string>();
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;

function syncRegistry(): void {
  updateRegistry(
    [...activeBots],
    { address: addressOf(runtime), version: runtime.version },
    Object.fromEntries(viewUrlsByBot)
  );
}

function registerBot(bot: Bot, fallbackName: string, viewerOffset: number): void {
  const name = bot.username || fallbackName;
  activeBots.add(name);
  syncRegistry();
  bot.once("spawn", () => {
    const viewerPort = viewerBasePort + viewerOffset;
    try {
      const started = startBotViewer(bot, viewerPort);
      if (!started) {
        console.log("[viewer] %s unavailable", name);
        return;
      }
      const url = `http://${viewerHost}:${viewerPort}/`;
      viewUrlsByBot.set(name, url);
      syncRegistry();
      console.log("[viewer] %s -> %s", name, url);
    } catch (err: any) {
      console.log("[viewer] %s failed: %s", name, err?.message ?? err);
    }
  });
  bot.on("end", () => {
    activeBots.delete(name);
    viewUrlsByBot.delete(name);
    syncRegistry();
  });
  bot.on("kicked", () => {
    activeBots.delete(name);
    viewUrlsByBot.delete(name);
    syncRegistry();
  });
}

registerBot(redLeader, "RedLeader", 0);
registerBot(redTwo, "RedTwo", 1);
registerBot(redThree, "RedThree", 2);
registerBot(blueLeader, "BlueLeader", 3);
registerBot(blueTwo, "BlueTwo", 4);
registerBot(blueThree, "BlueThree", 5);

process.on("exit", () => updateRegistry([], { address: addressOf(runtime), version: runtime.version }));
process.on("SIGINT", () => {
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
});

function isReady(bot: Bot): boolean {
  const client = (bot as any)._client;
  return Boolean(client && client.state === "play");
}

function schedule(bot: Bot, delayMs: number, action: () => void): void {
  setTimeout(() => {
    if (!isReady(bot)) {
      console.log(bot.username, "not ready for action at", delayMs);
      return;
    }
    try {
      action();
    } catch (err: any) {
      console.log(bot.username, "action error", err?.message ?? err);
    }
  }, delayMs);
}

function digBlock(bot: Bot, x: number, y: number, z: number): void {
  const block = bot.blockAt(new Vec3(x, y, z));
  if (!block) {
    console.log(bot.username, "block missing at", x, y, z);
    return;
  }
  bot.dig(block, true).catch((err: any) => console.log(bot.username, "dig error", err?.message ?? err));
}

function nudge(bot: Bot, durationMs: number): void {
  bot.setControlState("forward", true);
  setTimeout(() => bot.setControlState("forward", false), durationMs);
}

function attemptCarrierEdgeCases(bot: Bot): void {
  try { bot.setQuickBarSlot(1); } catch (err: any) { console.log(bot.username, "setQuickBarSlot error", err?.message ?? err); }
  try { (bot as any).swapHands?.(); } catch (err: any) { console.log(bot.username, "swapHands error", err?.message ?? err); }
  try {
    const slot = bot.inventory.slots[36];
    if (slot) {
      bot.tossStack(slot).catch((err: any) => console.log(bot.username, "toss error", err?.message ?? err));
    }
  } catch (err: any) {
    console.log(bot.username, "toss error", err?.message ?? err);
  }
}

function attackTarget(attacker: Bot, targetName: string, durationMs: number): void {
  const interval = setInterval(() => {
    const target = attacker.players[targetName]?.entity as any;
    if (!target) {
      console.log(attacker.username, "target missing", targetName);
      return;
    }
    try {
      attacker.lookAt(target.position.offset(0, 1.6, 0), true);
      attacker.attack(target);
    } catch (err: any) {
      console.log(attacker.username, "attack error", err?.message ?? err);
    }
  }, 500);

  setTimeout(() => clearInterval(interval), durationMs);
}

function joinTeam(bot: Bot, team: string, delay: number): void {
  bot.once("spawn", () => {
    schedule(bot, delay, () => bot.chat(`/ctf join ${team}`));
  });
}

joinTeam(redTwo, "red", 3000);
joinTeam(redThree, "red", 3500);
joinTeam(blueLeader, "blue", 3000);
joinTeam(blueTwo, "blue", 3500);
joinTeam(blueThree, "blue", 4000);

redLeader.once("spawn", () => {
  schedule(redLeader, 2000, () => redLeader.chat("/ctf join red"));

  schedule(redLeader, 5000, () => redLeader.chat(`/tp ${redLeader.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(redLeader, 6500, () => redLeader.chat("/ctf setflag red"));

  schedule(redLeader, 8000, () => redLeader.chat(`/tp ${redLeader.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(redLeader, 9500, () => redLeader.chat("/ctf setflag blue"));

  schedule(redLeader, 12000, () => redLeader.chat("/ctf start"));

  schedule(redLeader, 16000, () => redLeader.chat(`/tp ${redLeader.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(redLeader, 18000, () => digBlock(redLeader, blueBase.x, blueBase.y, blueBase.z));
  schedule(redLeader, 20000, () => attemptCarrierEdgeCases(redLeader));
  schedule(redLeader, 23000, () => redLeader.chat(`/tp ${redLeader.username} ${redBase.x} ${redBase.y + 1} ${redBase.z}`));
  schedule(redLeader, 24500, () => nudge(redLeader, 700));

  schedule(blueLeader, 27000, () => blueLeader.chat(`/tp ${blueLeader.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(blueLeader, 29000, () => digBlock(blueLeader, redBase.x, redBase.y, redBase.z));
  schedule(redLeader, 30000, () => redLeader.chat(`/tp ${redLeader.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(redTwo, 30500, () => redTwo.chat(`/tp ${redTwo.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(redLeader, 31000, () => attackTarget(redLeader, blueLeader.username, 15000));
  schedule(redTwo, 31200, () => attackTarget(redTwo, blueLeader.username, 15000));
  schedule(redThree, 31400, () => attackTarget(redThree, blueLeader.username, 15000));

  schedule(redTwo, 38000, () => redTwo.chat(`/tp ${redTwo.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(redTwo, 39000, () => digBlock(redTwo, redBase.x, redBase.y, redBase.z));

  schedule(blueTwo, 47000, () => blueTwo.chat(`/tp ${blueTwo.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(redTwo, 47500, () => redTwo.chat(`/tp ${redTwo.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(blueTwo, 48500, () => attackTarget(blueTwo, redTwo.username, 6000));

  schedule(redLeader, 52000, () => redLeader.chat(`/tp ${redLeader.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(redLeader, 54000, () => digBlock(redLeader, blueBase.x, blueBase.y, blueBase.z));
  schedule(redLeader, 57000, () => redLeader.chat(`/tp ${redLeader.username} ${redBase.x} ${redBase.y + 1} ${redBase.z}`));
  schedule(redLeader, 58500, () => nudge(redLeader, 700));

  schedule(redLeader, 65000, () => redLeader.chat(`/tp ${redLeader.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(redLeader, 67000, () => digBlock(redLeader, blueBase.x, blueBase.y, blueBase.z));
  schedule(redLeader, 70000, () => redLeader.chat(`/tp ${redLeader.username} ${redBase.x} ${redBase.y + 1} ${redBase.z}`));
  schedule(redLeader, 71500, () => nudge(redLeader, 700));

  schedule(redLeader, 78000, () => redLeader.chat("/ctf score"));
  schedule(redLeader, 80000, () => redLeader.chat("/ctf stop"));
  schedule(redLeader, 82000, () => redLeader.chat("/ctf leave"));
});

setTimeout(() => {
  console.log("Full match test script complete");
  for (const bot of [redLeader, redTwo, redThree, blueLeader, blueTwo, blueThree]) {
    if (bot.quit) bot.quit();
  }
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
}, 90000);
