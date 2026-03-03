package dev.tjxjnoobie.ctf.combat.metadata;

import java.util.UUID;

/**
 * Mutable per-shooter spear runtime state.
 */
public final class SpearShooterMetaData {
    private long cooldownUntilMs;
    private long activeUntilMs;
    private long returnUntilMs;
    private long hitUntilMs;
    private String hitTargetName;
    private Integer actionBarTaskId;
    private UUID activeSpearEntityId;
    private UUID lockedTargetId;
    private Integer spearTimerTaskId;
    private Integer spearReturnTimerTaskId;
    private int spearTick;

    public long getCooldownUntilMs() {
        return cooldownUntilMs;
    }

    public void setCooldownUntilMs(long cooldownUntilMs) {
        this.cooldownUntilMs = Math.max(0L, cooldownUntilMs);
    }

    public long getActiveUntilMs() {
        return activeUntilMs;
    }

    public void setActiveUntilMs(long activeUntilMs) {
        this.activeUntilMs = Math.max(0L, activeUntilMs);
    }

    public long getReturnUntilMs() {
        return returnUntilMs;
    }

    public void setReturnUntilMs(long returnUntilMs) {
        this.returnUntilMs = Math.max(0L, returnUntilMs);
    }

    public long getHitUntilMs() {
        return hitUntilMs;
    }

    public void setHitUntilMs(long hitUntilMs) {
        this.hitUntilMs = Math.max(0L, hitUntilMs);
    }

    public String getHitTargetName() {
        return hitTargetName;
    }

    public void setHitTargetName(String hitTargetName) {
        this.hitTargetName = hitTargetName;
    }

    public Integer getActionBarTaskId() {
        return actionBarTaskId;
    }

    public void setActionBarTaskId(Integer actionBarTaskId) {
        this.actionBarTaskId = actionBarTaskId;
    }

    public UUID getActiveSpearEntityId() {
        return activeSpearEntityId;
    }

    public void setActiveSpearEntityId(UUID activeSpearEntityId) {
        this.activeSpearEntityId = activeSpearEntityId;
    }

    public UUID getLockedTargetId() {
        return lockedTargetId;
    }

    public void setLockedTargetId(UUID lockedTargetId) {
        this.lockedTargetId = lockedTargetId;
    }

    public Integer getSpearTimerTaskId() {
        return spearTimerTaskId;
    }

    public void setSpearTimerTaskId(Integer spearTimerTaskId) {
        this.spearTimerTaskId = spearTimerTaskId;
    }

    public Integer getSpearReturnTimerTaskId() {
        return spearReturnTimerTaskId;
    }

    public void setSpearReturnTimerTaskId(Integer spearReturnTimerTaskId) {
        this.spearReturnTimerTaskId = spearReturnTimerTaskId;
    }

    public int getSpearTick() {
        return spearTick;
    }

    public void setSpearTick(int spearTick) {
        this.spearTick = Math.max(0, spearTick);
    }

    public void clearRuntimeState() {
        activeUntilMs = 0L;
        returnUntilMs = 0L;
        hitUntilMs = 0L;
        hitTargetName = null;
        activeSpearEntityId = null;
        lockedTargetId = null;
        spearTimerTaskId = null;
        spearReturnTimerTaskId = null;
        spearTick = 0;
    }

    public void clearAllState() {
        clearRuntimeState();
        cooldownUntilMs = 0L;
        actionBarTaskId = null;
    }
}
