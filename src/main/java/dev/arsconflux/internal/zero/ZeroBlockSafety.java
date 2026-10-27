package dev.arsconflux.internal.zero;

import dev.arsconflux.config.ConfluxStartupConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/** Reject relocation before removing blocks: a state-only capture cannot preserve inventories. */
public final class ZeroBlockSafety {
    private static final TagKey<Block> NO_RELOCATION = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "relocation_not_supported"));
    private ZeroBlockSafety() {}
    public static boolean immutable(Block block) {
        return ConfluxStartupConfig.zeroFixes() && block.defaultBlockState().is(NO_RELOCATION);
    }
    public static boolean movable(Level level, BlockPos pos, BlockState state) {
        if (!ConfluxStartupConfig.zeroFixes()) return true;
        var push = state.getPistonPushReaction();
        return !level.isOutsideBuildHeight(pos) && !state.hasBlockEntity() && !state.is(NO_RELOCATION)
                && state.getDestroySpeed(level, pos) >= 0
                && (push == PushReaction.NORMAL || push == PushReaction.PUSH_ONLY);
    }
}
