package dev.arsconflux.api.casting;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import java.util.List;
import java.util.UUID;
/** Providers own native timing, cooldown storage and presentation; Core owns the active-use boundary.
 * Callbacks run synchronously on the server. Queries must not spend resources or mutate cooldowns. */
public interface ActiveCastProvider {
    boolean supports(Spell spell);
    default boolean chantingEnabled(ActiveCastRequest request) { return false; }
    /** One effective preparation time per written occurrence. Never return channel/use-item lifetime. */
    default List<Integer> chantTicks(ActiveCastRequest request) { return List.of(); }
    /** May show the native failure message. Called before preparation and again before release. */
    default boolean allowed(ActiveCastRequest request) { return true; }
    /** New providers return a quiet structured rejection. The legacy allowed callback remains
     * compatible, but old implementations may still display their own messages. */
    default CastRejection rejection(ActiveCastRequest request) {
        return allowed(request) ? null : new CastRejection(net.minecraft.network.chat.Component.translatable("ars_conflux.cast.denied"), -1);
    }
    /** An acknowledged release, not a successful future hit. Deduplicate native cooldown keys here. */
    default void released(ActiveCastRequest request, List<Spell> dispatchedRecipes) {}
    /** remaining == 0 closes the token, including cancellation. Never initiate a native cast here. */
    default void chantProgress(ActiveCastRequest request, UUID token, int duration, int remaining) {}
}
