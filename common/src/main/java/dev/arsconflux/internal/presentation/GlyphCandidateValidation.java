package dev.arsconflux.internal.presentation;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.validation.SpellPhraseValidator;
import dev.arsconflux.api.projectile.ProjectileForm;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Book append previews only. Casting errors retain their original positions and messages. */
public final class GlyphCandidateValidation {
    private static final ThreadLocal<Map<Prefix, List<SpellValidationError>>> CACHE = new ThreadLocal<>();

    public static void scoped(Runnable draw) {
        var previous = CACHE.get();
        CACHE.set(new HashMap<>());
        try { draw.run(); }
        finally { if (previous == null) CACHE.remove(); else CACHE.set(previous); }
    }

    public static List<SpellValidationError> preview(ISpellValidator validator, List<AbstractSpellPart> recipe,
            List<SpellValidationError> errors) {
        int position = recipe.size() - 1;
        if (position < 0 || errors.stream().noneMatch(error -> error.getPosition() < position)) return errors;
        var prefix = List.copyOf(recipe.subList(0, position));
        var cache = CACHE.get();
        var key = new Prefix(validator, prefix);
        var previous = cache == null ? validator.validate(new ArrayList<>(prefix))
                : cache.computeIfAbsent(key, ignored -> validator.validate(new ArrayList<>(prefix)));
        var introduced = PreviewErrors.introduced(errors, previous, ErrorKey::of);
        var candidate = recipe.get(position);
        // Imported invalid recipes can already contain two payloads. A third remains invalid.
        int duplicateAction = -1;
        if (candidate instanceof ProjectileForm) {
            var phrases = SpellPhraseValidator.splitSpellIntoPhrases(recipe);
            if (!phrases.isEmpty()) {
                var last = phrases.get(phrases.size() - 1);
                if (last.getAugments().stream().filter(ProjectileForm.class::isInstance).count() > 1)
                    duplicateAction = last.getFirstPosition();
            }
        }
        var result = new ArrayList<SpellValidationError>(errors.size());
        for (var error : errors) {
            boolean relocate = error.getPosition() < position
                    && (introduced.contains(error) || error.getPosition() == duplicateAction);
            result.add(relocate ? new CandidateError(position, candidate, error) : error);
        }
        return result;
    }

    private record Prefix(ISpellValidator validator, List<AbstractSpellPart> parts) {}
    private record ErrorKey(Class<?> type, int position, ResourceLocation part, Component existing, Component adding) {
        static ErrorKey of(SpellValidationError error) {
            var part = error.getSpellPart();
            return new ErrorKey(error.getClass(), error.getPosition(), part == null ? null : part.getRegistryName(),
                    error.makeTextComponentExisting(), error.makeTextComponentAdding());
        }
    }
    private record CandidateError(int position, AbstractSpellPart spellPart, SpellValidationError original)
            implements SpellValidationError {
        public int getPosition() { return position; }
        public AbstractSpellPart getSpellPart() { return spellPart; }
        public MutableComponent makeTextComponentExisting() { return original.makeTextComponentExisting(); }
        public MutableComponent makeTextComponentAdding() { return original.makeTextComponentAdding(); }
    }
    private GlyphCandidateValidation() {}
}
