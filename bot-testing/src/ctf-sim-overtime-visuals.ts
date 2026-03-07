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

const overtimeTitleSamples: string[] = [];
const edgeCases: string[] = [];

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function stringify(value: any): string {
  if (value === null || value === undefined) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  if (Array.isArray(value)) {
    return value.map((entry) => stringify(entry)).join(" ");
  }
  if (typeof value === "object") {
    const asAny = value as Record<string, any>;
    if (typeof asAny.text === "string") {
      return asAny.text;
    }
    if (Array.isArray(asAny.extra)) {
      return stringify(asAny.extra);
    }
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function observeTitlePackets(bot: any): void {
  const client = (bot as any)?._client;
  if (!client || typeof client.on !== "function") {
    edgeCases.push("viewer_missing_client");
    return;
  }

  const recordTitle = (packet: any): void => {
    const text = stringify(packet?.text ?? packet?.title ?? packet?.content ?? packet).trim();
    if (!text) {
      return;
    }
    if (!text.toUpperCase().includes("OVERTIME")) {
      return;
    }
    overtimeTitleSamples.push(text);
  };

  client.on("set_title_text", (packet: any) => recordTitle(packet));
  client.on("set_title_subtitle_text", (packet: any) => recordTitle(packet));
  client.on("packet", (data: any, meta: any) => {
    const packetName = String(meta?.name ?? "");
    if (!packetName.includes("title")) {
      return;
    }
    recordTitle(data);
  });
}

function finish(): void {
  const uniqueSamples = Array.from(new Set(overtimeTitleSamples));
  const totalCount = overtimeTitleSamples.length;

  console.log(`[ot-visual] overtime title samples total=${totalCount} unique=${uniqueSamples.length}`);
  for (const sample of uniqueSamples.slice(-8)) {
    console.log(`[ot-visual] sample=${sample}`);
  }

  if (totalCount <= 0) {
    edgeCases.push("missing_overtime_title_packets");
  }
  if (totalCount > 20) {
    edgeCases.push("overtime_title_flashing_too_many_packets");
  }
  if (uniqueSamples.some((sample) => sample.includes("§k") || sample.toLowerCase().includes("obfuscated"))) {
    edgeCases.push("overtime_title_contains_obfuscated_segments");
  }

  if (edgeCases.length > 0) {
    console.log("[ot-visual] EDGE CASES:");
    for (const issue of edgeCases) {
      console.log(`[ot-visual] - ${issue}`);
    }
    process.exitCode = 1;
  } else {
    console.log("[ot-visual] PASS no overtime title visual edge cases");
  }

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
  setTimeout(() => process.exit(process.exitCode ?? 0), 500);
}

admin.once("spawn", () => {
  observeTitlePackets(admin);
  schedule(admin, 1500, () => admin.chat("/ctf join red"));
  schedule(blue, 1800, () => blue.chat("/ctf join blue"));
  schedule(admin, 4500, () => admin.chat("/ctf start"));

  // Force quick tie -> overtime.
  schedule(admin, 26000, () => admin.chat("/ctf setgametime 3"));

  schedule(admin, 47000, finish);
});
