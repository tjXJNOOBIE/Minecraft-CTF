package dev.tjxjnoobie.ctf.game.flag.effects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FlagEventEffectsTest extends TestLogSupport {

    private TeamManager teamManager;
    private BukkitMessageUtil bukkitMessageUtil;
    private FlagEventEffects flagEventEffects;

    @BeforeEach
    void setUp() {
        registerMessageHandler();

        teamManager = Mockito.mock(TeamManager.class);
        bukkitMessageUtil = Mockito.mock(BukkitMessageUtil.class);

        registerDependencies(
                TeamManager.class, teamManager,
                BukkitMessageUtil.class, bukkitMessageUtil);

        flagEventEffects = new FlagEventEffects();
        logStep("constructed real flag event effects");
    }

    @Test
    void showFlagPickupMessagingSendsTitleToCarrierAndDefendingTeam() {
        Player carrier = Mockito.mock(Player.class);
        Player defenderOne = Mockito.mock(Player.class);
        Player defenderTwo = Mockito.mock(Player.class);

        when(carrier.getName()).thenReturn("BlueScout");
        when(teamManager.getDisplayName(TeamId.RED)).thenReturn("Red");
        when(teamManager.getTeamPlayers(TeamId.RED)).thenReturn(java.util.List.of(defenderOne, defenderTwo));

        flagEventEffects.showFlagPickupMessaging(carrier, TeamId.RED);

        verify(bukkitMessageUtil, times(1)).sendTitle(eq(carrier), any(Title.class));
        verify(bukkitMessageUtil, times(1)).sendTitle(eq(defenderOne), any(Title.class));
        verify(bukkitMessageUtil, times(1)).sendTitle(eq(defenderTwo), any(Title.class));
        verify(bukkitMessageUtil).sendActionBar(eq(carrier), any());
        verify(bukkitMessageUtil).broadcastToArena(any());
    }
}
