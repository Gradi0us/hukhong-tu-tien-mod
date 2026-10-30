package com.hukhong.creature.pet;

/** Toan thuan cho linh thu: dieu kien thuan phuc, cap theo chu, nhat do. */
public final class PetMath {
    private PetMath() {}

    /** Thuan phuc duoc khi HP con lai <= hpFrac * HP toi da (mac dinh 30%). */
    public static boolean canTame(double health, double maxHealth, double hpFrac) {
        return maxHealth > 0 && health > 0 && health <= maxHealth * hpFrac + 1e-6;
    }

    /** Cap thu chien dau = cap chu (toi thieu 1). */
    public static int petLevel(int ownerLevel) { return Math.max(1, ownerLevel); }

    /** Nhat do duoc: da het delay, con do, du tuoi (tranh nhat do vua nem). */
    public static boolean pickable(boolean empty, int pickupDelay, int age) { return !empty && pickupDelay <= 0 && age >= 20; }

    /** Linh Ho chi mê hoac quai thuong, khong dong boss: HP toi da <= tran. */
    public static boolean charmable(double maxHealth) { return maxHealth <= CHARM_MAX_HP; }

    public static final double CHARM_MAX_HP = 400;
    public static final int CHARM_COOLDOWN = 160, CHARM_TICKS = 100;
}
