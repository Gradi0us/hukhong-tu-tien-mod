package com.hukhong.onboarding;

import com.hukhong.HuKhong;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class OnboardingAttachments {
    private OnboardingAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<OnboardingData>> ONBOARDING = ATTACHMENTS.register("onboarding",
            () -> AttachmentType.builder(() -> OnboardingData.DEFAULT)
                    .serialize(OnboardingData.CODEC).copyOnDeath().build());
}
