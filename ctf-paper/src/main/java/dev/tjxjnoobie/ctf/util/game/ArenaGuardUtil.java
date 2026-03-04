package dev.tjxjnoobie.ctf.util.game;

import dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.team.TeamManager;
import org.bukkit.entity.Player;

import dev.tjxjnoobie.ctf.dependency.interfaces.LifecycleDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.PlayerDependencyAccess;
/**
 * Shared arena-state guards.
 */
public final class ArenaGuardUtil {
    private interface ArenaGuardDependencyAccess extends LifecycleDependencyAccess, PlayerDependencyAccess {}

    // == Lifecycle ==
    private ArenaGuardUtil() {
    }

    // == Utilities ==
    private static final ArenaGuardDependencyAccess DEPENDENCIES = new ArenaGuardDependencyAccess() {};

    // == Predicates ==
    public static boolean isPlayerInArena(Player player) {
        TeamManager teamManager = DEPENDENCIES.getTeamManager();
        // Player must be assigned to a team to be considered in-arena.
        return player != null && teamManager != null && teamManager.getTeamKey(player) != null;
    }

    public static boolean isMatchInProgressOrOvertime(GameState state) {
        return state == GameState.IN_PROGRESS || state == GameState.OVERTIME; // Combat rules apply during live match or overtime.
    }

    public static boolean canPlayerUseCombatAbility(Player player) {
        GameStateManager gameStateManager = DEPENDENCIES.getGameStateManager();
        // Guard: short-circuit when player == null || gameStateManager == null.
        if (player == null || gameStateManager == null) {
            return false;
        }
        boolean playerInArena = isPlayerInArena(player);
        // Guard: short-circuit when !playerInArena.
        if (!playerInArena) {
            return false;
        }
        // Allow abilities only during match or overtime.
        return isMatchInProgressOrOvertime(gameStateManager.getGameState());
    }

    public static boolean shouldRestrictBuild(BuildToggleUtil buildToggleUtil,
                                              Player player) {
        // Guard: short-circuit when player == null || buildToggleUtil == null.
        if (player == null || buildToggleUtil == null) {
            return false;
        }
        boolean canBypass = buildToggleUtil.canBypass(player);
        // Guard: short-circuit when canBypass.
        if (canBypass) {
            return false;
        }
        // Restrict building for arena players without bypass.
        return isPlayerInArena(player);
    }

    public static boolean shouldRestrictLobbyInventory(BuildToggleUtil buildToggleUtil,
                                                       Player player) {
        boolean shouldRestrictBuild = shouldRestrictBuild(buildToggleUtil, player);
        // Guard: short-circuit when !shouldRestrictBuild.
        if (!shouldRestrictBuild) {
            return false;
        }
        GameStateManager gameStateManager = DEPENDENCIES.getGameStateManager();
        // Lobby state uses stricter inventory restrictions.
        return gameStateManager != null && gameStateManager.getGameState() == GameState.LOBBY;
    }
}
