package dev.arsconflux.internal.ans;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import dev.arsconflux.platform.AnsAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Optional ANS entrypoints. ANS still owns payload validation, targeting and all payments.
 * Never retain an ANS payment frame across a chant or replay a completed native effect. */
public final class AnsActiveCasts {
    private static final String PROXY = "com.otectus.arsnspells.spell.irons.ArsCrossProxySpell";
    private record Key(Object owner, String method) {}
    private static final ThreadLocal<Set<Key>> REENTERING = new ThreadLocal<>();
    private AnsActiveCasts() {}

    /** null means continue the unmodified method. A queued cast is not yet a success. */
    public static Object carrier(Player player, ItemStack carrier, ItemStack tool, InteractionHand hand,
                                 Object entry, Object[] arguments) {
        if (!(player instanceof ServerPlayer) || guarded(player, "carrier")) return null;
        Spell spell = spell(player, entry);
        if (spell == null) return null; // Let ANS report invalid, missing or blacklisted glyphs.
        if (ActiveCasts.currentPolicy(player) != null)
            return invokeGuarded(player, "carrier", AnsAccess.castMethod(), null, arguments);
        var snapshot = carrier.copy();
        Object refused = AnsAccess.refused();
        return ActiveCasts.perform(spell, player, hand, source(tool), () -> {
            if (!ItemStack.matches(carrier, snapshot) || player.isSleeping() || player.isSpectator()
                    || player.containerMenu != player.inventoryMenu || player.getItemInHand(hand) != tool) return refused;
            return invokeGuarded(player, "carrier", AnsAccess.castMethod(), null, arguments);
        }, refused, refused);
    }

    /** Prepare before Iron opens its lifecycle. Returning true only acknowledges queued input;
     * native payments, scroll consumption and cooldowns have not run yet. */
    public static Boolean initiate(Object proxy, ItemStack tool, int level, Level world, Player player,
                                   Object castSource, boolean cooldown, String slot) {
        if (!isProxy(proxy) || !(player instanceof ServerPlayer) || guarded(proxy, "initiate")) return null;
        Object data = invoke(Native.DATA, null, player);
        if ((boolean)invoke(Native.IS_CASTING, data)) return null; // Preserve native cancel/recast handling.
        Object result = invoke(Native.CAN_CAST, proxy, level, castSource, data, player);
        if (!(boolean)invoke(Native.SUCCESS, result)) return null; // Native error feedback, without a chant.
        Object carrier = carrier(proxy, tool, player);
        if (carrier == null) return null;
        ItemStack book = book(carrier);
        Spell spell = spell(player, invoke(Native.ENTRY, carrier));
        if (spell == null) return null;
        var snapshot = book.copy();
        var hand = Native.OFFHAND.equals(slot) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return ActiveCasts.perform(spell, player, hand, source(tool), () -> {
            Object current = carrier(proxy, tool, player);
            if (current == null || book(current) != book || !ItemStack.matches(book, snapshot)
                    || (boolean)invoke(Native.IS_CASTING, data)) return false;
            return (boolean)invokeGuarded(proxy, "initiate", Native.INITIATE, proxy,
                    tool, level, world, player, castSource, cooldown, slot);
        }, false, true);
    }

    /** Iron releases instant spells on a later server tick. Wrap only the proxy effect, inside
     * ANS's existing native payment frame; never re-enter Iron's outer castSpell lifecycle. */
    public static boolean release(Object proxy, Level world, int level, LivingEntity caster,
                                  Object castSource, Object data) {
        if (!isProxy(proxy) || !(caster instanceof ServerPlayer player) || guarded(proxy, "release")) return false;
        Object carrier = invoke(Native.CASTING, null, player, data, invoke(Native.POOL, proxy));
        if (carrier == null) return false;
        Spell spell = spell(player, invoke(Native.ENTRY, carrier));
        if (spell == null) return false;
        ItemStack tool = (ItemStack)invoke(Native.CASTING_ITEM, data);
        ActiveCasts.execute(spell, player, source(tool), () -> {
            invokeGuarded(proxy, "release", Native.RELEASE, proxy, world, level, player, castSource, data);
            return true;
        }, false);
        return true;
    }

    public static boolean isProxy(Object value) { return value != null && value.getClass().getName().equals(PROXY); }
    private static ActiveCastSource source(ItemStack tool) {
        return tool != null && (tool.getItem() instanceof SwordItem || tool.getItem() instanceof DiggerItem)
                ? ActiveCastSource.WEAPON : ActiveCastSource.CASTER_TOOL;
    }
    private static Object carrier(Object proxy, ItemStack tool, Player player) {
        int pool = (int)invoke(Native.POOL, proxy);
        Object direct = invoke(Native.FROM, null, tool, pool);
        return direct != null ? direct : invoke(Native.UNAMBIGUOUS, null, player, pool);
    }
    private static ItemStack book(Object carrier) { return (ItemStack)invoke(Native.STACK, carrier); }
    private static Spell spell(Player player, Object entry) {
        if (entry == null || !AnsAccess.valid(player, entry)) return null;
        CompoundTag tag = entry instanceof CompoundTag legacy ? legacy.getCompound("ars_spell")
                : (CompoundTag)((Optional<?>)invoke(AnsApi.TAG, entry)).orElse(null);
        if (tag == null || tag.isEmpty()
                || !((List<?>)invoke(AnsApi.MISSING, null, tag)).isEmpty()
                || !((List<?>)invoke(AnsApi.BLACKLISTED, null, tag)).isEmpty()) return null;
        Spell spell = AnsAccess.decode(tag);
        return spell == null || dev.arsconflux.api.glyph.ArsSpellAccess.parts(spell).isEmpty() ? null : spell;
    }
    private static boolean guarded(Object owner, String method) {
        var keys = REENTERING.get();
        return keys != null && keys.contains(new Key(owner, method));
    }
    private static Object invokeGuarded(Object owner, String name, Method method, Object target, Object... args) {
        Set<Key> keys = REENTERING.get();
        boolean root = keys == null;
        if (root) { keys = new HashSet<>(); REENTERING.set(keys); }
        var key = new Key(owner, name);
        keys.add(key);
        try { return invoke(method, target, args); }
        finally { keys.remove(key); if (root) REENTERING.remove(); }
    }
    public static Object invoke(Method method, Object target, Object... args) {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException("ANS cast failed", failure.getCause());
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Could not invoke ANS entrypoint", failure); }
    }
    private static class AnsApi {
        static final Method TAG = AnsAccess.entryTagMethod();
        static final Method MISSING, BLACKLISTED;
        static {
            try {
                var integrity = Class.forName("com.otectus.arsnspells.util.ArsSpellIntegrity");
                MISSING = integrity.getMethod("missingGlyphIds", CompoundTag.class);
                BLACKLISTED = integrity.getMethod("blacklistedGlyphIds", CompoundTag.class);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
    private static class Native {
        static final Method DATA, IS_CASTING, CAN_CAST, SUCCESS, POOL, FROM, UNAMBIGUOUS, CASTING,
                STACK, ENTRY, CASTING_ITEM, INITIATE, RELEASE;
        static final String OFFHAND;
        static {
            try {
                var nativeSpell = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                var data = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                var castSource = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
                var resolver = Class.forName("com.otectus.arsnspells.spell.irons.ProxyCarrierResolver");
                var carrier = Class.forName("com.otectus.arsnspells.spell.irons.ProxyCarrierResolver$Carrier");
                DATA = data.getMethod("getPlayerMagicData", LivingEntity.class);
                IS_CASTING = data.getMethod("isCasting");
                CAN_CAST = nativeSpell.getMethod("canBeCastedBy", int.class, castSource, data, Player.class);
                SUCCESS = CAN_CAST.getReturnType().getMethod("isSuccess");
                POOL = Class.forName(PROXY).getMethod("getPoolId");
                FROM = resolver.getMethod("from", ItemStack.class, int.class);
                UNAMBIGUOUS = resolver.getMethod("unambiguous", Player.class, int.class);
                CASTING = resolver.getMethod("casting", Player.class, data, int.class);
                STACK = carrier.getMethod("stack"); ENTRY = carrier.getMethod("entry");
                CASTING_ITEM = data.getMethod("getPlayerCastingItem");
                OFFHAND = (String)Class.forName("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager").getField("OFFHAND").get(null);
                INITIATE = nativeSpell.getMethod("attemptInitiateCast", ItemStack.class, int.class, Level.class, Player.class, castSource, boolean.class, String.class);
                RELEASE = Class.forName(PROXY).getMethod("onCast", Level.class, int.class, LivingEntity.class, castSource, data);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
}
