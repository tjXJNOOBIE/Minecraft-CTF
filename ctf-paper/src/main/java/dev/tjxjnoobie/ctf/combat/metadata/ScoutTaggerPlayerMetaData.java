package dev.tjxjnoobie.ctf.combat.metadata;

/**
 * Mutable per-player Scout Tagger state (ammo + cooldown HUD task).
 */
public final class ScoutTaggerPlayerMetaData {
    private int ammo;
    private long lastThrowAtMs;
    private Integer cooldownVisualTaskId;

    public ScoutTaggerPlayerMetaData(int ammo, long lastThrowAtMs) {
        this.ammo = Math.max(0, ammo);
        this.lastThrowAtMs = Math.max(0L, lastThrowAtMs);
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = Math.max(0, ammo);
    }

    public long getLastThrowAtMs() {
        return lastThrowAtMs;
    }

    public void setLastThrowAtMs(long lastThrowAtMs) {
        this.lastThrowAtMs = Math.max(0L, lastThrowAtMs);
    }

    public Integer getCooldownVisualTaskId() {
        return cooldownVisualTaskId;
    }

    public void setCooldownVisualTaskId(Integer cooldownVisualTaskId) {
        this.cooldownVisualTaskId = cooldownVisualTaskId;
    }
}
