package dev.arsconflux.platform;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.util.UUID;

/** ANS 3.4.0 Forge retains NBT entries and a structured dispatch result. */
public final class AnsAccess {
    private AnsAccess() {}
    public static Method castMethod() { return Api.CAST; }
    public static Method entryTagMethod() { return null; }
    public static Object refused() { return Api.REFUSED; }
    public static boolean valid(Player player, Object entry) {
        return (boolean)AnsActiveCasts.invoke(Api.OK, AnsActiveCasts.invoke(Api.VALID, null, player, entry, 0, 1));
    }
    public static Spell decode(CompoundTag tag) { return Spell.fromTag(tag); }
    private static class Api {
        static final Method CAST, VALID, OK;
        static final Object REFUSED;
        static {
            try {
                var handler = Class.forName("com.otectus.arsnspells.spell.CrossCastingHandler");
                CAST = handler.getMethod("castArsSpell", Player.class, ItemStack.class, ItemStack.class,
                        InteractionHand.class, CompoundTag.class, UUID.class,
                        Class.forName("com.otectus.arsnspells.spell.target.InteractionOrigin"),
                        Class.forName("com.otectus.arsnspells.spell.target.TargetHint"));
                VALID = Class.forName("com.otectus.arsnspells.spell.CrossCastValidator").getMethod("validate", Player.class, CompoundTag.class, int.class, int.class);
                OK = VALID.getReturnType().getMethod("ok");
                var result = CAST.getReturnType();
                var types = result.getRecordComponents();
                Object outcome = java.util.Arrays.stream(types[0].getType().getEnumConstants())
                        .filter(e -> ((Enum<?>)e).name().equals("RESOLVER_REFUSED")).findFirst().orElseThrow();
                Object entry = java.util.Arrays.stream(types[1].getType().getEnumConstants())
                        .filter(e -> ((Enum<?>)e).name().equals("NONE")).findFirst().orElseThrow();
                REFUSED = result.getConstructor(types[0].getType(), types[1].getType(), types[2].getType(), boolean.class)
                        .newInstance(outcome, entry, null, true);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
}
