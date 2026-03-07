import type { BotState, MatchPhase, MemoryModel } from "../types";
import type { SeededRandom } from "../util/random";
import { KeyedRateLimiter } from "../util/rateLimit";
import { heartsTag } from "../ui/heartsTag";

const globalChatLimiter = new KeyedRateLimiter(2, 0.06);
const perBotLimiter = new KeyedRateLimiter(1, 0.08);

function pickLine(state: BotState, memory: MemoryModel, phase: MatchPhase): string {
  const hp = heartsTag(state.bot.health ?? 20);
  const score = `${memory.score.red}-${memory.score.blue}`;
  if (phase === "overtime") {
    return `${state.username}: OT push ${hp}`;
  }
  if (state.objective?.kind === "escort") {
    return `${state.username}: escorting ${hp}`;
  }
  if (state.objective?.kind === "recover") {
    return `${state.username}: reset route ${hp}`;
  }
  if (state.objective?.kind === "hold") {
    return `${state.username}: holding base ${hp}`;
  }
  return `${state.username}: mid read ${score} ${hp}`;
}

export function maybeSendComms(
  state: BotState,
  memory: MemoryModel,
  phase: MatchPhase,
  rng: SeededRandom,
  now: number
): void {
  if (!state.online || state.isSilent || now < state.nextCommsAt) return;
  if (phase !== "match" && phase !== "overtime") return;
  if (!rng.chance(0.15)) return;

  if (!globalChatLimiter.allow("global", now)) return;
  if (!perBotLimiter.allow(state.username, now)) return;

  const msg = pickLine(state, memory, phase);
  state.bot.chat(msg);
  state.nextCommsAt = now + rng.int(18000, 32000);
}

export function assignSilenceProfile(state: BotState, rng: SeededRandom): void {
  // Small amount of silent bots keeps comms natural.
  state.isSilent = rng.chance(0.28);
}
