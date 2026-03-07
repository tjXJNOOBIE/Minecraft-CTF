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
const breakBlock = { x: 0, y: 64, z: 0 };
const placeBlockPos = { x: 1, y: 64, z: 0 };

function createBot(username: string): Bot {
  const bot = mineflayer.createBot({ host, port, username, auth: "offline", version });
  bot.on("messagestr", (msg) => console.log(`[${username}] ${msg}`));
  bot.on("error", (err) => console.log(username, "error", err.message));
  bot.on("kicked", (reason) => console.log(username, "kicked", reason));
  return bot;
}

const admin = createBot("RedLeader");
const ranger = createBot("RangerBot");
const blue = createBot("BlueScout");
const lateJoiner = createBot("LateJoiner");

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
registerBot(ranger, "RangerBot", 1);
registerBot(blue, "BlueScout", 2);
registerBot(lateJoiner, "LateJoiner", 3);

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

function selectKitOnce(bot: Bot, slot: number): void {
  let handled = false;
  bot.on("windowOpen", (window) => {
    if (handled) return;
    const title = typeof (window as any).title === "string" ? (window as any).title : JSON.stringify((window as any).title);
    if (!title.includes("Select Kit")) return;
    handled = true;
    setTimeout(() => {
      bot.clickWindow(slot, 0, 0).catch((err: any) => console.log(bot.username, "kit click error", err?.message ?? err));
      try {
        bot.closeWindow(window);
      } catch (err: any) {
        console.log(bot.username, "kit close error", err?.message ?? err);
      }
    }, 250);
  });
}

function digBlock(bot: Bot, x: number, y: number, z: number): void {
  const block = bot.blockAt(new Vec3(x, y, z));
  if (!block) {
    console.log(bot.username, "block missing at", x, y, z);
    return;
  }
  bot.dig(block, true).catch((err: any) => console.log(bot.username, "dig error", err?.message ?? err));
}

function placeBlock(bot: Bot, x: number, y: number, z: number): void {
  const support = bot.blockAt(new Vec3(x, y - 1, z));
  if (!support) {
    console.log(bot.username, "no support block for place", x, y, z);
    return;
  }
  bot.placeBlock(support, new Vec3(0, 1, 0)).catch((err: any) => {
    const message = String(err?.message ?? err ?? "");
    const isBlockUpdateTimeout = message.includes("blockUpdate:") && message.includes("did not fire within timeout");
    if (isBlockUpdateTimeout) {
      const targetBlock = bot.blockAt(new Vec3(x, y, z));
      const targetName = targetBlock?.name ?? "missing";
      if (targetName === "air" || targetName === "cave_air" || targetName === "void_air") {
        console.log(`${bot.username} place blocked as expected at ${x}, ${y}, ${z}`);
        return;
      }
    }
    console.log(bot.username, "place error", message);
  });
}

admin.once("spawn", () => {
  schedule(admin, 2000, () => admin.chat("/ctf join red"));
  schedule(admin, 5000, () => admin.chat(`/tp ${admin.username} ${redBase.x} ${redBase.y} ${redBase.z}`));
  schedule(admin, 6500, () => admin.chat("/ctf setflag red"));

  schedule(admin, 8000, () => admin.chat(`/tp ${admin.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));
  schedule(admin, 9500, () => admin.chat("/ctf setflag blue"));

  schedule(admin, 12000, () => admin.chat("/ctf start"));
  schedule(admin, 12500, () => admin.chat("/ctf setscorelimit 10"));
  schedule(admin, 12900, () => admin.chat("/ctf setscore red 1"));
  schedule(admin, 13300, () => admin.chat("/ctf setscore blue 1"));

  schedule(admin, 14000, () => admin.chat(`/tp ${ranger.username} ${breakBlock.x} ${breakBlock.y + 1} ${breakBlock.z}`));
  schedule(admin, 15500, () => admin.chat(`/give ${ranger.username} stone 1`));

  schedule(admin, 19500, () => admin.chat(`/tp ${ranger.username} 8 64 8`));
  schedule(admin, 20000, () => admin.chat(`/tp ${blue.username} 11 64 8`));

  schedule(admin, 24000, () => admin.chat(`/tp ${ranger.username} ${blueBase.x} ${blueBase.y} ${blueBase.z}`));

  schedule(admin, 30000, () => admin.chat("/ctf stop"));
});

ranger.once("spawn", () => {
  selectKitOnce(ranger, 5);
  schedule(ranger, 2500, () => ranger.chat("/ctf join red"));

  schedule(ranger, 16000, () => digBlock(ranger, breakBlock.x, breakBlock.y, breakBlock.z));
  schedule(ranger, 17000, () => {
    const block = ranger.blockAt(new Vec3(breakBlock.x, breakBlock.y, breakBlock.z));
    console.log("RangerBot break result", block ? block.name : "missing");
  });

  schedule(ranger, 18000, () => placeBlock(ranger, placeBlockPos.x, placeBlockPos.y, placeBlockPos.z));
  schedule(ranger, 19000, () => {
    const block = ranger.blockAt(new Vec3(placeBlockPos.x, placeBlockPos.y, placeBlockPos.z));
    console.log("RangerBot place result", block ? block.name : "missing");
  });

  schedule(ranger, 21000, () => {
    try { ranger.setQuickBarSlot(2); } catch (err: any) { console.log("RangerBot slot error", err?.message ?? err); }
    try { (ranger as any).activateItem?.(); } catch (err: any) { console.log("RangerBot spear error", err?.message ?? err); }
  });

  schedule(ranger, 22000, () => {
    try { ranger.setQuickBarSlot(2); } catch (err: any) { console.log("RangerBot slot error", err?.message ?? err); }
    try { (ranger as any).activateItem?.(); } catch (err: any) { console.log("RangerBot spear cooldown error", err?.message ?? err); }
  });

  schedule(ranger, 25500, () => digBlock(ranger, blueBase.x, blueBase.y, blueBase.z));
  schedule(ranger, 26500, () => {
    try { ranger.setQuickBarSlot(2); } catch (err: any) { console.log("RangerBot slot error", err?.message ?? err); }
    try { (ranger as any).activateItem?.(); } catch (err: any) { console.log("RangerBot spear carry error", err?.message ?? err); }
  });
});

blue.once("spawn", () => {
  selectKitOnce(blue, 3);
  schedule(blue, 2500, () => blue.chat("/ctf join blue"));
});

lateJoiner.once("spawn", () => {
  schedule(lateJoiner, 30200, () => lateJoiner.chat("/ctf join red"));
});

setTimeout(() => {
  console.log("Edge case follow-up script complete");
  for (const bot of [admin, ranger, blue, lateJoiner]) {
    if (bot.quit) bot.quit();
  }
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
}, 45000);
