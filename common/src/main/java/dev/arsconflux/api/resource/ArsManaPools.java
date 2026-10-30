package dev.arsconflux.api.resource;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import dev.arsconflux.platform.NativeManaPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Native Ars storage in player units. Pool access activates only Core's non-player fallback. */
public final class ArsManaPools {
    public interface Pool {double current();double maximum();double regeneration();void set(double amount);}
    public static Pool of(LivingEntity actor) {
        var cap=NativeManaPlatform.get(actor);if(cap==null)return null;
        return new Pool() {
            public double current(){return cap.getCurrentMana();}
            public double maximum(){return cap.getMaxMana();}
            public double regeneration(){return actor instanceof Player p?NativeManaPlatform.playerRegen(p):NativeManaPlatform.owns(cap)?MobMana.regeneration(actor,cap):0;}
            public void set(double amount){check(amount);if(actor.level().isClientSide)throw new IllegalStateException("Mana is server-owned");
                cap.setMana(Math.min(cap.getMaxMana(),amount));NativeManaPlatform.sync(actor,cap);}
        };
    }
    public static boolean trySpend(LivingEntity actor,double amount) {
        check(amount);if(actor.level().isClientSide||!actor.isAlive())return false;
        var account=new ArsManaAccount(LivingCaster.from(actor));if(account.free())return true;
        var pool=of(actor);if(pool==null||pool.current()-ResourceTransactions.reserved(account)<amount)return false;
        pool.set(Math.max(0,pool.current()-amount));return true;
    }
    public static double restore(LivingEntity actor,double amount) {
        check(amount);if(actor.level().isClientSide||!actor.isAlive())return 0;
        var pool=of(actor);if(pool==null)return 0;double before=pool.current();pool.set(Math.min(pool.maximum(),before+amount));return Math.max(0,pool.current()-before);
    }
    /** Transfers stored mana, including from creative players. Never discounts or mints it. */
    public static double transfer(LivingEntity from,LivingEntity to,double amount) {
        check(amount);if(from==to||from.level()!=to.level()||from.level().isClientSide||!from.isAlive()||!to.isAlive())return 0;
        var source=of(from);var target=of(to);if(source==null||target==null)return 0;
        double moved=Math.max(0,Math.min(amount,Math.min(source.current()-ResourceTransactions.reserved(new ArsManaAccount(LivingCaster.from(from))),target.maximum()-target.current())));
        source.set(source.current()-moved);target.set(target.current()+moved);return moved;
    }
    private static void check(double amount){if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid mana amount");}
    private ArsManaPools() {}
}
