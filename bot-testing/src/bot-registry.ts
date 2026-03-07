import fs from "fs";
import path from "path";

const defaultRegistryPath = path.join(__dirname, "..", "..", "web-client", "public", "bot-registry.json");
const registryPath = process.env.CTF_BOT_REGISTRY_PATH || defaultRegistryPath;

type RegistryShape = {
  address: string;
  version: string;
  cols: number;
  users: string[];
  viewUrls?: Record<string, string>;
};

const defaultAddress = process.env.CTF_ADDRESS || "146.235.232.128:25565";
const defaultVersion = process.env.CTF_VERSION || "1.21.11";
const defaultCols = Number(process.env.CTF_MULTIVIEW_COLS || 0);

function loadRegistry(): RegistryShape {
  try {
    if (fs.existsSync(registryPath)) {
      const raw = fs.readFileSync(registryPath, "utf8");
      const parsed = JSON.parse(raw);
      const parsedCols = Number(parsed.cols);
      const resolvedCols = Number.isFinite(parsedCols) && parsedCols > 0 ? parsedCols : defaultCols;
      return {
        address: parsed.address || defaultAddress,
        version: parsed.version || defaultVersion,
        cols: resolvedCols,
        users: Array.isArray(parsed.users) ? parsed.users : [],
        viewUrls: parsed.viewUrls && typeof parsed.viewUrls === "object" ? parsed.viewUrls : {}
      };
    }
  } catch {
    // ignore and recreate
  }

  return {
    address: defaultAddress,
    version: defaultVersion,
    cols: defaultCols,
    users: [],
    viewUrls: {}
  };
}

export function updateRegistry(
  users: string[],
  overrides?: Partial<RegistryShape>,
  viewUrls?: Record<string, string>
): void {
  const current = loadRegistry();
  const filteredUsers = users.filter((user) => typeof user === "string" && user.trim().length > 0);
  const safeViewUrls: Record<string, string> = {};
  const mergedViews = viewUrls ?? overrides?.viewUrls ?? current.viewUrls ?? {};
  for (const user of filteredUsers) {
    const url = mergedViews[user];
    if (typeof url === "string" && url.trim().length > 0) {
      safeViewUrls[user] = url;
    }
  }
  const resolvedCols = overrides?.cols ?? (defaultCols > 0 ? current.cols : 0);
  const next: RegistryShape = {
    address: overrides?.address ?? current.address,
    version: overrides?.version ?? current.version,
    cols: resolvedCols,
    users: filteredUsers,
    viewUrls: safeViewUrls
  };

  fs.mkdirSync(path.dirname(registryPath), { recursive: true });
  fs.writeFileSync(registryPath, JSON.stringify(next, null, 2));
  console.log(
    "[registry] address=%s users=%s views=%s",
    next.address,
    filteredUsers.join(","),
    Object.keys(safeViewUrls).join(",")
  );
}
