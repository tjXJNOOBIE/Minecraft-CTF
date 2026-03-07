import { spawn } from "child_process";
import path from "path";

const serverDir = path.join(__dirname, "..", "..", "test-server");
const server = spawn("java", ["-Xms1G", "-Xmx1G", "-jar", "paper.jar", "--nogui"], { cwd: serverDir });

let started = false;
let reloaded = false;
let buffer = "";

server.stdout.on("data", (data) => {
  const text = data.toString();
  process.stdout.write(text);
  buffer += text;

  if (!started && buffer.includes("Done (")) {
    started = true;
    console.log("[reload-check] Server ready, issuing reload");
    if (server.stdin) server.stdin.write("reload\n");
    setTimeout(() => {
      if (server.stdin) server.stdin.write("stop\n");
    }, 8000);
  }

  if (started && !reloaded && text.includes("Reload complete")) {
    reloaded = true;
    console.log("[reload-check] Reload complete detected");
  }
});

server.stderr.on("data", (data) => process.stderr.write(data.toString()));

server.on("exit", (code) => {
  console.log("[reload-check] Server exited with code", code);
  process.exit(0);
});

setTimeout(() => {
  if (!started) {
    console.log("[reload-check] Server did not start in time");
    try { server.kill("SIGINT"); } catch (err) {}
  }
}, 120000);
