package dev.arsconflux;
import dev.arsconflux.api.casting.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.config.ConfluxConfig;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public final class CooldownLifecycleCases {
 public static void clones(GameTestHelper h) {
  String key="fixture_"+UUID.randomUUID();
  var store=PersistentCooldowns.register(ResourceLocation.tryParse("fixture:clones_"+UUID.randomUUID()),key);
  var old=h.spawn(EntityType.COW,1,3,1);var next=h.spawn(EntityType.COW,3,3,1);
  var data=new net.minecraft.nbt.CompoundTag();data.putLong("legacy",h.getLevel().getServer().overworld().getGameTime()+80);old.getPersistentData().put(key,data);
  boolean previous=ConfluxConfig.preserveCooldownsOnDeath();
  try {
   h.assertTrue(store.remaining(old,"legacy")==80,"Reads the existing provider storage without migration");
   store.extend(old,"legacy",20);h.assertTrue(store.remaining(old,"legacy")==80,"Shorter sources cannot shorten a deadline");
   ConfluxConfig.PRESERVE_COOLDOWNS_ON_DEATH.set(true);PersistentCooldowns.copyOnClone(old,next,true);
   h.assertTrue(store.remaining(next,"legacy")==80,"Death preserves registered custom cooldowns by default");
   store.extend(next,"legacy",100);h.assertTrue(store.remaining(old,"legacy")==80,"Clone state is isolated");
   ConfluxConfig.PRESERVE_COOLDOWNS_ON_DEATH.set(false);PersistentCooldowns.copyOnClone(old,next,true);
   h.assertTrue(store.remaining(next,"legacy")==0,"Disabled death preservation clears copied deadlines");
   PersistentCooldowns.copyOnClone(old,next,false);h.assertTrue(store.remaining(next,"legacy")==80,"Non-death clones always preserve deadlines");
   h.succeed();
  } finally {ConfluxConfig.PRESERVE_COOLDOWNS_ON_DEATH.set(previous);}
 }
 public static void results(GameTestHelper h) {
  var actor=h.spawn(EntityType.COW,1,3,1);var store=PersistentCooldowns.register(ResourceLocation.tryParse("fixture:results_"+UUID.randomUUID()),"fixture_"+UUID.randomUUID());
  var spell=new Spell(MethodSelf.INSTANCE);var context=SpellContext.fromEntity(spell,actor,ItemStack.EMPTY);var resolver=new SpellResolver(context);
  ActiveCasts.execute(spell,actor,ActiveCastSource.CASTER_TOOL,()->{
   store.commit(actor,"a","grant",10);store.commit(actor,"a","grant",40);
   h.assertTrue(store.remaining(actor,"a")==0,"Result writes wait until the enclosing release ends");
   ActiveCasts.dispatched(resolver,true);return true;
  },false);
  h.assertTrue(store.remaining(actor,"a")==40,"Different successful results preserve the longest deadline");
  ActiveCasts.execute(spell,actor,ActiveCastSource.CASTER_TOOL,()->{store.commit(actor,"failed","grant",30);return false;},false);
  h.assertTrue(store.remaining(actor,"failed")==0,"Unacknowledged release does not commit a result cooldown");
  ActiveCasts.perform(spell,actor,InteractionHand.MAIN_HAND,ActiveCastSource.CASTER_TOOL,new ActiveCastPolicy(false,false),()->{
   store.commit(actor,"disabled","grant",30);ActiveCasts.dispatched(resolver,true);return true;
  },false,false);
  h.assertTrue(store.remaining(actor,"disabled")==0,"Entrypoint cooldown opt-out still applies");
  var off=new ActiveCastPolicy(false,false);off.attach(context);
  CastContexts.run(CastContext.of(context,null,null),()->store.commit(actor,"later","recovery",60));
  h.assertTrue(store.remaining(actor,"later")==0,"Later session cooldowns retain the source opt-out");
  h.succeed();
 }
}
