# Kuiba Mod — M1 skeleton brief (NeoForge 1.21.1)

Namespace: `kuiba`
Java: 21
Loader: NeoForge 1.21.1
Package root: `com.kuiba`

## Goal
Create a new NeoForge 1.21.1 mod project skeleton that implements M1 “脉门” foundation per agreed v2 review. Prefer official NeoForge MDK layout for 1.21.1. Mod id `kuiba`, display name 魁拔.

## Build order (implement in this order; stub later steps cleanly)
1. Attachment + Sync + config
2. tiers/skills reload + MaiApi
3. mai_crystal + simple ritual (use **冲脉丹** item for P0; same MaiRitual FSM; altar optional later)
4. Cast + HUD GuiLayer + MaiScreen equip
5. Skip backlash/overdraw (P0.5) — leave stubs/comments only

## Attachment `kuiba:mai` (authoritative fields only)
Do NOT persist `max`.
- gate: int 0–12
- current: float (or fixed-point int×100)
- equipped: ResourceLocation[4] nullable
- unlocked: Set<ResourceLocation>
- backlashStacks: int (unused in P0)
- backlashStickyUntil: long
- lastCombatTick: long
- regenPauseUntil: long
- cooldowns: Map<ResourceLocation, Integer>
- ritual?: { targetGate, startTick, altarPos optional }

Death / copyOnDeath: copy only gate + unlocked + equipped; on respawn current = 0.30 × max; clear backlash/cooldowns/ritual.
Config `kuiba-server.toml`: ritualMode=simple, keepGateOnDeath=true, regen rates optional.

Passive bonuses: apply AttributeModifier on gate change (attack damage / movement speed from tiers.modifiers).

## Network (CustomPacketPayload + StreamCodec + PayloadRegistrar)
- SyncMaiPayload S→C: gate, current, max (computed ok), backlashStacks, equipped[4], cooldown summary
- CastSkillPayload C→S: slot index
- EquipSkillPayload C→S: slot + skillId
- StartRitualPayload C→S: start|cancel + optional BlockPos
- RitualProgressPayload S→C: progress 0–1 / cancelled

## Datapack
`data/kuiba/mai_gate/tiers.json` — each tier:
gate, name_key, max_points, slots, modifiers[{attribute,amount,operation}], ritual:{duration_ticks, ingredients:[{item|tag,count}]}

Include full 0–12 rows; P0 gameplay through gate 6. Gates 7–12 data present, UI grey later.

Ritual materials (推演):
→1 mai_crystal×6 + #minecraft:logs×4
→2 mai_crystal×10 + cobblestone×16 + string×4
→3–6 as M1 canvas (use placeholder items kuiba:mai_beast_fluff, kuiba:condensed_mai_shard, kuiba:condensed_mai_core registered as stub items if needed)

`data/kuiba/mai_skill/*.json` — P0 nine skills from skill canvas:
pulse_flash, pulse_strike, pulse_bolt, mai_guard, mai_mend, mai_edge, mai_bind, mai_barrier, mai_burst
Fields: id, category, unlock_gate, cost, cooldown_ticks, cast, power, range?, duration_ticks?, cast_params?, icon

Load via SimpleJsonResourceReloadListener (or equivalent). Unknown cast → warn skip, no crash.

## MaiApi
getGate, getPoints, tryConsume (creative free; fail if insufficient — no overdraw), meetsGate, addBacklash stub.
Events: MaiGateChangedEvent, MaiCastEvent (Cancelable).

## Ritual rules (裁定 A)
Interrupt (move >2 blocks / hurt): no consume materials, no gate++, no backlash.
Success: consume ingredients from JSON, gate+1, modifiers, current=newMax*0.5, SyncMai, gate_reached advancement trigger.

Regen: combat 0.02×max/s; ooc 0.08×max/s; hurt pause 40t; ooc after 100t no damage dealt/taken.

## Client
RegisterGuiLayersEvent MaiHud; MaiScreen on R; keys V/B/N for slots 1–3; RegisterKeyMappingsEvent.
Placeholder textures OK (generated solid/simple PNGs).

## Packages
com.kuiba.core — attachments, networking bootstrap, config
com.kuiba.mai — MaiData, MaiApi, MaiTiers, MaiSkills, MaiRitual, events, payloads, client

## Done when
- Gradle project builds (`./gradlew build` or assemble) on NeoForge 1.21.1
- Attachment registered; Sync works on login
- tiers.json + skill JSONs load
- mai_crystal + ritual pill item registered; ritual start/cancel/progress wired (can be minimal but compiling)
- MaiApi usable; stub HUD/Screen/KeyMappings compile
- README in Chinese: how to open in IDEA, runClient, sync note
- Produce `artifacts/kuiba-src.zip` of the whole project (excluding .gradle/build) for handoff

Do not invent lore beyond IDs already specified. Prefer compiling stubs over incomplete half-APIs.
