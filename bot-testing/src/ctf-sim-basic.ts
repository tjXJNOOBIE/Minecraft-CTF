import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA } from "./scenario-positions";
import { createBot, createRegistry, engageTarget, pathTo, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const redLeader = createBot("RedLeader", runtime);
const redOne = createBot("RedOne", runtime);
const redTwo = createBot("RedTwo", runtime);
const blueLeader = createBot("BlueLeader", runtime);
const blueOne = createBot("BlueOne", runtime);
const blueTwo = createBot("BlueTwo", runtime);

registry.registerBot(redLeader, "RedLeader", 0);
registry.registerBot(redOne, "RedOne", 1);
registry.registerBot(redTwo, "RedTwo", 2);
registry.registerBot(blueLeader, "BlueLeader", 3);
registry.registerBot(blueOne, "BlueOne", 4);
registry.registerBot(blueTwo, "BlueTwo", 5);

selectKitOnOpen(redOne, "scout");
selectKitOnOpen(redTwo, "scout");
selectKitOnOpen(blueOne, "scout");
selectKitOnOpen(blueTwo, "scout");
selectKitOnOpen(redLeader, "ranger");
selectKitOnOpen(blueLeader, "ranger");

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function captureSequence(botName: string, targetFlag: Vec3, returnPoint: Vec3): Promise<void> {
  const bot = [redLeader, redOne, redTwo, blueLeader, blueOne, blueTwo].find((b) => b.username === botName);
  if (!bot) return;
  bot.setControlState("sprint", true);
  await pathTo(bot, targetFlag, 1.5, 25000);
  const block = bot.blockAt(targetFlag);
  if (block) {
    await bot.dig(block, true).catch((err: any) => console.log(bot.username, "dig error", err?.message ?? err));
  }
  await sleep(1000);
  await pathTo(bot, returnPoint, 2.5, 25000);
  bot.setControlState("sprint", false);
}

function joinTeams(): void {
  schedule(redLeader, 2000, () => redLeader.chat("/ctf join red"));
  schedule(redOne, 2300, () => redOne.chat("/ctf join red"));
  schedule(redTwo, 2600, () => redTwo.chat("/ctf join red"));
  schedule(blueLeader, 2000, () => blueLeader.chat("/ctf join blue"));
  schedule(blueOne, 2300, () => blueOne.chat("/ctf join blue"));
  schedule(blueTwo, 2600, () => blueTwo.chat("/ctf join blue"));
}

redLeader.once("spawn", () => {
  joinTeams();
  schedule(redLeader, 6000, () => redLeader.chat("/ctf start"));

  schedule(redLeader, 22000, () => {
    captureSequence("RedOne", new Vec3(ARENA.blueFlag.x, ARENA.blueFlag.y, ARENA.blueFlag.z), new Vec3(ARENA.redReturn.x, ARENA.redReturn.y, ARENA.redReturn.z));
  });

  schedule(redLeader, 26000, () => {
    captureSequence("BlueOne", new Vec3(ARENA.redFlag.x, ARENA.redFlag.y, ARENA.redFlag.z), new Vec3(ARENA.blueReturn.x, ARENA.blueReturn.y, ARENA.blueReturn.z));
  });

  schedule(redLeader, 30000, () => engageTarget(redTwo, blueTwo.username, 20000));
  schedule(redLeader, 30500, () => engageTarget(blueTwo, redTwo.username, 20000));

  schedule(redLeader, 65000, () => {
    console.log("[basic] scenario complete");
    for (const bot of [redLeader, redOne, redTwo, blueLeader, blueOne, blueTwo]) {
      if (bot.quit) bot.quit();
    }
    registry.clearRegistry();
    process.exit(0);
  });
});
