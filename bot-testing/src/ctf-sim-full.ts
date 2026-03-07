import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA } from "./scenario-positions";
import { createBot, createRegistry, engageTarget, isReady, pathTo, schedule, selectKitOnOpen } from "./headless-utils";

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const redNames = [
  "RedLeader",
  "RedTwo",
  "RedThree",
  "RedFour",
  "RedFive",
  "RedSix",
  "RedSeven",
  "RedEight",
  "RedNine",
  "RedTen",
  "RedEleven",
  "RedTwelve"
];
const blueNames = [
  "BlueLeader",
  "BlueTwo",
  "BlueThree",
  "BlueFour",
  "BlueFive",
  "BlueSix",
  "BlueSeven",
  "BlueEight",
  "BlueNine",
  "BlueTen",
  "BlueEleven",
  "BlueTwelve"
];

const bots = new Map<string, any>();
const totalBots = redNames.length + blueNames.length;
const viewerCount = Math.max(0, Number(process.env.CTF_VIEW_COUNT || totalBots));

function registerBot(name: string, kit: "ranger" | "scout", viewerOffset?: number): void {
  const bot = createBot(name, runtime);
  bots.set(name, bot);
  registry.registerBot(bot, name, viewerOffset);
  selectKitOnOpen(bot, kit);
}

let viewerIndex = 0;
for (const name of redNames) {
  registerBot(name, name.endsWith("Leader") ? "ranger" : "scout", viewerIndex < viewerCount ? viewerIndex++ : -1);
}
for (const name of blueNames) {
  registerBot(name, name.endsWith("Leader") ? "ranger" : "scout", viewerIndex < viewerCount ? viewerIndex++ : -1);
}

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function useSpear(bot: any): void {
  try {
    bot.setQuickBarSlot(2);
  } catch {
    // ignore
  }
  try {
    (bot as any).activateItem?.();
  } catch {
    // ignore
  }
}

function useScoutTagger(bot: any): void {
  try {
    bot.setQuickBarSlot(0);
  } catch {
    // ignore
  }
  try {
    (bot as any).activateItem?.();
  } catch {
    // ignore
  }
}

async function capture(botName: string, flagPos: Vec3, returnPos: Vec3): Promise<void> {
  const bot = bots.get(botName);
  if (!bot || !isReady(bot)) {
    return;
  }
  bot.setControlState("sprint", true);
  await pathTo(bot, flagPos, 1.6, 25000);
  const block = bot.blockAt(flagPos);
  if (block) {
    await bot.dig(block, true).catch(() => undefined);
  }
  await pathTo(bot, returnPos, 2.4, 25000);
  bot.setControlState("sprint", false);
}

function startSkirmish(red: string[], blue: string[]): void {
  for (const name of red) {
    const bot = bots.get(name);
    if (!bot) continue;
    schedule(bot, 1000 + Math.random() * 1500, () => engageTarget(bot, blue[Math.floor(Math.random() * blue.length)], 9000));
  }
  for (const name of blue) {
    const bot = bots.get(name);
    if (!bot) continue;
    schedule(bot, 1000 + Math.random() * 1500, () => engageTarget(bot, red[Math.floor(Math.random() * red.length)], 9000));
  }
}

function joinTeams(): void {
  for (const name of redNames) {
    const bot = bots.get(name);
    if (!bot) continue;
    schedule(bot, 2000 + Math.random() * 800, () => bot.chat("/ctf join red"));
  }
  for (const name of blueNames) {
    const bot = bots.get(name);
    if (!bot) continue;
    schedule(bot, 2000 + Math.random() * 800, () => bot.chat("/ctf join blue"));
  }
}

const admin = bots.get("RedLeader");
if (admin) {
  admin.once("spawn", () => {
    joinTeams();

    schedule(admin, 5000, () => admin.chat(`/tp ${admin.username} ${ARENA.redFlag.x} ${ARENA.redFlag.y} ${ARENA.redFlag.z}`));
    schedule(admin, 6500, () => admin.chat("/ctf setflag red"));
    schedule(admin, 8000, () => admin.chat(`/tp ${admin.username} ${ARENA.blueFlag.x} ${ARENA.blueFlag.y} ${ARENA.blueFlag.z}`));
    schedule(admin, 9500, () => admin.chat("/ctf setflag blue"));
    schedule(admin, 10800, () => admin.chat(`/tp ${admin.username} ${ARENA.redReturn.x} ${ARENA.redReturn.y} ${ARENA.redReturn.z}`));
    schedule(admin, 12000, () => admin.chat("/ctf setreturn red"));
    schedule(admin, 13500, () => admin.chat(`/tp ${admin.username} ${ARENA.blueReturn.x} ${ARENA.blueReturn.y} ${ARENA.blueReturn.z}`));
    schedule(admin, 15000, () => admin.chat("/ctf setreturn blue"));

    schedule(admin, 17000, () => admin.chat("/ctf setscorelimit 5"));
    schedule(admin, 18500, () => admin.chat("/ctf start"));

    schedule(admin, 25000, () => startSkirmish(redNames, blueNames));

    schedule(admin, 30000, () => {
      const ranger = bots.get("RedLeader");
      if (ranger) useSpear(ranger);
    });
    schedule(admin, 32000, () => {
      const ranger = bots.get("BlueLeader");
      if (ranger) useSpear(ranger);
    });
    schedule(admin, 34000, () => {
      const scout = bots.get("RedTwo");
      if (scout) useScoutTagger(scout);
    });
    schedule(admin, 36000, () => {
      const scout = bots.get("BlueTwo");
      if (scout) useScoutTagger(scout);
    });

    schedule(admin, 40000, () => {
      capture("RedThree", new Vec3(ARENA.blueFlag.x, ARENA.blueFlag.y, ARENA.blueFlag.z), new Vec3(ARENA.redReturn.x, ARENA.redReturn.y, ARENA.redReturn.z));
    });
    schedule(admin, 52000, () => {
      capture("BlueThree", new Vec3(ARENA.redFlag.x, ARENA.redFlag.y, ARENA.redFlag.z), new Vec3(ARENA.blueReturn.x, ARENA.blueReturn.y, ARENA.blueReturn.z));
    });
    schedule(admin, 64000, () => {
      capture("RedFour", new Vec3(ARENA.blueFlag.x, ARENA.blueFlag.y, ARENA.blueFlag.z), new Vec3(ARENA.redReturn.x, ARENA.redReturn.y, ARENA.redReturn.z));
    });
    schedule(admin, 76000, () => {
      capture("BlueFour", new Vec3(ARENA.redFlag.x, ARENA.redFlag.y, ARENA.redFlag.z), new Vec3(ARENA.blueReturn.x, ARENA.blueReturn.y, ARENA.blueReturn.z));
    });

    schedule(admin, 88000, () => admin.chat("/ctf setscore red 2"));
    schedule(admin, 90000, () => admin.chat("/ctf setscore blue 2"));
    schedule(admin, 92000, () => admin.chat("/ctf setgametime 35"));

    schedule(admin, 112000, () => startSkirmish(redNames, blueNames));

    // In overtime, set winning score with confirm to stop.
    schedule(admin, 135000, () => admin.chat("/ctf setscore red 5 confirm"));

    schedule(admin, 165000, () => {
      console.log("[full] simulation complete");
      for (const bot of bots.values()) {
        if (bot.quit) bot.quit();
      }
      registry.clearRegistry();
      process.exit(0);
    });
  });
}
