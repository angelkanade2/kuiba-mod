package com.kuiba.core;

/**
 * Calls a client-only payload handler without a constant-pool reference to {@code net.minecraft.client}.
 * Dedicated servers register the packets but never invoke these methods.
 */
public final class ClientDispatch {
    private ClientDispatch() {}

    public static void run(String method, Object payload) {
        try {
            Class<?> type = Class.forName("com.kuiba.mai.client.ClientPayloads");
            type.getDeclaredMethod(method, payload.getClass()).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Client payload " + method + " failed", exception);
        }
    }
}
