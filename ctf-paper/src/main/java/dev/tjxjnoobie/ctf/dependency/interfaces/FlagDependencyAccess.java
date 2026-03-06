package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;

import dev.tjxjnoobie.ctf.game.flag.handlers.BaseMarkerHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.CaptureZoneParticleRenderer;
import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.CarrierInventoryTracker;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigData;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBaseSetupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagConfigHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.FlagIndicator;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagLifecycleHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagPickupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagReturnHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagScoreHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.FlagUiTicker;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.team.TeamId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface FlagDependencyAccess {
    default FlagConfigHandler getFlagConfigHandler() { return DependencyLoaderAccess.findInstance(FlagConfigHandler.class); }
    default FlagEventEffects getFlagEventEffects() { return DependencyLoaderAccess.findInstance(FlagEventEffects.class); }
    default FlagIndicator getFlagIndicator() { return DependencyLoaderAccess.findInstance(FlagIndicator.class); }
    default CTFCaptureZoneHandler getCTFCaptureZoneHandler() { return DependencyLoaderAccess.findInstance(CTFCaptureZoneHandler.class); }
    default CaptureZoneParticleRenderer getCaptureZoneParticleRenderer() { return DependencyLoaderAccess.findInstance(CaptureZoneParticleRenderer.class); }
    default FlagStateRegistry getFlagStateRegistry() { return DependencyLoaderAccess.findInstance(FlagStateRegistry.class); }
    default TeamBaseMetaDataResolver getTeamBaseMetaDataResolver() { return DependencyLoaderAccess.findInstance(TeamBaseMetaDataResolver.class); }
    default FlagUiTicker getFlagUiTicker() { return DependencyLoaderAccess.findInstance(FlagUiTicker.class); }
    default FlagBlockPlacer getFlagBlockPlacer() { return DependencyLoaderAccess.findInstance(FlagBlockPlacer.class); }
    default CarrierInventoryTracker getCarrierInventoryTracker() { return DependencyLoaderAccess.findInstance(CarrierInventoryTracker.class); }
    default CarrierEffects getCarrierEffects() { return DependencyLoaderAccess.findInstance(CarrierEffects.class); }
    default FlagPickupHandler getFlagPickupHandler() { return DependencyLoaderAccess.findInstance(FlagPickupHandler.class); }
    default FlagDropHandler getFlagDropHandler() { return DependencyLoaderAccess.findInstance(FlagDropHandler.class); }
    default FlagReturnHandler getFlagReturnHandler() { return DependencyLoaderAccess.findInstance(FlagReturnHandler.class); }
    default BaseMarkerHandler getBaseMarkerHandler() { return DependencyLoaderAccess.findInstance(BaseMarkerHandler.class); }
    default FlagScoreHandler getFlagScoreHandler() { return DependencyLoaderAccess.findInstance(FlagScoreHandler.class); }
    default FlagCarrierStateHandler getFlagCarrierStateHandler() { return DependencyLoaderAccess.findInstance(FlagCarrierStateHandler.class); }
    default FlagBaseSetupHandler getFlagBaseSetupHandler() { return DependencyLoaderAccess.findInstance(FlagBaseSetupHandler.class); }
    default FlagLifecycleHandler getFlagLifecycleHandler() { return DependencyLoaderAccess.findInstance(FlagLifecycleHandler.class); }
    default FlagCarrierHandler getFlagCarrierHandler() { return DependencyLoaderAccess.findInstance(FlagCarrierHandler.class); }

    default void loadFlagConfig() {
        getFlagConfigHandler().loadFlagConfig();
    }

    default void reloadFlagConfig() {
        getFlagConfigHandler().reloadFlagConfig();
    }

    default FlagConfigData getFlagData(String teamKey) {
        return getFlagConfigHandler().getFlagData(teamKey);
    }

    default void setFlagData(String teamKey, FlagConfigData flagData) {
        getFlagConfigHandler().setFlagData(teamKey, flagData);
    }

    default Optional<Location> getFlagIndicatorLocation(String teamKey) {
        return getFlagConfigHandler().getIndicator(teamKey);
    }

    default Material getFlagMaterial(String teamKey, Material fallback) {
        return getFlagConfigHandler().getMaterial(teamKey, fallback);
    }

    default boolean isFlagConfigActive() {
        return getFlagConfigHandler().isActive();
    }

    default void showFlagPickupMessaging(Player player, TeamId flagTeam) {
        getFlagEventEffects().showFlagPickupMessaging(player, flagTeam);
    }

    default void playFlagPickupEffects(Player player, TeamId flagTeam) {
        getFlagEventEffects().playFlagPickupEffects(player, flagTeam);
    }

    default void showFlagDropBroadcast(Player player, TeamId carriedTeam) {
        getFlagEventEffects().showFlagDropBroadcast(player, carriedTeam);
    }

    default void playFlagDropEffects(Player player, TeamId carriedTeam, Location dropLocation) {
        getFlagEventEffects().playFlagDropEffects(player, carriedTeam, dropLocation);
    }

    default void showFlagReturnMessages(Player player, TeamId teamId) {
        getFlagEventEffects().showFlagReturnMessages(player, teamId);
    }

    default void playFlagReturnEffects(TeamId teamId, Location baseLocation) {
        getFlagEventEffects().playFlagReturnEffects(teamId, baseLocation);
    }

    default void showFlagCaptureTitle(Player player, TeamId scoringTeamId) {
        getFlagEventEffects().showFlagCaptureTitle(player, scoringTeamId);
    }

    default void showFlagCaptureBroadcast(Player player, TeamId scoringTeam, TeamId capturedTeam) {
        getFlagEventEffects().showFlagCaptureBroadcast(player, scoringTeam, capturedTeam);
    }

    default void playFlagCaptureEffects(TeamId scoringTeam, TeamId capturedTeam, Location location) {
        getFlagEventEffects().playFlagCaptureEffects(scoringTeam, capturedTeam, location);
    }

    default void spawnFlagIndicatorForTeam(String teamName, Location indicatorLocation) {
        getFlagIndicator().spawnFlagIndicatorForTeam(teamName, indicatorLocation);
    }

    default void removeFlagIndicatorForTeam(String teamName) {
        getFlagIndicator().removeFlagIndicatorForTeam(teamName);
    }

    default void removeAllFlagIndicators() {
        getFlagIndicator().removeAllFlagIndicators();
    }

    default void resetFlagIndicators(List<Location> indicatorLocations) {
        getFlagIndicator().resetFlagIndicators(indicatorLocations);
    }

    default void syncFlagIndicatorVisibility(List<Player> arenaPlayers) {
        getFlagIndicator().syncVisibility(arenaPlayers);
    }

    default boolean hasFlagIndicatorForTeam(String teamName) {
        return getFlagIndicator().hasFlagIndicatorForTeam(teamName);
    }

    default double getCaptureRadius() {
        return getCTFCaptureZoneHandler().getCaptureRadius();
    }

    default boolean isInsideCaptureZone(TeamBaseMetaData baseData, Location playerLocation) {
        return getCTFCaptureZoneHandler().isInsideCaptureZone(baseData, playerLocation);
    }

    default void renderCaptureZoneParticles(String teamKey,
                                            List<Player> viewers,
                                            List<Location> returnPoints,
                                            Location baseFallback,
                                            double radius) {
        getCaptureZoneParticleRenderer().renderTeamZones(teamKey, viewers, returnPoints, baseFallback, radius);
    }

    default void registerDefaultFlags() {
        getFlagStateRegistry().registerDefaultFlags();
    }

    default Map<TeamId, FlagMetaData> getFlagMetaDataByTeamId() {
        return getFlagStateRegistry().getFlagMetaDataByTeamId();
    }

    default Collection<FlagMetaData> getAllFlagMetaData() {
        return getFlagStateRegistry().getAllFlagMetaData();
    }

    default TeamId findCarriedFlagTeam(UUID playerId) {
        return getFlagStateRegistry().findCarriedFlagTeam(playerId);
    }

    default void setFlagAtBase(TeamId teamId, Location baseLocation) {
        getFlagStateRegistry().setAtBase(teamId, baseLocation);
    }

    default void setDroppedFlag(TeamId teamId, Location droppedLocation) {
        getFlagStateRegistry().setDropped(teamId, droppedLocation);
    }

    default void setCarriedFlag(TeamId teamId, UUID carrierId) {
        getFlagStateRegistry().setCarried(teamId, carrierId);
    }

    default FlagMetaData flagFor(TeamId teamId) {
        return getFlagStateRegistry().flagFor(teamId);
    }

    default void resetFlagsToBase(BiConsumer<TeamId, Location> clearDroppedFlagBlock,
                                  BiConsumer<TeamId, Location> placeBaseFlagBlock) {
        getFlagStateRegistry().resetFlagsToBase(clearDroppedFlagBlock, placeBaseFlagBlock);
    }

    default boolean isFlagCarrier(UUID playerId) {
        return getFlagStateRegistry().isFlagCarrier(playerId);
    }

    default TeamBaseMetaData resolveTeamBaseMetaData(TeamId teamId) {
        return getTeamBaseMetaDataResolver().resolveTeamBaseMetaData(teamId);
    }

    default Location teamBaseResolveIndicatorSpawnLocation(TeamId teamId, FlagMetaData flag) {
        return getTeamBaseMetaDataResolver().resolveIndicatorSpawnLocation(teamId, flag);
    }

    default Location teamBaseResolveBaseIndicatorLocation(TeamId teamId, FlagMetaData flag) {
        return getTeamBaseMetaDataResolver().resolveBaseIndicatorLocation(teamId, flag);
    }

    default Location teamBaseResolveBaseIndicatorLocation(TeamId teamId, Location baseLocation) {
        return getTeamBaseMetaDataResolver().resolveBaseIndicatorLocation(teamId, baseLocation);
    }

    default Material teamBaseResolveFlagMaterial(TeamId teamId) {
        return getTeamBaseMetaDataResolver().resolveFlagMaterial(teamId);
    }

    default void startFlagUiUpdateTimer() {
        getFlagUiTicker().startFlagUiUpdateTimer();
    }

    default void stopFlagUiUpdateTimer() {
        getFlagUiTicker().stopFlagUiUpdateTimer();
    }

    default void clearTeamGlowVisuals() {
        getFlagUiTicker().clearTeamGlowVisuals();
    }

    default void resetCaptureZoneParticleTickCounter() {
        getFlagUiTicker().resetCaptureZoneParticleTickCounter();
    }

    default void tickFlagUi() {
        getFlagUiTicker().tickFlagUi();
    }

    default void ensureFlagIndicators() {
        getFlagUiTicker().ensureFlagIndicators();
    }

    default void flagUiSyncIndicatorVisibilityNow() {
        getFlagUiTicker().syncIndicatorVisibilityNow();
    }

    default TeamId resolveFlagTeamAtBlockLocation(Location blockLocation, FlagStateRegistry stateRegistry) {
        return getFlagBlockPlacer().resolveFlagTeamAtBlockLocation(blockLocation, stateRegistry);
    }

    default void clearFlagBlock(Location location, TeamId teamId) {
        getFlagBlockPlacer().clearFlagBlock(location, teamId);
    }

    default void placeBaseFlagBlock(Location location, TeamId teamId, Location indicatorLocation) {
        getFlagBlockPlacer().placeBaseFlagBlock(location, teamId, indicatorLocation);
    }

    default void placeBaseFlagBlockWithoutIndicator(Location location, TeamId teamId) {
        getFlagBlockPlacer().placeBaseFlagBlockWithoutIndicator(location, teamId);
    }

    default void placeDroppedFlagBlock(Location location, TeamId teamId) {
        getFlagBlockPlacer().placeDroppedFlagBlock(location, teamId);
    }

    default Set<UUID> getTrackedCarrierIdsSnapshot() {
        return getCarrierInventoryTracker().getTrackedCarrierIdsSnapshot();
    }

    default void giveCarrierFlagItem(Player player, Material flagMaterial, Component flagDisplayName) {
        getCarrierInventoryTracker().giveCarrierFlagItem(player, flagMaterial, flagDisplayName);
    }

    default void restoreCarrierFlagItem(Player player) {
        getCarrierInventoryTracker().restoreCarrierFlagItem(player);
    }

    default void enforceCarrierFlagHotbarSlot(Player player,
                                              TeamId carriedFlagTeam,
                                              Predicate<Material> isFlagMaterial,
                                              BiConsumer<Player, TeamId> giveCarrierFlagItemAction) {
        getCarrierInventoryTracker().enforceCarrierFlagHotbarSlot(
                player, carriedFlagTeam, isFlagMaterial, giveCarrierFlagItemAction);
    }

    default void applyCarrierEffects(Player player) {
        getCarrierEffects().applyCarrierEffects(player);
    }

    default void carrierEffectsClear(Player player) {
        getCarrierEffects().clearCarrierEffects(player);
    }

    default void clearCarrierEffectsForCarrierIds(Collection<UUID> carrierIds) {
        getCarrierEffects().clearCarrierEffectsForCarrierIds(carrierIds);
    }

    default void clearCarrierGlowTeamEntries() {
        getCarrierEffects().clearCarrierGlowTeamEntries();
    }

    default boolean processEnemyFlagPickup(Player player, TeamId flagTeam) {
        return getFlagPickupHandler().processEnemyFlagPickup(player, flagTeam);
    }

    default void dropCarriedFlagIfPresent(Player player) {
        getFlagDropHandler().dropCarriedFlagIfPresent(player);
    }

    default boolean returnDroppedOwnFlagToBase(Player player, TeamId teamId) {
        return getFlagReturnHandler().returnDroppedOwnFlagToBase(player, teamId);
    }

    default void initializeBaseMarkersFromConfig() {
        getBaseMarkerHandler().initializeFromConfig();
    }

    default void spawnOrMoveBaseMarker(TeamBaseMetaData baseData) {
        getBaseMarkerHandler().spawnOrMoveBaseMarker(baseData);
    }

    default void removeAllBaseMarkers() {
        getBaseMarkerHandler().removeAllMarkers();
    }

    default int processFlagCapture(Player player, String scoringTeam, String capturedFlagTeam) {
        return getFlagScoreHandler().processFlagCapture(player, scoringTeam, capturedFlagTeam);
    }

    default void clearCarrierItems() {
        getFlagCarrierStateHandler().clearCarrierItems();
    }

    default void clearCarrierFlagItemAndEffects(Player player) {
        getFlagCarrierStateHandler().clearCarrierFlagItemAndEffects(player);
    }

    default void enforceCarrierStateHotbarSlot(Player player) {
        getFlagCarrierStateHandler().enforceCarrierFlagHotbarSlot(player);
    }

    default boolean isFlagCarrierState(UUID playerId) {
        return getFlagCarrierStateHandler().isFlagCarrier(playerId);
    }

    default void initializeFlagsFromConfig() {
        getFlagBaseSetupHandler().initializeFlagsFromConfig();
    }

    default Location getFlagBaseLocation(TeamId teamId) {
        return getFlagBaseSetupHandler().getBaseLocation(teamId);
    }

    default TeamBaseMetaData getFlagTeamBaseMetaData(TeamId teamId) {
        return getFlagBaseSetupHandler().getTeamBaseMetaData(teamId);
    }

    default boolean setFlagBase(Player player, TeamId teamId) {
        return getFlagBaseSetupHandler().setFlagBase(player, teamId);
    }

    default boolean areFlagBasesReady() {
        return getFlagBaseSetupHandler().areBasesReady();
    }

    default void onFlagMatchStart() {
        getFlagLifecycleHandler().onMatchStart();
    }

    default void onFlagMatchStop() {
        getFlagLifecycleHandler().onMatchStop();
    }

    default void resetFlagsToBaseLifecycle() {
        getFlagLifecycleHandler().resetFlagsToBase();
    }

    default void resetFlagIndicatorsLifecycle() {
        getFlagLifecycleHandler().resetFlagIndicators();
    }

    default void syncFlagIndicatorVisibilityLifecycle() {
        getFlagLifecycleHandler().syncIndicatorVisibility();
    }

    default boolean processFlagTouch(Player player, Location blockLocation, boolean isMatchRunning) {
        return getFlagCarrierHandler().processFlagTouch(player, blockLocation, isMatchRunning);
    }

    default void processFlagCarrierMovement(Player player, Location to, boolean isMatchRunning) {
        getFlagCarrierHandler().processFlagCarrierMovement(player, to, isMatchRunning);
    }
}
