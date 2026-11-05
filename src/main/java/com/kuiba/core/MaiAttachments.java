package com.kuiba.core;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiData;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Authoritative player attachment {@code kuiba:mai}.
 * {@code max} is never stored; it is derived from the loaded tier table.
 * <p>
 * {@link AttachmentType.Builder#copyOnDeath()} opts the attachment into death copy.
 * The copy handler cannot see whether the copy is a death or an End return, so
 * {@code MaiGameEvents} trims a death copy down to gate + unlocked + equipped
 * (and {@code wenyaoSlot} when {@code keepWenyaoOnDeath}) and then sets current to 30% of max.
 * {@code comboRuntime} is not serialized, so it is not copied on death.
 */
public final class MaiAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Kuiba.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<MaiData>> MAI = ATTACHMENT_TYPES.register(
            "mai",
            () -> AttachmentType.serializable(MaiData::new).copyOnDeath().build());

    private MaiAttachments() {}
}
