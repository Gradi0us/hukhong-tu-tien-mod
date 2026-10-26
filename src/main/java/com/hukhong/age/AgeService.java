package com.hukhong.age;

import com.hukhong.core.ModAttachments;
import com.hukhong.network.SyncPayload;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Tick tuổi + Thọ Nguyên + Thiên Nhân Ngũ Suy. R1: không chết vì tuổi, dừng ở GĐ V; gỡ bằng đột phá/Thọ Nguyên Đan. */
public final class AgeService {
    private AgeService() {}

    private static final ResourceLocation HP_ID = ResourceLocation.fromNamespaceAndPath("hukhong", "ngu_suy_hp");
    private static final ResourceLocation DMG_ID = ResourceLocation.fromNamespaceAndPath("hukhong", "ngu_suy_dmg");
    private static final ResourceLocation SPD_ID = ResourceLocation.fromNamespaceAndPath("hukhong", "ngu_suy_spd");
    private static final ResourceLocation RES_ID = ResourceLocation.fromNamespaceAndPath("hukhong", "ngu_suy_res");

    /** Giai đoạn Ngũ Suy hiện tại (0 = không). */
    public static int stageOf(ServerPlayer p) {
        AgeData a = p.getData(ModAttachments.AGE);
        int life = a.lifespan(p.getData(ModAttachments.CULTIVATION).realmIndex());
        return AgeMath.stage(a.ageYears(), life);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        AgeData a = p.getData(ModAttachments.AGE);
        boolean afk = AfkTracker.check(p);
        if (!afk || com.hukhong.society.SectEvents.isBeQuan(p)) { // R22: AFK hợp lệ trong Bế Quan vẫn già (S6 §1.2)
            long before = a.onlineTicks(), after = before + 20;
            a = a.withOnlineTicks(after);
            p.setData(ModAttachments.AGE, a);
            if (after / AgeData.TICKS_PER_MONTH > before / AgeData.TICKS_PER_MONTH) SyncPayload.send(p);
        }
        int life = a.lifespan(p.getData(ModAttachments.CULTIVATION).realmIndex());
        int stage = AgeMath.stage(a.ageYears(), life);
        applyDebuffs(p, stage);
        if (stage > 0 && p.tickCount % 40 == 0 && p.level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.3, 0.5, 0.3, 0.01);
        int key = AgeMath.warnKey(a.ageYears(), life);
        if (key != a.warned()) {
            p.setData(ModAttachments.AGE, a.withWarned(key));
            if (key > a.warned()) warn(p, key); // key giảm = đã gia hạn/đột phá, chỉ reset
            SyncPayload.send(p);
        }
    }

    private static void warn(ServerPlayer p, int key) {
        if (key <= 3) {
            p.sendSystemMessage(Component.translatable("hukhong.age.warn." + key));
        } else {
            int s = key - 3;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.age.suy.title." + s)));
            p.sendSystemMessage(Component.translatable("hukhong.age.suy.msg." + s));
        }
    }

    private static void applyDebuffs(ServerPlayer p, int stage) {
        mod(p, Attributes.MAX_HEALTH, HP_ID, AgeMath.HP_MULT[stage]);
        mod(p, Attributes.ATTACK_DAMAGE, DMG_ID, AgeMath.DMG_MULT[stage]);
        mod(p, Attributes.MOVEMENT_SPEED, SPD_ID, stage >= 5 ? -0.15 : 0);
        if (mod(p, com.hukhong.stats.ModAttributes.holder(com.hukhong.stats.ModAttributes.MAX_RESOURCE), RES_ID, AgeMath.maxResourceMult(stage)))
            com.hukhong.stats.StatService.syncResourceMax(p); // GĐ II: -10% tài nguyên tối đa
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** true nếu modifier thực sự đổi. */
    private static boolean mod(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double v) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return false;
        AttributeModifier cur = inst.getModifier(id);
        if (v == 0) { if (cur != null) { inst.removeModifier(id); return true; } return false; }
        if (cur != null && cur.amount() == v) return false;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return true;
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && stageOf(p) > 0)
            e.setAmount((float) (e.getAmount() * AgeMath.regenMult(1)));
    }
}
