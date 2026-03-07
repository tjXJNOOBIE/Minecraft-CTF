/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package dev.tjxjnoobie.ctf.game;

// Decompiled reference only; not part of the active plugin sources.

import dev.tjxjnoobie.ctf.config.message.interfaces.MessageAccess;
import dev.tjxjnoobie.ctf.dependency.ServiceLoaderAccess;
import dev.tjxjnoobie.ctf.game.GameLoopManager;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbility;
import dev.tjxjnoobie.ctf.game.combat.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.services.BaseMarkerService;
import dev.tjxjnoobie.ctf.game.lifecycle.services.MatchCleanupService;
import dev.tjxjnoobie.ctf.game.lifecycle.services.MatchFlowService;
import dev.tjxjnoobie.ctf.game.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.player.services.MatchPlayerSessionService;
import dev.tjxjnoobie.ctf.game.state.managers.GameStateManager;
import dev.tjxjnoobie.ctf.game.tags.GameState;
import dev.tjxjnoobie.ctf.game.tags.MatchStopReason;
import dev.tjxjnoobie.ctf.kit.KitManager;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.BukkitMessageSender;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class CtfMatchOrchestrator
implements MessageAccess,
BukkitMessageSender {
    private static final int DEFAULT_SCORE_LIMIT = 3;
    private static final int MIN_PLAYERS = 2;
    private final GameLoopManager gameLoopManager = this.getGameLoopManager();
    private final GameStateManager gameStateManager = this.getGameStateManager();
    private final TeamManager teamManager = this.getTeamManager();
    private final KitManager kitManager = this.getKitManager();
    private final ScoreBoardManager scoreBoardManager = this.getScoreBoardManager();
    private final Supplier<HomingSpearAbility> homingSpearAbility = () -> ServiceLoaderAccess.findInstance(HomingSpearAbility.class);
    private final Supplier<ScoutTaggerAbility> scoutTaggerAbility = () -> ServiceLoaderAccess.findInstance(ScoutTaggerAbility.class);
    private final Supplier<BaseMarkerService> baseMarkerService = () -> ServiceLoaderAccess.findInstance(BaseMarkerService.class);
    private final Supplier<MatchFlowService> matchFlowService = () -> this.getMatchFlowService();
    private final Supplier<MatchCleanupService> matchCleanupService = () -> this.getMatchCleanupService();
    private final Supplier<MatchPlayerSessionService> matchPlayerSessionService = () -> this.getMatchPlayerSessionService();
    private final Supplier<FlagBaseSetupHandler> flagBaseSetupHandler = () -> ServiceLoaderAccess.findInstance(FlagBaseSetupHandler.class);
    private final Supplier<FlagLifecycleHandler> flagLifecycleHandler = () -> ServiceLoaderAccess.findInstance(FlagLifecycleHandler.class);
    private final Supplier<FlagCarrierHandler> flagCarrierHandler = () -> ServiceLoaderAccess.findInstance(FlagCarrierHandler.class);
    private final Supplier<FlagCarrierStateHandler> flagCarrierStateHandler = () -> ServiceLoaderAccess.findInstance(FlagCarrierStateHandler.class);
    private int scoreLimit = 3;

    public boolean isRunning() {
        return !this.gameStateManager.isCleanupInProgress() && this.gameStateManager.isRunning();
    }

    public GameState getGameState() {
        return this.gameStateManager.getGameState();
    }

    public int getScoreLimit() {
        return this.scoreLimit;
    }

    public boolean hasMinimumArenaPlayers() {
        return this.teamManager.getJoinedPlayerCount() >= 2;
    }

    public boolean isArenaJoinLocked() {
        return this.gameStateManager.isCleanupInProgress();
    }

    public boolean areFlagBasesReady() {
        FlagBaseSetupHandler handler = this.flagBaseSetupHandler.get();
        return handler != null && handler.areBasesReady();
    }

    public boolean setFlagBase(Player player, TeamId teamId) {
        FlagBaseSetupHandler handler = this.flagBaseSetupHandler.get();
        return handler != null && handler.setFlagBase(player, teamId);
    }

    public TeamBaseMetaData getTeamBaseMetaData(TeamId teamId) {
        FlagBaseSetupHandler handler = this.flagBaseSetupHandler.get();
        return handler == null ? null : handler.getTeamBaseMetaData(teamId);
    }

    public void syncFlagIndicatorVisibility() {
        FlagLifecycleHandler handler = this.flagLifecycleHandler.get();
        if (handler != null) {
            handler.syncIndicatorVisibility();
        }
    }

    public Location getRespawnLocation(Player player) {
        return this.matchPlayerSessionService.get().getRespawnLocation(player);
    }

    public boolean isPlayerInArena(Player player) {
        return this.matchPlayerSessionService.get().isPlayerInArena(player);
    }

    public boolean hasKitSelection(Player player) {
        return this.kitManager.hasSelection(player);
    }

    public boolean isCombatAllowed(Player attacker, Player target) {
        boolean targetInArena;
        if (attacker == null || target == null) {
            return false;
        }
        boolean attackerInArena = this.isPlayerInArena(attacker);
        if (attackerInArena != (targetInArena = this.isPlayerInArena(target))) {
            return false;
        }
        if (!attackerInArena) {
            return true;
        }
        if (!this.isRunning()) {
            return false;
        }
        String attackerTeam = this.teamManager.getTeamKey(attacker);
        String targetTeam = this.teamManager.getTeamKey(target);
        if (attackerTeam == null || targetTeam == null || attackerTeam.equals(targetTeam)) {
            return false;
        }
        return this.kitManager.hasSelection(attacker) && this.kitManager.hasSelection(target);
    }

    public int getAllowedMatchTimeSeconds() {
        return this.matchFlowService.get().getAllowedMatchTimeSeconds();
    }

    @Override
    public HomingSpearAbility getHomingSpearAbility() {
        return this.homingSpearAbility.get();
    }

    @Override
    public ScoutTaggerAbility getScoutTaggerAbility() {
        return this.scoutTaggerAbility.get();
    }

    @Override
    public BaseMarkerService getBaseMarkerService() {
        return this.baseMarkerService.get();
    }

    public PlayerMatchStats getPlayerStats(UUID playerId) {
        return this.matchPlayerSessionService.get().getPlayerStats(playerId);
    }

    public int setScoreLimit(int newLimit) {
        this.scoreLimit = Math.max(1, newLimit);
        this.scoreBoardManager.setScoreLimit(this.scoreLimit);
        this.scoreBoardManager.updateScoreboards(this.gameLoopManager.getRemainingTime(), this.gameStateManager.getGameState());
        return this.scoreLimit;
    }

    public int setTeamScore(String teamKey, int score) {
        int applied = this.scoreBoardManager.setScore(teamKey, score);
        this.scoreBoardManager.updateScoreboards(this.gameLoopManager.getRemainingTime(), this.gameStateManager.getGameState());
        return applied;
    }

    public void addPlayerToArena(Player player, String teamKey) {
        this.matchPlayerSessionService.get().addPlayerToArena(player, teamKey);
    }

    public void removePlayerFromArena(Player player, boolean restoreLocation) {
        this.matchPlayerSessionService.get().removePlayerFromArena(player, restoreLocation);
    }

    public boolean setTeamSpawn(Player player, String teamKey) {
        return this.matchPlayerSessionService.get().setTeamSpawn(player, teamKey);
    }

    public boolean addTeamReturnPoint(Player player, String teamKey) {
        return this.matchPlayerSessionService.get().addTeamReturnPoint(player, teamKey);
    }

    public boolean setLobbySpawn(Player player) {
        return this.matchPlayerSessionService.get().setLobbySpawn(player);
    }

    public boolean setMatchTimeSeconds(long seconds) {
        return this.matchFlowService.get().setMatchTimeSeconds(seconds);
    }

    public String resolveJoinTeamKey(String requestedTeamKey) {
        if (requestedTeamKey == null || requestedTeamKey.isBlank()) {
            return this.teamManager.getBalancedTeamKey("red");
        }
        String normalized = this.teamManager.normalizeKey(requestedTeamKey);
        if (normalized == null) {
            return null;
        }
        if (this.isRunning()) {
            return this.teamManager.getBalancedTeamKey(normalized);
        }
        return normalized;
    }

    public boolean requestMatchStart() {
        return this.requestMatchStart(false);
    }

    public boolean requestMatchStart(boolean force) {
        return this.matchFlowService.get().requestMatchStart(force);
    }

    public void requestMatchStop(MatchStopReason reason) {
        this.requestMatchStop(reason, null);
    }

    public void requestMatchStop(MatchStopReason reason, String winningTeamKey) {
        this.matchCleanupService.get().requestMatchStop(reason, winningTeamKey);
    }

    public void shutdownMatchSystem() {
        this.matchCleanupService.get().shutdownMatchSystem();
    }

    public boolean processFlagTouch(Player player, Location blockLocation) {
        FlagCarrierHandler handler = this.flagCarrierHandler.get();
        if (handler == null) {
            return false;
        }
        return handler.processFlagTouch(player, blockLocation, this.isRunning());
    }

    public void processPlayerMovement(Player player, Location to) {
        FlagCarrierHandler carrierHandler = this.flagCarrierHandler.get();
        FlagCarrierStateHandler stateHandler = this.flagCarrierStateHandler.get();
        if (carrierHandler == null || stateHandler == null) {
            return;
        }
        carrierHandler.processPlayerMovement(player, to, this.isRunning());
        stateHandler.enforceCarrierFlagHotbarSlot(player);
    }

    @Override
    public void broadcastToArena(Component message) {
        BukkitMessageSender.super.broadcastToArena(message);
    }

    public void recordKill(Player killer) {
        this.matchPlayerSessionService.get().recordKill(killer);
    }

    public void recordDeath(Player victim) {
        this.matchPlayerSessionService.get().recordDeath(victim);
    }

    public void publishDebug(String message) {
        BukkitMessageSender.super.debug(message);
    }
}
