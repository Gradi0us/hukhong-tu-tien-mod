package com.hukhong.zone.form;

import com.hukhong.boss.phase.BossPhaseConfig.Pal;
import com.hukhong.honcot.Bone;

import java.util.List;
import java.util.Locale;

/**
 * 8 dang Hu Vuong (PLAN-v2 12.6). Thu tu = {@code ZoneData.Zone.form}: 0 dong bang, 1 nui, 2 rung, 3 tuyet, 4 sa mac, 5 dam, 6 bien, 7 vung lon (Thien Dien).
 * Thuan du lieu (khong phu thuoc Minecraft client): khung xuong, mau, Hon Cot roi, bo chieu theo pha.
 */
public enum HvForm {
    HUYET_MA("Huyết Ma", Skel.GIANT, Pal.BLOOD, Bone.HUYET_TRAO, 1.0, HvSkill.HUYET_TRAO, HvSkill.HUT_MAU, HvSkill.TRIEU_HU_THI),
    COT_LONG("Cốt Long", Skel.QUAD, Pal.BONE, Bone.LONG_HON, 1.0, HvSkill.LAO_HUC, HvSkill.DUOI_QUET, HvSkill.GAM_CHOANG),
    MOC_YEU("Mộc Yêu", Skel.GIANT, Pal.WOOD, Bone.HOI_SINH, 1.0, HvSkill.RE_TROI, HvSkill.GAI_MOC, HvSkill.HOI_MAU_CAY),
    BANG_PHACH("Băng Phách", Skel.SPIRIT, Pal.FROST, Bone.TINH_TAM, 0.9, HvSkill.MUA_BANG, HvSkill.DONG_BANG_VUNG, HvSkill.GUONG_BANG),
    SA_TRAN("Sa Trần", Skel.WORM, Pal.SAND, Bone.PHONG_HANH, 1.0, HvSkill.CHUI_CAT, HvSkill.LOC_CAT, HvSkill.NUOT),
    DOC_CHUONG("Độc Chướng", Skel.QUAD, Pal.TOXIC, Bone.HO_THE, 1.0, HvSkill.PHUN_DOC, HvSkill.AO_DOC, HvSkill.NO_CHUONG_KHI),
    HAI_UYEN("Hải Uyên", Skel.WORM, Pal.SEA, Bone.ANH_THIEM, 1.0, HvSkill.SONG_DU, HvSkill.KEO_XUONG_NUOC, HvSkill.XOAY),
    /** 3 mat = 3 pha, moi mat 3 chieu rieng (xem {@link #skills(int)}). */
    THIEN_DIEN("Thiên Diện", Skel.SPIRIT, Pal.HOLY, Bone.THIEN_MUC, 1.6,
            HvSkill.LUC_AM, HvSkill.QUANG_TIA, HvSkill.HO_MENH,
            HvSkill.HUYET_TOA, HvSkill.LOI_PHAT, HvSkill.NO_HAO,
            HvSkill.HU_ANH_TRIEU, HvSkill.DICH_CHUYEN, HvSkill.HU_VO_VUC);

    /** Khung xuong + kich thuoc hitbox (rong x cao). */
    public enum Skel {
        GIANT(2.2f, 4.1f), QUAD(3.0f, 2.6f), WORM(2.6f, 3.6f), SPIRIT(1.8f, 4.0f);
        public final float w, h;
        Skel(float w, float h) { this.w = w; this.h = h; }
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }

    public final String vi;
    public final Skel skel;
    public final Pal pal;
    /** Hon Cot chinh luon roi (khe hop dang). */
    public final Bone bone;
    /** He so mau goc so voi Hu Vuong mau. */
    public final double hpMul;
    private final List<HvSkill> all;

    HvForm(String vi, Skel skel, Pal pal, Bone bone, double hpMul, HvSkill... skills) {
        this.vi = vi; this.skel = skel; this.pal = pal; this.bone = bone; this.hpMul = hpMul; this.all = List.of(skills);
    }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    /** Id cau hinh pha ({@code BossPhases}) va tien to khoa lang phu de. */
    public String phaseId() { return "hvf_" + key(); }

    public String textureId() { return "hvf_" + key(); }

    public boolean multiFace() { return this == THIEN_DIEN; }

    /** Bo chieu tai pha {@code phase}: thuong pha 1 = 2 chieu dau, pha 2-3 = ca 3; Thien Dien = 3 chieu cua mat thu {@code phase}. */
    public List<HvSkill> skills(int phase) {
        int ph = Math.max(1, Math.min(3, phase));
        if (multiFace()) return all.subList((ph - 1) * 3, ph * 3);
        return ph == 1 ? all.subList(0, 2) : all;
    }

    public List<HvSkill> allSkills() { return all; }

    public static HvForm byId(int id) {
        HvForm[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, id))];
    }

    public static final int COUNT = 8;
}
