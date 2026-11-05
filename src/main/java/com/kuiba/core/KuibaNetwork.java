package com.kuiba.core;

import com.kuiba.mai.payload.CastComboPayload;
import com.kuiba.mai.payload.ComboStatePayload;
import com.kuiba.mai.payload.CastWeaponArtPayload;
import com.kuiba.mai.payload.CastSkillPayload;
import com.kuiba.mai.payload.EquipSkillPayload;
import com.kuiba.mai.payload.MaiAuraPayload;
import com.kuiba.mai.payload.MaiFxPayload;
import com.kuiba.mai.payload.MaiInputPayload;
import com.kuiba.mai.payload.OpenMaiPayload;
import com.kuiba.mai.payload.RitualProgressPayload;
import com.kuiba.mai.payload.SaveComboPayload;
import com.kuiba.mai.payload.SelectComboPayload;
import com.kuiba.mai.payload.StartRitualPayload;
import com.kuiba.mai.payload.SyncMaiPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class KuibaNetwork {
    private KuibaNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncMaiPayload.TYPE, SyncMaiPayload.STREAM_CODEC, SyncMaiPayload::handle);
        registrar.playToClient(RitualProgressPayload.TYPE, RitualProgressPayload.STREAM_CODEC, RitualProgressPayload::handle);
        registrar.playToClient(MaiAuraPayload.TYPE, MaiAuraPayload.STREAM_CODEC, MaiAuraPayload::handle);
        registrar.playToClient(MaiFxPayload.TYPE, MaiFxPayload.STREAM_CODEC, MaiFxPayload::handle);
        registrar.playToClient(ComboStatePayload.TYPE, ComboStatePayload.STREAM_CODEC, ComboStatePayload::handle);
        registrar.playToServer(CastSkillPayload.TYPE, CastSkillPayload.STREAM_CODEC, CastSkillPayload::handle);
        registrar.playToServer(EquipSkillPayload.TYPE, EquipSkillPayload.STREAM_CODEC, EquipSkillPayload::handle);
        registrar.playToServer(StartRitualPayload.TYPE, StartRitualPayload.STREAM_CODEC, StartRitualPayload::handle);
        registrar.playToServer(SaveComboPayload.TYPE, SaveComboPayload.STREAM_CODEC, SaveComboPayload::handle);
        registrar.playToServer(SelectComboPayload.TYPE, SelectComboPayload.STREAM_CODEC, SelectComboPayload::handle);
        registrar.playToServer(CastComboPayload.TYPE, CastComboPayload.STREAM_CODEC, CastComboPayload::handle);
        registrar.playToServer(CastWeaponArtPayload.TYPE, CastWeaponArtPayload.STREAM_CODEC, CastWeaponArtPayload::handle);
        registrar.playToServer(OpenMaiPayload.TYPE, OpenMaiPayload.STREAM_CODEC, OpenMaiPayload::handle);
        registrar.playToServer(MaiInputPayload.TYPE, MaiInputPayload.STREAM_CODEC, MaiInputPayload::handle);
    }
}
