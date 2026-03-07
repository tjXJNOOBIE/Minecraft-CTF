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
let rescuer: Bot | null = null;

let sawLoneStart = false;
let loneTicks = 0;
let sawLoneCancel = false;
let sawUnexpectedStop = false;

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
    loneTicks++;
  }
  if (msg.includes("Player count recovered. Match continues.")) {
    sawLoneCancel = true;
  }
  if (msg.includes("CTF stopped: Time limit reached") || msg.includes("CTF stopped: Stopped by admin")) {
    sawUnexpectedStop = true;
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

  // Leave one player in-match to trigger lone-player stop countdown.
  setTimeout(() => {
    console.log("[edge4] BlueSolo quitting to trigger lone-player timer");
    blue.quit("edge4-leave");
  }, 26000);

  // Rejoin during countdown and verify cancellation.
  setTimeout(() => {
    rescuer = createBot("RescueBlue");
    registerBot(rescuer);
    rescuer.once("spawn", () => {
      schedule(rescuer!, 1200, () => rescuer!.chat("/ctf join blue"));
    });
  }, 34000);

  schedule(admin, 52000, () => admin.chat("/ctf stop"));
});

blue.once("spawn", () => {
  schedule(blue, 3500, () => blue.chat("/ctf join blue"));
});

setTimeout(() => {
  console.log("[edge4] lone-start-seen=", sawLoneStart);
  console.log("[edge4] lone-tick-count=", loneTicks);
  console.log("[edge4] lone-cancel-seen=", sawLoneCancel);
  console.log("[edge4] unexpected-stop-seen=", sawUnexpectedStop);
  console.log("Edge case script 4 complete");

  for (const bot of [admin, blue, rescuer]) {
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
}, 60000);
