import type { BotRole, BotState, MatchPhase, Team } from "../types";
import type { MemoryModel } from "../types";
import type { SeededRandom } from "../util/random";

type RolePlan = Record<BotRole, number>;

function rolePlanFor(teamCount: number): RolePlan {
  const defenders = Math.max(2, Math.floor(teamCount * 0.2));
  const runners = Math.max(2, Math.floor(teamCount * 0.2));
  const ranged = Math.max(1, Math.floor(teamCount * 0.2));
  const escorts = Math.max(2, Math.floor(teamCount * 0.2));
  const assigned = defenders + runners + ranged + escorts;
  const flex = Math.max(0, teamCount - assigned);
  return { defender: defenders, runner: runners, ranged, escort: escorts, flex };
}

function nextRoleCycle(current: BotRole): BotRole {
  switch (current) {
    case "runner":
      return "escort";
    case "escort":
      return "ranged";
    case "ranged":
      return "defender";
    case "defender":
      return "flex";
    case "flex":
    default:
      return "runner";
  }
}

export function assignInitialRoles(
  bots: BotState[],
  team: Team,
  rng: SeededRandom,
  now: number
): void {
  const teamBots = rng.shuffle(bots.filter((state) => state.team === team));
  const plan = rolePlanFor(teamBots.length);

  const queue: BotRole[] = [];
  for (const [role, count] of Object.entries(plan) as Array<[BotRole, number]>) {
    for (let i = 0; i < count; i += 1) {
      queue.push(role);
    }
  }

  teamBots.forEach((state, index) => {
    state.role = queue[index] ?? "flex";
    state.nextRoleSwapAt = now + rng.int(28000, 46000);
  });
}

export function maybeSwapRoles(
  bots: BotState[],
  memory: MemoryModel,
  phase: MatchPhase,
  rng: SeededRandom,
  now: number
): void {
  if (phase !== "match" && phase !== "overtime") return;

  const redBehind = memory.score.red < memory.score.blue;
  const blueBehind = memory.score.blue < memory.score.red;

  for (const state of bots) {
    if (!state.online || now < state.nextRoleSwapAt) continue;
    const lateMatchBoost = phase === "overtime" ? 0.35 : 0.2;
    const behindBoost = state.team === "red" ? redBehind : blueBehind;
    const shouldSwap = rng.chance(behindBoost ? 0.55 : lateMatchBoost);
    if (!shouldSwap) {
      state.nextRoleSwapAt = now + rng.int(24000, 42000);
      continue;
    }

    const aggressiveBias = state.morale + (behindBoost ? 0.25 : 0);
    if (aggressiveBias > 0.6 && state.role === "defender") {
      state.role = "runner";
    } else if (aggressiveBias < -0.4 && state.role === "runner") {
      state.role = "defender";
    } else {
      state.role = nextRoleCycle(state.role);
    }
    state.nextRoleSwapAt = now + rng.int(28000, 52000);
  }
}

export function responderCapForTarget(
  attackersOnline: number,
  isHumanTarget: boolean,
  phase: MatchPhase
): number {
  const base = Math.max(1, Math.floor(attackersOnline * 0.25));
  const overtimeBoost = phase === "overtime" ? 1 : 0;
  if (isHumanTarget) {
    // Survivability: keep pressure believable but avoid full dogpile.
    return Math.max(1, Math.min(3, base + overtimeBoost - 1));
  }
  return Math.max(2, Math.min(5, base + overtimeBoost));
}

export function defenderLeashRadius(size: number): number {
  return Math.floor(size * 0.24);
}
