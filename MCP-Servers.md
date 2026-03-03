# MCP Servers (Remote Only)

This project should use **remote MCP servers only**.
Do **not** use `config.toml` for MCP interactions.

## Remote Host

- Host: `ubuntu@146.235.232.128`
- Base dir: `/srv/mcp-servers`
- Wrapper dir: `/srv/mcp-servers/bin`
- SSH key: `C:\Users\TJ\Documents\.ssh\NovusKey.key`

## Active Remote MCP Wrappers

Discovered on `2026-03-05` from `/srv/mcp-servers/bin`:

1. `ripgrep`
2. `tree-sitter`
3. `chrome-devtools`
4. `filesystem`
5. `git`
6. `qdrant`
7. `filesystem-localpc`
8. `qdrant-minecraft`

## Wrapper Behavior

- `ripgrep`: runs `npx --no-install mcp-ripgrep` in `/srv/mcp-servers/node`
- `tree-sitter`: runs `python3 -m mcp_server_tree_sitter.server`
- `chrome-devtools`: runs `npx --no-install chrome-devtools-mcp` in `/srv/mcp-servers/node`
- `filesystem`: runs `npx --no-install @modelcontextprotocol/server-filesystem /srv` in `/srv/mcp-servers/node`
- `git`: runs `uvx mcp-server-git`
- `qdrant`: runs `uvx mcp-server-qdrant` with:
  - `COLLECTION_NAME=codebases`
  - `EMBEDDING_MODEL=sentence-transformers/all-MiniLM-L6-v2`
  - `QDRANT_LOCAL_PATH=/srv/mcp-servers/vectors`
- `filesystem-localpc`: runs filesystem server scoped to the mounted local repo path
  - Allowed root: `/srv/local-pc-root/F:/workspace/Minecraft-CTF`
- `qdrant-minecraft`: runs qdrant server for Minecraft-CTF-specific indexing
  - `COLLECTION_NAME=minecraft_ctf_local`
  - `QDRANT_LOCAL_PATH=/srv/mcp-servers/vectors`

## Remote-Only Rules For MCP Calls

1. Prefer MCP tools first for search/index/git/filesystem operations.
2. Use Linux paths for MCP server arguments (for example `/srv/...`), not Windows paths (for example `F:\...`).
3. If a server exposes tools but not resources/templates, that is acceptable.
4. Keep all MCP server execution on the remote host; do not run local MCP server binaries for this repo.
5. Invoke servers directly from `/srv/mcp-servers/bin/*` over SSH.
6. Do not rely on `C:\Users\TJ\.codex\config.toml` to start or route MCP servers.

## Direct SSH Invocation

Use this pattern:

```powershell
ssh -i C:\Users\TJ\Documents\.ssh\NovusKey.key -o StrictHostKeyChecking=no ubuntu@146.235.232.128 "/srv/mcp-servers/bin/<server>"
```

Example:

```powershell
ssh -i C:\Users\TJ\Documents\.ssh\NovusKey.key -o StrictHostKeyChecking=no ubuntu@146.235.232.128 "/srv/mcp-servers/bin/ripgrep"
```

## Local-Mount Smoke Test

Run this from the repo root to verify MCP tool-call evaluation against the local-mounted project:

```powershell
python scripts/mcp_smoke.py
```

Current smoke checks:
1. `filesystem-localpc/read_text_file`
2. `ripgrep/search` (scoped to `ctf-paper/src/main/java`)
3. `tree-sitter/list_languages`
4. `qdrant-minecraft` store/find roundtrip

Known caveat on this host:
- `tree-sitter register_project_tool` on the SSHFS-mounted repo is currently very slow/hangs, so smoke uses a lightweight non-registration call.

## If A Needed MCP Server Is Missing

You approved cloning from GitHub when needed. Standard approach:

1. Clone server repo to `/srv/mcp-servers/node` (or another `/srv/mcp-servers/*` location).
2. Install dependencies on remote host.
3. Add a wrapper script in `/srv/mcp-servers/bin`.
4. Mark wrapper executable (`chmod +x /srv/mcp-servers/bin/<name>`).
5. Re-test by invoking the wrapper directly over SSH.
