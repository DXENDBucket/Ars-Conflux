package dev.arsconflux.api.glyph;

import net.minecraft.resources.ResourceLocation;

/** Optional AI hints, independent of schools, damage types and execution permission. */
public final class AbilityTags {
    public static final ResourceLocation ATTACK=id("attack"), HEALING=id("healing"), DEFENSE=id("defense"),
            CONTROL=id("control"), SUMMONING=id("summoning"), MOBILITY=id("mobility"), UTILITY=id("utility"),
            AREA=id("area"), CHANNELING=id("channeling"), SELF=id("receiver_self"), ALLY=id("receiver_ally"),
            ENEMY=id("receiver_enemy"), POSITION=id("receiver_position"), PLAYER_ONLY=id("player_only");
    private static ResourceLocation id(String path) { return ResourceLocation.tryParse("ars_conflux:"+path); }
    private AbilityTags() {}
}
