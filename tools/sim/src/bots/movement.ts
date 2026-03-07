import { goals } from "mineflayer-pathfinder";
import { Vec3 } from "vec3";
import type { ArenaLayout, BotState, MatchPhase } from "../types";
import type { SeededRandom } from "../util/random";
import { clampIntoBounds, containsXZ, distanceToEdgeXZ } from "../util/regionBounds";

const { GoalNear } = goals;

function asGoal(pos: Vec3, range: number): any {
  return new GoalNear(pos.x, pos.y, pos.z, range);
}

function stopBasicControls(state: BotState): void {
  const { bot } = state;
  bot.setControlState("forward", false);
  bot.setControlState("back", false);
  bot.setControlState("left", false);
  bot.setControlState("right", false);
  bot.setControlState("jump", false);
  bot.setControlState("sprint", false);
}

export function updateLobbyMovement(state: BotState, rng: SeededRandom, now: number): void {
  if (!state.bot.entity || !state.online) return;
  const roll = rng.float();

  if (roll < 0.18) {
    stopBasicControls(state);
    state.bot.look(state.bot.entity.yaw + rng.jitter(0, 0.8), rng.jitter(0, 0.2), true);
    return;
  }

  const strafeLeft = rng.chance(0.35);
  state.bot.setControlState("left", strafeLeft);
  state.bot.setControlState("right", !strafeLeft && rng.chance(0.35));
  state.bot.setControlState("forward", true);
  state.bot.setControlState("sprint", rng.chance(0.55));

  if (rng.chance(0.1)) {
    state.bot.setControlState("jump", true);
    setTimeout(() => state.bot.setControlState("jump", false), rng.int(90, 240));
  }

  // Rare crouch spam in lobby/cleanup to mimic idle players.
  if (rng.chance(0.03)) {
    state.bot.setControlState("sneak", true);
    setTimeout(() => state.bot.setControlState("sneak", false), rng.int(200, 700));
  }

  state.nextThinkingPauseAt = now + rng.int(900, 2800);
}

function avoidTeammateClump(state: BotState, teammates: BotState[], rng: SeededRandom): void {
  const selfPos = state.bot.entity?.position;
  if (!selfPos) return;
  const tooClose = teammates.filter((mate) => {
    if (mate.username === state.username) return false;
    const pos = mate.bot.entity?.position;
    return pos ? pos.distanceTo(selfPos) <= 2.4 : false;
  });
  if (tooClose.length >= 2) {
    state.bot.setControlState("left", rng.chance(0.5));
    state.bot.setControlState("right", rng.chance(0.5));
    state.bot.setControlState("back", rng.chance(0.25));
  }
}

function maybePauseForAwareness(state: BotState, rng: SeededRandom, now: number): boolean {
  if (now < state.nextThinkingPauseAt) return false;
  if (!rng.chance(0.22)) return false;
  stopBasicControls(state);
  state.nextThinkingPauseAt = now + rng.int(1200, 2600);
  return true;
}

export function moveByObjective(
  state: BotState,
  teammates: BotState[],
  arena: ArenaLayout,
  phase: MatchPhase,
  rng: SeededRandom,
  now: number
): void {
  if (!state.online || !state.bot.entity || !state.objective) return;
  if (phase !== "match" && phase !== "overtime") return;

  if (maybePauseForAwareness(state, rng, now)) {
    return;
  }

  const entityPos = state.bot.entity.position.floored();
  const objective = state.objective;
  let target = objective.target.floored();

  // Small pathing mistakes make movement less robotic.
  if (rng.chance(0.09)) {
    target = target.offset(rng.int(-2, 2), 0, rng.int(-2, 2));
  }

  if (!containsXZ(arena.bounds, target, 1)) {
    target = clampIntoBounds(arena.bounds, target, 2);
  }

  if (state.role === "defender") {
    const leash = Math.floor(arena.size * 0.24);
    if (state.defenderAnchor.distanceTo(target) > leash) {
      target = state.defenderAnchor.clone();
    }
  }

  // Carriers and cautious bots avoid open center lanes unless tilted.
  const edgeDist = distanceToEdgeXZ(arena.bounds, entityPos);
  const prefersWall = objective.kind === "recover"
    || objective.kind === "escort"
    || (objective.kind === "pushFlag" && state.role === "runner");
  if (prefersWall && state.tilt < 0.7 && edgeDist > 7 && rng.chance(0.55)) {
    const leftWall = new Vec3(arena.bounds.minX + 3, target.y, target.z);
    const rightWall = new Vec3(arena.bounds.maxX - 3, target.y, target.z);
    target = state.team === "red" ? leftWall : rightWall;
  }

  avoidTeammateClump(state, teammates, rng);

  // Staggered pushes for escorts: one engages, another lags/flanks.
  if (state.role === "escort") {
    const nearbyEscorts = teammates.filter((mate) => {
      if (mate.username === state.username || mate.role !== "escort") return false;
      const pos = mate.bot.entity?.position;
      return pos ? pos.distanceTo(entityPos) <= 6 : false;
    }).length;
    if (nearbyEscorts >= 1 && rng.chance(0.48)) {
      target = target.offset(state.team === "red" ? -3 : 3, 0, rng.int(-3, 3));
    }
  }

  state.bot.setControlState("sprint", rng.chance(phase === "overtime" ? 0.72 : 0.57));
  state.bot.pathfinder.setGoal(asGoal(target, rng.int(1, 3)), false);

  // Ping-pong control: short pauses behind cover-like points.
  if (rng.chance(0.12)) {
    state.bot.setControlState("sprint", false);
    state.bot.setControlState("forward", false);
  }
}

export function regroupAt(state: BotState, regroupPoint: Vec3): void {
  if (!state.online || !state.bot.entity) return;
  state.bot.pathfinder.setGoal(asGoal(regroupPoint.floored(), 2), false);
}

export function clearMovement(state: BotState): void {
  stopBasicControls(state);
  state.bot.pathfinder.setGoal(null);
}
