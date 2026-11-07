#!/usr/bin/env python3
"""C16: model NPC cốt truyện + Dạ Khuyết dựng theo ảnh concept (2026-11-07).
Khung người chuẩn Minecraft (đầu 8, thân 8x12x4, tay/chân 4x12x4); đầu hướng -z (north = mặt trước).
Chạy: python tools/gen_c16_npcs.py  (từ D:\\mc_mod)"""
import math, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _hum_engine import Model, Paint, human_anims, hexc, mix

SKIN = "e9b48f"; SKIN_OLD = "d9a27e"


def P(base, **kw): return Paint(base, **kw)


def frame(m, head_paint, body_paint, arm_paint, leg_paint, skirt_paint=None, skirt=(10, 11, 6)):
    """Khung người: chân, thân, đầu, tay (+ váy áo dài che chân)."""
    m.bone("right_leg", None, (-2, 12, 0)); m.cube("right_leg", (-4, 0, -2), (4, 12, 4), leg_paint)
    m.bone("left_leg", None, (2, 12, 0)); m.cube("left_leg", (0, 0, -2), (4, 12, 4), leg_paint)
    m.bone("body", None, (0, 24, 0)); m.cube("body", (-4, 12, -2), (8, 12, 4), body_paint)
    if skirt_paint:
        w, h, d = skirt
        m.cube("body", (-w / 2, 12 - h, -d / 2), (w, h, d), skirt_paint)
    m.bone("head", "body", (0, 24, 0)); m.cube("head", (-4, 24, -4), (8, 8, 8), head_paint)
    m.bone("right_arm", "body", (-5, 22, 0)); m.cube("right_arm", (-8, 12, -2), (4, 12, 4), arm_paint)
    m.bone("left_arm", "body", (5, 22, 0)); m.cube("left_arm", (4, 12, -2), (4, 12, 4), arm_paint)


def head(front, hair, side=None, back=None, pal=None, top=None):
    """Đầu: front = 8 dòng; side/back mặc định = tóc 3 dòng trên rồi da (side) / toàn tóc (back)."""
    pal = dict(pal or {})
    pal.setdefault("S", SKIN); pal.setdefault("H", hair); pal.setdefault("K", "141418"); pal.setdefault("W", "f4f4f4")
    side = side or ["HHHHHHHH"] * 3 + ["SSSSSSSS"] * 5
    back = back or ["HHHHHHHH"] * 8
    top = top or ["HHHHHHHH"] * 8
    return P(hair, noise=.03, art={"north": front, "east": side, "west": side, "south": back, "up": top}, pal=pal)


def robe_pattern(base, trim, collar=True, belt=None, belt_rows=(5, 6), clouds=None):
    B, T = hexc(base), hexc(trim)
    BL = hexc(belt) if belt else None
    CL = hexc(clouds) if clouds else None

    def pat(face, x, y, w, h):
        if BL and face in ("north", "south", "east", "west") and h >= 10 and y in belt_rows: return BL
        if collar and face == "north" and h >= 10 and y < 5 and (abs(x - (w - 1) / 2) - .5 == y or abs(x - (w - 1) / 2) - .5 == y - 1): return T
        if collar and face == "north" and h >= 10 and y >= 5 and x == w // 2 - 1 + (y % 2 == 0): return mix(T, B, .4)
        if CL and (x * 5 + y * 3) % 11 in (0, 1) and (x + y) % 3 == 0: return CL
        return None
    return pat


# ============================================================================ MỘ DAO
def mo_dao():
    m = Model("npc_mo_dao", 128, bounds=(3, 3), off=1.2)
    hd = head(["WWWWWWWW", "WSSSSSSW", "SWWSSWWS", "SWKSSKWS", "SSSnnSSS", "SWWWWWWS", "WWWmmWWW", "WWWWWWWW"], "eeeeee",
              side=["WWWWWWWW", "WWWWWWWW", "WWSSSSSS", "WSSSSSSS", "WSSSSSSS", "WSSSSSSW", "WWSSSSWW", "WWWWSWWW"],
              pal={"S": SKIN_OLD, "n": "c08a68", "m": "a87060", "W": "eeeeee"})
    robe = "7f93a4"
    body = P(robe, noise=.08, pattern=robe_pattern(robe, "5d6f80", belt="a88a5a", belt_rows=(7, 8)))
    arm = P(robe, noise=.08, bottom=SKIN_OLD, bottom_h=3,
            pattern=lambda f, x, y, w, h: hexc("8c3434") if f in ("north", "east") and y in (5, 6) and x in (1, 2) else None)
    leg = P("5a4a3a", bottom="3a2e24", bottom_h=2)
    frame(m, hd, body, arm, leg, P(robe, noise=.08, bottom="6c7f90", bottom_h=1, pattern=lambda f, x, y, w, h: hexc("6d8090") if (x * 3 + y * 7) % 13 == 0 else None))
    straw = "c8ab5c"
    m.cube("head", (-2, 32, -2), (4, 3, 4), P("eeeeee"))                          # búi tóc
    m.cube("head", (-5, 33, 0), (10, 1, 1), P("7a5232"))                          # trâm gỗ
    m.cube("head", (-3, 19, -4.6), (6, 5, 1), P("eeeeee", alpha_fringe=2))        # râu dài
    m.cube("body", (-7, 21, -3), (14, 3, 7), P(straw, noise=.12, alpha_fringe=3,  # áo tơi vai
           pattern=lambda f, x, y, w, h: hexc("a88c44") if x % 2 == 0 else None))
    m.cube("body", (-5, 8, 2.2), (10, 14, 1), P(straw, noise=.12, alpha_fringe=3,
           pattern=lambda f, x, y, w, h: hexc("a88c44") if x % 2 == 0 else None))
    m.cube("body", (2, 8, -3.6), (3, 4, 2), P("8a5a34"))                          # hồ lô
    m.cube("body", (2.5, 12, -3.4), (2, 2, 1), P("6a4024"))
    m.cube("right_arm", (-6.5, 0, -4), (1, 30, 1), P("6a4a2a", noise=.1))        # gậy
    m.build()
    human_anims("npc_mo_dao")


# ============================================================================ HỨA CỔ
def hua_co():
    m = Model("npc_hua_co", 128)
    hd = head(["GSSSSSSG", "GWWSSWWG", "SffSSffS", "fOKffKOf", "SffnnffS", "SWWWWWWS", "SSWWWWSS", "SSSWWSSS"], "9a9a9a",
              side=["GGGGGGGG", "GGGGGSSS", "GGGGSSSS", "fffffSSS", "GGGSSSSS", "GGSSSSSS", "GSSSSSSS", "SSSSSSSS"],
              pal={"S": SKIN_OLD, "G": "a8a8a8", "f": "6a4426", "O": "cfe0e8", "n": "c08a68", "W": "e0e0e0"})
    robe = "6b5038"
    body = P(robe, noise=.08, pattern=robe_pattern(robe, "4a3626", belt="8a6a48", belt_rows=(8, 9), clouds="7a5e44"))
    arm = P(robe, noise=.08, bottom=SKIN_OLD, bottom_h=3)
    leg = P("8a7458", bottom="3a2e24", bottom_h=2)
    frame(m, hd, body, arm, leg, P(robe, noise=.08, bottom="9a8466", bottom_h=2, pattern=lambda f, x, y, w, h: hexc("7c6046") if (x * 7 + y * 3) % 17 == 0 else None))
    m.cube("head", (-4.5, 31, -4.5), (9, 2, 9), P("141414"))                      # mũ nho sĩ
    m.cube("head", (-3.5, 33, -3.5), (7, 4, 7), P("1a1a1a", art={"north": [".......", ".......", "...g...", "......."]}, pal={"g": "3a3a3a"}))
    scroll = P("dcc48e", noise=.06, pattern=lambda f, x, y, w, h: hexc("6a4426") if f in ("east", "west") and 0 < x < w - 1 and 0 < y < h - 1 else None)
    for i, y in enumerate((14, 17.5, 21)):                                        # bó cuộn sách sau lưng
        m.cube("body", (-5, y, 2), (10, 3, 3), scroll)
    m.cube("body", (-0.5, 13, 1.8), (1, 12, 1), P("5a3a20"))                      # dây buộc
    m.cube("right_arm", (-7, 10.5, -6), (2, 2, 5), scroll)                       # cuộn sách trên tay
    m.build()
    human_anims("npc_hua_co")


# ============================================================================ SƯ PHỤ
def su_phu():
    m = Model("npc_su_phu", 128)
    hd = head(["KKKKKKKK", "KSSSSSSK", "SKKSSKKS", "SWKSSKWS", "SSSnnSSS", "SSSSSSSS", "SSSmmSSS", "SSSSSSSS"], "141418",
              pal={"n": "c98a64", "m": "a8604c"})
    robe = "eef4fa"
    body = P(robe, noise=.05, pattern=robe_pattern(robe, "7fb6e2", belt="6aa4d8", belt_rows=(7, 8), clouds="cfe3f3"))
    arm = P(robe, noise=.05, bottom=SKIN, bottom_h=3,
            pattern=lambda f, x, y, w, h: hexc("7fb6e2") if y in (h - 5, h - 4) else (hexc("cfe3f3") if (x + y * 3) % 7 == 0 else None))
    leg = P("eef4fa", bottom="2a2a30", bottom_h=2)
    frame(m, hd, body, arm, leg, P(robe, noise=.05, bottom="7fb6e2", bottom_h=1, pattern=robe_pattern(robe, robe, collar=False, clouds="cfe3f3")))
    m.cube("head", (-2, 32, -1), (4, 2, 3), P("141418"))                          # búi tóc
    m.cube("head", (-1.5, 33, -2), (3, 2, 2), P("2ea86a", noise=.1))              # ngọc quan
    piv = [0, 22, 3.5]
    m.cube("body", (-0.5, 10, 2.6), (1, 20, 1), P("c8d0d8", noise=.04), rot=[0, 0, 35], pivot=piv)   # kiếm sau lưng
    m.cube("body", (-2, 29, 2.4), (4, 1, 2), P("8a9098"), rot=[0, 0, 35], pivot=piv)
    m.cube("body", (-0.5, 30, 2.6), (1, 5, 1), P("4a3a2a"), rot=[0, 0, 35], pivot=piv)
    m.build()
    human_anims("npc_su_phu")


# ============================================================================ HẠNG CHIÊU
def hang_chieu():
    m = Model("npc_hang_chieu", 128)
    hd = head(["KKKKKKKK", "KKSKKSKK", "SKKSSKKS", "SWKSSKWS", "SSSnnSSS", "SSSSSSSS", "SSSSmmmS", "SSSSSSSS"], "141418",
              pal={"n": "c98a64", "m": "8a4a3c"})
    red, blk, gold = "9c1c2c", "1e1418", "d8a830"

    def chest(f, x, y, w, h):
        if f == "north" and h >= 10 and y < 7 and (x == y or x == w - 1 - y): return hexc(gold)
        if f in ("north", "south", "east", "west") and h >= 10 and y in (7, 8): return hexc(blk)
        if f in ("north", "south") and h >= 10 and y == 9 and x % 3 == 1: return hexc(gold)
        return None
    body = P(red, noise=.07, pattern=chest)
    arm = P(red, noise=.07, bottom=SKIN, bottom_h=3, pattern=lambda f, x, y, w, h: hexc(blk) if y == 2 else None)
    leg = P("5a1418", bottom="3a0e10", bottom_h=4)
    frame(m, hd, body, arm, leg, P(red, noise=.07, bottom=gold, bottom_h=1,
          pattern=lambda f, x, y, w, h: hexc(blk) if y == h - 2 or (f == "north" and x in (4, 5)) else None), skirt=(10, 9, 6))
    m.cube("head", (-4.5, 29, -4.5), (9, 2, 9), P("c41e2a", noise=.05))           # khăn đỏ
    m.cube("head", (-2, 25, 4.2), (1, 5, 1), P("c41e2a")); m.cube("head", (1, 25.5, 4.2), (1, 4, 1), P("c41e2a"))
    for x, z, h in ((-4, -4, 2), (-1, -4, 3), (2, -4, 2), (-3, 0, 2), (1, 1, 3)):  # tóc dựng
        m.cube("head", (x, 32, z), (2, h, 2), P("141418"))
    for sx in (-1, 1):                                                            # vòng tay vàng
        bn = "right_arm" if sx < 0 else "left_arm"
        x0 = -8.5 if sx < 0 else 3.5
        m.cube(bn, (x0, 15, -2.5), (5, 4, 5), P(gold, noise=.06, pattern=lambda f, x, y, w, h: hexc("a87818") if y == 0 or y == h - 1 else None))
    for sgn in (-1, 1):                                                           # song đao chéo sau lưng
        piv = [0, 22, 3.5]
        m.cube("body", (-1.5, 6, 2.6), (3, 18, 1), P("c4c8cc", noise=.05, pattern=lambda f, x, y, w, h: hexc("8a8e92") if x == 0 else None), rot=[0, 0, 30 * sgn], pivot=piv)
        m.cube("body", (-2.5, 24, 2.4), (5, 1, 2), P("8a7a5a"), rot=[0, 0, 30 * sgn], pivot=piv)
        m.cube("body", (-0.5, 25, 2.6), (1, 6, 1), P("6a2018"), rot=[0, 0, 30 * sgn], pivot=piv)
    m.build()
    human_anims("npc_hang_chieu")


# ============================================================================ TÔNG CHỦ THANH VÂN
def tong_chu():
    m = Model("npc_tong_chu_thanh_van", 128)
    hd = head(["KKKKKKKK", "KSSSSSSK", "KKKSSKKK", "KWKSSKWK", "KSSnnSSK", "KSSSSSSK", "KSSmmSSK", "KSSSSSSK"], "141824",
              side=["KKKKKKKK"] * 8, pal={"K": "141824", "n": "c98a64", "m": "a8604c"})
    az, wh = "4a9ad8", "e8f2fa"
    body = P(az, noise=.05, pattern=robe_pattern(az, wh, belt="dfeefa", belt_rows=(7, 8), clouds="9ccbef"))
    arm = P(az, noise=.05, bottom=SKIN, bottom_h=3, pattern=lambda f, x, y, w, h: hexc(wh) if y in (h - 5, h - 4) or (x + y) % 6 == 0 else None)
    leg = P(az, bottom="1a1e2c", bottom_h=2)
    frame(m, hd, body, arm, leg, P(az, noise=.05, bottom=wh, bottom_h=2, pattern=lambda f, x, y, w, h: hexc(wh) if f == "north" and x in (4, 5) else (hexc("9ccbef") if (x * 3 + y) % 9 == 0 else None)), skirt=(10, 11, 6))
    m.cube("head", (-4, 13, 4), (8, 12, 1), P("141824", alpha_fringe=2))          # tóc dài sau lưng
    cloud = ["...........", "....WW.....", "..WWWWW.W..", ".WW...WWWW.", "..WWWW.....", "..........."]
    m.cube("body", (-5, 2, 2.4), (10, 21, 1), P(az, noise=.04, art={"south": ["." * 10] * 9 + [r[:10] for r in cloud] + ["." * 10] * 6}, pal={"W": wh}))  # áo choàng
    for sx in (-1, 1):                                                            # vai áo phồng
        bn = "right_arm" if sx < 0 else "left_arm"
        x0 = -9 if sx < 0 else 3
        m.cube(bn, (x0, 19, -3), (6, 5, 6), P(az, noise=.05, bottom=wh, bottom_h=1, pattern=lambda f, x, y, w, h: hexc("9ccbef") if (x + y) % 4 == 0 else None))
    piv = [3.5, 12, -3]
    m.cube("body", (3, 3, -3.5), (1, 15, 2), P("8fdcb0", noise=.06), rot=[0, 0, -20], pivot=piv)   # kiếm ngọc bên hông
    m.cube("body", (2, 17, -3.8), (3, 1, 2), P("3a8a5a"), rot=[0, 0, -20], pivot=piv)
    m.build()
    human_anims("npc_tong_chu_thanh_van")


# ============================================================================ MẶC ĐỊNH (NPC phụ chưa có ảnh)
def default_npc():
    m = Model("npc_default", 128)
    hd = head(["HHHHHHHH", "HSSSSSSH", "SHHSSHHS", "SWKSSKWS", "SSSnnSSS", "SSSSSSSS", "SSSmmSSS", "SSSSSSSS"], "4a3424",
              pal={"n": "c98a64", "m": "a8604c"})
    robe = "8a7a5a"
    frame(m, hd, P(robe, noise=.07, pattern=robe_pattern(robe, "6a5a3e", belt="5a3a20", belt_rows=(8, 9))),
          P(robe, noise=.07, bottom=SKIN, bottom_h=3), P("5a4a3a", bottom="2a2018", bottom_h=2))
    m.build()
    human_anims("npc_default")




def da_khuyet():
    """Dạ Khuyết theo ảnh concept: áo choàng dài xanh đen + vết nứt tím, đai bạc ngọc tím, vai nhiều lớp,
    cổ áo cao, tóc trắng dài, mắt tím, vầng trăng khuyết lớn sau đầu có tua treo, lưỡi hái trăng.
    GIỮ tên xương (hoạt ảnh + BossLook dùng): right_leg left_leg body cloak tatter_1..4 right_arm left_arm crescent
    crescent_tip_r hood head halo wing ph2_spikes ph3_halo."""
    m = Model("da_khuyet", 256, bounds=(6, 6), off=2.2)
    NAVY, NAVY2, BLK, SIL, VIO, PALE, HAIR, HAIR2 = "232a58", "1a1f44", "0d0c18", "b4b4cc", "a050ff", "ece6f2", "ebe7f2", "c8c0dc"
    VI = hexc(VIO)

    class GP(Paint):
        """Paint có phát sáng cho pixel tím (vết nứt / ngọc)."""
        def __call__(s, face, x, y, w, h):
            c, g = Paint.__call__(s, face, x, y, w, h)
            return c, g or (len(c) == 4 and c[3] > 0 and c[2] - c[1] > 90 and c[0] > 100)

    def vein(face, x, y, w, h, seed=0, every=9):
        """1-2 đường nứt gấp khúc liền mạch mỗi mặt."""
        if w < 4 or h < 6: return None
        for k in range(1 + (w >= 10)):
            base = (seed * 5 + k * (w // 2) + 2) % w
            off = [0, 1, 1, 0, -1, -1, 0, 1, 2, 1, 0, -1][(y + seed + k * 3) % 12]
            if x == (base + off) % w and (y + seed * 3 + k * 5) % every != 0: return VI
        return None

    def robe(front_open=False, seed=0, trim_bottom=None):
        def pat(face, x, y, w, h):
            if front_open and face == "north" and w >= 10:
                mid = w // 2
                if x in (mid - 1, mid): return hexc(BLK)
                if x in (mid - 2, mid + 1): return hexc(SIL)
            v = vein(face, x, y, w, h, seed)
            if v: return v
            if (x * 7 + y * 3 + seed) % 17 == 0: return hexc(NAVY2)
            return None
        return GP(NAVY, noise=.05, pattern=pat, bottom=trim_bottom, bottom_h=1 if trim_bottom else 0)

    # ---- chân (phần lớn khuất sau vạt áo)
    for nm, px, x0 in (("right_leg", -2.5, -5), ("left_leg", 2.5, 0)):
        m.bone(nm, None, (px, 12, 0))
        m.cube(nm, (x0, 0, -2.5), (5, 12, 5), Paint(BLK, noise=.05, bottom="3a3a52", bottom_h=2,
                                                       pattern=lambda f, x, y, w, h: hexc(SIL) if y in (8, 9) and x % 2 == 0 else None))
    # ---- thân + vạt áo
    m.bone("body", None, (0, 12, 0))
    m.cube("body", (-6, 12, -3), (12, 24, 6), robe(front_open=True, seed=1))
    m.cube("body", (-7, 0, -4), (14, 13, 8), GP(NAVY, noise=.06, alpha_fringe=3,
           pattern=lambda f, x, y, w, h: hexc(BLK) if f == "north" and x in (6, 7) else (hexc(SIL) if f == "north" and x in (5, 8) else vein(f, x, y, w, h, 4))))
    m.cube("body", (-6.5, 20, -3.5), (13, 3, 7), GP(SIL, noise=.03,                                   # đai bạc + ngọc tím
           art={"north": [".....ppp.....", "....pVVVp....", ".....ppp....."]}, pal={"p": "6a6a80", "V": VIO}))
    for x in (-3, 2):                                                                               # dải treo đai
        m.cube("body", (x, 13, -3.6), (1, 7, 1), Paint(SIL)); m.cube("body", (x - .5, 12, -3.7), (2, 2, 1), GP(VIO))
    m.cube("body", (-4, 33, -3.6), (8, 4, 7), Paint(BLK, noise=.04, bottom=SIL, bottom_h=1))           # cổ áo cao
    m.cube("body", (-2, 30, -3.4), (4, 3, 1), Paint("101020"))                                      # khoảng tối trong cổ
    # ---- áo choàng sau + tua rách
    m.bone("cloak", "body", (0, 36, 3))
    emb = ["." * 14] * 12 + ["......SS......", ".....SVVS.....", "....SVVVVS....", ".....SVVS.....", "......SS......", "......SS......"] + ["." * 14] * 8
    m.cube("cloak", (-7, 10, 3.2), (14, 26, 1), GP(NAVY, noise=.05, art={"south": emb}, pal={"S": SIL, "V": VIO},
           pattern=lambda f, x, y, w, h: hexc(SIL) if f == "south" and x in (1, 12) and y > 4 else vein(f, x, y, w, h, 7)))
    for i, x in enumerate((-9, -3, 3, 9)):
        m.bone(f"tatter_{i + 1}", "cloak", (x, 12, 4))
        m.cube(f"tatter_{i + 1}", (x - 1.5, 0, 3.4), (3, 12, 1), GP(NAVY, noise=.06, alpha_fringe=3, bottom=VIO, bottom_h=2))
    # ---- tay áo rộng + vai giáp nhiều lớp
    for nm, px, x0, sx in (("right_arm", -6.5, -10, -1), ("left_arm", 6.5, 4, 1)):
        m.bone(nm, "body", (px, 35, 0))
        m.cube(nm, (x0, 14, -3), (6, 22, 6), robe(seed=3 + sx))
        m.cube(nm, (x0 - .5, 14, -3.5), (7, 3, 7), Paint(BLK, noise=.04, bottom=SIL, bottom_h=1))      # cổ tay áo
        m.cube(nm, (x0 + .5, 11, -2.5), (5, 3, 5), Paint(PALE, noise=.03))                             # bàn tay
        m.cube(nm, (x0 - 1, 31, -4), (8, 5, 8), Paint(BLK, noise=.05, bottom=SIL, bottom_h=1,          # vai lớp 1
               pattern=lambda f, x, y, w, h: VI if f == "up" and (x + y) % 5 == 0 else None))
        m.cube(nm, (x0 - .5, 27, -3.5), (7, 4, 7), robe(seed=9, trim_bottom=SIL))                      # vai lớp 2
        m.cube(nm, (x0 + (0 if sx < 0 else 4), 36, -2), (2, 3, 4), Paint(SIL))                         # gai vai
    # ---- lưỡi hái trăng
    m.bone("crescent", "right_arm", (-6.5, 19, -2))
    m.cube("crescent", (-8, 2, -3), (2, 40, 2), GP("15121f", pattern=lambda f, x, y, w, h: VI if y % 8 == 0 else None))
    for k in range(13):                                                                             # lưỡi cong ở đỉnh
        a = math.radians(90 + k * 14)
        r = 11 - abs(k - 6) * .35
        cx, cy = -7 + math.cos(a) * r, 41 + math.sin(a) * r - 10
        m.cube("crescent", (cx - 1.5, cy - 2, -3.5), (3, 4, 3), GP("d6d6ee" if k % 2 else "9a9ac0", noise=.03,
               pattern=lambda f, x, y, w, h: VI if f in ("north", "south") and x == 1 else None))
    m.bone("crescent_tip_r", "crescent", (-8, 56, 0))
    m.cube("crescent_tip_r", (-8, 42, -3), (2, 4, 2), GP(VIO))
    # ---- tóc trắng dài
    m.bone("hood", "body", (0, 36, 0))
    hairp = lambda fr: Paint(HAIR, noise=.04, alpha_fringe=fr, pattern=lambda f, x, y, w, h: hexc(HAIR2) if x % 3 == 0 else None)
    m.cube("hood", (-5.5, 14, 3.6), (11, 26, 2), hairp(3))                                          # tóc sau lưng
    m.cube("hood", (-7, 24, -3), (2, 13, 3), hairp(2)); m.cube("hood", (5, 24, -3), (2, 13, 3), hairp(2))   # tóc mai hai bên (ngoài mặt)
    # ---- đầu
    m.bone("head", "body", (0, 36, 0))
    face = ["HHHHHHHHHH",
            "HHHHHHHHHH",
            "HHHhHHHhHH",
            "HHSSSSSSHH",
            "HSKVVSVVKH",
            "HSSSSSSSSH",
            "HSSSSSSSSH",
            "HSSSmmSSSH",
            "HSSSSSSSSH",
            "HHSSSSSSHH"]
    m.cube("head", (-5, 36, -5), (10, 10, 10), GP(HAIR, noise=.03,
           art={"north": face, "east": ["HHHHHHHHHH"] * 10, "west": ["HHHHHHHHHH"] * 10, "south": ["HHHHHHHHHH"] * 10, "up": ["HHHHHHHHHH"] * 10},
           pal={"H": HAIR, "h": HAIR2, "S": PALE, "K": "2a1a40", "V": "b46cff", "m": "b49cc0"}))
    m.cube("head", (-5.5, 44, -5.5), (11, 3, 11), hairp(0))                                         # mái tóc phồng
    # vầng trăng khuyết lớn sau đầu (luôn hiện) + tua treo
    for k in range(30):
        deg = -60 + k * 9.5
        if 92 < deg < 128: continue                                                                # khe trăng khuyết
        a = math.radians(deg)
        th = 3 + 1.5 * math.sin(math.radians((deg + 60) * 180 / 280))
        r = 13
        cx, cy = math.cos(a) * r, 44 + math.sin(a) * r
        m.cube("head", (cx - th / 2, cy - th / 2, 5.5), (round(th), round(th), 2), GP("17132a" if k % 6 else VIO, noise=.05,
               pattern=lambda f, x, y, w, h: VI if f == "north" and (x + y) % 3 == 0 else None))
    for x, l in ((-12, 7), (12, 7), (-7, 5), (7, 5)):
        m.cube("head", (x - .5, 31 - l + 7, 6), (1, l, 1), Paint(SIL)); m.cube("head", (x - 1, 30 - l + 7, 5.5), (2, 2, 2), GP(VIO))
    # pha 2+: quầng trăng sáng
    m.bone("halo", "body", (0, 55, 0))
    for k in range(24):
        a = math.radians(k * 15)
        m.cube("halo", (math.cos(a) * 16 - 1, 50 + math.sin(a) * 16 - 1, 7.5), (2, 2, 1), GP("d0c0ff"))
    # pha 3: cánh hư không
    m.bone("wing", "body", (3, 32, 3))
    for r, l in ((-30, 26), (-55, 22), (30, 26), (55, 22)):
        x0 = 3 if r > 0 else -5
        m.cube("wing", (x0, 30, 4), (2, l, 1), GP(VIO, noise=.1), rot=[-20, 0, r], pivot=[x0 + 1, 30, 4])
    m.bone("ph2_spikes", "body", (0, 12, 2.5))
    for x, h, r in ((-4, 8, -20), (-1, 11, -6), (2, 10, 8), (5, 7, 22), (-6, 6, -30), (0, 6, 0), (7, 5, 32)):
        m.cube("ph2_spikes", (x - 1, 30, 3), (2, h, 2), GP("7a3ad8", noise=.08), rot=[-25, 0, r], pivot=[x, 30, 4])
    m.bone("ph3_halo", "head", (0, 47, 0))
    for k in range(8):
        a = math.radians(k * 45)
        m.cube("ph3_halo", (math.cos(a) * 6 - .5, 47, math.sin(a) * 6 - .5), (1, 4, 1), GP("e0d0ff"))
    m.build()


# ============================================================================ NHÂN VẬT PHỤ (ảnh concept đợt 2)
def hoang_toc():
    m = Model("npc_hoang_toc_sot", 128)
    hd = head(["HHHHHHHH", "HHSSSSHH", "HSSSSSSH", "HWKSSKWH", "HSSSSSSH", "HSSmmSSH", "HSSSSSSH", "HSSSSSSH"], "3a2414",
              side=["HHHHHHHH"] * 8, pal={"m": "b0705c"})
    gold, org, dk = "e8b020", "f08c1c", "b07810"
    chk = lambda f, x, y, w, h: hexc(org) if (x + y) % 2 == 0 and (x * 3 + y) % 5 < 2 else (hexc(dk) if (x * 7 + y * 5) % 13 == 0 else None)
    frame(m, hd, P(gold, noise=.08, pattern=lambda f, x, y, w, h: hexc(org) if f in ("north", "south") and h >= 10 and y in (7, 8) else chk(f, x, y, w, h)),
          P(gold, noise=.08, bottom=SKIN, bottom_h=3, pattern=chk), P("3a2418", bottom="241610", bottom_h=3),
          P(gold, noise=.1, alpha_fringe=3, pattern=chk), skirt=(10, 10, 6))
    m.cube("head", (-4.5, 13, 4), (9, 14, 1), P("3a2414", alpha_fringe=3))       # tóc dài rối
    for x in (-6, 5): m.cube("head", (x, 18, -1), (1, 8, 2), P("3a2414", alpha_fringe=2))
    crown = P(gold, noise=.05, pattern=lambda f, x, y, w, h: hexc("2a9a5a") if y == 1 and x % 3 == 1 else (hexc("b02a5a") if y == 1 and x % 5 == 3 else None))
    m.cube("left_arm", (5, 9, -6), (5, 2, 5), crown)                            # vương miện rơi trên tay
    for x, z in ((5, -6), (9, -6), (5, -2), (9, -2), (7, -6)): m.cube("left_arm", (x, 11, z), (1, 2, 1), P(gold))
    m.build(); human_anims("npc_hoang_toc_sot")


def kiem_si():
    m = Model("npc_kiem_si", 128)
    hd = head(["HHHHHHHH", "HHHSHHHH", "HSHSSHSH", "SWKSSKWS", "SSSSSSSS", "SSSSSSSS", "SSSmmSSS", "SSSSSSSS"], "2a1e16",
              pal={"m": "9a6050"})
    wh, gy = "e4e2dc", "a8a6a0"
    tat = lambda f, x, y, w, h: hexc(gy) if (x * 5 + y * 3) % 7 == 0 else None
    frame(m, hd, P(wh, noise=.08, pattern=lambda f, x, y, w, h: hexc(gy) if f == "north" and h >= 10 and y < 8 and abs(x - y * 0.6 - 2) < 1 else tat(f, x, y, w, h)),
          P(wh, noise=.08, bottom=SKIN, bottom_h=2, pattern=tat), P("3a2e24", bottom="241c14", bottom_h=3),
          P(wh, noise=.1, alpha_fringe=4, pattern=tat), skirt=(10, 10, 6))
    for x, z, h in ((-4, -4, 2), (-1, -4, 3), (2, -3, 2), (-3, 0, 3), (1, 1, 2), (3, 2, 2)):
        m.cube("head", (x, 32, z), (2, h, 2), P("2a1e16"))
    blades = (("c4c8cc", "2a8a5a", 30), ("3a3a50", "c8a030", -30), ("2a6a4a", "2a6a4a", 15), ("c4c8cc", "5a3a20", -15), ("1a1a2a", "8a2020", 45), ("c8a030", "1a1a2a", -45))
    for i, (bc, hc, r) in enumerate(blades):                                   # bó kiếm sau lưng
        piv = [0, 20, 3.5]
        m.cube("body", (-0.5, 4, 2.4 + (i % 2) * .4), (2, 22, 1), P(bc, noise=.05), rot=[0, 0, r], pivot=piv)
        m.cube("body", (-0.5, 26, 2.4 + (i % 2) * .4), (2, 5, 1), P(hc), rot=[0, 0, r], pivot=piv)
    m.cube("body", (-2, 18, 2.6), (4, 4, 2), P("7a5232"))                       # nút buộc da
    m.build(); human_anims("npc_kiem_si")


def lao_gia():
    m = Model("npc_lao_gia_bi_an", 128)
    hd = head(["HHHHHHHH", "HKKKKKKH", "HKYKKYKH", "HKKKKKKH", "HWWKKWWH", "HWWWWWWH", "HWWWWWWH", "HWWWWWWH"], "3c4048",
              side=["HHHHHHHH"] * 8, pal={"K": "060608", "Y": "ffe070", "W": "e8e8ec"})
    hd.glow = {"Y"}
    cl = "3c4048"
    pat = lambda f, x, y, w, h: hexc("5a6a7a") if (x * 3 + y * 5) % 11 == 0 else None
    frame(m, hd, P(cl, noise=.08, pattern=pat), P(cl, noise=.08, bottom=SKIN_OLD, bottom_h=2, pattern=pat), P("2a2a2e", bottom="1a1a1c", bottom_h=3),
          P(cl, noise=.1, alpha_fringe=4, pattern=pat), skirt=(10, 11, 6))
    m.cube("head", (-4.5, 24, -4.5), (9, 9, 9), P(cl, noise=.06, art={"north": ["HHHHHHHHH"] + ["H       H"] * 8}, pal={"H": cl}))  # mũ trùm
    m.cube("head", (-2, 18, -4.6), (4, 6, 1), P("e8e8ec", alpha_fringe=1))      # râu bạc
    beads = P("7a5232", pattern=lambda f, x, y, w, h: hexc("3a8a5a") if x % 4 == 2 else (hexc("c8b070") if x % 4 == 0 else None))
    m.cube("body", (-4.5, 20, -2.6), (9, 1, 1), beads); m.cube("body", (-3, 17, -2.7), (6, 1, 1), beads)
    m.cube("body", (-1, 15, -2.8), (2, 2, 1), P("3a8a5a"))
    m.build(); human_anims("npc_lao_gia_bi_an")


def hai_tuong():
    m = Model("npc_hai_tuong", 128)
    hd = head(["KKKKKKKK", "KSSSSSSK", "SKKSSKKS", "SWKSSKWS", "SSSnnSSS", "SSSSSSSS", "SSSmmSSS", "SSSSSSSS"], "101820",
              pal={"S": "c0845a", "n": "a06a48", "m": "8a5040", "K": "101820"})
    tl, tl2 = "2a9a9a", "1e7a80"
    scale = lambda f, x, y, w, h: hexc(tl2) if (x + y * 2) % 3 == 0 else (hexc("46c0c0") if (x * 2 + y) % 5 == 0 else None)
    frame(m, hd, P(tl, noise=.05, pattern=scale), P(tl, noise=.05, bottom="c0845a", bottom_h=2, pattern=scale), P(tl, noise=.05, pattern=scale),
          P(tl, noise=.05, pattern=scale), skirt=(10, 5, 6))
    m.cube("head", (-2, 32, -1), (4, 2, 3), P("101820")); m.cube("head", (-1, 34, -1.5), (2, 1, 2), P("f0c8a0"))
    m.cube("head", (1.5, 33, -1), (2, 2, 1), P("e86a6a"))                       # san hô
    wave = lambda f, x, y, w, h: hexc("a8e8f0") if f == "south" and ((y % 5 == 0 and (x + y // 5) % 4 < 2) or (y % 5 == 1 and (x + y // 5) % 4 == 1)) else (hexc("2a4ab0") if f == "south" and y % 5 == 2 else None)
    m.cube("body", (-5, 4, 2.4), (10, 19, 1), P("2a6ad0", noise=.04, pattern=wave))   # áo choàng sóng
    m.cube("right_arm", (-6.5, 0, -4), (1, 34, 1), P("2a9a9a"))                 # đinh ba
    for x in (-8.5, -6.5, -4.5): m.cube("right_arm", (x, 34, -4), (1, 5, 1), P("46c0c0"))
    m.cube("right_arm", (-8.5, 33, -4), (5, 1, 1), P("2a9a9a"))
    m.build(); human_anims("npc_hai_tuong")


def dau_gia_su():
    m = Model("npc_dau_gia_su", 128)
    hd = head(["KKKKKKKK", "KKSSSSKK", "KSSSSSSK", "KSKSSKSK", "KSSSSSSK", "KSSSSSSK", "KSSmmSSK", "KSSSSSSK"], "0e0e10",
              side=["KKKKKKKK"] * 8, pal={"K": "0e0e10", "m": "b0807a", "S": "f0d0b0"})
    pu, gd = "4a1a6a", "d8b040"
    emb = lambda f, x, y, w, h: hexc(gd) if (x * 7 + y * 3) % 13 in (0, 1) and (x + y) % 2 == 0 else None
    body = P(pu, noise=.05, pattern=lambda f, x, y, w, h: hexc(gd) if f == "north" and h >= 10 and x in (3, 4) and y < 6 else emb(f, x, y, w, h))
    frame(m, hd, body, P(pu, noise=.05, bottom="f0d0b0", bottom_h=2, pattern=lambda f, x, y, w, h: hexc(gd) if y == h - 4 else emb(f, x, y, w, h)),
          P(pu, bottom="1a0a20", bottom_h=1), P(pu, noise=.05, bottom=gd, bottom_h=1, pattern=lambda f, x, y, w, h: hexc(gd) if f == "north" and x in (4, 5) else emb(f, x, y, w, h)), skirt=(10, 11, 7))
    m.cube("head", (-4, 12, 4), (8, 14, 1), P("0e0e10", alpha_fringe=2))
    m.cube("head", (-1.5, 32, -1.5), (3, 3, 3), P("0e0e10")); m.cube("head", (-4, 33, -0.5), (8, 1, 1), P(gd))
    fan = P("e0d8c0", pattern=lambda f, x, y, w, h: hexc("6a4426") if x % 2 == 0 else None)
    m.cube("left_arm", (3, 10, -8), (8, 6, 1), fan, rot=[0, 0, 15], pivot=[6, 12, -7])   # quạt
    m.cube("right_arm", (-9, 10, -6), (3, 2, 3), P("7a4a24")); m.cube("right_arm", (-8, 10, -3), (1, 1, 3), P("6a3a1a"))  # búa đấu giá
    m.build(); human_anims("npc_dau_gia_su")


def chu_cho_den():
    m = Model("npc_chu_cho_den", 128)
    hd = head(["HHHHHHHH", "HHSSSSHH", "HKSSSSKH", "HWKSSKWH", "HMMMMMMH", "HMMMMMMH", "HMMMMMMH", "HHMMMMHH"], "141416",
              side=["HHHHHHHH"] * 8, pal={"M": "1e1e22", "S": "c09070"})
    bk = "1a1a1e"
    st = lambda f, x, y, w, h: hexc("2c2c32") if y % 3 == 0 else None
    frame(m, hd, P(bk, noise=.05, pattern=lambda f, x, y, w, h: hexc("4a3020") if h >= 10 and y in (7, 8) else (hexc("3a3a42") if f == "north" and h >= 10 and y < 7 and abs(x - 3.5) - .5 == y else None)),
          P(bk, noise=.05, bottom="c09070", bottom_h=2, pattern=lambda f, x, y, w, h: hexc("d8a830") if y == h - 3 else st(f, x, y, w, h)),
          P(bk, bottom="0e0e10", bottom_h=4, pattern=st), P(bk, noise=.06, alpha_fringe=2, pattern=st), skirt=(10, 10, 6))
    m.cube("head", (-4.5, 24, -4.5), (9, 9, 9), P(bk, noise=.05, art={"north": ["HHHHHHHHH"] + ["H       H"] * 8}, pal={"H": bk}))  # mũ trùm
    pouch = lambda c: P(c, noise=.06, pattern=lambda f, x, y, w, h: hexc("2a1a10") if y == 0 else None)
    for x, c in ((-4.5, "5a3a24"), (-1, "6a4a2a"), (2.5, "5a3a24")):
        m.cube("body", (x, 13, -3.5), (2, 3, 2), pouch(c))
    for x, c in ((-4, "5a4030"), (-1.5, "8a6a40"), (1, "a89a70"), (3, "9a9aa0")):
        m.cube("body", (x, 12, 2), (2, 3, 2), pouch(c))
    m.cube("body", (2.5, 9, -2.6), (2, 4, 1), P("b09060"))                       # cuộn dây thừng
    m.build(); human_anims("npc_chu_cho_den")


if __name__ == "__main__":
    for fn in (mo_dao, hua_co, su_phu, hang_chieu, tong_chu, default_npc, hoang_toc, kiem_si, lao_gia, hai_tuong, dau_gia_su, chu_cho_den):
        fn()
