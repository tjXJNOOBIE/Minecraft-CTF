import { goals } from "mineflayer-pathfinder";
import { Vec3 } from "vec3";
import type { ArenaLayout, BotState, MatchPhase, MemoryModel, Team } from "../types";
import type { SeededRandom } from "../util/random";
import { containsPoint } from "../util/regionBounds";
import { reserveThreatSlot } from "./memory";
import { responderCapForTarget } from "./roles";

const { GoalNear } = goals;

type Candidate = {
  username: string;
  team?: Team;
  isHuman: boolean;
  position: Vec3;
  distance: number;
  score: number;
};

function lineDistance(a: Vec3, b: Vec3, p: Vec3): number {
  const ab = b.minus(a);
  const ap = p.minus(a);
  const t = Math.max(0, Math.min(1, ap.dot(ab) / Math.max(0.0001, ab.dot(ab))));
  const proj = a.plus(ab.scaled(t));
  return proj.distanceTo(p);
}

function teammateBlocksShot(state: BotState, teammates: BotState[], targetPos: Vec3): boolean {
  const self = state.bot.entity?.position;
  if (!self) return false;
  for (const mate of teammates) {
    if (mate.username === state.username) continue;
    const pos = mate.bot.entity?.position;
    if (!pos) continue;
    if (lineDistance(self, targetPos, pos) <= 1.1) {
      const mateDist = self.distanceTo(pos);
      const targetDist = self.distanceTo(targetPos);
      if (mateDist < targetDist) return true;
    }
  }
  return false;
}

function detectItemSlot(state: BotState, matcher: (name: string) => boolean): number {
  const items = state.bot.inventory.items();
  const found = items.find((item) => matcher(item.name));
  if (!found) return -1;
  const hotbar = found.slot - 36;
  return hotbar >= 0 && hotbar <= 8 ? hotbar : -1;
}

function maybeEat(state: BotState, rng: SeededRandom): void {
  const health = state.bot.health ?? 20;
  const food = state.bot.food ?? 20;
  if (health > 10 && food > 14) return;
  if (!rng.chance(0.5)) return;

  const slot = detectItemSlot(state, (name) => name.includes("bread") || name.includes("cooked") || name.includes("apple"));
  if (slot < 0) return;
  try {
    state.bot.setQuickBarSlot(slot);
    state.bot.activateItem();
    setTimeout(() => state.bot.deactivateItem(), rng.int(500, 1100));
  } catch {
    // ignore
  }
}

function maybeUseProjectile(state: BotState, target: Candidate, teammates: BotState[], rng: SeededRandom): boolean {
  if (target.distance < 5 || target.distance > 17) return false;
  if (teammateBlocksShot(state, teammates, target.position)) return false;

  const snowballSlot = detectItemSlot(state, (name) => name.includes("snowball"));
  if (snowballSlot >= 0 && rng.chance(0.32)) {
    try {
      state.bot.setQuickBarSlot(snowballSlot);
      state.bot.activateItem();
      state.hits += rng.chance(0.36 + Math.max(0, 0.2 - state.aimJitter)) ? 1 : 0;
      if (!rng.chance(0.38)) state.misses += 1;
      return true;
    } catch {
      return false;
    }
  }

  const tridentSlot = detectItemSlot(state, (name) => name.includes("trident") || name.includes("spear"));
  if (tridentSlot >= 0 && target.distance >= 4 && target.distance <= 12 && rng.chance(0.22)) {
    try {
      state.bot.setQuickBarSlot(tridentSlot);
      state.bot.activateItem();
      setTimeout(() => state.bot.deactivateItem(), rng.int(420, 860));
      return true;
    } catch {
      return false;
    }
  }

  // Opportunistic utility: keep crossbow loaded and occasionally fire at medium range.
  const crossbowSlot = detectItemSlot(state, (name) => name.includes("crossbow"));
  if (crossbowSlot >= 0 && target.distance >= 7 && target.distance <= 20 && rng.chance(0.14)) {
    try {
      state.bot.setQuickBarSlot(crossbowSlot);
      state.bot.activateItem();
      setTimeout(() => state.bot.deactivateItem(), rng.int(380, 720));
      return true;
    } catch {
      return false;
    }
  }

  return false;
}

function maybeSwapMeleeWeapon(state: BotState): void {
  const weaponSlot = detectItemSlot(state, (name) => name.includes("sword") || name.includes("axe"));
  if (weaponSlot < 0) return;
  try {
    state.bot.setQuickBarSlot(weaponSlot);
  } catch {
    // ignore
  }
}

function scoreCandidate(
  state: BotState,
  targetName: string,
  targetTeam: Team | undefined,
  isHuman: boolean,
  distance: number,
  targetPos: Vec3,
  arena: ArenaLayout,
  rng: SeededRandom
): number {
  const closeScore = Math.max(0, 16 - distance);
  const laneScore = Math.max(0, 8 - Math.abs(targetPos.z - arena.center.z) * 0.2);
  const roleBias = state.role === "defender" ? (distance <= 12 ? 3 : -2) : 0;
  const humanBias = isHuman ? -1.5 : 0;
  const grudgeBias = state.grudgeTarget === targetName ? 4 : 0;
  const tunnelBias = Date.now() < state.tunnelVisionUntil && state.grudgeTarget === targetName ? 2.6 : 0;
  const objectiveBias = state.objective?.kind === "intercept" ? 1.8 : 0;
  const noise = rng.jitter(0, 0.9);
  if (targetTeam && targetTeam === state.team) return -999;
  return closeScore + laneScore + roleBias + grudgeBias + tunnelBias + objectiveBias + humanBias + noise;
}

function collectCandidates(
  state: BotState,
  allBots: BotState[],
  memory: MemoryModel,
  arena: ArenaLayout,
  rng: SeededRandom
): Candidate[] {
  const list: Candidate[] = [];
  const localPlayers = state.bot.players;
  const botTeams = new Map(allBots.map((entry) => [entry.username, entry.team] as const));

  for (const [username, playerInfo] of Object.entries(localPlayers)) {
    const entity = playerInfo?.entity;
    if (!entity || username === state.username) continue;
    const pos = entity.position.floored();
    if (!containsPoint(arena.bounds, pos, 0)) continue;

    const knownTeam = botTeams.get(username) ?? memory.playersTeam.get(username);
    const sameTeam = knownTeam === state.team;
    if (sameTeam) continue;

    const isHuman = !botTeams.has(username);
    const distance = state.bot.entity ? state.bot.entity.position.distanceTo(entity.position) : 999;
    if (distance > 28) continue;

    const score = scoreCandidate(state, username, knownTeam, isHuman, distance, pos, arena, rng);
    list.push({
      username,
      team: knownTeam,
      isHuman,
      position: pos,
      distance,
      score
    });
  }

  return list.sort((a, b) => b.score - a.score);
}

function holdFireAndReposition(state: BotState, candidate: Candidate, rng: SeededRandom): void {
  const jitter = new Vec3(rng.int(-3, 3), 0, rng.int(-3, 3));
  const fallback = candidate.position.offset(jitter.x, 0, jitter.z);
  state.bot.pathfinder.setGoal(new GoalNear(fallback.x, fallback.y, fallback.z, 2), false);
  state.bot.setControlState("sprint", rng.chance(0.4));
}

export function runCombatTick(
  bots: BotState[],
  arena: ArenaLayout,
  memory: MemoryModel,
  phase: MatchPhase,
  rng: SeededRandom,
  now: number
): void {
  for (const state of bots) {
    if (!state.online || !state.hasJoinedTeam || !state.bot.entity) continue;
    if (phase !== "match" && phase !== "overtime") continue;

    const teammates = bots.filter((entry) => entry.team === state.team && entry.online);
    const candidates = collectCandidates(state, bots, memory, arena, rng);
    if (candidates.length === 0) {
      continue;
    }

    const target = candidates[0];
    const respondersCap = responderCapForTarget(teammates.length, target.isHuman, phase);
    if (!reserveThreatSlot(memory, target.username, respondersCap)) {
      continue;
    }

    const justSaw = now - state.lastSeenEnemyAt < state.reactionMs;
    if (justSaw) {
      // Human reaction delay + info lag.
      continue;
    }
    state.lastSeenEnemyAt = now;
    state.grudgeTarget = target.username;
    if (rng.chance(0.2)) {
      state.tunnelVisionUntil = now + rng.int(1800, 4600);
    }
    state.temporaryGrudgeUntil = now + rng.int(12000, 28000);

    if ((state.bot.health ?? 20) <= 6) {
      state.lowHealthRetreatUntil = now + rng.int(2400, 5200);
      continue;
    }

    maybeEat(state, rng);
    maybeSwapMeleeWeapon(state);

    // Opportunistic utility pickup when safe.
    if (rng.chance(0.08)) {
      const loot = state.bot.nearestEntity((entity) => {
        if (!entity || entity.name !== "item") return false;
        const pos = entity.position.floored();
        return containsPoint(arena.bounds, pos, 0) && state.bot.entity!.position.distanceTo(entity.position) <= 5;
      });
      if (loot) {
        state.bot.pathfinder.setGoal(new GoalNear(loot.position.x, loot.position.y, loot.position.z, 1), false);
      }
    }

    // Mistake recovery: bad push then abort and regroup.
    if (rng.chance(0.07) && state.pushAbortAt < now) {
      state.pushAbortAt = now + rng.int(2000, 4000);
      state.bot.setControlState("sprint", true);
      state.bot.pathfinder.setGoal(new GoalNear(target.position.x, target.position.y, target.position.z, 1), false);
      continue;
    }
    if (state.pushAbortAt > now) {
      const regroup = state.defenderAnchor.offset(rng.int(-2, 2), 0, rng.int(-2, 2));
      state.bot.pathfinder.setGoal(new GoalNear(regroup.x, regroup.y, regroup.z, 2), false);
      continue;
    }

    const jitterY = rng.jitter(1.45, state.aimJitter);
    const lookAt = target.position.offset(rng.jitter(0, state.aimJitter), jitterY, rng.jitter(0, state.aimJitter));
    state.bot.lookAt(lookAt, true).catch(() => undefined);

    if (maybeUseProjectile(state, target, teammates, rng)) {
      continue;
    }

    if (target.distance <= 3.2) {
      try {
        const targetEntity = state.bot.players[target.username]?.entity;
        if (targetEntity && containsPoint(arena.bounds, targetEntity.position.floored(), 0)) {
          state.bot.attack(targetEntity);
          state.hits += rng.chance(0.48 + Math.max(0, 0.18 - state.aimJitter)) ? 1 : 0;
          if (!rng.chance(0.52)) state.misses += 1;
        }
      } catch {
        state.misses += 1;
      }
      state.bot.setControlState("left", rng.chance(0.45));
      state.bot.setControlState("right", rng.chance(0.45));
      state.bot.setControlState("jump", rng.chance(0.18));
      state.tilt = Math.max(-0.8, state.tilt - 0.02);
    } else {
      state.bot.setControlState("sprint", rng.chance(0.62));
      state.bot.pathfinder.setGoal(new GoalNear(target.position.x, target.position.y, target.position.z, 2), false);
      state.tilt = Math.min(1.4, state.tilt + 0.01);
    }

    // Micro-coordination: brief support when nearby teammates are skirmishing same target.
    const nearbySupporters = teammates.filter((mate) => {
      if (!mate.bot.entity || mate.username === state.username) return false;
      return mate.bot.entity.position.distanceTo(target.position) <= 6;
    }).length;
    if (nearbySupporters >= 1 && rng.chance(0.24)) {
      state.bot.pathfinder.setGoal(new GoalNear(target.position.x, target.position.y, target.position.z, 1), false);
    }

    // Hot/cold aim: stop whiffing and reposition.
    if (state.misses >= 4) {
      holdFireAndReposition(state, target, rng);
      state.misses = 0;
      state.hits = Math.max(0, state.hits - 1);
    }
  }
}
