import type { Bot } from "mineflayer";
import type { Vec3 } from "vec3";
import type { RegionBounds } from "./util/regionBounds";
import type { SeededRandom } from "./util/random";
import type { Logger } from "./util/log";
import type { WorldEditClient } from "./util/worldedit";

export type Team = "red" | "blue";
export type MatchPhase = "bootstrap" | "lobby" | "building" | "setup" | "match" | "overtime" | "cleanup" | "done";
export type BotRole = "runner" | "escort" | "defender" | "ranged" | "flex";

export interface ArenaPalette {
  primary: string[];
  secondary: string[];
  trim: string[];
  accent: string[];
  floor: string[];
  glass: string[];
  lights: string[];
  marker: string;
}

export interface ArenaLayout {
  size: 50 | 75 | 100;
  detailLevel: "medium" | "high";
  center: Vec3;
  floorY: number;
  bounds: RegionBounds;
  redSpawn: Vec3;
  blueSpawn: Vec3;
  redFlag: Vec3;
  blueFlag: Vec3;
  redReturnPoints: [Vec3, Vec3];
  blueReturnPoints: [Vec3, Vec3];
  redActiveReturn: Vec3;
  blueActiveReturn: Vec3;
  midLane: Vec3[];
  northLane: Vec3[];
  southLane: Vec3[];
  safeInteriorRadius: number;
}

export interface PlacementRecord {
  key: string;
  pos: Vec3;
  originalBlock: string;
  placedBlock: string;
}

export interface PlacementTracker {
  track(pos: Vec3, originalBlock: string, placedBlock: string): void;
  list(): PlacementRecord[];
  count(): number;
}

export interface ArenaBuildResult {
  layout: ArenaLayout;
  tracker: PlacementTracker;
  usedWorldEdit: boolean;
  worldEdit: WorldEditClient;
}

export interface SimConfig {
  host: string;
  port: number;
  version?: string;
  bots: number;
  seedText: string;
  arenaSize: 50 | 75 | 100;
  viewer: boolean;
  viewerBasePort: number;
  viewerCount: number;
  regulationSeconds: number;
  overtimeSeconds: number;
  commandSpacingMs: number;
  debugLog: boolean;
}

export interface ObjectiveState {
  kind: "hold" | "pushFlag" | "escort" | "intercept" | "recover" | "roam" | "regroup" | "cover";
  target: Vec3;
  expiresAt: number;
  notes?: string;
}

export interface BotState {
  bot: Bot;
  username: string;
  id: number;
  team: Team;
  role: BotRole;
  isSilent: boolean;
  isLateJoiner: boolean;
  hasJoinedTeam: boolean;
  online: boolean;
  reactionMs: number;
  aimJitter: number;
  infoLagMs: number;
  tunnelVisionUntil: number;
  lastSeenEnemyAt: number;
  lastDecisionAt: number;
  nextRoleSwapAt: number;
  nextCommsAt: number;
  nextThinkingPauseAt: number;
  nextAttentionLookAt: number;
  mistakesUntil: number;
  lowHealthRetreatUntil: number;
  pushAbortAt: number;
  temporaryGrudgeUntil: number;
  grudgeTarget?: string;
  objective?: ObjectiveState;
  lastObjectiveKey?: string;
  objectiveLoopCount: number;
  misses: number;
  hits: number;
  deaths: number;
  kills: number;
  tilt: number;
  morale: number;
  spacingBias: number;
  defenderAnchor: Vec3;
  knownCarrier?: string;
}

export interface ThreatTicket {
  target: string;
  allocated: number;
}

export interface ScoreModel {
  red: number;
  blue: number;
  lastCaptureAt: number;
  byBot: Map<string, number>;
}

export interface MemoryModel {
  playersTeam: Map<string, Team>;
  grudges: Map<string, { target: string; until: number }>;
  engagedByTarget: Map<string, number>;
  score: ScoreModel;
  events: string[];
  addEvent(event: string): void;
}

export interface BotSpawnResult {
  bots: BotState[];
  admin: BotState;
  joinInitialTeams: () => Promise<void>;
  pulsePresence: (phase: MatchPhase, now: number) => void;
  disconnectAll: () => Promise<void>;
}

export interface SimContext {
  config: SimConfig;
  rng: SeededRandom;
  logger: Logger;
  phase: MatchPhase;
  arena?: ArenaBuildResult;
  memory: MemoryModel;
}
