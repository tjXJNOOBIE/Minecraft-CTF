import type { Bot } from "mineflayer";
import { createBot, createRegistry, schedule } from "./headless-utils";
import { loadRuntimeConfig } from "./runtime-config";

type Sample = { t: number; label: string };

const runtime = loadRuntimeConfig();
const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

const redRanger = createBot("RedLeader", runtime);
const blueScout = createBot("BlueScout", runtime);

registry.registerBot(redRanger, "RedLeader", 0);
registry.registerBot(blueScout, "BlueScout", 1);

forceKitSelection(redRanger, 5);
forceKitSelection(blueScout, 3);

const startedAt = Date.now();
const spearNameSamples: Sample[] = [];
const scoutNameSamples: Sample[] = [];
const spearActionBarSamples: Sample[] = [];
const scoutActionBarSamples: Sample[] = [];
const edgeCases: string[] = [];
let sampler: NodeJS.Timeout | null = null;
let detectedSpearSlot: number | null = null;
let detectedScoutSlot: number | null = null;
let matchFlowStarted = false;
let debugActionbarLogged = false;
let packetStreamLogged = false;
let dataQueryTimer: NodeJS.Timeout | null = null;

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function relMs(): number {
  return Date.now() - startedAt;
}

function swapHands(bot: any): void {
  if (typeof bot.swapHands === "function") {
    bot.swapHands();
    return;
  }
  if (bot._client) {
    bot._client.write("block_dig", { status: 6, location: { x: 0, y: 0, z: 0 }, face: 1 });
  }
}

function forceKitSelection(bot: Bot, slot: number): void {
  bot.on("windowOpen", (window: any) => {
    bot.clickWindow(slot, 0, 0).catch((err: any) => {
      console.log(bot.username, "kit select failed", err?.message ?? err);
    });
    setTimeout(() => {
      try {
        bot.closeWindow(window);
      } catch {
        // ignore
      }
    }, 250);
  });
}

function extractJsonText(value: any): string {
  if (value == null) {
    return "";
  }
  if (typeof value === "string") {
    try {
      return extractJsonText(JSON.parse(value));
    } catch {
      return value;
    }
  }
  if (Array.isArray(value)) {
    return value.map((entry) => extractJsonText(entry)).join("");
  }
  if (typeof value === "object") {
    const parts: string[] = [];
    for (const [key, entry] of Object.entries(value)) {
      if ((key === "text" || key === "translate" || key === "keybind" || key === "selector")
        && typeof entry === "string") {
        parts.push(entry);
        continue;
      }
      if (key === "with" || key === "extra" || key === "hoverEvent" || key === "value" || key === "contents") {
        parts.push(extractJsonText(entry));
        continue;
      }
      if (typeof entry === "object") {
        parts.push(extractJsonText(entry));
      }
    }
    return parts.join("");
  }
  return String(value);
}

function getHotbarItem(bot: Bot, hotbarSlot: number): any {
  const slot = 36 + hotbarSlot;
  return bot.inventory?.slots?.[slot] ?? null;
}

function itemLabel(item: any): string {
  if (!item) {
    return "none";
  }
  const customName =
    item?.nbt?.value?.display?.value?.Name?.value
    ?? item?.nbt?.value?.display?.value?.Name
    ?? item?.nbt?.value?.display?.Name?.value
    ?? item?.nbt?.value?.display?.Name
    ?? item?.nbt?.display?.Name?.value
    ?? item?.nbt?.display?.Name;
  const parsedCustom = extractJsonText(customName);
  if (parsedCustom.trim().length > 0) {
    return parsedCustom.trim();
  }
  if (typeof item.customName === "string" && item.customName.trim().length > 0) {
    return item.customName.trim();
  }
  if (typeof item.displayName === "string" && item.displayName.trim().length > 0) {
    return item.displayName.trim();
  }
  if (typeof item.name === "string" && item.name.trim().length > 0) {
    return item.name.trim();
  }
  return "unknown";
}

function extractCooldown(label: string): number | null {
  const match = label.match(/(\d+\.\d)\s*s/i);
  if (!match) {
    return null;
  }
  const parsed = Number(match[1]);
  return Number.isFinite(parsed) ? parsed : null;
}

function pushActionBarSample(target: Sample[], raw: any): void {
  const text = extractJsonText(raw);
  if (!text || text.trim().length === 0) {
    return;
  }
  if (!debugActionbarLogged && /cooldown/i.test(text) && !/\d+\.\d/.test(text)) {
    debugActionbarLogged = true;
    console.log("[cooldown-visual] actionbar raw sample", stringifyPacket(raw));
  }
  target.push({ t: relMs(), label: text.trim() });
}

function watchActionBar(bot: Bot, spear: boolean): void {
  (bot as any).on?.("actionBar", (jsonMsg: any) => {
    pushActionBarSample(spear ? spearActionBarSamples : scoutActionBarSamples, jsonMsg);
  });

  bot.on("message", (jsonMsg: any, position: any) => {
    const posRaw = String(position ?? "").toLowerCase();
    if (posRaw !== "game_info" && posRaw !== "2" && !posRaw.includes("action")) {
      return;
    }
    pushActionBarSample(spear ? spearActionBarSamples : scoutActionBarSamples, jsonMsg);
  });

  const client = (bot as any)._client;
  if (!client) {
    return;
  }
  client.on("set_action_bar_text", (packet: any) => {
    pushActionBarSample(spear ? spearActionBarSamples : scoutActionBarSamples, packet?.text);
  });
  client.on("system_chat", (packet: any) => {
    if (packet?.overlay !== true) {
      return;
    }
    pushActionBarSample(spear ? spearActionBarSamples : scoutActionBarSamples, packet?.content);
  });

  // Fallback for protocol/mineflayer differences: scrape packet payloads for visual strings.
  client.on("packet", (data: any, meta: any) => {
    if (!packetStreamLogged) {
      packetStreamLogged = true;
      console.log("[cooldown-visual] packet stream active", String(meta?.name ?? "unknown"));
    }
    const flattened = extractJsonText(data);
    const raw = stringifyPacket(data);
    if (!raw && !flattened) {
      return;
    }

    if (spear) {
      const nameMatches = flattened.match(/Homing Spear \(\d+\.\ds\)|Spear (Locked|Returning) \d+\.\ds/g)
        ?? raw.match(/Homing Spear \(\d+\.\ds\)|Spear (Locked|Returning) \d+\.\ds/g);
      if (nameMatches) {
        for (const label of nameMatches) {
          spearNameSamples.push({ t: relMs(), label });
        }
      }
      const actionMatches = flattened.match(/Spear cooldown:\s*\d+\.\ds|CD:\s*\d+\.\ds/g)
        ?? raw.match(/Spear cooldown:\s*\d+\.\ds|CD:\s*\d+\.\ds/g);
      if (actionMatches) {
        for (const label of actionMatches) {
          spearActionBarSamples.push({ t: relMs(), label: label.trim() });
        }
      }
      return;
    }

    const scoutNameMatches = flattened.match(/Scout Tagger \(\d+\.\ds\)/g)
      ?? raw.match(/Scout Tagger \(\d+\.\ds\)/g);
    if (scoutNameMatches) {
      for (const label of scoutNameMatches) {
        scoutNameSamples.push({ t: relMs(), label });
      }
    }
    const scoutActionMatches = flattened.match(/Cooldown:\s*\d+\.\ds/g)
      ?? raw.match(/Cooldown:\s*\d+\.\ds/g);
    if (scoutActionMatches) {
      for (const label of scoutActionMatches) {
        scoutActionBarSamples.push({ t: relMs(), label: label.trim() });
      }
    }
  });
}

function analyzeSeries(name: string, samples: Sample[]): void {
  const values = samples
    .map((sample) => extractCooldown(sample.label))
    .filter((value): value is number => value !== null);

  if (values.length < 8) {
    edgeCases.push(`${name}: insufficient cooldown samples (${values.length})`);
    return;
  }

  let increases = 0;
  for (let i = 1; i < values.length; i += 1) {
    if (values[i] > values[i - 1] + 0.11) {
      increases += 1;
    }
  }
  if (increases > 0) {
    edgeCases.push(`${name}: cooldown increased ${increases} times`);
  }

  const minimum = Math.min(...values);
  if (minimum > 0.05) {
    edgeCases.push(`${name}: never reached 0.0 (lowest ${minimum.toFixed(1)})`);
  }
}

function analyzeActionBars(name: string, samples: Sample[]): void {
  const values = samples
    .map((sample) => extractCooldown(sample.label))
    .filter((value): value is number => value !== null);

  if (values.length === 0) {
    console.log(`[cooldown-visual] WARN ${name}: no action-bar cooldown samples captured by Mineflayer`);
    return;
  }
  analyzeSeries(name, samples);
}

function beginSampling(): void {
  if (sampler) {
    return;
  }
  console.log("[cooldown-visual] begin sampling");
  logHotbar("red initial hotbar", redRanger);
  logHotbar("blue initial hotbar", blueScout);
  sampler = setInterval(() => {
    if (detectedSpearSlot == null) {
      detectedSpearSlot = findSpearSlot(redRanger);
    }
    if (detectedScoutSlot == null) {
      detectedScoutSlot = findScoutSlot(blueScout);
    }

    const spearItem = detectedSpearSlot == null ? null : getHotbarItem(redRanger, detectedSpearSlot);
    const scoutItem = detectedScoutSlot == null ? null : getHotbarItem(blueScout, detectedScoutSlot);
    spearNameSamples.push({ t: relMs(), label: itemLabel(spearItem) });
    scoutNameSamples.push({ t: relMs(), label: itemLabel(scoutItem) });
  }, 100);

  dataQueryTimer = setInterval(() => {
    const spearSlot = detectedSpearSlot ?? 2;
    const scoutSlot = detectedScoutSlot ?? 0;
    redRanger.chat(`/data get entity ${redRanger.username} Inventory[{Slot:${spearSlot}b}]`);
    redRanger.chat(`/data get entity ${blueScout.username} Inventory[{Slot:${scoutSlot}b}]`);
  }, 250);
}

function stopSampling(): void {
  if (!sampler) {
    return;
  }
  clearInterval(sampler);
  sampler = null;
  if (dataQueryTimer) {
    clearInterval(dataQueryTimer);
    dataQueryTimer = null;
  }
}

function printSampleTail(title: string, samples: Sample[]): void {
  const tail = samples.slice(-12).map((entry) => `${entry.t}ms ${entry.label}`);
  console.log(`[cooldown-visual] ${title}`);
  for (const line of tail) {
    console.log(`[cooldown-visual]   ${line}`);
  }
}

function finalizeAndExit(): void {
  stopSampling();
  console.log(`[cooldown-visual] detected slots spear=${detectedSpearSlot ?? -1} scout=${detectedScoutSlot ?? -1}`);

  analyzeSeries("spear-name", spearNameSamples);
  analyzeSeries("scout-name", scoutNameSamples);
  analyzeActionBars("spear-actionbar", spearActionBarSamples);
  analyzeActionBars("scout-actionbar", scoutActionBarSamples);

  printSampleTail("spear name tail", spearNameSamples);
  printSampleTail("scout name tail", scoutNameSamples);
  printSampleTail("spear actionbar tail", spearActionBarSamples);
  printSampleTail("scout actionbar tail", scoutActionBarSamples);

  if (edgeCases.length > 0) {
    console.log("[cooldown-visual] EDGE CASES:");
    for (const entry of edgeCases) {
      console.log(`[cooldown-visual] - ${entry}`);
    }
    for (const bot of [redRanger, blueScout]) {
      if (bot.quit) bot.quit();
    }
    registry.clearRegistry();
    process.exit(2);
    return;
  }

  console.log("[cooldown-visual] PASS no cooldown visual edge cases");
  for (const bot of [redRanger, blueScout]) {
    if (bot.quit) bot.quit();
  }
  registry.clearRegistry();
  process.exit(0);
}

function findSpearSlot(bot: Bot): number | null {
  for (let slot = 0; slot < 9; slot += 1) {
    const item = getHotbarItem(bot, slot);
    if (!item) {
      continue;
    }
    const baseName = String(item?.name ?? "").toLowerCase();
    const label = itemLabel(item).toLowerCase();
    if (baseName.includes("trident") || baseName.includes("barrier") || label.includes("spear")) {
      return slot;
    }
  }
  return null;
}

function stringifyPacket(data: any): string {
  try {
    return JSON.stringify(data);
  } catch {
    return "";
  }
}

function findScoutSlot(bot: Bot): number | null {
  for (let slot = 0; slot < 9; slot += 1) {
    const item = getHotbarItem(bot, slot);
    if (!item) {
      continue;
    }
    const baseName = String(item?.name ?? "").toLowerCase();
    const label = itemLabel(item).toLowerCase();
    if (baseName.includes("wooden_sword") || label.includes("scout")) {
      return slot;
    }
  }
  return null;
}

function logHotbar(prefix: string, bot: Bot): void {
  const entries: string[] = [];
  for (let slot = 0; slot < 9; slot += 1) {
    const item = getHotbarItem(bot, slot);
    const base = item ? String(item?.name ?? "unknown") : "none";
    const label = itemLabel(item);
    entries.push(`${slot}:${base}:${label}`);
  }
  console.log(`[cooldown-visual] ${prefix} ${entries.join(" | ")}`);
}

redRanger.once("spawn", () => {
  watchActionBar(redRanger, true);
  watchActionBar(blueScout, false);

  schedule(redRanger, 1500, () => redRanger.chat("/ctf stop"));
  schedule(redRanger, 2200, () => redRanger.chat("/ctf join red"));
  schedule(blueScout, 2600, () => blueScout.chat("/ctf join blue"));

  schedule(redRanger, 5500, () => redRanger.chat(`/tp ${redRanger.username} ${runtime.redBase.x} ${runtime.redBase.y} ${runtime.redBase.z}`));
  schedule(redRanger, 7000, () => redRanger.chat("/ctf setflag red"));
  schedule(redRanger, 8500, () => redRanger.chat(`/tp ${redRanger.username} ${runtime.blueBase.x} ${runtime.blueBase.y} ${runtime.blueBase.z}`));
  schedule(redRanger, 10000, () => redRanger.chat("/ctf setflag blue"));

  schedule(redRanger, 12000, () => redRanger.chat(`/tp ${redRanger.username} ${runtime.redBase.x} ${runtime.redBase.y} ${runtime.redBase.z}`));
  schedule(redRanger, 13400, () => redRanger.chat("/ctf setreturn red"));
  schedule(redRanger, 14800, () => redRanger.chat(`/tp ${redRanger.username} ${runtime.blueBase.x} ${runtime.blueBase.y} ${runtime.blueBase.z}`));
  schedule(redRanger, 16200, () => redRanger.chat("/ctf setreturn blue"));

  schedule(redRanger, 18200, () => redRanger.chat("/ctf start"));
});

redRanger.on("messagestr", (msg: string) => {
  const spearMatch = msg.match(/Homing Spear \(\d+\.\ds\)|Spear (Locked|Returning) \d+\.\ds/g);
  if (spearMatch) {
    for (const label of spearMatch) {
      spearNameSamples.push({ t: relMs(), label });
    }
  }
  const scoutNameMatch = msg.match(/Scout Tagger \(\d+\.\ds\)/g);
  if (scoutNameMatch) {
    for (const label of scoutNameMatch) {
      scoutNameSamples.push({ t: relMs(), label });
    }
  }
  const spearActionMatch = msg.match(/Spear cooldown:\s*\d+\.\ds|CD:\s*\d+\.\ds/g);
  if (spearActionMatch) {
    for (const label of spearActionMatch) {
      spearActionBarSamples.push({ t: relMs(), label });
    }
  }
  const scoutActionMatch = msg.match(/Cooldown:\s*\d+\.\ds/g);
  if (scoutActionMatch) {
    for (const label of scoutActionMatch) {
      scoutActionBarSamples.push({ t: relMs(), label });
    }
  }

  if (!msg.includes("CTF match started.") || matchFlowStarted) {
    return;
  }
  matchFlowStarted = true;
  setTimeout(() => beginSampling(), 1000);

  setTimeout(() => {
    const slot = detectedSpearSlot ?? findSpearSlot(redRanger) ?? 2;
    detectedSpearSlot = slot;
    redRanger.setQuickBarSlot(slot);
    swapHands(redRanger as any);
  }, 2500);
  setTimeout(() => {
    const slot = detectedSpearSlot ?? findSpearSlot(redRanger) ?? 2;
    detectedSpearSlot = slot;
    redRanger.setQuickBarSlot(slot);
    swapHands(redRanger as any);
  }, 4200);

  setTimeout(() => {
    const slot = detectedScoutSlot ?? findScoutSlot(blueScout) ?? 0;
    detectedScoutSlot = slot;
    blueScout.setQuickBarSlot(slot);
    blueScout.activateItem();
  }, 6500);
  setTimeout(() => {
    const slot = detectedScoutSlot ?? findScoutSlot(blueScout) ?? 0;
    detectedScoutSlot = slot;
    blueScout.setQuickBarSlot(slot);
    blueScout.activateItem();
  }, 7100);

  setTimeout(() => finalizeAndExit(), 39000);
});
