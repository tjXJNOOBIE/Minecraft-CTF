import type { Bot } from "mineflayer";

type ViewerFactory = (bot: Bot, options: { port: number; firstPerson: boolean; viewDistance?: number }) => void;

let cachedViewerFactory: ViewerFactory | null | undefined;

function loadViewerFactory(): ViewerFactory | null {
  if (cachedViewerFactory !== undefined) {
    return cachedViewerFactory;
  }

  try {
    // Optional dependency: this may fail if prismarine-viewer/canvas is unavailable.
    const moduleRef = require("prismarine-viewer");
    const factory = moduleRef?.mineflayer;
    cachedViewerFactory = typeof factory === "function" ? factory : null;
  } catch {
    cachedViewerFactory = null;
  }

  return cachedViewerFactory;
}

export function startBotViewer(bot: Bot, port: number): boolean {
  const factory = loadViewerFactory();
  if (!factory) {
    return false;
  }
  const firstPerson = process.env.CTF_VIEW_FIRST_PERSON !== "0";
  const viewDistanceRaw = Number(process.env.CTF_VIEW_DISTANCE || 0);
  const viewDistance = Number.isFinite(viewDistanceRaw) && viewDistanceRaw > 0 ? Math.floor(viewDistanceRaw) : undefined;
  factory(bot, { port, firstPerson, viewDistance });
  return true;
}
