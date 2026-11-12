package com.hukhong.core;

import com.hukhong.HuKhong;
import com.hukhong.age.AgeData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<CultivationData>> CULTIVATION = ATTACHMENTS.register("cultivation",
            () -> AttachmentType.builder(() -> CultivationData.DEFAULT)
                    .serialize(CultivationData.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<AgeData>> AGE = ATTACHMENTS.register("age",
            () -> AttachmentType.builder(() -> AgeData.DEFAULT)
                    .serialize(AgeData.CODEC).copyOnDeath().build());

    /** Trùng Sinh: đã dùng đan, cửa sổ xác nhận, cờ chờ chọn lại đạo (giữ khi chết). */
    public static final Supplier<AttachmentType<com.hukhong.trungsinh.TrungSinhData>> TRUNG_SINH = ATTACHMENTS.register("trung_sinh",
            () -> AttachmentType.builder(() -> com.hukhong.trungsinh.TrungSinhData.DEFAULT)
                    .serialize(com.hukhong.trungsinh.TrungSinhData.CODEC).copyOnDeath().build());

    /** Thể Lực + Lách Né (không giữ khi chết: hồi sinh đầy). */
    public static final Supplier<AttachmentType<com.hukhong.dodge.DodgeData>> DODGE = ATTACHMENTS.register("dodge",
            () -> AttachmentType.builder(() -> com.hukhong.dodge.DodgeData.DEFAULT)
                    .serialize(com.hukhong.dodge.DodgeData.CODEC).build());
}
