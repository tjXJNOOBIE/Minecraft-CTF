# RUN_HEADLESS_TEST

- Shot 01: Run this first - verify multiview server on `7000` is live.
  - `Invoke-WebRequest -UseBasicParsing http://localhost:7000/multiview.html | Select-Object -ExpandProperty StatusCode`
  - `Invoke-WebRequest -UseBasicParsing http://localhost:7000/bot-registry.json | Select-Object -ExpandProperty StatusCode`

- Shot 02: Ensure `7001` is not in use.
  - `Get-NetTCPConnection -LocalPort 7001 -State Listen -ErrorAction SilentlyContinue`
  - If found: `Stop-Process -Id <PID> -Force`

- Shot 03: Keep multiview page open before bots.
  - `http://localhost:7000/multiview.html`

- Shot 04: Confirm bot runtime defaults are remote.
  - Host: `146.235.232.128`
  - Port: `25565`
  - Version: `1.21.11`
  - Override only with env vars: `CTF_HOST`, `CTF_PORT`, `CTF_VERSION`

- Shot 05: Run headless simulation (remote-targeted bots).
  - `npm run headless`

- Shot 06: Confirm bots are publishing to multiview registry while running.
  - `Get-Content web-client/public/bot-registry.json`
  - Expected `users` list to include active bot names.

- Shot 07: If registry has users but page is stuck loading, validate address/version.
  - Address should be `146.235.232.128:25565`
  - Version should be `1.21.11`

- Shot 08: Build plugin before deploy.
  - `./gradlew :ctf-paper:build` (or `gradlew.bat :ctf-paper:build` on Windows)

- Shot 09: Remove non-CTF plugins from server plugin directory before restart/reload tests.
  - Keep only `ctf.jar` (and required plugin data folder for CTF if present).

- Shot 10: Deploy updated config defaults.
  - Use `ctf-config.yml` values for team spawns, return points, and flag bases.

- Shot 11: Deploy jar and config to remote server paths (`/srv/speed` / plugin config folder), then restart remote server.

- Shot 12: Re-run Shot 01 -> Shot 06 to validate end-to-end visuals and gameplay logs.
