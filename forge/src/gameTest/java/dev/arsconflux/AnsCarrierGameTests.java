package dev.arsconflux;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("ars_conflux_ans") @PrefixGameTestTemplate(false)
public final class AnsCarrierGameTests {
    private static AnsCarrierCases.Access player(GameTestHelper h) {
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AnsCarrier");
        net.minecraft.server.level.ServerPlayer player;
        player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile);
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener) {}
        };
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,3,2)));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player).orElseThrow(IllegalStateException::new);
        mana.setMaxMana(10000);
        return new AnsCarrierCases.Access(player, amount -> {
            mana.setMana(amount);
            try {
                var type=Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                var data=type.getMethod("getPlayerMagicData",net.minecraft.world.entity.LivingEntity.class).invoke(null,player);
                type.getMethod("setMana",float.class).invoke(data,(float)amount);
            } catch(ClassNotFoundException absent) {} catch(ReflectiveOperationException failure) {throw new RuntimeException(failure);}
        });
    }
    private static boolean loaded() {return net.minecraftforge.fml.ModList.get().isLoaded("ars_n_spells");}
    @GameTest(template="empty",timeoutTicks=60) public static void scrollWaitsAndChecksCooldown(GameTestHelper h) {AnsCarrierCases.scroll(h,player(h),loaded());}
    @GameTest(template="empty",timeoutTicks=60) public static void weaponCancelsAndCanRetry(GameTestHelper h) {AnsCarrierCases.weaponCancellation(h,player(h),loaded());}
    @GameTest(template="empty",timeoutTicks=60) public static void nativeBookCompletesOnce(GameTestHelper h) {AnsCarrierCases.nativeBook(h,player(h),loaded(),false);}
    @GameTest(template="empty",timeoutTicks=60) public static void unpaidBookDoesNotStartCooldown(GameTestHelper h) {AnsCarrierCases.nativeBook(h,player(h),loaded(),true);}
    @GameTest(template="empty",timeoutTicks=30) public static void ordinaryArsRemainsImmediate(GameTestHelper h) {AnsCarrierCases.ordinary(h,player(h),loaded());}
    @GameTest(template="empty",timeoutTicks=60) public static void indirectBookReplacementCancels(GameTestHelper h) {AnsCarrierCases.indirectBookCancellation(h,player(h),loaded());}
}
