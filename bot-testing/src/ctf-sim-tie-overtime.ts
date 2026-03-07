import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA } from "./scenario-positions";
import { createBot, createRegistry, pathTo, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const admin = createBot("RedLeader", runtime);
const redRunner = createBot("RedRunner", runtime);
const blueRunner = createBot("BlueRunner", runtime);
const redSupport = createBot("RedSupport", runtime);
const blueSupport = createBot("BlueSupport", runtime);
const blueGuard = createBot("BlueGuard", runtime);

registry.registerBot(admin, "RedLeader", 0);
registry.registerBot(redRunner, "RedRunner", 1);
registry.registerBot(blueRunner, "BlueRunner", 2);
registry.registerBot(redSupport, "RedSupport", 3);
registry.registerBot(blueSupport, "BlueSupport", 4);
registry.registerBot(blueGuard, "BlueGuard", 5);

selectKitOnOpen(admin, "ranger");
selectKitOnOpen(redRunner, "scout");
selectKitOnOpen(blueRunner, "scout");
selectKitOnOpen(redSupport, "scout");
selectKitOnOpen(blueSupport, "scout");
selectKitOnOpen(blueGuard, "scout");

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

async function capture(bot: any, flagPos: Vec3, returnPos: Vec3): Promise<void> {
  bot.setControlState("sprint", true);
  await pathTo(bot, flagPos, 1.5, 25000);
  const block = bot.blockAt(flagPos);
  if (block) {
    await bot.dig(block, true).catch(() => undefined);
  }
  await pathTo(bot, returnPos, 2.5, 25000);
  bot.setControlState("sprint", false);
}

admin.once("spawn", () => {
  schedule(admin, 2000, () => admin.chat("/ctf join red"));
  schedule(redRunner, 2300, () => redRunner.chat("/ctf join red"));
  schedule(redSupport, 2600, () => redSupport.chat("/ctf join red"));
  schedule(blueRunner, 2000, () => blueRunner.chat("/ctf join blue"));
  schedule(blueSupport, 2300, () => blueSupport.chat("/ctf join blue"));
  schedule(blueGuard, 2600, () => blueGuard.chat("/ctf join blue"));

  schedule(admin, 6000, () => admin.chat("/ctf start"));

  schedule(admin, 18000, () => {
    capture(redRunner, new Vec3(ARENA.blueFlag.x, ARENA.blueFlag.y, ARENA.blueFlag.z), new Vec3(ARENA.redReturn.x, ARENA.redReturn.y, ARENA.redReturn.z));
  });

  schedule(admin, 26000, () => {
    capture(blueRunner, new Vec3(ARENA.redFlag.x, ARENA.redFlag.y, ARENA.redFlag.z), new Vec3(ARENA.blueReturn.x, ARENA.blueReturn.y, ARENA.blueReturn.z));
  });

  // Validate setgametime clamp behavior.
  schedule(admin, 34000, () => admin.chat("/ctf setgametime -10"));
  schedule(admin, 36000, () => admin.chat("/ctf setgametime 99999"));

  // Force overtime quickly.
  schedule(admin, 38000, () => admin.chat("/ctf setgametime 45"));

  // In overtime, red scores to win.
  schedule(admin, 44000, () => {
    capture(redRunner, new Vec3(ARENA.blueFlag.x, ARENA.blueFlag.y, ARENA.blueFlag.z), new Vec3(ARENA.redReturn.x, ARENA.redReturn.y, ARENA.redReturn.z));
  });

  schedule(admin, 90000, () => {
    console.log("[tie] scenario complete");
    for (const bot of [admin, redRunner, blueRunner, redSupport, blueSupport, blueGuard]) {
      if (bot.quit) bot.quit();
    }
    registry.clearRegistry();
    process.exit(0);
  });
});
