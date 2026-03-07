const SKY_CENTER_X = 200;
const SKY_CENTER_Z = 200;
const SKY_BASE_Y = 190;

export const ARENA = {
  center: { x: SKY_CENTER_X, y: SKY_BASE_Y, z: SKY_CENTER_Z },
  floorY: SKY_BASE_Y - 1,
  halfSize: 14,
  wallHeight: 6,
  redSpawn: { x: SKY_CENTER_X - 8, y: SKY_BASE_Y, z: SKY_CENTER_Z },
  blueSpawn: { x: SKY_CENTER_X + 8, y: SKY_BASE_Y, z: SKY_CENTER_Z },
  redFlag: { x: SKY_CENTER_X - 11, y: SKY_BASE_Y, z: SKY_CENTER_Z },
  blueFlag: { x: SKY_CENTER_X + 11, y: SKY_BASE_Y, z: SKY_CENTER_Z },
  redReturn: { x: SKY_CENTER_X - 10, y: SKY_BASE_Y, z: SKY_CENTER_Z + 2 },
  blueReturn: { x: SKY_CENTER_X + 10, y: SKY_BASE_Y, z: SKY_CENTER_Z + 2 }
};

export const LOBBY = {
  center: { x: SKY_CENTER_X, y: SKY_BASE_Y + 10, z: SKY_CENTER_Z + 35 },
  halfSize: 6
};
