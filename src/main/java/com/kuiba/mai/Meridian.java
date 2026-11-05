package com.kuiba.mai;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

/**
 * The twelve 脉门 on the body, declared in the order they open:
 * 仑泉 → 玄骨 → 朝迎风 → 昆海 → 幻血 → 暮还松 → 临缺 → 承暗 → 身破虚 → 比圆 → 启明 → 心断空.
 * <p>
 * {@code tiers.json} spells the same pairing out through {@code "meridian"} on each tier; this
 * declaration is the fallback used by {@link #defaultFor} when a tier omits it. Body placement
 * for the glow rings lives on the client ({@code MaiGateLayer}).
 */
public enum Meridian {
    /** 仑泉 — back of the left hand. */
    LUN_QUAN("lun_quan"),
    /** 玄骨 — back of the right hand. */
    XUAN_GU("xuan_gu"),
    /** 朝迎风 — chest. */
    CHAO_YING_FENG("chao_ying_feng"),
    /** 昆海 — upper back. */
    KUN_HAI("kun_hai"),
    /** 幻血 — lower back. */
    HUAN_XUE("huan_xue"),
    /** 暮还松 — right shoulder. */
    MU_HUAN_SONG("mu_huan_song"),
    /** 临缺 — left shoulder. */
    LIN_QUE("lin_que"),
    /** 承暗 — left knee. */
    CHENG_AN("cheng_an"),
    /** 身破虚 — right knee. */
    SHEN_PO_XU("shen_po_xu"),
    /** 比圆 — crown, a halo above the head. */
    BI_YUAN("bi_yuan"),
    /** 启明 — back of the head, right. */
    QI_MING("qi_ming"),
    /** 心断空 — back of the head, left. */
    XIN_DUAN_KONG("xin_duan_kong");

    private final String id;

    Meridian(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String nameKey() {
        return "meridian.kuiba." + id;
    }

    public static @Nullable Meridian byId(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        for (Meridian meridian : values()) {
            if (meridian.id.equals(key)) {
                return meridian;
            }
        }
        return null;
    }

    /** Default mapping: gate 1 → first constant, gate 12 → last. Null for gate 0 or out of range. */
    public static @Nullable Meridian defaultFor(int gate) {
        if (gate < 1 || gate > values().length) {
            return null;
        }
        return values()[gate - 1];
    }
}
