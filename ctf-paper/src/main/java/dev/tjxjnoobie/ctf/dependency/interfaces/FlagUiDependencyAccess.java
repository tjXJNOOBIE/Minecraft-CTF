package dev.tjxjnoobie.ctf.dependency.interfaces;

import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.dependency.DependencyLoaderAccess;
import dev.tjxjnoobie.ctf.game.flag.handlers.CaptureZoneUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBossBarUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagIndicatorUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamGlowUiHandler;

/**
 * Dependency-access surface for live match UI tied to flags, capture zones, and team glow.
 */
public interface FlagUiDependencyAccess {
    default BossBarManager getBossBarManager() { return DependencyLoaderAccess.findInstance(BossBarManager.class); }
    default FlagIndicatorUiHandler getFlagIndicatorUiHandler() { return DependencyLoaderAccess.findInstance(FlagIndicatorUiHandler.class); }
    default CaptureZoneUiHandler getCaptureZoneUiHandler() { return DependencyLoaderAccess.findInstance(CaptureZoneUiHandler.class); }
    default FlagBossBarUiHandler getFlagBossBarUiHandler() { return DependencyLoaderAccess.findInstance(FlagBossBarUiHandler.class); }
    default TeamGlowUiHandler getTeamGlowUiHandler() { return DependencyLoaderAccess.findInstance(TeamGlowUiHandler.class); }

    default void clearAllBossBars() {
        getBossBarManager().clearAll();
    }

    default void flagIndicatorUiEnsureFlagIndicators() {
        getFlagIndicatorUiHandler().ensureFlagIndicators();
    }

    default void flagIndicatorUiSyncIndicatorVisibilityNow() {
        getFlagIndicatorUiHandler().syncIndicatorVisibilityNow();
    }

    default void flagIndicatorUiTickIndicatorVisibility() {
        getFlagIndicatorUiHandler().tickIndicatorVisibility();
    }

    default void captureZoneUiResetCaptureZoneParticleTickCounter() {
        getCaptureZoneUiHandler().resetCaptureZoneParticleTickCounter();
    }

    default void captureZoneUiRenderCaptureZoneBorders() {
        getCaptureZoneUiHandler().renderCaptureZoneBorders();
    }

    default void flagBossBarUiUpdateReturnBossBars() {
        getFlagBossBarUiHandler().updateReturnBossBars();
    }

    default void flagBossBarUiUpdateCarrierBossBars() {
        getFlagBossBarUiHandler().updateCarrierBossBars();
    }

    default void teamGlowUiUpdateTeamGlowVisuals() {
        getTeamGlowUiHandler().updateTeamGlowVisuals();
    }

    default void teamGlowUiClearTeamGlowVisuals() {
        getTeamGlowUiHandler().clearTeamGlowVisuals();
    }
}
