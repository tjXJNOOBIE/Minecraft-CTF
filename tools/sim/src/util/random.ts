export type SeedInput = string | number | undefined;

export interface SeededRandom {
  readonly seed: number;
  readonly seedText: string;
  float(): number;
  int(minInclusive: number, maxInclusive: number): number;
  chance(probability: number): boolean;
  pick<T>(items: readonly T[]): T;
  pickWeighted<T>(items: readonly T[], weights: readonly number[]): T;
  shuffle<T>(items: readonly T[]): T[];
  jitter(base: number, spread: number): number;
  gaussian(mean?: number, stdDev?: number): number;
}

function hashSeed(text: string): number {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < text.length; i += 1) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

function mulberry32(seed: number): () => number {
  let t = seed >>> 0;
  return () => {
    t += 0x6d2b79f5;
    let r = Math.imul(t ^ (t >>> 15), 1 | t);
    r ^= r + Math.imul(r ^ (r >>> 7), 61 | r);
    return ((r ^ (r >>> 14)) >>> 0) / 4294967296;
  };
}

function asSeed(input: SeedInput): { seed: number; seedText: string } {
  if (typeof input === "number" && Number.isFinite(input)) {
    const normalized = Math.floor(Math.abs(input)) >>> 0;
    return { seed: normalized, seedText: String(normalized) };
  }
  const seedText = (typeof input === "string" && input.trim().length > 0)
    ? input.trim()
    : String(Date.now());
  return { seed: hashSeed(seedText), seedText };
}

export function createSeededRandom(seedInput: SeedInput): SeededRandom {
  const { seed, seedText } = asSeed(seedInput);
  const next = mulberry32(seed);
  let gaussianSpare: number | null = null;

  return {
    seed,
    seedText,
    float(): number {
      return next();
    },
    int(minInclusive: number, maxInclusive: number): number {
      const lo = Math.min(minInclusive, maxInclusive);
      const hi = Math.max(minInclusive, maxInclusive);
      const span = hi - lo + 1;
      return lo + Math.floor(next() * span);
    },
    chance(probability: number): boolean {
      if (probability <= 0) return false;
      if (probability >= 1) return true;
      return next() < probability;
    },
    pick<T>(items: readonly T[]): T {
      if (items.length === 0) {
        throw new Error("pick() requires at least one item");
      }
      return items[Math.floor(next() * items.length)];
    },
    pickWeighted<T>(items: readonly T[], weights: readonly number[]): T {
      if (items.length === 0 || items.length !== weights.length) {
        throw new Error("pickWeighted() requires equal non-empty arrays");
      }
      const total = weights.reduce((sum, weight) => sum + Math.max(0, weight), 0);
      if (total <= 0) {
        return this.pick(items);
      }
      let roll = next() * total;
      for (let i = 0; i < items.length; i += 1) {
        roll -= Math.max(0, weights[i]);
        if (roll <= 0) return items[i];
      }
      return items[items.length - 1];
    },
    shuffle<T>(items: readonly T[]): T[] {
      const copy = [...items];
      for (let i = copy.length - 1; i > 0; i -= 1) {
        const j = Math.floor(next() * (i + 1));
        const tmp = copy[i];
        copy[i] = copy[j];
        copy[j] = tmp;
      }
      return copy;
    },
    jitter(base: number, spread: number): number {
      if (!Number.isFinite(spread) || spread <= 0) return base;
      return base + (next() * 2 - 1) * spread;
    },
    gaussian(mean = 0, stdDev = 1): number {
      if (gaussianSpare !== null) {
        const spare = gaussianSpare;
        gaussianSpare = null;
        return mean + spare * stdDev;
      }
      let u = 0;
      let v = 0;
      while (u === 0) u = next();
      while (v === 0) v = next();
      const mag = Math.sqrt(-2.0 * Math.log(u));
      const z0 = mag * Math.cos(2 * Math.PI * v);
      const z1 = mag * Math.sin(2 * Math.PI * v);
      gaussianSpare = z1;
      return mean + z0 * stdDev;
    }
  };
}
