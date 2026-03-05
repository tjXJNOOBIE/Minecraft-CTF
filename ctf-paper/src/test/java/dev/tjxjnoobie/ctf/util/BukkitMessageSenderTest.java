package dev.tjxjnoobie.ctf.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.bossbar.BossBarManager;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.CTFPlayerMetaData;
import dev.tjxjnoobie.ctf.game.debug.managers.DebugFeed;
import dev.tjxjnoobie.ctf.util.bukkit.message.tags.BukkitBossBarType;
import dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtil;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BukkitMessageSenderTest extends TestLogSupport {
    // Dependencies
    private BukkitMessageSenderClient client;

    @BeforeEach
    void setUp() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
        client = new BukkitMessageSenderClient();
    }

    @AfterEach
    void tearDown() {
        DependencyLoader.getFallbackDependencyLoader().resetInstances();
    }

    @Test
    void defaultMethodsDelegatePlayerUxCalls() {
        Player player = mock(Player.class);
        Component message = Component.text("hello");
        Title title = Title.title(Component.text("header"), Component.text("sub"));
        registerSender();

        client.sendActionBar(player, message);
        client.sendMessage(player, message);
        client.sendTitle(player, title);

        verify(player).sendActionBar(message);
        verify(player).sendMessage(message);
        verify(player).showTitle(title);
    }

    @Test
    void defaultMethodsDelegateBroadcastAndDebug() {
        CTFPlayerMetaData container = mock(CTFPlayerMetaData.class);
        DebugFeed debugFeed = mock(DebugFeed.class);
        Component message = Component.text("arena");
        Title title = Title.title(Component.text("header"), Component.text("sub"));

        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CTFPlayerMetaData.class, container);
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(DebugFeed.class, debugFeed);
        registerSender();

        client.broadcastToArena(message);
        client.broadcastToArenaTitle(title);
        client.sendDebugMessage("trace");

        verify(container).broadcast(message);
        verify(container).broadcastTitle(title);
        verify(debugFeed).send("trace");
    }

    @Test
    void bossbarTypeRoutingWorks() {
        BossBarManager bossBarManager = mock(BossBarManager.class);
        Player player = mock(Player.class);
        Component text = Component.text("status");
        registerSender();
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(BossBarManager.class, bossBarManager);

        client.showBossBar(player, BukkitBossBarType.CARRIER, text, 0.20f);
        client.hideBossBar(player, BukkitBossBarType.CARRIER);
        client.showBossBar(player, BukkitBossBarType.RETURN, text, 0.40f);
        client.hideBossBar(player, BukkitBossBarType.RETURN);
        client.showBossBar(player, BukkitBossBarType.WAITING, text, 0.60f);
        client.hideBossBar(player, BukkitBossBarType.WAITING);
        client.showBossBar(player, BukkitBossBarType.KILL, text, 0.80f);
        client.hideBossBar(player, BukkitBossBarType.KILL);

        verify(bossBarManager).showCarrierBar(player, text, 0.20f);
        verify(bossBarManager).hideCarrierBar(player);
        verify(bossBarManager).showReturnBar(player, text, 0.40f);
        verify(bossBarManager).hideReturnBar(player);
        verify(bossBarManager).showWaitingBar(player, text, 0.60f);
        verify(bossBarManager).hideWaitingBar(player);
        verify(bossBarManager).showKillBar(player, text);
        verify(bossBarManager).hideKillBar(player);
    }

    @Test
    void missingSenderServiceIsNoOp() {
        Player player = mock(Player.class);
        Component message = Component.text("line");

        assertDoesNotThrow(() -> client.sendActionBar(player, message));
        verifyNoInteractions(player);
    }

    @Test
    void titleComponentOverloadsCreateEquivalentTitles() {
        Player player = mock(Player.class);
        CTFPlayerMetaData container = mock(CTFPlayerMetaData.class);
        registerSender();
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(CTFPlayerMetaData.class, container);

        Component simpleHeader = Component.text("simple");
        Component simpleSub = Component.text("subtitle");
        Title.Times timed = Title.Times.times(Duration.ofMillis(120), Duration.ofMillis(1800), Duration.ofMillis(220));
        Component timedHeader = Component.text("timed");
        Component timedSub = Component.text("subtitle");

        client.sendTitle(player, simpleHeader, simpleSub);
        client.sendTitle(player, timedHeader, timedSub, timed);
        client.broadcastToArenaTitle(simpleHeader, simpleSub);
        client.broadcastToArenaTitle(timedHeader, timedSub, timed);

        verify(player).showTitle(Title.title(simpleHeader, simpleSub));
        verify(player).showTitle(Title.title(timedHeader, timedSub, timed));
        verify(container).broadcastTitle(Title.title(simpleHeader, simpleSub));
        verify(container).broadcastTitle(Title.title(timedHeader, timedSub, timed));
    }

    private void registerSender() {
        DependencyLoader.getFallbackDependencyLoader().registerImportantInstance(BukkitMessageUtil.class, new BukkitMessageUtil());
    }
}
