#!/usr/bin/env bash
set -euo pipefail

# Mirror the remote SSH details that live in remote-port.txt and open-remote-server.cmd.
KEY="${HOME}/Documents/.ssh/NovusPriKey"
HOST="ubuntu@146.235.232.128"

# Local services that the remote headless runner expects: the web UI on 7000 and viewer ports starting at 8600.
# Pick remote ports well above 25565 (the Minecraft server port reported in remote-port.txt) so you do not conflict.
REMOTE_WEB_PORT=${REMOTE_WEB_PORT:-8700}
REMOTE_VIEW_BASE=${REMOTE_VIEW_BASE:-8701}
VIEW_COUNT=${VIEW_COUNT:-4}
WEB_LOCAL_PORT=${WEB_LOCAL_PORT:-7000}
VIEW_BASE_LOCAL_PORT=${VIEW_BASE_LOCAL_PORT:-8600}

ssh_args=(
  -i "$KEY"
  -o StrictHostKeyChecking=no
  -o ExitOnForwardFailure=yes
  -o ServerAliveInterval=15
  -o ServerAliveCountMax=3
)

ssh_args+=("-R" "${REMOTE_WEB_PORT}:127.0.0.1:${WEB_LOCAL_PORT}")
for ((i = 0; i < VIEW_COUNT; i += 1)); do
  local_port=$((VIEW_BASE_LOCAL_PORT + i))
  remote_port=$((REMOTE_VIEW_BASE + i))
  ssh_args+=("-R" "${remote_port}:127.0.0.1:${local_port}")
done

ssh_args+=("$HOST")

exec ssh "${ssh_args[@]}"
