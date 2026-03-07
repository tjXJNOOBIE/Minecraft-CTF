import { Vec3 } from "vec3";

export interface RegionBounds {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
  minZ: number;
  maxZ: number;
}

export function makeBounds(center: Vec3, size: number, floorY: number, height: number): RegionBounds {
  const half = Math.floor(size / 2);
  return {
    minX: center.x - half,
    maxX: center.x + half,
    minY: floorY,
    maxY: floorY + Math.max(1, height),
    minZ: center.z - half,
    maxZ: center.z + half
  };
}

export function containsPoint(bounds: RegionBounds, point: Vec3, padding = 0): boolean {
  return point.x >= bounds.minX + padding
    && point.x <= bounds.maxX - padding
    && point.y >= bounds.minY + padding
    && point.y <= bounds.maxY - padding
    && point.z >= bounds.minZ + padding
    && point.z <= bounds.maxZ - padding;
}

export function containsXZ(bounds: RegionBounds, point: Vec3, padding = 0): boolean {
  return point.x >= bounds.minX + padding
    && point.x <= bounds.maxX - padding
    && point.z >= bounds.minZ + padding
    && point.z <= bounds.maxZ - padding;
}

export function centerOf(bounds: RegionBounds): Vec3 {
  return new Vec3(
    Math.floor((bounds.minX + bounds.maxX) / 2),
    Math.floor((bounds.minY + bounds.maxY) / 2),
    Math.floor((bounds.minZ + bounds.maxZ) / 2)
  );
}

export function mirrorAcrossX(centerX: number, point: Vec3): Vec3 {
  const dx = point.x - centerX;
  return new Vec3(centerX - dx, point.y, point.z);
}

export function clampIntoBounds(bounds: RegionBounds, point: Vec3, margin = 0): Vec3 {
  const x = Math.min(bounds.maxX - margin, Math.max(bounds.minX + margin, point.x));
  const y = Math.min(bounds.maxY - margin, Math.max(bounds.minY + margin, point.y));
  const z = Math.min(bounds.maxZ - margin, Math.max(bounds.minZ + margin, point.z));
  return new Vec3(x, y, z);
}

export function distanceToEdgeXZ(bounds: RegionBounds, point: Vec3): number {
  const dx = Math.min(point.x - bounds.minX, bounds.maxX - point.x);
  const dz = Math.min(point.z - bounds.minZ, bounds.maxZ - point.z);
  return Math.min(dx, dz);
}

export function regionToWorldEditSelection(bounds: RegionBounds): { pos1: Vec3; pos2: Vec3 } {
  return {
    pos1: new Vec3(bounds.minX, bounds.minY, bounds.minZ),
    pos2: new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ)
  };
}
