package com.hukhong.society;

import com.hukhong.HuKhong;
import com.hukhong.city.CityBuilder;
import com.hukhong.city.Placement;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuMob;
import com.hukhong.gates.RewardGate;
import com.hukhong.vfx.ModParticles;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tông môn: sinh lười sơn môn, Bế Quan tại Linh Khí Đài (R22), thư mời ngẫu nhiên, buff, tiến độ nhiệm vụ diệt. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SectEvents {
    private SectEvents() {}

    public static final int GEN_RADIUS = 110;
    /** Tích EXP lẻ (EXP/giây rất nhỏ ở cấp thấp). */
    private static final Map<UUID, Double> ACCUM = new ConcurrentHashMap<>();
    /** Người đang Bế Quan hợp lệ (đủ điều kiện + ghế + trả phí): tuổi vẫn trôi dù AFK (R22). */
    private static final java.util.Set<UUID> BE_QUAN = ConcurrentHashMap.newKeySet();
    private static final String SIT_KEY = "hk_altar_sit_s"; // giây ngồi cộng dồn (persistent: không reset khi ngồi lại/relog)
    public static final AltarSeats SEATS = new AltarSeats();

    public static BlockPos site(ServerLevel ow, SectDef d) {
        BlockPos c = SectSavedData.get(ow).center(d.id());
        if (c != null) return c;
        BlockPos sp = ow.getSharedSpawnPos();
        int[] xz = Placement.site(sp.getX(), sp.getZ(), d.ax(), d.az(), ow.getSeed(), "sect:" + d.id());
        return new BlockPos(xz[0], 64, xz[1]);
    }

    // ---- sinh lười sơn môn ----
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        if (t % 20 == 7) SectTrial.tick(server);
        if (t % 40 != 19 || t < 100 || com.hukhong.city.Prebuild.owns()) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        SectSavedData reg = SectSavedData.get(ow);
        for (ServerPlayer p : ow.players()) {
            for (SectDef d : SectDef.ALL) {
                if (!d.buildable() || reg.isBuilt(d.id())) continue;
                BlockPos s = site(ow, d);
                if (Math.abs(p.getX() - s.getX()) > GEN_RADIUS || Math.abs(p.getZ() - s.getZ()) > GEN_RADIUS) continue;
                BlockPos c = SectBuilder.build(ow, CityBuilder.adjustSite(ow, s), d);
                reg.markBuilt(d.id(), c);
                com.mojang.logging.LogUtils.getLogger().info("Sect {} built at {}", d.id(), c);
                return;
            }
        }
    }

    // ---- người chơi ----
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel lv)) return;
        int t = p.tickCount;
        if (t % 20 == 0) meditate(p, lv);
        if (t % 100 == 50) SectBuffs.refresh(p);
        if (t % 1200 == 600) rollInvite(p, lv);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SectBuffs.refresh(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SectBuffs.refresh(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        stopMeditating(e.getEntity().getUUID());
        if (e.getEntity() instanceof ServerPlayer p) SectTrial.onPlayerGone(p);
    }

    public static boolean isBeQuan(ServerPlayer p) { return BE_QUAN.contains(p.getUUID()); }

    private static void stopMeditating(UUID id) {
        ACCUM.remove(id);
        BE_QUAN.remove(id);
        SEATS.release(id);
    }

    @SubscribeEvent
    public static void onTrialDrops(LivingDropsEvent e) { if (SectTrial.isTrialMob(e.getEntity())) e.setCanceled(true); }

    @SubscribeEvent
    public static void onTrialXp(LivingExperienceDropEvent e) { if (SectTrial.isTrialMob(e.getEntity())) e.setDroppedExperience(0); }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        SectTrial.onMobDeath(e.getEntity());
        if (e.getEntity() instanceof HuMob && RewardGate.canReward(e.getEntity()) && e.getSource().getEntity() instanceof ServerPlayer p)
            SectService.onHuKill(p);
    }

    // ---- Bế Quan (R22) ----

    /** Đang ngồi thiền: Shift + đứng trên Linh Khí Đài. */
    public static boolean onAltar(ServerPlayer p) {
        return p.isShiftKeyDown() && p.onGround() && p.level().getBlockState(p.getOnPos()).is(SocietyRegistry.LINH_KHI_DAI.get());
    }

    private static void meditate(ServerPlayer p, ServerLevel lv) {
        if (!onAltar(p) || p.hurtTime > 0) { stopMeditating(p.getUUID()); return; }
        CultivationData d = CultivationService.get(p);
        if (d.milestoneLocked() || d.inBottleneck() || CultivationService.inLightBottleneck(d) || d.level() >= CultivationData.MAX_LEVEL) {
            stopMeditating(p.getUUID());
            if (p.tickCount % 100 == 0) p.displayClientMessage(Component.translatable("hukhong.lkd.blocked"), true);
            return;
        }
        double mult = 1.0;
        SectData sd = SectService.get(p);
        String altarSect = SectSavedData.get(lv).nearest(p.getX(), p.getZ(), SectService.BOARD_RADIUS);
        if (altarSect != null) {
            // Đài tông: chỉ đệ tử tông đó, có ghế, trả phí mỗi giờ bằng Cống Hiến khả dụng (R22).
            SectDef def = SectDef.byId(altarSect);
            if (def == null || !altarSect.equals(sd.sect())) {
                stopMeditating(p.getUUID());
                if (p.tickCount % 100 == 0) p.displayClientMessage(Component.translatable("hukhong.lkd.not_disciple"), true);
                return;
            }
            if (!SEATS.tryTake(altarSect, p.getUUID(), SectMath.seats(def.tier()))) {
                if (p.tickCount % 100 == 0) p.displayClientMessage(Component.translatable("hukhong.lkd.full", SectMath.seats(def.tier())), true);
                return;
            }
            int secs = p.getPersistentData().getInt(SIT_KEY);
            if (SectMath.feeDue(secs)) { // 1 lần mỗi 3600 giây ngồi cộng dồn (không tính lại mỗi lần ngồi)
                int fee = SectMath.altarFeePerHour(def.tier());
                int left = SectMath.chargeFee(sd.wallet(), fee);
                boolean paidTt = false;
                if (left < 0 && com.hukhong.currency.Wallet.pay(p, com.hukhong.currency.TuTinh.altarFee(fee))) { // tra bang Tu Tinh thay Cong Hien
                    left = sd.wallet();
                    paidTt = true;
                    p.sendSystemMessage(Component.translatable("hukhong.lkd.fee_paid_tt", com.hukhong.currency.TuTinhText.of(com.hukhong.currency.TuTinh.altarFee(fee))));
                } else if (left < 0) {
                    stopMeditating(p.getUUID());
                    if (p.tickCount % 100 == 0) p.displayClientMessage(Component.translatable("hukhong.lkd.no_fee", fee, sd.wallet()), true);
                    return;
                }
                p.setData(SocietyRegistry.SECT, sd.withWallet(left));
                if (!paidTt) p.sendSystemMessage(Component.translatable("hukhong.lkd.fee_paid", fee, left));
            }
            p.getPersistentData().putInt(SIT_KEY, secs + 1);
            mult = SectMath.rankMult(SectService.rankOf(p, sd));
        }
        mult *= com.hukhong.age.AgeMath.meditationMult(com.hukhong.age.AgeService.stageOf(p)); // Ngũ Suy V: thiền x0,5
        BE_QUAN.add(p.getUUID());
        double perSec = SectMath.afkExpPerSecond(CultivationService.expToNext(d.level()), mult);
        double acc = ACCUM.merge(p.getUUID(), perSec, Double::sum);
        long whole = (long) Math.floor(acc);
        if (whole > 0) {
            ACCUM.put(p.getUUID(), acc - whole);
            CultivationService.addExp(p, whole);
        }
        lv.sendParticles(ModParticles.LINH_KHI.get(), p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.6, 0.4, 0.02);
        if (p.tickCount % 100 == 0)
            p.displayClientMessage(Component.translatable("hukhong.lkd.meditating", Math.round(perSec * 3600), mult), true);
    }

    // ---- thư mời ngẫu nhiên ----
    private static void rollInvite(ServerPlayer p, ServerLevel lv) {
        SectData sd = SectService.get(p);
        CultivationData c = CultivationService.get(p);
        if (sd.member() || !sd.invite().isEmpty() || c.level() < 10) return;
        var apt = SectMath.best(c.aptitude().values());
        List<SectDef> pool = new ArrayList<>();
        SectSavedData reg = SectSavedData.get(lv);
        for (SectDef d : SectDef.ALL)
            if (!d.hidden() && d.buildable() && reg.isBuilt(d.id()) && SectService.checkApply(p, d, true) == SectMath.OK) pool.add(d);
        if (!pool.isEmpty()) {
            SectDef d = pool.get(p.getRandom().nextInt(pool.size()));
            if (p.getRandom().nextDouble() < SectMath.inviteChance(d.tier(), apt)) {
                p.setData(SocietyRegistry.SECT, sd.withInvite(d.id()));
                BlockPos c0 = reg.center(d.id());
                p.sendSystemMessage(Component.translatable("hukhong.sect.invited", Component.translatable(d.nameKey()), c0.getX(), c0.getZ()));
                return;
            }
        }
        // Tông ẩn: tin đồn + Huyết Ảnh Lệnh, chỉ tới Ma tu mang Ác Danh (tọa độ làm tròn 100 block).
        for (SectDef d : SectDef.ALL) {
            if (!d.hidden() || SectService.checkApply(p, d, true) != SectMath.OK) continue;
            if (p.getRandom().nextDouble() >= SectMath.inviteChance(d.tier(), apt) * 3) return;
            p.setData(SocietyRegistry.SECT, sd.withInvite(d.id()));
            BlockPos s = site(lv, d);
            ItemStack token = new ItemStack(SocietyRegistry.HUYET_ANH_LENH.get());
            if (!p.getInventory().add(token)) p.drop(token, false);
            p.sendSystemMessage(Component.translatable("hukhong.sect.rumor", Math.round(s.getX() / 100.0) * 100, Math.round(s.getZ() / 100.0) * 100));
            return;
        }
    }

    // ---- lệnh ----
    public static int locate(CommandSourceStack src, String only) {
        ServerLevel ow = src.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return 0;
        SectSavedData reg = SectSavedData.get(ow);
        double px = src.getPosition().x, pz = src.getPosition().z;
        int n = 0;
        for (SectDef d : SectDef.ALL) {
            if (only != null && !d.id().equals(only)) continue;
            if (!d.buildable()) continue;
            BlockPos s = site(ow, d);
            boolean built = reg.isBuilt(d.id());
            src.sendSuccess(() -> Component.translatable("hukhong.sect.locate_line", Component.translatable(d.nameKey()),
                    Component.translatable(d.tier().key()), s.getX(), s.getZ(), (int) Math.hypot(s.getX() - px, s.getZ() - pz),
                    Component.translatable(built ? "hukhong.city.built" : "hukhong.city.unbuilt")), false);
            n++;
        }
        if (n == 0) src.sendFailure(Component.translatable("hukhong.sect.unknown"));
        return n;
    }
}
