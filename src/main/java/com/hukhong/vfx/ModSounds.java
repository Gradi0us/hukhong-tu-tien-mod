package com.hukhong.vfx;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound event hukhong:* (alias sang sound vanilla trong sounds.json, phối pitch). */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, HuKhong.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> SKILL_THANH_QUANG = reg("skill.thanh_quang");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKILL_LINH_KHI = reg("skill.linh_khi");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKILL_BA_KHI = reg("skill.ba_khi");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKILL_HU_KHONG = reg("skill.hu_khong");
    public static final DeferredHolder<SoundEvent, SoundEvent> BREAKTHROUGH = reg("breakthrough");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDITATE = reg("meditate");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORE_RING = reg("core_ring");

    public static void play(ServerLevel lv, Entity at, DeferredHolder<SoundEvent, SoundEvent> s, float vol, float pitch) {
        lv.playSound(null, at.blockPosition(), s.get(), SoundSource.PLAYERS, vol, pitch);
    }
}
