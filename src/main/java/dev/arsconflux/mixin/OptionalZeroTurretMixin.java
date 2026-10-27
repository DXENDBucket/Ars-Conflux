package dev.arsconflux.mixin;

import com.github.ars_zero.common.block.MultiphaseSpellTurretTile;
import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.internal.zero.ZeroSourceBilling;
import dev.arsconflux.internal.zero.ZeroTemporal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="com.github.ars_zero.common.block.MultiphaseSpellTurretTile",remap=false)
public abstract class OptionalZeroTurretMixin {
    @Unique private int arsConflux$paidSource;
    @WrapMethod(method="shootSpell")
    private void arsConflux$sourceScope(Operation<Void> original) {
        int previous=arsConflux$paidSource; arsConflux$paidSource=0;
        try { original.call(); } finally { arsConflux$paidSource=previous; }
    }
    @ModifyExpressionValue(method="shootSpell",at=@At(value="INVOKE",target="Lcom/github/ars_zero/common/block/MultiphaseSpellTurretTile;getManaCost()I"))
    private int arsConflux$quote(int cost) { arsConflux$paidSource=cost; return cost; }
    @WrapOperation(method="shootSpell",at=@At(value="INVOKE",target="Lcom/github/ars_zero/common/block/MultiphaseSpellTurretTile;wrapResolverForPhase(Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;Lcom/github/ars_zero/common/spell/SpellPhase;)Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;"))
    private SpellResolver arsConflux$paid(MultiphaseSpellTurretTile tile,SpellResolver resolver,SpellPhase phase,Operation<SpellResolver> original) {
        var wrapped=original.call(tile,resolver,phase);
        if(ConfluxStartupConfig.zeroFixes())ZeroSourceBilling.prepareSourceCast(wrapped.spellContext,arsConflux$paidSource);
        ZeroTemporal.begin(wrapped,phase);
        return wrapped;
    }
}
