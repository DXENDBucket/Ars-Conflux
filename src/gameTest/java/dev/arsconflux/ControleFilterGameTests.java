package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.internal.controle.RandomOperands;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_conflux_optional") @PrefixGameTestTemplate(false)
public final class ControleFilterGameTests {
    @GameTest(template="empty", timeoutTicks=40)
    public static void binaryRandomOperandsHaveIndependentAugmentsAndSnapshots(GameTestHelper h) throws Exception {
        Class<?> type;
        try { type = Class.forName("dev.qther.ars_controle.spell.filter.FilterBinary"); }
        catch (ClassNotFoundException absent) { h.succeed(); return; }
        var binary = (AbstractSpellPart)type.getField("XOR").get(null);
        var randomType = Class.forName("dev.qther.ars_controle.spell.filter.FilterRandom");
        var random = (AbstractSpellPart)randomType.getField("INSTANCE").get(null);
        var caster = h.spawn(EntityType.COW, 2, 4, 2); caster.setNoAi(true);
        var resolver = new SpellResolver(new SpellContext(h.getLevel(),
                new Spell(MethodSelf.INSTANCE, binary, random, AugmentAmplify.INSTANCE, random, AugmentDampen.INSTANCE),
                caster, LivingCaster.from(caster), ItemStack.EMPTY));
        resolver.spellContext.setCurrentIndex(2);
        Object previous = type.getField("res").get(binary);
        double previousChance = randomType.getField("chance").getDouble(random);
        try {
            type.getField("res").set(binary, resolver);
            @SuppressWarnings("unchecked")
            var filters = (Pair<Integer, Pair<IFilter, IFilter>>)type.getMethod("getFilters").invoke(binary);
            h.assertTrue(filters.second().first() instanceof RandomOperands.Snapshot && filters.second().second() instanceof RandomOperands.Snapshot,
                    "Both occurrences of the random singleton become independent operands");
            var first = (RandomOperands.Snapshot)filters.second().first();
            var second = (RandomOperands.Snapshot)filters.second().second();
            h.assertTrue(first.chance() == 0.75 && second.chance() == 0.25,
                    "First amplified operand and second dampened operand use their own augment positions");
            randomType.getField("chance").setDouble(random, 0.99);
            h.assertTrue(first.chance() == 0.75 && second.chance() == 0.25, "Later casts cannot overwrite this binary evaluation");
        } finally { type.getField("res").set(binary, previous); randomType.getField("chance").setDouble(random, previousChance); }
        h.succeed();
    }
}
