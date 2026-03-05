package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.CarrierInventoryTracker;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FlagCarrierStateHandlerTest extends TestLogSupport {
    private TeamManager teamManager;
    private FlagStateRegistry flagStateRegistry;
    private CarrierInventoryTracker carrierInventoryTracker;
    private CarrierEffects carrierEffects;
    private TeamBaseMetaDataResolver teamBaseMetaDataResolver;

    private FlagCarrierStateHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageAndSender();

        teamManager = Mockito.mock(TeamManager.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);
        carrierInventoryTracker = Mockito.mock(CarrierInventoryTracker.class);
        carrierEffects = Mockito.mock(CarrierEffects.class);
        teamBaseMetaDataResolver = Mockito.mock(TeamBaseMetaDataResolver.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagStateRegistry.class, flagStateRegistry);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CarrierInventoryTracker.class, carrierInventoryTracker);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CarrierEffects.class, carrierEffects);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamBaseMetaDataResolver.class, teamBaseMetaDataResolver);

        handler = new FlagCarrierStateHandler();
    }

    @Test
    void isFlagCarrierDelegatesToRegistry() {
        UUID playerId = UUID.randomUUID();
        when(flagStateRegistry.isFlagCarrier(playerId)).thenReturn(true);

        assertTrue(handler.isFlagCarrier(playerId));
    }

    @Test
    void enforceCarrierFlagHotbarSlotUsesTrackedCarriedTeam() {
        Player player = Mockito.mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(flagStateRegistry.findCarriedFlagTeam(playerId)).thenReturn(TeamId.BLUE);

        handler.enforceCarrierFlagHotbarSlot(player);

        verify(carrierInventoryTracker).enforceCarrierFlagHotbarSlot(eq(player), eq(TeamId.BLUE), any(), any());
    }

    @Test
    void clearCarrierFlagItemAndEffectsRestoresInventoryAndClearsEffects() {
        Player player = Mockito.mock(Player.class);

        handler.clearCarrierFlagItemAndEffects(player);

        verify(carrierInventoryTracker).restoreCarrierFlagItem(player);
        verify(carrierEffects).clearCarrierEffects(player);
    }

    @Test
    void clearCarrierItemsOnlyClearsOnlineCarriers() {
        UUID onlineCarrierId = UUID.randomUUID();
        UUID offlineCarrierId = UUID.randomUUID();

        FlagMetaData onlineCarriedFlag = new FlagMetaData();
        onlineCarriedFlag.state = FlagState.CARRIED;
        onlineCarriedFlag.carrier = onlineCarrierId;

        FlagMetaData offlineCarriedFlag = new FlagMetaData();
        offlineCarriedFlag.state = FlagState.CARRIED;
        offlineCarriedFlag.carrier = offlineCarrierId;

        when(flagStateRegistry.getAllFlagMetaData()).thenReturn(List.of(onlineCarriedFlag, offlineCarriedFlag));

        Player onlineCarrier = Mockito.mock(Player.class);
        when(Bukkit.getServer().getPlayer(onlineCarrierId)).thenReturn(onlineCarrier);
        when(Bukkit.getServer().getPlayer(offlineCarrierId)).thenReturn(null);

        handler.clearCarrierItems();

        verify(carrierInventoryTracker).restoreCarrierFlagItem(onlineCarrier);
        verify(carrierInventoryTracker, never()).restoreCarrierFlagItem((Player) Mockito.isNull());
    }
}
