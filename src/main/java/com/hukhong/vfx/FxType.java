package com.hukhong.vfx;

/** Loại hiệu ứng dựng phía client (xem client/fx). Thứ tự = id mạng, chỉ được thêm vào cuối. */
public enum FxType {
    SWORD_CRESCENT, SWORD_RAIN, SWORD_DOMAIN, TALISMAN_CIRCLE, LIGHT_PILLAR, GROUND_CRACK, SHOCKWAVE,
    BLOOD_VORTEX, DEMON_SILHOUETTE, LIGHTNING_BOLT, TELEGRAPH, STORM_CLOUD, SCREEN_TINT, BEAM,
    BREAKTHROUGH, UPGRADE_BURST;

    private static final FxType[] V = values();
    public static FxType of(int i) { return V[Math.floorMod(i, V.length)]; }
}
