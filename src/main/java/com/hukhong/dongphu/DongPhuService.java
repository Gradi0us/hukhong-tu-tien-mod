package com.hukhong.dongphu;

import com.hukhong.auction.Tx;
import com.hukhong.currency.SpiritStoneToll;
import com.hukhong.dongphu.DongPhuData.Claim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Động Phủ: 1 Trận Bàn/người. Bán kính theo tầng; Tụ Linh (+10%/tầng tu vi), Dược Viên (tầng 2+: thúc cây),
 * Hộ Trận (chặn quái Hư Không tự sinh), Hồi Phục (tầng 4+). Công phá mỗi 3 ngày game nếu chủ nhân trong vùng.
 */
public final class DongPhuService {
    private DongPhuService() {}

    public static final int MAX_TIER = 5;
    static final int[] RADIUS = {16, 18, 22, 27, 32};
    /** Hạ Phẩm Linh Thạch để nâng từ tầng t lên t+1 (index = t - 1). */
    static final int[] COST = {50, 150, 400, 1000};
    /** 3 ngày game (24000 tick/ngày). */
    public static final long RAID_EVERY = 3L * 24000L;
    static final double BONUS_PER_TIER = 0.10;

    public static int clampTier(int t) { return Math.max(1, Math.min(MAX_TIER, t)); }
    public static int radius(int tier) { return RADIUS[clampTier(tier) - 1]; }
    /** Linh Thạch cần để nâng từ tier lên tier+1; -1 nếu đã max. */
    public static int upgradeCost(int tier) { return tier >= MAX_TIER ? -1 : COST[clampTier(tier) - 1]; }
    public static double bonusFor(int tier) { return 1.0 + BONUS_PER_TIER * clampTier(tier); }

    static ServerLevel levelOf(MinecraftServer s, Claim c) {
        return s.getLevel(ResourceKey.create(Registries.DIMENSION, c.dim()));
    }

    static boolean inside(Vec3 p, Claim c) {
        double dx = p.x - (c.pos().getX() + 0.5), dz = p.z - (c.pos().getZ() + 0.5), r = radius(c.tier());
        return dx * dx + dz * dz <= r * r;
    }

    /** Hệ số tu vi/linh khí của người chơi: >1 khi là chủ Động Phủ và đứng trong vùng, còn lại = 1. */
    public static double bonus(Player p) {
        if (!(p instanceof ServerPlayer sp)) return 1.0;
        Claim c = DongPhuData.get(sp.server).of(sp.getUUID());
        if (c == null || !c.dim().equals(sp.level().dimension().location()) || !inside(sp.position(), c)) return 1.0;
        return bonusFor(c.tier());
    }

    /** Lượng EXP sau khi nhân hệ số Tụ Linh (dùng tại điểm cộng EXP tu luyện). */
    public static long boost(ServerPlayer p, long amount) {
        if (amount <= 0) return amount;
        return Math.round(amount * bonus(p));
    }

    /** true nếu vùng tròn (x,z,r) chạm vùng bảo hộ của một Động Phủ (cùng chiều). */
    public static boolean wardTouches(ServerLevel lv, double x, double z, double r) {
        for (Claim c : DongPhuData.get(lv.getServer()).all()) {
            if (!c.dim().equals(lv.dimension().location())) continue;
            double dx = x - c.pos().getX(), dz = z - c.pos().getZ(), rr = r + radius(c.tier());
            if (dx * dx + dz * dz <= rr * rr) return true;
        }
        return false;
    }

    /** true nếu (x,z) nằm trong vùng bảo hộ của một Động Phủ bất kỳ cùng chiều (dùng cho Hộ Trận / lan Hư Không). */
    public static boolean isWarded(ServerLevel lv, double x, double z) {
        for (Claim c : DongPhuData.get(lv.getServer()).all()) {
            if (!c.dim().equals(lv.dimension().location())) continue;
            double dx = x - (c.pos().getX() + 0.5), dz = z - (c.pos().getZ() + 0.5), r = radius(c.tier());
            if (dx * dx + dz * dz <= r * r) return true;
        }
        return false;
    }

    /** null nếu được phép đặt; ngược lại là lý do (chủ đã có Động Phủ còn tồn tại). */
    public static Component validatePlace(ServerPlayer p) {
        DongPhuData d = DongPhuData.get(p.server);
        Claim old = d.of(p.getUUID());
        if (old == null) return null;
        ServerLevel ol = levelOf(p.server, old);
        boolean alive = ol == null || !ol.hasChunkAt(old.pos()) || ol.getBlockState(old.pos()).is(DongPhuRegistry.TRAN_BAN.get());
        if (alive) return Tx.t("hukhong.dongphu.err_owned", "Bạn đã có Động Phủ tại (%s, %s, %s). Mỗi người chỉ được một Trận Bàn.",
                "You already own a Dong Phu at (%s, %s, %s). One formation core per player.",
                old.pos().getX(), old.pos().getY(), old.pos().getZ());
        d.remove(p.getUUID()); // trận bàn cũ đã mất: cho phép lập lại
        return null;
    }

    /** Lập trận: gọi sau khi khối đã đặt (Block.setPlacedBy). */
    public static void claim(ServerPlayer p, ServerLevel lv, BlockPos pos) {
        long now = lv.getServer().overworld().getGameTime();
        DongPhuData.get(p.server).put(new Claim(p.getUUID(), lv.dimension().location(), pos.immutable(), 1, now));
        DongPhuFx.activate(p, lv, Vec3.atBottomCenterOf(pos), 1, radius(1));
        p.sendSystemMessage(Tx.t("hukhong.dongphu.claimed", "Động Phủ đã an vị tại (%s, %s, %s): bán kính %s khối. Chuột phải để nâng tầng.",
                "Dong Phu settled at (%s, %s, %s): radius %s blocks. Right-click to raise its tier.",
                pos.getX(), pos.getY(), pos.getZ(), radius(1)));
    }

    /** Khối trận bàn bị phá: bỏ vùng (không hoàn Linh Thạch). */
    public static void forget(ServerLevel lv, BlockPos pos) {
        DongPhuData d = DongPhuData.get(lv.getServer());
        for (Claim c : d.all()) if (c.pos().equals(pos) && c.dim().equals(lv.dimension().location())) d.remove(c.owner());
    }

    private static Claim ownedAt(ServerPlayer p, ServerLevel lv, BlockPos pos) {
        Claim c = DongPhuData.get(p.server).of(p.getUUID());
        return c != null && c.pos().equals(pos) && c.dim().equals(lv.dimension().location()) ? c : null;
    }

    /** Nâng tầng bằng Hạ Phẩm Linh Thạch (lấy từ túi). null = thành công. */
    public static Component upgrade(ServerPlayer p, ServerLevel lv, BlockPos pos) {
        Claim c = ownedAt(p, lv, pos);
        if (c == null) return Tx.t("hukhong.dongphu.err_not_owner", "Đây không phải Động Phủ của bạn.", "This is not your Dong Phu.");
        if (c.tier() >= MAX_TIER) return Tx.t("hukhong.dongphu.err_max", "Động Phủ đã đạt tầng tối đa.", "The Dong Phu is already at max tier.");
        int cost = upgradeCost(c.tier());
        if (!SpiritStoneToll.canPay(p, cost))
            return Tx.t("hukhong.dongphu.err_stones", "Cần %s Hạ Phẩm Linh Thạch để nâng Động Phủ lên tầng %s.",
                    "Need %s Lower-grade Spirit Stones to raise the Dong Phu to tier %s.", cost, c.tier() + 1);
        SpiritStoneToll.pay(p, cost);
        int t = c.tier() + 1;
        DongPhuData.get(p.server).put(c.withTier(t));
        lv.setBlock(pos, lv.getBlockState(pos).setValue(DongPhuTranBanBlock.TIER, t), 3);
        DongPhuFx.activate(p, lv, Vec3.atBottomCenterOf(pos), t, radius(t));
        p.sendSystemMessage(Tx.t("hukhong.dongphu.upgraded", "Động Phủ nâng lên tầng %s: bán kính %s khối, Tụ Linh +%s%%.",
                "Dong Phu raised to tier %s: radius %s blocks, Gathering Qi +%s%%.", t, radius(t), Math.round(BONUS_PER_TIER * 100 * t)));
        return null;
    }

    /** Shift + chuột phải: trạng thái trong khung chat. */
    public static void status(ServerPlayer p, ServerLevel lv, BlockPos pos) {
        Claim c = ownedAt(p, lv, pos);
        if (c == null) { p.sendSystemMessage(Tx.t("hukhong.dongphu.err_not_owner", "Đây không phải Động Phủ của bạn.", "This is not your Dong Phu.")); return; }
        int t = c.tier();
        long left = Math.max(0, c.lastRaid() + RAID_EVERY - lv.getServer().overworld().getGameTime());
        p.sendSystemMessage(Tx.t("hukhong.dongphu.st_head", "=== Động Phủ · Tầng %s/%s · Bán kính %s khối ===",
                "=== Dong Phu · Tier %s/%s · Radius %s blocks ===", t, MAX_TIER, radius(t)));
        p.sendSystemMessage(Tx.t("hukhong.dongphu.st_linh", "Tụ Linh: +%s%% tu vi/linh khí (trong vùng)",
                "Gathering Qi: +%s%% cultivation gain (inside)", Math.round(BONUS_PER_TIER * 100 * t)));
        p.sendSystemMessage(Tx.t("hukhong.dongphu.st_garden", "Dược Viên: %s", "Herb Garden: %s",
                t >= 2 ? Tx.t("hukhong.dongphu.on", "đang hoạt động", "active") : Tx.t("hukhong.dongphu.need2", "mở ở tầng 2", "unlocks at tier 2")));
        p.sendSystemMessage(Tx.t("hukhong.dongphu.st_regen", "Hồi Phục: %s", "Restoration: %s",
                t >= 4 ? Tx.t("hukhong.dongphu.on", "đang hoạt động", "active") : Tx.t("hukhong.dongphu.need4", "mở ở tầng 4", "unlocks at tier 4")));
        p.sendSystemMessage(Tx.t("hukhong.dongphu.st_raid", "Công phá kế tiếp: còn khoảng %s phút (chủ nhân phải ở trong vùng)",
                "Next assault: about %s minutes left (owner must be inside)", left / 1200));
        int cost = upgradeCost(t);
        if (cost > 0) p.sendSystemMessage(Tx.t("hukhong.dongphu.st_next", "Nâng tầng %s: %s Hạ Phẩm Linh Thạch (chuột phải)",
                "Raise to tier %s: %s Lower-grade Spirit Stones (right-click)", t + 1, cost));
    }

    /** Mỗi 20 tick: kích hoạt công phá, hồi phục (tầng 4+), thúc cây (tầng 2+). */
    public static void tick(MinecraftServer s) {
        DongPhuData d = DongPhuData.get(s);
        long now = s.overworld().getGameTime();
        for (Claim c : d.all()) {
            ServerPlayer o = s.getPlayerList().getPlayer(c.owner());
            if (o == null || !o.isAlive() || o.isSpectator()) continue;
            ServerLevel lv = levelOf(s, c);
            if (lv == null || o.level() != lv || !inside(o.position(), c)) continue;
            if (c.tier() >= 2 && now % 60 == 0) garden(lv, c);
            if (c.tier() >= 4 && now % 40 == 0 && o.getHealth() < o.getMaxHealth()) o.heal(c.tier() >= 5 ? 2f : 1f);
            if (now - c.lastRaid() >= RAID_EVERY && !DongPhuRaid.active(o.getUUID())) {
                d.put(c.withLastRaid(now));
                DongPhuRaid.start(o, lv, c);
            }
        }
    }

    /** Dược Viên: thúc ngẫu nhiên vài cây trồng (CropBlock) trong vùng. */
    static void garden(ServerLevel lv, Claim c) {
        RandomSource r = lv.random;
        int r0 = radius(c.tier()), n = 3 + 2 * c.tier();
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * r0;
            int x = (int) Math.floor(c.pos().getX() + 0.5 + Math.cos(a) * d), z = (int) Math.floor(c.pos().getZ() + 0.5 + Math.sin(a) * d);
            if (!lv.hasChunk(x >> 4, z >> 4)) continue;
            int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            BlockState st = lv.getBlockState(bp);
            if (st.isRandomlyTicking() && st.getBlock() instanceof CropBlock) st.randomTick(lv, bp, r);
        }
    }
}
