package dev.tjxjnoobie.ctf.game.combat.scout.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.combat.scout.metadata.ScoutTaggerPlayerMetaData;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierStateHandler;
import dev.tjxjnoobie.ctf.config.message.MessageConfigHandler;
import dev.tjxjnoobie.ctf.items.kit.ScoutSwordItem;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ScoutTaggerAbilityTest extends TestLogSupport {
    // == Dependencies ==
    private MatchPlayerSessionHandler matchPlayerSessionHandler;
    private KitSelectionHandler kitSelectionHandler;
    private TeamManager teamManager;
    private FlagCarrierStateHandler flagCarrierStateHandler;
    private GameStateManager gameStateManager;
    private ScoutSwordItem scoutSwordItem;
    private BukkitMessageUtil bukkitMessageUtil;

    // == Runtime ==
    private ScoutTaggerRuntimeHandler runtimeHandler;
    private ScoutTaggerCooldownHandler cooldownHandler;
    private ScoutTaggerGlowHandler glowHandler;
    private ScoutTaggerAbility ability;

    @BeforeEach
    void setUp() {
        MessageConfigHandler messageConfigHandler = Mockito.mock(MessageConfigHandler.class);
        when(messageConfigHandler.getMessageFormatted(eq(CTFKeys.uiScoutCooldownActionbarKey()), Mockito.<Object[]>any()))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArgument(1, Object[].class);
                    return Component.text("Scout Tagger (" + args[0] + "/6) Regen: " + args[1] + "s");
                });
        when(messageConfigHandler.getMessageFormatted(Mockito.anyString(), Mockito.<Object[]>any()))
                .thenAnswer(invocation -> Component.text(invocation.getArgument(0, String.class)));
        when(messageConfigHandler.getMessage(Mockito.anyString()))
                .thenAnswer(invocation -> Component.text(invocation.getArgument(0, String.class)));
        registerDependency(MessageConfigHandler.class, messageConfigHandler);

        matchPlayerSessionHandler = Mockito.mock(MatchPlayerSessionHandler.class);
        kitSelectionHandler = Mockito.mock(KitSelectionHandler.class);
        teamManager = Mockito.mock(TeamManager.class);
        flagCarrierStateHandler = Mockito.mock(FlagCarrierStateHandler.class);
        gameStateManager = new GameStateManager();
        scoutSwordItem = Mockito.mock(ScoutSwordItem.class);
        bukkitMessageUtil = Mockito.mock(BukkitMessageUtil.class);

        registerDependencies(
                MatchPlayerSessionHandler.class, matchPlayerSessionHandler,
                KitSelectionHandler.class, kitSelectionHandler,
                TeamManager.class, teamManager,
                FlagCarrierStateHandler.class, flagCarrierStateHandler,
                GameStateManager.class, gameStateManager,
                ScoutSwordItem.class, scoutSwordItem,
                BukkitMessageUtil.class, bukkitMessageUtil);

        runtimeHandler = new ScoutTaggerRuntimeHandler();
        cooldownHandler = new ScoutTaggerCooldownHandler(matchPlayerSessionHandler, kitSelectionHandler, runtimeHandler);
        glowHandler = new ScoutTaggerGlowHandler();
        ability = new ScoutTaggerAbility(runtimeHandler, cooldownHandler, glowHandler);

        logStep("constructed concrete scout runtime stack");
    }

    @Test
    void tryThrowScoutSnowballConsumesAmmoAppliesBoostAndStartsCooldown() {
        Player player = mockValidScoutPlayer("ScoutOne");
        UUID playerUUID = player.getUniqueId();
        Snowball snowball = Mockito.mock(Snowball.class);
        PersistentDataContainer persistentDataContainer = Mockito.mock(PersistentDataContainer.class);

        when(snowball.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(player.launchProjectile(Snowball.class)).thenReturn(snowball);

        boolean thrown = ability.tryThrowScoutSnowball(player);

        assertTrue(thrown);
        assertEquals(5, runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());

        verify(player).launchProjectile(Snowball.class);
        verify(player).setVelocity(any(Vector.class));
        verify(player).playSound(any(Location.class), eq("entity.snowball.throw"), eq(0.7f), eq(1.1f));
        verify(player).spawnParticle(eq(Particle.SNOWFLAKE), any(Location.class), eq(6), eq(0.12), eq(0.12), eq(0.12), eq(0.01));
        verify(persistentDataContainer, Mockito.atLeastOnce()).set(any(), any(), any());
        logValue("remainingAmmo", runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());
    }

    @Test
    void tryThrowScoutSnowballBlocksFlagCarrierWithActionbarAndNoProjectile() {
        Player player = mockValidScoutPlayer("CarrierScout");
        UUID playerUUID = player.getUniqueId();

        when(flagCarrierStateHandler.isFlagCarrier(playerUUID)).thenReturn(true);

        boolean thrown = ability.tryThrowScoutSnowball(player);

        assertFalse(thrown);
        verify(player, never()).launchProjectile(Snowball.class);
        verify(player).playSound(any(Location.class), eq("entity.villager.no"), eq(0.6f), eq(0.9f));
    }

    @Test
    void tryThrowScoutSnowballBlocksWhenAmmoIsEmpty() {
        Player player = mockValidScoutPlayer("EmptyScout");
        UUID playerUUID = player.getUniqueId();

        runtimeHandler.ensurePlayerState(playerUUID, 6).setAmmo(0);

        boolean thrown = ability.tryThrowScoutSnowball(player);

        assertFalse(thrown);
        verify(player, never()).launchProjectile(Snowball.class);
        verify(player).playSound(any(Location.class), eq("entity.villager.no"), eq(0.6f), eq(0.9f));
    }

    @Test
    void tryThrowScoutSnowballConsumesEntireClipBeforeBlocking() {
        Player player = mockValidScoutPlayer("ClipScout");
        UUID playerUUID = player.getUniqueId();
        Snowball[] snowballs = new Snowball[6];
        PersistentDataContainer[] persistentDataContainers = new PersistentDataContainer[6];

        for (int i = 0; i < 6; i++) {
            snowballs[i] = Mockito.mock(Snowball.class);
            persistentDataContainers[i] = Mockito.mock(PersistentDataContainer.class);
            when(snowballs[i].getPersistentDataContainer()).thenReturn(persistentDataContainers[i]);
        }
        when(player.launchProjectile(Snowball.class)).thenReturn(
                snowballs[0], snowballs[1], snowballs[2], snowballs[3], snowballs[4], snowballs[5]);

        for (int i = 0; i < 6; i++) {
            assertTrue(ability.tryThrowScoutSnowball(player));
        }
        boolean seventhThrown = ability.tryThrowScoutSnowball(player);

        assertFalse(seventhThrown);
        assertEquals(0, runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());
        verify(player, Mockito.times(6)).launchProjectile(Snowball.class);
    }

    @Test
    void regenAmmoRestoresOneChargeAndAllowsAnotherThrow() throws Exception {
        Player player = mockValidScoutPlayer("RepeatScout");
        UUID playerUUID = player.getUniqueId();
        Snowball[] snowballs = new Snowball[7];
        PersistentDataContainer[] persistentDataContainers = new PersistentDataContainer[7];

        for (int i = 0; i < 7; i++) {
            snowballs[i] = Mockito.mock(Snowball.class);
            persistentDataContainers[i] = Mockito.mock(PersistentDataContainer.class);
            when(snowballs[i].getPersistentDataContainer()).thenReturn(persistentDataContainers[i]);
        }
        when(player.launchProjectile(Snowball.class)).thenReturn(
                snowballs[0], snowballs[1], snowballs[2], snowballs[3], snowballs[4], snowballs[5], snowballs[6]);

        for (int i = 0; i < 6; i++) {
            assertTrue(ability.tryThrowScoutSnowball(player));
        }
        assertFalse(ability.tryThrowScoutSnowball(player));

        when(teamManager.getJoinedPlayers()).thenReturn(List.of(player));
        invokeRegenAmmo();

        boolean nextThrown = ability.tryThrowScoutSnowball(player);

        assertTrue(nextThrown);
        assertEquals(0, runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());
        verify(player, Mockito.times(7)).launchProjectile(Snowball.class);
    }

    @Test
    void processScoutSnowballHitOnlyAppliesGlowToEnemyPlayers() {
        Player shooter = Mockito.mock(Player.class);
        Player enemyVictim = Mockito.mock(Player.class);
        Player friendlyVictim = Mockito.mock(Player.class);
        Snowball snowball = Mockito.mock(Snowball.class);
        PersistentDataContainer persistentDataContainer = Mockito.mock(PersistentDataContainer.class);

        when(shooter.getName()).thenReturn("ScoutShooter");
        when(enemyVictim.getName()).thenReturn("EnemyVictim");
        when(snowball.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(snowball.getShooter()).thenReturn(shooter);
        when(persistentDataContainer.has(any(), any())).thenReturn(true);
        when(teamManager.getTeamKey(shooter)).thenReturn("red");
        when(teamManager.getTeamKey(enemyVictim)).thenReturn("blue");
        when(teamManager.getTeamKey(friendlyVictim)).thenReturn("red");

        ability.processScoutSnowballHit(shooter, enemyVictim, snowball);
        ability.processScoutSnowballHit(shooter, friendlyVictim, snowball);

        verify(enemyVictim).setGlowing(true);
        verify(friendlyVictim, never()).setGlowing(true);
    }

    @Test
    void processScoutSnowballDamageCancelCancelsEnemyDamage() {
        Player shooter = Mockito.mock(Player.class);
        Player victim = Mockito.mock(Player.class);
        Snowball snowball = Mockito.mock(Snowball.class);
        PersistentDataContainer persistentDataContainer = Mockito.mock(PersistentDataContainer.class);
        EntityDamageByEntityEvent event = Mockito.mock(EntityDamageByEntityEvent.class);

        when(snowball.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(snowball.getShooter()).thenReturn(shooter);
        when(persistentDataContainer.has(any(), any())).thenReturn(true);
        when(teamManager.getTeamKey(shooter)).thenReturn("red");
        when(teamManager.getTeamKey(victim)).thenReturn("blue");

        ability.processScoutSnowballDamageCancel(snowball, victim, event);

        verify(event).setCancelled(true);
    }

    @Test
    void regenAmmoRestoresOneChargeForActiveScoutOnlyDuringRunningState() throws Exception {
        Player player = mockValidScoutPlayer("RegenScout");
        UUID playerUUID = player.getUniqueId();
        Snowball snowball = Mockito.mock(Snowball.class);
        PersistentDataContainer persistentDataContainer = Mockito.mock(PersistentDataContainer.class);

        when(snowball.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(player.launchProjectile(Snowball.class)).thenReturn(snowball);
        when(teamManager.getJoinedPlayers()).thenReturn(List.of(player));

        boolean thrown = ability.tryThrowScoutSnowball(player);
        assertTrue(thrown);
        assertEquals(5, runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());

        invokeRegenAmmo();
        assertEquals(6, runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());

        gameStateManager.setGameState(GameState.LOBBY);
        runtimeHandler.ensurePlayerState(playerUUID, 6).setAmmo(4);
        invokeRegenAmmo();
        assertEquals(4, runtimeHandler.ensurePlayerState(playerUUID, 6).getAmmo());
    }
    private Player mockValidScoutPlayer(String playerName) {
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        ItemStack heldItem = Mockito.mock(ItemStack.class);
        UUID playerUUID = UUID.randomUUID();
        World world = Mockito.mock(World.class);
        Location location = Mockito.mock(Location.class);
        Location eyeLocation = Mockito.mock(Location.class);
        Vector direction = new Vector(1.0, 0.0, 0.0);
        Vector velocity = new Vector(0.0, 0.0, 0.0);

        when(player.getUniqueId()).thenReturn(playerUUID);
        when(player.getName()).thenReturn(playerName);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(heldItem);
        when(player.getLocation()).thenReturn(location);
        when(player.getEyeLocation()).thenReturn(eyeLocation);
        when(player.getVelocity()).thenReturn(velocity);
        when(location.getDirection()).thenReturn(direction);
        when(location.getWorld()).thenReturn(world);
        when(eyeLocation.getWorld()).thenReturn(world);

        when(matchPlayerSessionHandler.isPlayerInArena(player)).thenReturn(true);
        when(kitSelectionHandler.isScout(player)).thenReturn(true);
        when(teamManager.getTeamKey(player)).thenReturn("red");
        when(flagCarrierStateHandler.isFlagCarrier(playerUUID)).thenReturn(false);
        when(scoutSwordItem.matches(heldItem)).thenReturn(true);

        gameStateManager.setGameState(GameState.IN_PROGRESS);
        return player;
    }

    private void invokeRegenAmmo() throws Exception {
        Method regenAmmoMethod = ScoutTaggerAbility.class.getDeclaredMethod("regenAmmo");
        regenAmmoMethod.setAccessible(true);
        regenAmmoMethod.invoke(ability);
        logStep("invoked private regenAmmo() for runtime verification");
    }
}
