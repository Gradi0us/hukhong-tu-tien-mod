package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.CreatureRegistry;
import com.hukhong.creature.LinhThu;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

/**
 * Ấu thể -> trưởng thành. Linh thú mới thuần phục là ấu thể (cỡ nhỏ); điểm trưởng thành 0..100:
 * +1 mỗi phút ở gần chủ, +5 khi cho ăn, +2 khi cùng hạ quái. Thân to dần theo điểm; đủ 100 -> trưởng thành
 * (rộng ~4 khối, cao > 2 khối). Loài có dạng lớn riêng (Thiết Bối Hùng Con -> Thiết Bối Hùng) thì đổi hẳn sang model lớn.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PetGrowth {
    private PetGrowth() {}

    public static final int MAX = 100;
    /** Bề ngang dạng trưởng thành (khối). */
    static final double ADULT_WIDTH = 4.0;
    static final Map<CreatureKind, CreatureKind> ADULT = Map.of(CreatureKind.THIET_BOI_HUNG_CON, CreatureKind.THIET_BOI_HUNG);
    static final ResourceLocation MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "pet_growth");

    /** Đang là ấu thể (đã thuần phục, chưa trưởng thành). */
    public static boolean juvenile(LinhThu t) { return t.isTame() && t.growth() < MAX; }

    public static void add(LinhThu t, int n) {
        if (!juvenile(t) || n <= 0) return;
        t.setGrowth(t.growth() + n);
        applyScale(t);
        if (t.growth() >= MAX) mature(t);
    }

    /** Hệ số cỡ theo điểm trưởng thành: ấu thể = cỡ gốc, trưởng thành = ngang ~4 khối. */
    public static void applyScale(LinhThu t) {
        AttributeInstance a = t.getAttribute(Attributes.SCALE);
        if (a == null) return;
        a.removeModifier(MOD);
        if (!t.isTame()) return;
        double w = Math.max(0.3, t.getType().getDimensions().width());
        double adult = ADULT_WIDTH / w;                       // hệ số khi trưởng thành
        // loài có dạng lớn riêng: ấu thể lớn tới ~1.8 lần rồi đổi model; loài khác lớn đều tới cỡ trưởng thành
        double end = ADULT.containsKey(t.kind()) && t.growth() < MAX ? Math.min(adult, 1.8) : adult;
        double f = 1.0 + (end - 1.0) * Math.min(MAX, t.growth()) / (double) MAX;
        a.addPermanentModifier(new AttributeModifier(MOD, f - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Trưởng thành: đổi sang loài lớn (nếu có) hoặc phóng thân, kèm hiệu ứng. */
    static void mature(LinhThu t) {
        if (!(t.level() instanceof ServerLevel sl) || !(t.getOwner() instanceof ServerPlayer owner)) return;
        LinhThu result = t;
        CreatureKind big = ADULT.get(t.kind());
        if (big != null && CreatureRegistry.type(big).create(sl) instanceof LinhThu a) {
            a.moveTo(t.getX(), t.getY(), t.getZ(), t.getYRot(), 0);
            a.tame(owner);
            if (t.hasCustomName()) a.setCustomName(t.getCustomName());
            a.setBond(t.bond());
            a.setPetForm(t.petStage(), t.petBranch());
            a.setGrowth(MAX);
            a.setOrderedToSit(t.isOrderedToSit());
            sl.addFreshEntity(a);
            PetService.syncLevel(a);
            PetForm.apply(a);
            applyScale(a);
            a.setHealth(a.getMaxHealth());
            PetData d = PetService.data(owner);
            for (String slot : PetService.SLOTS)
                if (d.get(slot).map(u -> u.equals(t.getUUID())).orElse(false)) owner.setData(CreatureRegistry.PETS.get(), d.with(slot, a.getUUID()));
            t.discard();
            result = a;
        } else {
            applyScale(t);
            t.setHealth(t.getMaxHealth());
        }
        fx(sl, owner, result);
    }

    static void fx(ServerLevel sl, ServerPlayer owner, LinhThu t) {
        Vec3 c = t.position();
        int rgb = 0x9FE08A;
        Fx.play(sl, FxType.SHOCKWAVE, c.add(0, 0.1, 0), Vec3.ZERO, 4f, rgb, 20);
        Fx.groundRing(sl, owner, c.add(0, 0.05, 0), 5f, rgb, 50);
        Fx.rune(sl, owner, c.add(0, 0.08, 0), 3.5f, rgb, 60, 6);
        Fx.shake(sl, c, 0.6f, 14, 20f);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 1.5, c.z, 60, 1.8, 1.5, 1.8, 0.1);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 40, 1.2, 2, 1.2, 0.08);
        sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.2f, 0.7f);
        sl.playSound(null, t.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 1.0f, 0.9f);
        try {
            owner.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 16));
            owner.connection.send(new ClientboundSetSubtitleTextPacket(t.getDisplayName().copy().withColor(0xDFFFD0)));
            owner.connection.send(new ClientboundSetTitleTextPacket(Tx.t("hukhong.pet.grow.title", "Trưởng Thành", "Fully Grown").copy().withColor(rgb)));
        } catch (RuntimeException ignored) { } // người chơi giả
    }

    // ------------------------------------------------------------------ nguồn điểm
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 1200 != 600) return;
        for (String s : PetService.SLOTS) {
            LinhThu t = PetService.loaded(p, s);
            if (t != null && t.isAlive() && t.distanceToSqr(p) < 32 * 32) add(t, 1);
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof LinhThu t && !t.level().isClientSide) add(t, 2);
    }
}
