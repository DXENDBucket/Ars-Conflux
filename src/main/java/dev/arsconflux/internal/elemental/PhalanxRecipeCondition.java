package dev.arsconflux.internal.elemental;

import com.mojang.serialization.MapCodec;
import dev.arsconflux.config.ConfluxStartupConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Avoid adding a recipe when optional content is disabled, even in upstream development mode. */
public record PhalanxRecipeCondition() implements ICondition {
    public static final MapCodec<PhalanxRecipeCondition> CODEC = MapCodec.unit(new PhalanxRecipeCondition());
    public boolean test(IContext context) { return ConfluxStartupConfig.elementalPhalanx(); }
    public MapCodec<? extends ICondition> codec() { return CODEC; }
}
