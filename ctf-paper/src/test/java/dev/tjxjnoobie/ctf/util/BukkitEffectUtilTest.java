package dev.tjxjnoobie.ctf.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import dev.tjxjnoobie.ctf.util.bukkit.effects.IBukkitEffectUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class BukkitEffectUtilTest {

    private final IBukkitEffectUtil effectUtil = new IBukkitEffectUtil() {
    };

    @Test
    void canonicalInterfaceDelegatesGlowChanges() {
        Entity entity = mock(Entity.class);

        effectUtil.setGlowing(entity, true);

        verify(entity).setGlowing(true);
    }

    @Test
    void canonicalInterfaceDelegatesSoundPlayback() {
        Player player = mock(Player.class);

        effectUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        verify(player).playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    @Test
    void invalidInputsRemainNoOps() {
        assertDoesNotThrow(() -> effectUtil.setGlowing((Entity) null, true));
        assertDoesNotThrow(() -> effectUtil.playSound(null, null, 1.0f, 1.0f));
    }
}
