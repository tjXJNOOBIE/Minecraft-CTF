export function heartsTag(health: number, maxHealth = 20): string {
  const clamped = Math.max(0, Math.min(maxHealth, health));
  const ratio = maxHealth <= 0 ? 0 : clamped / maxHealth;
  const icon = ratio > 0.6 ? "❤" : ratio > 0.3 ? "♡" : "!" ;
  const buckets = Math.max(1, Math.round(ratio * 5));
  return `[${icon.repeat(buckets)}${".".repeat(Math.max(0, 5 - buckets))}]`;
}
