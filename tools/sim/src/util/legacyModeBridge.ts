type ArgMap = Record<string, string | boolean>;

function readArgs(argv: string[]): ArgMap {
  const result: ArgMap = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) continue;
    const [key, inlineValue] = token.slice(2).split("=", 2);
    if (inlineValue !== undefined) {
      result[key] = inlineValue;
      continue;
    }

    const maybeValue = argv[i + 1];
    if (!maybeValue || maybeValue.startsWith("--")) {
      result[key] = true;
      continue;
    }

    result[key] = maybeValue;
    i += 1;
  }
  return result;
}

function intOrDefault(raw: string | boolean | undefined, fallback: number): number {
  if (raw === undefined || raw === true) {
    return fallback;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? Math.floor(parsed) : fallback;
}

function boolOrDefault(raw: string | boolean | undefined, fallback: boolean): boolean {
  if (raw === undefined) {
    return fallback;
  }
  if (raw === true) {
    return true;
  }
  const normalized = String(raw).trim().toLowerCase();
  if (["1", "true", "yes", "on"].includes(normalized)) {
    return true;
  }
  if (["0", "false", "no", "off"].includes(normalized)) {
    return false;
  }
  return fallback;
}

export function applyLegacyModeBridge(argv: string[]): void {
  const args = readArgs(argv);
  const host = String(args.host ?? process.env.CTF_SIM_HOST ?? process.env.CTF_HOST ?? "127.0.0.1");
  const port = intOrDefault(args.port ?? process.env.CTF_SIM_PORT ?? process.env.CTF_PORT, 25565);
  const requestedViewers = boolOrDefault(args.viewer, false);
  const existingViewCount = process.env.CTF_VIEW_COUNT;
  const parsedViewCount = existingViewCount === undefined ? undefined : Number(existingViewCount);
  const hasConfiguredViewers = Number.isFinite(parsedViewCount) && parsedViewCount > 0;

  process.env.CTF_HOST = host;
  process.env.CTF_PORT = String(port);

  if (!process.env.CTF_VIEW_HOST) {
    process.env.CTF_VIEW_HOST = host;
  }
  if (process.env.CTF_DISABLE_VIEWERS === undefined) {
    process.env.CTF_DISABLE_VIEWERS = requestedViewers || hasConfiguredViewers ? "0" : "1";
  }
  if (!process.env.CTF_VIEW_COUNT) {
    process.env.CTF_VIEW_COUNT = requestedViewers ? "1" : "0";
  }
}
