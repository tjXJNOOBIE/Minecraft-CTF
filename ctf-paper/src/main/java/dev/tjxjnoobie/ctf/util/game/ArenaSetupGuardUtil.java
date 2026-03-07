package dev.tjxjnoobie.ctf.util.game;

import dev.tjxjnoobie.ctf.dependency.interfaces.FlagDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.TeamDependencyAccess;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared arena setup guards for commands and match start flow.
 */
public final class ArenaSetupGuardUtil {
    private interface ArenaSetupDependencyAccess extends FlagDependencyAccess, TeamDependencyAccess {}

    private static final ArenaSetupDependencyAccess DEPENDENCIES = new ArenaSetupDependencyAccess() {};

    private ArenaSetupGuardUtil() {
    }

    public static boolean isArenaConfigured() {
        return getMissingSetupItems().isEmpty();
    }

    public static String describeMissingArenaSetup() {
        List<String> missingItems = getMissingSetupItems();
        if (missingItems.isEmpty()) {
            return "";
        }
        return String.join(", ", missingItems);
    }

    public static List<String> getMissingSetupItems() {
        List<String> missingItems = new ArrayList<>();
        TeamManager teamManager = DEPENDENCIES.getTeamManager();
        FlagBaseSetupHandler flagBaseSetupHandler = DEPENDENCIES.getFlagBaseSetupHandler();

        if (teamManager == null || flagBaseSetupHandler == null) {
            missingItems.add("arena setup services");
            return missingItems;
        }

        if (teamManager.getLobbySpawn().isEmpty()) {
            missingItems.add("lobby spawn");
        }
        if (teamManager.getSpawn(TeamId.RED).isEmpty()) {
            missingItems.add("red team spawn");
        }
        if (teamManager.getSpawn(TeamId.BLUE).isEmpty()) {
            missingItems.add("blue team spawn");
        }
        if (flagBaseSetupHandler.getBaseLocation(TeamId.RED) == null) {
            missingItems.add("red flag base");
        }
        if (flagBaseSetupHandler.getBaseLocation(TeamId.BLUE) == null) {
            missingItems.add("blue flag base");
        }
        if (teamManager.getReturnPoints(TeamId.RED).isEmpty()) {
            missingItems.add("red return point");
        }
        if (teamManager.getReturnPoints(TeamId.BLUE).isEmpty()) {
            missingItems.add("blue return point");
        }
        return missingItems;
    }
}
