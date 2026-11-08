package com.kuiba.core;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Stable ids for attribute modifiers. 1.20.1 modifiers are keyed by UUID, not a resource location. */
public final class KuibaIds {
    public static final UUID GATE_HOST_HEALTH = uuid("kuiba:gate_host_health");
    public static final UUID WENYAO_MELEE = uuid("kuiba:wenyao_melee");
    public static final UUID SPEAR_REACH = uuid("kuiba:spear_reach");

    private KuibaIds() {}

    public static UUID uuid(String id) {
        return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
    }
}
