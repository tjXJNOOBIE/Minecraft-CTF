export type LogLevel = "debug" | "info" | "warn" | "error";

const ORDER: Record<LogLevel, number> = {
  debug: 10,
  info: 20,
  warn: 30,
  error: 40
};

export interface Logger {
  level: LogLevel;
  debug(message: string, ...args: unknown[]): void;
  info(message: string, ...args: unknown[]): void;
  warn(message: string, ...args: unknown[]): void;
  error(message: string, ...args: unknown[]): void;
  child(scope: string): Logger;
}

function fmt(scope: string, level: LogLevel, message: string): string {
  const stamp = new Date().toISOString();
  return `${stamp} [sim:${scope}] [${level}] ${message}`;
}

export function createLogger(scope: string, level: LogLevel = "info"): Logger {
  const log = (msgLevel: LogLevel, message: string, args: unknown[]): void => {
    if (ORDER[msgLevel] < ORDER[level]) return;
    const line = fmt(scope, msgLevel, message);
    if (msgLevel === "warn") {
      console.warn(line, ...args);
      return;
    }
    if (msgLevel === "error") {
      console.error(line, ...args);
      return;
    }
    console.log(line, ...args);
  };

  return {
    level,
    debug(message: string, ...args: unknown[]) {
      log("debug", message, args);
    },
    info(message: string, ...args: unknown[]) {
      log("info", message, args);
    },
    warn(message: string, ...args: unknown[]) {
      log("warn", message, args);
    },
    error(message: string, ...args: unknown[]) {
      log("error", message, args);
    },
    child(nextScope: string): Logger {
      return createLogger(`${scope}/${nextScope}`, level);
    }
  };
}
