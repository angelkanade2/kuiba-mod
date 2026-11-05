package com.kuiba.mai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kuiba.Kuiba;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class MaiSkills extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final MaiSkills SERVER = new MaiSkills();
    public static final MaiSkills CLIENT = new MaiSkills();

    private static volatile Map<ResourceLocation, MaiSkill> byId = Map.of();
    private static volatile List<MaiSkill> ordered = List.of();

    public MaiSkills() {
        super(GSON, "mai_skill");
    }

    public static @Nullable MaiSkill get(ResourceLocation id) {
        return byId.get(id);
    }

    public static List<MaiSkill> all() {
        return ordered;
    }

    public static void unlockFor(MaiData data) {
        for (MaiSkill skill : ordered) {
            if (skill.unlockGate() <= data.gate()) {
                data.unlock(skill.id());
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MaiSkill> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) {
                    Kuiba.LOGGER.warn("Skipping mai skill {} because it is not an object", entry.getKey());
                    continue;
                }
                MaiSkill skill = parse(entry.getKey(), entry.getValue().getAsJsonObject());
                if (skill != null) {
                    if (parsed.containsKey(skill.id())) {
                        Kuiba.LOGGER.warn("Duplicate mai skill id {}, later file wins", skill.id());
                    }
                    parsed.put(skill.id(), skill);
                }
            } catch (Exception exception) {
                Kuiba.LOGGER.error("Failed to parse mai skill {}", entry.getKey(), exception);
            }
        }
        List<MaiSkill> list = new ArrayList<>(parsed.values());
        list.sort((a, b) -> {
            int gate = Integer.compare(a.unlockGate(), b.unlockGate());
            return gate != 0 ? gate : a.id().compareTo(b.id());
        });
        byId = Map.copyOf(parsed);
        ordered = List.copyOf(list);
        Kuiba.LOGGER.info("Loaded {} mai skills", ordered.size());
        if (this == SERVER) {
            MaiApi.onResourcesReloaded();
        }
    }

    private static @Nullable MaiSkill parse(ResourceLocation file, JsonObject json) {
        ResourceLocation id = json.has("id") ? ResourceLocation.tryParse(json.get("id").getAsString()) : file;
        if (id == null) {
            Kuiba.LOGGER.warn("Mai skill {} has an invalid id", file);
            return null;
        }
        String cast = json.has("cast") ? json.get("cast").getAsString() : "";
        if (!MaiCast.isKnown(cast)) {
            Kuiba.LOGGER.warn("Skipping mai skill {} with unknown cast '{}'", id, cast);
            return null;
        }
        String category = json.has("category") ? json.get("category").getAsString() : "misc";
        int unlockGate = json.has("unlock_gate") ? json.get("unlock_gate").getAsInt() : 0;
        if (unlockGate < 0 || unlockGate > 12) {
            Kuiba.LOGGER.warn("Skipping mai skill {} with unlock_gate {}", id, unlockGate);
            return null;
        }
        float cost = json.has("cost") ? json.get("cost").getAsFloat() : 0.0F;
        int cooldown = json.has("cooldown_ticks") ? json.get("cooldown_ticks").getAsInt() : 0;
        float power = json.has("power") ? json.get("power").getAsFloat() : 1.0F;
        float range = json.has("range") ? json.get("range").getAsFloat() : 0.0F;
        int duration = json.has("duration_ticks") ? json.get("duration_ticks").getAsInt() : 0;
        ResourceLocation icon = Kuiba.id("textures/skill/" + id.getPath() + ".png");
        if (json.has("icon")) {
            ResourceLocation parsedIcon = ResourceLocation.tryParse(json.get("icon").getAsString());
            if (parsedIcon != null) {
                icon = parsedIcon;
            }
        }
        return new MaiSkill(id, category, unlockGate, Math.max(0.0F, cost), Math.max(0, cooldown), cast, power, range, Math.max(0, duration), icon);
    }
}
