package dev.arsconflux.mixin.client;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenuSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.arsconflux.api.glyph.*;
import dev.arsconflux.internal.presentation.SpellPreviewSelection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

@Mixin(value=SpellBook.class,remap=false)
public abstract class ProjectileRadialPreviewMixin {
    @WrapOperation(method="getRadialMenuSlotsForSpellpart",at=@At(value="NEW",target="com/hollingsworth/arsnouveau/client/gui/radial_menu/RadialMenuSlot"))
    private RadialMenuSlot<AbstractSpellPart> conflux$payload(String name,Object primary,List<AbstractSpellPart> secondary,
            Operation<RadialMenuSlot<AbstractSpellPart>> original,@Local Spell spell){
        var payload=SpellPreviewSelection.payload(ArsSpellAccess.parts(spell),p->p instanceof AbstractAugment,
                p->p instanceof AbstractEffect,GlyphCategories::projectile);
        return original.call(name,payload==null?primary:payload,secondary);
    }
}
