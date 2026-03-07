import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA } from "./scenario-positions";
import { createBot, createRegistry, pathTo, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const redLeader = createBot("RedLeader", runtime);
const redHelper = createBot("RedHelper", runtime);
const blueScout = createBot("BlueScout", runtime);
const blueTarget = createBot("BlueTarget", runtime);

registry.registerBot(redLeader, "RedLeader", 0);
registry.registerBot(redHelper, "RedHelper", 1);
registry.registerBot(blueScout, "BlueScout", 2);
registry.registerBot(blueTarget, "BlueTarget", 3);

selectKitOnOpen(redLeader, "ranger");
selectKitOnOpen(redHelper, "scout");
selectKitOnOpen(blueScout, "scout");
selectKitOnOpen(blueTarget, "scout");

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function swapHands(bot: any): void {
  if (typeof bot.swapHands === "function") {
    bot.swapHands();
    return;
  }
  if (bot._client) {
    bot._client.write("block_dig", { status: 6, location: { x: 0, y: 0, z: 0 }, face: 1 });
  }
}

async function moveAllToCenter(): Promise<void> {
  const center = new Vec3(ARENA.center.x, ARENA.center.y, ARENA.center.z);
  await Promise.all([
    pathTo(redLeader, center, 2, 20000),
    pathTo(redHelper, center, 2, 20000),
    pathTo(blueScout, center, 2, 20000),
    pathTo(blueTarget, center, 2, 20000)
  ]);
}

redLeader.once("spawn", () => {
  schedule(redLeader, 2000, () => redLeader.chat("/ctf join red"));
  schedule(redHelper, 2300, () => redHelper.chat("/ctf join red"));
  schedule(blueScout, 2000, () => blueScout.chat("/ctf join blue"));
  schedule(blueTarget, 2300, () => blueTarget.chat("/ctf join blue"));

  schedule(redLeader, 6000, () => redLeader.chat("/ctf start"));

  schedule(redLeader, 20000, () => {
    moveAllToCenter().catch((err) => console.log("move error", err?.message ?? err));
  });

  // Scout throws snowballs at red helper.
  schedule(blueScout, 26000, () => {
    blueScout.setControlState("sprint", true);
    blueScout.setQuickBarSlot(0);
    const interval = setInterval(() => {
      blueScout.activateItem();
    }, 2200);
    setTimeout(() => {
      clearInterval(interval);
      blueScout.setControlState("sprint", false);
    }, 12000);
  });

  // Ranger uses F-throw spear ability.
  schedule(redLeader, 30000, () => {
    redLeader.setQuickBarSlot(2);
    swapHands(redLeader as any);
  });

  // Vanilla trident throw + return.
  schedule(redLeader, 38000, () => {
    redLeader.setQuickBarSlot(2);
    redLeader.activateItem();
    setTimeout(() => redLeader.deactivateItem(), 800);
  });

  // Scout tries to throw while carrying flag (should be blocked).
  schedule(blueScout, 46000, async () => {
    const flagPos = new Vec3(ARENA.redFlag.x, ARENA.redFlag.y, ARENA.redFlag.z);
    await pathTo(blueScout, flagPos, 1.5, 20000);
    const block = blueScout.blockAt(flagPos);
    if (block) {
      await blueScout.dig(block, true).catch(() => undefined);
    }
    blueScout.setQuickBarSlot(0);
    blueScout.activateItem();
  });

  // Ranger tries spear while carrying flag (should be blocked).
  schedule(redLeader, 52000, async () => {
    const flagPos = new Vec3(ARENA.blueFlag.x, ARENA.blueFlag.y, ARENA.blueFlag.z);
    await pathTo(redLeader, flagPos, 1.5, 20000);
    const block = redLeader.blockAt(flagPos);
    if (block) {
      await redLeader.dig(block, true).catch(() => undefined);
    }
    redLeader.setQuickBarSlot(2);
    swapHands(redLeader as any);
  });

  schedule(redLeader, 70000, () => {
    console.log("[abilities] scenario complete");
    for (const bot of [redLeader, redHelper, blueScout, blueTarget]) {
      if (bot.quit) bot.quit();
    }
    registry.clearRegistry();
    process.exit(0);
  });
});
