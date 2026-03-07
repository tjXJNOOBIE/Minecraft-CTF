import http from "http";
import fs from "fs";
import { execFile, spawn } from "child_process";
import os from "os";
import path from "path";
import { promisify } from "util";

const webPort = Number(process.env.CTF_WEB_PORT || 7000);
const webHost = process.env.CTF_WEB_HOST || "127.0.0.1";
const sshHost = process.env.CTF_SSH_HOST || "ubuntu@146.235.232.128";
const sshKey = process.env.CTF_SSH_KEY || path.join(os.homedir(), "Documents", ".ssh", "NovusKey.key");
const remoteHeadlessDir = process.env.CTF_HEADLESS_DIR || "/srv/headless";
const remoteRegistryPath = process.env.CTF_REMOTE_REGISTRY_PATH || "/srv/headless/web-client/public/bot-registry.json";
const localRegistryPath = process.env.CTF_LOCAL_REGISTRY_PATH || path.join(__dirname, "..", "..", "web-client", "public", "bot-registry.json");
const sshCommand = process.platform === "win32" ? "ssh.exe" : "ssh";
const enableViewTunnels = process.env.CTF_ENABLE_VIEW_TUNNELS !== "0";
const viewBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const requestedViewCount = Number(process.env.CTF_VIEW_COUNT || 0);
const viewCountLocked = Number.isFinite(requestedViewCount) && requestedViewCount > 0;
let viewCount = viewCountLocked ? Math.floor(requestedViewCount) : 24;
const viewPublicHost = process.env.CTF_VIEW_PUBLIC_HOST || webHost;
const enableViewHealthLogs = process.env.CTF_VIEW_HEALTH_LOGS !== "0";
const headlessScript = process.env.CTF_HEADLESS_SCRIPT || "bot-testing/src/ctf-test.ts";
const configuredViewDistance = Number(process.env.CTF_VIEW_DISTANCE || 0);
const configuredViewStaggerMs = Number(process.env.CTF_VIEW_STAGGER_MS || 0);

const execFileAsync = promisify(execFile);

let tunnelProcess: ReturnType<typeof spawn> | null = null;
let syncTimer: NodeJS.Timeout | null = null;
let viewHealthTimer: NodeJS.Timeout | null = null;
let syncInFlight = false;
const lastViewHealthByUrl = new Map<string, boolean>();

function checkUrl(url: string, timeoutMs = 5000): Promise<boolean> {
  return new Promise((resolve) => {
    const req = http.get(url, (res) => {
      resolve((res.statusCode ?? 500) >= 200 && (res.statusCode ?? 500) < 400);
      res.resume();
    });
    req.on("error", () => resolve(false));
    req.setTimeout(timeoutMs, () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function ensureMultiviewReady(): Promise<void> {
  const multiviewUrl = `http://${webHost}:${webPort}/multiview.html`;
  const registryUrl = `http://${webHost}:${webPort}/bot-registry.json`;

  const multiviewOk = await checkUrl(multiviewUrl);
  const registryOk = await checkUrl(registryUrl);
  if (!multiviewOk || !registryOk) {
    throw new Error(`Multiview is not ready on ${multiviewUrl}. Keep the 7000 web server running before starting headless bots.`);
  }

  console.log(`[runner] Multiview ready: ${multiviewUrl}`);
  console.log(`[runner] Registry ready: ${registryUrl}`);
}

function parseRegistry(raw: string): any | null {
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function normalizeLocalRegistryShape(input: any): any {
  const users = Array.isArray(input?.users) ? input.users.filter((entry: any) => typeof entry === "string" && entry.trim().length > 0) : [];
  const viewUrls: Record<string, string> = {};
  const rawViews = input?.viewUrls && typeof input.viewUrls === "object" ? input.viewUrls : {};
  for (const user of users) {
    const value = rawViews[user];
    if (typeof value === "string" && value.trim().length > 0) {
      viewUrls[user] = rewriteViewUrlToLocal(value);
    }
  }
  return {
    address: typeof input?.address === "string" && input.address.length > 0 ? input.address : "146.235.232.128:25565",
    version: typeof input?.version === "string" && input.version.length > 0 ? input.version : "1.21.11",
    cols: Number(input?.cols) > 0 ? Number(input.cols) : 0,
    users,
    viewUrls
  };
}

function rewriteViewUrlToLocal(rawUrl: string): string {
  try {
    const parsed = new URL(rawUrl);
    const port = Number(parsed.port);
    const minPort = viewBasePort;
    const maxPort = viewBasePort + viewCount - 1;
    if (Number.isFinite(port) && port >= minPort && port <= maxPort) {
      parsed.protocol = "http:";
      parsed.hostname = viewPublicHost;
      return parsed.toString();
    }
    return rawUrl;
  } catch {
    return rawUrl;
  }
}

async function readRemoteRegistry(): Promise<string | null> {
  const fallback = "{\"address\":\"146.235.232.128:25565\",\"version\":\"1.21.11\",\"cols\":2,\"users\":[],\"viewUrls\":{}}";
  const command = `cat ${remoteRegistryPath} 2>/dev/null || echo '${fallback}'`;
  try {
    const { stdout } = await execFileAsync(sshCommand, [
      "-i", sshKey,
      "-o", "StrictHostKeyChecking=no",
      sshHost,
      command
    ], { maxBuffer: 1024 * 1024, timeout: 5000 });
    return stdout?.trim() || null;
  } catch {
    return null;
  }
}

function extractDesiredViewCount(input: any): number | null {
  if (!input?.viewUrls || typeof input.viewUrls !== "object") {
    return null;
  }
  let maxPort = -1;
  for (const value of Object.values(input.viewUrls)) {
    if (typeof value !== "string") {
      continue;
    }
    try {
      const parsed = new URL(value);
      const port = Number(parsed.port);
      if (Number.isFinite(port)) {
        maxPort = Math.max(maxPort, port);
      }
    } catch {
      const match = value.match(/:(\d{2,5})/);
      if (!match) continue;
      const port = Number(match[1]);
      if (Number.isFinite(port)) {
        maxPort = Math.max(maxPort, port);
      }
    }
  }
  if (maxPort < viewBasePort) {
    return null;
  }
  return Math.max(1, maxPort - viewBasePort + 1);
}

async function maybeScaleViewTunnels(input: any): Promise<void> {
  if (viewCountLocked) {
    return;
  }
  const desired = extractDesiredViewCount(input);
  if (!desired || desired <= viewCount) {
    return;
  }
  viewCount = desired;
  console.log(`[runner] Scaling view tunnels to ${viewCount} views`);
  stopViewTunnelProcess();
  await startViewTunnelProcess();
}

async function syncRemoteRegistryToLocal(): Promise<void> {
  const raw = await readRemoteRegistry();
  if (!raw) {
    return;
  }
  const parsed = parseRegistry(raw);
  if (!parsed) {
    return;
  }
  await maybeScaleViewTunnels(parsed);
  const normalized = normalizeLocalRegistryShape(parsed);
  fs.mkdirSync(path.dirname(localRegistryPath), { recursive: true });
  fs.writeFileSync(localRegistryPath, JSON.stringify(normalized, null, 2));
}

function startRegistrySyncLoop(): void {
  syncTimer = setInterval(async () => {
    if (syncInFlight) {
      return;
    }
    syncInFlight = true;
    try {
      await syncRemoteRegistryToLocal();
    } finally {
      syncInFlight = false;
    }
  }, 1000);
}

function stopRegistrySyncLoop(): void {
  if (syncTimer) {
    clearInterval(syncTimer);
    syncTimer = null;
  }
}

async function startViewTunnelProcess(): Promise<void> {
  if (!enableViewTunnels) {
    console.log("[runner] View tunnels disabled by CTF_ENABLE_VIEW_TUNNELS=0");
    return;
  }
  if (tunnelProcess) {
    return;
  }

  const forwards: string[] = [];
  for (let i = 0; i < viewCount; i += 1) {
    const port = viewBasePort + i;
    forwards.push("-L", `${port}:127.0.0.1:${port}`);
  }

  const args = [
    "-i", sshKey,
    "-o", "StrictHostKeyChecking=no",
    "-o", "ExitOnForwardFailure=yes",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=3",
    ...forwards,
    "-N",
    sshHost
  ];

  tunnelProcess = spawn(sshCommand, args, { stdio: "pipe" });
  let startupError = "";
  tunnelProcess.stderr?.on("data", (chunk) => {
    startupError += chunk.toString();
  });
  tunnelProcess.on("error", (err) => {
    console.error("[runner] View tunnel failed:", err.message);
  });
  tunnelProcess.on("exit", (code) => {
    if (code !== null && code !== 0) {
      console.error("[runner] View tunnel exited with code", code);
    }
  });

  await new Promise<void>((resolve, reject) => {
    const timer = setTimeout(() => {
      if (!tunnelProcess || tunnelProcess.killed || tunnelProcess.exitCode !== null) {
        reject(new Error("view tunnel did not stay alive"));
        return;
      }
      resolve();
    }, 1200);

    tunnelProcess?.once("exit", () => {
      clearTimeout(timer);
      reject(new Error(startupError.trim() || "view tunnel exited before startup"));
    });
  });
  console.log(`[runner] View tunnels local:${viewBasePort}-${viewBasePort + viewCount - 1} -> remote:${viewBasePort}-${viewBasePort + viewCount - 1}`);
}

function stopViewTunnelProcess(): void {
  if (!tunnelProcess) {
    return;
  }
  try {
    tunnelProcess.kill();
  } catch {
    // ignore
  }
  tunnelProcess = null;
}

function startViewHealthLoop(): void {
  if (!enableViewHealthLogs) {
    return;
  }
  viewHealthTimer = setInterval(async () => {
    let raw = "";
    try {
      raw = fs.readFileSync(localRegistryPath, "utf8");
    } catch {
      return;
    }
    const parsed = parseRegistry(raw);
    if (!parsed?.viewUrls || typeof parsed.viewUrls !== "object") {
      return;
    }
    const checks = Object.values(parsed.viewUrls).filter((value): value is string => typeof value === "string");
    for (const url of checks) {
      const ok = await checkUrl(url, 2000);
      const prev = lastViewHealthByUrl.get(url);
      if (prev === undefined || prev !== ok) {
        lastViewHealthByUrl.set(url, ok);
        console.log(`[runner] camera route ${ok ? "UP" : "DOWN"} ${url}`);
      }
    }
  }, 2000);
}

function stopViewHealthLoop(): void {
  if (viewHealthTimer) {
    clearInterval(viewHealthTimer);
    viewHealthTimer = null;
  }
  lastViewHealthByUrl.clear();
}

async function ensureRemoteViewerDependencies(): Promise<void> {
  const checkCommand = `cd ${remoteHeadlessDir} && node -e "require('prismarine-viewer');require('canvas')"`;
  try {
    await execFileAsync(sshCommand, [
      "-i", sshKey,
      "-o", "StrictHostKeyChecking=no",
      sshHost,
      checkCommand
    ], { timeout: 8000 });
    return;
  } catch {
    console.log("[runner] Installing remote viewer dependencies (prismarine-viewer, canvas)...");
  }

  const installCommand = `cd ${remoteHeadlessDir} && npm i prismarine-viewer canvas`;
  await execFileAsync(sshCommand, [
    "-i", sshKey,
    "-o", "StrictHostKeyChecking=no",
    sshHost,
    installCommand
  ], { maxBuffer: 4 * 1024 * 1024, timeout: 180000 });
}

async function main(): Promise<void> {
  await ensureMultiviewReady();
  await ensureRemoteViewerDependencies();
  await syncRemoteRegistryToLocal();
  startRegistrySyncLoop();
  await startViewTunnelProcess();
  startViewHealthLoop();
  console.log("[runner] Starting REMOTE headless CTF bot simulation");

  const viewDistance = configuredViewDistance > 0 ? Math.floor(configuredViewDistance) : (viewCount >= 16 ? 4 : 0);
  const viewStaggerMs = configuredViewStaggerMs > 0 ? Math.floor(configuredViewStaggerMs) : (viewCount >= 16 ? 200 : 0);
  const remoteEnvParts = [
    "CTF_VIEW_HOST=127.0.0.1",
    `CTF_VIEW_BASE_PORT=${viewBasePort}`,
    `CTF_BOT_REGISTRY_PATH=${remoteRegistryPath}`,
    `CTF_VIEW_COUNT=${viewCount}`
  ];
  if (viewDistance > 0) {
    remoteEnvParts.push(`CTF_VIEW_DISTANCE=${viewDistance}`);
  }
  if (viewStaggerMs > 0) {
    remoteEnvParts.push(`CTF_VIEW_STAGGER_MS=${viewStaggerMs}`);
  }
  const remoteEnv = remoteEnvParts.join(" ");
  const remoteCommand = `cd ${remoteHeadlessDir} && ${remoteEnv} npx tsx ${headlessScript}`;
  const processRef = spawn(sshCommand, [
    "-i", sshKey,
    "-o", "StrictHostKeyChecking=no",
    sshHost,
    remoteCommand
  ], { stdio: "inherit" });

  processRef.on("error", (err) => {
    stopViewHealthLoop();
    stopRegistrySyncLoop();
    stopViewTunnelProcess();
    console.error("[runner] Failed to launch remote bot script", err.message);
    process.exit(1);
  });
  processRef.on("exit", async (code) => {
    stopViewHealthLoop();
    stopRegistrySyncLoop();
    stopViewTunnelProcess();
    await syncRemoteRegistryToLocal();
    console.log("[runner] Remote bot script finished with code", code);
    process.exit(code ?? 0);
  });
}

main().catch((err) => {
  console.error("[runner]", err.message);
  process.exit(1);
});
