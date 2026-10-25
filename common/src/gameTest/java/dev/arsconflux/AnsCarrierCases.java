package dev.arsconflux;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLight;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.api.casting.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.internal.ans.AnsActiveCasts;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.IntConsumer;

/** Real ANS carrier/inscription/binder and native Iron release, with one isolated timing provider. */
public final class AnsCarrierCases {
    public record Access(ServerPlayer player, IntConsumer mana) {}
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;
    private AnsCarrierCases() {}
    private static final class Timing implements ActiveCastProvider {
        final Player player; int releases, opens, closes; boolean enabled = true;
        final java.util.Set<UUID> tokens = new java.util.HashSet<>();
        Timing(Player player) { this.player = player; }
        public boolean supports(Spell s) { return ArsSpellAccess.parts(s).contains(EffectHeal.INSTANCE); }
        public boolean chantingEnabled(ActiveCastRequest r) { return enabled && r.caster() == player; }
        public List<Integer> chantTicks(ActiveCastRequest r) { return List.of(5); }
        public boolean allowed(ActiveCastRequest r) { return r.caster() != player || releases == 0; }
        public void released(ActiveCastRequest r, List<Spell> s) { if (r.caster() == player) releases++; }
        public void chantProgress(ActiveCastRequest r, UUID token, int duration, int remaining) {
            if (remaining > 0) { opens++; tokens.add(token); } else closes++;
        }
    }
    private static Timing setup(GameTestHelper h, Access a) {
        a.mana.accept(100);
        a.player.setHealth(5);
        var timing = new Timing(a.player);
        ActiveCasts.register(CarrierRegistry.id("ans_fixture:" + a.player.getUUID()), timing);
        return timing;
    }
    public static void scroll(GameTestHelper h, Access a, boolean loaded) {
        if (!loaded) { h.succeed(); return; }
        var timing = setup(h, a); var api = new Api();
        var stack = api.scroll(new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE));
        a.player.setItemInHand(HAND, stack);
        h.assertTrue(!api.cast(a.player, stack), "Queued carrier is not reported as a resolved spell");
        h.assertTrue(ActiveCasts.isChanting(a.player) && a.player.getHealth() == 5, "Scroll queues without running effects");
        api.cast(a.player, stack);
        h.runAfterDelay(8, () -> {
            h.assertTrue(a.player.getHealth() > 5 && timing.releases == 1, "Exported scroll releases and commits once");
            h.assertTrue(timing.tokens.size() == 1 && timing.closes == 1, "Repeated input neither restarts nor duplicates the chant");
            float health = a.player.getHealth(); api.cast(a.player, stack);
            h.assertTrue(a.player.getHealth() == health && !ActiveCasts.isChanting(a.player), "Carrier respects committed provider cooldown");
            h.succeed();
        });
    }
    public static void weaponCancellation(GameTestHelper h, Access a, boolean loaded) {
        if (!loaded) { h.succeed(); return; }
        var timing = setup(h, a); var api = new Api();
        var stack = new ItemStack(Items.IRON_SWORD);
        api.inscribe(stack, new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE));
        a.player.setItemInHand(HAND, stack); api.cast(a.player, stack);
        h.assertTrue(ActiveCasts.isChanting(a.player), "Inscribed weapon uses the same preparation");
        a.player.setItemInHand(HAND, new ItemStack(Items.STICK));
        h.runAfterDelay(8, () -> {
            h.assertTrue(!ActiveCasts.isChanting(a.player) && timing.closes == 1 && timing.releases == 0
                    && a.player.getHealth() == 5, "Switching tool closes preparation without release");
            a.player.setItemInHand(HAND, stack); api.cast(a.player, stack);
            h.runAfterDelay(8, () -> {
                h.assertTrue(timing.releases == 1 && a.player.getHealth() > 5, "Cancelled weapon can be used again");
                h.succeed();
            });
        });
    }
    public static void nativeBook(GameTestHelper h, Access a, boolean loaded, boolean emptyAtRelease) {
        if (!loaded) { h.succeed(); return; }
        var timing = setup(h, a); var api = new Api();
        var scroll = api.scroll(new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE));
        var book = new ItemStack(BuiltInRegistries.ITEM.get(CarrierRegistry.id("irons_spellbooks:iron_spell_book")));
        h.assertTrue(!book.isEmpty(), "Native Iron spellbook fixture exists");
        var bind = api.bind(a.player, scroll, book);
        h.assertTrue((boolean)api.call(bind, "wasAdded"), "Real ANS binder writes native proxy and sidecar");
        a.player.setItemInHand(HAND, book);
        Object proxy = api.proxy(book);
        h.assertTrue(AnsActiveCasts.isProxy(proxy), "Bound native slot points to ANS proxy");
        h.assertTrue(api.initiate(proxy, book, a.player), "Native input accepts deferred preparation");
        api.initiate(proxy, book, a.player);
        h.assertTrue(ActiveCasts.isChanting(a.player) && !api.casting(a.player)
                && !api.cooling(a.player, proxy), "No native cast state or native cooldown while queued");
        h.runAfterDelay(8, () -> {
            h.assertTrue(!ActiveCasts.isChanting(a.player) && api.casting(a.player), "Chant enters native lifecycle only at release");
            if (emptyAtRelease) { a.mana.accept(0); api.nativeMana(a.player, 0); }
            api.release(proxy, a.player);
            if (emptyAtRelease) {
                h.assertTrue(a.player.getHealth() == 5 && timing.releases == 0 && !api.cooling(a.player, proxy),
                        "Unpaid native delegate starts neither effects nor either cooldown");
            } else {
                h.assertTrue(a.player.getHealth() > 5 && timing.releases == 1 && api.cooling(a.player, proxy),
                        "Native release retains ANS payment frame and commits both cooldowns once");
            }
            api.reset(a.player); h.succeed();
        });
    }
    public static void ordinary(GameTestHelper h, Access a, boolean loaded) {
        if (!loaded) { h.succeed(); return; }
        var api = new Api(); a.mana.accept(100);
        var scroll = api.scroll(new Spell(MethodSelf.INSTANCE, EffectLight.INSTANCE));
        a.player.setItemInHand(HAND, scroll);
        h.assertTrue(api.cast(a.player, scroll) && !ActiveCasts.isChanting(a.player), "Ordinary Ars carrier is still immediate");
        h.succeed();
    }
    public static void indirectBookCancellation(GameTestHelper h, Access a, boolean loaded) {
        if (!loaded) { h.succeed(); return; }
        var timing = setup(h,a); var api = new Api();
        var book = new ItemStack(BuiltInRegistries.ITEM.get(CarrierRegistry.id("irons_spellbooks:iron_spell_book")));
        api.bind(a.player,api.scroll(new Spell(MethodSelf.INSTANCE,EffectHeal.INSTANCE)),book);
        Object proxy = api.proxy(book);
        var tool = new ItemStack(Items.STICK);
        a.player.setItemInHand(HAND,tool);
        a.player.setItemInHand(InteractionHand.OFF_HAND,book);
        api.initiate(proxy,tool,a.player);
        h.assertTrue(ActiveCasts.isChanting(a.player),"Native tool resolves a separate carried book");
        a.player.setItemInHand(InteractionHand.OFF_HAND,book.copy());
        h.runAfterDelay(8,()->{
            h.assertTrue(!ActiveCasts.isChanting(a.player) && !api.casting(a.player)
                    && timing.releases==0 && a.player.getHealth()==5,
                    "Replacing the source book cancels even if the casting tool did not change");
            h.succeed();
        });
    }
    private static final class Api {
        final Class<?> handler, source, nativeSpell, dataType;
        final Method handle, manaData;
        final Object castAction, spellbook;
        Api() {
            try {
                handler = Class.forName("com.otectus.arsnspells.spell.CrossCastingHandler");
                handle = Arrays.stream(handler.getMethods()).filter(m -> m.getName().equals("serverHandleCast") && m.getParameterCount() == 5).findFirst().orElseThrow();
                castAction = constant(handle.getParameterTypes()[3], "CAST");
                source = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
                spellbook = constant(source, "SPELLBOOK");
                nativeSpell = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                dataType = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                manaData = dataType.getMethod("getPlayerMagicData", LivingEntity.class);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }
        ItemStack scroll(Spell spell) { return (ItemStack)staticCall("com.otectus.arsnspells.spell.ArsSpellExportUtil", "createIronsScrollCarrier", new Class[]{Spell.class}, spell); }
        void inscribe(ItemStack stack, Spell spell) { staticCall(handler.getName(), "addCrossModSpell", new Class[]{ItemStack.class, Spell.class}, stack, spell); }
        boolean cast(ServerPlayer p, ItemStack s) { return (boolean)AnsActiveCasts.invoke(handle, null, p, s, HAND, castAction, UUID.randomUUID()); }
        Object bind(ServerPlayer p, ItemStack scroll, ItemStack book) {
            try {
                var type = Class.forName("com.otectus.arsnspells.spell.IronsSpellbookBinder");
                var method = Arrays.stream(type.getMethods()).filter(m -> m.getName().equals("bind")).findFirst().orElseThrow();
                return AnsActiveCasts.invoke(method, null, p, scroll, book, constant(method.getParameterTypes()[3], "TABLE"));
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }
        Object proxy(ItemStack book) {
            Object container = staticCall("io.redspace.ironsspellbooks.api.spells.ISpellContainer", "getOrCreate", new Class[]{ItemStack.class}, book);
            int count = (int)call(container,"getMaxSpellCount");
            for (int i=0;i<count;i++) {
                Object spell = call(call(container,"getSpellAtIndex",new Class[]{int.class},i),"getSpell");
                if (AnsActiveCasts.isProxy(spell)) return spell;
            }
            throw new IllegalStateException("Bound book has no ANS proxy in "+count+" native slots: "+book);
        }
        boolean initiate(Object proxy, ItemStack tool, ServerPlayer p) {
            String slot = field("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager", "MAINHAND").toString();
            return (boolean)call(proxy, "attemptInitiateCast", new Class[]{ItemStack.class,int.class,Level.class,Player.class,source,boolean.class,String.class},tool,1,p.level(),p,spellbook,true,slot);
        }
        void release(Object proxy, ServerPlayer p) { call(proxy,"castSpell",new Class[]{Level.class,int.class,ServerPlayer.class,source,boolean.class},p.level(),1,p,spellbook,true); }
        boolean casting(ServerPlayer p) { return (boolean)call(data(p),"isCasting"); }
        boolean cooling(ServerPlayer p,Object spell) { return (boolean)call(call(data(p),"getPlayerCooldowns"),"isOnCooldown",new Class[]{nativeSpell},spell); }
        void nativeMana(ServerPlayer p,float mana) { call(data(p),"setMana",new Class[]{float.class},mana); }
        void reset(ServerPlayer p) { call(data(p),"resetCastingState"); }
        Object data(ServerPlayer p) { return AnsActiveCasts.invoke(manaData,null,p); }
        Object call(Object target,String name) { return call(target,name,new Class[0]); }
        Object call(Object target,String name,Class<?>[] types,Object... args) {
            try { return AnsActiveCasts.invoke(target.getClass().getMethod(name,types),target,args); }
            catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
        }
        Object staticCall(String type,String name,Class<?>[] types,Object... args) {
            try { return AnsActiveCasts.invoke(Class.forName(type).getMethod(name,types),null,args); }
            catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
        }
        Object field(String type,String name) {
            try { return Class.forName(type).getField(name).get(null); }
            catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
        }
        Object constant(Class<?> type,String name) { return Arrays.stream(type.getEnumConstants()).filter(v -> ((Enum<?>)v).name().equals(name)).findFirst().orElseThrow(); }
    }
}
