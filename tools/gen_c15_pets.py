#!/usr/bin/env python3
"""C15: linh thú 3 giai đoạn dựng theo ảnh mẫu (docs: ảnh concept người dùng gửi 2026-11-07).
  *_g0 ấu thể, *_g1 thiếu niên, *_g2 trưởng thành. Gấu: thiet_boi_hung_con_g0/_g1 -> thiet_boi_hung_g2.
  Mọi kích thước là số nguyên (engine làm tròn kích thước). Đầu hướng -z.
Chạy: python tools/gen_c15_pets.py  (từ D:\\mc_mod)"""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from gen_c7_models import quad_legs, cfg


def skin(b, g=None):
    """Lông/da không chấm phát sáng (engine mặc định chấm tím cho skin)."""
    m = Mat("skin", b, None, None, .05)
    m.glow = g
    m.lo = mul(b, .86)  # vân lông nhạt, không lấm tấm
    return m


# ============================================================================ THIẾT BỐI HÙNG
def bear_mats():
    return dict(FR=skin((64, 64, 70)), MZ=skin((150, 112, 82)), NS=skin((20, 20, 24)), MN=skin((26, 26, 30)),
                PL=plate((138, 144, 154), (92, 96, 108)), ST=cloth((46, 34, 28)), IV=skin((236, 228, 200)),
                GL=glow((255, 160, 40)), PW=skin((36, 34, 38)))


AMBER = (255, 190, 60)


def bear_g0():
    M = bear_mats(); m = Model("thiet_boi_hung_con_g0", 128, (2, 2), 0.6)
    m.bone("body", None, (0, 4, 0))
    m.cube("body", (-5, 4, -6), (10, 8, 13), M["FR"])
    m.cube("body", (-4, 12, -3), (8, 1, 7), M["PL"])                 # tấm giáp nhỏ trên lưng
    m.cube("body", (-6, 5, 0), (12, 7, 1), M["ST"])                  # dây đai
    m.bone("head", "body", (0, 11, -6))
    m.cube("head", (-5, 7, -13), (10, 9, 8), M["FR"], eyes=[(2, 4, 2, 1, AMBER), (6, 4, 2, 1, AMBER)])
    m.cube("head", (-2, 7, -15), (4, 4, 2), M["MZ"])
    m.cube("head", (-1, 10, -16), (2, 1, 1), M["NS"])
    m.cube("head", (-5, 16, -10), (2, 2, 2), M["FR"]); m.cube("head", (3, 16, -10), (2, 2, 2), M["FR"])
    q = quad_legs(m, "body", 4, (-3, 3), (-3.5, 4), 4, 4, M["FR"], M["PW"])
    return m, cfg(quad=q, legs=False, skills=dict(attack="strike", skill="roar"))


def bear_g1():
    M = bear_mats(); m = Model("thiet_boi_hung_con_g1", 128, (3, 3), 0.8)
    m.bone("body", None, (0, 6, 0))
    m.cube("body", (-6, 6, -9), (12, 10, 18), M["FR"])
    m.cube("body", (-7, 7, -11), (14, 11, 6), M["MN"])               # bờm đen quanh cổ vai
    m.cube("body", (-4, 18, -10), (8, 2, 4), M["MN"])
    m.cube("body", (-5, 16, -1), (10, 2, 8), M["PL"], rune=True)    # yên giáp phù văn
    m.cube("body", (-7, 7, 3), (14, 9, 1), M["ST"])                  # dây đai
    m.bone("head", "body", (0, 13, -9))
    m.cube("head", (-4, 9, -17), (8, 8, 7), M["FR"], eyes=[(1, 3, 2, 1, AMBER), (5, 3, 2, 1, AMBER)])
    m.cube("head", (-2, 9, -20), (4, 4, 3), M["MZ"])
    m.cube("head", (-1, 12, -21), (2, 1, 1), M["NS"])
    m.cube("head", (-4, 17, -13), (2, 2, 2), M["FR"]); m.cube("head", (2, 17, -13), (2, 2, 2), M["FR"])
    q = quad_legs(m, "body", 6, (-3.5, 3.5), (-5.5, 5.5), 5, 5, M["FR"], M["PW"])
    return m, cfg(quad=q, legs=False, skills=dict(attack="strike", skill="roar"))


def bear_g2():
    M = bear_mats(); m = Model("thiet_boi_hung_g2", 256, (4, 4), 1.2)
    m.bone("body", None, (0, 9, 0))
    m.cube("body", (-7, 9, -13), (14, 13, 26), M["FR"])
    m.cube("body", (-8, 11, -15), (16, 12, 9), M["MN"])              # bờm cổ
    for x, z, h in ((-5, -15, 8), (-2, -16, 11), (1, -16, 10), (4, -15, 8), (-4, -11, 9), (0, -12, 12), (3, -11, 9), (-1, -8, 8)):
        m.cube("body", (x, 21, z), (3, h, 3), M["MN"], rot=[-28, 0, 0], pivot=[x + 1.5, 21, z + 1.5])   # bờm gai dựng
    for sx in (-1, 1):                                                # giáp vai lớn
        x0 = 7 if sx > 0 else -10
        m.cube("body", (x0, 12, -13), (3, 11, 10), M["PL"], rune=True)
        m.cube("body", (x0 - sx, 22, -12), (3, 2, 8), M["PL"])
    m.cube("body", (-6, 22, -3), (12, 2, 13), M["PL"], rune=True)   # yên giáp
    m.cube("body", (-1, 24, -3), (2, 1, 13), M["GL"])                 # vân cam
    m.cube("body", (-1, 23, -9), (2, 1, 6), M["GL"])
    m.cube("body", (-8, 10, 4), (16, 11, 1), M["ST"])
    m.bone("head", "body", (0, 18, -15))
    m.cube("head", (-5, 13, -24), (10, 10, 9), M["FR"], eyes=[(2, 3, 2, 1, AMBER), (6, 3, 2, 1, AMBER)])
    m.cube("head", (-3, 13, -27), (6, 5, 3), M["MZ"])
    m.cube("head", (-1, 16, -28), (2, 2, 1), M["NS"])
    m.cube("head", (-5, 23, -18), (2, 3, 2), M["FR"]); m.cube("head", (3, 23, -18), (2, 3, 2), M["FR"])
    for sx in (-1, 1):                                                # ngà cong
        x = -5 if sx < 0 else 3
        m.cube("head", (x, 11, -28), (2, 4, 2), M["IV"], rot=[35, 0, 0], pivot=[x + 1, 14, -27])
        m.cube("head", (x, 9, -31), (2, 4, 2), M["IV"], rot=[-30, 0, 0], pivot=[x + 1, 11, -30])
    q = quad_legs(m, "body", 9, (-4.5, 4.5), (-9, 9), 6, 6, M["FR"], M["PW"])
    return m, cfg(quad=q, legs=False, skills=dict(attack="strike", skill="roar"))


# ============================================================================ LINH HỒ
def fox_mats():
    return dict(FR=skin((226, 230, 238)), SH=skin((176, 182, 196)), WH=skin((248, 250, 252)), NS=skin((30, 30, 40)),
                GL=glow((90, 235, 245)), CR=crystal((70, 190, 220), (150, 245, 255)))


DARK = (30, 30, 46)
CYAN = (90, 235, 245)


def fox_tails(m, M, n, pivot, ln, spread, lift, tip=3):
    """Quạt đuôi: mỗi đuôi 1 xương xoay quanh trục z (xòe), cube bên trong ngóc lên (xoay x)."""
    sway = []
    px, py, pz = pivot
    for i in range(n):
        roll = -spread / 2 + spread * i / max(1, n - 1)
        nm = f"tail_{i + 1}"
        m.bone(nm, "body", pivot, rot=[0, 0, roll])
        rot = [lift, 0, 0]
        m.cube(nm, (px - 1.5, py - 1.5, pz), (3, 3, ln), M["FR"], rot=rot, pivot=list(pivot))
        m.cube(nm, (px - 1.5, py - 1.5, pz + ln), (3, 3, tip), M["GL"], rot=rot, pivot=list(pivot))
        sway.append((nm, 1, 6 + i % 3 * 2, 1, i % 3))
    return sway


def fox_g0():
    M = fox_mats(); m = Model("linh_ho_g0", 128, (2, 2), 0.5)
    m.bone("body", None, (0, 3, 0))
    m.cube("body", (-3, 3, -4), (6, 5, 8), M["FR"])
    m.bone("head", "body", (0, 7, -4))
    m.cube("head", (-4, 5, -10), (8, 7, 6), M["FR"], eyes=[(1, 3, 2, 2, DARK), (5, 3, 2, 2, DARK)])
    m.cube("head", (-2, 5, -12), (4, 2, 2), M["WH"])
    m.cube("head", (-1, 6, -13), (2, 1, 1), M["NS"])
    m.cube("head", (-1, 9, -11), (2, 2, 1), M["CR"])
    m.cube("head", (-4, 12, -8), (2, 2, 1), M["FR"]); m.cube("head", (2, 12, -8), (2, 2, 1), M["FR"])
    sway = fox_tails(m, M, 3, (0, 6, 3), 5, 70, 50)
    q = quad_legs(m, "body", 3, (-2, 2), (-2.5, 2.5), 2, 2, M["FR"], M["SH"])
    return m, cfg(quad=q, legs=False, sway=sway)


def fox_g1():
    M = fox_mats(); m = Model("linh_ho_g1", 128, (3, 3), 0.7)
    m.bone("body", None, (0, 7, 0))
    m.cube("body", (-3, 7, -6), (6, 5, 13), M["FR"])
    m.cube("body", (-3, 6, -7), (6, 7, 3), M["WH"])                  # ngực
    for sx in (-1, 1):                                                # vân linh quang
        x = 3 if sx > 0 else -4
        m.cube("body", (x, 9, -3), (1, 1, 3), M["GL"]); m.cube("body", (x, 10, 2), (1, 1, 2), M["GL"])
    m.bone("head", "body", (0, 11, -6))
    m.cube("head", (-3, 10, -12), (6, 5, 6), M["FR"], eyes=[(1, 2, 1, 1, DARK), (4, 2, 1, 1, DARK)])
    m.cube("head", (-1, 10, -15), (2, 2, 3), M["WH"])
    m.cube("head", (-1, 11, -16), (2, 1, 1), M["NS"])
    m.cube("head", (-1, 13, -13), (2, 2, 1), M["CR"])
    m.cube("head", (-3, 15, -9), (2, 3, 1), M["FR"]); m.cube("head", (1, 15, -9), (2, 3, 1), M["FR"])
    sway = fox_tails(m, M, 5, (0, 10, 6), 8, 100, 45)
    q = quad_legs(m, "body", 7, (-1.5, 1.5), (-4.5, 5), 2, 2, M["FR"], M["SH"])
    return m, cfg(quad=q, legs=False, sway=sway)


def fox_g2():
    M = fox_mats(); m = Model("linh_ho_g2", 128, (4, 4), 0.9)
    m.bone("body", None, (0, 10, 0))
    m.cube("body", (-3, 10, -8), (6, 7, 17), M["FR"])
    m.cube("body", (-4, 8, -11), (8, 10, 5), M["WH"])                # bờm ngực xù
    m.cube("body", (-3, 18, -10), (6, 2, 3), M["WH"])
    for sx in (-1, 1):
        x = 3 if sx > 0 else -4
        for z, y, l in ((-6, 12, 3), (-2, 14, 2), (2, 12, 4), (6, 14, 2)):
            m.cube("body", (x, y, z), (1, 1, l), M["GL"])
    m.bone("head", "body", (0, 17, -9))
    m.cube("head", (-3, 16, -16), (6, 6, 7), M["FR"], eyes=[(1, 2, 1, 1, CYAN), (4, 2, 1, 1, CYAN)])
    m.cube("head", (-2, 16, -19), (4, 3, 3), M["WH"])
    m.cube("head", (-1, 18, -20), (2, 1, 1), M["NS"])
    m.cube("head", (-1, 19, -17), (2, 1, 1), M["GL"])
    m.cube("head", (-3, 22, -12), (2, 4, 2), M["FR"]); m.cube("head", (1, 22, -12), (2, 4, 2), M["FR"])
    for x, h, z in ((-2, 4, -15), (0, 6, -15), (2, 3, -15), (-1, 3, -13), (1, 4, -13)):   # vương miện pha lê
        m.cube("head", (x - .5, 22, z), (1, h, 1), M["CR"])
    sway = fox_tails(m, M, 9, (0, 15, 8), 14, 130, 50, tip=4)
    q = quad_legs(m, "body", 10, (-1.5, 1.5), (-6, 6.5), 3, 3, M["FR"], M["SH"])
    return m, cfg(quad=q, legs=False, sway=sway)


# ============================================================================ NGỌC THỐ
def rab_mats():
    return dict(FR=skin((240, 246, 242)), MT=skin((196, 236, 216)), PK=skin((255, 168, 196)), WH=skin((252, 252, 252)),
                GM=crystal((40, 190, 130), (130, 255, 200)), GO=plate((196, 186, 96), (150, 140, 60)), GL=glow((130, 255, 210)),
                MO=crystal((170, 220, 255), (230, 245, 255)), NS=skin((255, 150, 180)))


RED = (160, 40, 54)


def ears(m, M, y, z, h, w=2, d=2, ring=False, earring=False):
    for i, x in enumerate((-3, 1)):
        nm = f"ear_{i}"
        m.bone(nm, "head", (x + 1, y, z + 1))
        m.cube(nm, (x, y, z), (w, h, d), M["FR"])
        m.cube(nm, (x, y + 1, z - 1), (w, h - 2, 1), M["PK"])
        if ring: m.cube(nm, (x - 1 if i == 0 else x, y + 1, z), (w + 1, 1, d + 1), M["GO"])
        if earring and i == 1:
            m.cube(nm, (x + w, y - 1, z), (1, 2, 1), M["GO"]); m.cube(nm, (x + w, y - 3, z), (1, 2, 1), M["GM"])


def swirl(m, M, base, n, s0, R):
    """Đuôi linh quang cuộn: chuỗi khối đi lên-ra sau rồi cuộn tròn (mặt phẳng y-z)."""
    import math as _m
    m.bone("tail", "body", base)
    bx, by, bz = base
    cz, cy = bz + R + 1, by + R
    for k in range(n):
        t = k / max(1, n - 1)
        a = _m.pi + t * 1.7 * _m.pi
        r = R * (1 - .45 * t)
        z, y = cz + _m.cos(a) * r, cy - _m.sin(a) * r
        sz = max(1, round(s0 - k * (s0 - 1) / max(1, n - 1)))
        m.cube("tail", (bx - sz / 2, y - sz / 2, z - sz / 2), (sz, sz, sz), M["MT"] if k % 4 == 0 else M["GL"])


def rab_g0():
    M = rab_mats(); m = Model("ngoc_tho_g0", 128, (2, 2), 0.5)
    m.bone("body", None, (0, 2, 0))
    m.cube("body", (-3, 2, -3), (6, 5, 7), M["MT"])
    m.cube("body", (-3, 2, -4), (6, 4, 2), M["WH"])
    m.bone("head", "body", (0, 7, -3))
    m.cube("head", (-3, 6, -9), (6, 6, 6), M["FR"], eyes=[(0, 2, 2, 2, RED), (4, 2, 2, 2, RED)])
    m.cube("head", (-1, 6, -10), (2, 2, 1), M["WH"]); m.cube("head", (-1, 7, -11), (2, 1, 1), M["NS"])
    m.cube("head", (-1, 10, -10), (2, 2, 1), M["GM"])
    ears(m, M, 12, -6, 6)
    swirl(m, M, (0, 4, 3), 5, 3, 1.5)
    q = quad_legs(m, "body", 2, (-1.5, 1.5), (-2, 2.5), 2, 2, M["FR"], M["WH"])
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 10, 1, 0)])


def rab_g1():
    M = rab_mats(); m = Model("ngoc_tho_g1", 128, (3, 3), 0.6)
    m.bone("body", None, (0, 4, 0))
    m.cube("body", (-3, 4, -4), (6, 6, 10), M["MT"])
    m.cube("body", (-4, 3, -6), (8, 7, 3), M["WH"])                  # bờm ngực
    m.bone("head", "body", (0, 10, -4))
    m.cube("head", (-3, 9, -10), (6, 6, 6), M["FR"], eyes=[(0, 2, 2, 2, RED), (4, 2, 2, 2, RED)])
    m.cube("head", (-1, 9, -11), (2, 2, 1), M["WH"]); m.cube("head", (-1, 10, -12), (2, 1, 1), M["NS"])
    m.cube("head", (-1, 13, -11), (2, 2, 1), M["GM"]); m.cube("head", (-2, 13, -11), (1, 1, 1), M["GO"]); m.cube("head", (1, 13, -11), (1, 1, 1), M["GO"])
    ears(m, M, 15, -7, 9, ring=True, earring=True)
    swirl(m, M, (0, 7, 5), 8, 3, 2.5)
    q = quad_legs(m, "body", 4, (-2, 2), (-3, 3.5), 2, 2, M["FR"], M["WH"])
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 10, 1, 0), ("ear_0", 0, 5, 1, 0), ("ear_1", 0, 5, 1, 1)])


def rab_g2():
    M = rab_mats(); m = Model("ngoc_tho_g2", 128, (4, 4), 0.8)
    m.bone("body", None, (0, 6, 0))
    m.cube("body", (-4, 6, -6), (8, 7, 13), M["MT"])
    m.cube("body", (-5, 4, -9), (10, 11, 4), M["WH"])                # bờm ngực lớn
    m.cube("body", (-4, 15, -8), (8, 2, 3), M["WH"])
    for sx in (-1, 1):                                                # vân xoáy ngọc
        x = 4 if sx > 0 else -5
        m.cube("body", (x, 9, -1), (1, 3, 3), M["GL"]); m.cube("body", (x, 8, 3), (1, 2, 2), M["GL"])
    m.bone("head", "body", (0, 14, -7))
    m.cube("head", (-3, 13, -14), (7, 7, 7), M["FR"], eyes=[(1, 3, 2, 2, RED), (4, 3, 2, 2, RED)])
    m.cube("head", (-1, 13, -15), (3, 2, 1), M["WH"]); m.cube("head", (0, 14, -16), (1, 1, 1), M["NS"])
    m.cube("head", (-2, 18, -15), (5, 1, 1), M["MO"]); m.cube("head", (-2, 19, -15), (1, 1, 1), M["MO"]); m.cube("head", (2, 19, -15), (1, 1, 1), M["MO"])
    ears(m, M, 20, -10, 12, w=3, d=3, earring=True)
    swirl(m, M, (0, 9, 6), 12, 4, 4)
    q = quad_legs(m, "body", 6, (-2.5, 2.5), (-4, 4.5), 3, 3, M["FR"], M["WH"])
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 10, 1, 0), ("ear_0", 0, 5, 1, 0), ("ear_1", 0, 5, 1, 1)])


JOBS = [bear_g0, bear_g1, bear_g2, fox_g0, fox_g1, fox_g2, rab_g0, rab_g1, rab_g2]

if __name__ == "__main__":
    for d in ("geo/entity", "animations/entity", "textures/entity"): (A / d).mkdir(parents=True, exist_ok=True)
    for fn in JOBS:
        m, c = fn()
        m.build()
        geo = json.loads((A / "geo/entity" / f"{m.id}.geo.json").read_text())
        build_anims(m.id, geo, c)
        print("ok", m.id)
