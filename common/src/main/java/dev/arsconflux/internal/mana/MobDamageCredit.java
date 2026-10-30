package dev.arsconflux.internal.mana;
import dev.arsconflux.api.damage.SpellPower;
import dev.arsconflux.api.resource.MobMana;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
/** Ars Harm uses a compatibility player for source creation, even with a real mob context. */
public final class MobDamageCredit {
    public static Entity resolve(Entity declared) {
        var profile=SpellPower.current();
        if(profile==null||profile.caster()==null||profile.caster() instanceof Player
                ||!profile.caster().getPersistentData().getBoolean(MobMana.ACTIVE)
                ||!dev.arsconflux.platform.NativeManaPlatform.isProxy(declared,profile.caster()))return declared;
        return profile.damageOwner()==null?profile.caster():profile.damageOwner();
    }
    private MobDamageCredit() {}
}
