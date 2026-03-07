import { loadRuntimeConfig } from "./runtime-config";
import { createBot, createRegistry, isReady, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const admin = createBot("RedLeader", runtime);
const blue = createBot("BlueRunner", runtime);

registry.registerBot(admin, "RedLeader", 0);
registry.registerBot(blue, "BlueRunner", 1);

selectKitOnOpen(admin, "ranger");
selectKitOnOpen(blue, "scout");

let overtimeCount = 0;
let timeoutStop = false;

function onMessage(message: string): void {
  const line = String(message || "");
  if (line.includes("OVERTIME")) {
    overtimeCount += 1;
  }
  if (line.includes("Time limit reached")) {
    timeoutStop = true;
  }
}

admin.on("messagestr", onMessage);
blue.on("messagestr", onMessage);

function finish(): void {
  console.log(`[ot-chain] overtimeCount=${overtimeCount} timeoutStop=${timeoutStop}`);
  for (const bot of [admin, blue]) {
    try {
      if (isReady(bot) && bot.quit) {
        bot.quit();
      }
    } catch {
      // ignore quit errors
    }
  }
  registry.clearRegistry();

  if (overtimeCount >= 2 && !timeoutStop) {
    console.log("[ot-chain] PASS overtime chain extended");
    process.exit(0);
  }

  console.log("[ot-chain] FAIL overtime did not extend as expected");
  process.exit(1);
}

admin.once("spawn", () => {
  schedule(admin, 1500, () => admin.chat("/ctf join red"));
  schedule(blue, 1800, () => blue.chat("/ctf join blue"));
  schedule(admin, 4500, () => admin.chat("/ctf start"));

  // First overtime trigger.
  schedule(admin, 26000, () => admin.chat("/ctf setgametime 3"));

  // While still tied in overtime, shorten again to force another overtime cycle.
  schedule(admin, 42000, () => admin.chat("/ctf setgametime 3"));

  schedule(admin, 70000, finish);
});
