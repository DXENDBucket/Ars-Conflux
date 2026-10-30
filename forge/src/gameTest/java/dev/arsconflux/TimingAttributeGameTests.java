package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.entity.EntityType;
import dev.arsconflux.platform.TimingAttributes;
@GameTestHolder("ars_conflux_timing") @PrefixGameTestTemplate(false)
public final class TimingAttributeGameTests {
    @GameTest(template="empty",timeoutTicks=30) public static void attributesAndCooldownSnapshots(GameTestHelper h){
        var actor=h.spawn(EntityType.COW,2,3,2);actor.setNoAi(true);
        TimingAttributeCases.cooldowns(h,actor,actor.getAttribute(TimingAttributes.COOLDOWN_REDUCTION.get()),actor.getAttribute(TimingAttributes.CAST_TIME_REDUCTION.get()));
    }
    @GameTest(template="empty",timeoutTicks=30) public static void sharedPreparationAndPureArs(GameTestHelper h){
        var actor=h.spawn(EntityType.COW,2,3,2);actor.setNoAi(true);
        TimingAttributeCases.preparation(h,actor,actor.getAttribute(TimingAttributes.COOLDOWN_REDUCTION.get()),actor.getAttribute(TimingAttributes.CAST_TIME_REDUCTION.get()));
    }
}
