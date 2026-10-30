package com.hukhong.poi;

import com.hukhong.chg.ChgKit;
import com.hukhong.chg.ChgService;
import com.hukhong.story.StoryService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Tin don (Khach Diem) va chi dan (Bia Da): tro toi buoc truyen ke tiep, dia diem chua kham pha, hang CHG chua ha, nguon Di thu chua nhat. */
public final class PoiRumor {
    private PoiRumor() {}

    /** Dia diem gan nhat chua kham pha thoa filter (thuan, test duoc); null neu khong co. */
    public static PoiInstance nearestUndiscovered(Iterable<PoiInstance> all, Set<String> found, double x, double z, Predicate<PoiInstance> filter) {
        PoiInstance best = null;
        double bd = Double.MAX_VALUE;
        for (PoiInstance i : all) {
            if (found.contains(i.key) || !filter.test(i)) continue;
            double d = i.dist2(x, z);
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    /** Nguon Di thu (lore.clue "c*") gan nhat ma nguoi choi chua co (thuan, test duoc). */
    public static PoiInstance nearestDocSource(Iterable<PoiInstance> all, Function<String, PoiDef> defs, Predicate<String> hasDoc,
                                               double x, double z, PoiInstance except) {
        PoiInstance best = null;
        double bd = Double.MAX_VALUE;
        for (PoiInstance i : all) {
            if (i == except) continue;
            PoiDef d = defs.apply(i.defId);
            String clue = d == null ? null : d.lore().clue();
            if (clue == null || !clue.startsWith("c") || hasDoc.test(clue)) continue;
            double dd = i.dist2(x, z);
            if (dd < bd) { bd = dd; best = i; }
        }
        return best;
    }

    public static String storyKey(int chapter) { return "hukhong.poi.rumor.chapter." + Math.max(1, Math.min(10, chapter)); }

    public static Component direction(PoiInstance target, double px, double pz) {
        double dx = target.x - px, dz = target.z - pz;
        return Component.translatable("hukhong.poi.dir." + PoiMath.compass8(dx, dz), (int) Math.round(PoiMath.dist(px, pz, target.x, target.z)));
    }

    public record Rumor(Component text, PoiInstance target) {}

    /** Tin don cua tieu nhi: luot chan -> dia diem chua kham pha gan nhat (neu con), luot le -> buoc truyen ke tiep. */
    public static Rumor pick(ServerPlayer p, PoiSavedData data, PoiInstance inn) {
        String ck = "rumor:" + p.getUUID();
        int n = inn.extra.getInt(ck);
        inn.extra.putInt(ck, n + 1);
        data.setDirty();
        PoiInstance t = null;
        if (n % 2 == 0)
            t = nearestUndiscovered(data.all(), data.foundBy(p.getUUID()), p.getX(), p.getZ(), i -> i != inn && !i.defId.equals(inn.defId));
        if (t != null) {
            PoiDef d = PoiDefs.get(t.defId);
            Component line = Component.translatable("hukhong.poi.rumor.fmt", Component.translatable(d == null ? "hukhong.poi.n." + t.defId : d.rumorKey()),
                    direction(t, p.getX(), p.getZ()));
            return new Rumor(line, t);
        }
        int ch = com.hukhong.story.world.StoryHooks.chapter(p);
        return new Rumor(Component.translatable(storyKey(ch)), null);
    }

    /** Chi dan cua Bia Da: loai 1 = hang CHG (idx), 2 = nguon Di thu (poi), 0 = khong con gi. */
    public record Lead(int kind, int chgIdx, PoiInstance poi) {}

    public static int firstUndefeatedChg(ServerPlayer p) {
        int lair = StoryService.get(p).lair();
        for (int idx = 1; idx <= ChgKit.COUNT; idx++) if (!ChgKit.defeated(lair, idx)) return idx;
        return 0;
    }

    public static Lead lead(ServerPlayer p, ServerLevel ow, PoiSavedData data, PoiInstance stele) {
        int chg = firstUndefeatedChg(p);
        PoiInstance doc = nearestDocSource(data.all(), PoiDefs::get, c -> StoryService.get(p).has(c), p.getX(), p.getZ(), stele);
        boolean chgFirst = stele.seed % 2 == 0;
        if (chgFirst) {
            if (chg > 0) return new Lead(1, chg, null);
            if (doc != null) return new Lead(2, 0, doc);
        } else {
            if (doc != null) return new Lead(2, 0, doc);
            if (chg > 0) return new Lead(1, chg, null);
        }
        return new Lead(0, 0, null);
    }

    /** Hien chi dan cho nguoi choi (va danh dau nguon Di thu tren ban do). */
    public static void showLead(ServerPlayer p, ServerLevel ow, PoiSavedData data, Lead lead) {
        switch (lead.kind()) {
            case 1 -> ChgService.revealLair(p, lead.chgIdx());
            case 2 -> {
                PoiDef d = PoiDefs.get(lead.poi().defId);
                p.sendSystemMessage(Component.translatable("hukhong.poi.lead.doc", Component.translatable(d == null ? lead.poi().defId : d.nameKey()),
                        direction(lead.poi(), p.getX(), p.getZ())));
                PoiService.discover(p, lead.poi());
            }
            default -> p.sendSystemMessage(Component.translatable("hukhong.poi.lead.none"));
        }
    }
}
