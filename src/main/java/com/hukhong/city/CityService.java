package com.hukhong.city;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuMob;
import com.hukhong.gates.RewardGate;
import com.hukhong.society.Herbs;
import com.hukhong.society.SocietyRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 10 thành: sinh lười khi người chơi tới gần, chức vị theo điểm cống hiến, bảng Thành, lệnh locate. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CityService {
    private CityService() {}

    /** Khoảng cách (block, theo XZ) tới neo thì dựng thành (chunk quanh người chơi đã/đang nạp). */
    public static final int GEN_RADIUS = 110;
    /** Tầm tính điểm khi hạ quái Hư Hóa gần thành. */
    public static final int KILL_RADIUS = 96;

    // ---- vị trí ----

    /** Tâm thành đã dựng, hoặc null. */
    public static BlockPos builtCenter(ServerLevel ow, CityDef d) {
        if (d.index() == 0) {
            CitySavedData c = CitySavedData.get(ow);
            return c.isBuilt() ? c.center() : null;
        }
        return CityRegistryData.get(ow).center(d.id());
    }

    /** Vị trí đặt (đã dựng thì tâm thực, chưa thì neo + lệch tất định, chỉ xấp xỉ vì có thể né nước). */
    public static BlockPos site(ServerLevel ow, CityDef d) {
        BlockPos c = builtCenter(ow, d);
        if (c != null) return c;
        BlockPos sp = ow.getSharedSpawnPos();
        if (d.index() == 0) return CityBuilder.chooseSite(ow);
        int[] xz = Placement.site(sp.getX(), sp.getZ(), d.ax(), d.az(), ow.getSeed(), d.id());
        return new BlockPos(xz[0], 64, xz[1]);
    }

    // ---- sinh lười ----

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        if (t % 40 != 7) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || t < 100) return;
        CityRegistryData reg = CityRegistryData.get(ow);
        for (ServerPlayer p : ow.players()) {
            touchResidency(ow, p);
            for (CityDef d : CityDef.ALL) {
                if (d.index() == 0 || reg.isBuilt(d.id())) continue;
                BlockPos s = site(ow, d);
                if (Math.abs(p.getX() - s.getX()) > GEN_RADIUS || Math.abs(p.getZ() - s.getZ()) > GEN_RADIUS) continue;
                BlockPos c = CityBuilder.build(ow, CityBuilder.adjustSite(ow, s), d);
                reg.markBuilt(d.id(), c);
                com.mojang.logging.LogUtils.getLogger().info("City {} built at {}", d.id(), c);
                return; // tối đa 1 thành mỗi lần kiểm để tránh giật
            }
        }
    }

    /** Bước vào hộp thành = đăng ký Cư Dân. */
    private static void touchResidency(ServerLevel ow, ServerPlayer p) {
        for (CityDef d : CityDef.ALL) {
            BlockPos c = builtCenter(ow, d);
            if (c == null || Math.abs(p.getX() - c.getX()) > CityBuilder.HALF + 1 || Math.abs(p.getZ() - c.getZ()) > CityBuilder.HALF + 1) continue;
            CityStanding st = p.getData(SocietyRegistry.CITY_STANDING);
            if (!st.resident(d.id())) {
                p.setData(SocietyRegistry.CITY_STANDING, st.withResident(d.id()));
                if (d.index() != 0) p.sendSystemMessage(Component.translatable("hukhong.city.resident_of", Component.translatable(d.nameKey())));
            }
        }
    }

    // ---- điểm + chức vị ----

    public static long cycleOf(ServerLevel l) { return l.getGameTime() / CityRank.CYCLE_TICKS; }

    public static CityRank.Rank rankOf(ServerPlayer p, ServerLevel ow, CityDef d) {
        CityStanding st = p.getData(SocietyRegistry.CITY_STANDING);
        boolean free = CityRegistryData.get(ow).lordFreeFor(d.id(), p.getUUID());
        return CityRank.rankFor(st.of(d.id()), CultivationService.get(p).level(), d.tier(), free);
    }

    /** Cộng điểm (áp trần Chu Kỳ). Trả số điểm thực cộng. */
    public static int addPoints(ServerPlayer p, ServerLevel ow, CityDef d, int add) {
        CityStanding st = p.getData(SocietyRegistry.CITY_STANDING);
        long cyc = cycleOf(ow);
        int ok = CityRank.allowed(st.earnedIn(cyc), add, d.tier());
        if (ok <= 0) return 0;
        CityRank.Rank before = rankOf(p, ow, d);
        p.setData(SocietyRegistry.CITY_STANDING, st.withAdded(d.id(), ok, cyc));
        refreshRank(p, ow, d, before);
        return ok;
    }

    /** Sau khi đổi điểm/cảnh giới: chiếm ghế Thành Chủ nếu đủ, báo thăng chức. */
    public static void refreshRank(ServerPlayer p, ServerLevel ow, CityDef d, CityRank.Rank before) {
        CityRank.Rank now = rankOf(p, ow, d);
        if (now == CityRank.Rank.THANH_CHU) CityRegistryData.get(ow).claimLord(d.id(), p.getUUID(), p.getGameProfile().getName());
        if (now.ordinal() > before.ordinal())
            p.sendSystemMessage(Component.translatable("hukhong.city.promoted", Component.translatable(d.nameKey()), Component.translatable(now.key())));
    }

    /** Thành đã dựng gần nhất trong bán kính r (XZ), hoặc null. */
    public static CityDef nearest(ServerLevel ow, double x, double z, double r) {
        CityDef best = null;
        double bd = r * r;
        for (CityDef d : CityDef.ALL) {
            BlockPos c = builtCenter(ow, d);
            if (c == null) continue;
            double dd = (c.getX() - x) * (c.getX() - x) + (c.getZ() - z) * (c.getZ() - z);
            if (dd <= bd) { bd = dd; best = d; }
        }
        return best;
    }

    /** Hạ quái Hư Hóa (không từ spawner) gần thành -> điểm thành đó. */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof HuMob m) || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (!RewardGate.canReward(e.getEntity()) || !(p.level() instanceof ServerLevel)) return;
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        CityDef d = nearest(ow, e.getEntity().getX(), e.getEntity().getZ(), KILL_RADIUS);
        if (d == null || !p.getData(SocietyRegistry.CITY_STANDING).resident(d.id())) return;
        int got = addPoints(p, ow, d, CityRank.killPoints(m.huLevel()));
        if (got > 0) p.displayClientMessage(Component.translatable("hukhong.city.points_gain", got, Component.translatable(d.nameKey())), true);
    }

    // ---- bảng Thành ----

    public static void openBoard(ServerPlayer sp, BlockPos pos) {
        ServerLevel ow = sp.server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        CityDef d = nearest(ow, pos.getX(), pos.getZ(), CityBuilder.HALF + 8);
        if (d == null) { sp.displayClientMessage(Component.translatable("hukhong.city.board_orphan"), true); return; }
        CityStanding st = sp.getData(SocietyRegistry.CITY_STANDING);
        if (!st.resident(d.id())) { st = st.withResident(d.id()); sp.setData(SocietyRegistry.CITY_STANDING, st); }
        CityRegistryData reg = CityRegistryData.get(ow);
        var snap = new CityMenu.Snapshot(pos, d.id(), rankOf(sp, ow, d).ordinal(), st.of(d.id()), CultivationService.get(sp).level(),
                reg.lordName(d.id()), sp.getUUID().equals(reg.lord(d.id())),
                CityRank.cycleCap(d.tier()) - st.earnedIn(cycleOf(ow)), Herbs.count(sp.getInventory()));
        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CityMenu(id, inv, snap), Component.translatable(d.nameKey())),
                buf -> snap.write(buf));
    }

    public static boolean handleButton(ServerPlayer sp, BlockPos pos, int id) {
        if (id != CityMenu.B_DONATE) return false;
        ServerLevel ow = sp.server.getLevel(Level.OVERWORLD);
        if (ow == null) return false;
        CityDef d = nearest(ow, pos.getX(), pos.getZ(), CityBuilder.HALF + 8);
        if (d == null) return false;
        if (Herbs.count(sp.getInventory()) < CityRank.HERBS_PER_DONATION) {
            sp.displayClientMessage(Component.translatable("hukhong.city.need_herbs", CityRank.HERBS_PER_DONATION), true);
            return false;
        }
        long cyc = cycleOf(ow);
        int room = CityRank.allowed(sp.getData(SocietyRegistry.CITY_STANDING).earnedIn(cyc), CityRank.DONATION_POINTS, d.tier());
        if (room < CityRank.DONATION_POINTS) {
            sp.displayClientMessage(Component.translatable("hukhong.city.cycle_cap"), true);
            return false;
        }
        Herbs.take(sp.getInventory(), CityRank.HERBS_PER_DONATION);
        addPoints(sp, ow, d, CityRank.DONATION_POINTS);
        openBoard(sp, pos);
        return true;
    }

    // ---- lệnh ----

    /** /hukhong locate_city [id]: liệt kê 10 thành (hoặc 1), kèm gần nhất. */
    public static int locate(CommandSourceStack src, String only) {
        ServerLevel ow = src.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return 0;
        double px = src.getPosition().x, pz = src.getPosition().z;
        int n = 0;
        CityDef near = null;
        double nd = Double.MAX_VALUE;
        for (CityDef d : CityDef.ALL) {
            if (only != null && !d.id().equals(only)) continue;
            BlockPos s = site(ow, d);
            boolean built = builtCenter(ow, d) != null;
            double dist = Math.hypot(s.getX() - px, s.getZ() - pz);
            if (dist < nd) { nd = dist; near = d; }
            src.sendSuccess(() -> Component.translatable("hukhong.city.locate_line", Component.translatable(d.nameKey()),
                    Component.translatable(d.tier().key()), s.getX(), s.getZ(), (int) dist,
                    Component.translatable(built ? "hukhong.city.built" : "hukhong.city.unbuilt")), false);
            n++;
        }
        if (n == 0) { src.sendFailure(Component.translatable("hukhong.city.unknown")); return 0; }
        if (only == null && near != null) {
            CityDef nn = near;
            src.sendSuccess(() -> Component.translatable("hukhong.city.nearest", Component.translatable(nn.nameKey())), false);
        }
        return n;
    }
}
