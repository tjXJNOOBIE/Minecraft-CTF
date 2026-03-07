import { loadRuntimeConfig } from "./runtime-config";
import { createBot, createRegistry, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const admin = createBot("RedLeader", runtime);
const redSolo = createBot("RedSolo", runtime);
const blueSolo = createBot("BlueSolo", runtime);
const blueSupport = createBot("BlueSupport", runtime);

registry.registerBot(admin, "RedLeader", 0);
registry.registerBot(redSolo, "RedSolo", 1);
registry.registerBot(blueSolo, "BlueSolo", 2);
registry.registerBot(blueSupport, "BlueSupport", 3);

selectKitOnOpen(admin, "ranger");
selectKitOnOpen(redSolo, "scout");
selectKitOnOpen(blueSolo, "scout");
selectKitOnOpen(blueSupport, "scout");

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

admin.once("spawn", () => {
  schedule(admin, 2000, () => admin.chat("/ctf join red"));
  schedule(redSolo, 2300, () => redSolo.chat("/ctf join red"));
  schedule(blueSolo, 2000, () => blueSolo.chat("/ctf join blue"));
  schedule(blueSupport, 2300, () => blueSupport.chat("/ctf join blue"));

  schedule(admin, 6000, () => admin.chat("/ctf start"));

  // Force quick tie into overtime.
  schedule(admin, 18000, () => admin.chat("/ctf setgametime 5"));

  // Wait for overtime + timeout.
  schedule(admin, 190000, () => {
    console.log("[ot-timeout] scenario complete");
    for (const bot of [admin, redSolo, blueSolo, blueSupport]) {
      if (bot.quit) bot.quit();
    }
    registry.clearRegistry();
    process.exit(0);
  });
});
