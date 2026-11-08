package com.kuiba.core;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.kuiba.Kuiba;
import com.kuiba.mai.payload.CastComboPayload;
import com.kuiba.mai.payload.CastSkillPayload;
import com.kuiba.mai.payload.CastWeaponArtPayload;
import com.kuiba.mai.payload.ComboStatePayload;
import com.kuiba.mai.payload.EquipSkillPayload;
import com.kuiba.mai.payload.FixedComboPayload;
import com.kuiba.mai.payload.MaiAuraPayload;
import com.kuiba.mai.payload.MaiFxPayload;
import com.kuiba.mai.payload.MaiInputPayload;
import com.kuiba.mai.payload.OpenMaiPayload;
import com.kuiba.mai.payload.RitualProgressPayload;
import com.kuiba.mai.payload.SaveComboPayload;
import com.kuiba.mai.payload.SelectComboPayload;
import com.kuiba.mai.payload.StartRitualPayload;
import com.kuiba.mai.payload.SyncMaiPayload;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class KuibaNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Kuiba.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int nextId;

    private KuibaNetwork() {}

    public static void register() {
        client(SyncMaiPayload.class, SyncMaiPayload::encode, SyncMaiPayload::decode, SyncMaiPayload::handle);
        client(RitualProgressPayload.class, RitualProgressPayload::encode, RitualProgressPayload::decode, RitualProgressPayload::handle);
        client(MaiAuraPayload.class, MaiAuraPayload::encode, MaiAuraPayload::decode, MaiAuraPayload::handle);
        client(MaiFxPayload.class, MaiFxPayload::encode, MaiFxPayload::decode, MaiFxPayload::handle);
        client(ComboStatePayload.class, ComboStatePayload::encode, ComboStatePayload::decode, ComboStatePayload::handle);
        server(CastSkillPayload.class, CastSkillPayload::encode, CastSkillPayload::decode, CastSkillPayload::handle);
        server(EquipSkillPayload.class, EquipSkillPayload::encode, EquipSkillPayload::decode, EquipSkillPayload::handle);
        server(StartRitualPayload.class, StartRitualPayload::encode, StartRitualPayload::decode, StartRitualPayload::handle);
        server(SaveComboPayload.class, SaveComboPayload::encode, SaveComboPayload::decode, SaveComboPayload::handle);
        server(SelectComboPayload.class, SelectComboPayload::encode, SelectComboPayload::decode, SelectComboPayload::handle);
        server(FixedComboPayload.class, FixedComboPayload::encode, FixedComboPayload::decode, FixedComboPayload::handle);
        server(CastComboPayload.class, CastComboPayload::encode, CastComboPayload::decode, CastComboPayload::handle);
        server(CastWeaponArtPayload.class, CastWeaponArtPayload::encode, CastWeaponArtPayload::decode, CastWeaponArtPayload::handle);
        server(OpenMaiPayload.class, OpenMaiPayload::encode, OpenMaiPayload::decode, OpenMaiPayload::handle);
        server(MaiInputPayload.class, MaiInputPayload::encode, MaiInputPayload::decode, MaiInputPayload::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToTrackingAndSelf(Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static void sendToTracking(Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

    public static void sendNear(ServerLevel level, double x, double y, double z, double radius, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, radius, level.dimension())), message);
    }

    private static <T> void client(Class<T> type, BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        message(type, encoder, decoder, handler);
    }

    private static <T> void server(Class<T> type, BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        message(type, encoder, decoder, handler);
    }

    private static <T> void message(Class<T> type, BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
            Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
        CHANNEL.messageBuilder(type, nextId++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(handler)
                .add();
    }
}
