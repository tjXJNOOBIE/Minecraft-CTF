package dev.tjxjnoobie.ctf.game.combat.metadata;

import java.util.UUID;

/**
 * Mutable per-shooter spear runtime state.
 */
public final class SpearShooterMetaData {

    // == Constants ==
    private long cooldownUntilMs;
    private long activeUntilMs;
    private long returnUntilMs;
    private long hitUntilMs;
    private String hitTargetName;
    private UUID activeSpearEntityId;
    private UUID lockedTargetId;
    private Integer spearReturnTimerTaskId;
    private int spearTick;

    // == Getters ==
    public long getCooldownUntilMs() {
        return cooldownUntilMs;
    }

    public long getActiveUntilMs() {
        return activeUntilMs;
    }

    public long getReturnUntilMs() {
        return returnUntilMs;
    }

    public long getHitUntilMs() {
        return hitUntilMs;
    }

    public String getHitTargetName() {
        return hitTargetName;
    }

    public UUID getActiveSpearEntityId() {
        return activeSpearEntityId;
    }

    public UUID getLockedTargetId() {
        return lockedTargetId;
    }

    public Integer getSpearReturnTimerTaskId() {
        return spearReturnTimerTaskId;
    }

    public int getSpearTick() {
        return spearTick;
    }

    // == Setters ==
    public void setCooldownUntilMs(long cooldownUntilMs) {
        this.cooldownUntilMs = Math.max(0L, cooldownUntilMs);
    }

    public void setActiveUntilMs(long activeUntilMs) {
        this.activeUntilMs = Math.max(0L, activeUntilMs);
    }

    public void setReturnUntilMs(long returnUntilMs) {
        this.returnUntilMs = Math.max(0L, returnUntilMs);
    }

    public void setHitUntilMs(long hitUntilMs) {
        this.hitUntilMs = Math.max(0L, hitUntilMs);
    }

    public void setHitTargetName(String hitTargetName) {
        this.hitTargetName = hitTargetName;
    }

    public void setActiveSpearEntityId(UUID activeSpearEntityId) {
        this.activeSpearEntityId = activeSpearEntityId;
    }

    public void setLockedTargetId(UUID lockedTargetId) {
        this.lockedTargetId = lockedTargetId;
    }

    public void setSpearReturnTimerTaskId(Integer spearReturnTimerTaskId) {
        this.spearReturnTimerTaskId = spearReturnTimerTaskId;
    }

    public void setSpearTick(int spearTick) {
        this.spearTick = Math.max(0, spearTick);
    }

    // == Utilities ==
    /**
     * Executes clearSpearShooterTimer.
     */
    public void clearSpearShooterTimer() {
        activeUntilMs = 0L;
        returnUntilMs = 0L;
        hitUntilMs = 0L;
        hitTargetName = null;
        activeSpearEntityId = null;
        lockedTargetId = null;
        spearReturnTimerTaskId = null;
        spearTick = 0;
    }

    /**
     * Executes clearSpearShooterMetaData.
     */
    public void clearSpearShooterMetaData() {
        clearSpearShooterTimer();
        cooldownUntilMs = 0L;
    }
}
