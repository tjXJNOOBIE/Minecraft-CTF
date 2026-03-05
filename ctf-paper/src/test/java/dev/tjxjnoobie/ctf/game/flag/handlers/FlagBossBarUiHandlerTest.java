package dev.tjxjnoobie.ctf.game.flag.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class FlagBossBarUiHandlerTest extends TestLogSupport {
    // Dependencies
    private TeamManager teamManager;
    private FlagStateRegistry flagStateRegistry;
    private TeamBaseMetaDataResolver teamBaseMetaDataResolver;
    private BukkitMessageUtil bukkitMessageUtil;
    private FlagBossBarUiHandler handler;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        registerMessageHandler();
        teamManager = Mockito.mock(TeamManager.class);
        flagStateRegistry = Mockito.mock(FlagStateRegistry.class);
        teamBaseMetaDataResolver = Mockito.mock(TeamBaseMetaDataResolver.class);
        bukkitMessageUtil = Mockito.mock(BukkitMessageUtil.class);

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamManager.class, teamManager);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(FlagStateRegistry.class, flagStateRegistry);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(TeamBaseMetaDataResolver.class, teamBaseMetaDataResolver);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(BukkitMessageUtil.class, bukkitMessageUtil);

        handler = new FlagBossBarUiHandler();
    }

    @Test
    void updateReturnBossBarsShowsReturnBarForDroppedFlag() {
        World world = Mockito.mock(World.class);
        Player player = Mockito.mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(teamManager.getTeamPlayers(TeamId.RED.key())).thenReturn(List.of(player));
        when(teamManager.getTeamPlayers(TeamId.BLUE.key())).thenReturn(List.of());

        FlagMetaData dropped = new FlagMetaData();
        dropped.state = FlagState.DROPPED;
        dropped.activeLocation = new Location(world, 5, 64, 0);
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(dropped);
        when(flagStateRegistry.flagFor(TeamId.BLUE)).thenReturn(new FlagMetaData());

        handler.updateReturnBossBars();

        verify(bukkitMessageUtil).showBossBar(eq(player), eq(BukkitBossBarType.RETURN), any(Component.class), anyFloat());
    }

    @Test
    void updateReturnBossBarsHidesReturnBarWhenNotDropped() {
        Player player = Mockito.mock(Player.class);
        when(teamManager.getTeamPlayers(TeamId.RED.key())).thenReturn(List.of(player));
        when(teamManager.getTeamPlayers(TeamId.BLUE.key())).thenReturn(List.of());

        FlagMetaData atBase = new FlagMetaData();
        atBase.state = FlagState.AT_BASE;
        when(flagStateRegistry.flagFor(TeamId.RED)).thenReturn(atBase);
        when(flagStateRegistry.flagFor(TeamId.BLUE)).thenReturn(new FlagMetaData());

        handler.updateReturnBossBars();

        verify(bukkitMessageUtil).hideBossBar(player, BukkitBossBarType.RETURN);
    }

    @Test
    void updateCarrierBossBarsShowsCarrierBarAndActionBar() {
        World world = Mockito.mock(World.class);
        Player carrier = Mockito.mock(Player.class);
        when(carrier.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(teamManager.getTeamKey(carrier)).thenReturn(TeamId.BLUE.key());

        UUID carrierId = UUID.randomUUID();
        FlagMetaData carried = new FlagMetaData();
        carried.state = FlagState.CARRIED;
        carried.carrier = carrierId;
        Map<TeamId, FlagMetaData> flags = new EnumMap<>(TeamId.class);
        flags.put(TeamId.RED, carried);
        when(flagStateRegistry.getFlagMetaDataByTeamId()).thenReturn(flags);

        TeamBaseMetaData baseData = new TeamBaseMetaData();
        baseData.setFlagSpawnLocation(new Location(world, 10, 64, 0));
        when(teamBaseMetaDataResolver.resolveTeamBaseMetaData(TeamId.BLUE)).thenReturn(baseData);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(carrierId)).thenReturn(carrier);

            handler.updateCarrierBossBars();
        }

        verify(bukkitMessageUtil).showBossBar(eq(carrier), eq(BukkitBossBarType.CARRIER), any(Component.class), anyFloat());
        verify(bukkitMessageUtil).sendActionBar(eq(carrier), any(Component.class));
    }
}

