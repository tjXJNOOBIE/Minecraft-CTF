import { Vec3 } from "vec3";
import type { ArenaLayout, BotState, MatchPhase, MemoryModel, ObjectiveState, Team } from "../types";
import type { SeededRandom } from "../util/random";

function teamSign(team: Team): number {
  return team === "red" ? -1 : 1;
}

function ownSpawn(arena: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? arena.redSpawn : arena.blueSpawn;
}

function ownFlag(arena: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? arena.redFlag : arena.blueFlag;
}

function ownReturn(arena: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? arena.redActiveReturn : arena.blueActiveReturn;
}

function enemyFlag(arena: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? arena.blueFlag : arena.redFlag;
}

function enemyReturn(arena: ArenaLayout, team: Team): Vec3 {
  return team === "red" ? arena.blueActiveReturn : arena.redActiveReturn;
}

function laneAnchor(arena: ArenaLayout, lane: "mid" | "north" | "south", team: Team, depth = 0.5): Vec3 {
  const path = lane === "mid" ? arena.midLane : lane === "north" ? arena.northLane : arena.southLane;
  const idx = Math.max(0, Math.min(path.length - 1, Math.floor(path.length * depth)));
  const point = path[idx];
  const xShift = Math.floor(teamSign(team) * 2);
  return point.offset(xShift, 0, 0);
}

function tieBias(memory: MemoryModel, team: Team, phase: MatchPhase): number {
  const diff = memory.score.red - memory.score.blue;
  if (phase !== "match" && phase !== "overtime") return 0;
  if (diff === 0) return 0;
  if (team === "red") return diff > 0 ? -0.22 : 0.22;
  return diff < 0 ? -0.22 : 0.22;
}

function applyLoopGuard(state: BotState, key: string): boolean {
  if (state.lastObjectiveKey === key) {
    state.objectiveLoopCount += 1;
  } else {
    state.lastObjectiveKey = key;
    state.objectiveLoopCount = 0;
  }
  return state.objectiveLoopCount >= 4;
}

export function updateObjectives(
  bots: BotState[],
  arena: ArenaLayout,
  memory: MemoryModel,
  phase: MatchPhase,
  rng: SeededRandom,
  now: number,
  regulationEndsAt: number
): void {
  const nearEnd = now >= regulationEndsAt - 45000;

  for (const state of bots) {
    if (!state.online || !state.hasJoinedTeam) continue;
    if (state.objective && state.objective.expiresAt > now && now - state.lastDecisionAt < state.infoLagMs) {
      continue;
    }

    const tieAdjust = tieBias(memory, state.team, phase);
    const moraleShift = nearEnd ? 0.18 : 0;
    const aggression = state.morale + state.tilt + tieAdjust + moraleShift;
    const egoPlay = rng.chance(0.07 + Math.max(0, aggression * 0.04));
    const wantsRegroup = state.deaths > state.kills + 2 && rng.chance(0.3);
    const lowHp = (state.bot.health ?? 20) <= 8;
    const grudge = state.grudgeTarget && state.temporaryGrudgeUntil > now ? state.grudgeTarget : undefined;

    let kind: ObjectiveState["kind"] = "roam";
    let target = laneAnchor(arena, "mid", state.team, 0.5);
    let notes = "";

    if (lowHp || now < state.lowHealthRetreatUntil) {
      kind = "recover";
      target = ownSpawn(arena, state.team);
      notes = "low-hp disengage";
    } else if (wantsRegroup) {
      kind = "regroup";
      target = ownReturn(arena, state.team);
      notes = "spawn regroup";
    } else {
      switch (state.role) {
        case "defender": {
          kind = "hold";
          target = ownFlag(arena, state.team).offset(teamSign(state.team) * 3, 0, 0);
          notes = "defender hold radius";
          if (rng.chance(0.12) && aggression > 0.65) {
            // Defender patience break only when pressure spikes.
            kind = "intercept";
            target = laneAnchor(arena, "mid", state.team, 0.44);
            notes = "defender short intercept";
          }
          break;
        }
        case "runner": {
          kind = "pushFlag";
          target = enemyFlag(arena, state.team);
          notes = "primary run";
          if (rng.chance(0.22)) {
            // Objective feint: begin near one return lane then rotate.
            const fake = rng.pick(["north", "south"] as const);
            target = laneAnchor(arena, fake, state.team, 0.58);
            notes = `feint-${fake}`;
          }
          if (aggression > 1 || egoPlay) {
            target = target.offset(teamSign(state.team) * 2, 0, rng.int(-2, 2));
            notes = `${notes}-ego`;
          }
          break;
        }
        case "escort": {
          kind = "escort";
          target = laneAnchor(arena, rng.pick(["mid", "north", "south"] as const), state.team, 0.62);
          notes = "escort stagger";
          if (rng.chance(0.22)) {
            target = target.offset(0, 0, rng.pick([-4, 4]));
            notes = "escort flank";
          }
          break;
        }
        case "ranged": {
          kind = "cover";
          target = laneAnchor(arena, rng.pick(["mid", "north", "south"] as const), state.team, 0.46);
          notes = "ranged cover";
          break;
        }
        case "flex":
        default: {
          kind = aggression > 0.55 ? "pushFlag" : "intercept";
          target = aggression > 0.55 ? enemyReturn(arena, state.team) : laneAnchor(arena, "mid", state.team, 0.52);
          notes = "flex adaptive";
          break;
        }
      }
    }

    if (grudge && rng.chance(0.5)) {
      kind = "intercept";
      notes = "grudge pressure";
    }

    // Third-party cleanup: short contest near hot lanes.
    if (rng.chance(0.09 + Math.max(0, aggression * 0.03))) {
      kind = "intercept";
      const hotLane = rng.pick(["north", "mid", "south"] as const);
      target = laneAnchor(arena, hotLane, state.team, rng.jitter(0.5, 0.2));
      notes = `third-party-${hotLane}`;
    }

    const key = `${kind}:${target.x},${target.z}`;
    const looped = applyLoopGuard(state, key);
    if (looped) {
      // Anti-stall: switch lanes and force regroup route.
      kind = "regroup";
      target = ownReturn(arena, state.team);
      notes = "anti-stall reset";
      state.objectiveLoopCount = 0;
    }

    state.objective = {
      kind,
      target,
      expiresAt: now + rng.int(2200, 5200),
      notes
    };
    state.lastDecisionAt = now;
  }
}
