package dev.arsconflux.api.school;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Display metadata and optional native Ars associations, never automatic glyph membership. */
public record SchoolDefinition(ResourceLocation id, String name, String translationKey, int color,
                               Set<String> arsSchools) {
    public SchoolDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(translationKey);
        arsSchools = Set.copyOf(arsSchools);
        if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("School color must be RGB");
    }
    public Component displayName() { return translationKey.isBlank() ? Component.literal(name) : Component.translatable(translationKey); }
}
