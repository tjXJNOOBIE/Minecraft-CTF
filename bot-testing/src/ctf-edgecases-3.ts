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
const redCarrier = createBot("RedCarrier");
const blueGuard = createBot("BlueGuard");

const activeBots = new Set<string>();
const viewUrlsByBot = new Map<string, string>();
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
let ownFlagBlockedMessages = 0;

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
registerBot(redCarrier, "RedCarrier", 1);
registerBot(blueGuard, "BlueGuard", 2);

redCarrier.on("messagestr", (msg) => {
  if (msg.toLowerCase().includes("flag is already home")) {
    ownFlagBlockedMessages++;
  }
});

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
  bot.dig(block, true).catch((err: any) => {
    const message = String(err?.message ?? err ?? "");
    if (message.includes("Digging aborted")) {
      console.log(`${bot.username} dig blocked as expected at ${x}, ${y}, ${z}`);
      return;
    }
    console.log(bot.username, "dig error", message);
  });
}

function inspectDroppedBlueFlagNearRedBase(bot: Bot): void {
  const baseBlock = bot.blockAt(new Vec3(redBase.x, redBase.y, redBase.z));
  const baseName = baseBlock?.name ?? "missing";
  let foundOffsetBlueWool = false;
  let foundAt = "";

  for (let dx = -6; dx <= 6 && !foundOffsetBlueWool; dx++) {
    for (let dz = -6; dz <= 6 && !foundOffsetBlueWool; dz++) {
      const block = bot.blockAt(new Vec3(redBase.x + dx, redBase.y, redBase.z + dz));
      if (!block || block.name !== "blue_wool") {
        continue;
      }
      foundOffsetBlueWool = true;
      foundAt = `${redBase.x + dx},${redBase.y},${redBase.z + dz}`;
    }
  }

  console.log("[edge3] red-base block=", baseName);
  console.log("[edge3] dropped-blue-wool-near-red-base=", foundOffsetBlueWool, foundAt);
}

admin.once("spawn", () => {
  schedule(admin, 1500, () => admin.chat("/ctf stop"));
  schedule(admin, 2600, () => admin.chat("/ctf join red"));
  schedule(admin, 4500, () => admin.chat(`/tp ${admin.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(admin, 5600, () => admin.chat("/ctf setflag red"));
  schedule(admin, 7000, () => admin.chat(`/tp ${admin.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(admin, 8100, () => admin.chat("/ctf setflag blue"));
  schedule(admin, 10000, () => admin.chat("/ctf start"));

  // Carrier picks enemy flag, then is killed inside own scoring zone.
  schedule(admin, 17000, () => admin.chat(`/tp ${redCarrier.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(admin, 22500, () => admin.chat(`/tp ${redCarrier.username} ${redBase.x} ${redBase.y + 1} ${redBase.z}`));
  schedule(admin, 23500, () => admin.chat(`/kill ${redCarrier.username}`));
  schedule(admin, 25500, () => admin.chat(`/tp ${admin.username} ${redBase.x} ${redBase.y + 1} ${redBase.z}`));
  schedule(admin, 26200, () => inspectDroppedBlueFlagNearRedBase(admin));

  // Hit own flag twice quickly to validate blocked message cooldown behavior.
  schedule(admin, 29000, () => admin.chat(`/tp ${redCarrier.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(admin, 30000, () => digBlock(redCarrier, redBase.x, redBase.y, redBase.z));
  schedule(admin, 30400, () => digBlock(redCarrier, redBase.x, redBase.y, redBase.z));

  schedule(admin, 38000, () => admin.chat("/ctf stop"));
});

redCarrier.once("spawn", () => {
  schedule(redCarrier, 3500, () => redCarrier.chat("/ctf join red"));
  schedule(redCarrier, 18200, () => digBlock(redCarrier, blueBase.x, blueBase.y, blueBase.z));
});

blueGuard.once("spawn", () => {
  schedule(blueGuard, 3600, () => blueGuard.chat("/ctf join blue"));
});

setTimeout(() => {
  console.log("[edge3] own-flag-blocked-messages=", ownFlagBlockedMessages);
  console.log("Edge case script 3 complete");
  for (const bot of [admin, redCarrier, blueGuard]) {
    if (bot.quit) bot.quit();
  }
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
}, 47000);
