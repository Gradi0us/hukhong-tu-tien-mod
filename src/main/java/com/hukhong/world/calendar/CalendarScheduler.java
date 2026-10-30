package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.events.WorldBossManager;
import com.hukhong.story.world.StoryHooks;
import com.hukhong.travel.TravelService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bo lap lich (server tick, 1 lan / 20 tick): bao truoc 2 phut + 30 giay, bat dau / ket thuc, toi da 2 su kien cung luc. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CalendarScheduler {
    private CalendarScheduler() {}

    public static final int WARN_1 = 2400, WARN_2 = 600, MAX_CONCURRENT = 2, STEP = 20, SYNC_EVERY = 100;
    public static final long HORIZON = 3 * LichMath.DAY, LATE_GRACE = 100;
    /** Ban kinh (block) de coi nguoi choi "o gan" luc su kien ket thuc. */
    public static final int NEAR_RADIUS = 64;

    public record Occ(EventDef def, long start) {
        public String key() { return def.id() + "@" + start; }
        public long end() { return start + def.duration(); }
    }

    /** Cac lan xuat hien co thoi diem bat dau trong [from, to), theo thu tu thoi gian. */
    public static List<Occ> occurrences(long from, long to) {
        List<Occ> out = new ArrayList<>();
        for (EventDef d : EventDefs.all())
            for (long s : d.rec().starts(d.id(), from, to)) out.add(new Occ(d, s));
        out.sort(Comparator.comparingLong(Occ::start));
        return out;
    }

    /** Dung lai mot Occ tu key (kiem tra chu ky co sinh ra start do). null neu khong hop le. */
    public static Occ parse(String key) {
        int i = key.indexOf('@');
        if (i <= 0) return null;
        EventDef d = EventDefs.get(key.substring(0, i));
        if (d == null) return null;
        long s = CalendarData.startOf(key);
        return d.rec().starts(d.id(), s, s + 1).isEmpty() ? null : new Occ(d, s);
    }

    public static BlockPos locate(ServerLevel ow, EventDef d, long start) {
        if ("loi_vu".equals(d.type())) return LoiVuHandler.locate(ow, d.id(), start); // ngay tai hang CHG
        int c = d.cityOf(start, CityDef.ALL.size());
        if (c < 0) return new BlockPos(d.loc().x(), 64, d.loc().z());
        return CityService.site(ow, CityDef.ALL.get(c));
    }

    public static CalendarData.Act toAct(ServerLevel ow, Occ o) {
        BlockPos p = locate(ow, o.def(), o.start());
        return new CalendarData.Act(o.key(), o.def().id(), o.start(), o.end(), o.def().cityOf(o.start(), CityDef.ALL.size()), p.getX(), p.getZ());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().overworld();
        if (ow == null) return;
        long t = ow.getGameTime();
        if (t % STEP != 0) return;
        step(ow, t);
        if (t % SYNC_EVERY == 0) CalendarNet.syncAll(ow);
    }

    static void step(ServerLevel ow, long now) {
        CalendarData data = CalendarData.get(ow);
        // ket thuc
        for (CalendarData.Act a : new ArrayList<>(data.active().values()))
            if (now >= a.end()) finish(ow, data, a);
        // bat dau + bao truoc
        for (Occ o : occurrences(now - LATE_GRACE, now + HORIZON)) {
            String key = o.key();
            if (data.isDone(key)) continue;
            if (o.start() <= now) {
                data.markDone(key);
                if (data.active().size() >= MAX_CONCURRENT) continue; // qua gioi han: bo lan nay
                begin(ow, data, o);
                continue;
            }
            long left = o.start() - now;
            int bits = data.announced(key), nb = bits;
            if (left <= WARN_2) nb |= 3;
            else if (left <= WARN_1) nb |= 1;
            if (nb != bits) {
                data.setAnnounced(key, nb);
                if ((bits & 1) == 0 && (nb & 1) != 0 && left > WARN_2) announce(ow, o, "hukhong.cal.warn_2m");
                if ((bits & 2) == 0 && (nb & 2) != 0) announce(ow, o, "hukhong.cal.warn_30s");
            }
        }
        if (now % LichMath.DAY == 0) data.prune(now - 2 * LichMath.DAY);
    }

    private static void announce(ServerLevel ow, Occ o, String key) {
        BlockPos p = locate(ow, o.def(), o.start());
        for (ServerPlayer pl : ow.players()) {
            if (o.def().story() && !StoryHooks.allowed(pl, o.def().lore())) continue;
            pl.sendSystemMessage(Component.translatable(key, Component.translatable(o.def().nameKey()), place(o.def(), o.start(), p)));
            pl.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 0.9f, 0.8f);
        }
    }

    private static Component place(EventDef d, long start, BlockPos p) {
        int c = d.cityOf(start, CityDef.ALL.size());
        return c >= 0 ? Component.translatable(CityDef.ALL.get(c).nameKey()) : Component.literal(p.getX() + ", " + p.getZ());
    }

    private static void begin(ServerLevel ow, CalendarData data, Occ o) {
        CalendarData.Act a = toAct(ow, o);
        data.addActive(a);
        EventDef d = o.def();
        BlockPos p = new BlockPos(a.x(), 64, a.z());
        for (ServerPlayer pl : ow.players()) {
            if (d.story() && !StoryHooks.allowed(pl, d.lore())) continue;
            pl.sendSystemMessage(Component.translatable("hukhong.cal.started", Component.translatable(d.nameKey()), place(d, o.start(), p)));
            pl.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 1.0f, 0.6f);
        }
        try {
            if (CalendarHandlers.begin(ow, d, a)) { /* handler rieng (Dot 5) */ } else switch (d.type()) {
                case "yeu_vuong" -> {                                  // boss the gioi co san, sinh gan diem su kien
                    BlockPos land = TravelService.findLanding(ow, a.x(), a.z(), null);
                    int kind = WorldBossManager.kindOf(ow.getBiome(land).unwrapKey().map(k -> k.location().getPath()).orElse(""));
                    if (kind < 0) kind = (int) (LichMath.unit(d.id(), o.start()) * 3);
                    WorldBossManager.spawn(ow, kind, land);
                }
                default -> { } // linh_khi_trieu / thu_trieu / prophecy: chi danh dau vi tri + dong ho (chua co he thong gameplay)
            }
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().error("Calendar event {} trigger failed", d.id(), ex);
        }
    }

    /** Ket thuc: lan story-lane, nguoi choi o gan (trong NEAR_RADIUS) va duoc phep thi nhan StoryHooks.complete. */
    private static void finish(ServerLevel ow, CalendarData data, CalendarData.Act a) {
        data.removeActive(a.key());
        try { CalendarHandlers.end(ow, a); } catch (RuntimeException ex) { com.mojang.logging.LogUtils.getLogger().error("Calendar end {} failed", a.defId(), ex); }
        EventDef d = EventDefs.get(a.defId());
        if (d == null) return;
        for (ServerPlayer pl : ow.players()) {
            if (d.story() && !StoryHooks.allowed(pl, d.lore())) continue;
            pl.sendSystemMessage(Component.translatable("hukhong.cal.ended", Component.translatable(d.nameKey())));
            if (d.story()) {
                double dx = pl.getX() - a.x(), dz = pl.getZ() - a.z();
                if (dx * dx + dz * dz <= (double) NEAR_RADIUS * NEAR_RADIUS) StoryHooks.complete(pl, d.lore());
            }
        }
    }
}
