package com.hukhong.zone.form;

import com.hukhong.honcot.Bone;
import com.hukhong.honcot.HonCotMath;

import java.util.Random;

/** Quy tac roi Hon Cot cua Hu Vuong (thuan, test duoc): khe theo dang, pham theo hang vung, chi so theo cap vung. */
public final class HvDrops {
    private HvDrops() {}

    public static final double SHARE_MIN = 0.10, SECOND_CHANCE = 0.30;

    /** Pham: vung nho -> Tu (thap), Tinh Anh -> Hac (giua), Co -> Hong (cao); Thien Dien luon Hong. */
    public static int tier(HvForm form, int zoneTier) {
        if (form == HvForm.THIEN_DIEN) return HonCotMath.TIER_HONG;
        return Math.max(HonCotMath.TIER_TU, Math.min(HonCotMath.TIER_HONG, zoneTier));
    }

    /** Roll chat luong 0..100 tang theo cap vung 20..85 (chi so Hon Cot = base x pham x roll). */
    public static int roll(int zoneLevel) {
        return (int) Math.round(Math.max(0, Math.min(1, (zoneLevel - 20) / 65.0)) * 100);
    }

    /** Nguoi choi gay >= 10% tong sat thuong cua nguoi choi duoc 1 Hon Cot rieng. */
    public static boolean qualifies(double dmg, double total) { return total > 0 && dmg >= SHARE_MIN * total; }

    /** Thien Dien: 30% ra Hon Cot thu hai (khac cai chinh). */
    public static Bone second(HvForm form, Random r) {
        if (!form.multiFace() || r.nextDouble() >= SECOND_CHANCE) return null;
        Bone[] all = Bone.values();
        Bone b;
        do { b = all[r.nextInt(all.length)]; } while (b == form.bone);
        return b;
    }
}
