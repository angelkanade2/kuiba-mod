package com.kuiba.mai;

import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.kuiba.Kuiba;
import com.kuiba.mai.payload.ComboStatePayload;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Named combo presets, the 技能圆盘, and the short sequence runner.
 * <p>
 * The editor keeps {@link #COMBO_COUNT} presets. A preset shows up on the wheel when it has steps and
 * {@code onWheel} is set; the wheel lists them in preset order. Holding the wheel key and releasing over
 * one of them makes it the selected combo, and 施放脉技 casts whatever is selected.
 * <p>
 * Charge (倍率) is 1 / 2 / 3 / 5 / 10 and is no longer gated by 门阶; the only limit on a saved combo
 * is that its total cost fits the player's mai pool.
 */
public final class MaiCombo {
    /** Named presets the editor keeps. */
    public static final int COMBO_COUNT = 8;
    /** Presets that fit on the wheel. Equal to {@link #COMBO_COUNT}, so any preset can be placed. */
    public static final int WHEEL_SIZE = 8;
    public static final int MAX_STEPS = 5;
    /** Code points, not UTF-16 units, so a Chinese name is not cut in half. */
    public static final int NAME_MAX = 12;
    /** No combo selected. */
    public static final int NO_COMBO = -1;

    public static final int DEFAULT_GAP_TICKS = 10;
    /**
     * 倍率 steps the editor offers. The value doubles as the cost multiple, so ×10 really costs ten
     * times the base cost. Effect is deliberately sub-linear so a high charge is not a free win.
     */
    public static final int[] CHARGE_LEVELS = { 1, 2, 3, 5, 10 };
    private static final float[] EFFECT_MULTIPLIER = { 1.0F, 1.75F, 2.75F, 4.0F, 5.5F };

    private MaiCombo() {}

    public static int clampGap(int gapTicks) {
        return Mth.clamp(gapTicks, 0, 200);
    }

    /** Trim, drop formatting codes, and cut to {@link #NAME_MAX} code points. Blank names are allowed. */
    public static String clampName(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        String text = ChatFormatting.stripFormatting(raw).strip();
        if (text.codePointCount(0, text.length()) <= NAME_MAX) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, NAME_MAX));
    }

    /** Snap an arbitrary value to the nearest allowed 倍率 step. */
    public static int clampCharge(int charge) {
        int best = CHARGE_LEVELS[0];
        int bestDelta = Integer.MAX_VALUE;
        for (int level : CHARGE_LEVELS) {
            int delta = Math.abs(level - charge);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = level;
            }
        }
        return best;
    }

    public static boolean isChargeLevel(int charge) {
        for (int level : CHARGE_LEVELS) {
            if (level == charge) {
                return true;
            }
        }
        return false;
    }

    /** Index of {@code charge} inside {@link #CHARGE_LEVELS}, clamped to the table. Used for colours and density. */
    public static int chargeIndex(int charge) {
        int level = clampCharge(charge);
        for (int i = 0; i < CHARGE_LEVELS.length; i++) {
            if (CHARGE_LEVELS[i] == level) {
                return i;
            }
        }
        return 0;
    }

    /** 倍率 doubles as the cost multiple. */
    public static float costMultiplier(int charge) {
        return clampCharge(charge);
    }

    public static float effectMultiplier(int charge) {
        return EFFECT_MULTIPLIER[chargeIndex(charge)];
    }

    /** Sum of every step's charged cost. Steps whose skill is not loaded count as zero. */
    public static float totalCost(Preset preset) {
        float total = 0.0F;
        for (Step step : preset.steps()) {
            MaiSkill skill = MaiSkills.get(step.skillId());
            if (skill != null) {
                total += skill.cost() * costMultiplier(step.charge());
            }
        }
        return total;
    }

    /** Combo name, or the first step's skill name when the player never typed one. */
    public static Component displayName(Preset preset) {
        if (!preset.name().isEmpty()) {
            return Component.literal(preset.name());
        }
        List<Step> steps = preset.steps();
        if (steps.isEmpty()) {
            return Component.translatable("hud.kuiba.combo.empty");
        }
        MaiSkill first = MaiSkills.get(steps.get(0).skillId());
        return first == null
                ? Component.translatable("hud.kuiba.combo.empty")
                : Component.translatable(first.nameKey());
    }

    /** Preset indices currently placed on the wheel, in preset order. */
    public static List<Integer> wheelEntries(MaiData data) {
        List<Integer> entries = new ArrayList<>();
        for (int i = 0; i < COMBO_COUNT; i++) {
            Preset preset = data.combo(i);
            if (preset.onWheel() && !preset.steps().isEmpty()) {
                entries.add(i);
            }
        }
        return entries;
    }

    public static void save(ServerPlayer player, int index, Preset preset) {
        if (index < 0 || index >= COMBO_COUNT) {
            return;
        }
        MaiData data = MaiApi.data(player);
        Preset clean = sanitize(player, preset);
        // The only gate left on saving: the combo has to fit inside the player's mai pool.
        float max = MaiApi.getMax(player);
        float total = totalCost(clean);
        if (total > max + 0.01F) {
            player.displayClientMessage(
                    Component.translatable("message.kuiba.combo.too_expensive", Math.round(total), Math.round(max)), true);
            return;
        }
        data.setCombo(index, clean);
        if (clean.steps().isEmpty() && data.selectedCombo() == index) {
            data.setSelectedCombo(NO_COMBO);
        }
        MaiApi.sync(player);
        player.displayClientMessage(Component.translatable("message.kuiba.combo.saved"), true);
    }

    /** Store the wheel selection. {@link #NO_COMBO} (or an empty preset) clears it. */
    public static void select(ServerPlayer player, int comboIndex) {
        MaiData data = MaiApi.data(player);
        int index = comboIndex;
        if (index < 0 || index >= COMBO_COUNT || data.combo(index).steps().isEmpty()) {
            index = NO_COMBO;
        }
        if (data.selectedCombo() == index) {
            return;
        }
        data.setSelectedCombo(index);
        MaiApi.sync(player);
        if (index != NO_COMBO) {
            player.displayClientMessage(
                    Component.translatable("message.kuiba.combo.selected", displayName(data.combo(index))), true);
        }
    }

    /**
     * Run a combo. {@code slot} is a preset index, or a negative value to run the wheel selection.
     * 脉闪 direction is not captured here: the server keeps the live backward-key state pushed by
     * {@code MaiInputPayload}, so every step decides its own direction when it fires.
     */
    public static void start(ServerPlayer player, int slot) {
        MaiData data = MaiApi.data(player);
        clearStaleRuntime(player, data);
        int index = slot < 0 ? data.selectedCombo() : slot;
        if (index < 0 || index >= COMBO_COUNT) {
            player.displayClientMessage(Component.translatable("message.kuiba.combo.none"), true);
            return;
        }
        if (data.comboRuntime() != null) {
            Kuiba.LOGGER.debug("Combo cast ignored: preset {} is still running", data.comboRuntime().slot());
            return;
        }
        Preset preset = data.combo(index);
        if (preset.steps().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.kuiba.combo.empty"), true);
            return;
        }
        Kuiba.LOGGER.debug("Combo {} starting with {} step(s), gap {} tick(s)", index, preset.steps().size(), preset.gapTicks());
        data.setComboRuntime(new Runtime(index, 0, player.level().getGameTime()));
        notify(player, index, 0, preset.steps().size(), ComboStatePayload.STATE_RUNNING);
        tick(player);
    }

    /** Ticks a step is allowed to sit overdue before the run is written off as abandoned. */
    private static final long STALE_TICKS = 400L;

    /**
     * A runtime left behind by a disconnect or a reload would block every later cast, because
     * {@link #start} refuses to begin while one is live. Drop it once it is clearly abandoned.
     */
    private static void clearStaleRuntime(ServerPlayer player, MaiData data) {
        Runtime runtime = data.comboRuntime();
        if (runtime == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - runtime.readyTick() > STALE_TICKS) {
            Kuiba.LOGGER.debug("Dropping stale combo runtime at step {}", runtime.nextStep());
            data.clearComboRuntime();
        }
    }

    public static void tick(ServerPlayer player) {
        int guard = 0;
        while (guard++ < MAX_STEPS) {
            MaiData data = MaiApi.data(player);
            clearStaleRuntime(player, data);
            Runtime runtime = data.comboRuntime();
            if (runtime == null) {
                return;
            }
            long now = player.level().getGameTime();
            if (now < runtime.readyTick) {
                return;
            }
            Preset preset = data.combo(runtime.slot);
            int total = preset.steps().size();
            if (runtime.nextStep >= total) {
                data.clearComboRuntime();
                notify(player, runtime.slot, total, total, ComboStatePayload.STATE_DONE);
                return;
            }
            Step step = preset.steps().get(runtime.nextStep);
            boolean ok = MaiCast.execute(player, step.skillId(), step.charge(), -1);
            if (!ok) {
                // Earlier steps stay spent. Nothing is refunded.
                Kuiba.LOGGER.debug("Combo {} stopped at step {} ({})", runtime.slot, runtime.nextStep, step.skillId());
                MaiApi.data(player).clearComboRuntime();
                notify(player, runtime.slot, runtime.nextStep, total, ComboStatePayload.STATE_FAILED);
                return;
            }
            int next = runtime.nextStep + 1;
            if (next >= total) {
                MaiApi.data(player).clearComboRuntime();
                notify(player, runtime.slot, total, total, ComboStatePayload.STATE_DONE);
                return;
            }
            long ready = now + preset.gapTicks();
            MaiApi.data(player).setComboRuntime(new Runtime(runtime.slot, next, ready));
            notify(player, runtime.slot, next, total, ComboStatePayload.STATE_RUNNING);
            if (preset.gapTicks() > 0) {
                return;
            }
        }
    }

    private static void notify(ServerPlayer player, int slot, int done, int total, int state) {
        com.kuiba.core.KuibaNetwork.sendToPlayer(player, new ComboStatePayload(slot, done, total, state));
    }

    private static Preset sanitize(ServerPlayer player, @Nullable Preset preset) {
        if (preset == null) {
            return Preset.EMPTY;
        }
        List<Step> steps = new ArrayList<>();
        MaiData data = MaiApi.data(player);
        for (Step step : preset.steps()) {
            if (steps.size() >= MAX_STEPS || step.skillId() == null) {
                continue;
            }
            int charge = clampCharge(step.charge());
            MaiSkill skill = MaiSkills.get(step.skillId());
            if (skill == null || !data.isUnlocked(skill.id())) {
                continue;
            }
            steps.add(new Step(skill.id(), charge));
        }
        return new Preset(clampName(preset.name()), clampGap(preset.gapTicks()), preset.onWheel(), List.copyOf(steps));
    }

    public record Step(ResourceLocation skillId, int charge) {}

    /** One named preset. A preset without steps is empty, and an empty preset never sits on the wheel. */
    public record Preset(String name, int gapTicks, boolean onWheel, List<Step> steps) {
        public static final Preset EMPTY = new Preset("", DEFAULT_GAP_TICKS, false, List.of());

        public Preset {
            name = clampName(name);
            gapTicks = clampGap(gapTicks);
            steps = List.copyOf(steps.size() > MAX_STEPS ? steps.subList(0, MAX_STEPS) : steps);
            onWheel = onWheel && !steps.isEmpty();
        }

        public boolean empty() {
            return steps.isEmpty();
        }

        public Preset withName(String newName) {
            return new Preset(newName, gapTicks, onWheel, steps);
        }

        public Preset withOnWheel(boolean placed) {
            return new Preset(name, gapTicks, placed, steps);
        }
    }

    public static final class Runtime {
        private final int slot;
        private final int nextStep;
        private final long readyTick;

        public Runtime(int slot, int nextStep, long readyTick) {
            this.slot = slot;
            this.nextStep = nextStep;
            this.readyTick = readyTick;
        }

        public int slot() {
            return slot;
        }

        public int nextStep() {
            return nextStep;
        }

        public long readyTick() {
            return readyTick;
        }
    }
}
