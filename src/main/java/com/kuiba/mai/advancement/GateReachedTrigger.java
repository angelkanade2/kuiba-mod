package com.kuiba.mai.advancement;

import java.util.Optional;

import com.kuiba.Kuiba;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class GateReachedTrigger extends SimpleCriterionTrigger<GateReachedTrigger.TriggerInstance> {
    public static final DeferredRegister<net.minecraft.advancements.CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Kuiba.MOD_ID);

    public static final DeferredHolder<net.minecraft.advancements.CriterionTrigger<?>, GateReachedTrigger> GATE_REACHED =
            TRIGGERS.register("gate_reached", GateReachedTrigger::new);

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int gate) {
        this.trigger(player, instance -> instance.gate().matches(gate));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints gate) implements SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("gate", MinMaxBounds.Ints.ANY).forGetter(TriggerInstance::gate))
                .apply(instance, TriggerInstance::new));
    }
}
