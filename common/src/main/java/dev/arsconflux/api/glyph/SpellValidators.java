package dev.arsconflux.api.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.validation.SpellPhraseValidator;
import dev.arsconflux.api.projectile.ProjectileForm;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Function;

/** Providers contribute validation without each installing another Ars validator mixin. */
public final class SpellValidators {
    private static final Map<ResourceLocation, Function<List<AbstractSpellPart>, List<SpellValidationError>>> RULES = new LinkedHashMap<>();
    private SpellValidators() {}
    public static void register(ResourceLocation provider, Function<List<AbstractSpellPart>, List<SpellValidationError>> validator) {
        if (RULES.putIfAbsent(provider, validator) != null) throw new IllegalArgumentException("Duplicate validator " + provider);
    }
    public static List<SpellValidationError> validate(List<AbstractSpellPart> recipe) {
        var errors = new ArrayList<SpellValidationError>();
        for (var phrase : SpellPhraseValidator.splitSpellIntoPhrases(recipe))
            if (phrase.getAugments().stream().filter(ProjectileForm.class::isInstance).count() > 1)
                errors.add(new PayloadError(phrase.getFirstPosition(), phrase.getAction()));
        for (var rule : RULES.values()) errors.addAll(rule.apply(recipe));
        return errors;
    }
    private record PayloadError(int position, AbstractSpellPart spellPart) implements SpellValidationError {
        public int getPosition() { return position; }
        public AbstractSpellPart getSpellPart() { return spellPart; }
        public MutableComponent makeTextComponentExisting() { return Component.translatable("ars_conflux.validation.one_payload"); }
        public MutableComponent makeTextComponentAdding() { return makeTextComponentExisting(); }
    }
}
