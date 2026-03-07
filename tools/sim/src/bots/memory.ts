import type { BotState, MemoryModel, Team } from "../types";

const MAX_EVENTS = 160;

function normalizeName(name: string): string {
  return name.trim();
}

export function createMemoryModel(): MemoryModel {
  const model: MemoryModel = {
    playersTeam: new Map<string, Team>(),
    grudges: new Map<string, { target: string; until: number }>(),
    engagedByTarget: new Map<string, number>(),
    score: {
      red: 0,
      blue: 0,
      lastCaptureAt: 0,
      byBot: new Map<string, number>()
    },
    events: [],
    addEvent(event: string): void {
      this.events.push(event);
      if (this.events.length > MAX_EVENTS) {
        this.events.splice(0, this.events.length - MAX_EVENTS);
      }
    }
  };
  return model;
}

export function observeServerMessage(memory: MemoryModel, raw: string, now = Date.now()): void {
  const msg = raw.toLowerCase();

  const joinMatch = raw.match(/([A-Za-z0-9_]+)\s+joined\s+(red|blue)/i);
  if (joinMatch) {
    const player = normalizeName(joinMatch[1]);
    const team = joinMatch[2].toLowerCase() as Team;
    memory.playersTeam.set(player, team);
    memory.addEvent(`${player} joined ${team}`);
  }

  const leaveMatch = raw.match(/([A-Za-z0-9_]+)\s+left\s+(red|blue)/i);
  if (leaveMatch) {
    const player = normalizeName(leaveMatch[1]);
    memory.playersTeam.delete(player);
    memory.addEvent(`${player} left team`);
  }

  const captureMatch = raw.match(/(red|blue)\s+(?:team\s+)?(?:captured|scored)/i);
  if (captureMatch) {
    const team = captureMatch[1].toLowerCase() as Team;
    if (team === "red") memory.score.red += 1;
    if (team === "blue") memory.score.blue += 1;
    memory.score.lastCaptureAt = now;
    memory.addEvent(`capture:${team}`);
  }

  const scoreLine = raw.match(/score.*red\D+(\d+)\D+blue\D+(\d+)/i);
  if (scoreLine) {
    memory.score.red = Number(scoreLine[1]);
    memory.score.blue = Number(scoreLine[2]);
    memory.addEvent(`score:${memory.score.red}-${memory.score.blue}`);
  }

  if (msg.includes("overtime")) {
    memory.addEvent("overtime");
  }
}

export function setPlayerTeam(memory: MemoryModel, player: string, team: Team): void {
  memory.playersTeam.set(normalizeName(player), team);
}

export function getPlayerTeam(memory: MemoryModel, player: string): Team | undefined {
  return memory.playersTeam.get(normalizeName(player));
}

export function addGrudge(memory: MemoryModel, source: string, target: string, now: number, durationMs = 30000): void {
  memory.grudges.set(normalizeName(source), { target: normalizeName(target), until: now + durationMs });
}

export function readGrudge(memory: MemoryModel, source: string, now: number): string | undefined {
  const entry = memory.grudges.get(normalizeName(source));
  if (!entry) return undefined;
  if (entry.until < now) {
    memory.grudges.delete(normalizeName(source));
    return undefined;
  }
  return entry.target;
}

export function releaseExpired(memory: MemoryModel, now = Date.now()): void {
  for (const [source, entry] of memory.grudges.entries()) {
    if (entry.until < now) {
      memory.grudges.delete(source);
    }
  }
}

export function clearThreatAllocations(memory: MemoryModel): void {
  memory.engagedByTarget.clear();
}

export function reserveThreatSlot(memory: MemoryModel, target: string, cap: number): boolean {
  const current = memory.engagedByTarget.get(target) ?? 0;
  if (current >= cap) return false;
  memory.engagedByTarget.set(target, current + 1);
  return true;
}

export function reportKill(memory: MemoryModel, killer: BotState, victimName: string): void {
  killer.kills += 1;
  memory.score.byBot.set(killer.username, (memory.score.byBot.get(killer.username) ?? 0) + 1);
  memory.addEvent(`kill:${killer.username}->${victimName}`);
}

export function reportDeath(memory: MemoryModel, state: BotState, now = Date.now()): void {
  state.deaths += 1;
  state.tilt = Math.min(1.4, state.tilt + 0.14);
  memory.addEvent(`death:${state.username}`);
  const grudge = readGrudge(memory, state.username, now);
  if (!grudge) {
    const latest = [...memory.events].reverse().find((event) => event.startsWith("kill:"));
    if (latest) {
      const killedBy = latest.split("->")[0].replace("kill:", "");
      addGrudge(memory, state.username, killedBy, now, 26000);
    }
  }
}
