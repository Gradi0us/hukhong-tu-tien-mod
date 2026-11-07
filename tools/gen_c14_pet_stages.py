#!/usr/bin/env python3
"""C14: model theo giai đoạn trưởng thành của linh thú (ấu thể = model gốc gen_c7_models).
  *_g1 = thiếu niên, *_g2 = trưởng thành. Thú lớn bằng thuộc tính SCALE; model giữ cùng "dấu chân" với model gốc
  nhưng tỉ lệ + chi tiết khác hẳn (đầu nhỏ lại, chân dài, giáp / bờm / đuôi / tinh thể).
Chạy: python tools/gen_c14_pet_stages.py  (từ D:\\mc_mod)"""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from gen_c7_models import quad_legs, cfg


# ----------------------------------------------------------------------------- NGỌC THỐ
def ngoc_tho_g1():
    """Thiếu niên: thân dài hơn, chân cao, tai dài có chóp ngọc, chùm đuôi ngọc."""
    m = Model("ngoc_tho_g1", 64, (2, 2), 0.5)
    JD = skin((200, 236, 216)); WH = skin((248, 250, 248)); PK = skin((255, 170, 200)); GM = crystal((60, 200, 150), (140, 255, 210))
    GOLD = plate((150, 160, 80), (210, 220, 130)); GL = glow((140, 255, 210))
    m.bone("body", None, (0, 5, 0))
    m.cube("body", (-2.2, 6, -4.2), (4.4, 3.5, 1.5), WH)              # bờm ngực
    m.cube("body", (-2, 4.5, -3.5), (4, 4, 7), JD)
    m.cube("body", (-1.5, 4.2, -3), (3, 1, 6), WH)
    m.cube("body", (-1, 6, 3.5), (2, 2, 2), GM)
    m.bone("head", "body", (0, 8, -3.5))
    m.cube("head", (-1.5, 7.5, -6.5), (3, 3, 3), JD, eyes=[(0, 1, 1, 1, (255, 90, 150)), (2, 1, 1, 1, (255, 90, 150))])
    for x in (-1.6, .4):
        m.cube("head", (x, 10.5, -5), (1.2, 7, 1.6), WH)
        m.cube("head", (x + .2, 11, -5.2), (.8, 6, .3), PK)
    m.cube("head", (-1.8, 10.2, -5.3), (3.6, .8, 1.6), GOLD)          # vòng ngọc trên trán/tai
    m.cube("head", (-.6, 9.8, -6.9), (1.2, 1.2, .6), GM)
    m.cube("head", (1.3, 8.5, -4.8), (.5, 1.5, .5), GOLD); m.cube("head", (1.25, 7.6, -4.85), (.6, .9, .6), GM)  # hoa tai
    m.cube("body", (-1.5, 6, 5), (3, 3, 2), GL); m.cube("body", (-1, 8.5, 5.5), (2, 2, 1.5), GL)
    q = quad_legs(m, "body", 4.5, (-1.5, 1.5), (-2.5, 2.5), 1, 1, JD, None, wb=2, db=2)
    return m, cfg(quad=q, legs=False)


def ngoc_tho_g2():
    """Nguyệt Ngọc Thố (trưởng thành): thân thon dài, bờm ngực trắng, tai dài quấn vòng ngọc, trăng khuyết pha lê trên trán,
    đuôi linh quang, chân cao mảnh."""
    m = Model("ngoc_tho_g2", 128, (3, 3), 0.6)
    JD = skin((190, 232, 210)); WH = skin((250, 252, 250)); PK = skin((255, 176, 206)); GM = crystal((60, 200, 150), (150, 255, 215))
    MO = crystal((170, 200, 255), (220, 235, 255)); GL = glow((150, 255, 210))
    m.bone("body", None, (0, 6, 0))
    m.cube("body", (-2.5, 6, -4.5), (5, 5, 9), JD)
    m.cube("body", (-2, 5.6, -4), (4, 1, 8), WH)
    m.cube("body", (-2.2, 7, -5.5), (4.4, 4, 2), WH)               # bờm ngực
    m.cube("body", (-1.5, 9.5, -4.6), (3, 2, 1), WH)
    m.cube("body", (-2.8, 5.5, -6), (5.6, 6, 2.5), WH)               # bờm ngực lớn
    m.bone("tail", "body", (0, 9, 4.5))
    import math as _m
    for k in range(9):                                              # đuôi linh quang xoáy (chuỗi khối)
        a = k * 0.75
        x, y, z = _m.sin(a) * (1 + k * .35), 9 + k * .9, 5 + k * 1.1 - (k > 5) * (k - 5) * 1.4
        sz = 2.6 - k * .18
        m.cube("tail", (x - sz / 2, y - sz / 2, z - sz / 2), (sz, sz, sz), GL if k % 2 else GM)
    m.bone("head", "body", (0, 10.5, -5))
    m.cube("head", (-2, 10, -9), (4, 4, 4), JD, eyes=[(0, 1, 1, 1, (255, 80, 150)), (3, 1, 1, 1, (255, 80, 150))])
    m.cube("head", (-1, 10, -10), (2, 2, 1), WH)
    for i, x in enumerate((-2, 1)):
        nm = f"ear_{i}"
        m.bone(nm, "head", (x + .5, 14, -6.5))
        m.cube(nm, (x, 14, -7), (1, 9, 1.5), JD)
        m.cube(nm, (x + .2, 14, -7.3), (.6, 7, .4), PK)
        for y in (16, 19, 22):
            m.cube(nm, (x - .25, y, -7.25), (1.5, .6, 2), GM)
        if i == 1:                                                  # hoa tai ngọc
            m.cube(nm, (x + 1, 13, -6.8), (.5, 2, .5), GM); m.cube(nm, (x + .9, 11.8, -6.9), (.7, 1.2, .7), GL)
    m.cube("head", (-1.5, 14, -9.3), (3, 1, 1), MO)                # trăng khuyết
    m.cube("head", (-2, 15, -9.3), (1, 1, 1), MO); m.cube("head", (1, 15, -9.3), (1, 1, 1), MO)
    q = quad_legs(m, "body", 6, (-1.8, 1.8), (-3.5, 3.5), 1.6, 1.6, WH, WH, wb=2.4, db=3)
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 10, 1, 0), ("ear_0", 0, 6, 1, 0), ("ear_1", 0, 6, 1, 1)])


# ----------------------------------------------------------------------------- LINH HỒ
def fox(id, tails, adult):
    m = Model(id, 128, (3, 3) if not adult else (4, 4), 0.8)
    SV = skin((214, 218, 234) if not adult else (226, 230, 244)); WH = skin((252, 252, 255)); DK = skin((60, 62, 84)); PK = skin((255, 200, 220))
    GL = glow((120, 220, 255)); CR = crystal((90, 150, 230), (150, 230, 255))
    L = 7 if adult else 6.5
    m.bone("body", None, (0, L + 2, 0))
    m.cube("body", (-2.5, L, -5.5), (5, 5, 11), SV, rune=adult)
    m.cube("body", (-2, L - .5, -4.5), (4, 1, 9), WH)
    for x in (-2.6, 2.1):                                           # vân linh quang hai bên sườn
        m.cube("body", (x, L + 2, -3), (.5, 1, 4), GL); m.cube("body", (x, L + 3, 1), (.5, 1, 3), GL)
    if adult:
        m.cube("body", (-3, L + 1, -6.5), (6, 5, 3), WH)            # bờm cổ
        m.cube("body", (-2, L + 5, -5), (4, 1, 4), GL)              # vân sáng lưng
    m.bone("head", "body", (0, L + 4, -5.5))
    m.cube("head", (-2.5, L + 2, -9.5), (5, 4, 4), SV, eyes=[(0, 1, 1, 1, (90, 200, 255)), (4, 1, 1, 1, (90, 200, 255))])
    m.cube("head", (-1, L + 2, -11.5), (2, 2, 2), WH); m.cube("head", (-.5, L + 3, -11.8), (1, 1, 1), DK)
    eh = 4 if adult else 3
    for x in (-2.5, 1.5):
        m.cube("head", (x, L + 6, -7.5), (1, eh, 1), SV); m.cube("head", (x + .1, L + 6, -7.6), (.8, eh - 1, 1), PK)
        m.cube("head", (x, L + 6 + eh, -7.5), (1, 1, 1), GL)
    m.cube("head", (-.5, L + 5.5, -8.8), (1, 1, 1), CR)
    if adult:                                                       # vương miện pha lê
        for x, h in ((-1.5, 2), (-.5, 3), (.5, 2)):
            m.cube("head", (x, L + 6, -8.3), (1, h, 1), CR)
    spread = 80 if adult else 56
    for i in range(tails):
        ry = -spread / 2 + spread * i / max(1, tails - 1)
        nm = f"tail_{i + 1}"
        m.bone(nm, "body", (0, L + 3, 5.5))
        ln = 9 if adult else 7.5
        m.cube(nm, (-1.5, L + 2, 5.5), (3, 3, ln), SV, rot=[-15 if adult else -8, ry, 0], pivot=[0, L + 3, 5.5])
        m.cube(nm, (-1, L + 2.5, 5.5 + ln), (2, 2, 2), GL, rot=[-15 if adult else -8, ry, 0], pivot=[0, L + 3, 5.5])
    q = quad_legs(m, "body", L, (-1.8, 1.8), (-4, 4), 2, 2, SV, DK)
    sway = [(f"tail_{i + 1}", 1, 8 + i % 3 * 2, 1, i % 3) for i in range(tails)]
    return m, cfg(quad=q, legs=False, sway=sway)


def linh_ho_g1(): return fox("linh_ho_g1", 5, False)


def linh_ho_g2(): return fox("linh_ho_g2", 9, True)


# ----------------------------------------------------------------------------- THIẾT BỐI HÙNG
def bear_stage(id, k, tw, adult):
    """Thiếu niên (k nhỏ, đầu nhỏ lại so với ấu thể, giáp lưng mới nhú) / trưởng thành (giáp vai, ngà, bờm gai, vân phù văn)."""
    m = Model(id, tw, (3 * k + 1, 3 * k + 1), 1.0 * k)
    FR = skin((80, 76, 86)); LT = skin((128, 122, 130)); PL = plate((118, 122, 134), (210, 222, 240)); DK = skin((38, 36, 42))
    GL = glow((255, 190, 90)); WH = bone((230, 224, 206))
    s = lambda v: v * k
    I = lambda v: max(1, int(round(v)))
    m.bone("body", None, (0, s(14), 0))
    m.cube("body", (-s(6.5), s(14), -s(10)), (s(13), s(13), s(20)), FR)
    m.cube("body", (-s(5.5), s(12.5), -s(9)), (s(11), s(2), s(18)), LT)
    # yên giáp phù văn trên lưng + dây đai bụng
    m.cube("body", (-s(5.5), s(26.5), -s(1)), (s(11), max(1, s(3)), s(10)), PL)
    m.cube("body", (-s(6.8), s(18), s(3)), (s(13.6), s(9), max(1, s(1.2))), DK)
    m.cube("body", (-s(6.2), s(12.6), -s(10.5)), (s(12.4), s(14), s(5)), DK)          # bờm đen quanh cổ/vai
    if adult:
        for x in (-1, 1):                                           # giáp vai lớn phủ xuống
            m.cube("body", (s(7.5 * x - 3.5), s(14), -s(9)), (s(7), s(14), s(9)), PL)
            m.cube("body", (s(7.5 * x - 3), s(27), -s(8.5)), (s(6), s(2), s(8)), PL)
        for i, (z, h) in enumerate(((-13, 6), (-10, 8), (-7, 9), (-4, 7), (-1, 5))):  # bờm gai dựng
            for x in (-3, 0, 3):
                m.cube("body", (s(x - 1.2), s(27), s(z)), (s(2.4), s(h - abs(x) * .6), s(2.4)), DK, rot=[-25, 0, x * 6], pivot=[s(x), s(27), s(z)])
        m.cube("body", (-s(.6), s(29.6), -s(1)), (s(1.2), max(1, s(.6)), s(10)), GL)    # vân cam trên yên
        m.cube("body", (-s(2.5), s(18), -s(10.6)), (s(5), s(5), max(1, s(1))), GL)
        m.cube("body", (-s(.5), s(27.5), -s(9)), (s(1), max(1, s(.5)), s(19)), GL)  # vân sáng dọc lưng
    m.bone("head", "body", (0, s(22), -s(10)))
    hs = 7 if adult else 6.5
    m.cube("head", (-s(hs / 2), s(18), -s(15.5)), (s(hs), s(hs), s(6)), FR,
           eyes=[(I(s(1)), I(s(2)), I(s(1.4)), I(s(1)), (255, 190, 90)), (I(s(hs)) - I(s(1)) - I(s(1.4)), I(s(2)), I(s(1.4)), I(s(1)), (255, 190, 90))])
    m.cube("head", (-s(2), s(18), -s(18.5)), (s(4), s(3), s(3)), LT)
    m.cube("head", (-s(1), s(19.5), -s(18.9)), (max(1, s(2)), max(1, s(1)), max(1, s(1))), DK)
    m.cube("head", (-s(4), s(24.5), -s(12.5)), (s(2), s(2), max(1, s(1))), FR); m.cube("head", (s(2), s(24.5), -s(12.5)), (s(2), s(2), max(1, s(1))), FR)
    if adult:
        for x in (-1, 1):                                           # ngà
            m.cube("head", (s(3.2 * x - .6), s(15), -s(18)), (s(1.2), s(4), s(1.2)), WH, rot=[-35, 0, x * -20], pivot=[s(3.2 * x), s(17), -s(17.5)])
        m.cube("head", (-s(3.5), s(25), -s(15)), (s(7), s(1.5), s(3)), PL)  # mũ giáp trán
    hip = 14
    q = quad_legs(m, "body", s(hip), (-s(4.8), s(4.8)), (-s(7), s(7)), s(4.8), s(4.8), FR, PL if adult else None)
    return m, cfg(quad=q, legs=False, skills=dict(attack="strike", skill="roar"))


def thiet_boi_hung_con_g1(): return bear_stage("thiet_boi_hung_con_g1", 0.6, 64, False)


def thiet_boi_hung_g2(): return bear_stage("thiet_boi_hung_g2", 1.0, 128, True)


JOBS = [("ngoc_tho_g1", ngoc_tho_g1), ("ngoc_tho_g2", ngoc_tho_g2), ("linh_ho_g1", linh_ho_g1), ("linh_ho_g2", linh_ho_g2),
        ("thiet_boi_hung_con_g1", thiet_boi_hung_con_g1), ("thiet_boi_hung_g2", thiet_boi_hung_g2)]

if __name__ == "__main__":
    for d in ("geo/entity", "animations/entity", "textures/entity"): (A / d).mkdir(parents=True, exist_ok=True)
    for id, fn in JOBS:
        m, c = fn()
        m.build()
        geo = json.loads((A / "geo/entity" / f"{id}.geo.json").read_text())
        build_anims(id, geo, c)
        print("ok", id)
