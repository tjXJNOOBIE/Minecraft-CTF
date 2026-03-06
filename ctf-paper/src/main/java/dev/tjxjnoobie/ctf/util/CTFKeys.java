package dev.tjxjnoobie.ctf.util;

/**
 * Centralized catalog of string keys used across CTF systems.
 *
 * This class provides stable accessors for metadata tags, message keys, UI keys,
 * and scoreboard identifiers so callers avoid hardcoded literals and keep key
 * namespaces consistent.
 */
public final class CTFKeys {

    // == Utilities ==
    private CTFKeys() {
    }

    public static String combatHomingSpearTag() {
        return Combat.HOMING_SPEAR_TAG;
    }

    public static String combatHomingSpearShooterTag() {
        return Combat.HOMING_SPEAR_SHOOTER_TAG;
    }

    public static String combatReturningSpearShooterTag() {
        return Combat.RETURNING_SPEAR_SHOOTER_TAG;
    }

    public static String combatScoutSnowballTag() {
        return Combat.SCOUT_SNOWBALL_TAG;
    }

    public static String combatScoutSnowballShooterTag() {
        return Combat.SCOUT_SNOWBALL_SHOOTER_TAG;
    }

    public static String flagTeamMetadataTag() {
        return Flag.FLAG_TEAM_METADATA_TAG;
    }

    public static String uiKitLockedActionbarKey() {
        return Ui.KIT_LOCKED_ACTIONBAR_KEY;
    }

    public static String uiKitSelectedMessageKey() {
        return Ui.KIT_SELECTED_MESSAGE_KEY;
    }

    public static String uiSpearBlockedActionbarKey() {
        return Ui.SPEAR_BLOCKED_ACTIONBAR_KEY;
    }

    public static String uiSpearHitActionbarKey() {
        return Ui.SPEAR_HIT_ACTIONBAR_KEY;
    }

    public static String uiSpearLockedActionbarKey() {
        return Ui.SPEAR_LOCKED_ACTIONBAR_KEY;
    }

    public static String uiSpearReturningActionbarKey() {
        return Ui.SPEAR_RETURNING_ACTIONBAR_KEY;
    }

    public static String uiSpearTickBusKey() {
        return Ui.SPEAR_TICK_BUS_KEY;
    }

    public static String uiSpearActionbarTickBusKey() {
        return Ui.SPEAR_ACTIONBAR_TICK_BUS_KEY;
    }

    public static String uiScoutFlagActionbarKey() {
        return Ui.SCOUT_FLAG_ACTIONBAR_KEY;
    }

    public static String uiScoutEmptyActionbarKey() {
        return Ui.SCOUT_EMPTY_ACTIONBAR_KEY;
    }

    public static String uiScoutCooldownActionbarKey() {
        return Ui.SCOUT_COOLDOWN_ACTIONBAR_KEY;
    }

    public static String uiScoutAmmoActionbarKey() {
        return Ui.SCOUT_AMMO_ACTIONBAR_KEY;
    }

    public static String uiFlagCarrierActionbarKey() {
        return Ui.FLAG_CARRIER_ACTIONBAR_KEY;
    }

    public static String messageBuildBlockedErrorKey() {
        return Message.ERROR_BUILD_BLOCKED_KEY;
    }

    public static String messageErrorNoPermissionKey() {
        return Message.ERROR_NO_PERMISSION_KEY;
    }

    public static String teamScoreboardRedId() {
        return Team.SCOREBOARD_RED_ID;
    }

    public static String teamScoreboardBlueId() {
        return Team.SCOREBOARD_BLUE_ID;
    }

    public static String coreFallbackNamespace() {
        return Core.FALLBACK_NAMESPACE;
    }

    public static String permissionAdmin() {
        return Permission.ADMIN;
    }

    public static String permissionDebug() {
        return Permission.DEBUG;
    }

    public static String permissionSimulate() {
        return Permission.SIMULATE;
    }

    public static String scoreboardObjectiveId() {
        return Scoreboard.OBJECTIVE_ID;
    }

    public static String spawnLobbyPath() {
        return Spawn.LOBBY_PATH;
    }

    public static String spawnTeamSpawnsPathPrefix() {
        return Spawn.TEAM_SPAWNS_PATH_PREFIX;
    }

    public static String spawnReturnPointsPathPrefix() {
        return Spawn.RETURN_POINTS_PATH_PREFIX;
    }

    public static String spawnCaptureZoneRadiusPath() {
        return Spawn.CAPTURE_ZONE_RADIUS_PATH;
    }

    public static String flagConfigFlagsPathPrefix() {
        return FlagConfig.FLAGS_PATH_PREFIX;
    }

    private static final class Combat {
        private static final String HOMING_SPEAR_TAG = "ctf_homing_spear";
        private static final String HOMING_SPEAR_SHOOTER_TAG = "ctf_homing_spear_shooter";
        private static final String RETURNING_SPEAR_SHOOTER_TAG = "ctf_return_spear_shooter";
        private static final String SCOUT_SNOWBALL_TAG = "ctf_scout_snowball";
        private static final String SCOUT_SNOWBALL_SHOOTER_TAG = "ctf_scout_snowball_shooter";

        private Combat() {
        }
    }

    private static final class Flag {
        private static final String FLAG_TEAM_METADATA_TAG = "ctf-flag-team";

        private Flag() {
        }
    }

    private static final class Ui {
        private static final String KIT_LOCKED_ACTIONBAR_KEY = "actionbar.kit.locked";
        private static final String KIT_SELECTED_MESSAGE_KEY = "kit.selected";
        private static final String SPEAR_BLOCKED_ACTIONBAR_KEY = "actionbar.spear.blocked";
        private static final String SPEAR_HIT_ACTIONBAR_KEY = "actionbar.spear.hit";
        private static final String SPEAR_LOCKED_ACTIONBAR_KEY = "actionbar.spear.locked";
        private static final String SPEAR_RETURNING_ACTIONBAR_KEY = "actionbar.spear.returning";
        private static final String SPEAR_TICK_BUS_KEY = "homing-spear-runtime";
        private static final String SPEAR_ACTIONBAR_TICK_BUS_KEY = "homing-spear-actionbar";
        private static final String SCOUT_FLAG_ACTIONBAR_KEY = "actionbar.scout.flag";
        private static final String SCOUT_EMPTY_ACTIONBAR_KEY = "actionbar.scout.empty";
        private static final String SCOUT_COOLDOWN_ACTIONBAR_KEY = "actionbar.scout.cooldown";
        private static final String SCOUT_AMMO_ACTIONBAR_KEY = "actionbar.scout.ammo";
        private static final String FLAG_CARRIER_ACTIONBAR_KEY = "actionbar.flag_carrier";

        private Ui() {
        }
    }

    private static final class Message {
        private static final String ERROR_BUILD_BLOCKED_KEY = "error.build_blocked";
        private static final String ERROR_NO_PERMISSION_KEY = "error.no_permission";

        private Message() {
        }
    }

    private static final class Team {
        private static final String SCOREBOARD_RED_ID = "ctf_red";
        private static final String SCOREBOARD_BLUE_ID = "ctf_blue";

        private Team() {
        }
    }

    private static final class Core {
        private static final String FALLBACK_NAMESPACE = "ctf";

        private Core() {
        }
    }

    private static final class Permission {
        private static final String ADMIN = "ctf.admin";
        private static final String DEBUG = "ctf.debug";
        private static final String SIMULATE = "ctf.simulate";

        private Permission() {
        }
    }

    private static final class Scoreboard {
        private static final String OBJECTIVE_ID = "ctf_score";

        private Scoreboard() {
        }
    }

    private static final class Spawn {
        private static final String LOBBY_PATH = "lobby-spawn";
        private static final String TEAM_SPAWNS_PATH_PREFIX = "team-spawns.";
        private static final String RETURN_POINTS_PATH_PREFIX = "return-points.";
        private static final String CAPTURE_ZONE_RADIUS_PATH = "capture-zone.radius";

        private Spawn() {
        }
    }

    private static final class FlagConfig {
        private static final String FLAGS_PATH_PREFIX = "flags.";

        private FlagConfig() {
        }
    }
}
