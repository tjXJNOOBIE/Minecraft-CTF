package dev.tjxjnoobie.ctf.game.flag;

import dev.tjxjnoobie.ctf.game.flag.handlers.CaptureZoneUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagBossBarUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagIndicatorUiHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamGlowUiHandler;
import dev.tjxjnoobie.ctf.util.tasks.FastTickBus;

import dev.tjxjnoobie.ctf.dependency.interfaces.TaskDependencyAccess;
import dev.tjxjnoobie.ctf.dependency.interfaces.FlagUiDependencyAccess;
/**
 * Owns periodic flag UI rendering: boss bars, indicators, capture particles, and team glow visuals.
 */
public final class FlagUiTicker implements TaskDependencyAccess, FlagUiDependencyAccess {
    private static final String TICK_KEY = "flag-ui";

    // == Lifecycle ==
    /**
     * Constructs a FlagUiTicker instance.
     */
    public FlagUiTicker() {
    }

    /**
     * Executes the startFlagUiUpdateTimer operation.
     */
    public void startFlagUiUpdateTimer() {
        getFastTickBus().register(TICK_KEY, 2, this::tickFlagUi);
    }

    /**
     * Executes the stopFlagUiUpdateTimer operation.
     */
    public void stopFlagUiUpdateTimer() {
        getFastTickBus().unregister(TICK_KEY);
    }

    // == Utilities ==
    /**
     * Executes clearTeamGlowVisuals.
     */
    public void clearTeamGlowVisuals() {
        getTeamGlowUiHandler().clearTeamGlowVisuals();
    }

    /**
     * Executes resetCaptureZoneParticleTickCounter.
     */
    public void resetCaptureZoneParticleTickCounter() {
        getCaptureZoneUiHandler().resetCaptureZoneParticleTickCounter();
    }

    /**
     * Executes tickFlagUi.
     */
    public void tickFlagUi() {
        ensureFlagIndicators();
        getFlagIndicatorUiHandler().tickIndicatorVisibility();
        getCaptureZoneUiHandler().renderCaptureZoneBorders();
        getFlagBossBarUiHandler().updateReturnBossBars();
        getFlagBossBarUiHandler().updateCarrierBossBars();
        getTeamGlowUiHandler().updateTeamGlowVisuals();
    }

    /**
     * Executes ensureFlagIndicators.
     */
    public void ensureFlagIndicators() {
        getFlagIndicatorUiHandler().ensureFlagIndicators();
    }

    /**
     * Executes syncIndicatorVisibilityNow.
     */
    public void syncIndicatorVisibilityNow() {
        getFlagIndicatorUiHandler().syncIndicatorVisibilityNow();
    }
}

