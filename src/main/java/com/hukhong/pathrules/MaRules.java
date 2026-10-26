package com.hukhong.pathrules;

import com.hukhong.HuKhong;
import com.hukhong.charcreate.CharCreateService;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.cores.CoreAttachments;
import com.hukhong.cores.CoreSlots;
import com.hukhong.gates.Resources;
import com.hukhong.gates.SpawnerMarks;
import com.hukhong.network.MaSyncPayload;
import com.hukhong.skill.SkillAttachments;
import com.hukhong.skill.SkillService;
import com.hukhong.skill.SkillState;
import com.hukhong.stats.ModAttributes;
import com.hukhong.stats.StatService;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Luật Ma Tu (S2 2.4, canon R17/R23): Hấp Huyết, Huyết Tế (chỉ NPC, có quota), Ác Tính + 3 tầng cảnh báo,
 * Mất Kiểm Soát, Tẩu Hỏa Nhập Ma = phế tu (reset + mở lại màn tạo nhân vật). Ma Tu không có kỹ năng hỗ trợ đồng đội.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class MaRules {
    private MaRules() {}

    public static final String BRED_TAG = "hukhong_bred";
    public static final double STACK_MAX = 100;
    /** Quota Huyết Tế / ngày game (R17). Số cuối cùng do S5 đặt; đây là giá trị P1. */
    public static final double DAILY_PLAYER = 60, DAILY_GLOBAL = 600;
    public static final double BRED_MULT = 0.1;
    public static final int COUNTDOWN_TICKS = 160;   // 8s
    public static final int LOST_TICKS = 80;         // 4s
    public static final int THAN_TICKS = 240;        // 12s
    public static final double ATK_CAP_SOLO = 0.90, ATK_CAP_MULTI = 0.70, HH_BONUS_CAP = 0.13;

    private static long globalDay = -1;
    private static double globalGain = 0;

    // ------------------------------------------------------------------ thuần (test được)

    /** 0 = bình thường; 1 >= 80; 2 >= 90; 3 >= 95. */
    public static int tierOf(double acTinh) {
        return acTinh >= 95 ? 3 : acTinh >= 90 ? 2 : acTinh >= 80 ? 1 : 0;
    }

    public static double stateAtk(double a) { return a >= 80 ? 0.20 : a >= 60 ? 0.15 : a >= 30 ? 0.08 : 0; }

    public static double stateHh(double a) { return a >= 80 ? 0.03 : a >= 60 ? 0.02 : a >= 30 ? 0.01 : 0; }

    /** Cộng thêm vào Công (phân số của base): Huyết Tế + trạng thái Ác Tính + Mất Kiểm Soát + Ma Thần; có trần T6. */
    public static double atkBonus(MaData m, boolean multiPath) {
        double b = (m.absorb ? m.huyetTe * 0.005 : 0) + stateAtk(m.acTinh)
                + (m.lostTicks > 0 ? 0.25 : 0) + (m.thanTicks > 0 ? 0.40 : 0);
        return Math.min(multiPath ? ATK_CAP_MULTI : ATK_CAP_SOLO, b);
    }

    public static double lifestealBonus(MaData m) {
        double b = (m.absorb ? m.huyetTe * 0.001 : 0) + stateHh(m.acTinh) + (m.thanTicks > 0 ? 0.04 : 0);
        return Math.min(HH_BONUS_CAP, b);
    }

    // ------------------------------------------------------------------ truy cập

    public static MaData data(ServerPlayer p) { return p.getData(SkillAttachments.MA); }

    private static boolean isMa(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).paths().contains(Path.MA); }

    public static boolean isLost(ServerPlayer p) { return data(p).lostTicks > 0 && isMa(p); }

    /** Mất Kiểm Soát tắt Hấp Huyết. */
    public static boolean noLifesteal(ServerPlayer p) { return data(p).lostTicks > 0; }

    public static void addAcTinh(ServerPlayer p, double delta) {
        MaData m = data(p);
        m.acTinh = Math.max(0, Math.min(100, m.acTinh + com.hukhong.honcot.HonCotMath.acTinhDelta(delta,
                com.hukhong.honcot.HonCotService.has(p, com.hukhong.honcot.Bone.TINH_TAM))));
    }

    public static void activateThan(ServerPlayer p) {
        data(p).thanTicks = THAN_TICKS;
        addAcTinh(p, 15);
    }

    /** Đổi lộ (UI Ma Lộ + lệnh). absorb=false (Tu Luyện thường) xóa Huyết Tế. Chỉ Ma tu; trả true nếu đổi được. */
    public static boolean setLo(ServerPlayer p, boolean absorb) {
        if (!isMa(p)) return false;
        MaData m = data(p);
        m.absorb = absorb;
        if (!absorb) m.huyetTe = 0;
        return true;
    }

    public static void setAcTinh(ServerPlayer p, double v) { data(p).acTinh = Math.max(0, Math.min(100, v)); }

    /** EXP Hấp Thu x1.45 cho NPC hợp lệ (không boss/spawner/người chơi, chưa bão hòa). */
    public static float expMultiplier(ServerPlayer p, LivingEntity victim) {
        if (!isMa(p)) return 1f;
        MaData m = data(p);
        return m.absorb && absorbable(victim) && m.acTinh < 90 ? 1.45f : 1f;
    }

    private static boolean isBoss(LivingEntity v) { return v.getType().is(Tags.EntityTypes.BOSSES); }

    private static boolean absorbable(LivingEntity v) {
        return !(v instanceof Player) && !isBoss(v) && !SpawnerMarks.is(v);
    }

    // ------------------------------------------------------------------ sự kiện chung

    /** Gọi từ PathRules khi Ma Tu gây sát thương > 0: +1 Ô Uế Lực mỗi đòn trúng (tối đa +10/giây). */
    public static void onHit(ServerPlayer atk) {
        MaData m = data(atk);
        if (m.hitSecond >= 10) return;
        m.hitSecond++;
        Resources.add(atk, 1);
    }

    /** Gọi từ GateEvents khi người chơi giết 1 sinh vật không phải người chơi. rg = RewardGate đã ghi nhận 1 lần. */
    public static void onKill(ServerPlayer p, LivingEntity v, float rg) {
        if (!isMa(p) || v instanceof Player) return;
        MaData m = data(p);
        boolean villager = v instanceof AbstractVillager;
        if (villager) { // R17: giết dân làng bất kỳ -> Ác Danh (không bị KillRateGate giảm)
            m.acDanh += 1;
            p.displayClientMessage(Component.translatable("hukhong.ma.ac_danh", String.format("%.1f", m.acDanh)), true);
        }
        if (!m.absorb || !absorbable(v) || m.acTinh >= 90) return; // lộ thường / boss / spawner / bão hòa: không hấp thu
        boolean elite = !villager && v.getMaxHealth() >= 40;
        // Hình phạt Ác Tính: không bị rg giảm
        addAcTinh(p, villager ? 5 : elite ? 3 : 1);
        if (m.lostTicks > 0) addAcTinh(p, 8);
        // Lợi ích: nhân rg, dân nhân giống x0,1, quota ngày
        double mult = rg * (v.getPersistentData().getBoolean(BRED_TAG) ? BRED_MULT : 1.0);
        rollDay(p, m);
        double room = Math.max(0, Math.min(DAILY_PLAYER - m.dayGain, DAILY_GLOBAL - globalGain));
        double gain = Math.min(room, (elite ? 3 : 1) * mult);
        if (gain > 0) {
            m.huyetTe = Math.min(STACK_MAX, m.huyetTe + gain);
            m.dayGain += gain;
            globalGain += gain;
        }
        int oul = (int) Math.round((villager ? 15 : 8) * mult);
        if (oul > 0) Resources.add(p, oul);
    }

    private static void rollDay(ServerPlayer p, MaData m) {
        long day = p.getServer().overworld().getGameTime() / 24000L;
        if (m.day != day) { m.day = day; m.dayGain = 0; }
        if (globalDay != day) { globalDay = day; globalGain = 0; }
    }

    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent e) {
        if (e.getChild() != null) e.getChild().getPersistentData().putBoolean(BRED_TAG, true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        // Giết người chơi: Ác Tính +10, Ác Danh +1.5, không hưởng gì (R17 / S2 2.4.1)
        if (e.getEntity() instanceof ServerPlayer victim && e.getSource().getEntity() instanceof ServerPlayer k
                && k != victim && isMa(k)) {
            addAcTinh(k, 10);
            data(k).acDanh += 1.5;
        }
        // Chết thường: Ác Tính -50%
        if (e.getEntity() instanceof ServerPlayer p && isMa(p)) {
            MaData m = data(p);
            m.acTinh *= 0.5;
            m.countdown = -1;
            m.lostTicks = 0;
            m.thanTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) data(p).sentAc = -1;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) data(p).sentAc = -1; // ép sync lại
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { globalDay = -1; globalGain = 0; }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        long now = lv.getGameTime();
        MaData m = data(p);
        if (!isMa(p)) {
            if (now % 20 == 0) {
                clearBonuses(p);
                if (m.acTinh != 0 || m.huyetTe != 0 || m.countdown >= 0) m.reset();
            }
            return;
        }
        SkillState st = p.getData(SkillAttachments.STATE);

        if (m.lostTicks > 0) {
            if (now % 8 == 0) lostSwing(p);
            if (--m.lostTicks == 0) addAcTinh(p, -10);
        }
        if (m.thanTicks > 0) {
            m.thanTicks--;
            if (now % 4 == 0) lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), p.getX(), p.getY() + 1, p.getZ(),
                    3, 0.4, 0.8, 0.4, 0.02);
        }
        if (now % 20 == 0) perSecond(p, m, st, now);

        if (m.acTinh >= 100) { tauHoa(p); return; }
        if (m.acTinh >= 95) {
            if (m.countdown < 0) m.countdown = COUNTDOWN_TICKS;
            else if (--m.countdown <= 0) { tauHoa(p); return; }
        } else {
            m.countdown = -1;
        }
        if (now % 10 == 0) {
            applyBonuses(p, m);
            warn(p, m, now);
            sync(p, m);
        }
    }

    private static void perSecond(ServerPlayer p, MaData m, SkillState st, long now) {
        m.hitSecond = 0;
        long sinceCombat = now - st.lastCombat;
        if (sinceCombat > 60) Resources.add(p, -2); // Ô Uế Lực không tự hồi, tan -2/s ngoài combat
        if (!m.absorb) m.huyetTe = 0;
        if (m.lostTicks <= 0) {
            if (st.meditating() && sinceCombat > 100) addAcTinh(p, -1.5);          // thiền
            else if (sinceCombat > 200 && m.acTinh < 60) addAcTinh(p, -0.2);       // tự giảm
        }
        if (now % 1200 == 0 && sinceCombat > 100 && m.huyetTe > 0) m.huyetTe = Math.max(0, m.huyetTe - 1);
        // Mất Kiểm Soát: mỗi 20s xác suất (Ác Tính - 75)% khi 80..94
        if (now % 400 == 0 && m.lostTicks <= 0 && m.acTinh >= 80 && m.acTinh < 95
                && p.getRandom().nextDouble() < (m.acTinh - 75) / 100.0) {
            m.lostTicks = LOST_TICKS;
            p.displayClientMessage(Component.translatable("hukhong.ma.lost_control"), true);
            p.playNotifySound(SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.5f, 0.6f);
        }
    }

    /** Mất Kiểm Soát: tự đánh mục tiêu sống gần nhất (kể cả đồng đội/dân làng). */
    private static void lostSwing(ServerPlayer p) {
        LivingEntity t = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(3.2),
                        e -> e != p && e.isAlive() && !e.isSpectator()).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
        if (t == null) return;
        p.lookAt(EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        p.swing(InteractionHand.MAIN_HAND, true);
        p.attack(t);
    }

    private static final net.minecraft.resources.ResourceLocation ATK_ID = ModAttributes.id("ma/attack");
    private static final net.minecraft.resources.ResourceLocation HH_ID = ModAttributes.id("ma/lifesteal");

    private static void applyBonuses(ServerPlayer p, MaData m) {
        boolean multi = p.getData(ModAttachments.CULTIVATION).paths().size() > 1;
        set(p, ModAttributes.ATTACK_POWER, ATK_ID, atkBonus(m, multi), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, ModAttributes.LIFESTEAL, HH_ID, lifestealBonus(m), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void clearBonuses(ServerPlayer p) {
        set(p, ModAttributes.ATTACK_POWER, ATK_ID, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, ModAttributes.LIFESTEAL, HH_ID, 0, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void set(ServerPlayer p, String attr, net.minecraft.resources.ResourceLocation id, double v,
                            AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(ModAttributes.holder(attr));
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (v <= 0) { if (cur != null) inst.removeModifier(id); return; }
        if (cur == null || cur.amount() != v) inst.addOrUpdateTransientModifier(new AttributeModifier(id, v, op));
    }

    // ------------------------------------------------------------------ cảnh báo 3 tầng (âm thanh + chữ; hình ở client)

    private static void warn(ServerPlayer p, MaData m, long now) {
        int tier = m.countdown >= 0 ? 3 : tierOf(m.acTinh);
        if (tier > m.lastTier) {
            switch (tier) {
                case 1 -> p.sendSystemMessage(Component.translatable("hukhong.ma.warn1"));
                case 2 -> {
                    p.sendSystemMessage(Component.translatable("hukhong.ma.warn2"));
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.ma.warn2.title")));
                }
                default -> p.sendSystemMessage(Component.translatable("hukhong.ma.warn3", (m.countdown + 19) / 20));
            }
        }
        m.lastTier = tier;
        switch (tier) {
            case 1 -> { if (now % 40 == 0) beat(p, 0.6f, 0.8f); }
            case 2 -> {
                if (now % 20 == 0) beat(p, 0.9f, 1.2f);
                if (now % 60 == 0) p.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8f, 0.5f);
            }
            case 3 -> {
                if (now % 10 == 0) beat(p, 1.0f, 1.5f);
                if (now % 40 == 0) p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.7f, 1.0f);
                if (now % 20 == 0) {
                    p.displayClientMessage(Component.translatable("hukhong.ma.warn3.bar", (m.countdown + 19) / 20), true);
                    p.sendSystemMessage(Component.translatable("hukhong.ma.warn3", (m.countdown + 19) / 20));
                }
            }
            default -> { }
        }
    }

    private static void beat(ServerPlayer p, float vol, float pitch) {
        SoundEvent s = SoundEvents.WARDEN_HEARTBEAT;
        p.playNotifySound(s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void sync(ServerPlayer p, MaData m) {
        boolean lost = m.lostTicks > 0;
        int ab = m.absorb ? 1 : 0;
        if (m.sentAc == (float) m.acTinh && m.sentCd == m.countdown && m.sentLost == lost && m.sentHt == (int) m.huyetTe && m.sentAb == ab) return;
        m.sentAc = (float) m.acTinh; m.sentCd = m.countdown; m.sentLost = lost; m.sentHt = (int) m.huyetTe; m.sentAb = ab;
        PacketDistributor.sendToPlayer(p, new MaSyncPayload((float) m.acTinh, m.countdown, lost, (int) m.huyetTe, m.absorb));
    }

    // ------------------------------------------------------------------ Tẩu Hỏa Nhập Ma (R23)

    /**
     * Phế tu: reset toàn bộ tu vi (cấp 1, Lõi, đạo, tư chất, Ác Tính, Huyết Tế), roll lại và mở màn tạo nhân vật.
     * Giữ túi đồ (không chết thật nên không rơi đồ, không kích hoạt Bất Diệt). Hồn Cốt chưa có hệ thống -> hook ở đây.
     */
    public static void tauHoa(ServerPlayer p) {
        MaData m = data(p);
        m.reset();
        clearBonuses(p);
        if (p.level() instanceof ServerLevel lv) {
            lv.sendParticles(com.hukhong.vfx.ModParticles.MA_KHI.get(), p.getX(), p.getY() + 1, p.getZ(), 80, 0.7, 1.0, 0.7, 0.3);
            lv.playSound(null, p.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.ma.tau_hoa.title")));
        p.sendSystemMessage(Component.translatable("hukhong.ma.tau_hoa"));
        p.setData(ModAttachments.CULTIVATION, CultivationData.DEFAULT);
        p.setData(CoreAttachments.CORE_SLOTS, CoreSlots.EMPTY);
        SkillState st = p.getData(SkillAttachments.STATE);
        st.stance = false;
        Arrays.fill(st.cooldownEnd, 0L);
        p.removeAllEffects();
        CharCreateService.reroll(p);   // xóa đạo/tư chất, roll mới, tuổi khởi đầu, mở màn tạo nhân vật
        StatService.refresh(p);
        SkillService.sync(p);
        PacketDistributor.sendToPlayer(p, new MaSyncPayload(0f, -1, false, 0, true));
    }
}
