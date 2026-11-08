package com.kuiba.item;

import com.kuiba.core.GateHostData;
import com.kuiba.core.MaiGateHosts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 特殊怪的刷怪蛋. Spawns the vanilla mob through the normal {@link EntityType#spawn} path (so AI, naming
 * and persistence all behave like a real spawn), then overwrites its 脉门 state, giving exactly the same
 * drops and damage ramp as a naturally spawned 特殊怪.
 */
public class GateHostSpawnEgg extends Item {
    private final EntityType<? extends Mob> type;
    private final GateHostData.Kind kind;
    private final int minGates;
    private final int maxGates;

    public GateHostSpawnEgg(EntityType<? extends Mob> type, GateHostData.Kind kind, int minGates, int maxGates,
            Item.Properties properties) {
        super(properties);
        this.type = type;
        this.kind = kind;
        this.minGates = minGates;
        this.maxGates = maxGates;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        net.minecraft.world.entity.Entity spawned = type.spawn(server, context.getItemInHand(), context.getPlayer(), pos, MobSpawnType.SPAWN_EGG, false, false);
        if (!(spawned instanceof Mob mob)) {
            return InteractionResult.FAIL;
        }
        MaiGateHosts.forceHost(mob, kind, minGates, maxGates);
        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
