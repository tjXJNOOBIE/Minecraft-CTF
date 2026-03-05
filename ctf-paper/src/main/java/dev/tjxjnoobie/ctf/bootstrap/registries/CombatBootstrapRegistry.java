package dev.tjxjnoobie.ctf.bootstrap.registries;

import dev.tjxjnoobie.ctf.Main;
import dev.tjxjnoobie.ctf.dependency.DependencyLoader;
import dev.tjxjnoobie.ctf.events.handlers.CombatDamageByEntityHandler;
import dev.tjxjnoobie.ctf.events.handlers.HomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IHomingSpearCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.IScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.events.handlers.ScoutTaggerCombatEventHandler;
import dev.tjxjnoobie.ctf.game.combat.HomingSpearAbilityCooldown;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerAbility;
import dev.tjxjnoobie.ctf.game.combat.handlers.CombatDamageRestrictionHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.ScoutTaggerAbilityEvents;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerCooldownHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerGlowHandler;
import dev.tjxjnoobie.ctf.game.combat.scout.handlers.ScoutTaggerRuntimeHandler;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearAbilityHandler;
import dev.tjxjnoobie.ctf.game.combat.handlers.HomingSpearRuntimeRegistry;
import dev.tjxjnoobie.ctf.game.combat.spear.HomingSpearAbilityEvents;
import dev.tjxjnoobie.ctf.game.player.handlers.MatchPlayerSessionHandler;
import dev.tjxjnoobie.ctf.game.combat.util.HomingSpearInventoryUtils;
import dev.tjxjnoobie.ctf.items.kit.HomingSpearItem;
import dev.tjxjnoobie.ctf.items.kit.SpearLockedPlaceholderItem;
import dev.tjxjnoobie.ctf.items.kit.SpearReturningPlaceholderItem;
import dev.tjxjnoobie.ctf.kit.KitSelectionHandler;

public final class CombatBootstrapRegistry implements BootstrapRegistry {
        /**
         * Updates state for register.
         *
         * @param loader Service loader used to register this bootstrap component.
         * @param plugin Plugin instance used to access Bukkit runtime services.
     */
        // == Lifecycle ==

        @Override
        public void register(DependencyLoader loader, Main plugin) {
                // Register combat-related services.
                loader.queueDependency(CombatDamageRestrictionHandler.class,
                                CombatDamageRestrictionHandler::new);
                loader.queueDependency(CombatDamageByEntityHandler.class,
                                () -> loader.requireInstance(CombatDamageRestrictionHandler.class));
                loader.queueDependency(HomingSpearInventoryUtils.class,
                                () -> new HomingSpearInventoryUtils(
                                                loader.requireInstance(HomingSpearItem.class),
                                                loader.requireInstance(SpearLockedPlaceholderItem.class),
                                                loader.requireInstance(SpearReturningPlaceholderItem.class)));
                loader.queueDependency(HomingSpearRuntimeRegistry.class, HomingSpearRuntimeRegistry::new);
                loader.queueDependency(HomingSpearAbilityCooldown.class,
                                () -> new HomingSpearAbilityCooldown(
                                                loader.requireInstance(HomingSpearInventoryUtils.class),
                                                loader.requireInstance(HomingSpearRuntimeRegistry.class)));
                loader.queueDependency(HomingSpearAbilityHandler.class,
                                () -> new HomingSpearAbilityHandler(
                                                loader.requireInstance(HomingSpearAbilityCooldown.class),
                                                loader.requireInstance(HomingSpearInventoryUtils.class)));
                loader.queueDependency(HomingSpearAbilityEvents.class,
                                () -> new HomingSpearAbilityEvents(
                                                loader.requireInstance(HomingSpearAbilityHandler.class),
                                                loader.requireInstance(HomingSpearInventoryUtils.class)));
                loader.queueDependency(HomingSpearCombatEventHandler.class,
                                () -> loader.requireInstance(HomingSpearAbilityEvents.class));
                loader.queueDependency(IHomingSpearCombatEventHandler.class,
                                () -> loader.requireInstance(HomingSpearAbilityEvents.class));
                loader.queueDependency(ScoutTaggerRuntimeHandler.class, ScoutTaggerRuntimeHandler::new);
                loader.queueDependency(ScoutTaggerGlowHandler.class, ScoutTaggerGlowHandler::new);
                loader.queueDependency(ScoutTaggerCooldownHandler.class,
                                () -> new ScoutTaggerCooldownHandler(
                                                loader.requireInstance(MatchPlayerSessionHandler.class),
                                                loader.requireInstance(KitSelectionHandler.class),
                                                loader.requireInstance(ScoutTaggerRuntimeHandler.class)));
                loader.queueDependency(ScoutTaggerAbility.class,
                                () -> new ScoutTaggerAbility(
                                                loader.requireInstance(ScoutTaggerRuntimeHandler.class),
                                                loader.requireInstance(ScoutTaggerCooldownHandler.class),
                                                loader.requireInstance(ScoutTaggerGlowHandler.class)));
                loader.queueDependency(ScoutTaggerAbilityEvents.class,
                                () -> new ScoutTaggerAbilityEvents(
                                                loader.requireInstance(ScoutTaggerAbility.class)));
                loader.queueDependency(ScoutTaggerCombatEventHandler.class,
                                () -> loader.requireInstance(ScoutTaggerAbilityEvents.class));
                loader.queueDependency(IScoutTaggerCombatEventHandler.class,
                                () -> loader.requireInstance(ScoutTaggerAbilityEvents.class));
        }
}
