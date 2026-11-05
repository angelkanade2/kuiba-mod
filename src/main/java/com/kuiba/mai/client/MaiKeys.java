package com.kuiba.mai.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class MaiKeys {
    public static final String CATEGORY = "key.categories.kuiba";

    /** Opens the 脉术修炼 screen. */
    public static final KeyMapping OPEN = new KeyMapping("key.kuiba.open_mai", GLFW.GLFW_KEY_R, CATEGORY);
    /** Fills the mai pool and plays the 开脉门 effect. */
    public static final KeyMapping OPEN_GATE = new KeyMapping("key.kuiba.open_gate", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping SLOT_1 = new KeyMapping("key.kuiba.slot1", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping SLOT_2 = new KeyMapping("key.kuiba.slot2", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping SLOT_3 = new KeyMapping("key.kuiba.slot3", GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping COMBO_1 = new KeyMapping("key.kuiba.combo1", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping COMBO_2 = new KeyMapping("key.kuiba.combo2", GLFW.GLFW_KEY_X, CATEGORY);
    /** Registered only. P0 has two combo slots, so C does not cast. */
    public static final KeyMapping COMBO_OPTIONAL = new KeyMapping("key.kuiba.combo_optional", GLFW.GLFW_KEY_C, CATEGORY);
    /** Hold to open the 技能圆盘; release over a combo to select it. */
    public static final KeyMapping COMBO_WHEEL = new KeyMapping("key.kuiba.combo_wheel", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    /** Opens the combo editor. */
    public static final KeyMapping COMBO_EDITOR = new KeyMapping("key.kuiba.combo_editor", GLFW.GLFW_KEY_J, CATEGORY);

    private MaiKeys() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
        event.register(OPEN_GATE);
        event.register(SLOT_1);
        event.register(SLOT_2);
        event.register(SLOT_3);
        event.register(COMBO_1);
        event.register(COMBO_2);
        event.register(COMBO_OPTIONAL);
        event.register(COMBO_WHEEL);
        event.register(COMBO_EDITOR);
    }

    /** Display name of the key bound to combo slot {@code slot}, e.g. "Z". */
    public static String comboKeyName(int slot) {
        KeyMapping key = slot == 0 ? COMBO_1 : COMBO_2;
        return key.getTranslatedKeyMessage().getString();
    }

    /** Key that opens the 技能圆盘. */
    public static String wheelKeyName() {
        return COMBO_WHEEL.getTranslatedKeyMessage().getString();
    }

    /** Key that casts the combo picked on the wheel. */
    public static String castKeyName() {
        return COMBO_1.getTranslatedKeyMessage().getString();
    }
}
