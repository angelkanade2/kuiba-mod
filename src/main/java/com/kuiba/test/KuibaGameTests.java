package com.kuiba.test;

import com.kuiba.core.GateHostData;
import com.kuiba.core.MaiAttachments;
import com.kuiba.core.MaiGateHosts;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiData;
import com.kuiba.wenyao.WenyaoTier;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

/** Server-side checks for persistence and 特殊怪 health. Not a substitute for playing the mod. */
public class KuibaGameTests {
    public static void register(RegisterGameTestsEvent event) {
        event.register(KuibaGameTests.class);
    }

    @GameTest(templateNamespace = "kuiba", template = "empty")
    public static void maiDataRoundTrip(GameTestHelper helper) {
        MaiData data = new MaiData();
        data.setGate(4);
        data.markInitialized();
        data.setCurrent(12.5F, 100.0F);
        data.unlock(new ResourceLocation("kuiba", "pulse_bolt"));
        CompoundTag tag = data.serializeNBT();
        MaiData copy = new MaiData();
        copy.deserializeNBT(tag);
        helper.assertTrue(copy.gate() == 4, "gate");
        helper.assertTrue(copy.initialized(), "initialized");
        helper.assertTrue(Math.abs(copy.current() - 12.5F) < 0.01F, "current " + copy.current());
        helper.assertTrue(copy.isUnlocked(new ResourceLocation("kuiba", "pulse_bolt")), "unlock");
        helper.assertTrue(WenyaoTier.byName("wang") == WenyaoTier.WANG, "wenyao wang");
        helper.assertTrue(WenyaoTier.byName("missing") == WenyaoTier.COMMON, "wenyao fallback");
        helper.assertTrue(MaiCombo.clampCharge(9) == 10, "charge snap");
        helper.assertTrue(MaiCombo.clampCharge(1) == 1, "charge keep");
        helper.succeed();
    }

    @GameTest(templateNamespace = "kuiba", template = "empty")
    public static void forcedGateHostGainsHealth(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2, 2, 2);
        float before = zombie.getMaxHealth();
        MaiGateHosts.forceHost(zombie, GateHostData.Kind.SHARD, 3, 3);
        GateHostData data = MaiAttachments.host(zombie);
        helper.assertTrue(data.isHost(), "host");
        helper.assertTrue(data.kind() == GateHostData.Kind.SHARD, "kind");
        helper.assertTrue(data.gates() == 3, "gates " + data.gates());
        helper.assertTrue(zombie.getMaxHealth() > before, "health " + zombie.getMaxHealth() + " from " + before);
        helper.succeed();
    }
}
