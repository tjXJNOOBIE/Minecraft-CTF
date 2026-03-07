import { applyLegacyModeBridge } from "../util/legacyModeBridge";

applyLegacyModeBridge(process.argv.slice(2));
require("../../../../bot-testing/src/ctf-sim-basic.ts");
