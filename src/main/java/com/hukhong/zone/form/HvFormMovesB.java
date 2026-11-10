package com.hukhong.zone.form;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.entity.HuSkill;
import com.hukhong.vfx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bo chieu RIENG (kieu Elden Ring) cho 4 dang Hu Vuong: Sa Tran (Great Sand Worm), Doc Chuong (Rot Knight / Kindred of Rot),
 * Hai Uyen (Magma/Lizard Wyrm duoi bien), Thien Dien (Godskin Duo: than gay nhanh + than beo cham luan phien).
 * Moi dang 7 chieu thuong + 1 toi thuong (cutscene). Moi chieu co telegraph truoc khi gay sat thuong.
 * Thi trien: AI rieng ({@link #pick}) + director ({@code PhasedBoss.startOwnMove} -> {@link #start}); trang thai trong {@link St} cua thuc the.
 * Thoai: hukhong.dialog.hvf_&lt;dang&gt;.*; ten chieu: hukhong.boss.move.&lt;id&gt;.
 */
public final class HvFormMovesB {
    private static final Logger LOG = LogUtils.getLogger();
    private HvFormMovesB() {}

    public static final int SAND = 0xE0B060, SAND_D = 0x8A6A30, ROT = 0xD0402A, TOXIC = 0x7BD04A, SEA = 0x2E8FD8, FOAM = 0xBFEFFF,
            BLACK = 0x2A0A10, EMBER = 0xFF5A20, GOLD = 0xFFE9A0, DANGER = 0xFF2030;

    /** Mot chieu: id, he so sat thuong (x ATTACK_DAMAGE), hieu ung trung. */
    public record Mv(String id, double mult, Hit hit) {}

    private static final Map<HvForm, List<Mv>> SETS = new EnumMap<>(HvForm.class);
    static {
        SETS.put(HvForm.SA_TRAN, List.of(new Mv("st_quat_duoi", 1.0, Hit.KNOCK_UP), new Mv("st_chui_vot", 1.4, Hit.KNOCK_UP),
                new Mv("st_suoi_cat", 1.0, Hit.KNOCK_UP), new Mv("st_lao_nuot", 1.3, Hit.NAUSEA), new Mv("st_phun_cat", 1.0, Hit.BLIND),
                new Mv("st_lun_cat", 0.8, Hit.SLOW), new Mv("st_cuong_xoay", 0.9, Hit.KNOCK_UP), new Mv("st_sa_hai_thon", 2.2, Hit.KNOCK_UP)));
        SETS.put(HvForm.DOC_CHUONG, List.of(new Mv("dc_nem_giao", 0.9, Hit.POISON), new Mv("dc_mu_thoi", 1.0, Hit.POISON),
                new Mv("dc_doc_tri", 0.7, Hit.POISON), new Mv("dc_an_phuc", 1.4, Hit.KNOCK_UP), new Mv("dc_dam_lien", 1.0, Hit.WEAK),
                new Mv("dc_mua_giao", 0.9, Hit.POISON), new Mv("dc_thoi_bao", 1.3, Hit.KNOCK_UP), new Mv("dc_van_thoi", 2.0, Hit.POISON)));
        SETS.put(HvForm.HAI_UYEN, List.of(new Mv("hy_tho_thuy", 1.0, Hit.SLOW), new Mv("hy_cuong_trieu", 1.2, Hit.SLOW),
                new Mv("hy_lao_can", 1.3, Hit.KNOCK_UP), new Mv("hy_xoay_keo", 1.0, Hit.SLOW), new Mv("hy_quat_duoi", 1.0, Hit.KNOCK_UP),
                new Mv("hy_thuy_tru", 1.0, Hit.KNOCK_UP), new Mv("hy_lan_song", 1.3, Hit.KNOCK_UP), new Mv("hy_uyen_hai_dao", 2.2, Hit.SLOW)));
        SETS.put(HvForm.THIEN_DIEN, List.of(new Mv("td_song_nhan", 0.8, Hit.WEAK), new Mv("td_hac_diem_cau", 1.1, Hit.WEAK),
                new Mv("td_thien_loi", 1.1, Hit.BLIND), new Mv("td_lan_than", 1.3, Hit.KNOCK_UP), new Mv("td_ap_bung", 1.4, Hit.KNOCK_UP),
                new Mv("td_hoa_luan", 1.0, Hit.WEAK), new Mv("td_hac_diem_nghi", 1.0, Hit.KNOCK_UP), new Mv("td_than_bi_le", 2.4, Hit.WEAK)));
    }

    /** Thien Dien: chieu than GAY (nhanh) va than BEO (cham), luan phien. */
    static final Set<String> TD_THIN = Set.of("td_song_nhan", "td_hac_diem_cau", "td_thien_loi", "td_hac_diem_nghi");

    public static boolean owns(HvForm f) { return SETS.containsKey(f); }

    /** Cac id chieu cua dang (7 thuong + toi thuong cuoi). Rong neu dang khong thuoc bo nay. */
    public static List<String> ids(HvForm f) {
        List<String> out = new ArrayList<>();
        for (Mv m : SETS.getOrDefault(f, List.of())) out.add(m.id);
        return out;
    }

    public static String ultId(HvForm f) { List<String> l = ids(f); return l.isEmpty() ? null : l.get(l.size() - 1); }

    private static Mv mv(HvForm f, String id) {
        for (Mv m : SETS.getOrDefault(f, List.of())) if (m.id.equals(id)) return m;
        return null;
    }

    // ------------------------------------------------------------------ trang thai (1 / thuc the)
    /** Vung dat nguy hiem: cat lun / thoi / xoay nuoc / hac diem. */
    static final class Pool {
        final Vec3 c; final double r, pull; int left; final float dmg; final Hit hit; final int rgb;
        Pool(Vec3 c, double r, int left, float dmg, Hit hit, int rgb, double pull) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; this.hit = hit; this.rgb = rgb; this.pull = pull; }
    }

    public static final class St {
        String mv; int t, ultCd = 500, done;
        /** Game-time luc chieu bat dau (watchdog, doc lap voi t). */
        long startGt;
        boolean fat;
        Vec3 aim = new Vec3(0, 0, 1), spot, from, to;
        LivingEntity tgt;
        final Set<UUID> hit = new HashSet<>();
        final List<Vec3> spots = new ArrayList<>();
        final List<Pool> pools = new ArrayList<>();
        public boolean busy() { return mv != null; }
        public String current() { return mv; }
        /** So chieu rieng da thi trien xong (GameTest). */
        public int done() { return done; }
        public int pools() { return pools.size(); }
        public boolean fat() { return fat; }
    }

    // ------------------------------------------------------------------ cong vao tu HuVuongForm
    /** Bat dau chieu rieng; false neu dang khong thuoc bo nay / id la / dang ban. */
    public static boolean start(ServerLevel sl, HuVuongForm b, String id, LivingEntity target) {
        St s = b.mvB;
        Mv m = mv(b.form(), id);
        if (m == null || s.mv != null || !b.isAlive()) return false;
        s.mv = id; s.t = 0; s.startGt = sl.getGameTime(); s.tgt = target; s.spot = null; s.from = null; s.to = null;
        s.hit.clear(); s.spots.clear();
        b.getNavigation().stop();
        reaim(b, s);
        boolean ult = id.equals(ultId(b.form()));
        Component name = Component.translatable("hukhong.boss.move." + id);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : name, true);
        b.triggerAnim("skill", ult ? "phase" : "cast");
        if (ult) { s.ultCd = 900; ultScene(sl, b); }
        return true;
    }

    /** Chieu dai nhat vai tram tick; qua 400 tick (move time hoac game-time) la ket. */
    static final int MAX_MOVE_TICKS = 400;

    static boolean expired(ServerLevel sl, St s) {
        return s.t > MAX_MOVE_TICKS || sl.getGameTime() - s.startGt > MAX_MOVE_TICKS;
    }

    /** Moi tick server (khi khong dong bang): chay vung nguy hiem + chieu dang thi trien. true = dang ban chieu rieng. */
    public static boolean step(ServerLevel sl, HuVuongForm b) {
        St s = b.mvB;
        if (s.ultCd > 0) s.ultCd--;
        if (!s.pools.isEmpty()) tickPools(sl, b, s);
        if (s.mv == null) return false;
        if (b.director().transitioning() || !b.isAlive()) { end(b, s); return false; }
        b.getNavigation().stop();
        if (expired(sl, s)) {
            LOG.warn("HvFormMovesB watchdog: move {} exceeded {} ticks, ending", s.mv, MAX_MOVE_TICKS);
            end(b, s);
            return false;
        }
        boolean more;
        try { more = run(sl, b, s, s.t++); }
        catch (RuntimeException e) {
            LOG.warn("HvFormMovesB move {} threw, ending: {}", s.mv, e.toString(), e);
            more = false;
        }
        if (!more) {
            end(b, s);
            s.done++;
            if (b.form() == HvForm.THIEN_DIEN) swapBody(sl, b, s);
            b.holdSkills(com.hukhong.events.BossBuff.cd(b.phase() == 1 ? 70 : b.phase() == 2 ? 55 : 40));
        }
        return true;
    }

    /** AI rieng chon chieu (khi het hoi chieu, co muc tieu gan). false = dang khong thuoc bo nay (dung bo chung). */
    public static boolean pick(ServerLevel sl, HuVuongForm b, LivingEntity t) {
        HvForm f = b.form();
        if (!owns(f)) return false;
        St s = b.mvB;
        if (s.mv != null) return true;
        List<String> all = ids(f);
        int ph = b.phase();
        if (ph >= 3 && s.ultCd <= 0 && b.getRandom().nextInt(4) == 0) return start(sl, b, all.get(7), t) || true;
        List<String> pool = new ArrayList<>(all.subList(0, ph == 1 ? 4 : ph == 2 ? 6 : 7));
        if (f == HvForm.THIEN_DIEN) pool.removeIf(id -> TD_THIN.contains(id) == s.fat);
        if (pool.isEmpty()) pool.add(all.get(0));
        start(sl, b, pool.get(b.getRandom().nextInt(pool.size())), t);
        return true;
    }

    public static void cancel(HuVuongForm b) {
        St s = b.mvB;
        end(b, s);
        s.pools.clear();
    }

    private static void end(HuVuongForm b, St s) {
        s.mv = null; s.tgt = null; s.spots.clear();
        b.setNoGravity(false);
        if (b.burrowing()) b.setBurrowing(false);
    }

    private static boolean run(ServerLevel sl, HuVuongForm b, St s, int t) {
        return switch (s.mv) {
            case "st_quat_duoi" -> tailWhip(sl, b, s, t, SAND, 7.5);
            case "st_chui_vot" -> burrowStrike(sl, b, s, t, SAND, 4.5, false);
            case "st_suoi_cat" -> geysers(sl, b, s, t, SAND, 3);
            case "st_lao_nuot" -> lunge(sl, b, s, t, SAND, 12, 1);
            case "st_phun_cat" -> breath(sl, b, s, t, SAND, 12, 32);
            case "st_lun_cat" -> quicksand(sl, b, s, t);
            case "st_cuong_xoay" -> thrash(sl, b, s, t);
            case "st_sa_hai_thon" -> sandSea(sl, b, s, t);
            case "dc_nem_giao" -> spearThrows(sl, b, s, t);
            case "dc_mu_thoi" -> breath(sl, b, s, t, ROT, 10, 40);
            case "dc_doc_tri" -> toxicPools(sl, b, s, t);
            case "dc_an_phuc" -> burrowStrike(sl, b, s, t, ROT, 3.8, true);
            case "dc_dam_lien" -> thrusts(sl, b, s, t);
            case "dc_mua_giao" -> spearRain(sl, b, s, t);
            case "dc_thoi_bao" -> leapSlam(sl, b, s, t, ROT, 4.5, true);
            case "dc_van_thoi" -> rotBloom(sl, b, s, t);
            case "hy_tho_thuy" -> breath(sl, b, s, t, SEA, 14, 25);
            case "hy_cuong_trieu" -> tidalWave(sl, b, s, t);
            case "hy_lao_can" -> lunge(sl, b, s, t, SEA, 15, 2);
            case "hy_xoay_keo" -> whirlpool(sl, b, s, t);
            case "hy_quat_duoi" -> tailWhip(sl, b, s, t, SEA, 7);
            case "hy_thuy_tru" -> geysers(sl, b, s, t, SEA, 2);
            case "hy_lan_song" -> burrowStrike(sl, b, s, t, SEA, 6, false);
            case "hy_uyen_hai_dao" -> abyssTide(sl, b, s, t);
            case "td_song_nhan" -> twinblade(sl, b, s, t);
            case "td_hac_diem_cau" -> fireball(sl, b, s, t);
            case "td_thien_loi" -> lightning(sl, b, s, t);
            case "td_lan_than" -> bodyRoll(sl, b, s, t);
            case "td_ap_bung" -> leapSlam(sl, b, s, t, BLACK, 5.5, false);
            case "td_hoa_luan" -> flameWheels(sl, b, s, t);
            case "td_hac_diem_nghi" -> flameRitual(sl, b, s, t);
            case "td_than_bi_le" -> godskinRite(sl, b, s, t);
            default -> false;
        };
    }

    // ================================================================== chieu dung chung (tham so hoa theo mau)

    /** Quat duoi: quet 270 do sau lung (nón ngược) - telegraph 16 tick. */
    private static boolean tailWhip(ServerLevel sl, HuVuongForm b, St s, int t, int rgb, double r) {
        if (t == 0) { Fx.telegraphCone(sl, null, b.position(), s.aim.scale(-1), (float) r, 135, 16); b.playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f); }
        if (t == 16) {
            b.triggerAnim("skill", "attack");
            hitCone(sl, b, s, b.position(), s.aim.scale(-1), r, 135, 1.0);
            for (int i = 0; i < 3; i++) Fx.qi(sl, b.position().add(0, 0.8 + i * 0.4, 0), s.aim.scale(-r * 0.8), (float) (r * 0.7), rgb, 60 + i * 50, 10);
            dust(sl, b.position(), rgb, r, 40);
            Fx.shake(sl, b.position(), 1.2f, 8, 24f);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
        }
        return t < 26;
    }

    /** Chui dat -> truot ngam toi muc tieu -> vong canh bao -> troi len duoi chan (bat tu khi chui). */
    private static boolean burrowStrike(ServerLevel sl, HuVuongForm b, St s, int t, int rgb, double r, boolean spear) {
        if (t == 0) {
            b.setBurrowing(true);
            s.from = b.position();
            dust(sl, b.position(), rgb, 3, 60);
            Fx.groundCrack(sl, null, b.position(), 3f, rgb, 6, 30);
            b.playSound(SoundEvents.ROOTED_DIRT_BREAK, 3f, 0.5f);
        }
        if (t > 0 && t <= 20) {
            Vec3 q = s.from.lerp(tpos(b, s), t / 20.0);
            b.teleportTo(q.x, q.y, q.z);
            if (t % 2 == 0) dust(sl, q, rgb, 1.2, 8);
        }
        if (t == 20) { s.spot = tpos(b, s); Fx.telegraphCircle(sl, null, s.spot, (float) r, 16, DANGER); Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) r * 0.6f, rgb, 16, 6); }
        if (t > 20 && t < 36 && t % 3 == 0) dust(sl, s.spot, rgb, r * 0.5, 10);
        if (t == 36) {
            b.teleportTo(s.spot.x, s.spot.y, s.spot.z);
            b.setBurrowing(false);
            b.triggerAnim("skill", "attack");
            hitCircle(sl, b, s, s.spot, r, 1.0);
            Fx.elem(sl, null, Fx.E_SPIKES, s.spot, new Vec3(0, 1, 0), (float) r * 0.4f, rgb, 0);
            Fx.groundCrack(sl, null, s.spot, (float) r, rgb, 8, 40);
            if (spear) { Fx.rift(sl, null, s.spot, new Vec3(0, 6, 0), 0.6f, rgb, 20); addPool(sl, b, s, s.spot, 3, 100, 0.2, Hit.POISON, ROT, 0); }
            else Fx.ribbonHelix(sl, null, s.spot, s.spot.add(0, 6, 0), r * 0.4, 0, 10, 0, 0.6f, rgb);
            dust(sl, s.spot, rgb, r, 80);
            Fx.shake(sl, s.spot, 2.5f, 14, 32f);
            b.setDeltaMovement(0, 0.6, 0); b.hurtMarked = true;
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
        }
        return t < 48;
    }

    /** Suoi phun (cat / nuoc): vong 2.6 duoi chan moi nguoi + diem ngau nhien, n dot. */
    private static boolean geysers(ServerLevel sl, HuVuongForm b, St s, int t, int rgb, int waves) {
        int per = 18;
        if (t % per == 0 && t < waves * per) {
            s.spots.clear();
            for (Player p : players(sl, b.position(), 24)) s.spots.add(p.position());
            for (int i = 0; i < 2; i++) s.spots.add(b.position().add(b.getRandom().nextGaussian() * 6, 0, b.getRandom().nextGaussian() * 6));
            for (Vec3 c : s.spots) Fx.telegraphCircle(sl, null, c, 2.6f, per, DANGER);
            b.playSound(rgb == SEA ? SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE : SoundEvents.SAND_BREAK, 3f, 0.5f);
            s.hit.clear();
        }
        if (t % per == per - 1 && t < waves * per) {
            for (Vec3 c : s.spots) {
                hitCircleOnce(sl, b, s, c, 2.6, 1.0);
                Fx.ribbonHelix(sl, null, c, c.add(0, 7, 0), 0.9, t * 0.4, 10, 0, 0.45f, rgb);
                Fx.elem(sl, null, rgb == SEA ? Fx.E_TIDE : Fx.E_SPIKES, c, new Vec3(0, 1, 0), 1.2f, rgb, 0);
                sl.sendParticles(rgb == SEA ? ParticleTypes.SPLASH : dustOpt(), c.x, c.y + 2, c.z, 30, 0.6, 2, 0.6, 0.1);
            }
            Fx.shake(sl, b.position(), 1f, 6, 28f);
        }
        return t < waves * per + 6;
    }

    /** Lao can: vach canh bao -> luot thang + can manh o cuoi (n lan). */
    private static boolean lunge(ServerLevel sl, HuVuongForm b, St s, int t, int rgb, double len, int n) {
        int cyc = 26;
        int k = t % cyc, i = t / cyc;
        if (i >= n) return t < n * cyc + 4;
        if (k == 0) {
            reaim(b, s);
            s.hit.clear();
            double l = Math.min(len, Math.max(6, Math.sqrt(flat2(tpos(b, s), b.position())) + 4));  // vot qua muc tieu ~4 block
            s.from = b.position(); s.to = s.from.add(s.aim.scale(l));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.to.subtract(s.from), 3.0f, 16, DANGER);
            b.playSound(SoundEvents.RAVAGER_ATTACK, 2.5f, 0.5f);
        }
        if (k >= 16 && k < 22) {
            if (k == 16) b.triggerAnim("skill", "attack");
            Vec3 q = s.from.lerp(s.to, (k - 15) / 6.0);
            b.teleportTo(q.x, q.y, q.z);
            dust(sl, q, rgb, 1.5, 10);
            hitAround(sl, b, s, q, 2.4, 0.6);
        }
        if (k == 22) {
            Vec3 jaw = s.to.add(s.aim.scale(1.5));
            for (Player p : players(sl, jaw, 3)) if (flat2(p.position(), jaw) <= 9) strike(b, p, b.dmg(mult(b, s) * 0.7), hit(b, s));
            Fx.elem(sl, null, Fx.E_TONGUES, jaw, s.aim, 1.4f, rgb, 0);
            Fx.qi(sl, jaw.add(0, 1.2, 0), s.aim.scale(2), 1.8f, rgb, 90, 8);
            b.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 3f, 0.5f);
        }
        return true;
    }

    /** Hoi tho (cat / thoi / nuoc): non canh bao 16 tick -> phun quet trong `dur` tick, sat thuong nho lien tuc. */
    private static boolean breath(ServerLevel sl, HuVuongForm b, St s, int t, int rgb, double r, int halfDeg) {
        int dur = 24;
        if (t == 0) { Fx.telegraphCone(sl, null, b.position(), s.aim, (float) r, halfDeg, 16); b.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2f, 0.8f); }
        if (t >= 16 && t < 16 + dur) {
            if (t == 16) b.triggerAnim("skill", "attack");
            double sweep = Math.toRadians(halfDeg * 0.6) * Math.sin((t - 16) / (double) dur * Math.PI * 2);
            Vec3 a = rot(s.aim, sweep);
            Vec3 mouth = b.position().add(0, b.getBbHeight() * 0.6, 0);
            ParticleOptions po = rgb == SEA ? ParticleTypes.SPLASH : rgb == ROT ? ParticleTypes.SPORE_BLOSSOM_AIR : dustOpt();
            for (int i = 1; i <= 6; i++) {
                Vec3 q = mouth.add(a.scale(r * i / 6.0)).add(0, -i * 0.15, 0);
                sl.sendParticles(po, q.x, q.y, q.z, 4, 0.3 * i * 0.4, 0.2, 0.3 * i * 0.4, 0.02);
            }
            if ((t - 16) % 4 == 0) {
                Fx.qi(sl, mouth, a.scale(r * 0.8), (float) (r * 0.25), rgb, 0, 8);
                Fx.ribbonWave(sl, null, mouth, mouth.add(a.scale(r)).add(0, -1.5, 0), 0.3, 0.6, 6, 0, 0.6f, rgb);
                hitCone(sl, b, s, b.position(), a, r, halfDeg * 0.7, 0.3);
                if (rgb == SEA) for (Player p : players(sl, b.position(), r)) push(p, a, 0.35);
            }
            if (t == 16 + dur - 1 && rgb == ROT) addPool(sl, b, s, b.position().add(s.aim.scale(r * 0.6)), 3, 100, 0.15, Hit.POISON, ROT, 0);
        }
        return t < 16 + dur + 4;
    }

    /** Nhay dap: bat len, vong canh bao tai muc tieu, roi xuong; rot = de lai vung thoi. */
    private static boolean leapSlam(ServerLevel sl, HuVuongForm b, St s, int t, int rgb, double r, boolean rot) {
        if (t == 0) {
            s.spot = tpos(b, s);
            Fx.telegraphCircle(sl, null, s.spot, (float) r, 24, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) r * 0.7f, rgb, 24, 6);
            b.setNoGravity(true);
            b.playSound(SoundEvents.RAVAGER_STEP, 3f, 0.4f);
        }
        if (t < 10) { b.setDeltaMovement(0, 0.7, 0); b.hurtMarked = true; }
        else if (t == 12) { b.teleportTo(s.spot.x, s.spot.y + 8, s.spot.z); b.setDeltaMovement(Vec3.ZERO); }
        else if (t > 12 && t < 24) b.setDeltaMovement(Vec3.ZERO);
        if (t == 24) {
            b.teleportTo(s.spot.x, s.spot.y, s.spot.z);
            b.setNoGravity(false);
            b.triggerAnim("skill", "attack");
            hitCircle(sl, b, s, s.spot, r, 1.0);
            Fx.groundCrack(sl, null, s.spot, (float) r, rgb, 10, 40);
            Fx.groundRing(sl, null, s.spot, (float) r * 1.3f, rot ? TOXIC : EMBER, 20);
            if (rot) addPool(sl, b, s, s.spot, 4, 120, 0.2, Hit.POISON, ROT, 0);
            else { Fx.elem(sl, null, Fx.E_TONGUES, s.spot, Vec3.ZERO, 2f, EMBER, 0); addPool(sl, b, s, s.spot, 3, 80, 0.2, Hit.WEAK, BLACK, 0); }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.spot.x, s.spot.y + 0.5, s.spot.z, 1, 0, 0, 0, 0);
            Fx.shake(sl, s.spot, 2.5f, 14, 32f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        }
        return t < 34;
    }

    // ================================================================== SA TRAN (Great Sand Worm)

    /** Cat lun: vung xoay keo vao tam, lam cham + sat thuong nho moi 10 tick. */
    private static boolean quicksand(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t == 0) { s.spot = tpos(b, s); Fx.telegraphCircle(sl, null, s.spot, 6f, 20, DANGER); b.playSound(SoundEvents.SAND_BREAK, 3f, 0.4f); }
        if (t < 20 && t % 2 == 0) Fx.ribbonHelix(sl, null, s.spot.add(0, 0.2, 0), s.spot.add(0, 0.6, 0), 5 - t * 0.2, t * 0.5, 6, 0, 0.3f, SAND_D);
        if (t == 20) {
            hitCircle(sl, b, s, s.spot, 6, 0.6);
            addPool(sl, b, s, s.spot, 6, 120, 0.25, Hit.SLOW, SAND, 0.09);
            Fx.groundRing(sl, null, s.spot, 6f, SAND, 120);
            Fx.elem(sl, null, Fx.E_TIDE, s.spot, Vec3.ZERO, 2f, SAND, 0);
        }
        return t < 30;
    }

    /** Cuong xoay: than giun quan quai, 3 vong song cat quanh than. */
    private static boolean thrash(ServerLevel sl, HuVuongForm b, St s, int t) {
        double r = 8;
        if (t == 0) { Fx.telegraphCircle(sl, null, b.position(), (float) r, 22, DANGER); b.playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.4f); }
        if (t == 22 || t == 30 || t == 38) {
            b.triggerAnim("skill", "attack");
            hitCircle(sl, b, s, b.position(), r, 0.6);
            Fx.groundRing(sl, null, b.position(), (float) r, SAND, 16);
            Fx.qi(sl, b.position().add(0, 1, 0), s.aim.scale(r), (float) r * 0.6f, SAND, t * 40, 10);
            dust(sl, b.position(), SAND, r, 50);
            Fx.shake(sl, b.position(), 1.5f, 8, 24f);
            b.playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 2f, 0.4f);
        }
        return t < 46;
    }

    /** TOI THUONG Sa Hai Thon Thien: chui xuong, ca dau truong thanh xoay cat hut vao tam, suoi cat vong ngoai, troi len nuot tron. */
    private static boolean sandSea(ServerLevel sl, HuVuongForm b, St s, int t) {
        double R = 22;
        if (t == 0) {
            s.spot = b.position();
            b.setBurrowing(true);
            Fx.telegraphCircle(sl, null, s.spot, (float) R, 60, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) R * 0.5f, SAND, 100, 10);
            b.playSound(SoundEvents.WARDEN_DIG, 4f, 0.5f);
        }
        if (t < 80) {
            if (t % 3 == 0) Fx.ribbonHelix(sl, null, s.spot.add(0, 0.2, 0), s.spot.add(0, 1, 0), R * (1 - (t % 30) / 30.0), t * 0.3, 8, 0, 0.5f, t % 6 == 0 ? SAND : SAND_D);
            for (Player p : players(sl, s.spot, R)) pullTo(p, s.spot, 0.07);
            if (t % 10 == 0) for (Player p : players(sl, s.spot, R)) if (flat2(p.position(), s.spot) <= R * R) strike(b, p, b.dmg(0.15), Hit.BLIND);
        }
        if (t == 20) {
            s.spots.clear();
            for (int i = 0; i < 10; i++) { double a = i * Math.PI / 5; s.spots.add(s.spot.add(Math.cos(a) * 14, 0, Math.sin(a) * 14)); }
            for (Vec3 c : s.spots) Fx.telegraphCircle(sl, null, c, 3f, 16, DANGER);
        }
        if (t == 36) for (Vec3 c : s.spots) {
            hitCircleOnce(sl, b, s, c, 3, 0.6);
            Fx.ribbonHelix(sl, null, c, c.add(0, 9, 0), 1.0, 0, 12, 0, 0.5f, SAND);
            sl.sendParticles(dustOpt(), c.x, c.y + 2, c.z, 40, 0.8, 2.5, 0.8, 0.1);
        }
        if (t == 60) { Fx.telegraphCircle(sl, null, s.spot, 7f, 20, DANGER); Fx.groundCrack(sl, null, s.spot, 7f, SAND_D, 12, 30); }
        if (t == 80) {
            b.teleportTo(s.spot.x, s.spot.y, s.spot.z);
            b.setBurrowing(false);
            b.triggerAnim("skill", "phase");
            s.hit.clear();
            hitCircle(sl, b, s, s.spot, 7, 1.0);
            Fx.elem(sl, null, Fx.E_NUKE, s.spot, Vec3.ZERO, 2.5f, SAND, 0);
            Fx.elem(sl, null, Fx.E_SPIKES, s.spot, new Vec3(0, 1, 0), 3f, SAND_D, 0);
            Fx.groundRing(sl, null, s.spot, 12f, SAND, 30);
            dust(sl, s.spot, SAND, 8, 200);
            Fx.shake(sl, s.spot, 3.5f, 24, 48f);
            b.setDeltaMovement(0, 0.9, 0); b.hurtMarked = true;
            b.playSound(SoundEvents.WARDEN_EMERGE, 4f, 0.6f);
        }
        return t < 96;
    }

    // ================================================================== DOC CHUONG (Rot Knight / Kindred of Rot)

    /** Nem giao doc: 3 cay giao bay toi vi tri muc tieu (vong canh bao), no doc + vung thoi nho. */
    private static boolean spearThrows(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t % 12 == 0 && t < 36) {
            Vec3 p = tpos(b, s);
            if (s.tgt != null) p = p.add(s.tgt.getDeltaMovement().multiply(6, 0, 6));
            s.spots.add(p);
            Fx.telegraphCircle(sl, null, p, 2.2f, 12, DANGER);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.TRIDENT_THROW.value(), 2.5f, 0.6f);
        }
        if (t % 12 == 11 && t < 36 && !s.spots.isEmpty()) {
            Vec3 p = s.spots.get(s.spots.size() - 1);
            Vec3 hand = b.position().add(0, b.getBbHeight() * 0.8, 0);
            Fx.ribbon(sl, null, hand, p, 1.2, 4, 0, 0.25f, TOXIC);
            Fx.rift(sl, null, p, new Vec3(0, 2.5, 0), 0.3f, ROT, 30);
            s.hit.clear();
            hitCircleOnce(sl, b, s, p, 2.2, 1.0);
            addPool(sl, b, s, p, 1.8, 60, 0.15, Hit.POISON, ROT, 0);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y + 0.5, p.z, 30, 1, 0.5, 1, 0.02);
            b.playSound(SoundEvents.TRIDENT_HIT, 2.5f, 0.6f);
        }
        return t < 44;
    }

    /** Ao doc: 5 vung thoi lan ra (duoi chan nguoi choi + ngau nhien). */
    private static boolean toxicPools(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t == 0) {
            for (Player p : players(sl, b.position(), 24)) s.spots.add(p.position());
            while (s.spots.size() < 5) s.spots.add(b.position().add(b.getRandom().nextGaussian() * 7, 0, b.getRandom().nextGaussian() * 7));
            for (Vec3 c : s.spots) { Fx.telegraphCircle(sl, null, c, 2.8f, 20, DANGER); Fx.ribbon(sl, null, b.position().add(0, 2, 0), c, 3, 18, 0, 0.3f, TOXIC); }
            b.playSound(SoundEvents.SLIME_SQUISH, 3f, 0.4f);
        }
        if (t == 20) for (Vec3 c : s.spots) {
            hitCircleOnce(sl, b, s, c, 2.8, 1.0);
            addPool(sl, b, s, c, 2.8, 140, 0.2, Hit.POISON, ROT, 0);
            Fx.elem(sl, null, Fx.E_TIDE, c, Vec3.ZERO, 1.2f, TOXIC, 0);
        }
        return t < 28;
    }

    /** Dam lien: 3 nhat giao (vach canh bao), nhat 3 lao toi. */
    private static boolean thrusts(ServerLevel sl, HuVuongForm b, St s, int t) {
        int[] tel = {0, 12, 24}, hitT = {10, 22, 38};
        for (int i = 0; i < 3; i++) {
            if (t == tel[i]) {
                reaim(b, s);
                double len = i == 2 ? 12 : 9;
                Fx.telegraphLine(sl, null, b.position().add(0, 0.05, 0), s.aim.scale(len), 2.2f, hitT[i] - tel[i], DANGER);
            }
            if (t == hitT[i]) {
                double len = i == 2 ? 12 : 9;
                if (i == 2) { Vec3 q = b.position().add(s.aim.scale(5)); b.teleportTo(q.x, q.y, q.z); }
                b.triggerAnim("skill", "attack");
                s.hit.clear();
                hitLine(sl, b, s, b.position().subtract(s.aim.scale(i == 2 ? 5 : 0)), s.aim, len, 1.4, i == 2 ? 1.3 : 0.8);
                Vec3 hand = b.position().add(0, 1.4, 0);
                Fx.rift(sl, null, hand, s.aim.scale(len - (i == 2 ? 5 : 0)), 0.4f, ROT, 14);
                Fx.ribbonWave(sl, null, hand, hand.add(s.aim.scale(len)), 0.2, 0.4, 5, 0, 0.3f, TOXIC);
                b.playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 2.5f, 0.7f);
            }
        }
        return t < 46;
    }

    /** Mua giao (Kindred of Rot): 2 dot giao doc roi tu troi xuong 8 diem. */
    private static boolean spearRain(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t == 0 || t == 30) {
            s.spots.clear();
            for (Player p : players(sl, b.position(), 24)) s.spots.add(p.position());
            while (s.spots.size() < 8) s.spots.add(b.position().add(b.getRandom().nextGaussian() * 9, 0, b.getRandom().nextGaussian() * 9));
            for (Vec3 c : s.spots) Fx.telegraphCircle(sl, null, c, 2f, 22, DANGER);
            Fx.rune(sl, null, b.position().add(0, b.getBbHeight() + 4, 0), 4f, ROT, 24, 8);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 3f, 0.5f);
        }
        if (t == 22 || t == 52) {
            s.hit.clear();
            for (Vec3 c : s.spots) {
                Fx.ribbon(sl, null, c.add(0, 14, 0), c, 0, 3, 0, 0.2f, TOXIC);
                Fx.elem(sl, null, Fx.E_NEEDLES, c, new Vec3(0, -1, 0), 1f, ROT, 0);
                hitCircleOnce(sl, b, s, c, 2, 1.0);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 0.3, c.z, 12, 0.6, 0.2, 0.6, 0.02);
            }
            b.playSound(SoundEvents.TRIDENT_HIT, 3f, 0.5f);
        }
        return t < 60;
    }

    /** TOI THUONG Van Thoi Hong Lien: bay len, hoa thoi do no ra, 3 song vanh khan lan rong + vung thoi khap noi. */
    private static boolean rotBloom(ServerLevel sl, HuVuongForm b, St s, int t) {
        double R = 20;
        if (t == 0) {
            s.spot = b.position();
            b.setNoGravity(true);
            Fx.telegraphCircle(sl, null, s.spot, (float) R, 40, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) R * 0.6f, ROT, 90, 10);
            b.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < 20) { b.setDeltaMovement(0, 0.15, 0); b.hurtMarked = true; } else if (t < 84) b.setDeltaMovement(Vec3.ZERO);
        if (t % 4 == 0 && t < 40) sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, b.getX(), b.getY() + 2, b.getZ(), 20, 3, 2, 3, 0.03);
        if (t == 40) {
            for (Player p : players(sl, s.spot, R))
                if (flat2(p.position(), s.spot) <= R * R && strike(b, p, b.dmg(mult(b, s) * 0.5), Hit.POISON)) p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                Vec3 c = s.spot.add(Math.cos(a) * 11, 0, Math.sin(a) * 11);
                addPool(sl, b, s, c, 3, 160, 0.2, Hit.POISON, ROT, 0);
                Fx.ribbon(sl, null, b.position().add(0, 2, 0), c, 3, 10, 0, 0.5f, i % 2 == 0 ? ROT : TOXIC);
            }
            Fx.elem(sl, null, Fx.E_HALOS, b.position().add(0, 2, 0), Vec3.ZERO, 4f, ROT, 0);
            Fx.shake(sl, s.spot, 2f, 14, 40f);
        }
        double[][] waves = {{50, 0, 8}, {62, 6, 14}, {74, 12, 20}};
        for (double[] w : waves) {
            if (t == (int) w[0] - 10) Fx.groundRing(sl, null, s.spot, (float) w[2], DANGER, 10);
            if (t == (int) w[0]) {
                for (Player p : players(sl, s.spot, w[2])) {
                    double d2 = flat2(p.position(), s.spot);
                    if (d2 >= w[1] * w[1] && d2 <= w[2] * w[2]) strike(b, p, b.dmg(mult(b, s) * 0.4), Hit.POISON);
                }
                Fx.groundRing(sl, null, s.spot, (float) w[2], ROT, 18);
                Fx.ribbonHelix(sl, null, s.spot, s.spot.add(0, 3, 0), w[2], t, 8, 0, 0.6f, ROT);
                b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.4f);
            }
        }
        if (t == 84) { b.setNoGravity(false); b.triggerAnim("skill", "attack"); }
        return t < 96;
    }

    // ================================================================== HAI UYEN (Wyrm duoi bien)

    /** Cuong trieu: buc tuong song rong 10 tran toi 20 block, day lui + lam cham. */
    private static boolean tidalWave(ServerLevel sl, HuVuongForm b, St s, int t) {
        double len = 20, half = 5;
        if (t == 0) {
            s.from = b.position();
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.aim.scale(len), (float) half * 2, 20, DANGER);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 3f, 0.5f);
        }
        if (t >= 20 && t < 40) {
            double d = (t - 19) * (len / 20.0);
            Vec3 c = s.from.add(s.aim.scale(d));
            Vec3 side = new Vec3(-s.aim.z, 0, s.aim.x);
            if (t % 2 == 0) {
                Fx.ribbonWave(sl, null, c.add(side.scale(-half)).add(0, 1, 0), c.add(side.scale(half)).add(0, 1, 0), 1.5, 1, 4, 0, 0.8f, t % 4 == 0 ? SEA : FOAM);
                for (int i = -5; i <= 5; i++) sl.sendParticles(ParticleTypes.SPLASH, c.x + side.x * i, c.y + 1.5, c.z + side.z * i, 6, 0.3, 0.8, 0.3, 0.1);
            }
            for (Player p : players(sl, c, half + 1)) {
                Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
                if (Math.abs(rel.dot(s.aim)) > 1.5 || Math.abs(rel.dot(side)) > half) continue;
                if (s.hit.add(p.getUUID())) strike(b, p, b.dmg(mult(b, s)), Hit.SLOW);
                push(p, s.aim, 0.6);
            }
        }
        return t < 44;
    }

    /** Xoay keo: xoay nuoc quanh than hut nguoi choi vao, cuoi cung no ap luc. */
    private static boolean whirlpool(ServerLevel sl, HuVuongForm b, St s, int t) {
        double R = 12;
        Vec3 c = b.position();
        if (t == 0) { Fx.telegraphCircle(sl, null, c, (float) R, 40, DANGER); Fx.elem(sl, null, Fx.E_TIDE, c, Vec3.ZERO, 3f, SEA, 40); b.playSound(SoundEvents.CONDUIT_ACTIVATE, 3f, 0.5f); }
        if (t < 40) {
            for (Player p : players(sl, c, R)) pullTo(p, c, 0.09);
            if (t % 3 == 0) Fx.ribbonHelix(sl, null, c.add(0, 0.2, 0), c.add(0, 1.2, 0), R * (1 - (t % 15) / 15.0), t * 0.4, 6, 0, 0.4f, t % 6 == 0 ? SEA : FOAM);
            if (t % 10 == 0) for (Player p : players(sl, c, R)) if (flat2(p.position(), c) <= R * R) strike(b, p, b.dmg(0.15), Hit.SLOW);
            if (t == 28) Fx.telegraphCircle(sl, null, c, 5f, 12, DANGER);
        }
        if (t == 40) {
            b.triggerAnim("skill", "attack");
            hitCircle(sl, b, s, c, 5, 1.0);
            for (Player p : players(sl, c, 5)) { p.setDeltaMovement(p.getDeltaMovement().add(0, 0.9, 0)); p.hurtMarked = true; }
            Fx.elem(sl, null, Fx.E_NUKE, c, Vec3.ZERO, 1.5f, SEA, 0);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1, c.z, 120, 3, 1.5, 3, 0.3);
            Fx.shake(sl, c, 2f, 10, 32f);
            b.playSound(SoundEvents.GENERIC_SPLASH, 3f, 0.4f);
        }
        return t < 50;
    }

    /** TOI THUONG Uyen Hai Dao Quyen: 4 buc song tu 4 phia khep vao, roi xoay vuc tam hut + no. */
    private static boolean abyssTide(ServerLevel sl, HuVuongForm b, St s, int t) {
        double R = 24;
        if (t == 0) {
            s.spot = b.position();
            Fx.telegraphCircle(sl, null, s.spot, (float) R, 30, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) R * 0.5f, SEA, 84, 12);
            b.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 4f, 0.5f);
        }
        if (t >= 30 && t < 60) {
            double d = R * (1 - (t - 29) / 30.0);
            for (int k = 0; k < 4; k++) {
                double a = k * Math.PI / 2;
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a)), side = new Vec3(-dir.z, 0, dir.x), c = s.spot.add(dir.scale(d));
                if (t % 2 == 0) Fx.ribbonWave(sl, null, c.add(side.scale(-d)).add(0, 1.5, 0), c.add(side.scale(d)).add(0, 1.5, 0), 2, 1, 4, 0, 1.0f, (t + k) % 2 == 0 ? SEA : FOAM);
                for (Player p : players(sl, c, Math.max(2, d))) {
                    Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
                    if (Math.abs(rel.dot(dir)) > 1.5) continue;
                    if (s.hit.add(p.getUUID())) strike(b, p, b.dmg(mult(b, s) * 0.5), Hit.SLOW);
                    push(p, dir.scale(-1), 0.5);
                }
            }
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.SPLASH, s.spot.x, s.spot.y + 1, s.spot.z, 40, d * 0.5, 1, d * 0.5, 0.2);
        }
        if (t == 60) { addPool(sl, b, s, s.spot, 8, 30, 0.2, Hit.SLOW, SEA, 0.1); Fx.telegraphCircle(sl, null, s.spot, 8f, 14, DANGER); Fx.elem(sl, null, Fx.E_TIDE, s.spot, Vec3.ZERO, 3f, SEA, 0); }
        if (t == 74) {
            b.triggerAnim("skill", "attack");
            s.hit.clear();
            hitCircle(sl, b, s, s.spot, 8, 1.0);
            Fx.elem(sl, null, Fx.E_NUKE, s.spot, Vec3.ZERO, 2.5f, SEA, 0);
            Fx.ribbonHelix(sl, null, s.spot, s.spot.add(0, 12, 0), 3, 0, 14, 0, 0.8f, FOAM);
            sl.sendParticles(ParticleTypes.SPLASH, s.spot.x, s.spot.y + 2, s.spot.z, 200, 4, 3, 4, 0.4);
            Fx.shake(sl, s.spot, 3.5f, 20, 48f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.4f);
        }
        return t < 86;
    }

    // ================================================================== THIEN DIEN (Godskin Duo)

    /** Than gay: song nhan kiem 3 nhat nhanh (non canh bao 8 tick moi nhat). */
    private static boolean twinblade(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t == 0 || t == 10 || t == 20) { reaim(b, s); Fx.telegraphCone(sl, null, b.position(), s.aim, 6f, 70, 8); }
        if (t == 8 || t == 18 || t == 28) {
            b.triggerAnim("skill", "attack");
            Vec3 q = b.position().add(s.aim.scale(1.2));
            b.teleportTo(q.x, q.y, q.z);
            hitCone(sl, b, s, b.position(), s.aim, 6, 70, t == 28 ? 1.3 : 0.8);
            Fx.qi(sl, b.position().add(0, 1.3, 0), s.aim.scale(4), 3.5f, t == 18 ? GOLD : BLACK, t == 8 ? 30 : t == 18 ? -30 : 0, 8);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, b.getX() + s.aim.x * 3, b.getY() + 1, b.getZ() + s.aim.z * 3, 12, 1.2, 0.4, 1.2, 0.02);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 1.2f);
        }
        return t < 34;
    }

    /** Than gay: cau hac diem bay thang, no + vung lua den. */
    private static boolean fireball(ServerLevel sl, HuVuongForm b, St s, int t) {
        double len = 16;
        if (t == 0) {
            s.from = b.position(); s.to = s.from.add(s.aim.scale(len));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.aim.scale(len), 2f, 14, DANGER);
            Fx.telegraphCircle(sl, null, s.to, 3.5f, 20, DANGER);
            Fx.shell(sl, null, b, 1.2f, BLACK, 14);
            b.playSound(SoundEvents.BLAZE_SHOOT, 2.5f, 0.4f);
        }
        if (t >= 14 && t < 20) {
            Vec3 q = s.from.lerp(s.to, (t - 13) / 6.0).add(0, 1.4, 0);
            sl.sendParticles(ParticleTypes.SMOKE, q.x, q.y, q.z, 10, 0.3, 0.3, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 6, 0.2, 0.2, 0.2, 0.01);
            hitAround(sl, b, s, q.add(0, -1.4, 0), 1.6, 0.6);
        }
        if (t == 20) {
            hitCircle(sl, b, s, s.to, 3.5, 1.0);
            Fx.elem(sl, null, Fx.E_NUKE, s.to, Vec3.ZERO, 1f, BLACK, 0);
            Fx.groundRing(sl, null, s.to, 3.5f, EMBER, 16);
            addPool(sl, b, s, s.to, 2.5, 80, 0.2, Hit.WEAK, BLACK, 0);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.8f);
        }
        return t < 26;
    }

    /** Than gay: sam set thien diem 2 dot duoi chan moi nguoi choi. */
    private static boolean lightning(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t == 0 || t == 18) {
            s.spots.clear();
            for (Player p : players(sl, b.position(), 26)) s.spots.add(p.position());
            if (s.spots.isEmpty()) s.spots.add(tpos(b, s));
            for (Vec3 c : s.spots) { Fx.telegraphCircle(sl, null, c, 2.4f, 16, DANGER); Fx.rune(sl, null, c.add(0, 0.1, 0), 1.6f, GOLD, 16, 5); }
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.BEACON_POWER_SELECT, 3f, 0.6f);
        }
        if (t == 16 || t == 34) {
            s.hit.clear();
            for (Vec3 c : s.spots) {
                LightningBolt lb = EntityType.LIGHTNING_BOLT.create(sl);
                if (lb != null) { lb.moveTo(c.x, c.y, c.z); lb.setVisualOnly(true); sl.addFreshEntity(lb); }
                hitCircleOnce(sl, b, s, c, 2.4, 1.0);
                Fx.elem(sl, null, Fx.E_ARC, c.add(0, 4, 0), new Vec3(0, -1, 0), 1.2f, GOLD, 0);
            }
        }
        return t < 40;
    }

    /** Than beo: lan than tron (body slam) doc vach, 2 luot (luot 2 nham lai). */
    private static boolean bodyRoll(ServerLevel sl, HuVuongForm b, St s, int t) {
        double len = 16;
        int k = t % 34, i = t / 34;
        if (i >= 2) return t < 70;
        if (k == 0) {
            reaim(b, s);
            s.hit.clear();
            s.from = b.position(); s.to = s.from.add(s.aim.scale(len));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.aim.scale(len), 3.6f, 20, DANGER);
            b.playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.6f);
        }
        if (k >= 20 && k < 32) {
            if (k == 20) b.triggerAnim("skill", "attack");
            Vec3 q = s.from.lerp(s.to, (k - 19) / 12.0);
            b.teleportTo(q.x, q.y, q.z);
            b.setXRot((k * 40) % 360 - 180);
            hitAround(sl, b, s, q, 2.4, 1.0);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, q.x, q.y + 0.4, q.z, 3, 0.6, 0.1, 0.6, 0.01);
            if (k % 3 == 0) Fx.groundRing(sl, null, q, 1.8f, EMBER, 8);
            if (k % 4 == 0) b.playSound(SoundEvents.RAVAGER_STEP, 2f, 0.5f);
        }
        if (k == 32) { b.setXRot(0); Fx.shake(sl, b.position(), 1.5f, 8, 24f); }
        return true;
    }

    /** Than beo: 6 banh xe hac diem lan ra tu than. */
    private static boolean flameWheels(ServerLevel sl, HuVuongForm b, St s, int t) {
        double len = 16;
        if (t == 0) {
            s.from = b.position();
            for (int i = 0; i < 6; i++) Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), rot(s.aim, i * Math.PI / 3).scale(len), 2.2f, 18, DANGER);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.FIRECHARGE_USE, 3f, 0.5f);
        }
        if (t >= 18 && t < 34) {
            double d = (t - 17) * len / 16.0;
            for (int i = 0; i < 6; i++) {
                Vec3 dir = rot(s.aim, i * Math.PI / 3), c = s.from.add(dir.scale(d)).add(0, 1, 0);
                if (t % 2 == 0) Fx.ribbonHelix(sl, null, c.subtract(dir.scale(0.5)), c.add(dir.scale(0.5)), 1.0, t * 0.8, 4, 0, 0.35f, t % 4 == 0 ? BLACK : EMBER);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 3, 0.4, 0.4, 0.4, 0.01);
                hitAround(sl, b, s, c.add(0, -1, 0), 1.6, 1.0);
            }
        }
        return t < 38;
    }

    /** Nghi le hac diem: vong cot lua den quanh than (trong roi ngoai). */
    private static boolean flameRitual(ServerLevel sl, HuVuongForm b, St s, int t) {
        if (t == 0) {
            s.spot = b.position();
            s.spots.clear();
            for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; s.spots.add(s.spot.add(Math.cos(a) * 5, 0, Math.sin(a) * 5)); }
            for (int i = 0; i < 12; i++) { double a = i * Math.PI / 6 + 0.26; s.spots.add(s.spot.add(Math.cos(a) * 10, 0, Math.sin(a) * 10)); }
            for (int i = 0; i < 8; i++) Fx.telegraphCircle(sl, null, s.spots.get(i), 1.8f, 20, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), 4f, BLACK, 50, 8);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 3f, 0.5f);
        }
        if (t == 14) for (int i = 8; i < s.spots.size(); i++) Fx.telegraphCircle(sl, null, s.spots.get(i), 1.8f, 20, DANGER);
        if (t == 20 || t == 34) {
            int from = t == 20 ? 0 : 8, to = t == 20 ? 8 : s.spots.size();
            s.hit.clear();
            for (int i = from; i < to; i++) {
                Vec3 c = s.spots.get(i);
                hitCircleOnce(sl, b, s, c, 1.8, 1.0);
                Fx.ribbonHelix(sl, null, c, c.add(0, 6, 0), 0.6, i, 8, 0, 0.35f, i % 2 == 0 ? BLACK : EMBER);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 12, 0.3, 1.5, 0.3, 0.02);
            }
            b.playSound(SoundEvents.BLAZE_SHOOT, 3f, 0.4f);
        }
        return t < 42;
    }

    /** TOI THUONG Than Bi Hac Diem Dai Le: ca hai than cung len - vanh hac diem, sam set, banh xe xoan, nen tam. */
    private static boolean godskinRite(ServerLevel sl, HuVuongForm b, St s, int t) {
        double R = 20;
        if (t == 0) {
            s.spot = b.position();
            b.setNoGravity(true);
            Fx.telegraphCircle(sl, null, s.spot, (float) R, 30, DANGER);
            Fx.telegraphCircle(sl, null, s.spot, 6f, 30, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) R * 0.5f, BLACK, 90, 12);
            Fx.shell(sl, null, b, 2f, BLACK, 30);
            b.playSound(SoundEvents.WITHER_SPAWN, 3f, 0.7f);
        }
        if (t < 20) { b.setDeltaMovement(0, 0.2, 0); b.hurtMarked = true; } else if (t < 76) b.setDeltaMovement(Vec3.ZERO);
        if (t == 30) {
            for (Player p : players(sl, s.spot, R)) {
                double d2 = flat2(p.position(), s.spot);
                if (d2 >= 36 && d2 <= R * R) strike(b, p, b.dmg(mult(b, s) * 0.45), Hit.WEAK);
            }
            Fx.groundRing(sl, null, s.spot, (float) R, BLACK, 30);
            Fx.groundRing(sl, null, s.spot, (float) R * 0.7f, EMBER, 24);
            for (int i = 0; i < 12; i++) { double a = i * Math.PI / 6; Vec3 c = s.spot.add(Math.cos(a) * 14, 0, Math.sin(a) * 14); Fx.ribbonHelix(sl, null, c, c.add(0, 7, 0), 0.6, a, 8, 0, 0.35f, BLACK); }
            Fx.shake(sl, s.spot, 2f, 12, 40f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        }
        if (t == 38) { s.spots.clear(); for (Player p : players(sl, s.spot, R + 4)) s.spots.add(p.position()); for (Vec3 c : s.spots) Fx.telegraphCircle(sl, null, c, 2.6f, 14, DANGER); }
        if (t == 52) {
            s.hit.clear();
            for (Vec3 c : s.spots) {
                LightningBolt lb = EntityType.LIGHTNING_BOLT.create(sl);
                if (lb != null) { lb.moveTo(c.x, c.y, c.z); lb.setVisualOnly(true); sl.addFreshEntity(lb); }
                hitCircleOnce(sl, b, s, c, 2.6, 0.5);
            }
        }
        if (t >= 40 && t < 70 && t % 2 == 0) {
            s.hit.clear();
            for (int k = 0; k < 3; k++) {
                double a = k * Math.PI * 2 / 3 + (t - 40) * 0.12, r = 3 + (t - 40) * 0.5;
                Vec3 c = s.spot.add(Math.cos(a) * r, 1, Math.sin(a) * r);
                Fx.ribbonHelix(sl, null, c.add(-0.4, 0, -0.4), c.add(0.4, 0, 0.4), 1.0, t, 4, 0, 0.35f, k == 0 ? GOLD : EMBER);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 4, 0.4, 0.4, 0.4, 0.01);
                hitCircleOnce(sl, b, s, c.add(0, -1, 0), 1.6, 0.25);
            }
        }
        if (t == 66) Fx.telegraphCircle(sl, null, s.spot, 6f, 10, DANGER);
        if (t == 76) {
            b.teleportTo(s.spot.x, s.spot.y, s.spot.z);
            b.setNoGravity(false);
            b.triggerAnim("skill", "attack");
            s.hit.clear();
            hitCircle(sl, b, s, s.spot, 6, 1.0);
            Fx.elem(sl, null, Fx.E_NUKE, s.spot, Vec3.ZERO, 2.5f, BLACK, 0);
            addPool(sl, b, s, s.spot, 5, 100, 0.25, Hit.WEAK, BLACK, 0);
            Fx.shake(sl, s.spot, 3.5f, 20, 48f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.4f);
        }
        return t < 90;
    }

    /** Thien Dien doi than (gay <-> beo) sau moi chieu: hao quang + am thanh. */
    private static void swapBody(ServerLevel sl, HuVuongForm b, St s) {
        s.fat = !s.fat;
        Fx.rune(sl, null, b.position().add(0, 0.1, 0), 2.5f, s.fat ? EMBER : GOLD, 20, 6);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, b.getX(), b.getY() + 1.5, b.getZ(), 30, 0.8, 1.2, 0.8, 0.03);
        b.playSound(s.fat ? SoundEvents.RAVAGER_AMBIENT : SoundEvents.EVOKER_AMBIENT, 2f, 0.6f);
    }

    // ================================================================== tro giup
    private static double mult(HuVuongForm b, St s) { Mv m = mv(b.form(), s.mv); return m == null ? 0.8 : m.mult; }
    private static Hit hit(HuVuongForm b, St s) { Mv m = mv(b.form(), s.mv); return m == null ? Hit.NONE : m.hit; }

    private static Vec3 tpos(HuVuongForm b, St s) {
        if (s.tgt != null && s.tgt.isAlive()) return s.tgt.position();
        return b.position().add(s.aim.scale(6));
    }

    private static void reaim(HuVuongForm b, St s) {
        Vec3 dir = tpos(b, s).subtract(b.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) s.aim = dir.normalize();
        b.setYRot((float) Math.toDegrees(Math.atan2(-s.aim.x, s.aim.z)));
        b.yBodyRot = b.getYRot(); b.yHeadRot = b.getYRot();
    }

    private static Vec3 rot(Vec3 v, double a) {
        double c = Math.cos(a), si = Math.sin(a);
        return new Vec3(v.x * c - v.z * si, 0, v.x * si + v.z * c);
    }

    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    static boolean strike(HuVuongForm b, Player p, float dmg, Hit h) {
        boolean ok = p.hurt(b.damageSources().mobAttack(b), dmg);
        if (ok) { HuSkill.hu(p, 100); PhaseMoves.applyHit(p, h); }
        return ok;
    }

    private static void hitCircle(ServerLevel sl, HuVuongForm b, St s, Vec3 c, double r, double mul) {
        for (Player p : players(sl, c, r)) if (flat2(p.position(), c) <= r * r && Math.abs(p.getY() - c.y) < 4) strike(b, p, b.dmg(mult(b, s) * mul), hit(b, s));
    }

    private static void hitCircleOnce(ServerLevel sl, HuVuongForm b, St s, Vec3 c, double r, double mul) {
        for (Player p : players(sl, c, r))
            if (flat2(p.position(), c) <= r * r && Math.abs(p.getY() - c.y) < 4 && s.hit.add(p.getUUID())) strike(b, p, b.dmg(mult(b, s) * mul), hit(b, s));
    }

    private static void hitAround(ServerLevel sl, HuVuongForm b, St s, Vec3 c, double r, double mul) { hitCircleOnce(sl, b, s, c, r, mul); }

    private static void hitCone(ServerLevel sl, HuVuongForm b, St s, Vec3 c, Vec3 dir, double r, double halfDeg, double mul) {
        double cos = Math.cos(Math.toRadians(halfDeg));
        for (Player p : players(sl, c, r)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double d = rel.length();
            if (d > r || (d > 1.2 && rel.normalize().dot(dir) < cos)) continue;
            strike(b, p, b.dmg(mult(b, s) * mul), hit(b, s));
        }
    }

    private static void hitLine(ServerLevel sl, HuVuongForm b, St s, Vec3 from, Vec3 dir, double len, double half, double mul) {
        for (Player p : players(sl, from, len)) {
            Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < -0.5 || along > len || rel.subtract(dir.scale(along)).length() > half) continue;
            if (s.hit.add(p.getUUID())) strike(b, p, b.dmg(mult(b, s) * mul), hit(b, s));
        }
    }

    private static void push(Player p, Vec3 dir, double f) { p.setDeltaMovement(p.getDeltaMovement().add(dir.x * f, 0.05, dir.z * f)); p.hurtMarked = true; }

    private static void pullTo(Player p, Vec3 c, double f) {
        Vec3 d = c.subtract(p.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1) return;
        d = d.normalize().scale(f);
        p.setDeltaMovement(p.getDeltaMovement().add(d.x, 0, d.z));
        p.hurtMarked = true;
    }

    private static ParticleOptions dustOpt() { return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()); }

    private static void dust(ServerLevel sl, Vec3 c, int rgb, double r, int n) {
        ParticleOptions po = rgb == SEA ? ParticleTypes.SPLASH : rgb == ROT ? ParticleTypes.SPORE_BLOSSOM_AIR : dustOpt();
        sl.sendParticles(po, c.x, c.y + 0.4, c.z, n, r * 0.5, 0.4, r * 0.5, 0.15);
    }

    private static void addPool(ServerLevel sl, HuVuongForm b, St s, Vec3 c, double r, int life, double mul, Hit h, int rgb, double pull) {
        s.pools.add(new Pool(c, r, life, b.dmg(mul), h, rgb, pull));
        Fx.pool(sl, null, c, (float) r, rgb, life);
    }

    private static void tickPools(ServerLevel sl, HuVuongForm b, St s) {
        for (Iterator<Pool> it = s.pools.iterator(); it.hasNext(); ) {
            Pool p = it.next();
            if (--p.left <= 0) { it.remove(); continue; }
            if (p.left % 6 == 0) dust(sl, p.c, p.rgb == BLACK ? SAND_D : p.rgb, p.r, 3);
            for (Player pl : players(sl, p.c, p.r)) {
                if (flat2(pl.position(), p.c) > p.r * p.r || Math.abs(pl.getY() - p.c.y) > 2) continue;
                if (p.pull > 0) pullTo(pl, p.c, p.pull);
                if (p.left % 10 == 0) {
                    pl.hurt(b.damageSources().magic(), p.dmg);
                    PhaseMoves.applyHit(pl, p.hit);
                    if (p.rgb == BLACK) pl.igniteForSeconds(2);
                }
            }
        }
    }

    // ================================================================== cutscene + lo dien pha
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    private static String dk(HuVuongForm b, String sub) { return "hukhong.dialog." + b.form().phaseId() + "." + sub; }

    /** Gioi thieu: Sa Tran / Hai Uyen troi tu duoi len (camera thap); Doc Chuong cham ve phia giao; Thien Dien vong quanh 2 than. */
    public static List<CamPath.Key> introCam(HuVuongForm b, int T) {
        if (!owns(b.form())) return null;
        return switch (b.form()) {
            case SA_TRAN, HAI_UYEN -> List.of(k(0, 0, 0.3, 8.0, 80, 1.0, -10), k(T * 2 / 5, 3.0, 0.5, 5.0, 64, 3.0, -20),
                    k(T * 3 / 4, -2.0, 2.5, 4.0, 52, 3.5, -6), k(T, 0, 3.0, 8.0, 68, 2.8, 0));
            case DOC_CHUONG -> List.of(k(0, 4.0, 1.0, 6.0, 70, 1.2, 0), k(T * 2 / 5, 2.0, 1.4, 3.0, 55, 1.6, -4),
                    k(T * 3 / 4, -1.0, 2.0, 3.0, 45, 2.0, 0), k(T, 0, 2.6, 7.0, 66, 1.6, 0));
            default -> List.of(k(0, 0, 4.0, 6.0, 60, 3.5, 10), k(T / 3, 4.0, 2.0, 2.0, 55, 3.0, 0),
                    k(T * 2 / 3, -4.0, 2.0, 2.0, 55, 3.0, 0), k(T, 0, 3.2, 7.0, 68, 3.0, 0));
        };
    }

    public static List<SubLine> introSubs(HuVuongForm b, int T) {
        if (!owns(b.form())) return null;
        return List.of(new SubLine(6, 36, dk(b, "name")), new SubLine(36, 72, dk(b, "intro.1")), new SubLine(72, T - 4, dk(b, "intro.2")));
    }

    static final int ULT_T = 70;

    private static void ultScene(ServerLevel sl, HuVuongForm b) {
        double s = Math.max(1.0, b.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 3.0 * s, 0.6 * s, 5.0 * s, 62, 1.8 * s, -8), k(ULT_T / 2, -3.0 * s, 0.4 * s, 4.5 * s, 72, 2.6 * s, -16),
                k(ULT_T, 0, 4.0 * s, 12.0 * s, 85, 1.0 * s, -25));
        BossPhaseDirector.sendCutscene(sl, b, ULT_T, cam, List.of(new SubLine(2, 24, dk(b, "ult.1")),
                new SubLine(24, 48, dk(b, "ult.2")), new SubLine(48, ULT_T - 2, dk(b, "ult.3"))));
        Fx.shake(sl, b.position(), 1.5f, ULT_T, 40f);
    }

    /** Lo dien pha 2/3 (goi tu applyPhase khi dang chuyen pha). */
    public static void phaseFx(ServerLevel sl, HuVuongForm b, int ph) {
        HvForm f = b.form();
        if (!owns(f) || ph < 2) return;
        int rgb = switch (f) { case SA_TRAN -> SAND; case DOC_CHUONG -> ROT; case HAI_UYEN -> SEA; default -> BLACK; };
        Vec3 c = b.position();
        Fx.groundRing(sl, null, c, ph == 2 ? 8f : 12f, rgb, 40);
        Fx.elem(sl, null, ph == 2 ? Fx.E_HALOS : Fx.E_TONGUES, c.add(0, 2, 0), Vec3.ZERO, 3f, rgb, 0);
        dust(sl, c, rgb, 6, 120);
        if (f == HvForm.THIEN_DIEN) Fx.ribbonHelix(sl, null, c, c.add(0, 8, 0), 2.5, 0, 20, 0, 0.5f, ph == 2 ? GOLD : EMBER);
    }

    /** Tu tran: cutscene tai cho nga. */
    public static void death(ServerLevel sl, HuVuongForm b) {
        if (!owns(b.form())) return;
        cancel(b);
        int T = 100;
        Vec3 at = b.position();
        int rgb = switch (b.form()) { case SA_TRAN -> SAND; case DOC_CHUONG -> ROT; case HAI_UYEN -> SEA; default -> BLACK; };
        List<CamPath.Key> cam = List.of(k(0, 0, 1.8, 5.0, 55, 1.4, 0), k(50, 3.0, 2.2, 3.5, 60, 1.2, 0), k(T, 0, 5.0, 10.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, b.yBodyRot, T, cam, List.of(new SubLine(6, 36, dk(b, "death.1")),
                new SubLine(36, 68, dk(b, "death.2")), new SubLine(68, T - 4, dk(b, "death.3"))));
        Fx.groundRing(sl, null, at, 8f, rgb, 60);
        Fx.pool(sl, null, at, 5f, rgb, 140);
        dust(sl, at, rgb, 5, 150);
    }
}
