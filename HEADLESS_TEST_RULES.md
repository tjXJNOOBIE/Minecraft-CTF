# Headless Test Rules

- Use the existing web/multiview server on port `7000` only.
- Do not run local Paper headless servers for this flow; target the remote CTF server.
- Before bots, verify `http://localhost:7000/`, `http://localhost:7000/multiview.html`, and `http://localhost:7000/bot-registry.json` return `200`.
- Port `7000` must be used. 
- Keep `http://localhost:7000/multiview.html` open before starting bots so visuals can be observed live.
- Start bots with `npm run headless` (runs `bot-testing/src/run-headless.ts`).
- In local dashboard mode, `run-headless.ts` mirrors remote registry data into local `web-client/public/bot-registry.json`.
- In local dashboard mode, `run-headless.ts` opens SSH local-forward tunnels for camera ports `8600-8605` by default.
- `run-headless.ts` must use strict tunnel startup (`ExitOnForwardFailure`) so camera route bind failures fail fast.
- `run-headless.ts` rewrites remote camera URLs to local-facing hosts for browser access (`CTF_VIEW_PUBLIC_HOST`, default `CTF_WEB_HOST`).
- Keep local ports `8600-8605` free so bot camera view routes can bind correctly through tunnels.
- Bot defaults must point to remote server `146.235.232.128:25565` unless env vars override.
- Registry writes must target `web-client/public/bot-registry.json` so multiview tiles populate.
- Confirm registry users become non-empty while bots are active.
- If bots connect but multiview stays loading, verify address/version in registry match remote server.
