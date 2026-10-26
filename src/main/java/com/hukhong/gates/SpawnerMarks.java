package com.hukhong.gates;

import com.mojang.serialization.Codec;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;

/** Dấu mob sinh từ spawner (lưu qua unload chunk). Đăng ký ở skill.SkillAttachments. */
public final class SpawnerMarks {
    private SpawnerMarks() {}

    public static final AttachmentType<Boolean> MARK =
            AttachmentType.builder(() -> false).serialize(Codec.BOOL).build();

    public static boolean is(LivingEntity e) { return e.hasData(MARK) && e.getData(MARK); }
}
