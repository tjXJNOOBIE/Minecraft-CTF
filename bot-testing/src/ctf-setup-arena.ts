import { Vec3 } from "vec3";
import { loadRuntimeConfig } from "./runtime-config";
import { ARENA, LOBBY } from "./scenario-positions";
import { createBot, createRegistry, schedule } from "./headless-utils";

const runtime = loadRuntimeConfig();
const admin = createBot("RedLeader", runtime);

const viewerHost = process.env.CTF_VIEW_HOST || runtime.host;
const viewerBasePort = Number(process.env.CTF_VIEW_BASE_PORT || 8600);
const registry = createRegistry(runtime, viewerHost, viewerBasePort);

registry.registerBot(admin, "RedLeader", 0);

process.on("exit", () => registry.clearRegistry());
process.on("SIGINT", () => {
  registry.clearRegistry();
  process.exit(0);
});

function chat(cmd: string): void {
  admin.chat(cmd);
}

function setPos1(x: number, y: number, z: number): void {
  chat(`//pos1 ${x},${y},${z}`);
}

function setPos2(x: number, y: number, z: number): void {
  chat(`//pos2 ${x},${y},${z}`);
}

admin.once("spawn", () => {
  const floorMinX = ARENA.center.x - ARENA.halfSize;
  const floorMaxX = ARENA.center.x + ARENA.halfSize;
  const floorMinZ = ARENA.center.z - ARENA.halfSize;
  const floorMaxZ = ARENA.center.z + ARENA.halfSize;

  const wallMinY = ARENA.floorY + 1;
  const wallMaxY = ARENA.floorY + ARENA.wallHeight;
  const clearMargin = ARENA.halfSize + 12;
  const clearFloor = ARENA.floorY - 8;
  const clearCeiling = wallMaxY + 12;

  schedule(admin, 1000, () => chat("/ctf stop"));

  // Clear arena volume.
  schedule(admin, 2000, () => setPos1(floorMinX - clearMargin, clearFloor, floorMinZ - clearMargin));
  schedule(admin, 2600, () => setPos2(floorMaxX + clearMargin, clearCeiling, floorMaxZ + clearMargin));
  schedule(admin, 3200, () => chat("//set air"));

  // Arena floor.
  schedule(admin, 5000, () => setPos1(floorMinX, ARENA.floorY, floorMinZ));
  schedule(admin, 5600, () => setPos2(floorMaxX, ARENA.floorY, floorMaxZ));
  schedule(admin, 6200, () => chat("//set stone"));

  // Arena walls.
  schedule(admin, 7200, () => setPos1(floorMinX, wallMinY, floorMinZ));
  schedule(admin, 7800, () => setPos2(floorMaxX, wallMaxY, floorMaxZ));
  schedule(admin, 8400, () => chat("//walls glass"));

  // Lobby platform.
  const lobbyMinX = LOBBY.center.x - LOBBY.halfSize;
  const lobbyMaxX = LOBBY.center.x + LOBBY.halfSize;
  const lobbyMinZ = LOBBY.center.z - LOBBY.halfSize;
  const lobbyMaxZ = LOBBY.center.z + LOBBY.halfSize;

  schedule(admin, 9400, () => setPos1(lobbyMinX, LOBBY.center.y - 1, lobbyMinZ));
  schedule(admin, 10000, () => setPos2(lobbyMaxX, LOBBY.center.y - 1, lobbyMaxZ));
  schedule(admin, 10600, () => chat("//set oak_planks"));
  schedule(admin, 11200, () => setPos1(lobbyMinX, LOBBY.center.y, lobbyMinZ));
  schedule(admin, 11800, () => setPos2(lobbyMaxX, LOBBY.center.y + 3, lobbyMaxZ));
  schedule(admin, 12400, () => chat("//walls glass"));

  // Set lobby spawn.
  schedule(admin, 13200, () => chat(`/tp ${admin.username} ${LOBBY.center.x} ${LOBBY.center.y} ${LOBBY.center.z}`));
  schedule(admin, 14000, () => chat("/ctf setlobby"));

  // Set red team markers.
  schedule(admin, 15000, () => chat(`/tp ${admin.username} ${ARENA.redSpawn.x} ${ARENA.redSpawn.y} ${ARENA.redSpawn.z}`));
  schedule(admin, 15600, () => chat("/ctf setspawn red"));
  schedule(admin, 16200, () => chat("/ctf setreturn red"));
  schedule(admin, 16800, () => chat(`/tp ${admin.username} ${ARENA.redFlag.x} ${ARENA.redFlag.y} ${ARENA.redFlag.z}`));
  schedule(admin, 17400, () => chat("/ctf setflag red"));

  // Set blue team markers.
  schedule(admin, 18400, () => chat(`/tp ${admin.username} ${ARENA.blueSpawn.x} ${ARENA.blueSpawn.y} ${ARENA.blueSpawn.z}`));
  schedule(admin, 19000, () => chat("/ctf setspawn blue"));
  schedule(admin, 19600, () => chat("/ctf setreturn blue"));
  schedule(admin, 20200, () => chat(`/tp ${admin.username} ${ARENA.blueFlag.x} ${ARENA.blueFlag.y} ${ARENA.blueFlag.z}`));
  schedule(admin, 20800, () => chat("/ctf setflag blue"));

  schedule(admin, 23000, () => {
    console.log("[setup] arena build complete");
    if (admin.quit) admin.quit();
    registry.clearRegistry();
    process.exit(0);
  });
});
