package com.kuiba.wenyao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kuiba.Kuiba;
import com.kuiba.item.KuibaComponents;
import com.kuiba.mai.MaiApi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right-click weapon arts. Cooldown is stored apart from skill cooldowns. */
public final class WeaponArts extends SimpleJsonResourceReloadListener {
    // GSON must be initialised before the listener instances that pass it to super().
    private static final Gson GSON = new Gson();
    public static final WeaponArts SERVER = new WeaponArts();
    public static final WeaponArts CLIENT = new WeaponArts();
    public static final ResourceKey<DamageType> MAI_WEAPON = ResourceKey.create(Registries.DAMAGE_TYPE, Kuiba.id("mai_weapon"));

    private static volatile Map<ResourceLocation, Art> byId = Map.of();

    public WeaponArts() {
        super(GSON, "weapon_art");
    }

    public static @Nullable Art get(ResourceLocation id) {
        return byId.get(id);
    }

    public static void tryCast(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        ResourceLocation artId = stack.get(KuibaComponents.WEAPON_ART.get());
        if (artId == null) {
            player.displayClientMessage(Component.translatable("message.kuiba.art.none"), true);
            return;
        }
        Art art = get(artId);
        if (art == null) {
            player.displayClientMessage(Component.translatable("message.kuiba.art.none"), true);
            return;
        }
        if (art.weapon() != null) {
            Item expected = BuiltInRegistries.ITEM.get(art.weapon());
            if (expected == null || !stack.is(expected)) {
                player.displayClientMessage(Component.translatable("message.kuiba.art.none"), true);
                return;
            }
        }
        if (!MaiApi.meetsGate(player, art.unlockGate())) {
            player.displayClientMessage(Component.translatable("message.kuiba.art.locked", art.unlockGate()), true);
            return;
        }
        if (MaiApi.data(player).artCooldown(art.id()) > 0) {
            player.displayClientMessage(Component.translatable("message.kuiba.art.cooldown"), true);
            return;
        }
        if (MaiApi.isMaiClosed(player)) {
            player.displayClientMessage(Component.translatable("message.kuiba.open_mai.closed"), true);
            return;
        }
        if (!MaiApi.tryConsume(player, art.cost(), art.id())) {
            player.displayClientMessage(Component.translatable("message.kuiba.art.no_points"), true);
            return;
        }
        MaiApi.data(player).setArtCooldown(art.id(), art.cooldownTicks());
        float scale = WenyaoApi.artPowerScale(player);
        if ("pierce".equals(art.cast())) {
            pierce(player, art, scale);
        } else {
            rift(player, art, scale);
        }
        player.displayClientMessage(Component.translatable("message.kuiba.art.cast", Component.translatable(art.nameKey())), true);
        MaiApi.sync(player);
    }

    private static void rift(ServerPlayer player, Art art, float scale) {
        Vec3 look = flatLook(player);
        double distance = art.range() * scale;
        Vec3 from = player.position();
        Vec3 to = clipForward(player, from, look, distance);
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
        player.hurtMarked = true;
        DamageSource source = source(player);
        AABB box = new AABB(from, to).inflate(0.8D);
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())) {
            entity.hurt(source, art.power() * scale);
        }
    }

    private static void pierce(ServerPlayer player, Art art, float scale) {
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(look.scale(art.range()));
        AABB box = player.getBoundingBox().expandTowards(look.scale(art.range())).inflate(0.6D);
        List<LivingEntity> hits = new ArrayList<>(player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity != player && entity.isAlive() && entity.getBoundingBox().clip(eye, end).isPresent()));
        hits.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        DamageSource source = source(player);
        if (!hits.isEmpty()) {
            hits.get(0).hurt(source, art.power() * scale);
        }
        if (WenyaoApi.resonating(player) && hits.size() > 1) {
            hits.get(1).hurt(source, art.power() * scale * 0.5F);
        }
    }

    private static Vec3 clipForward(ServerPlayer player, Vec3 from, Vec3 look, double distance) {
        Vec3 aimed = from.add(0.0D, player.getBbHeight() * 0.5D, 0.0D).add(look.scale(distance));
        HitResult hit = player.level().clip(new ClipContext(
                from.add(0.0D, player.getBbHeight() * 0.5D, 0.0D),
                aimed,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        Vec3 stop = hit.getType() == HitResult.Type.MISS ? aimed : hit.getLocation().subtract(look.scale(0.35D));
        return new Vec3(stop.x, from.y, stop.z);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
    }

    private static DamageSource source(ServerPlayer player) {
        var registry = player.level().registryAccess().registry(Registries.DAMAGE_TYPE);
        if (registry.isPresent() && registry.get().getHolder(MAI_WEAPON).isPresent()) {
            return new DamageSource(registry.get().getHolder(MAI_WEAPON).get(), player);
        }
        return player.damageSources().playerAttack(player);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, Art> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            try {
                Art art = parse(entry.getKey(), entry.getValue().getAsJsonObject());
                if (art != null) {
                    parsed.put(art.id(), art);
                }
            } catch (RuntimeException exception) {
                Kuiba.LOGGER.error("Failed to parse weapon art {}", entry.getKey(), exception);
            }
        }
        byId = Map.copyOf(parsed);
        Kuiba.LOGGER.info("Loaded {} weapon arts", byId.size());
    }

    private static @Nullable Art parse(ResourceLocation file, JsonObject json) {
        ResourceLocation id = json.has("id") ? ResourceLocation.tryParse(json.get("id").getAsString()) : file;
        if (id == null) {
            return null;
        }
        String cast = json.has("cast") ? json.get("cast").getAsString() : "rift_flash";
        if (!"rift_flash".equals(cast) && !"pierce".equals(cast)) {
            Kuiba.LOGGER.warn("Skipping weapon art {} with unknown cast '{}'", id, cast);
            return null;
        }
        int gate = json.has("unlock_gate") ? json.get("unlock_gate").getAsInt() : 0;
        float cost = json.has("cost") ? json.get("cost").getAsFloat() : 0.0F;
        int cooldown = json.has("cooldown_ticks") ? json.get("cooldown_ticks").getAsInt() : 0;
        float range = json.has("range") ? json.get("range").getAsFloat() : 2.0F;
        float power = json.has("power") ? json.get("power").getAsFloat() : 8.0F;
        ResourceLocation weapon = null;
        if (json.has("weapon")) {
            weapon = ResourceLocation.tryParse(json.get("weapon").getAsString());
        }
        return new Art(id, gate, cost, cooldown, cast, range, power, weapon);
    }

    public record Art(
            ResourceLocation id,
            int unlockGate,
            float cost,
            int cooldownTicks,
            String cast,
            float range,
            float power,
            @Nullable ResourceLocation weapon) {
        public String nameKey() {
            return "art.kuiba." + id.getPath();
        }
    }
}
