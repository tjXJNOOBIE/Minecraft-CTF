package dev.tjxjnoobie.ctf.game.combat.scout.handlers;

import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.config.message.MessageConfigHandler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.items.kit.ScoutSwordItem;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.util.CTFKeys;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ScoutTaggerCooldownHandlerTest extends TestLogSupport {

    private MatchPlayerSessionHandler matchPlayerSessionHandler;
    private KitSelectionHandler kitSelectionHandler;
    private ScoutTaggerRuntimeHandler runtimeHandler;
    private ScoutSwordItem scoutSwordItem;
    private BukkitMessageUtil bukkitMessageUtil;
    private ScoutTaggerCooldownHandler cooldownHandler;

    @BeforeEach
    void setUp() {
        MessageConfigHandler messageConfigHandler = Mockito.mock(MessageConfigHandler.class);
        when(messageConfigHandler.getMessageFormatted(org.mockito.ArgumentMatchers.eq(CTFKeys.uiScoutCooldownActionbarKey()), Mockito.<Object[]>any()))
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
        runtimeHandler = new ScoutTaggerRuntimeHandler();
        scoutSwordItem = Mockito.mock(ScoutSwordItem.class);
        bukkitMessageUtil = Mockito.mock(BukkitMessageUtil.class);

        registerDependencies(
                MatchPlayerSessionHandler.class, matchPlayerSessionHandler,
                KitSelectionHandler.class, kitSelectionHandler,
                ScoutSwordItem.class, scoutSwordItem,
                BukkitMessageUtil.class, bukkitMessageUtil);

        cooldownHandler = new ScoutTaggerCooldownHandler(matchPlayerSessionHandler, kitSelectionHandler, runtimeHandler);
    }

    @Test
    void refreshCooldownVisualsUsesActionBarOnly() {
        Player player = Mockito.mock(Player.class);
        PlayerInventory inventory = Mockito.mock(PlayerInventory.class);
        ItemStack heldItem = Mockito.mock(ItemStack.class);
        UUID playerUUID = UUID.randomUUID();

        when(player.getUniqueId()).thenReturn(playerUUID);
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(heldItem);
        when(matchPlayerSessionHandler.isPlayerInArena(player)).thenReturn(true);
        when(kitSelectionHandler.isScout(player)).thenReturn(true);
        when(scoutSwordItem.matches(heldItem)).thenReturn(true);

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(() -> Bukkit.getPlayer(playerUUID)).thenReturn(player);
            mockedBukkit.when(Bukkit::getLogger).thenReturn(Logger.getLogger("ScoutTaggerCooldownHandlerTest"));

            runtimeHandler.ensurePlayerState(playerUUID, 6).setAmmo(5);
            cooldownHandler.setNextAmmoRegenAtMs(System.currentTimeMillis() + 900L);
            cooldownHandler.refreshCooldownVisuals();

            verify(scoutSwordItem, never()).applyCooldownName(Mockito.any(), Mockito.anyLong(), Mockito.any());
            verify(scoutSwordItem, never()).restoreName(Mockito.any());
        }
    }
}
