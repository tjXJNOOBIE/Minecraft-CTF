export type CtfRuntimeConfig = {
  host: string;
  port: number;
  version: string;
  redBase: { x: number; y: number; z: number };
  blueBase: { x: number; y: number; z: number };
};

function intFromEnv(name: string, fallback: number): number {
  const raw = process.env[name];
  if (!raw) return fallback;
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function loadRuntimeConfig(): CtfRuntimeConfig {
  return {
    host: process.env.CTF_HOST || "146.235.232.128",
    port: intFromEnv("CTF_PORT", 25565),
    version: process.env.CTF_VERSION || "1.21.11",
    redBase: {
      x: intFromEnv("CTF_RED_BASE_X", 2),
      y: intFromEnv("CTF_RED_BASE_Y", 64),
      z: intFromEnv("CTF_RED_BASE_Z", 2)
    },
    blueBase: {
      x: intFromEnv("CTF_BLUE_BASE_X", 12),
      y: intFromEnv("CTF_BLUE_BASE_Y", 64),
      z: intFromEnv("CTF_BLUE_BASE_Z", 2)
    }
  };
}

export function addressOf(config: CtfRuntimeConfig): string {
  return `${config.host}:${config.port}`;
}
