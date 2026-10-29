package com.hukhong.vfx;

/** Loại hiệu ứng dựng phía client (xem client/fx). Thứ tự = id mạng, chỉ được thêm vào cuối. */
public enum FxType {
    SWORD_CRESCENT, SWORD_RAIN, SWORD_DOMAIN, TALISMAN_CIRCLE, LIGHT_PILLAR, GROUND_CRACK, SHOCKWAVE,
    BLOOD_VORTEX, DEMON_SILHOUETTE, LIGHTNING_BOLT, TELEGRAPH, STORM_CLOUD, SCREEN_TINT, BEAM,
    BREAKTHROUGH, UPGRADE_BURST, SWORD_FLY,
    /** Dải kiếm khí lưỡi liềm bay theo hướng chém (pos, dir = hướng x quãng đường, scale = bán kính). */
    SWORD_QI,
    /** Kiếm của chủ vung cung chém trước mặt (dx=ownerId, dy=roll độ (dấu = chiều), dz=bán kính, scale=cỡ kiếm, color=delay<<24, life=nhịp quét). */
    SWORD_SLASH,
    /** Kiếm rơi mũi chúc xuống rồi cắm đất một lúc, bụi + vết nứt nhỏ khi chạm (như SWORD_FLY). */
    SWORD_STAB,
    /** Dải vệt sáng (ribbon) bay from->to, xem client/fx/FxTrail. */
    RIBBON,
    /** Decal mặt đất: rune, vòng, nứt chữ thập, vũng, telegraph tròn/nón/đường (kind trong byte cao của color). */
    DECAL,
    /** Cự kiếm (model item phóng to) giáng xuống kèm viền sáng, xem client/fx/FxGiantSword. */
    GIANT_SWORD,
    /** Vết rách không gian: thấu kính mở rồi khép. */
    RIFT,
    /** Rung camera theo khoảng cách tới pos. */
    CAMERA_SHAKE,
    /** Vỏ lục giác bao quanh thực thể (Hộ Thân Cương Khí). */
    SHELL,
    /** VFX riêng từng nguyên tố Pháp Tu (kind ở byte cao color), xem client/fx/FxElem. */
    ELEM;

    private static final FxType[] V = values();
    public static FxType of(int i) { return V[Math.floorMod(i, V.length)]; }
}
