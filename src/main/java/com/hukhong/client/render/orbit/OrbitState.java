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

        Flight(int owner, int target, Vec3 from, Vec3 to, long start, int flight, int ret, double side, float size, ItemStack stack) {
            this.owner = owner; this.target = target; this.from = from; this.to = to; this.start = start;
            this.flight = flight; this.ret = ret; this.side = side; this.size = size; this.stack = stack;
        }

        boolean done(double time) { return time - start >= flight + (owner >= 0 ? ret : 5); }
    }

    private static final Random RND = new Random();
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private static final int MAX = 80;

    public static List<Flight> all() { return FLIGHTS; }

    private static void add(Flight f) {
        if (FLIGHTS.size() >= MAX) FLIGHTS.remove(0);
        FLIGHTS.add(f);
    }

    /** Đòn đánh thường: count kiếm phóng nối tiếp (cách 2 tick) từ sau vai chủ tới mục tiêu. */
    public static void onFx(int ownerId, int targetId, int flight, int count) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity t = level.getEntity(targetId);
        Vec3 to = t == null ? Vec3.ZERO : t.position().add(0, t.getBbHeight() * 0.55, 0);
        for (int i = 0; i < count; i++)
            add(new Flight(ownerId, targetId, null, to, level.getGameTime() + 2L * i, flight,
                    BanMenhMath.STRIKE_RETURN_TICKS, i % 2 == 0 ? 1 : -1, 1f, ItemStack.EMPTY));
    }

    /** Phi kiếm kỹ năng: from -> from+dir trong flight tick sau delay tick. Chất lượng thấp bỏ bớt các kiếm trễ. */
    public static void addSkill(Vec3 from, Vec3 dir, int flight, int delay, float size) {
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
        add(new Flight(-1, -1, from, to, level.getGameTime() + delay, flight, 0, RND.nextBoolean() ? 1 : -1,
                size <= 0 ? 1f : size, st));
    }

    /** Chủ có kiếm đang bay (kể cả chờ phóng / quay về) -> không vẽ kiếm treo sau vai. */
    public static boolean busy(int ownerId, double time) {
        for (Flight f : FLIGHTS) if (f.owner == ownerId && !f.done(time)) return true;
        return false;
    }

    public static void prune(double time) { FLIGHTS.removeIf(f -> f.done(time)); }

    public static void clear() { FLIGHTS.clear(); }
}
