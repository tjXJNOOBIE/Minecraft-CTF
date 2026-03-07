import type { Bot } from "mineflayer";
import { Vec3 } from "vec3";
import type { Logger } from "./log";
import { CooldownGate } from "./rateLimit";
import { sleep } from "./safety";
import type { RegionBounds } from "./regionBounds";
import { regionToWorldEditSelection } from "./regionBounds";

export interface WorldEditClient {
  readonly available: boolean;
  readonly latencyMs: number;
  chat(command: string): Promise<void>;
  setPos1(pos: Vec3): Promise<void>;
  setPos2(pos: Vec3): Promise<void>;
  set(block: string): Promise<void>;
  walls(block: string): Promise<void>;
  replace(from: string, to: string): Promise<void>;
  clearBounds(bounds: RegionBounds): Promise<void>;
}

class RealWorldEditClient implements WorldEditClient {
  public readonly available = true;
  public readonly latencyMs: number;
  private readonly gate = new CooldownGate(125);
  private chain: Promise<void> = Promise.resolve();

  constructor(
    private readonly bot: Bot,
    private readonly logger: Logger,
    commandDelayMs: number
  ) {
    this.latencyMs = Math.max(50, commandDelayMs);
  }

  chat(command: string): Promise<void> {
    this.chain = this.chain.then(async () => {
      if (!this.gate.allow("we")) {
        await sleep(125);
      }
      this.bot.chat(command);
      await sleep(this.latencyMs);
    }).catch((err) => {
      this.logger.warn("worldedit command failed: %s (%s)", command, err?.message ?? err);
    });
    return this.chain;
  }

  async setPos1(pos: Vec3): Promise<void> {
    await this.chat(`//pos1 ${pos.x},${pos.y},${pos.z}`);
  }

  async setPos2(pos: Vec3): Promise<void> {
    await this.chat(`//pos2 ${pos.x},${pos.y},${pos.z}`);
  }

  async set(block: string): Promise<void> {
    await this.chat(`//set ${block}`);
  }

  async walls(block: string): Promise<void> {
    await this.chat(`//walls ${block}`);
  }

  async replace(from: string, to: string): Promise<void> {
    await this.chat(`//replace ${from} ${to}`);
  }

  async clearBounds(bounds: RegionBounds): Promise<void> {
    const { pos1, pos2 } = regionToWorldEditSelection(bounds);
    await this.setPos1(pos1);
    await this.setPos2(pos2);
    await this.set("air");
  }
}

const unavailableClient: WorldEditClient = {
  available: false,
  latencyMs: 0,
  async chat(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  },
  async setPos1(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  },
  async setPos2(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  },
  async set(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  },
  async walls(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  },
  async replace(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  },
  async clearBounds(): Promise<void> {
    throw new Error("WorldEdit is unavailable");
  }
};

export async function detectWorldEdit(bot: Bot, logger: Logger): Promise<WorldEditClient> {
  const forced = process.env.CTF_SIM_FORCE_WORLD_EDIT;
  if (forced === "1" || forced === "true") {
    logger.info("WorldEdit forced enabled by env");
    return new RealWorldEditClient(bot, logger, 90);
  }

  const probeTimeoutMs = 2200;
  const hitPatterns = [/worldedit/i, /fawe/i, /\/\/help/i, /unknown command/i];
  let resolved = false;
  let hasSignal = false;

  await new Promise<void>((resolve) => {
    const onMessage = (msg: string): void => {
      if (hitPatterns.some((pattern) => pattern.test(msg))) {
        hasSignal = !/unknown command/i.test(msg);
        resolved = true;
        cleanup();
        resolve();
      }
    };

    const cleanup = (): void => {
      bot.off("messagestr", onMessage);
    };

    bot.on("messagestr", onMessage);
    bot.chat("//help");

    setTimeout(() => {
      if (!resolved) {
        cleanup();
        resolve();
      }
    }, probeTimeoutMs);
  });

  if (hasSignal) {
    logger.info("WorldEdit detected");
    return new RealWorldEditClient(bot, logger, 90);
  }

  logger.info("WorldEdit not detected; using manual block placement");
  return unavailableClient;
}
