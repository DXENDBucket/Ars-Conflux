package dev.arsconflux.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsconflux.internal.presentation.GlyphCandidateValidation;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GuiSpellBook.class, remap = false)
public abstract class GlyphCandidatePreviewMixin {
    @WrapMethod(method = "validate(Lcom/hollingsworth/arsnouveau/api/spell/ISpellValidator;)V")
    private void conflux$previewScope(ISpellValidator validator, Operation<Void> original) {
        GlyphCandidateValidation.scoped(() -> original.call(validator));
    }
    @WrapOperation(method = "validate(Lcom/hollingsworth/arsnouveau/api/spell/ISpellValidator;)V",
            at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/ISpellValidator;validate(Ljava/util/List;)Ljava/util/List;", ordinal = 1))
    private List<SpellValidationError> conflux$candidate(ISpellValidator validator, List<AbstractSpellPart> recipe,
            Operation<List<SpellValidationError>> original) {
        return GlyphCandidateValidation.preview(validator, recipe, original.call(validator, recipe));
    }
}
