package com.hukhong.currency;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Vi Tu Tinh: so du luu theo Hạ (long) trong attachment nguoi choi. Giu khi chet, dong bo client (HUD / shop). */
public final class WalletAttachment {
    private WalletAttachment() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<Long>> TU_TINH = ATTACHMENTS.register("tu_tinh_wallet",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath()
                    .sync(ByteBufCodecs.VAR_LONG).build());

    /** Vi Hon Tinh: so du (long), tach rieng khoi Tu Tinh nen save cu mac dinh = 0. */
    public static final Supplier<AttachmentType<Long>> HON_TINH = ATTACHMENTS.register("hon_tinh_wallet",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath()
                    .sync(ByteBufCodecs.VAR_LONG).build());

    public static void register(IEventBus bus) { ATTACHMENTS.register(bus); }
}
