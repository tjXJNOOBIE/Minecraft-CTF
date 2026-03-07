import type { ArenaBuildResult, BotState } from "../types";
import type { Logger } from "../util/log";
import { sleep } from "../util/safety";

export type TeardownArenaOptions = {
  admin: BotState;
  build: ArenaBuildResult;
  logger: Logger;
  commandDelayMs: number;
};

async function restoreTrackedBlocks(
  admin: BotState,
  build: ArenaBuildResult,
  commandDelayMs: number,
  logger: Logger
): Promise<void> {
  const records = build.tracker.list().sort((a, b) => b.pos.y - a.pos.y);
  logger.info("manual teardown using tracked placements (%d blocks)", records.length);
  for (const entry of records) {
    admin.bot.chat(`/setblock ${entry.pos.x} ${entry.pos.y} ${entry.pos.z} ${entry.originalBlock}`);
    await sleep(commandDelayMs);
  }
}

export async function teardownArena50(options: TeardownArenaOptions): Promise<void> {
  const { admin, build, logger } = options;
  const spacing = Math.max(30, options.commandDelayMs);

  if (build.usedWorldEdit && build.worldEdit.available) {
    logger.info("teardown via WorldEdit region clear");
    await build.worldEdit.clearBounds(build.layout.bounds);
    return;
  }

  await restoreTrackedBlocks(admin, build, spacing, logger);
}
