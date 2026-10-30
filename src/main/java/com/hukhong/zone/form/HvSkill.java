package com.hukhong.zone.form;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.Pal;
import com.hukhong.vfx.Fx;

/**
 * Chieu rieng cua cac dang Hu Vuong (thuan du lieu; thi trien o {@link HvSkills}). {@code kind} = hinh thuc; mau/hat/FxElem rieng tung chieu.
 * windup = tick gong (co telegraph), r = ban kinh/chieu dai, dmg = he so x sat thuong boss.
 */
public enum HvSkill {
    // Huyet Ma
    HUYET_TRAO(Kind.CONE, 24, 7.5, 1.2, Hit.WEAK, Pal.BLOOD, Fx.E_TONGUES),
    HUT_MAU(Kind.DRAIN, 30, 12, 0.7, Hit.SLOW, Pal.BLOOD, Fx.E_HALOS),
    TRIEU_HU_THI(Kind.SUMMON, 36, 4, 0, Hit.NONE, Pal.VOID, -1),
    // Cot Long
    LAO_HUC(Kind.DASH, 28, 16, 1.4, Hit.KNOCK_UP, Pal.BONE, Fx.E_BOULDER),
    DUOI_QUET(Kind.RING, 26, 9, 1.1, Hit.SLOW, Pal.BONE, Fx.E_SPIKES),
    GAM_CHOANG(Kind.ROAR, 34, 14, 0.6, Hit.WEAK, Pal.BONE, Fx.E_HALOS),
    // Moc Yeu
    RE_TROI(Kind.ROOT, 36, 2.4, 0.8, Hit.SLOW, Pal.WOOD, Fx.E_TONGUES),
    GAI_MOC(Kind.THORNS, 30, 2.4, 1.0, Hit.KNOCK_UP, Pal.WOOD, Fx.E_SPIKES),
    HOI_MAU_CAY(Kind.HEAL_TREE, 40, 18, 0, Hit.NONE, Pal.WOOD, Fx.E_HALOS),
    // Bang Phach
    MUA_BANG(Kind.METEOR, 40, 3.0, 0.9, Hit.FREEZE, Pal.FROST, Fx.E_NEEDLES),
    DONG_BANG_VUNG(Kind.RING, 34, 10, 0.8, Hit.FREEZE, Pal.FROST, Fx.E_HALOS),
    GUONG_BANG(Kind.MIRROR, 24, 4, 0, Hit.NONE, Pal.FROST, Fx.E_SPIKES),
    // Sa Tran
    CHUI_CAT(Kind.BURROW, 40, 6, 1.3, Hit.KNOCK_UP, Pal.SAND, Fx.E_SPIKES),
    LOC_CAT(Kind.TORNADO, 20, 5, 0.5, Hit.BLIND, Pal.SAND, Fx.E_TIDE),
    NUOT(Kind.SWALLOW, 30, 7, 1.8, Hit.NAUSEA, Pal.SAND, Fx.E_TONGUES),
    // Doc Chuong
    PHUN_DOC(Kind.SPIT, 24, 20, 0.7, Hit.POISON, Pal.TOXIC, -1),
    AO_DOC(Kind.POOLS, 30, 3.6, 0.4, Hit.POISON, Pal.TOXIC, Fx.E_TIDE),
    NO_CHUONG_KHI(Kind.GAS, 50, 10, 1.8, Hit.NAUSEA, Pal.TOXIC, Fx.E_HALOS),
    // Hai Uyen
    SONG_DU(Kind.WAVE, 30, 22, 1.3, Hit.SLOW, Pal.SEA, Fx.E_TIDE),
    KEO_XUONG_NUOC(Kind.DRAG, 40, 16, 1.0, Hit.SLOW, Pal.SEA, Fx.E_HALOS),
    XOAY(Kind.WHIRL, 20, 7, 0.5, Hit.SLOW, Pal.SEA, Fx.E_TIDE),
    // Thien Dien - mat Tu Bi
    LUC_AM(Kind.JUMP_RING, 30, 11, 1.0, Hit.SLOW, Pal.HOLY, Fx.E_HALOS),
    QUANG_TIA(Kind.BEAMS, 30, 22, 1.1, Hit.BLIND, Pal.HOLY, Fx.E_ARC),
    HO_MENH(Kind.SHIELD, 20, 4, 0, Hit.NONE, Pal.HOLY, Fx.E_HALOS),
    // Thien Dien - mat Phan No
    HUYET_TOA(Kind.PILLARS, 36, 2.6, 1.0, Hit.KNOCK_UP, Pal.BLOOD, Fx.E_SPIKES),
    LOI_PHAT(Kind.LIGHTNING, 30, 3.0, 1.3, Hit.WEAK, Pal.MOON, Fx.E_ARC),
    NO_HAO(Kind.ROAR, 34, 15, 0.8, Hit.WEAK, Pal.BLOOD, Fx.E_HALOS),
    // Thien Dien - mat Hu Vo
    HU_ANH_TRIEU(Kind.SUMMON, 36, 4, 0, Hit.NONE, Pal.VOID, -1),
    DICH_CHUYEN(Kind.TELEPORT, 20, 5, 1.5, Hit.KNOCK_UP, Pal.VOID, Fx.E_SPIKES),
    HU_VO_VUC(Kind.POOLS, 30, 4.0, 0.6, Hit.SLOW, Pal.VOID, Fx.E_TIDE);

    public enum Kind { CONE, DRAIN, SUMMON, DASH, RING, ROAR, ROOT, THORNS, HEAL_TREE, METEOR, MIRROR, BURROW, TORNADO, SWALLOW,
        SPIT, POOLS, GAS, WAVE, DRAG, WHIRL, JUMP_RING, BEAMS, SHIELD, PILLARS, LIGHTNING, TELEPORT }

    public final Kind kind;
    public final int windup;
    public final double r, dmg;
    public final Hit hit;
    public final Pal pal;
    /** Fx.E_* (FxElem) rieng hoac -1. */
    public final int elem;

    HvSkill(Kind kind, int windup, double r, double dmg, Hit hit, Pal pal, int elem) {
        this.kind = kind; this.windup = windup; this.r = r; this.dmg = dmg; this.hit = hit; this.pal = pal; this.elem = elem;
    }

    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }

    /** Khoa lang ten chieu (actionbar). */
    public String langKey() { return "hukhong.boss.move.hv_" + id(); }
}
