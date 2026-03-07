import mineflayer from "mineflayer";
import type { Bot } from "mineflayer";
import { updateRegistry } from "./bot-registry";
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

const activeBots = new Set<string>();
const admin = createBot("RedLeader");
const blue = createBot("BlueSolo");

let sawLoneStart = false;
let loneTickCount = 0;
let sawGenericStop = false;
let sawAdminStop = false;

function syncRegistry(): void {
  updateRegistry([...activeBots], { address: addressOf(runtime), version: runtime.version });
}

function registerBot(bot: Bot): void {
  const name = bot.username;
  activeBots.add(name);
  syncRegistry();
  bot.on("end", () => {
    activeBots.delete(name);
    syncRegistry();
  });
  bot.on("kicked", () => {
    activeBots.delete(name);
    syncRegistry();
  });
}

registerBot(admin);
registerBot(blue);

admin.on("messagestr", (msg) => {
  if (msg.includes("Only one player remains. Match ends in")) {
    sawLoneStart = true;
  }
  if (msg.includes("due to low player count")) {
    loneTickCount++;
  }
  if (msg.includes("CTF stopped.")) {
    sawGenericStop = true;
  }
  if (msg.includes("CTF stopped: Stopped by admin")) {
    sawAdminStop = true;
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

admin.once("spawn", () => {
  schedule(admin, 1500, () => admin.chat("/ctf stop"));
  schedule(admin, 3000, () => admin.chat("/ctf join red"));
  schedule(admin, 5000, () => admin.chat(`/tp ${admin.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(admin, 6200, () => admin.chat("/ctf setflag red"));
  schedule(admin, 7800, () => admin.chat(`/tp ${admin.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(admin, 9000, () => admin.chat("/ctf setflag blue"));
  schedule(admin, 11000, () => admin.chat("/ctf start"));

  setTimeout(() => {
    console.log("[edge5] BlueSolo quitting to run lone-player timer to zero");
    blue.quit("edge5-leave");
  }, 26000);
});

blue.once("spawn", () => {
  schedule(blue, 3500, () => blue.chat("/ctf join blue"));
});

setTimeout(() => {
  console.log("[edge5] lone-start-seen=", sawLoneStart);
  console.log("[edge5] lone-tick-count=", loneTickCount);
  console.log("[edge5] generic-stop-seen=", sawGenericStop);
  console.log("[edge5] admin-stop-seen=", sawAdminStop);
  console.log("Edge case script 5 complete");

  for (const bot of [admin, blue]) {
    if (bot && bot.quit) {
      try {
        bot.quit();
      } catch (err: any) {
        console.log("quit error", err?.message ?? err);
      }
    }
  }
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
}, 56000);
