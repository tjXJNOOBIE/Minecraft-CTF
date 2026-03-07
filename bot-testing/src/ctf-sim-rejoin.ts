import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA } from "./scenario-positions";
import { createBot, createRegistry, pathTo, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const admin = createBot("RedLeader", runtime);
const redOne = createBot("RedRejoin", runtime);
const blueOne = createBot("BlueRejoin", runtime);

let blueCarrier = createBot("BlueCarrier", runtime);

registry.registerBot(admin, "RedLeader", 0);
registry.registerBot(redOne, "RedRejoin", 1);
registry.registerBot(blueOne, "BlueRejoin", 2);
registry.registerBot(blueCarrier, "BlueCarrier", 3);

selectKitOnOpen(admin, "ranger");
selectKitOnOpen(redOne, "scout");
selectKitOnOpen(blueOne, "scout");
selectKitOnOpen(blueCarrier, "scout");

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

async function carrierGrabAndQuit(): Promise<void> {
  blueCarrier.setControlState("sprint", true);
  const flagPos = new Vec3(ARENA.redFlag.x, ARENA.redFlag.y, ARENA.redFlag.z);
  await pathTo(blueCarrier, flagPos, 1.5, 25000);
  const block = blueCarrier.blockAt(flagPos);
  if (block) {
    await blueCarrier.dig(block, true).catch(() => undefined);
  }
  blueCarrier.setControlState("sprint", false);

  setTimeout(() => {
    console.log("[rejoin] BlueCarrier quitting while holding flag");
    if (blueCarrier.quit) blueCarrier.quit();
  }, 4000);
}

function respawnCarrier(): void {
  blueCarrier = createBot("BlueCarrier", runtime);
  registry.registerBot(blueCarrier, "BlueCarrier", 4);
  selectKitOnOpen(blueCarrier, "scout");
  schedule(blueCarrier, 2000, () => blueCarrier.chat("/ctf join blue"));
}

admin.once("spawn", () => {
  schedule(admin, 2000, () => admin.chat("/ctf join red"));
  schedule(redOne, 2300, () => redOne.chat("/ctf join red"));
  schedule(blueOne, 2000, () => blueOne.chat("/ctf join blue"));
  schedule(blueCarrier, 2300, () => blueCarrier.chat("/ctf join blue"));

  schedule(admin, 6000, () => admin.chat("/ctf start"));

  // Red player leaves and rejoins mid-match.
  schedule(redOne, 18000, () => redOne.chat("/ctf leave"));
  schedule(redOne, 23000, () => redOne.chat("/ctf join red"));

  // Blue carrier grabs flag and quits to simulate combat logging.
  schedule(admin, 26000, () => {
    carrierGrabAndQuit().catch((err) => console.log("carrier error", err?.message ?? err));
  });

  // Rejoin same player name.
  schedule(admin, 36000, () => respawnCarrier());

  schedule(admin, 60000, () => admin.chat("/ctf stop"));

  schedule(admin, 72000, () => {
    console.log("[rejoin] scenario complete");
    for (const bot of [admin, redOne, blueOne, blueCarrier]) {
      if (bot.quit) bot.quit();
    }
    registry.clearRegistry();
    process.exit(0);
  });
});
