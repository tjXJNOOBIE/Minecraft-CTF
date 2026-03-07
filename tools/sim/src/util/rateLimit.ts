type Bucket = {
  tokens: number;
  updatedAt: number;
};

export class KeyedRateLimiter {
  private readonly buckets = new Map<string, Bucket>();

  constructor(
    private readonly capacity: number,
    private readonly refillPerSecond: number
  ) {}

  allow(key: string, now = Date.now(), cost = 1): boolean {
    const refillPerMs = this.refillPerSecond / 1000;
    const bucket = this.buckets.get(key) ?? { tokens: this.capacity, updatedAt: now };
    const elapsed = Math.max(0, now - bucket.updatedAt);
    bucket.tokens = Math.min(this.capacity, bucket.tokens + elapsed * refillPerMs);
    bucket.updatedAt = now;

    if (bucket.tokens < cost) {
      this.buckets.set(key, bucket);
      return false;
    }

    bucket.tokens -= cost;
    this.buckets.set(key, bucket);
    return true;
  }

  reset(key?: string): void {
    if (key) {
      this.buckets.delete(key);
      return;
    }
    this.buckets.clear();
  }
}

export class CooldownGate {
  private readonly lastAt = new Map<string, number>();

  constructor(private readonly minIntervalMs: number) {}

  allow(key: string, now = Date.now()): boolean {
    const last = this.lastAt.get(key) ?? 0;
    if (now - last < this.minIntervalMs) return false;
    this.lastAt.set(key, now);
    return true;
  }

  touch(key: string, now = Date.now()): void {
    this.lastAt.set(key, now);
  }
}
