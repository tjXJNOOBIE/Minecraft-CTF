import type { ArenaLayout, BotState } from "../types";
import type { Logger } from "../util/log";
import { sleep } from "../util/safety";

type AdminCommand = (command: string) => Promise<void>;

function tpCommand(botName: string, x: number, y: number, z: number): string {
  return `/tp ${botName} ${x} ${y} ${z}`;
}

export async function setupPoints(
  admin: BotState,
  layout: ArenaLayout,
  logger: Logger,
  runAdminCommand: AdminCommand
): Promise<void> {
  const doCmd = async (command: string, waitMs = 180): Promise<void> => {
    await runAdminCommand(command);
    await sleep(waitMs);
  };

  const botName = admin.username;
  logger.info("setting CTF points");

  await doCmd(tpCommand(botName, layout.redSpawn.x, layout.redSpawn.y, layout.redSpawn.z));
  await doCmd("/ctf setspawn red");
  await doCmd(tpCommand(botName, layout.redFlag.x, layout.redFlag.y, layout.redFlag.z));
  await doCmd("/ctf setflag red");
  await doCmd(tpCommand(botName, layout.redActiveReturn.x, layout.redActiveReturn.y, layout.redActiveReturn.z));
  await doCmd("/ctf setreturn red");

  await doCmd(tpCommand(botName, layout.blueSpawn.x, layout.blueSpawn.y, layout.blueSpawn.z));
  await doCmd("/ctf setspawn blue");
  await doCmd(tpCommand(botName, layout.blueFlag.x, layout.blueFlag.y, layout.blueFlag.z));
  await doCmd("/ctf setflag blue");
  await doCmd(tpCommand(botName, layout.blueActiveReturn.x, layout.blueActiveReturn.y, layout.blueActiveReturn.z));
  await doCmd("/ctf setreturn blue");

  logger.info("CTF points configured");
}
