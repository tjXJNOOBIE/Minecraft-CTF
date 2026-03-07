import mineflayer from "mineflayer";
import type { Bot } from "mineflayer";
import { Vec3 } from "vec3";
import { pathfinder, Movements, goals } from "mineflayer-pathfinder";
import { updateRegistry } from "./bot-registry";
import { startBotViewer } from "./bot-viewer";
import { addressOf, loadRuntimeConfig } from "./runtime-config";

const { GoalBlock, GoalNear } = goals;

export type RuntimeConfig = ReturnType<typeof loadRuntimeConfig>;

function shouldLogBotMessage(msg: string): boolean {
  const text = msg.trim();
  if (!text) {
    return false;
  }

  const lower = text.toLowerCase();
  if (lower.includes("scout tagger")) {
    return false;
  }
  if (lower.includes("cd:")) {
    return false;
  }
  if (lower.includes("return to base")) {
    return false;
  }
  if (lower.includes("your flag is down")) {
    return false;
  }
  if (lower.includes("you are the flag carrier")) {
    return false;
  }
  return true;
}

function parseBoolEnv(raw: string | undefined): boolean | undefined {
  if (raw === undefined) {
    return undefined;
  }
  const normalized = raw.trim().toLowerCase();
  if (["1", "true", "yes", "on"].includes(normalized)) {
    return true;
  }
  if (["0", "false", "no", "off"].includes(normalized)) {
    return false;
  }
  return undefined;
}

function viewersDisabledByConfig(): boolean {
  const explicitDisable = parseBoolEnv(process.env.CTF_DISABLE_VIEWERS);
  if (explicitDisable !== undefined) {
    return explicitDisable;
  }
  const viewCountRaw = process.env.CTF_VIEW_COUNT;
  if (viewCountRaw === undefined) {
    return false;
  }
  const viewCount = Number(viewCountRaw);
  return Number.isFinite(viewCount) && viewCount <= 0;
}

export function createBot(username: string, runtime: RuntimeConfig): Bot {
  const timeoutMs = Number(process.env.CTF_BOT_TIMEOUT_MS || 60000);
  const checkTimeoutInterval = Number.isFinite(timeoutMs) && timeoutMs > 0 ? Math.floor(timeoutMs) : 60000;
  const bot = mineflayer.createBot({
    host: runtime.host,
    port: runtime.port,
    username,
    auth: "offline",
    version: runtime.version,
    checkTimeoutInterval
  });
  bot.loadPlugin(pathfinder);
  bot.on("messagestr", (msg) => {
    if (!shouldLogBotMessage(msg)) {
      return;
    }
    console.log(`[${username}] ${msg}`);
  });
  bot.on("error", (err) => console.log(username, "error", err.message));
  bot.on("kicked", (reason) => console.log(username, "kicked", reason));
  bot.once("spawn", () => configureMovements(bot));
  return bot;
}

export function configureMovements(bot: Bot): void {
  try {
    const mcData = require("minecraft-data")(bot.version);
    const movements = new Movements(bot, mcData);
    movements.allowParkour = true;
    movements.allowSprinting = true;
    movements.canOpenDoors = true;
    movements.canDig = false;
    bot.pathfinder.setMovements(movements);
  } catch (err: any) {
    console.log(bot.username, "movement init failed", err?.message ?? err);
  }
}

export function isReady(bot: Bot): boolean {
  const client = (bot as any)._client;
  return Boolean(client && client.state === "play");
}

export function schedule(bot: Bot, delayMs: number, action: () => void): void {
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

export function createRegistry(runtime: RuntimeConfig, viewerHost: string, viewerBasePort = 8600) {
  const activeBots = new Set<string>();
  const viewUrlsByBot = new Map<string, string>();
  const viewerStaggerMs = Number(process.env.CTF_VIEW_STAGGER_MS || 0);
  const viewersDisabled = viewersDisabledByConfig();

  function syncRegistry(): void {
    updateRegistry(
      [...activeBots],
      { address: addressOf(runtime), version: runtime.version },
      Object.fromEntries(viewUrlsByBot)
    );
  }

  function registerBot(bot: Bot, fallbackName: string, viewerOffset?: number): void {
    const name = bot.username || fallbackName;
    activeBots.add(name);
    syncRegistry();
    bot.once("spawn", () => {
      if (viewerOffset === undefined || viewerOffset < 0 || viewersDisabled) {
        return;
      }
      const viewerPort = viewerBasePort + viewerOffset;
      const startViewer = () => {
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
      };
      if (viewerStaggerMs > 0 && viewerOffset > 0) {
        setTimeout(startViewer, viewerOffset * viewerStaggerMs);
      } else {
        startViewer();
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

  function clearRegistry(): void {
    updateRegistry([], { address: addressOf(runtime), version: runtime.version });
  }

  return { registerBot, syncRegistry, clearRegistry, activeBots };
}

export function pathTo(bot: Bot, target: Vec3, range = 1, timeoutMs = 15000): Promise<boolean> {
  return new Promise((resolve) => {
    if (!bot.pathfinder) {
      resolve(false);
      return;
    }

    const goal = range <= 0 ? new GoalBlock(target.x, target.y, target.z) : new GoalNear(target.x, target.y, target.z, range);
    let resolved = false;
    const timeout = setTimeout(() => {
      if (resolved) return;
      resolved = true;
      bot.pathfinder.setGoal(null);
      resolve(false);
    }, timeoutMs);

    const onGoal = () => {
      if (resolved) return;
      resolved = true;
      clearTimeout(timeout);
      bot.pathfinder.setGoal(null);
      resolve(true);
    };

    bot.pathfinder.setGoal(goal);
    bot.once("goal_reached", onGoal);
  });
}

export function moveToPlayer(bot: Bot, targetName: string, range = 2, timeoutMs = 15000): Promise<boolean> {
  const target = bot.players[targetName]?.entity;
  if (!target) {
    return Promise.resolve(false);
  }
  return pathTo(bot, target.position, range, timeoutMs);
}

export function engageTarget(bot: Bot, targetName: string, durationMs: number): void {
  const start = Date.now();
  const interval = setInterval(() => {
    if (!isReady(bot)) {
      return;
    }
    if (!bot.entity) {
      return;
    }
    const target = bot.players[targetName]?.entity;
    if (!target) {
      return;
    }

    try {
      bot.lookAt(target.position.offset(0, 1.6, 0), true);
    } catch {
      // ignore
    }

    const distance = bot.entity.position.distanceTo(target.position);
    if (distance <= 3.2) {
      try {
        bot.attack(target);
      } catch {
        // ignore
      }
      randomStrafe(bot);
      maybeJump(bot);
    } else {
      bot.setControlState("sprint", true);
      bot.pathfinder.setGoal(new GoalNear(target.position.x, target.position.y, target.position.z, 2));
    }

    if (Date.now() - start >= durationMs) {
      clearInterval(interval);
      bot.pathfinder.setGoal(null);
      bot.setControlState("sprint", false);
      stopStrafe(bot);
    }
  }, 300);
}

export function randomStrafe(bot: Bot): void {
  const roll = Math.random();
  if (roll < 0.33) {
    bot.setControlState("left", true);
    bot.setControlState("right", false);
  } else if (roll < 0.66) {
    bot.setControlState("right", true);
    bot.setControlState("left", false);
  } else {
    stopStrafe(bot);
  }
}

export function stopStrafe(bot: Bot): void {
  bot.setControlState("left", false);
  bot.setControlState("right", false);
}

export function maybeJump(bot: Bot): void {
  if (Math.random() < 0.25) {
    bot.setControlState("jump", true);
    setTimeout(() => bot.setControlState("jump", false), 250);
  }
}

export function selectKitOnOpen(bot: Bot, kit: "scout" | "ranger"): void {
  const targetSlot = kit === "ranger" ? 5 : 3;
  let handled = false;
  bot.on("windowOpen", (window) => {
    if (handled) {
      return;
    }
    const titleValue = (window as any).title;
    const title = typeof titleValue === "string" ? titleValue : JSON.stringify(titleValue);
    if (!title.toLowerCase().includes("select kit")) {
      return;
    }
    handled = true;
    setTimeout(() => {
      bot.clickWindow(targetSlot, 0, 0).catch((err: any) => {
        console.log(bot.username, "kit select failed", err?.message ?? err);
      });
      try {
        bot.closeWindow(window);
      } catch (err: any) {
        console.log(bot.username, "kit close failed", err?.message ?? err);
      }
    }, 250);
  });
}
