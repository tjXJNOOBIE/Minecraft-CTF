package dev.tjxjnoobie.ctf.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.TestLogSupport;
import dev.tjxjnoobie.ctf.bootstrap.PluginBootstrap;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.flag.CarrierEffects;
import dev.tjxjnoobie.ctf.game.flag.FlagDropHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagPickupHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagReturnHandler;
import dev.tjxjnoobie.ctf.game.flag.FlagStateRegistry;
import dev.tjxjnoobie.ctf.game.flag.effects.FlagEventEffects;
import dev.tjxjnoobie.ctf.game.flag.handlers.FlagCarrierHandler;
import dev.tjxjnoobie.ctf.game.flag.tags.FlagState;
import dev.tjxjnoobie.ctf.game.lifecycle.handlers.MatchCleanupHandler;
import dev.tjxjnoobie.ctf.game.player.effects.PlayerDeathEffects;
import dev.tjxjnoobie.ctf.items.flag.FlagItem;
import dev.tjxjnoobie.ctf.items.flag.FlagIndicatorItem;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.items.kit.SpearLockedPlaceholderItem;
import dev.tjxjnoobie.ctf.items.kit.SpearReturningPlaceholderItem;
import dev.tjxjnoobie.ctf.game.player.metadata.PlayerMatchStats;
import dev.tjxjnoobie.ctf.game.player.handlers.PlayerRespawnScheduler;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.state.GameState;
import dev.tjxjnoobie.ctf.game.state.GameStateManager;
import dev.tjxjnoobie.ctf.kit.KitSlots;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;
import dev.tjxjnoobie.ctf.kit.tags.KitType;
import dev.tjxjnoobie.ctf.scoreboard.ScoreBoardManager;
import dev.tjxjnoobie.ctf.team.TeamId;
import dev.tjxjnoobie.ctf.team.TeamManager;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class FullPluginSimulationTest extends TestLogSupport {
    @Test
    void fullPluginSimulationLoadsRealConfigsAndRunsInlineThreeVsThreeMatch() throws Exception {
        logStep("arranging full inline 3v3 simulation with live config/bootstrap wiring"); // Mark the start of the end-to-end simulation setup
        Main plugin = Mockito.mock(Main.class); // Mock the plugin entrypoint passed into bootstrap
        PluginManager pluginManager = Mockito.mock(PluginManager.class); // Mock Bukkit's plugin manager dependency
        org.bukkit.World world = Mockito.mock(org.bukkit.World.class); // Mock the world loaded for the match map
        Block worldBlock = Mockito.mock(Block.class); // Mock world blocks queried during spawn and flag checks
        MockedConstruction<FlagIndicatorItem> flagIndicatorItemConstruction = Mockito.mockConstruction(
            FlagIndicatorItem.class,
            (mocked, context) -> {
                ItemStack indicatorItem = mock(ItemStack.class); // Build a synthetic indicator item instance
                when(indicatorItem.getType()).thenReturn(Material.EMERALD); // Expose the expected indicator material
                when(mocked.create()).thenReturn(indicatorItem); // Return the mocked indicator whenever constructed
            }
        );
        MockedConstruction<FlagItem> flagItemConstruction = Mockito.mockConstruction(
            FlagItem.class,
            (mocked, context) -> {
                Material material = context.arguments().isEmpty() // Preserve the requested wool color when the flag item is created
                    ? Material.WHITE_WOOL
                    : (Material) context.arguments().get(0);
                ItemStack flagItem = mock(ItemStack.class); // Build a synthetic flag item instance
                when(flagItem.getType()).thenReturn(material); // Surface the flag's configured material to consumers
                when(mocked.create()).thenReturn(flagItem); // Return the mocked flag item whenever constructed
            }
        );
        MockedConstruction<CarrierEffects> carrierEffectsConstruction = Mockito.mockConstruction(CarrierEffects.class); // Stub carrier side effects
        MockedConstruction<FlagEventEffects> flagEventEffectsConstruction = Mockito.mockConstruction(FlagEventEffects.class); // Stub flag event visuals and sounds
        MockedConstruction<PlayerDeathEffects> playerDeathEffectsConstruction = Mockito.mockConstruction(PlayerDeathEffects.class); // Stub death effect emission
        MockedConstruction<MatchCleanupHandler> matchCleanupHandlerConstruction = Mockito.mockConstruction(MatchCleanupHandler.class); // Intercept cleanup handler creation
        MockedConstruction<PlayerRespawnScheduler> playerRespawnSchedulerConstruction = Mockito.mockConstruction(PlayerRespawnScheduler.class); // Intercept respawn scheduling
        MockedConstruction<HomingSpearItem> homingSpearItemConstruction = Mockito.mockConstruction(
            HomingSpearItem.class,
            (mocked, context) -> {
                ItemStack spearItem = mock(ItemStack.class); // Build a mocked spear item for ranger loadouts
                ItemMeta spearMeta = mock(ItemMeta.class); // Build metadata consumed by spear matching logic
                when(spearItem.getType()).thenReturn(Material.TRIDENT); // Present the spear as a trident item
                when(spearItem.getItemMeta()).thenReturn(spearMeta); // Return the mocked spear metadata
                when(spearMeta.hasCustomModelData()).thenReturn(true); // Report custom model data as present
                when(spearMeta.getCustomModelData()).thenReturn(HomingSpearItem.CUSTOM_MODEL_DATA); // Match the spear model id
                when(spearMeta.displayName()).thenReturn(Component.text(HomingSpearItem.NAME_TEXT)); // Match the spear display name
                when(mocked.create()).thenReturn(spearItem); // Return the mocked spear on creation
                when(mocked.matches(any(ItemStack.class))).thenAnswer(invocation -> {
                    ItemStack item = invocation.getArgument(0, ItemStack.class); // Inspect the item passed into the matcher
                    if (item == null || item.getType() != Material.TRIDENT) {
                        return false; // Reject nulls and non-trident items immediately
                    }
                    ItemMeta meta = item.getItemMeta(); // Read metadata for custom spear verification
                    return meta != null
                        && meta.hasCustomModelData()
                        && meta.getCustomModelData() == HomingSpearItem.CUSTOM_MODEL_DATA
                        && meta.displayName() != null;
                });
            }
        );
        MockedConstruction<SpearLockedPlaceholderItem> spearLockedPlaceholderItemConstruction = Mockito.mockConstruction(
            SpearLockedPlaceholderItem.class,
            (mocked, context) -> {
                ItemStack placeholderItem = mock(ItemStack.class); // Build the locked-spear placeholder item
                ItemMeta placeholderMeta = mock(ItemMeta.class); // Build placeholder metadata for name checks
                when(placeholderItem.getType()).thenReturn(Material.BARRIER); // Present the placeholder as a barrier
                when(placeholderItem.getItemMeta()).thenReturn(placeholderMeta); // Return the placeholder metadata
                when(placeholderMeta.displayName()).thenReturn(SpearLockedPlaceholderItem.NAME); // Match the locked placeholder name
                when(mocked.create()).thenReturn(placeholderItem); // Return the mocked locked placeholder
            }
        );
        MockedConstruction<SpearReturningPlaceholderItem> spearReturningPlaceholderItemConstruction = Mockito.mockConstruction(
            SpearReturningPlaceholderItem.class,
            (mocked, context) -> {
                ItemStack placeholderItem = mock(ItemStack.class); // Build the returning-spear placeholder item
                ItemMeta placeholderMeta = mock(ItemMeta.class); // Build placeholder metadata for name checks
                when(placeholderItem.getType()).thenReturn(Material.BARRIER); // Present the placeholder as a barrier
                when(placeholderItem.getItemMeta()).thenReturn(placeholderMeta); // Return the placeholder metadata
                when(placeholderMeta.displayName()).thenReturn(SpearReturningPlaceholderItem.NAME); // Match the returning placeholder name
                when(mocked.create()).thenReturn(placeholderItem); // Return the mocked returning placeholder
            }
        );

        Path dataDir = Files.createTempDirectory("ctf-full-sim-data"); // Create an isolated plugin data folder
        Path worldContainer = Files.createTempDirectory("ctf-full-sim-world"); // Create an isolated Bukkit world container
        Path worldDir = worldContainer.resolve("CTFMap"); // Point the fake world container at the expected map name
        Files.createDirectories(worldDir); // Materialize the mocked world directory
        Files.writeString(worldDir.resolve("level.dat"), "sim", StandardCharsets.UTF_8); // Seed a minimal world file so map loading passes

        try (InputStream configStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(configStream); // Ensure the bundled config resource is available
            Files.copy(configStream, dataDir.resolve("config.yml")); // Copy the live config into the temp data folder
        }
        try (InputStream messagesStream = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
            assertNotNull(messagesStream); // Ensure the bundled messages resource is available
            Files.copy(messagesStream, dataDir.resolve("messages.yml")); // Copy the live messages file into the temp data folder
        }
        try (InputStream spawnsStream = getClass().getClassLoader().getResourceAsStream("ctf-spawns.yml")) {
            assertNotNull(spawnsStream); // Ensure the bundled spawns resource is available
            Files.copy(spawnsStream, dataDir.resolve("ctf-spawns.yml")); // Copy the live spawn definitions into the temp data folder
        }
        try (InputStream flagsStream = getClass().getClassLoader().getResourceAsStream("flag-locations.yml")) {
            assertNotNull(flagsStream); // Ensure the bundled flag locations resource is available
            Files.copy(flagsStream, dataDir.resolve("flag-locations.yml")); // Copy the live flag layout into the temp data folder
        }

        YamlConfiguration liveConfig = YamlConfiguration.loadConfiguration(dataDir.resolve("config.yml").toFile()); // Load the copied config exactly as the plugin would
        when(plugin.getDataFolder()).thenReturn(dataDir.toFile()); // Point plugin data access at the temp folder
        when(plugin.getConfig()).thenReturn(liveConfig); // Return the live parsed config to bootstrap
        when(plugin.getServer()).thenReturn(Bukkit.getServer()); // Route plugin server calls through the mocked Bukkit server
        when(plugin.getName()).thenReturn("ctf-sim"); // Provide a stable plugin name for registrations
        when(plugin.getCommand("ctf")).thenReturn(null); // Skip command wiring because this test exercises gameplay only
        when(plugin.getResource(any(String.class))).thenAnswer(invocation -> {
            String name = invocation.getArgument(0, String.class); // Read whichever bundled resource bootstrap requests
            return getClass().getClassLoader().getResourceAsStream(name); // Stream resources from the test classpath
        });

        when(Bukkit.getServer().getPluginManager()).thenReturn(pluginManager); // Route plugin manager lookups to the mock
        when(Bukkit.getServer().getWorldContainer()).thenReturn(worldContainer.toFile()); // Point world loading at the temp world container
        when(Bukkit.getServer().getWorld(Mockito.anyString())).thenReturn(world); // Return the mocked world for direct lookups
        when(Bukkit.getServer().getWorlds()).thenReturn(List.of(world)); // Expose a single loaded world to bootstrap
        when(Bukkit.getServer().createWorld(Mockito.any(org.bukkit.WorldCreator.class))).thenReturn(world); // Return the mocked world for lazy creation
        logStep("bootstrapping plugin with resource-backed configs"); // Mark the transition into real bootstrap execution
        org.bukkit.scoreboard.ScoreboardManager bukkitScoreboardManager = Bukkit.getServer().getScoreboardManager(); // Reuse Bukkit's mocked scoreboard manager
        org.bukkit.scoreboard.Scoreboard dynamicScoreboard = mock(org.bukkit.scoreboard.Scoreboard.class); // Build a scoreboard instance for the match
        org.bukkit.scoreboard.Objective dynamicObjective = mock(org.bukkit.scoreboard.Objective.class); // Build a scoreboard objective for scores
        org.bukkit.scoreboard.Score dynamicScore = mock(org.bukkit.scoreboard.Score.class); // Build a score entry returned by the objective
        Map<String, org.bukkit.scoreboard.Team> dynamicTeams = new java.util.HashMap<>(); // Track registered teams by name inside the mock scoreboard
        when(bukkitScoreboardManager.getNewScoreboard()).thenReturn(dynamicScoreboard); // Return the mocked scoreboard when match boards are created
        when(dynamicScoreboard.getObjective(any(String.class))).thenReturn(dynamicObjective); // Return the mocked objective for any objective lookup
        when(dynamicObjective.getScore(any(String.class))).thenReturn(dynamicScore); // Return the mocked score entry for any entry lookup
        when(dynamicScoreboard.getTeam(any(String.class))).thenAnswer(invocation -> {
            String teamName = invocation.getArgument(0, String.class); // Read the requested team name
            return dynamicTeams.get(teamName); // Return the tracked mock team if it exists
        });
        when(dynamicScoreboard.registerNewTeam(any(String.class))).thenAnswer(invocation -> {
            String teamName = invocation.getArgument(0, String.class); // Read the team name being registered
            org.bukkit.scoreboard.Team dynamicTeam = mock(org.bukkit.scoreboard.Team.class); // Build a new scoreboard team mock
            java.util.Set<String> entries = new java.util.HashSet<>(); // Track team entries locally for assertions and lookups
            when(dynamicTeam.getEntries()).thenReturn(entries); // Surface the tracked entries set from the mock
            when(dynamicTeam.hasEntry(any(String.class))).thenAnswer(hasEntryInvocation -> {
                String entry = hasEntryInvocation.getArgument(0, String.class); // Read the entry being checked
                return entries.contains(entry); // Return whether the entry is on the tracked team roster
            });
            doAnswer(addEntryInvocation -> entries.add(addEntryInvocation.getArgument(0, String.class)))
                .when(dynamicTeam)
                .addEntry(any(String.class));
            doAnswer(removeEntryInvocation -> entries.remove(removeEntryInvocation.getArgument(0, String.class)))
                .when(dynamicTeam)
                .removeEntry(any(String.class));
            dynamicTeams.put(teamName, dynamicTeam); // Persist the registered team in the scoreboard map
            return dynamicTeam; // Return the newly registered team mock
        });
        when(world.getName()).thenReturn("CTFMap"); // Match the world name referenced by the config
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0.5, 65.0, 0.5)); // Provide a safe fallback spawn location
        when(world.getHighestBlockYAt(Mockito.anyInt(), Mockito.anyInt())).thenReturn(65); // Keep highest-block checks on solid ground
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(worldBlock); // Route coordinate block lookups to the same mock block
        when(world.getBlockAt(any(Location.class))).thenReturn(worldBlock); // Route location block lookups to the same mock block
        when(worldBlock.getType()).thenReturn(Material.WHITE_WOOL); // Treat looked-up blocks as solid wool blocks
        Map<UUID, Entity> spawnedEntitiesById = new java.util.HashMap<>(); // Track spawned display entities by UUID for later lookups
        when(Bukkit.getEntity(any(UUID.class))).thenAnswer(invocation -> {
            UUID entityId = invocation.getArgument(0, UUID.class); // Read the requested entity id
            return spawnedEntitiesById.get(entityId); // Return the tracked spawned entity if it exists
        });
        when(world.getEntitiesByClass(ItemDisplay.class)).thenAnswer(invocation -> spawnedEntitiesById.values()
            .stream()
            .filter(ItemDisplay.class::isInstance)
            .map(ItemDisplay.class::cast)
            .toList()); // Surface only live item displays from the tracked spawned entities
        doAnswer(invocation -> {
            Location spawnLocation = invocation.getArgument(0, Location.class); // Capture where the item display is spawned
            ItemDisplay itemDisplay = mock(ItemDisplay.class); // Build the spawned item display mock
            UUID itemDisplayId = UUID.randomUUID(); // Assign a synthetic entity id to the display
            java.util.Set<String> itemDisplayTags = new java.util.HashSet<>(); // Track scoreboard tags on the display
            when(itemDisplay.getUniqueId()).thenReturn(itemDisplayId); // Expose the synthetic entity id
            when(itemDisplay.isValid()).thenReturn(true); // Report the spawned display as alive
            when(itemDisplay.getLocation()).thenReturn(spawnLocation); // Preserve the spawn location for flag checks
            when(itemDisplay.getScoreboardTags()).thenReturn(itemDisplayTags); // Expose the tracked scoreboard tags
            doAnswer(addTagInvocation -> itemDisplayTags.add(addTagInvocation.getArgument(0, String.class)))
                .when(itemDisplay)
                .addScoreboardTag(any(String.class));
            doAnswer(removeInvocation -> {
                spawnedEntitiesById.remove(itemDisplayId); // Drop the entity from the registry on removal
                return null; // Match Bukkit's void remove contract
            }).when(itemDisplay).remove();
            spawnedEntitiesById.put(itemDisplayId, itemDisplay); // Register the spawned display for global lookups
            return itemDisplay; // Return the spawned display to the caller
        }).when(world).spawn(any(Location.class), Mockito.eq(ItemDisplay.class));

        PluginBootstrap bootstrap = new PluginBootstrap(plugin); // Create the real bootstrap against the mocked plugin shell
        bootstrap.onEnable(); // Execute the actual plugin enable flow
        logStep("plugin enabled and dependency graph loaded"); // Mark successful bootstrap completion

        DependencyLoader loader = bootstrap.getDependencyLoader(); // Access the live dependency graph built during enable
        TeamManager teamManager = loader.requireInstance(TeamManager.class); // Resolve team orchestration services
        MatchPlayerSessionHandler sessionHandler = loader.requireInstance(MatchPlayerSessionHandler.class); // Resolve player session and stats tracking
        GameStateManager gameStateManager = loader.requireInstance(GameStateManager.class); // Resolve match state management
        ScoreBoardManager scoreBoardManager = loader.requireInstance(ScoreBoardManager.class); // Resolve score tracking
        FlagPickupHandler flagPickupHandler = loader.requireInstance(FlagPickupHandler.class); // Resolve enemy flag pickup logic
        FlagDropHandler flagDropHandler = loader.requireInstance(FlagDropHandler.class); // Resolve forced flag drop logic
        FlagReturnHandler flagReturnHandler = loader.requireInstance(FlagReturnHandler.class); // Resolve dropped flag return logic
        FlagCarrierHandler flagCarrierHandler = loader.requireInstance(FlagCarrierHandler.class); // Resolve scoring via carrier movement
        FlagStateRegistry flagStateRegistry = loader.requireInstance(FlagStateRegistry.class); // Resolve flag state inspection support
        KitSelectionHandler kitSelectionHandler = loader.requireInstance(KitSelectionHandler.class); // Resolve kit selection logic
        ScoutTaggerAbility scoutTaggerAbility = loader.requireInstance(ScoutTaggerAbility.class); // Resolve the scout tagger ability handler
        HomingSpearAbilityHandler homingSpearAbility = loader.requireInstance(HomingSpearAbilityHandler.class); // Resolve the ranger spear ability handler
        MatchCleanupHandler matchCleanupHandler = loader.requireInstance(MatchCleanupHandler.class); // Resolve cleanup orchestration
        doAnswer(invocation -> {
            gameStateManager.setCleanupInProgress(true); // Simulate the cleanup flag flipping when the match stops
            return null; // Match the handler's void contract
        }).when(matchCleanupHandler).requestMatchStop(any(), any(String.class));
        logStep("resolved live handlers from dependency loader"); // Mark successful live dependency resolution

        assertTrue(teamManager.getSpawn(TeamId.RED).isPresent()); // Confirm the red spawn loaded from config
        assertTrue(teamManager.getSpawn(TeamId.BLUE).isPresent()); // Confirm the blue spawn loaded from config
        assertTrue(!teamManager.getReturnPoints(TeamId.RED).isEmpty()); // Confirm the red capture point loaded from config
        assertTrue(!teamManager.getReturnPoints(TeamId.BLUE).isEmpty()); // Confirm the blue capture point loaded from config
        assertEquals(3.0, teamManager.getSpawnConfigHandler().getCaptureRadius(0.0)); // Confirm the expected capture radius from bundled config
        logStep("verified bundled match config and spawn data"); // Mark config validation completion

        Player red1 = mock(Player.class); // Mock the first red player
        Player red2 = mock(Player.class); // Mock the second red player
        Player red3 = mock(Player.class); // Mock the third red player
        Player blue1 = mock(Player.class); // Mock the first blue player
        Player blue2 = mock(Player.class); // Mock the second blue player
        Player blue3 = mock(Player.class); // Mock the third blue player
        PlayerInventory red1Inventory = mock(PlayerInventory.class); // Mock RedOne's inventory
        PlayerInventory red2Inventory = mock(PlayerInventory.class); // Mock RedTwo's inventory
        PlayerInventory red3Inventory = mock(PlayerInventory.class); // Mock RedThree's inventory
        PlayerInventory blue1Inventory = mock(PlayerInventory.class); // Mock BlueOne's inventory
        PlayerInventory blue2Inventory = mock(PlayerInventory.class); // Mock BlueTwo's inventory
        PlayerInventory blue3Inventory = mock(PlayerInventory.class); // Mock BlueThree's inventory
        UUID red1Id = UUID.randomUUID(); // Assign a stable UUID to RedOne
        UUID red2Id = UUID.randomUUID(); // Assign a stable UUID to RedTwo
        UUID red3Id = UUID.randomUUID(); // Assign a stable UUID to RedThree
        UUID blue1Id = UUID.randomUUID(); // Assign a stable UUID to BlueOne
        UUID blue2Id = UUID.randomUUID(); // Assign a stable UUID to BlueTwo
        UUID blue3Id = UUID.randomUUID(); // Assign a stable UUID to BlueThree
        AtomicReference<Location> red1Location = new AtomicReference<>(new Location(world, 8.0, 65.0, -8.0)); // Track RedOne's mutable location
        AtomicReference<Location> red2Location = new AtomicReference<>(new Location(world, 7.0, 65.0, -8.0)); // Track RedTwo's mutable location
        AtomicReference<Location> red3Location = new AtomicReference<>(new Location(world, 6.0, 65.0, -8.0)); // Track RedThree's mutable location
        AtomicReference<Location> blue1Location = new AtomicReference<>(new Location(world, -8.0, 65.0, 8.0)); // Track BlueOne's mutable location
        AtomicReference<Location> blue2Location = new AtomicReference<>(new Location(world, -7.0, 65.0, 8.0)); // Track BlueTwo's mutable location
        AtomicReference<Location> blue3Location = new AtomicReference<>(new Location(world, -6.0, 65.0, 8.0)); // Track BlueThree's mutable location
        AtomicReference<Vector> red1Velocity = new AtomicReference<>(new Vector()); // Track RedOne's mutable velocity
        AtomicReference<Vector> blue2Velocity = new AtomicReference<>(new Vector()); // Track BlueTwo's mutable velocity
        Map<PlayerInventory, Map<Integer, ItemStack>> inventoryItemsByInventory = Map.of(
            red1Inventory, new java.util.HashMap<>(), // Back RedOne's inventory with a mutable slot map
            red2Inventory, new java.util.HashMap<>(), // Back RedTwo's inventory with a mutable slot map
            red3Inventory, new java.util.HashMap<>(), // Back RedThree's inventory with a mutable slot map
            blue1Inventory, new java.util.HashMap<>(), // Back BlueOne's inventory with a mutable slot map
            blue2Inventory, new java.util.HashMap<>(), // Back BlueTwo's inventory with a mutable slot map
            blue3Inventory, new java.util.HashMap<>() // Back BlueThree's inventory with a mutable slot map
        ); // Group inventory slot state by inventory mock

        when(red1.getUniqueId()).thenReturn(red1Id); // Return RedOne's UUID
        when(red2.getUniqueId()).thenReturn(red2Id); // Return RedTwo's UUID
        when(red3.getUniqueId()).thenReturn(red3Id); // Return RedThree's UUID
        when(blue1.getUniqueId()).thenReturn(blue1Id); // Return BlueOne's UUID
        when(blue2.getUniqueId()).thenReturn(blue2Id); // Return BlueTwo's UUID
        when(blue3.getUniqueId()).thenReturn(blue3Id); // Return BlueThree's UUID
        when(red1.getName()).thenReturn("RedOne"); // Return RedOne's username
        when(red2.getName()).thenReturn("RedTwo"); // Return RedTwo's username
        when(red3.getName()).thenReturn("RedThree"); // Return RedThree's username
        when(blue1.getName()).thenReturn("BlueOne"); // Return BlueOne's username
        when(blue2.getName()).thenReturn("BlueTwo"); // Return BlueTwo's username
        when(blue3.getName()).thenReturn("BlueThree"); // Return BlueThree's username
        when(red1.displayName()).thenReturn(Component.text("RedOne")); // Return RedOne's display name
        when(red2.displayName()).thenReturn(Component.text("RedTwo")); // Return RedTwo's display name
        when(red3.displayName()).thenReturn(Component.text("RedThree")); // Return RedThree's display name
        when(blue1.displayName()).thenReturn(Component.text("BlueOne")); // Return BlueOne's display name
        when(blue2.displayName()).thenReturn(Component.text("BlueTwo")); // Return BlueTwo's display name
        when(blue3.displayName()).thenReturn(Component.text("BlueThree")); // Return BlueThree's display name
        when(red1.isOnline()).thenReturn(true); // Report RedOne as online
        when(red2.isOnline()).thenReturn(true); // Report RedTwo as online
        when(red3.isOnline()).thenReturn(true); // Report RedThree as online
        when(blue1.isOnline()).thenReturn(true); // Report BlueOne as online
        when(blue2.isOnline()).thenReturn(true); // Report BlueTwo as online
        when(blue3.isOnline()).thenReturn(true); // Report BlueThree as online
        when(red1.getInventory()).thenReturn(red1Inventory); // Return RedOne's inventory
        when(red2.getInventory()).thenReturn(red2Inventory); // Return RedTwo's inventory
        when(red3.getInventory()).thenReturn(red3Inventory); // Return RedThree's inventory
        when(blue1.getInventory()).thenReturn(blue1Inventory); // Return BlueOne's inventory
        when(blue2.getInventory()).thenReturn(blue2Inventory); // Return BlueTwo's inventory
        when(blue3.getInventory()).thenReturn(blue3Inventory); // Return BlueThree's inventory
        when(red1.getWorld()).thenReturn(world); // Keep RedOne in the simulated world
        when(red2.getWorld()).thenReturn(world); // Keep RedTwo in the simulated world
        when(red3.getWorld()).thenReturn(world); // Keep RedThree in the simulated world
        when(blue1.getWorld()).thenReturn(world); // Keep BlueOne in the simulated world
        when(blue2.getWorld()).thenReturn(world); // Keep BlueTwo in the simulated world
        when(blue3.getWorld()).thenReturn(world); // Keep BlueThree in the simulated world
        when(red1.getLocation()).thenAnswer(invocation -> red1Location.get()); // Return RedOne's tracked location
        when(red2.getLocation()).thenAnswer(invocation -> red2Location.get()); // Return RedTwo's tracked location
        when(red3.getLocation()).thenAnswer(invocation -> red3Location.get()); // Return RedThree's tracked location
        when(blue1.getLocation()).thenAnswer(invocation -> blue1Location.get()); // Return BlueOne's tracked location
        when(blue2.getLocation()).thenAnswer(invocation -> blue2Location.get()); // Return BlueTwo's tracked location
        when(blue3.getLocation()).thenAnswer(invocation -> blue3Location.get()); // Return BlueThree's tracked location
        when(red1.getEyeLocation()).thenAnswer(invocation -> red1Location.get().clone().add(0.0, 1.62, 0.0)); // Return RedOne's eye position for projectile logic
        when(blue2.getEyeLocation()).thenAnswer(invocation -> blue2Location.get().clone().add(0.0, 1.62, 0.0)); // Return BlueTwo's eye position for projectile logic
        when(red1.getVelocity()).thenAnswer(invocation -> red1Velocity.get()); // Return RedOne's tracked velocity
        when(blue2.getVelocity()).thenAnswer(invocation -> blue2Velocity.get()); // Return BlueTwo's tracked velocity
        doAnswer(invocation -> {
            red1Velocity.set(invocation.getArgument(0, Vector.class)); // Persist RedOne's new velocity vector
            return null; // Match Bukkit's void setter contract
        }).when(red1).setVelocity(any(Vector.class));
        doAnswer(invocation -> {
            blue2Velocity.set(invocation.getArgument(0, Vector.class)); // Persist BlueTwo's new velocity vector
            return null; // Match Bukkit's void setter contract
        }).when(blue2).setVelocity(any(Vector.class));
        doAnswer(invocation -> {
            red1Location.set(invocation.getArgument(0, Location.class)); // Persist RedOne's teleported location
            return true; // Report teleport success
        }).when(red1).teleport(any(Location.class));
        doAnswer(invocation -> {
            red2Location.set(invocation.getArgument(0, Location.class)); // Persist RedTwo's teleported location
            return true; // Report teleport success
        }).when(red2).teleport(any(Location.class));
        doAnswer(invocation -> {
            red3Location.set(invocation.getArgument(0, Location.class)); // Persist RedThree's teleported location
            return true; // Report teleport success
        }).when(red3).teleport(any(Location.class));
        doAnswer(invocation -> {
            blue1Location.set(invocation.getArgument(0, Location.class)); // Persist BlueOne's teleported location
            return true; // Report teleport success
        }).when(blue1).teleport(any(Location.class));
        doAnswer(invocation -> {
            blue2Location.set(invocation.getArgument(0, Location.class)); // Persist BlueTwo's teleported location
            return true; // Report teleport success
        }).when(blue2).teleport(any(Location.class));
        doAnswer(invocation -> {
            blue3Location.set(invocation.getArgument(0, Location.class)); // Persist BlueThree's teleported location
            return true; // Report teleport success
        }).when(blue3).teleport(any(Location.class));
        when(Bukkit.getServer().getPlayer("RedOne")).thenReturn(red1); // Resolve RedOne by name from the server
        when(Bukkit.getServer().getPlayer("RedTwo")).thenReturn(red2); // Resolve RedTwo by name from the server
        when(Bukkit.getServer().getPlayer("RedThree")).thenReturn(red3); // Resolve RedThree by name from the server
        when(Bukkit.getServer().getPlayer("BlueOne")).thenReturn(blue1); // Resolve BlueOne by name from the server
        when(Bukkit.getServer().getPlayer("BlueTwo")).thenReturn(blue2); // Resolve BlueTwo by name from the server
        when(Bukkit.getServer().getPlayer("BlueThree")).thenReturn(blue3); // Resolve BlueThree by name from the server
        when(Bukkit.getServer().getPlayer(red1Id)).thenReturn(red1); // Resolve RedOne by UUID from the server
        when(Bukkit.getServer().getPlayer(red2Id)).thenReturn(red2); // Resolve RedTwo by UUID from the server
        when(Bukkit.getServer().getPlayer(red3Id)).thenReturn(red3); // Resolve RedThree by UUID from the server
        when(Bukkit.getServer().getPlayer(blue1Id)).thenReturn(blue1); // Resolve BlueOne by UUID from the server
        when(Bukkit.getServer().getPlayer(blue2Id)).thenReturn(blue2); // Resolve BlueTwo by UUID from the server
        when(Bukkit.getServer().getPlayer(blue3Id)).thenReturn(blue3); // Resolve BlueThree by UUID from the server
        Mockito.doReturn(java.util.List.of(red1, red2, red3, blue1, blue2, blue3))
            .when(Bukkit.getServer())
            .getOnlinePlayers(); // Expose the full six-player roster as online
        for (Map.Entry<PlayerInventory, Map<Integer, ItemStack>> entry : inventoryItemsByInventory.entrySet()) {
            PlayerInventory inventory = entry.getKey(); // Read the inventory mock being configured
            Map<Integer, ItemStack> slots = entry.getValue(); // Read the backing slot map for that inventory
            doAnswer(invocation -> {
                int slot = invocation.getArgument(0, Integer.class); // Read the slot being written
                ItemStack item = invocation.getArgument(1, ItemStack.class); // Read the item being stored
                slots.put(slot, item); // Persist the item in the backing slot map
                return null; // Match Bukkit's void setter contract
            }).when(inventory).setItem(any(Integer.class), any(ItemStack.class));
            when(inventory.getItem(any(Integer.class))).thenAnswer(invocation -> {
                int slot = invocation.getArgument(0, Integer.class); // Read the slot being queried
                return slots.get(slot); // Return the current item in that slot
            });
            when(inventory.getItemInMainHand()).thenAnswer(invocation -> slots.get(0)); // Treat slot zero as the held item
            doAnswer(invocation -> {
                slots.clear(); // Clear the entire backing inventory state
                return null; // Match Bukkit's void clear contract
            }).when(inventory).clear();
            doAnswer(invocation -> null).when(inventory).setArmorContents(any(ItemStack[].class)); // Ignore armor mutations for this simulation
            when(inventory.getHeldItemSlot()).thenReturn(0); // Keep the selected hotbar slot on zero
        }
        ItemStack blue2ScoutSword = mock(ItemStack.class); // Build the scout tagger weapon for BlueTwo
        ItemMeta blue2ScoutSwordMeta = mock(ItemMeta.class); // Build metadata for the scout tagger weapon
        when(blue2ScoutSword.getType()).thenReturn(Material.WOODEN_SWORD); // Present the scout tagger as a wooden sword
        when(blue2ScoutSword.getItemMeta()).thenReturn(blue2ScoutSwordMeta); // Return the scout sword metadata
        when(blue2ScoutSwordMeta.displayName()).thenReturn(Component.text("Scout Tagger")); // Match the expected scout weapon name
        inventoryItemsByInventory.get(blue2Inventory).put(0, blue2ScoutSword); // Place the scout weapon in BlueTwo's main hand

        ItemStack red1SpearItem = mock(ItemStack.class); // Build the ranger spear item for RedOne
        ItemMeta red1SpearMeta = mock(ItemMeta.class); // Build metadata for the ranger spear
        when(red1SpearItem.getType()).thenReturn(Material.TRIDENT); // Present the ranger spear as a trident
        when(red1SpearItem.getItemMeta()).thenReturn(red1SpearMeta); // Return the ranger spear metadata
        when(red1SpearMeta.hasCustomModelData()).thenReturn(true); // Report custom model data as present on the spear
        when(red1SpearMeta.getCustomModelData()).thenReturn(HomingSpearItem.CUSTOM_MODEL_DATA); // Match the ranger spear model id
        when(red1SpearMeta.displayName()).thenReturn(Component.text(HomingSpearItem.NAME_TEXT)); // Match the ranger spear display name
        inventoryItemsByInventory.get(red1Inventory).put(0, red1SpearItem); // Put the spear in RedOne's hand
        inventoryItemsByInventory.get(red1Inventory).put(KitSlots.SPEAR_SLOT, red1SpearItem); // Mirror the spear into the dedicated spear slot

        Snowball scoutSnowball = mock(Snowball.class); // Build the projectile spawned by the scout tagger ability
        Trident homingTrident = mock(Trident.class); // Build the projectile spawned by the homing spear ability
        PersistentDataContainer scoutPdc = mock(PersistentDataContainer.class); // Build metadata storage for the scout projectile
        PersistentDataContainer spearPdc = mock(PersistentDataContainer.class); // Build metadata storage for the spear projectile
        when(scoutSnowball.getPersistentDataContainer()).thenReturn(scoutPdc); // Return the scout projectile metadata container
        when(homingTrident.getPersistentDataContainer()).thenReturn(spearPdc); // Return the spear projectile metadata container
        when(homingTrident.getUniqueId()).thenReturn(UUID.randomUUID()); // Give the spear projectile a stable id
        when(homingTrident.isDead()).thenReturn(false); // Keep the spear projectile alive during the simulation
        when(scoutPdc.has(any(), any())).thenReturn(true); // Pretend the scout projectile carries the expected tag metadata
        when(blue2.launchProjectile(Snowball.class)).thenReturn(scoutSnowball); // Return the scout projectile when BlueTwo fires
        when(red1.launchProjectile(Trident.class)).thenReturn(homingTrident); // Return the spear projectile when RedOne throws
        logStep("constructed mocked 3v3 roster and inventories"); // Mark completion of roster and loadout setup

        gameStateManager.setGameState(GameState.IN_PROGRESS); // Start the simulated match state
        gameStateManager.setCleanupInProgress(false); // Ensure cleanup starts disabled before gameplay
        scoreBoardManager.setScoreLimit(3); // Set first-to-3 win condition
        kitSelectionHandler.selectKit(red1, KitType.RANGER); // Assign RedOne to Ranger
        kitSelectionHandler.selectKit(red2, KitType.SCOUT); // Assign RedTwo to Scout
        kitSelectionHandler.selectKit(red3, KitType.RANGER); // Assign RedThree to Ranger
        kitSelectionHandler.selectKit(blue1, KitType.RANGER); // Assign BlueOne to Ranger
        kitSelectionHandler.selectKit(blue2, KitType.SCOUT); // Assign BlueTwo to Scout
        kitSelectionHandler.selectKit(blue3, KitType.RANGER); // Assign BlueThree to Ranger
        teamManager.joinTeam(red1, TeamManager.RED); // Add RedOne to red team
        teamManager.joinTeam(red2, TeamManager.RED); // Add RedTwo to red team
        teamManager.joinTeam(red3, TeamManager.RED); // Add RedThree to red team
        teamManager.joinTeam(blue1, TeamManager.BLUE); // Add BlueOne to blue team
        teamManager.joinTeam(blue2, TeamManager.BLUE); // Add BlueTwo to blue team
        teamManager.joinTeam(blue3, TeamManager.BLUE); // Add BlueThree to blue team
        logStep("assigned teams and kits for 3 red vs 3 blue players");

        assertEquals(6, teamManager.getJoinedPlayerCount()); // Confirm all six mocked players joined teams
        assertEquals(TeamManager.RED, teamManager.getTeamKey(red1)); // Confirm RedOne is on the red team
        assertEquals(TeamManager.BLUE, teamManager.getTeamKey(blue1)); // Confirm BlueOne is on the blue team

        scoutTaggerAbility.processScoutSnowballHit(blue2, red1, scoutSnowball); // Blue scout tags RedOne with scout ability
        Mockito.verify(red1).setGlowing(true); // Confirm the scout tagger made RedOne glow
        assertTrue(homingSpearAbility.tryActivateSpearAbility(red1)); // Red ranger activates homing spear ability
        logStep("validated scout tagger and homing spear ability paths");

        assertTrue(flagPickupHandler.processEnemyFlagPickup(blue1, TeamId.RED)); // BlueOne steals the red flag
        assertEquals(TeamId.RED, flagStateRegistry.findCarriedFlagTeam(blue1Id)); // Confirm BlueOne is carrying the red flag
        logStep("validated enemy flag pickup state");

        PlayerDeathEvent blueCarrierDeath = mock(PlayerDeathEvent.class); // Build the death event for the blue flag carrier
        EntityDamageByEntityEvent blueCarrierDamage = mock(EntityDamageByEntityEvent.class); // Build the killer damage context for the blue carrier
        List<ItemStack> blueCarrierDrops = new ArrayList<>(); // Track drops emitted from the blue carrier death
        when(blueCarrierDeath.getEntity()).thenReturn(blue1); // Report BlueOne as the player who died
        when(blueCarrierDeath.getDrops()).thenReturn(blueCarrierDrops); // Return the mutable drop list for death handling
        when(blue1.getKiller()).thenReturn(red2); // Report RedTwo as the killer
        when(blue1.getLastDamageCause()).thenReturn(blueCarrierDamage); // Attach the killer damage event to BlueOne
        when(blueCarrierDamage.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK); // Mark the death as melee damage
        when(blueCarrierDamage.getDamager()).thenReturn(red2); // Report RedTwo as the damaging entity
        loader.requireInstance(dev.tjxjnoobie.ctf.game.player.handlers.PlayerDeathHandler.class).onPlayerDeath(blueCarrierDeath); // RedTwo kills BlueOne, forcing a flag drop
        logStep("validated carrier death and forced flag drop");

        assertEquals(FlagState.DROPPED, flagStateRegistry.flagFor(TeamId.RED).state); // Confirm the red flag moved into the dropped state
        assertTrue(flagReturnHandler.returnDroppedOwnFlagToBase(red2, TeamId.RED)); // RedTwo returns the dropped red flag
        assertEquals(FlagState.AT_BASE, flagStateRegistry.flagFor(TeamId.RED).state); // Confirm the red flag reset to base
        logStep("validated dropped-flag return flow");

        Location redCaptureLocation = teamManager.getReturnPoints(TeamId.RED).get(0); // Grab the red capture point used for scoring
        assertNotNull(redCaptureLocation); // Confirm the capture point exists before running captures

        assertTrue(flagPickupHandler.processEnemyFlagPickup(red1, TeamId.BLUE)); // RedOne steals blue flag for capture 1
        flagCarrierHandler.processFlagCarrierMovement(red1, redCaptureLocation, true); // RedOne reaches red return point and scores
        assertEquals(1, scoreBoardManager.getScore(TeamManager.RED)); // Confirm the first capture increments red's score

        assertTrue(flagPickupHandler.processEnemyFlagPickup(red1, TeamId.BLUE)); // RedOne steals blue flag for capture 2
        flagCarrierHandler.processFlagCarrierMovement(red1, redCaptureLocation, true); // RedOne reaches red return point and scores
        assertEquals(2, scoreBoardManager.getScore(TeamManager.RED)); // Confirm the second capture increments red's score

        assertTrue(flagPickupHandler.processEnemyFlagPickup(red1, TeamId.BLUE)); // RedOne steals blue flag for capture 3
        flagCarrierHandler.processFlagCarrierMovement(red1, redCaptureLocation, true); // RedOne reaches red return point and wins
        assertEquals(3, scoreBoardManager.getScore(TeamManager.RED)); // Confirm the third capture reaches the score limit
        assertTrue(gameStateManager.isCleanupInProgress()); // Confirm cleanup started after the winning capture
        logStep("validated three captures, score limit, and cleanup transition");

        PlayerDeathEvent redDeath = mock(PlayerDeathEvent.class); // Build the post-win death event for RedThree
        EntityDamageByEntityEvent redDamage = mock(EntityDamageByEntityEvent.class); // Build the killer damage context for RedThree
        List<ItemStack> redDrops = new ArrayList<>(); // Track drops emitted from RedThree's death
        when(redDeath.getEntity()).thenReturn(red3); // Report RedThree as the player who died
        when(redDeath.getDrops()).thenReturn(redDrops); // Return the mutable drop list for death handling
        when(red3.getKiller()).thenReturn(blue3); // Report BlueThree as the killer
        when(red3.getLastDamageCause()).thenReturn(redDamage); // Attach the killer damage event to RedThree
        when(redDamage.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK); // Mark the death as melee damage
        when(redDamage.getDamager()).thenReturn(blue3); // Report BlueThree as the damaging entity
        loader.requireInstance(dev.tjxjnoobie.ctf.game.player.handlers.PlayerDeathHandler.class).onPlayerDeath(redDeath); // BlueThree kills RedThree for post-win death stats

        PlayerMatchStats redOneStats = sessionHandler.getPlayerStats(red1Id); // Load RedOne's accumulated match stats
        PlayerMatchStats redTwoStats = sessionHandler.getPlayerStats(red2Id); // Load RedTwo's accumulated match stats
        PlayerMatchStats redThreeStats = sessionHandler.getPlayerStats(red3Id); // Load RedThree's accumulated match stats
        PlayerMatchStats blueOneStats = sessionHandler.getPlayerStats(blue1Id); // Load BlueOne's accumulated match stats
        PlayerMatchStats blueThreeStats = sessionHandler.getPlayerStats(blue3Id); // Load BlueThree's accumulated match stats
        assertNotNull(redOneStats); // Confirm RedOne has a stats entry
        assertNotNull(redTwoStats); // Confirm RedTwo has a stats entry
        assertNotNull(redThreeStats); // Confirm RedThree has a stats entry
        assertNotNull(blueOneStats); // Confirm BlueOne has a stats entry
        assertNotNull(blueThreeStats); // Confirm BlueThree has a stats entry
        assertTrue(redOneStats.getCaptures() >= 3); // Confirm RedOne logged at least three captures
        assertTrue(redTwoStats.getKills() >= 1); // Confirm RedTwo logged the carrier kill
        assertTrue(blueOneStats.getDeaths() >= 1); // Confirm BlueOne logged the carrier death
        assertTrue(blueThreeStats.getKills() >= 1); // Confirm BlueThree logged the post-win kill
        assertTrue(redThreeStats.getDeaths() >= 1); // Confirm RedThree logged the post-win death

        flagDropHandler.dropCarriedFlagIfPresent(red1); // Ensure no carried flag remains on RedOne
        bootstrap.onDisable(); // Simulate plugin shutdown at end of match
        spearReturningPlaceholderItemConstruction.close(); // Release the returning placeholder construction hook
        spearLockedPlaceholderItemConstruction.close(); // Release the locked placeholder construction hook
        homingSpearItemConstruction.close(); // Release the homing spear construction hook
        playerRespawnSchedulerConstruction.close(); // Release the respawn scheduler construction hook
        matchCleanupHandlerConstruction.close(); // Release the cleanup handler construction hook
        playerDeathEffectsConstruction.close(); // Release the death effects construction hook
        flagEventEffectsConstruction.close(); // Release the flag event effects construction hook
        carrierEffectsConstruction.close(); // Release the carrier effects construction hook
        flagItemConstruction.close(); // Release the flag item construction hook
        flagIndicatorItemConstruction.close(); // Release the flag indicator construction hook
    }
}
