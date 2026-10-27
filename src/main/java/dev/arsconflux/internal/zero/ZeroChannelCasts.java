package dev.arsconflux.internal.zero;

import com.github.ars_zero.common.item.multi.AbstractMultiPhaseCastDevice;
import com.github.ars_zero.common.spell.MultiPhaseCastContext;
import com.github.ars_zero.common.spell.SpellPhase;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.config.ConfluxConfig;
import dev.arsconflux.config.ConfluxStartupConfig;
import dev.arsconflux.mixin.OptionalZeroCurioAccess;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Native Zero owns phase dispatch, targeting, visuals and cleanup; Core owns active admission. */
public final class ZeroChannelCasts {
    private static final Map<UUID, List<Channel>> CHANNELS = new HashMap<>();
    private static final class Channel {
        final ServerPlayer player; final ServerLevel world; final ItemStack stack, snapshot;
        final boolean curio; final List<Spell> plan;
        ActiveCasts.Sequence use; boolean closed;
        Channel(ServerPlayer player, ItemStack stack, boolean curio) {
            this.player=player; this.world=player.serverLevel(); this.stack=stack; this.snapshot=stack.copy();
            this.curio=curio; this.plan=plan(stack);
        }
    }
    private ZeroChannelCasts() {}
    private static Channel find(Player player, ItemStack stack) {
        return CHANNELS.getOrDefault(player.getUUID(), List.of()).stream().filter(c -> c.stack==stack).findFirst().orElse(null);
    }
    public static void begin(Player player, ItemStack stack, MultiPhaseCastContext.CastSource source, Runnable original) {
        if (!(player instanceof ServerPlayer server) || !ConfluxStartupConfig.zero()) { original.run(); return; }
        var old=find(player,stack);
        if(old!=null && !old.closed) return;
        if(old!=null) remove(old);
        var channel=new Channel(server,stack,source==MultiPhaseCastContext.CastSource.CURIO);
        CHANNELS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(channel);
        var hand=player.getOffhandItem()==stack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var policy=new ActiveCastPolicy(ConfluxConfig.zeroChanting(),ConfluxConfig.zeroCooldowns());
        var guard=new ActiveCastGuard(!channel.curio, () -> live(channel)
                && (channel.use!=null || ItemStack.matches(channel.stack,channel.snapshot)));
        var result=ActiveCasts.prepareSequence(channel.plan,player,hand,ActiveCastSource.CASTER_TOOL,policy,guard,use -> {
            if(channel.closed) { use.close(); return; }
            channel.use=use;
            try { original.run(); }
            catch(RuntimeException | Error failure) { cancel(channel); throw failure; }
        });
        if(result==ActiveCasts.Deferral.DENIED) channel.closed=true;
    }
    public static boolean phase(Player player,ItemStack stack,Spell spell,SpellPhase phase,Supplier<Boolean> original) {
        var channel=find(player,stack);
        if(channel==null) return original.get();
        if(channel.closed || channel.use==null) return false;
        if(!ArsSpellAccess.parts(channel.plan.get(phase.ordinal())).equals(ArsSpellAccess.parts(spell))) {
            cancel(channel); return false;
        }
        return ActiveCasts.executeSequence(channel.use,spell,original,false);
    }
    public static void end(Player player,ItemStack stack,Runnable original) {
        var channel=find(player,stack);
        try { original.run(); }
        finally { if(channel!=null) { channel.closed=true; if(channel.use!=null)channel.use.close(); remove(channel); } }
    }
    private static List<Spell> plan(ItemStack stack) {
        var caster=SpellCasterRegistry.from(stack);
        if(caster==null) return List.of(new Spell(),new Spell(),new Spell());
        int slot=Math.max(0,Math.min(9,caster.getCurrentSlot()));
        boolean populated=false;
        for(int i=0;i<3;i++)populated |= !caster.getSpell(slot*3+i).isEmpty();
        if(!populated) for(int candidate=0;candidate<10;candidate++) {
            for(int i=0;i<3;i++) if(!caster.getSpell(candidate*3+i).isEmpty()) {slot=candidate;populated=true;break;}
            if(populated)break;
        }
        return List.of(caster.getSpell(slot*3),caster.getSpell(slot*3+1),caster.getSpell(slot*3+2));
    }
    private static boolean live(Channel c) {
        if(c.closed || !ConfluxStartupConfig.zero() || !c.player.isAlive() || c.player.hasDisconnected()
                || c.player.level()!=c.world) return false;
        return c.curio ? OptionalZeroCurioAccess.arsConflux$active().contains(c.player.getUUID())
                && OptionalZeroCurioAccess.arsConflux$circlet(c.player).orElse(ItemStack.EMPTY)==c.stack
                : c.player.isUsingItem() && c.player.getUseItem()==c.stack;
    }
    private static void remove(Channel c) {
        var list=CHANNELS.get(c.player.getUUID());
        if(list!=null) { list.remove(c); if(list.isEmpty())CHANNELS.remove(c.player.getUUID()); }
    }
    private static void cancel(Channel c) {
        c.closed=true;
        if(c.use!=null)c.use.close();
        try { if(c.stack.getItem() instanceof AbstractMultiPhaseCastDevice device)device.endPhase(c.player,c.stack); }
        finally { remove(c); }
    }
    public static void tick() {
        var channels=CHANNELS.values().stream().flatMap(Collection::stream).toList();
        for(var c:channels) if(!live(c) || c.use==null && !ActiveCasts.isChanting(c.player)) cancel(c);
    }
    public static void stop() {
        for(var c:CHANNELS.values().stream().flatMap(Collection::stream).toList())cancel(c);
        CHANNELS.clear();
    }
    public static void unload(net.minecraft.world.level.LevelAccessor level) {
        for(var c:CHANNELS.values().stream().flatMap(Collection::stream).toList())if(c.world==level)cancel(c);
    }
}
