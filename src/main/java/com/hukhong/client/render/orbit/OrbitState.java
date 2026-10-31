package com.hukhong.client.render.orbit;

import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Trạng thái các phi kiếm đang bay phía client: đòn đánh thường của chủ (từ vai) và phi kiếm kỹ năng (FxPayload). */
public final class OrbitState {
    private OrbitState() {}

    public static final class Flight {
        final int owner;       // >=0: kiếm của người chơi, xuất phát/quay về sau vai
        final int target;      // entity mục tiêu (owner flight) hoặc -1
        Vec3 from, to;         // skill: cố định; owner: to = vị trí mục tiêu (cập nhật)
        final long start;
        final int flight, ret;
        final double side;     // chiều cong của cung bay (+-1)
        final float size;
        final ItemStack stack; // skill: kiếm hiển thị (owner: lấy từ tay chủ lúc vẽ)
        int hold = 5;          // skill: số tick còn lại (cắm đất) sau khi tới đích
        boolean slash;         // chủ vung cung chém trước mặt (thay vì phóng tới mục tiêu)
        double roll, radius;   // cung chém: góc nghiêng (độ, dấu = chiều quét) và bán kính
        boolean giant;         // kiếm khổng lồ (Liên Hoàn Trảm)
        double tiltDeg = Double.NaN; // góc nghiêng mặt chém có dấu (>0: đầu cung cao, <0: đầu cung thấp = chém hất lên); NaN: |roll|
        boolean arc;           // đòn đánh thường: phát cung kiếm khí khi vung
        boolean arcFired;
        double reach;          // tầm quạt quét (khớp hitbox server)
        int color = 0x9FE8FF;
        static final int WIND = 3;

        Flight(int owner, int target, Vec3 from, Vec3 to, long start, int flight, int ret, double side, float size, ItemStack stack) {
            this.owner = owner; this.target = target; this.from = from; this.to = to; this.start = start;
            this.flight = flight; this.ret = ret; this.side = side; this.size = size; this.stack = stack;
        }

        boolean done(double time) {
            if (slash) return time - start >= WIND + flight + ret;
            return time - start >= flight + (owner >= 0 ? ret : hold);
        }
    }

    private static final Random RND = new Random();
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private static final int MAX = 80;

    public static List<Flight> all() { return FLIGHTS; }

    private static void add(Flight f) {
        if (FLIGHTS.size() >= MAX) FLIGHTS.remove(0);
        FLIGHTS.add(f);
    }

    /** 4 kiểu chém luân phiên: {chiều quét (+1 = từ phải sang trái), độ nghiêng có dấu}. */
    static final double[][] COMBO = {{1, 22}, {-1, 22}, {1, -38}, {-1, 62}};

    /**
     * Đòn đánh thường: kiếm sau vai bay ra, chém một cung TRƯỚC MẶT chủ (hướng về mục tiêu) rồi quay về vai.
     * count kiếm nối tiếp (cách 3 tick); kiểu chém đổi theo combo (ngang phải->trái, trái->phải, hất chéo lên, bổ chéo xuống).
     * Mỗi kiếm phát một cung kiếm khí lưỡi liềm bay ra tới tầm quạt quét của server; mục tiêu xa hơn: thêm vệt kiếm khí bay tới đó.
     */
    public static void onFx(int ownerId, int targetId, int flight, int count, int combo, float reach) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity t = level.getEntity(targetId);
        Entity o = level.getEntity(ownerId);
        double dist = t != null && o != null ? o.position().distanceTo(t.position()) : 0;
        double rch = reach > 1 ? reach : 3.8;
        double radius = Math.max(2.0, rch - 0.85); // mũi kiếm chạm đúng mép quạt hitbox
        int color = o instanceof Player pl ? OrbitRenderer.colorOf(OrbitRenderer.heldSword(pl)) : 0x9FE8FF;
        for (int i = 0; i < count; i++) {
            Flight f = new Flight(ownerId, targetId, null, Vec3.ZERO, level.getGameTime() + 3L * i, SWEEP_TICKS,
                    BanMenhMath.STRIKE_RETURN_TICKS - 3, 1, 1f, ItemStack.EMPTY);
            double[] c = COMBO[Math.floorMod(combo + i, COMBO.length)];
            f.slash = true; f.radius = radius; f.roll = c[0]; f.tiltDeg = c[1];
            f.arc = true; f.reach = rch; f.color = color;
            add(f);
        }
        if (t != null && o != null && dist > rch + 0.4)
            com.hukhong.client.fx.FxClient.qi(o.getEyePosition().subtract(0, 0.5, 0), t.position().add(0, t.getBbHeight() * 0.5, 0), 0.8f, color, 3, flight);
    }

    public static final int SWEEP_TICKS = 5;

    /** Kiếm chủ vung cung chém (kỹ năng): roll độ (dấu = chiều quét), bán kính, cỡ kiếm; giant = kiếm khổng lồ. */
    public static void addSlash(int ownerId, double roll, double radius, int delay, int sweep, float size) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Flight f = new Flight(ownerId, -1, null, Vec3.ZERO, level.getGameTime() + delay, Math.max(3, sweep), 6, 1, size <= 0 ? 1f : size, ItemStack.EMPTY);
        f.slash = true; f.roll = roll; f.radius = radius; f.giant = size >= 2.5f;
        add(f);
    }

    /** Phi kiếm kỹ năng: from -> from+dir trong flight tick sau delay tick. Chất lượng thấp bỏ bớt các kiếm trễ. */
    public static void addSkill(Vec3 from, Vec3 dir, int flight, int delay, float size) { addSkill(from, dir, flight, delay, size, 5); }

    public static void addSkill(Vec3 from, Vec3 dir, int flight, int delay, float size, int hold) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float q = switch (ClientConfig.fxQuality()) { case LOW -> 0.5f; case MED -> 0.75f; default -> 1f; };
        if (delay > 0 && RND.nextFloat() > q) return;
        Vec3 to = from.add(dir);
        ItemStack st = ItemStack.EMPTY;
        for (Player p : level.players()) {
            if (p.distanceToSqr(to) > 24 * 24 && p.distanceToSqr(from) > 24 * 24) continue;
            ItemStack s = OrbitRenderer.heldSword(p);
            if (!s.isEmpty()) { st = s.copyWithCount(1); break; }
        }
        if (st.isEmpty()) st = new ItemStack(Items.IRON_SWORD);
        Flight fl = new Flight(-1, -1, from, to, level.getGameTime() + delay, flight, 0, RND.nextBoolean() ? 1 : -1,
                size <= 0 ? 1f : size, st);
        fl.hold = Math.max(1, hold);
        add(fl);
    }

    /** Chủ có kiếm đang bay (kể cả chờ phóng / quay về) -> không vẽ kiếm treo sau vai. */
    public static boolean busy(int ownerId, double time) {
        for (Flight f : FLIGHTS) if (f.owner == ownerId && !f.done(time) && (!f.slash || time >= f.start)) return true;
        return false;
    }

    public static void prune(double time) { FLIGHTS.removeIf(f -> f.done(time)); }

    public static void clear() { FLIGHTS.clear(); }
}
