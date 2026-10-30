package dev.arsconflux.api.resource;

import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import net.minecraft.world.entity.player.Player;
import dev.arsconflux.platform.ArsAccess;

/** Default Ars adapter. Non-living device providers can supply their own stable pool key. */
public record ArsManaAccount(IWrappedCaster source, Object poolOwner, boolean free) implements ResourceAccount {
    public ArsManaAccount(IWrappedCaster source) {
        this(source, source instanceof LivingCaster living ? living.livingEntity : source,
                source instanceof LivingCaster living && living.livingEntity instanceof Player player && player.isCreative());
    }
    public int quoteCost(int raw) {return source instanceof LivingCaster living?ArsManaDiscounts.quote(living.livingEntity,raw):raw;}
    public Object reservationKey() { return ResourcePoolKey.of("ars_nouveau:mana", poolOwner); }
    public long capacity() {
        if(free)return Long.MAX_VALUE;
        if(source instanceof LivingCaster living){var pool=ArsManaPools.of(living.livingEntity);return pool==null?0:(long)pool.maximum();}
        return -1;
    }
    public boolean canSpend(int amount) { return amount == 0 || ArsAccess.canSpend(source, amount); }
    public void spend(int amount) { ArsAccess.spend(source, amount); }
}
