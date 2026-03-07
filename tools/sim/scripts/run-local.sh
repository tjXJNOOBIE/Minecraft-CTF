#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SIM_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${SIM_DIR}/../.." && pwd)"

HOST="${CTF_SIM_HOST:-127.0.0.1}"
PORT="${CTF_SIM_PORT:-25565}"
BOTS="${CTF_SIM_BOTS:-12}"
ARENA_SIZE="${CTF_SIM_ARENA_SIZE:-75}"
SEED="${CTF_SIM_SEED:-}"
MODE="${CTF_SIM_MODE:-headless}"
VIEWER=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --host)
      HOST="${2:-}"
      shift 2
      ;;
    --port)
      PORT="${2:-}"
      shift 2
      ;;
    --bots)
      BOTS="${2:-}"
      shift 2
      ;;
    --arenaSize)
      ARENA_SIZE="${2:-}"
      shift 2
      ;;
    --seed)
      SEED="${2:-}"
      shift 2
      ;;
    --viewer)
      VIEWER=1
      shift 1
      ;;
    -h|--help)
      echo "Usage: $0 [--host <host>] [--port <port>] [--bots <bots>] [--arenaSize <50|75|100>] [--seed <seed>] [--viewer]"
      echo "Env: CTF_SIM_HOST, CTF_SIM_PORT, CTF_SIM_BOTS, CTF_SIM_ARENA_SIZE, CTF_SIM_SEED"
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      echo "Usage: $0 [--host <host>] [--port <port>] [--bots <bots>] [--arenaSize <50|75|100>] [--seed <seed>] [--viewer]" >&2
      exit 1
      ;;
  esac
done

cd "$SIM_DIR"

MODE_PATH="${REPO_ROOT}/tools/sim/src/modes/${MODE}.ts"
if [[ ! -f "$MODE_PATH" ]]; then
  echo "Unknown simulation mode: ${MODE}" >&2
  exit 1
fi

if [[ ! -d "${REPO_ROOT}/node_modules" ]]; then
  (cd "${REPO_ROOT}" && npm ci)
fi

export CTF_HOST="$HOST"
export CTF_PORT="$PORT"
export CTF_VIEW_HOST="${CTF_VIEW_HOST:-$HOST}"
export CTF_BOT_REGISTRY_PATH="${CTF_BOT_REGISTRY_PATH:-$REPO_ROOT/bot-registry.json}"
if [[ -z "${CTF_DISABLE_VIEWERS:-}" ]]; then
  if [[ "$VIEWER" == "1" ]]; then
    export CTF_DISABLE_VIEWERS="0"
  else
    export CTF_DISABLE_VIEWERS="1"
  fi
fi
if [[ -z "${CTF_VIEW_COUNT:-}" && "${CTF_DISABLE_VIEWERS}" == "1" ]]; then
  export CTF_VIEW_COUNT="0"
fi

CMD=(npx tsx "tools/sim/src/modes/${MODE}.ts" --host "$HOST" --port "$PORT" --bots "$BOTS" --arenaSize "$ARENA_SIZE")
if [[ -n "$SEED" ]]; then
  CMD+=(--seed "$SEED")
fi
if [[ "$VIEWER" == "1" ]]; then
  CMD+=(--viewer)
fi

(cd "${REPO_ROOT}" && "${CMD[@]}")
