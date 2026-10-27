package com.hukhong.banmenh;

import com.hukhong.HuKhong;
import com.hukhong.item.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phi kiếm: client gửi mục tiêu đã raycast; server kiểm tầm/chắn/chủ sở hữu, phát hiệu ứng bay,
 * rồi sau STRIKE_DELAY_TICKS gọi đúng đường đánh cận chiến của vanilla (Player.attack) để mọi hiệu ứng
 * Bản Mệnh Kiếm ở BanMenhEvents.onDamage áp như đánh thường.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class BanMenhStrike {
    private BanMenhStrike() {}

    private record Pending(UUID player, UUID target, long due) {}

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, Long> READY = new HashMap<>();

    /** Kiếm hợp lệ (đúng chủ, đã roll loại) ở tay chính, hoặc rỗng. */
    private static ItemStack heldSword(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (!s.is(ModItems.BAN_MENH_KIEM.get()) || BanMenhKiemItem.typeOf(s) == null || !BanMenhKiemItem.isOwner(s, p))
            return ItemStack.EMPTY;
        return s;
    }

    public static void request(ServerPlayer p, int targetId) {
        if (!p.isAlive() || p.isSpectator()) return;
        ItemStack s = heldSword(p);
        if (s.isEmpty()) return;
        if (!(p.level().getEntity(targetId) instanceof LivingEntity t) || t == p || !t.isAlive()) return;
        double range = BanMenhMath.strikeRange(BanMenhKiemItem.stageOf(s));
        Vec3 eye = p.getEyePosition();
        Vec3 mid = t.getBoundingBox().getCenter();
        if (!inRange(eye, t, range) || !clear(p, eye, mid)) return;
        long now = p.level().getGameTime();
        Long ready = READY.get(p.getUUID());
        if (ready != null && now < ready) return;
        int stage = BanMenhKiemItem.stageOf(s);
        // thời gian bay = khoảng cách (vai -> mục tiêu) / tốc độ; server áp sát thương đúng lúc kiếm tới
        int flight = Math.max(5, BanMenhMath.flightTicks(p.position().add(0, 1.5, 0).distanceTo(mid))); // >= 5: khớp nhịp kiếm quét ngang trước mặt
        int count = BanMenhMath.strikeSwords(stage);
        READY.put(p.getUUID(), now + flight + BanMenhMath.STRIKE_RETURN_TICKS);
        PENDING.add(new Pending(p.getUUID(), t.getUUID(), now + flight));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,
                new BanMenhStrikeFxPayload(p.getId(), t.getId(), flight, count));
    }

    /** Khoảng cách từ mắt tới điểm gần nhất của hộp va chạm (dung sai 1.5 block cho độ trễ mạng). */
    private static boolean inRange(Vec3 eye, LivingEntity t, double range) {
        var b = t.getBoundingBox();
        double dx = Math.max(Math.max(b.minX - eye.x, 0), eye.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - eye.y, 0), eye.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - eye.z, 0), eye.z - b.maxZ);
        double r = range + 1.5;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    private static boolean clear(ServerPlayer p, Vec3 eye, Vec3 mid) {
        HitResult hit = p.level().clip(new ClipContext(eye, mid, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(eye) >= mid.distanceToSqr(eye) - 1.0;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = e.getServer();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pd = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(pd.player());
            if (p == null || !p.isAlive()) { it.remove(); continue; }
            if (p.level().getGameTime() < pd.due()) continue;
            it.remove();
            ItemStack s = heldSword(p);
            if (s.isEmpty()) continue; // đã đổi tay / mất kiếm: hủy đòn
            if (!(p.serverLevel().getEntity(pd.target()) instanceof LivingEntity t) || !t.isAlive()) continue;
            if (!inRange(p.getEyePosition(), t, BanMenhMath.strikeRange(BanMenhKiemItem.stageOf(s)) + 2.0)) continue;
            p.attack(t);
            sweep(p, t, s);
        }
    }

    /** Kiếm quét cung trước mặt: sinh vật quanh chủ trong hình quạt ~3,8 block (hướng về mục tiêu chính) cũng trúng, 50% đòn gốc. */
    private static void sweep(ServerPlayer p, LivingEntity main, ItemStack s) {
        var cd = p.getData(com.hukhong.core.ModAttachments.CULTIVATION);
        double bonus = cd.paths().contains(com.hukhong.core.Path.KIEM)
                ? com.hukhong.passive.PassiveMath.kiemSweepBonus(cd.aptitude().get(com.hukhong.core.Path.KIEM)) : 0;
        final double reach = SWEEP_REACH * (1 + bonus);
        if (p.level() instanceof net.minecraft.server.level.ServerLevel lv) sweepFx(lv, p, reach);
        Vec3 dir = main.position().subtract(p.position());
        dir = new Vec3(dir.x, 0, dir.z);
        if (dir.lengthSqr() < 1e-3) { Vec3 lk = p.getLookAngle(); dir = new Vec3(lk.x, 0, lk.z); }
        if (dir.lengthSqr() < 1e-6) return;
        final Vec3 fwd = dir.normalize();
        final Vec3 origin = p.position();
        var list = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach, 1.5, reach), e -> {
            if (e == p || e == main || !e.isAlive() || e.isSpectator() || e instanceof net.minecraft.world.entity.player.Player
                    || e instanceof net.minecraft.world.entity.npc.Npc
                    || (e instanceof net.minecraft.world.entity.TamableAnimal ta && ta.isOwnedBy(p))) return false;
            Vec3 c = e.position().subtract(origin);
            double dist = Math.sqrt(c.x * c.x + c.z * c.z);
            if (dist > reach + e.getBbWidth() / 2) return false;
            return dist < 0.8 || (c.x * fwd.x + c.z * fwd.z) / dist >= SWEEP_COS_HALF;
        });
        if (list.isEmpty()) return;
        float dmg = (float) (com.hukhong.stats.StatService.attackPower(p) * BanMenhService.QUALITY[BanMenhKiemItem.stageOf(s)] * 0.5);
        for (LivingEntity e : list) BanMenhEvents.extra(p, e, dmg);
    }

    /** Cung chém mềm, rõ và chậm hơn (9 nhịp thay vì 5), màu xanh nhạt. */
    private static void sweepFx(net.minecraft.server.level.ServerLevel lv, ServerPlayer p, double reach) {
        com.hukhong.vfx.Fx.slash(lv, p, 25, Math.min(4.6, reach * 0.9), 0, 9, 1.4f);
        Vec3 look = p.getLookAngle();
        Vec3 from = p.getEyePosition().subtract(0, 0.5, 0).add(look.scale(0.6));
        com.hukhong.vfx.Fx.qi(lv, from, look.scale(reach * 0.8), 2.2f, 0xBFE4FF, 25, 14);
    }

    public static final double SWEEP_REACH = 3.8, SWEEP_COS_HALF = 0.3;

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        READY.remove(id);
        PENDING.removeIf(pd -> pd.player().equals(id));
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) {
        PENDING.clear();
        READY.clear();
    }
}
