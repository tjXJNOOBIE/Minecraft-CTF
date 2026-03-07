import type { Logger } from "./log";

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function runWithCleanup(
  logger: Logger,
  run: () => Promise<void>,
  cleanup: (reason: string) => Promise<void>
): Promise<void> {
  let cleaned = false;

  const runCleanup = async (reason: string): Promise<void> => {
    if (cleaned) return;
    cleaned = true;
    try {
      logger.info("cleanup start (%s)", reason);
      await cleanup(reason);
      logger.info("cleanup complete (%s)", reason);
    } catch (err: any) {
      logger.error("cleanup failed (%s): %s", reason, err?.message ?? err);
    }
  };

  const sigHandler = async (signal: NodeJS.Signals): Promise<void> => {
    logger.warn("received %s", signal);
    await runCleanup(signal);
    process.exit(signal === "SIGINT" ? 130 : 143);
  };

  const onReject = async (err: unknown): Promise<void> => {
    logger.error("unhandled rejection: %o", err);
    await runCleanup("unhandledRejection");
    process.exit(1);
  };

  const onUncaught = async (err: unknown): Promise<void> => {
    logger.error("uncaught exception: %o", err);
    await runCleanup("uncaughtException");
    process.exit(1);
  };

  process.on("SIGINT", sigHandler);
  process.on("SIGTERM", sigHandler);
  process.on("unhandledRejection", onReject);
  process.on("uncaughtException", onUncaught);

  try {
    await run();
  } finally {
    process.off("SIGINT", sigHandler);
    process.off("SIGTERM", sigHandler);
    process.off("unhandledRejection", onReject);
    process.off("uncaughtException", onUncaught);
    await runCleanup("finally");
  }
}
