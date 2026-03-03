package dev.tjxjnoobie.ctf.game.combat.scout.metadata;

/**
 * Mutable per-player Scout Tagger ammo state.
 */
public final class ScoutTaggerPlayerMetaData {

    // == Runtime state ==
    private int ammo;
    // == Lifecycle ==
    /**
     * Constructs a ScoutTaggerPlayerMetaData instance.
     *
     * @param ammo Numeric value used by this operation.
     */
    public ScoutTaggerPlayerMetaData(int ammo) {
        this.ammo = Math.max(0, ammo);
    }

    // == Getters ==
    /**
     * Returns data for getAmmo.
     */
    public int getAmmo() {
        return ammo;
    }

    // == Setters ==
    /**
     * Updates state for setAmmo.
     *
     * @param ammo Numeric value used by this operation.
     */
    public void setAmmo(int ammo) {
        this.ammo = Math.max(0, ammo);
    }

}
