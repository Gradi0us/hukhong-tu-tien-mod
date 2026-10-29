package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.skill.Skill;
import com.hukhong.skill.SkillService;
import com.hukhong.society.SocietyRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Predicate;

/** Server: học bí tịch, gán phím, Cường Hóa. Mọi yêu cầu từ client đều được xác thực lại ở đây. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SkillBookService {
    private SkillBookService() {}

    public static SkillBookData data(ServerPlayer p) { return p.getData(SkillBookAttachments.BOOK); }

    private static void set(ServerPlayer p, SkillBookData d) {
        p.setData(SkillBookAttachments.BOOK, d);
        sync(p);
        SkillService.sync(p); // tổng hồi chiêu HUD đổi theo cấp / gán phím
    }

    public static void sync(ServerPlayer p) { PacketDistributor.sendToPlayer(p, new BookSyncPayload(data(p))); }

    private static void msg(ServerPlayer p, String key, String fallback, Object... a) {
        p.displayClientMessage(Component.translatableWithFallback(key, fallback, a), true);
    }

    /** Cấp tối đa nâng được lúc này (theo cấp tu luyện). */
    public static int levelCap(ServerPlayer p) { return SkillBookMath.levelCap(p.getData(ModAttachments.CULTIVATION).level()); }

    // ------------------------------------------------------------------ học
    /** true nếu học thành công (bí tịch bị tiêu). Bản trùng không tiêu: giữ làm vật liệu Cường Hóa. */
    public static boolean learn(ServerPlayer p, BookSkill s) {
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        SkillBookData d = data(p);
        Component name = Component.translatableWithFallback(s.langKey(), s.viName);
        if (d.learned(s.id)) {
            msg(p, "hukhong.skillbook.msg.known", "Đã học %s. Giữ bản này làm vật liệu Cường Hóa.", name);
            return false;
        }
        if (s.path != null && !cd.paths().contains(s.path)) {
            msg(p, "hukhong.skillbook.msg.wrong_path", "Bí tịch này thuộc đạo bạn chưa tu.");
            return false;
        }
        if (cd.level() < s.minLevel()) {
            msg(p, "hukhong.skillbook.msg.low_level", "Cần cấp tu luyện %s để học %s.", s.minLevel(), name);
            return false;
        }
        set(p, d.withLevel(s.id, 1));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
        p.sendSystemMessage(Component.translatableWithFallback("hukhong.skillbook.msg.learned",
                "Lĩnh ngộ thành công: %s. Mở Công Pháp (K) để gán phím.", name));
        return true;
    }

    // ------------------------------------------------------------------ gán phím
    public static void assign(ServerPlayer p, int pathOrd, int slot, String id) {
        Path[] ps = Path.values();
        if (pathOrd < 0 || pathOrd >= ps.length || slot < 0 || slot > 3) return;
        Path page = ps[pathOrd];
        if (!p.getData(ModAttachments.CULTIVATION).paths().contains(page)) return;
        SkillBookData d = data(p);
        if (id == null || id.isEmpty()) {
            set(p, d.withSlot(page, slot, null));
            return;
        }
        if (page == Path.PHAP && (slot == 3 || Skill.PHAP_ULT.icon.equals(id)) && com.hukhong.phaptu.beam.PhapBeamService.locksT(p)) {
            msg(p, "hukhong.phap.beam.locked", "Tuyệt kỹ của nguyên tố này cố định ở phím T, không thể thay bằng kỹ năng khác.");
            return;
        }
        if (!SkillBook.assignable(d, page, id)) {
            msg(p, "hukhong.skillbook.msg.cant_assign", "Không gán được kỹ năng này vào đạo này.");
            return;
        }
        set(p, d.withSlot(page, slot, id));
    }

    // ------------------------------------------------------------------ Cường Hóa
    public static SkillGrade gradeOf(String id) { return SkillBook.gradeOf(id); }

    public static boolean known(SkillBookData d, java.util.Set<Path> paths, String id) {
        Skill s = Skill.byId(id);
        if (s != null) return paths.contains(s.path);
        return d.learned(id);
    }

    public static void upgrade(ServerPlayer p, String id, int modeOrd) {
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        SkillBookData d = data(p);
        if (id == null || !known(d, cd.paths(), id)) return;
        int lv = d.level(id);
        if (lv >= SkillBookMath.MAX_LEVEL) {
            msg(p, "hukhong.skillbook.msg.max", "Kỹ năng đã đạt cấp tối đa.");
            return;
        }
        int cap = SkillBookMath.levelCap(cd.level());
        if (lv >= cap) {
            msg(p, "hukhong.skillbook.msg.cap", "Cấp tu luyện hiện tại chỉ cho Cường Hóa tối đa cấp %s.", cap);
            return;
        }
        SkillBookMath.Mode mode = SkillBookMath.Mode.of(modeOrd);
        BookSkill book = BookSkill.byId(id);
        if (mode == SkillBookMath.Mode.SCROLL && book == null) {
            msg(p, "hukhong.skillbook.msg.no_scroll_mode", "Kỹ năng nền không có bí tịch để cống nạp.");
            return;
        }
        SkillBookMath.Cost c = SkillBookMath.cost(lv, gradeOf(id), mode);
        if (honTinh(p) < c.honTinh()) {
            msg(p, "hukhong.skillbook.msg.need_hon_tinh", "Thiếu Hồn Tinh (cần %s).", c.honTinh());
            return;
        }
        if (c.scrolls() > 0 && countScrolls(p, book) < c.scrolls()) {
            msg(p, "hukhong.skillbook.msg.need_scroll", "Thiếu bản sao bí tịch (cần %s).", c.scrolls());
            return;
        }
        var sect = p.getData(SocietyRegistry.SECT);
        if (c.contribution() > 0) {
            if (!sect.member()) {
                msg(p, "hukhong.skillbook.msg.need_sect", "Cần gia nhập tông môn để dùng Cống Hiến (hoặc chọn trả bằng Hồn Tinh).");
                return;
            }
            if (sect.wallet() < c.contribution()) {
                msg(p, "hukhong.skillbook.msg.need_ch", "Thiếu Cống Hiến (cần %s).", c.contribution());
                return;
            }
        }
        com.hukhong.currency.Wallet.payHonTinh(p, c.honTinh());
        if (c.scrolls() > 0) take(p, s -> SkillBookRegistry.skillOf(s) == book, c.scrolls());
        if (c.contribution() > 0) p.setData(SocietyRegistry.SECT, sect.withWallet(sect.wallet() - c.contribution()));
        set(p, d.withLevel(id, lv + 1));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        msg(p, "hukhong.skillbook.msg.upgraded", "%s lên cấp %s.", nameOf(id), lv + 1);
    }

    public static Component nameOf(String id) { return SkillBook.nameOf(id); }

    // ------------------------------------------------------------------ kho đồ
    public static int honTinh(ServerPlayer p) { return (int) Math.min(Integer.MAX_VALUE, com.hukhong.currency.Wallet.honTinh(p)); }

    public static int countScrolls(ServerPlayer p, BookSkill b) { return b == null ? 0 : count(p, s -> SkillBookRegistry.skillOf(s) == b); }

    private static int count(ServerPlayer p, Predicate<ItemStack> f) {
        Inventory inv = p.getInventory();
        int n = 0;
        for (ItemStack s : inv.items) if (!s.isEmpty() && f.test(s)) n += s.getCount();
        for (ItemStack s : inv.offhand) if (!s.isEmpty() && f.test(s)) n += s.getCount();
        return n;
    }

    private static void take(ServerPlayer p, Predicate<ItemStack> f, int n) {
        Inventory inv = p.getInventory();
        for (var list : java.util.List.of(inv.items, inv.offhand)) {
            for (ItemStack s : list) {
                if (n <= 0) return;
                if (s.isEmpty() || !f.test(s)) continue;
                int t = Math.min(n, s.getCount());
                s.shrink(t);
                n -= t;
            }
        }
    }

    // ------------------------------------------------------------------ Trục Nâng Cấp Kỹ Năng
    /** Kỹ năng người chơi đang có và chưa đạt cấp tối đa (kỹ năng nền theo đạo đã tu + bí tịch đã học). */
    public static java.util.List<String> scrollCandidates(ServerPlayer p) {
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        SkillBookData d = data(p);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Skill s : Skill.values()) if (cd.paths().contains(s.path) && d.level(s.icon) < SkillBookMath.MAX_LEVEL) out.add(s.icon);
        for (BookSkill b : BookSkill.values()) if (d.learned(b.id) && d.level(b.id) < SkillBookMath.MAX_LEVEL) out.add(b.id);
        return out;
    }

    /** Dùng 1 Trục Nâng Cấp (trong túi) để +1 cấp kỹ năng id. Bỏ qua trần cấp tu luyện, trần tuyệt đối {@link SkillBookMath#MAX_LEVEL}. */
    public static boolean applyScroll(ServerPlayer p, String id) {
        CultivationData cd = p.getData(ModAttachments.CULTIVATION);
        SkillBookData d = data(p);
        if (id == null || !known(d, cd.paths(), id)) return false;
        if (count(p, s -> s.is(SkillBookRegistry.UPGRADE_SCROLL.get())) < 1) {
            msg(p, "hukhong.skillbook.upgrade_scroll.missing", "Không có Trục Nâng Cấp Kỹ Năng.");
            return false;
        }
        int lv = d.level(id);
        if (lv >= SkillBookMath.MAX_LEVEL) {
            msg(p, "hukhong.skillbook.msg.max", "Kỹ năng đã đạt cấp tối đa.");
            return false;
        }
        take(p, s -> s.is(SkillBookRegistry.UPGRADE_SCROLL.get()), 1);
        set(p, d.withLevel(id, lv + 1));
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.6f);
        msg(p, "hukhong.skillbook.msg.upgraded", "%s lên cấp %s.", nameOf(id), lv + 1);
        return true;
    }

    // ------------------------------------------------------------------ sự kiện
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }
}
