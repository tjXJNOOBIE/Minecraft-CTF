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

const admin = createBot("RedLeader");
const guestOne = createBot("EdgeGuest");
const guestTwo = createBot("EdgeGuestTwo");

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

registerBot(admin, "RedLeader", 0);
registerBot(guestOne, "EdgeGuest", 1);
registerBot(guestTwo, "EdgeGuestTwo", 2);

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

admin.once("spawn", () => {
  schedule(admin, 1500, () => admin.chat("/ctf score"));
  schedule(admin, 2500, () => admin.chat(`/tp ${admin.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(admin, 3500, () => admin.chat("/ctf setflag red"));
  schedule(admin, 5000, () => admin.chat(`/tp ${admin.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(admin, 6500, () => admin.chat("/ctf setflag blue"));
  schedule(admin, 9000, () => admin.chat("/ctf start"));
  schedule(admin, 22000, () => admin.chat("/ctf stop"));
});

guestOne.once("spawn", () => {
  schedule(guestOne, 2000, () => guestOne.chat("/ctf score"));
  schedule(guestOne, 12000, () => guestOne.chat("/ctf join"));
  schedule(guestOne, 16000, () => digBlock(guestOne, redBase.x, redBase.y, redBase.z));
  schedule(guestOne, 26000, () => guestOne.chat("/ctf score"));
});

guestTwo.once("spawn", () => {
  schedule(guestTwo, 13000, () => guestTwo.chat("/ctf join"));
});

setTimeout(() => {
  console.log("Edge case script complete");
  for (const bot of [admin, guestOne, guestTwo]) {
    if (bot.quit) bot.quit();
  }
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
}, 35000);
