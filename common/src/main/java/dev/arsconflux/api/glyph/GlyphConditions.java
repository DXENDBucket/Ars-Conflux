package dev.arsconflux.api.glyph;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
/** Optional quiet usefulness checks for AI; never replaces native casting/target permissions. */
public final class GlyphConditions {
    private static final Map<ResourceLocation,BiPredicate<LivingEntity,LivingEntity>> CHECKS=new ConcurrentHashMap<>();
    public static void declare(ResourceLocation glyph,BiPredicate<LivingEntity,LivingEntity> check) {
        if(CHECKS.putIfAbsent(Objects.requireNonNull(glyph),Objects.requireNonNull(check))!=null)
            throw new IllegalArgumentException("Duplicate glyph condition: "+glyph);
    }
    public static boolean useful(AbstractSpellPart glyph,LivingEntity caster,LivingEntity recipient) {
        var check=glyph==null?null:CHECKS.get(glyph.getRegistryName());return check==null||check.test(caster,recipient);
    }
    private GlyphConditions() {}
}
