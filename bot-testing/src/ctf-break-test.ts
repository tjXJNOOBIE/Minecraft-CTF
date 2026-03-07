import mineflayer from "mineflayer";
import { Vec3 } from "vec3";
import type { Bot } from "mineflayer";
import { updateRegistry } from "./bot-registry";
import { startBotViewer } from "./bot-viewer";
import { addressOf, loadRuntimeConfig } from "./runtime-config";

const runtime = loadRuntimeConfig();
const bot: Bot = mineflayer.createBot({ host: runtime.host, port: runtime.port, username: "BreakBot", auth: "offline", version: runtime.version });

const activeBots = new Set<string>();
const viewUrlsByBot = new Map<string, string>();
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const name = bot.username || "BreakBot";
activeBots.add(name);
updateRegistry(
  [...activeBots],
  { address: addressOf(runtime), version: runtime.version },
  Object.fromEntries(viewUrlsByBot)
);

bot.once("spawn", () => {
  const viewerPort = viewerBasePort;
  try {
    const started = startBotViewer(bot, viewerPort);
    if (!started) {
      console.log("[viewer] %s unavailable", name);
      return;
    }
    const url = `http://${viewerHost}:${viewerPort}/`;
    viewUrlsByBot.set(name, url);
    updateRegistry(
      [...activeBots],
      { address: addressOf(runtime), version: runtime.version },
      Object.fromEntries(viewUrlsByBot)
    );
    console.log("[viewer] %s -> %s", name, url);
  } catch (err: any) {
    console.log("[viewer] %s failed: %s", name, err?.message ?? err);
  }
});

bot.on("end", () => {
  activeBots.delete(name);
  viewUrlsByBot.delete(name);
  updateRegistry(
    [...activeBots],
    { address: addressOf(runtime), version: runtime.version },
    Object.fromEntries(viewUrlsByBot)
  );
});
bot.on("kicked", () => {
  activeBots.delete(name);
  viewUrlsByBot.delete(name);
  updateRegistry(
    [...activeBots],
    { address: addressOf(runtime), version: runtime.version },
    Object.fromEntries(viewUrlsByBot)
  );
});

process.on("exit", () => updateRegistry([], { address: addressOf(runtime), version: runtime.version }));
process.on("SIGINT", () => {
  updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  process.exit(0);
});

bot.on("messagestr", (msg) => console.log(`[BreakBot] ${msg}`));
bot.on("error", (err) => console.log("BreakBot error", err.message));
bot.on("kicked", (reason) => console.log("BreakBot kicked", reason));

bot.once("spawn", () => {
  setTimeout(() => bot.chat("/tp 2 65 2"), 4000);
  setTimeout(() => {
    const block = bot.blockAt(new Vec3(2, 64, 2));
    if (!block) {
      console.log("BreakBot block missing at 2 64 2");
      return;
    }
    bot.dig(block, true).catch((err: any) => console.log("BreakBot dig error", err?.message ?? err));
  }, 7000);
  setTimeout(() => {
    bot.quit();
    process.exit(0);
  }, 12000);
});
