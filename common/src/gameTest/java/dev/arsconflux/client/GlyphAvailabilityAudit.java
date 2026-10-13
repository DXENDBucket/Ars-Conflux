package dev.arsconflux.client;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.validation.CombinedSpellValidator;
import com.hollingsworth.arsnouveau.common.spell.validation.GlyphMaxTierValidator;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.internal.presentation.GlyphCandidateValidation;
import java.util.*;

/** Registry-wide editor checks against actual installed provider validators. No cast/world simulation. */
final class GlyphAvailabilityAudit {
    private final ISpellValidator validator = ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator();
    private final List<String> failures = new ArrayList<>();
    private final Map<String, Integer> providers = new TreeMap<>();
    private int checked, limits, conflicts;

    static void run() {
        var audit = new GlyphAvailabilityAudit();
        GlyphCandidateValidation.scoped(audit::checkRegistry);
        var log = org.slf4j.LoggerFactory.getLogger("Ars Conflux availability audit");
        for (var failure : audit.failures.stream().limit(30).toList()) log.error(failure);
        log.info("AUDIT: {} candidate checks, {} limit boundaries, {} cross-payload conflicts, {} failures",
                audit.checked, audit.limits, audit.conflicts, audit.failures.size());
        log.info("AUDIT candidate namespaces: {}", audit.providers);
        if (!audit.failures.isEmpty()) throw new AssertionError("Glyph availability audit: " + audit.failures.size() + " failures");
    }

    private void checkRegistry() {
        var parts = new ArrayList<>(GlyphRegistry.getSpellpartMap().values());
        parts.sort(Comparator.comparing(part -> part.getRegistryName().toString()));
        var augments = parts.stream().filter(AbstractAugment.class::isInstance).toList();
        var payloads = augments.stream().filter(ProjectileForm.class::isInstance).toList();
        var projectile = MethodProjectile.INSTANCE;
        var descriptions = snapshotDescriptions(parts);
        // Valid prefixes permit an independent oracle: every resulting validator error is new.
        for (var action : parts) {
            if (action instanceof AbstractAugment) continue;
            List<AbstractSpellPart> prefix = action instanceof AbstractCastMethod ? List.of(action) : List.of(projectile, action);
            if (!validator.validate(new ArrayList<>(prefix)).isEmpty()) continue;
            for (var candidate : augments) {
                append(prefix, candidate);
                if (!action.compatibleAugments.contains(candidate)) continue;
                int limit = action.getAugmentLimit(candidate.getRegistryName());
                if (limit > 0 && limit <= 10) {
                    var atLimit = new ArrayList<>(prefix);
                    for (int i = 0; i < limit; i++) atLimit.add(candidate);
                    if (validator.validate(atLimit).isEmpty()) { append(atLimit, candidate); limits++; }
                }
            }
        }
        // All registered carriers, including NEG/Elemental, with every supported native payload.
        for (var profile : CarrierRegistry.profiles()) {
            var carrier = profile.glyph();
            if (carrier == null || !(carrier instanceof AbstractCastMethod)) continue;
            for (var payload : payloads) {
                var prefix = List.of(carrier, payload);
                if (!validator.validate(new ArrayList<>(prefix)).isEmpty()) continue;
                for (var candidate : augments) { append(prefix, candidate); if (candidate instanceof ProjectileForm) conflicts++; }
                // A following effect starts a new phrase; its modifiers must stay independent.
                var effectPrefix = List.of(carrier, payload, EffectHarm.INSTANCE);
                if (validator.validate(new ArrayList<>(effectPrefix)).isEmpty()) {
                    expect(effectPrefix, AugmentAmplify.INSTANCE, false);
                    expect(effectPrefix, payload, true);
                }
            }
        }
        // Native shape restrictions and a second cast method, not only augments.
        for (var candidate : parts) append(List.of(projectile, EffectHarm.INSTANCE), candidate);
        var tierOne = new CombinedSpellValidator(new ISpellValidator[]{validator, new GlyphMaxTierValidator(1)});
        for (var candidate : parts) {
            var prefix = List.<AbstractSpellPart>of(projectile, EffectHarm.INSTANCE);
            var recipe = new ArrayList<>(prefix); recipe.add(candidate);
            expect(tierOne, prefix, candidate, !tierOne.validate(recipe).isEmpty());
        }
        // A stale violation must not hide another invalid modifier, or poison an unrelated one.
        expect(List.of(projectile, AugmentAmplify.INSTANCE), AugmentDampen.INSTANCE, true);
        expect(List.of(projectile, AugmentAmplify.INSTANCE), AugmentAccelerate.INSTANCE, false);
        expect(List.of(projectile, AugmentExtendTime.INSTANCE), AugmentDurationDown.INSTANCE, true);
        expect(List.of(projectile, AugmentExtendTime.INSTANCE), AugmentAccelerate.INSTANCE, false);
        var fireball = optional("irons_nouveau:glyph_fireball");
        if (fireball != null) expect(List.of(projectile, fireball, fireball), AugmentAccelerate.INSTANCE, false);
        var fangs = optional("goety_nouveau:glyph_fangs");
        var absorb = optional("goety_nouveau:glyph_life_absorb");
        if (fangs != null && absorb != null) {
            expect(List.of(projectile, fangs), absorb, false);
            expect(List.of(projectile, fangs, absorb), absorb, true);
            expect(List.of(projectile, fangs, absorb), AugmentAmplify.INSTANCE, true);
            expect(List.of(projectile, fangs, AugmentAmplify.INSTANCE), absorb, true);
        }
        if (!descriptions.equals(snapshotDescriptions(parts))) throw new AssertionError("Augment description maps changed");
    }

    private void append(List<AbstractSpellPart> prefix, AbstractSpellPart candidate) {
        var recipe = new ArrayList<>(prefix); recipe.add(candidate);
        expect(prefix, candidate, !validator.validate(recipe).isEmpty());
    }

    private void expect(List<AbstractSpellPart> prefix, AbstractSpellPart candidate, boolean expectedDim) {
        expect(validator, prefix, candidate, expectedDim);
    }

    private void expect(ISpellValidator validator, List<AbstractSpellPart> prefix, AbstractSpellPart candidate, boolean expectedDim) {
        checked++;
        providers.merge(candidate.getRegistryName().getNamespace(), 1, Integer::sum);
        var recipe = new ArrayList<>(prefix); recipe.add(candidate);
        var raw = validator.validate(recipe);
        var preview = GlyphCandidateValidation.preview(validator, recipe, raw);
        boolean dim = preview.stream().anyMatch(error -> error.getPosition() >= recipe.size() - 1);
        if (dim != expectedDim) failures.add(prefix.stream().map(p -> p.getRegistryName().toString()).toList()
                + " -> " + candidate.getRegistryName() + ": expected dim=" + expectedDim + ", actual=" + dim);
        if (preview.size() != raw.size()) throw new AssertionError("Preview changed error count");
        for (int i = 0; i < raw.size(); i++) {
            if (!Objects.equals(raw.get(i).makeTextComponentExisting(), preview.get(i).makeTextComponentExisting())
                    || !Objects.equals(raw.get(i).makeTextComponentAdding(), preview.get(i).makeTextComponentAdding()))
                throw new AssertionError("Preview changed native error text");
        }
    }

    private static AbstractSpellPart optional(String id) { return GlyphRegistry.getSpellPart(CarrierRegistry.id(id)); }

    private static Map<AbstractSpellPart, Map<?, ?>> snapshotDescriptions(List<AbstractSpellPart> parts) {
        var result = new HashMap<AbstractSpellPart, Map<?, ?>>();
        try {
            var field = AbstractSpellPart.class.getField("augmentDescriptions");
            for (var part : parts) result.put(part, new HashMap<>((Map<?, ?>)field.get(part)));
        } catch (NoSuchFieldException ars4) { /* Ars 4 uses a different description API. */ }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        return result;
    }
}
