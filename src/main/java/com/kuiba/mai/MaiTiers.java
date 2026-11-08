package com.kuiba.mai;

import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kuiba.Kuiba;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class MaiTiers extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final MaiTiers SERVER = new MaiTiers();

    private static volatile Map<Integer, Tier> byGate = Map.of();
    private static volatile List<Tier> ordered = List.of();

    public MaiTiers() {
        super(GSON, "mai_gate");
    }

    public static @Nullable Tier get(int gate) {
        return byGate.get(gate);
    }

    public static List<Tier> all() {
        return ordered;
    }

    public static float maxPoints(int gate) {
        Tier tier = get(gate);
        return tier == null ? 100.0F : tier.maxPoints();
    }

    public static int slots(int gate) {
        Tier tier = get(gate);
        return tier == null ? 1 : tier.slots();
    }

    public static String nameKey(int gate) {
        Tier tier = get(gate);
        return tier == null ? "mai.kuiba.gate." + gate : tier.nameKey();
    }

    /** Meridian opened by reaching {@code gate}. Falls back to {@link Meridian#defaultFor}. Null for gate 0. */
    public static @Nullable Meridian meridian(int gate) {
        Tier tier = get(gate);
        if (tier != null && tier.meridian() != null) {
            return tier.meridian();
        }
        return Meridian.defaultFor(gate);
    }

    public static Set<UUID> modifierIds() {
        Set<UUID> ids = new HashSet<>();
        for (Tier tier : ordered) {
            for (ModifierSpec modifier : tier.modifiers()) {
                ids.add(modifier.modifierId());
            }
        }
        return ids;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Integer, Tier> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) {
                    Kuiba.LOGGER.warn("Skipping mai tier file {} because it is not an object", entry.getKey());
                    continue;
                }
                JsonObject root = entry.getValue().getAsJsonObject();
                if (!root.has("tiers") || !root.get("tiers").isJsonArray()) {
                    Kuiba.LOGGER.warn("Skipping mai tier file {} because it has no tiers array", entry.getKey());
                    continue;
                }
                JsonArray tiers = root.getAsJsonArray("tiers");
                for (JsonElement element : tiers) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    Tier tier = parseTier(element.getAsJsonObject(), entry.getKey());
                    if (tier != null) {
                        parsed.put(tier.gate(), tier);
                    }
                }
            } catch (Exception exception) {
                Kuiba.LOGGER.error("Failed to parse mai tiers from {}", entry.getKey(), exception);
            }
        }
        List<Tier> list = new ArrayList<>(parsed.values());
        list.sort((a, b) -> Integer.compare(a.gate(), b.gate()));
        byGate = Map.copyOf(parsed);
        ordered = List.copyOf(list);
        Kuiba.LOGGER.info("Loaded {} mai gate tiers", ordered.size());
        if (this == SERVER) {
            MaiApi.onResourcesReloaded();
        }
    }

    private static @Nullable Tier parseTier(JsonObject json, ResourceLocation file) {
        if (!json.has("gate")) {
            Kuiba.LOGGER.warn("Mai tier in {} is missing gate", file);
            return null;
        }
        int gate = json.get("gate").getAsInt();
        if (gate < 0 || gate > 12) {
            Kuiba.LOGGER.warn("Mai tier gate {} in {} is outside 0-12; skipped", gate, file);
            return null;
        }
        String nameKey = json.has("name_key") ? json.get("name_key").getAsString() : "mai.kuiba.gate." + gate;
        float maxPoints = json.has("max_points") ? json.get("max_points").getAsFloat() : 100.0F;
        int slots = json.has("slots") ? json.get("slots").getAsInt() : 1;
        slots = Mth.clamp(slots, 1, MaiData.EQUIPPED_SLOTS);
        List<ModifierSpec> modifiers = new ArrayList<>();
        if (json.has("modifiers") && json.get("modifiers").isJsonArray()) {
            int index = 0;
            for (JsonElement element : json.getAsJsonArray("modifiers")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                ModifierSpec modifier = parseModifier(element.getAsJsonObject(), gate, index, file);
                if (modifier != null) {
                    modifiers.add(modifier);
                    index++;
                }
            }
        }
        RitualDef ritual = null;
        if (json.has("ritual") && json.get("ritual").isJsonObject()) {
            ritual = parseRitual(json.getAsJsonObject("ritual"), file, gate);
        }
        Meridian meridian = null;
        if (json.has("meridian")) {
            String raw = json.get("meridian").getAsString();
            meridian = Meridian.byId(raw);
            if (meridian == null) {
                Kuiba.LOGGER.warn("Unknown meridian '{}' on gate {} in {}; using the default order", raw, gate, file);
            }
        }
        return new Tier(gate, nameKey, maxPoints, slots, List.copyOf(modifiers), ritual, meridian);
    }

    private static @Nullable ModifierSpec parseModifier(JsonObject json, int gate, int index, ResourceLocation file) {
        if (!json.has("attribute")) {
            return null;
        }
        ResourceLocation attributeId = com.kuiba.Kuiba.parse(json.get("attribute").getAsString());
        if (attributeId == null) {
            Kuiba.LOGGER.warn("Bad attribute id on gate {} in {}", gate, file);
            return null;
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
        if (attribute == null) {
            Kuiba.LOGGER.warn("Unknown attribute {} on gate {} in {}", attributeId, gate, file);
            return null;
        }
        double amount = json.has("amount") ? json.get("amount").getAsDouble() : 0.0D;
        AttributeModifier.Operation operation = parseOperation(json.has("operation") ? json.get("operation").getAsString() : "add_value");
        String modifierName = "kuiba:gate/" + gate + "/" + index + "/" + attributeId.getPath().replace('.', '_').replace('/', '_');
        UUID modifierId = com.kuiba.core.KuibaIds.uuid(modifierName);
        return new ModifierSpec(attributeId, attribute, amount, operation, modifierId);
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        return switch (raw) {
            case "add_multiplied_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    private static @Nullable RitualDef parseRitual(JsonObject json, ResourceLocation file, int gate) {
        int duration = json.has("duration_ticks") ? json.get("duration_ticks").getAsInt() : 100;
        if (duration < 1) {
            duration = 1;
        }
        List<IngredientSpec> ingredients = new ArrayList<>();
        if (json.has("ingredients") && json.get("ingredients").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("ingredients")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject ingredient = element.getAsJsonObject();
                int count = ingredient.has("count") ? ingredient.get("count").getAsInt() : 1;
                if (count < 1) {
                    continue;
                }
                ResourceLocation item = null;
                ResourceLocation tag = null;
                if (ingredient.has("item")) {
                    item = com.kuiba.Kuiba.parse(ingredient.get("item").getAsString());
                }
                if (ingredient.has("tag")) {
                    tag = com.kuiba.Kuiba.parse(ingredient.get("tag").getAsString());
                }
                if (item == null && tag == null) {
                    Kuiba.LOGGER.warn("Ritual ingredient on gate {} in {} has neither item nor tag", gate, file);
                    continue;
                }
                ingredients.add(new IngredientSpec(item, tag, count));
            }
        }
        return new RitualDef(duration, List.copyOf(ingredients));
    }

    public record Tier(
            int gate,
            String nameKey,
            float maxPoints,
            int slots,
            List<ModifierSpec> modifiers,
            @Nullable RitualDef ritual,
            @Nullable Meridian meridian) {}

    public record ModifierSpec(
            ResourceLocation attributeId,
            Attribute attribute,
            double amount,
            AttributeModifier.Operation operation,
            UUID modifierId) {}

    public record RitualDef(int durationTicks, List<IngredientSpec> ingredients) {}

    public record IngredientSpec(@Nullable ResourceLocation item, @Nullable ResourceLocation tag, int count) {}
}
