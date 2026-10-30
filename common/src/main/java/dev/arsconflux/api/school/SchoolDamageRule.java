package dev.arsconflux.api.school;

import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import java.util.Objects;

/** Highest-priority matching rule wins per school; equal priorities use lexicographic rule ID. */
public record SchoolDamageRule(ResourceLocation id, ResourceLocation school, ResourceLocation targetType,
                               ResourceLocation targetTag, int priority, double multiplier) {
    public SchoolDamageRule {
        Objects.requireNonNull(id); Objects.requireNonNull(school);
        if (!Double.isFinite(multiplier) || multiplier < 0) throw new IllegalArgumentException("Invalid school damage multiplier");
    }
    public boolean matches(Entity entity) {
        return entity != null && (targetType == null || targetType.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))
                && (targetTag == null || entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, targetTag)));
    }
}
