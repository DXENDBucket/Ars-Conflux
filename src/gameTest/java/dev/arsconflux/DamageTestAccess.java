package dev.arsconflux;
import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.UUID;
import java.util.function.Consumer;
final class DamageTestAccess {
    static LivingEntity caster(ServerLevel world) {
        var player=FakePlayerFactory.get(world,new GameProfile(UUID.randomUUID(),"DamageTest"));
        player.setHealth(player.getMaxHealth());
        player.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS).setBaseValue(6);
        return player;
    }
    static AutoCloseable observe(Consumer<SpellDamageEvent.Pre> pre,Consumer<SpellDamageEvent.Post> post) {
        NeoForge.EVENT_BUS.addListener(pre);NeoForge.EVENT_BUS.addListener(post);
        return ()->{NeoForge.EVENT_BUS.unregister(pre);NeoForge.EVENT_BUS.unregister(post);};
    }
}
