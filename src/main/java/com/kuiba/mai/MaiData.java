package com.kuiba.mai;

import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Persisted mai state. {@code max} is intentionally not written.
 */
public final class MaiData implements INBTSerializable<CompoundTag> {
    public static final int EQUIPPED_SLOTS = 4;

    private int gate;
    private float current;
    private boolean initialized;
    private final ResourceLocation[] equipped = new ResourceLocation[EQUIPPED_SLOTS];
    private final Set<ResourceLocation> unlocked = new HashSet<>();
    private int backlashStacks;
    private long backlashStickyUntil;
    private long lastCombatTick;
    private long regenPauseUntil;
    /** Ticks until 开脉 can be used again. Not a skill or weapon-art cooldown. */
    private int openMaiCooldown;
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    /** Weapon-art cooldowns. Separate from skill cooldowns. */
    private final Map<ResourceLocation, Integer> artCooldowns = new HashMap<>();
    @Nullable
    private RitualState ritual;
    /** Empty until the wenyao screen exists. Persisted. Kept on death when {@code keepWenyaoOnDeath}. */
    private ItemStack wenyaoSlot = ItemStack.EMPTY;
    /** Persisted named presets. Presets marked {@code onWheel} show up on the 技能圆盘. */
    private final MaiCombo.Preset[] combos = emptyCombos();
    /** Preset index currently picked on the wheel, or {@link MaiCombo#NO_COMBO}. Persisted. */
    private int selectedCombo = MaiCombo.NO_COMBO;
    /** Preset index the fixed combo key casts, independent of the wheel pick. Persisted. */
    private int fixedCombo = 1;
    /**
     * Ephemeral sequence cursor. Not written to NBT, so copyOnDeath does not keep it.
     * Cleared on death and disconnect.
     */
    @Nullable
    private MaiCombo.Runtime comboRuntime;
    /** Client display cap from sync. Negative means "use the tier table". */
    private float syncedMax = -1.0F;
    /** Server scratch: last aura (gate, level) broadcast to trackers. Not persisted. */
    private int lastAuraGate = -1;
    private int lastAuraLevel = -1;
    /** Live client movement input: W/S as {@code inputForward}, A/D as {@code inputStrafe}. Not persisted. */
    private int inputForward;
    private int inputStrafe;
    /** Game time until which fall damage is ignored. Set by 脉闪, never serialized. */
    private long noFallUntil;

    public int gate() {
        return gate;
    }

    public void setGate(int gate) {
        this.gate = Mth.clamp(gate, 0, 12);
    }

    public float current() {
        return current;
    }

    public void setCurrent(float current) {
        setCurrent(current, shownMax());
    }

    public void setCurrent(float current, float cap) {
        if (current < 0.0F) {
            current = 0.0F;
        }
        if (cap < 0.0F) {
            cap = 0.0F;
        }
        if (current > cap) {
            current = cap;
        }
        this.current = current;
    }

    /** Tier table only. Wenyao bonus is applied in {@link MaiApi#getMax}. Never persisted. */
    public float tierMax() {
        return MaiTiers.maxPoints(gate);
    }

    /** Prefer {@link #tierMax()} on the attachment, or {@link MaiApi#getMax} for the player cap. */
    public float max() {
        return tierMax();
    }

    public void setSyncedMax(float max) {
        this.syncedMax = max;
    }

    /** Tier max, or the server-sent cap after a sync. */
    public float shownMax() {
        return syncedMax >= 0.0F ? syncedMax : tierMax();
    }

    /** @return true when the aura differs from the last broadcast; records the new value. */
    public boolean updateAura(int gate, int level) {
        if (gate == lastAuraGate && level == lastAuraLevel) {
            return false;
        }
        lastAuraGate = gate;
        lastAuraLevel = level;
        return true;
    }

    public boolean initialized() {
        return initialized;
    }

    public void markInitialized() {
        this.initialized = true;
    }

    public ResourceLocation[] equipped() {
        return equipped;
    }

    public @Nullable ResourceLocation equipped(int slot) {
        if (slot < 0 || slot >= EQUIPPED_SLOTS) {
            return null;
        }
        return equipped[slot];
    }

    public void setEquipped(int slot, @Nullable ResourceLocation skill) {
        if (slot < 0 || slot >= EQUIPPED_SLOTS) {
            return;
        }
        equipped[slot] = skill;
    }

    public void trimEquipped(int openSlots) {
        for (int i = Math.max(0, openSlots); i < EQUIPPED_SLOTS; i++) {
            equipped[i] = null;
        }
    }

    public Set<ResourceLocation> unlocked() {
        return unlocked;
    }

    public boolean isUnlocked(ResourceLocation skill) {
        return unlocked.contains(skill);
    }

    public void unlock(ResourceLocation skill) {
        unlocked.add(skill);
    }

    public void clearUnlocked() {
        unlocked.clear();
    }

    public int backlashStacks() {
        return backlashStacks;
    }

    public long backlashStickyUntil() {
        return backlashStickyUntil;
    }

    public long lastCombatTick() {
        return lastCombatTick;
    }

    public void setLastCombatTick(long tick) {
        this.lastCombatTick = tick;
    }

    public long regenPauseUntil() {
        return regenPauseUntil;
    }

    public void setRegenPauseUntil(long tick) {
        this.regenPauseUntil = tick;
    }

    public int openMaiCooldown() {
        return openMaiCooldown;
    }

    public void setOpenMaiCooldown(int ticks) {
        this.openMaiCooldown = Math.max(0, ticks);
    }

    public void tickOpenMaiCooldown() {
        if (openMaiCooldown > 0) {
            openMaiCooldown--;
        }
    }

    public int cooldown(ResourceLocation skill) {
        return cooldowns.getOrDefault(skill, 0);
    }

    public void setCooldown(ResourceLocation skill, int ticks) {
        if (ticks <= 0) {
            cooldowns.remove(skill);
        } else {
            cooldowns.put(skill, ticks);
        }
    }

    public Map<ResourceLocation, Integer> cooldownsView() {
        return cooldowns;
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    public int artCooldown(ResourceLocation art) {
        return artCooldowns.getOrDefault(art, 0);
    }

    public void setArtCooldown(ResourceLocation art, int ticks) {
        if (ticks <= 0) {
            artCooldowns.remove(art);
        } else {
            artCooldowns.put(art, ticks);
        }
    }

    public boolean tickArtCooldowns() {
        return tickMap(artCooldowns);
    }

    /** @return true when a cooldown entry expired this tick */
    public boolean tickCooldowns() {
        return tickMap(cooldowns);
    }

    private static boolean tickMap(Map<ResourceLocation, Integer> map) {
        if (map.isEmpty()) {
            return false;
        }
        boolean removed = false;
        Iterator<Map.Entry<ResourceLocation, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry = it.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                it.remove();
                removed = true;
            } else {
                entry.setValue(next);
            }
        }
        return removed;
    }

    private static CompoundTag writeCooldownMap(Map<ResourceLocation, Integer> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            tag.putInt(entry.getKey().toString(), entry.getValue());
        }
        return tag;
    }

    private static void readCooldownMap(CompoundTag tag, Map<ResourceLocation, Integer> into) {
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = com.kuiba.Kuiba.parse(key);
            if (id != null) {
                into.put(id, tag.getInt(key));
            }
        }
    }

    public @Nullable RitualState ritual() {
        return ritual;
    }

    public void setRitual(@Nullable RitualState ritual) {
        this.ritual = ritual;
    }

    public ItemStack wenyaoSlot() {
        return wenyaoSlot.copy();
    }

    public void setWenyaoSlot(ItemStack stack) {
        this.wenyaoSlot = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public void clearWenyaoSlot() {
        this.wenyaoSlot = ItemStack.EMPTY;
    }

    public MaiCombo.Preset combo(int slot) {
        if (slot < 0 || slot >= combos.length) {
            return MaiCombo.Preset.EMPTY;
        }
        return combos[slot];
    }

    public void setCombo(int slot, MaiCombo.Preset preset) {
        if (slot < 0 || slot >= combos.length) {
            return;
        }
        combos[slot] = preset == null ? MaiCombo.Preset.EMPTY : preset;
    }

    /** Preset index picked on the 技能圆盘, or {@link MaiCombo#NO_COMBO}. */
    public int selectedCombo() {
        return selectedCombo;
    }

    public void setSelectedCombo(int index) {
        selectedCombo = index >= 0 && index < combos.length && !combos[index].empty() ? index : MaiCombo.NO_COMBO;
    }

    /** Preset index the fixed combo key casts. Any slot is allowed, empty ones simply do nothing. */
    public int fixedCombo() {
        return fixedCombo;
    }

    public void setFixedCombo(int index) {
        if (index >= 0 && index < combos.length) {
            fixedCombo = index;
        }
    }

    /** Live client movement input, refreshed whenever the keys change. {@code -1 / 0 / 1} each. */
    public int inputForward() {
        return inputForward;
    }

    public int inputStrafe() {
        return inputStrafe;
    }

    public void setInput(int forward, int strafe) {
        this.inputForward = Mth.clamp(forward, -1, 1);
        this.inputStrafe = Mth.clamp(strafe, -1, 1);
    }

    /** Game time until which fall damage is ignored. 0 means "no grace". Not persisted. */
    public long noFallUntil() {
        return noFallUntil;
    }

    public void setNoFallUntil(long tick) {
        noFallUntil = tick;
    }

    public @Nullable MaiCombo.Runtime comboRuntime() {
        return comboRuntime;
    }

    public void setComboRuntime(@Nullable MaiCombo.Runtime comboRuntime) {
        this.comboRuntime = comboRuntime;
    }

    public void clearComboRuntime() {
        this.comboRuntime = null;
    }

    /**
     * Death copy keeps progression only. Backlash, cooldowns, ritual, and combat timers go away.
     * Caller sets {@code current} afterwards.
     */
    public void retainProgressionOnly() {
        backlashStacks = 0;
        backlashStickyUntil = 0L;
        lastCombatTick = 0L;
        regenPauseUntil = 0L;
        openMaiCooldown = 0;
        cooldowns.clear();
        artCooldowns.clear();
        ritual = null;
        comboRuntime = null;
    }

    public void resetProgression() {
        gate = 0;
        unlocked.clear();
        Arrays.fill(equipped, null);
        retainProgressionOnly();
    }

    public void applySync(int gate, float current, float max, int backlashStacks, ResourceLocation[] equipped, Map<ResourceLocation, Integer> cooldowns, Set<ResourceLocation> unlocked) {
        this.gate = Mth.clamp(gate, 0, 12);
        this.syncedMax = max;
        this.current = current;
        this.backlashStacks = backlashStacks;
        for (int i = 0; i < EQUIPPED_SLOTS; i++) {
            this.equipped[i] = i < equipped.length ? equipped[i] : null;
        }
        this.cooldowns.clear();
        this.cooldowns.putAll(cooldowns);
        this.unlocked.clear();
        this.unlocked.addAll(unlocked);
        this.initialized = true;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Gate", gate);
        tag.putFloat("Current", current);
        tag.putBoolean("Initialized", initialized);
        tag.putInt("BacklashStacks", backlashStacks);
        tag.putLong("BacklashStickyUntil", backlashStickyUntil);
        tag.putLong("LastCombatTick", lastCombatTick);
        tag.putLong("RegenPauseUntil", regenPauseUntil);
        tag.putInt("OpenMaiCooldown", openMaiCooldown);

        ListTag equippedTag = new ListTag();
        for (ResourceLocation id : equipped) {
            equippedTag.add(StringTag.valueOf(id == null ? "" : id.toString()));
        }
        tag.put("Equipped", equippedTag);

        ListTag unlockedTag = new ListTag();
        for (ResourceLocation id : unlocked) {
            unlockedTag.add(StringTag.valueOf(id.toString()));
        }
        tag.put("Unlocked", unlockedTag);

        tag.put("Cooldowns", writeCooldownMap(cooldowns));
        tag.put("ArtCooldowns", writeCooldownMap(artCooldowns));

        if (ritual != null) {
            CompoundTag ritualTag = new CompoundTag();
            ritualTag.putInt("TargetGate", ritual.targetGate());
            ritualTag.putLong("StartTick", ritual.startTick());
            ritualTag.putDouble("X", ritual.x());
            ritualTag.putDouble("Y", ritual.y());
            ritualTag.putDouble("Z", ritual.z());
            if (ritual.altarPos() != null) {
                ritualTag.putInt("AltarX", ritual.altarPos().getX());
                ritualTag.putInt("AltarY", ritual.altarPos().getY());
                ritualTag.putInt("AltarZ", ritual.altarPos().getZ());
                ritualTag.putBoolean("HasAltar", true);
            }
            tag.put("Ritual", ritualTag);
        }

        if (!wenyaoSlot.isEmpty()) {
            CompoundTag saved = new CompoundTag();
            wenyaoSlot.save(saved);
            tag.put("WenyaoSlot", saved);
        }

        ListTag comboTag = new ListTag();
        for (MaiCombo.Preset preset : combos) {
            CompoundTag presetTag = new CompoundTag();
            presetTag.putString("Name", preset.name());
            presetTag.putInt("Gap", preset.gapTicks());
            presetTag.putBoolean("OnWheel", preset.onWheel());
            ListTag steps = new ListTag();
            for (MaiCombo.Step step : preset.steps()) {
                CompoundTag stepTag = new CompoundTag();
                stepTag.putString("Skill", step.skillId().toString());
                stepTag.putInt("Charge", step.charge());
                steps.add(stepTag);
            }
            presetTag.put("Steps", steps);
            comboTag.add(presetTag);
        }
        tag.put("Combos", comboTag);
        tag.putInt("Selected", selectedCombo);
        tag.putInt("FixedCombo", fixedCombo);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        gate = Mth.clamp(tag.getInt("Gate"), 0, 12);
        current = tag.getFloat("Current");
        initialized = tag.getBoolean("Initialized");
        backlashStacks = tag.getInt("BacklashStacks");
        backlashStickyUntil = tag.getLong("BacklashStickyUntil");
        lastCombatTick = tag.getLong("LastCombatTick");
        regenPauseUntil = tag.getLong("RegenPauseUntil");
        openMaiCooldown = tag.getInt("OpenMaiCooldown");

        Arrays.fill(equipped, null);
        ListTag equippedTag = tag.getList("Equipped", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(EQUIPPED_SLOTS, equippedTag.size()); i++) {
            String raw = equippedTag.getString(i);
            if (!raw.isEmpty()) {
                equipped[i] = com.kuiba.Kuiba.parse(raw);
            }
        }

        unlocked.clear();
        ListTag unlockedTag = tag.getList("Unlocked", Tag.TAG_STRING);
        for (int i = 0; i < unlockedTag.size(); i++) {
            ResourceLocation id = com.kuiba.Kuiba.parse(unlockedTag.getString(i));
            if (id != null) {
                unlocked.add(id);
            }
        }

        cooldowns.clear();
        readCooldownMap(tag.getCompound("Cooldowns"), cooldowns);
        artCooldowns.clear();
        readCooldownMap(tag.getCompound("ArtCooldowns"), artCooldowns);

        ritual = null;
        if (tag.contains("Ritual", Tag.TAG_COMPOUND)) {
            CompoundTag ritualTag = tag.getCompound("Ritual");
            BlockPos altar = null;
            if (ritualTag.getBoolean("HasAltar")) {
                altar = new BlockPos(ritualTag.getInt("AltarX"), ritualTag.getInt("AltarY"), ritualTag.getInt("AltarZ"));
            }
            ritual = new RitualState(
                    ritualTag.getInt("TargetGate"),
                    ritualTag.getLong("StartTick"),
                    altar,
                    ritualTag.getDouble("X"),
                    ritualTag.getDouble("Y"),
                    ritualTag.getDouble("Z"));
        }

        wenyaoSlot = ItemStack.EMPTY;
        if (tag.contains("WenyaoSlot")) {
            wenyaoSlot = ItemStack.of(tag.getCompound("WenyaoSlot"));
        }

        for (int i = 0; i < combos.length; i++) {
            combos[i] = MaiCombo.Preset.EMPTY;
        }
        comboRuntime = null;
        if (tag.contains("Combos", Tag.TAG_LIST)) {
            ListTag comboTag = tag.getList("Combos", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(combos.length, comboTag.size()); i++) {
                combos[i] = readCombo(comboTag.getCompound(i));
            }
        }
        int savedFixed = tag.contains("FixedCombo") ? tag.getInt("FixedCombo") : 1;
        fixedCombo = savedFixed >= 0 && savedFixed < combos.length ? savedFixed : 1;
        int savedSelected = tag.contains("Selected") ? tag.getInt("Selected") : MaiCombo.NO_COMBO;
        selectedCombo = savedSelected >= 0 && savedSelected < combos.length && !combos[savedSelected].empty()
                ? savedSelected
                : MaiCombo.NO_COMBO;
    }

    private static MaiCombo.Preset[] emptyCombos() {
        MaiCombo.Preset[] presets = new MaiCombo.Preset[MaiCombo.COMBO_COUNT];
        java.util.Arrays.fill(presets, MaiCombo.Preset.EMPTY);
        return presets;
    }

    private static MaiCombo.Preset readCombo(CompoundTag tag) {
        String name = tag.contains("Name") ? tag.getString("Name") : "";
        boolean onWheel = tag.getBoolean("OnWheel");
        int gap = tag.contains("Gap") ? tag.getInt("Gap") : MaiCombo.DEFAULT_GAP_TICKS;
        List<MaiCombo.Step> steps = new ArrayList<>();
        ListTag list = tag.getList("Steps", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && steps.size() < MaiCombo.MAX_STEPS; i++) {
            CompoundTag stepTag = list.getCompound(i);
            ResourceLocation skill = com.kuiba.Kuiba.parse(stepTag.getString("Skill"));
            if (skill == null) {
                continue;
            }
            int charge = MaiCombo.clampCharge(stepTag.getInt("Charge"));
            steps.add(new MaiCombo.Step(skill, charge));
        }
        return new MaiCombo.Preset(MaiCombo.clampName(name), MaiCombo.clampGap(gap), onWheel, List.copyOf(steps));
    }

    public record RitualState(int targetGate, long startTick, @Nullable BlockPos altarPos, double x, double y, double z) {}
}
