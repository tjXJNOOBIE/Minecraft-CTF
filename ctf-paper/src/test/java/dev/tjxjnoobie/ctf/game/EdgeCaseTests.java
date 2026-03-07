package dev.tjxjnoobie.ctf.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.bootstrap.PluginBootstrap;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.FlagBlockPlacer;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagPickupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.handlers.CTFCaptureZoneHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.handlers.TeamBaseMetaDataResolver;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.game.flag.metadata.FlagMetaData;
import dev.tjxjnoobie.ctf.game.flag.metadata.TeamBaseMetaData;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.LonePlayerCountdownHandler;
import dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem;
import dev.tjxjnoobie.ctf.items.flag.FlagItem;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * Integration-style edge-case coverage using the real bootstrap and concrete runtime handlers.
 */
class EdgeCaseTests extends TestLogSupport {
    @Test
    void ownFlagTouchCooldownAndDroppedReturnUseRealHandlers() throws Exception {
        try (IntegrationHarness harness = new IntegrationHarness()) {
            logStep("bootstrapped plugin for own-flag touch edge case");
            DependencyLoader loader = harness.loader();
            TeamManager teamManager = loader.requireInstance(TeamManager.class);
            FlagCarrierHandler flagCarrierHandler = loader.requireInstance(FlagCarrierHandler.class);
            FlagStateRegistry flagStateRegistry = loader.requireInstance(FlagStateRegistry.class);
            FlagBlockPlacer flagBlockPlacer = loader.requireInstance(FlagBlockPlacer.class);

            Location redBase = flagStateRegistry.flagFor(TeamId.RED).activeLocation.clone();
            Player redPlayer = harness.createPlayer("EdgeRed", redBase.clone().add(0.5, 0.0, 0.5));
            teamManager.joinTeam(redPlayer, TeamManager.RED);
            logValue("redBase", redBase);

            assertFalse(flagCarrierHandler.processFlagTouch(redPlayer, redBase, true));
            assertFalse(flagCarrierHandler.processFlagTouch(redPlayer, redBase, true));
            assertEquals(1, harness.messagesFor(redPlayer).size());
            logStep("validated own-flag blocked message cooldown");

            Location droppedOwnFlag = redBase.clone().add(4.0, 0.0, 0.0);
            flagStateRegistry.setDropped(TeamId.RED, droppedOwnFlag);
            flagBlockPlacer.placeDroppedFlagBlock(droppedOwnFlag, TeamId.RED);

            assertTrue(flagCarrierHandler.processFlagTouch(redPlayer, droppedOwnFlag, true));
            FlagMetaData redFlag = flagStateRegistry.flagFor(TeamId.RED);
            assertEquals(FlagState.AT_BASE, redFlag.state);
            assertEquals(redBase.getBlockX(), redFlag.activeLocation.getBlockX());
            assertEquals(redBase.getBlockY(), redFlag.activeLocation.getBlockY());
            assertEquals(redBase.getBlockZ(), redFlag.activeLocation.getBlockZ());
            logStep("validated dropped own flag returns to base via real handler path");
        }
    }

    @Test
    void droppingEnemyFlagNearOwnBaseMovesDropOutsideCaptureZone() throws Exception {
        try (IntegrationHarness harness = new IntegrationHarness()) {
            logStep("bootstrapped plugin for dropped-flag safety edge case");
            DependencyLoader loader = harness.loader();
            TeamManager teamManager = loader.requireInstance(TeamManager.class);
            FlagPickupHandler flagPickupHandler = loader.requireInstance(FlagPickupHandler.class);
            FlagDropHandler flagDropHandler = loader.requireInstance(FlagDropHandler.class);
            FlagStateRegistry flagStateRegistry = loader.requireInstance(FlagStateRegistry.class);
            CTFCaptureZoneHandler captureZoneHandler = loader.requireInstance(CTFCaptureZoneHandler.class);
            TeamBaseMetaDataResolver teamBaseMetaDataResolver = loader.requireInstance(TeamBaseMetaDataResolver.class);

            Location redBase = flagStateRegistry.flagFor(TeamId.RED).baseLocation.clone();
            Player redCarrier = harness.createPlayer("DropRed", redBase.clone().add(1.0, 0.0, 0.0));
            teamManager.joinTeam(redCarrier, TeamManager.RED);

            assertTrue(flagPickupHandler.processEnemyFlagPickup(redCarrier, TeamId.BLUE));
            flagDropHandler.dropCarriedFlagIfPresent(redCarrier);

            FlagMetaData blueFlag = flagStateRegistry.flagFor(TeamId.BLUE);
            TeamBaseMetaData redBaseData = teamBaseMetaDataResolver.resolveTeamBaseMetaData(TeamId.RED);
            assertEquals(FlagState.DROPPED, blueFlag.state);
            assertNotNull(blueFlag.activeLocation);
            assertFalse(captureZoneHandler.isInsideCaptureZone(redBaseData, blueFlag.activeLocation));
            logValue("droppedBlueFlag", blueFlag.activeLocation);
            logStep("validated dropped enemy flag is pushed outside the scoring zone");
        }
    }

    @Test
    void enemyFlagCarrierCannotScoreWhileOwnFlagIsAwayFromBase() throws Exception {
        try (IntegrationHarness harness = new IntegrationHarness()) {
            logStep("bootstrapped plugin for blocked-capture edge case");
            DependencyLoader loader = harness.loader();
            TeamManager teamManager = loader.requireInstance(TeamManager.class);
            FlagPickupHandler flagPickupHandler = loader.requireInstance(FlagPickupHandler.class);
            FlagCarrierHandler flagCarrierHandler = loader.requireInstance(FlagCarrierHandler.class);
            FlagStateRegistry flagStateRegistry = loader.requireInstance(FlagStateRegistry.class);
            ScoreBoardManager scoreBoardManager = loader.requireInstance(ScoreBoardManager.class);

            Location redBase = flagStateRegistry.flagFor(TeamId.RED).baseLocation.clone();
            Location blueBase = flagStateRegistry.flagFor(TeamId.BLUE).baseLocation.clone();
            Player redCarrier = harness.createPlayer("BlockedRedCarrier", redBase.clone().add(1.0, 0.0, 0.0));
            Player blueCarrier = harness.createPlayer("BlockedBlueCarrier", blueBase.clone().add(1.0, 0.0, 0.0));
            teamManager.joinTeam(redCarrier, TeamManager.RED);
            teamManager.joinTeam(blueCarrier, TeamManager.BLUE);

            assertTrue(flagPickupHandler.processEnemyFlagPickup(redCarrier, TeamId.BLUE));
            assertTrue(flagPickupHandler.processEnemyFlagPickup(blueCarrier, TeamId.RED));
            logStep("both teams are now carrying the opposing flag");

            Location redCaptureLocation = redBase.clone().add(0.5, 0.0, 0.5);
            flagCarrierHandler.processFlagCarrierMovement(redCarrier, redCaptureLocation, true);

            FlagMetaData blueFlag = flagStateRegistry.flagFor(TeamId.BLUE);
            FlagMetaData redFlag = flagStateRegistry.flagFor(TeamId.RED);
            assertEquals(FlagState.CARRIED, blueFlag.state);
            assertEquals(redCarrier.getUniqueId(), blueFlag.carrier);
            assertEquals(FlagState.CARRIED, redFlag.state);
            assertEquals(blueCarrier.getUniqueId(), redFlag.carrier);
            assertEquals(0, scoreBoardManager.getScore(TeamManager.RED));
            assertEquals(0, scoreBoardManager.getScore(TeamManager.BLUE));
            logStep("validated no score is awarded while the scoring team's own flag is away");
        }
    }

    @Test
    void runningMatchJoinResolutionStillBalancesRequestedTeamChoices() throws Exception {
        try (IntegrationHarness harness = new IntegrationHarness()) {
            logStep("bootstrapped plugin for running-match join balancing edge case");
            DependencyLoader loader = harness.loader();
            TeamManager teamManager = loader.requireInstance(TeamManager.class);
            MatchPlayerSessionHandler sessionHandler = loader.requireInstance(MatchPlayerSessionHandler.class);
            GameStateManager gameStateManager = loader.requireInstance(GameStateManager.class);

            Player redAnchor = harness.createPlayer("BalanceRed", new Location(harness.world(), 2.5, 64.0, 2.5));
            teamManager.joinTeam(redAnchor, TeamManager.RED);
            gameStateManager.setGameState(GameState.IN_PROGRESS);

            assertEquals(TeamManager.BLUE, sessionHandler.resolveJoinTeamKey(TeamManager.RED));
            assertEquals(TeamManager.BLUE, sessionHandler.resolveJoinTeamKey(null));
            logStep("validated running match join resolution still forces balancing");
        }
    }

    @Test
    void lonePlayerCountdownStartsAndCancelsWhenRosterRecovers() throws Exception {
        try (IntegrationHarness harness = new IntegrationHarness()) {
            logStep("bootstrapped plugin for lone-player countdown recovery edge case");
            TeamManager teamManager = harness.loader().requireInstance(TeamManager.class);
            Player red = harness.createPlayer("LoneRed", new Location(harness.world(), 2.5, 64.0, 2.5));
            Player blue = harness.createPlayer("LoneBlue", new Location(harness.world(), 12.5, 64.0, 2.5));
            teamManager.joinTeam(red, TeamManager.RED);
            teamManager.joinTeam(blue, TeamManager.BLUE);

            AtomicBoolean running = new AtomicBoolean(true);
            AtomicInteger stopRequests = new AtomicInteger(0);
            List<Component> broadcasts = new ArrayList<>();
            LonePlayerCountdownHandler handler = new LonePlayerCountdownHandler(
                teamManager,
                running::get,
                stopRequests::incrementAndGet,
                broadcasts::add
            );

            teamManager.leaveTeam(blue);
            handler.onPlayerLeftDuringMatch();
            assertNotNull(readLonePlayerTask(handler));
            assertEquals(1, broadcasts.size());
            logStep("validated lone-player countdown start");

            Player rescuer = harness.createPlayer("RescueBlue", new Location(harness.world(), 12.5, 64.0, 3.5));
            teamManager.joinTeam(rescuer, TeamManager.BLUE);
            handler.onPlayerJoinedDuringMatch();

            assertNull(readLonePlayerTask(handler));
            assertEquals(2, broadcasts.size());
            assertEquals(0, stopRequests.get());
            logStep("validated lone-player countdown cancellation on roster recovery");
        }
    }

    @Test
    void emptyRosterRequestsImmediateMatchStopWithoutCountdown() throws Exception {
        try (IntegrationHarness harness = new IntegrationHarness()) {
            logStep("bootstrapped plugin for empty-roster stop edge case");
            TeamManager teamManager = harness.loader().requireInstance(TeamManager.class);
            Player red = harness.createPlayer("SoloRed", new Location(harness.world(), 2.5, 64.0, 2.5));
            teamManager.joinTeam(red, TeamManager.RED);

            AtomicBoolean running = new AtomicBoolean(true);
            AtomicInteger stopRequests = new AtomicInteger(0);
            List<Component> broadcasts = new ArrayList<>();
            LonePlayerCountdownHandler handler = new LonePlayerCountdownHandler(
                teamManager,
                running::get,
                stopRequests::incrementAndGet,
                broadcasts::add
            );

            teamManager.leaveTeam(red);
            handler.onPlayerLeftDuringMatch();

            assertEquals(1, stopRequests.get());
            assertNull(readLonePlayerTask(handler));
            assertTrue(broadcasts.isEmpty());
            logStep("validated immediate stop when the arena roster becomes empty");
        }
    }

    private Object readLonePlayerTask(LonePlayerCountdownHandler handler) throws Exception {
        java.lang.reflect.Field field = LonePlayerCountdownHandler.class.getDeclaredField("lonePlayerEndTimerTask");
        field.setAccessible(true);
        return field.get(handler);
    }

    /**
     * Real bootstrap harness with stateful world blocks and online-player registry support.
     */
    private static final class IntegrationHarness implements AutoCloseable {
        private final Main plugin;
        private final World world;
        private final PluginBootstrap bootstrap;
        private final Path dataDir;
        private final Path worldContainer;
        private final MockedConstruction<FlagIndicatorItem> flagIndicatorItemConstruction;
        private final MockedConstruction<FlagItem> flagItemConstruction;
        private final Map<String, Player> onlinePlayersByName = new HashMap<>();
        private final Map<UUID, Player> onlinePlayersById = new HashMap<>();
        private final Map<UUID, List<Component>> messagesByPlayerId = new HashMap<>();
        private final Map<String, BlockState> blocksByKey = new HashMap<>();
        private final Map<UUID, Entity> spawnedEntitiesById = new HashMap<>();

        private IntegrationHarness() throws Exception {
            plugin = Mockito.mock(Main.class);
            PluginManager pluginManager = Mockito.mock(PluginManager.class);
            world = Mockito.mock(World.class);
            dataDir = Files.createTempDirectory("ctf-edge-data");
            worldContainer = Files.createTempDirectory("ctf-edge-worlds");
            Path worldDir = worldContainer.resolve("CTFMap");
            Files.createDirectories(worldDir);
            Files.writeString(worldDir.resolve("level.dat"), "edge", StandardCharsets.UTF_8);

            copyBundledResource("config.yml");
            copyBundledResource("messages.yml");
            copyBundledResource("ctf-spawns.yml");
            copyBundledResource("flag-locations.yml");

            YamlConfiguration liveConfig = YamlConfiguration.loadConfiguration(dataDir.resolve("config.yml").toFile());
            when(plugin.getDataFolder()).thenReturn(dataDir.toFile());
            when(plugin.getConfig()).thenReturn(liveConfig);
            when(plugin.getServer()).thenReturn(Bukkit.getServer());
            when(plugin.getName()).thenReturn("ctf-edge");
            when(plugin.getCommand("ctf")).thenReturn(null);
            when(plugin.getResource(any(String.class))).thenAnswer(invocation -> {
                String name = invocation.getArgument(0, String.class);
                return getClass().getClassLoader().getResourceAsStream(name);
            });

            when(Bukkit.getServer().getPluginManager()).thenReturn(pluginManager);
            when(Bukkit.getServer().getWorldContainer()).thenReturn(worldContainer.toFile());
            when(Bukkit.getServer().getWorld(Mockito.anyString())).thenReturn(world);
            when(Bukkit.getServer().getWorlds()).thenReturn(List.of(world));
            when(Bukkit.getServer().createWorld(Mockito.any(org.bukkit.WorldCreator.class))).thenReturn(world);
            when(Bukkit.getServer().getOnlinePlayers()).thenAnswer(invocation -> onlinePlayers());
            when(Bukkit.getServer().getPlayer(Mockito.anyString())).thenAnswer(invocation ->
                onlinePlayersByName.get(invocation.getArgument(0, String.class)));
            when(Bukkit.getServer().getPlayer(Mockito.any(UUID.class))).thenAnswer(invocation ->
                onlinePlayersById.get(invocation.getArgument(0, UUID.class)));
            when(Bukkit.getEntity(Mockito.any(UUID.class))).thenAnswer(invocation ->
                spawnedEntitiesById.get(invocation.getArgument(0, UUID.class)));

            org.bukkit.scoreboard.ScoreboardManager scoreboardManager = Bukkit.getServer().getScoreboardManager();
            org.bukkit.scoreboard.Scoreboard dynamicScoreboard = mock(org.bukkit.scoreboard.Scoreboard.class);
            org.bukkit.scoreboard.Objective dynamicObjective = mock(org.bukkit.scoreboard.Objective.class);
            org.bukkit.scoreboard.Score dynamicScore = mock(org.bukkit.scoreboard.Score.class);
            Map<String, org.bukkit.scoreboard.Team> dynamicTeams = new HashMap<>();
            when(scoreboardManager.getNewScoreboard()).thenReturn(dynamicScoreboard);
            when(dynamicScoreboard.getObjective(any(String.class))).thenReturn(dynamicObjective);
            when(dynamicObjective.getScore(any(String.class))).thenReturn(dynamicScore);
            when(dynamicScoreboard.getTeam(any(String.class))).thenAnswer(invocation ->
                dynamicTeams.get(invocation.getArgument(0, String.class)));
            when(dynamicScoreboard.registerNewTeam(any(String.class))).thenAnswer(invocation -> {
                String teamName = invocation.getArgument(0, String.class);
                org.bukkit.scoreboard.Team team = mock(org.bukkit.scoreboard.Team.class);
                Set<String> entries = new HashSet<>();
                when(team.getEntries()).thenReturn(entries);
                when(team.hasEntry(any(String.class))).thenAnswer(hasEntryInvocation ->
                    entries.contains(hasEntryInvocation.getArgument(0, String.class)));
                doAnswer(addEntryInvocation -> entries.add(addEntryInvocation.getArgument(0, String.class)))
                    .when(team).addEntry(any(String.class));
                doAnswer(removeEntryInvocation -> entries.remove(removeEntryInvocation.getArgument(0, String.class)))
                    .when(team).removeEntry(any(String.class));
                dynamicTeams.put(teamName, team);
                return team;
            });

            when(world.getName()).thenReturn("CTFMap");
            when(world.getSpawnLocation()).thenReturn(new Location(world, 0.5, 65.0, 0.5));
            when(world.getHighestBlockYAt(Mockito.anyInt(), Mockito.anyInt())).thenReturn(65);
            when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(invocation ->
                blockAt(
                    invocation.getArgument(0, Integer.class),
                    invocation.getArgument(1, Integer.class),
                    invocation.getArgument(2, Integer.class)
                ));
            when(world.getBlockAt(any(Location.class))).thenAnswer(invocation -> {
                Location location = invocation.getArgument(0, Location.class);
                return blockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            });
            when(world.getEntitiesByClass(ItemDisplay.class)).thenAnswer(invocation -> spawnedEntitiesById.values()
                .stream()
                .filter(ItemDisplay.class::isInstance)
                .map(ItemDisplay.class::cast)
                .toList());
            doAnswer(invocation -> {
                Location spawnLocation = invocation.getArgument(0, Location.class);
                ItemDisplay itemDisplay = mock(ItemDisplay.class);
                UUID itemDisplayId = UUID.randomUUID();
                Set<String> scoreboardTags = new HashSet<>();
                AtomicReference<Location> itemLocation = new AtomicReference<>(spawnLocation);
                AtomicReference<ItemStack> itemStack = new AtomicReference<>();
                AtomicBoolean valid = new AtomicBoolean(true);

                when(itemDisplay.getUniqueId()).thenReturn(itemDisplayId);
                when(itemDisplay.getLocation()).thenAnswer(displayInvocation -> itemLocation.get());
                when(itemDisplay.getScoreboardTags()).thenReturn(scoreboardTags);
                when(itemDisplay.getItemStack()).thenAnswer(displayInvocation -> itemStack.get());
                when(itemDisplay.isValid()).thenAnswer(displayInvocation -> valid.get());
                doAnswer(setItemInvocation -> {
                    itemStack.set(setItemInvocation.getArgument(0, ItemStack.class));
                    return null;
                }).when(itemDisplay).setItemStack(any(ItemStack.class));
                doAnswer(addTagInvocation -> scoreboardTags.add(addTagInvocation.getArgument(0, String.class)))
                    .when(itemDisplay).addScoreboardTag(any(String.class));
                doAnswer(removeInvocation -> {
                    valid.set(false);
                    spawnedEntitiesById.remove(itemDisplayId);
                    return null;
                }).when(itemDisplay).remove();

                spawnedEntitiesById.put(itemDisplayId, itemDisplay);
                return itemDisplay;
            }).when(world).spawn(any(Location.class), Mockito.eq(ItemDisplay.class));

            flagIndicatorItemConstruction = Mockito.mockConstruction(
                FlagIndicatorItem.class,
                (mocked, context) -> {
                    ItemStack indicatorItem = mock(ItemStack.class);
                    when(indicatorItem.getType()).thenReturn(Material.EMERALD);
                    when(mocked.create()).thenReturn(indicatorItem);
                }
            );
            flagItemConstruction = Mockito.mockConstruction(
                FlagItem.class,
                (mocked, context) -> {
                    Material material = context.arguments().isEmpty()
                        ? Material.WHITE_WOOL
                        : (Material) context.arguments().get(0);
                    ItemStack flagItem = mock(ItemStack.class);
                    when(flagItem.getType()).thenReturn(material);
                    when(mocked.create()).thenReturn(flagItem);
                }
            );

            bootstrap = new PluginBootstrap(plugin);
            bootstrap.onEnable();
            bootstrap.getDependencyLoader().replaceInstance(CarrierEffects.class, () -> mock(CarrierEffects.class));
            bootstrap.getDependencyLoader().replaceInstance(FlagEventEffects.class, () -> mock(FlagEventEffects.class));
        }

        private void copyBundledResource(String name) throws Exception {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
                assertNotNull(stream);
                Files.copy(stream, dataDir.resolve(name));
            }
        }

        private Collection<? extends Player> onlinePlayers() {
            return new ArrayList<>(onlinePlayersByName.values());
        }

        private Block blockAt(int x, int y, int z) {
            String key = x + ":" + y + ":" + z;
            BlockState existing = blocksByKey.get(key);
            if (existing != null) {
                return existing.block;
            }

            Location blockLocation = new Location(world, x, y, z);
            Block block = mock(Block.class);
            AtomicReference<Material> type = new AtomicReference<>(Material.AIR);
            Map<String, List<MetadataValue>> metadataByKey = new HashMap<>();

            when(block.getLocation()).thenReturn(blockLocation);
            when(block.getType()).thenAnswer(invocation -> type.get());
            when(block.getMetadata(any(String.class))).thenAnswer(invocation -> {
                String metadataKey = invocation.getArgument(0, String.class);
                return new ArrayList<>(metadataByKey.getOrDefault(metadataKey, List.of()));
            });
            doAnswer(invocation -> {
                type.set(invocation.getArgument(0, Material.class));
                return null;
            }).when(block).setType(any(Material.class));
            doAnswer(invocation -> {
                String metadataKey = invocation.getArgument(0, String.class);
                MetadataValue value = invocation.getArgument(1, MetadataValue.class);
                metadataByKey.computeIfAbsent(metadataKey, ignored -> new ArrayList<>()).add(value);
                return null;
            }).when(block).setMetadata(any(String.class), any(MetadataValue.class));
            doAnswer(invocation -> {
                String metadataKey = invocation.getArgument(0, String.class);
                org.bukkit.plugin.Plugin owner = invocation.getArgument(1, org.bukkit.plugin.Plugin.class);
                List<MetadataValue> values = metadataByKey.get(metadataKey);
                if (values == null) {
                    return null;
                }
                values.removeIf(value -> value.getOwningPlugin() == owner);
                if (values.isEmpty()) {
                    metadataByKey.remove(metadataKey);
                }
                return null;
            }).when(block).removeMetadata(any(String.class), any(org.bukkit.plugin.Plugin.class));

            blocksByKey.put(key, new BlockState(block));
            return block;
        }

        private Player createPlayer(String name, Location initialLocation) {
            Player player = mock(Player.class);
            PlayerInventory inventory = mock(PlayerInventory.class);
            UUID playerId = UUID.randomUUID();
            AtomicReference<Location> locationRef = new AtomicReference<>(initialLocation);
            Map<Integer, ItemStack> inventorySlots = new HashMap<>();

            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getName()).thenReturn(name);
            when(player.displayName()).thenReturn(Component.text(name));
            when(player.isOnline()).thenReturn(true);
            when(player.getWorld()).thenReturn(world);
            when(player.getLocation()).thenAnswer(invocation -> locationRef.get());
            when(player.getInventory()).thenReturn(inventory);
            when(player.hasPermission(any(String.class))).thenReturn(false);
            doAnswer(invocation -> {
                locationRef.set(invocation.getArgument(0, Location.class));
                return true;
            }).when(player).teleport(any(Location.class));
            doAnswer(invocation -> {
                messagesByPlayerId.computeIfAbsent(playerId, ignored -> new ArrayList<>())
                    .add(invocation.getArgument(0, Component.class));
                return null;
            }).when(player).sendMessage(any(Component.class));
            doAnswer(invocation -> null).when(player).playSound(any(Location.class), any(org.bukkit.Sound.class), Mockito.anyFloat(), Mockito.anyFloat());
            doAnswer(invocation -> null).when(player).spawnParticle(any(org.bukkit.Particle.class), any(Location.class), Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble());
            doAnswer(invocation -> null).when(player).showEntity(any(org.bukkit.plugin.Plugin.class), any(Entity.class));
            doAnswer(invocation -> null).when(player).hideEntity(any(org.bukkit.plugin.Plugin.class), any(Entity.class));
            doAnswer(invocation -> null).when(player).setGlowing(Mockito.anyBoolean());

            when(inventory.getHeldItemSlot()).thenReturn(0);
            when(inventory.getItem(Mockito.anyInt())).thenAnswer(invocation ->
                inventorySlots.get(invocation.getArgument(0, Integer.class)));
            when(inventory.getItemInMainHand()).thenAnswer(invocation -> inventorySlots.get(0));
            doAnswer(invocation -> {
                inventorySlots.put(invocation.getArgument(0, Integer.class), invocation.getArgument(1, ItemStack.class));
                return null;
            }).when(inventory).setItem(Mockito.anyInt(), any(ItemStack.class));
            doAnswer(invocation -> {
                inventorySlots.clear();
                return null;
            }).when(inventory).clear();
            doAnswer(invocation -> null).when(inventory).setArmorContents(any(ItemStack[].class));

            onlinePlayersByName.put(name, player);
            onlinePlayersById.put(playerId, player);
            return player;
        }

        private List<Component> messagesFor(Player player) {
            return messagesByPlayerId.getOrDefault(player.getUniqueId(), List.of());
        }

        private DependencyLoader loader() {
            return bootstrap.getDependencyLoader();
        }

        private World world() {
            return world;
        }

        @Override
        public void close() throws Exception {
            bootstrap.onDisable();
            flagItemConstruction.close();
            flagIndicatorItemConstruction.close();
        }

        private record BlockState(Block block) {
        }
    }
}
