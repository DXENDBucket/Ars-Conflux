package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.util.UUID;

/** Lazy reflective surface for ANS 3.4.0 on NeoForge; no hard dependency. */
public final class AnsAccess {
    private AnsAccess() {}
    public static Method castMethod() { return Api.CAST; }
    public static Method entryTagMethod() { return Api.TAG; }
    public static Object refused() { return false; }
    public static boolean valid(Player player, Object entry) {
        return (boolean)AnsActiveCasts.invoke(Api.OK, AnsActiveCasts.invoke(Api.VALID, null, entry, 0, 1));
    }
    public static Spell decode(CompoundTag tag) { return (Spell)AnsActiveCasts.invoke(Api.DECODE, null, tag); }
    private static class Api {
        static final Method CAST, TAG, DECODE, VALID, OK;
        static {
            try {
                var handler = Class.forName("com.otectus.arsnspells.spell.CrossCastingHandler");
                var entry = Class.forName("com.otectus.arsnspells.spell.CrossModSpell");
                CAST = handler.getMethod("castArsSpell", Player.class, ItemStack.class, entry, UUID.class,
                        InteractionHand.class, Class.forName("com.otectus.arsnspells.spell.target.CastTargetHint"));
                TAG = entry.getMethod("arsSpellTag"); DECODE = handler.getMethod("decodeArsSpell", CompoundTag.class);
                VALID = Class.forName("com.otectus.arsnspells.spell.CrossCastValidator").getMethod("validate", entry, int.class, int.class);
                OK = VALID.getReturnType().getMethod("ok");
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
}
