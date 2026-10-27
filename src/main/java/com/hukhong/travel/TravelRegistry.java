package com.hukhong.travel;

import com.hukhong.HuKhong;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Dang ky dich chuyen: attachment diem da kham pha (dong bo client) + payload. Goi 1 lan tu HuKhong. */
public final class TravelRegistry {
    private TravelRegistry() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);
    public static final Supplier<AttachmentType<TravelData>> TRAVEL = ATTACHMENTS.register("travel",
            () -> AttachmentType.builder(() -> TravelData.EMPTY).serialize(TravelData.CODEC).copyOnDeath()
                    .sync(TravelData.STREAM_CODEC).build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        bus.addListener(TravelNet::register);
    }
}
