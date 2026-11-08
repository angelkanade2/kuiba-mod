package com.kuiba.mai.advancement;

import java.lang.reflect.Method;

import com.google.gson.JsonObject;
import com.kuiba.Kuiba;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class GateReachedTrigger extends SimpleCriterionTrigger<GateReachedTrigger.TriggerInstance> {
    public static final ResourceLocation ID = new ResourceLocation(Kuiba.MOD_ID, "gate_reached");
    public static final GateReachedTrigger GATE_REACHED = new GateReachedTrigger();

    public static void register() {
        try {
            Method method = CriteriaTriggers.class.getDeclaredMethod("register", net.minecraft.advancements.CriterionTrigger.class);
            method.setAccessible(true);
            method.invoke(null, GATE_REACHED);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register kuiba:gate_reached", exception);
        }
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, int gate) {
        this.trigger(player, instance -> instance.matches(gate));
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        MinMaxBounds.Ints gate = MinMaxBounds.Ints.fromJson(json.get("gate"));
        return new TriggerInstance(player, gate);
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final MinMaxBounds.Ints gate;

        public TriggerInstance(ContextAwarePredicate player, MinMaxBounds.Ints gate) {
            super(ID, player);
            this.gate = gate;
        }

        public boolean matches(int gate) {
            return this.gate.matches(gate);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.add("gate", this.gate.serializeToJson());
            return json;
        }
    }
}
