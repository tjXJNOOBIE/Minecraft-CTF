import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const DEFAULT_DURATION_MS = 150 * 60 * 1000; // 2.5 hours
const durationMs = Number(process.env.CTF_LONG_HAUL_MS || DEFAULT_DURATION_MS);
const startedAt = Date.now();
const logDir = path.resolve("logs", "bots", "events", "longhaul");
const scenarios = [
  "ctf-sim-full.ts",
  "ctf-sim-cooldown-visuals.ts",
  "ctf-sim-overtime-visuals.ts",
  "ctf-edgecases.ts",
  "ctf-edgecases-2.ts",
  "ctf-edgecases-3.ts",
  "ctf-edgecases-4.ts",
  "ctf-edgecases-5.ts"
];

const issuePatterns = [
  /warn/i,
  /failed?/i,
  /exception/i,
  /error/i,
  /edge case/i
];

type RunResult = {
  scenario: string;
  round: number;
  exitCode: number;
  durationMs: number;
  logPath: string;
  matchedIssues: string[];
};

const results: RunResult[] = [];
const uniqueIssues = new Set<string>();

if (!Number.isFinite(durationMs) || durationMs <= 0) {
  throw new Error(`Invalid CTF_LONG_HAUL_MS: ${process.env.CTF_LONG_HAUL_MS}`);
}

fs.mkdirSync(logDir, { recursive: true });

function relMs(): number {
  return Date.now() - startedAt;
}

function log(msg: string): void {
  const elapsed = Math.floor(relMs() / 1000);
  console.log(`[longhaul +${elapsed}s] ${msg}`);
}

function runScenario(scenario: string, round: number): Promise<RunResult> {
  return new Promise((resolve) => {
    const scenarioStart = Date.now();
    const fileStem = scenario.replace(/\.ts$/i, "");
    const logPath = path.join(logDir, `round-${String(round).padStart(3, "0")}-${fileStem}.log`);
    const stream = fs.createWriteStream(logPath, { flags: "w" });
    const localMatches = new Set<string>();

    const child = spawn(
      "npx",
      ["tsx", `bot-testing/src/${scenario}`],
      {
        env: process.env,
        stdio: ["ignore", "pipe", "pipe"],
        shell: process.platform === "win32"
      }
    );

    const onOutput = (buffer: Buffer): void => {
      const text = buffer.toString();
      stream.write(text);
      for (const line of text.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed) {
          continue;
        }
        for (const pattern of issuePatterns) {
          if (pattern.test(trimmed)) {
            localMatches.add(trimmed);
            break;
          }
        }
      }
    };

    child.stdout.on("data", onOutput);
    child.stderr.on("data", onOutput);
    child.on("error", (err) => {
      stream.write(`[longhaul] spawn_error ${String(err)}\n`);
      stream.end();
      resolve({
        scenario,
        round,
        exitCode: 1,
        durationMs: Date.now() - scenarioStart,
        logPath,
        matchedIssues: [`spawn_error ${String(err)}`]
      });
    });
    child.on("close", (code) => {
      stream.end();
      const matchedIssues = Array.from(localMatches);
      const result: RunResult = {
        scenario,
        round,
        exitCode: code ?? 1,
        durationMs: Date.now() - scenarioStart,
        logPath,
        matchedIssues
      };
      resolve(result);
    });
  });
}

function writeSummary(): string {
  const summaryPath = path.join(logDir, "summary.txt");
  const lines: string[] = [];
  lines.push(`started_at=${new Date(startedAt).toISOString()}`);
  lines.push(`finished_at=${new Date().toISOString()}`);
  lines.push(`duration_ms=${Date.now() - startedAt}`);
  lines.push(`planned_duration_ms=${durationMs}`);
  lines.push(`runs=${results.length}`);
  lines.push("");
  lines.push("run_results:");
  for (const result of results) {
    lines.push(
      `${result.round}\t${result.scenario}\texit=${result.exitCode}\tms=${result.durationMs}\tlog=${result.logPath}`
    );
  }
  lines.push("");
  lines.push("unique_issue_lines:");
  if (uniqueIssues.size == 0) {
    lines.push("(none)");
  } else {
    for (const issue of uniqueIssues) {
      lines.push(issue);
    }
  }
  fs.writeFileSync(summaryPath, lines.join("\n"), "utf8");
  return summaryPath;
}

async function main(): Promise<void> {
  log(`start durationMs=${durationMs}`);
  let round = 0;
  let hardFailures = 0;

  while (relMs() < durationMs) {
    round += 1;
    for (const scenario of scenarios) {
      if (relMs() >= durationMs) {
        break;
      }

      log(`round=${round} scenario=${scenario} begin`);
      const result = await runScenario(scenario, round);
      results.push(result);
      if (result.exitCode != 0) {
        hardFailures += 1;
      }

      for (const issue of result.matchedIssues) {
        uniqueIssues.add(`[${scenario}] ${issue}`);
      }

      log(
        `round=${round} scenario=${scenario} exit=${result.exitCode} ms=${result.durationMs} issues=${result.matchedIssues.length}`
      );
    }
  }

  const summaryPath = writeSummary();
  log(`complete runs=${results.length} uniqueIssues=${uniqueIssues.size} summary=${summaryPath}`);
  if (hardFailures > 0) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error("[longhaul] fatal", err);
  process.exit(1);
});
