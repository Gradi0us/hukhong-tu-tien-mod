#!/usr/bin/env python3
"""Dot 7b (F4): model GeckoLib (geo + animation + texture + glowmask) cho 14 sinh vat. Dung _gm2_engine.
Ten animation khop code Java (CreatureCommon.controllers): idle/walk/attack/skill.
Chay: python tools/gen_c7.py (goi file nay)  hoac  python tools/gen_c7_models.py [id ...]  (tu D:\\mc_mod)"""
import json, math, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *

TAU = math.tau
LEN = dict(idle=2.0, walk=1.0, attack=0.6, skill=1.0)


def quad_legs(m, parent, hip, xs, zs, w, d, mat, hoof=None, wb=None, db=None):
    """4 chan: (fl, fr, bl, br). zs = (truoc, sau). Tra ve cfg quad."""
    names = []
    for zi, zn in enumerate(("f", "b")):
        for xi, xn in enumerate(("l", "r")):
            nm = f"leg_{zn}{xn}"
            x = xs[xi]; z = zs[zi]
            ww = w if zi == 0 or wb is None else wb
            dd = d if zi == 0 or db is None else db
            m.bone(nm, parent, (x, hip, z))
            m.cube(nm, (x - ww / 2, 0, z - dd / 2), (ww, hip, dd), mat)
            if hoof: m.cube(nm, (x - ww / 2 - .2, 0, z - dd / 2 - .2), (ww + .4, 1.5, dd + .4), hoof)
            names.append(nm)
    return [(names[0], 0), (names[1], 3.14), (names[2], 3.14), (names[3], 0)]


def cfg(**kw):
    kw.setdefault("skills", dict(attack="strike", skill="cast"))
    kw["lengths"] = dict(LEN)
    return kw


# ----------------------------------------------------------------------------- PASSIVE
def linh_loc():
    """Linh Loc: huou lanh long vang nau, sung tinh the xanh ngoc phat sang, lung diem trang."""
    m = Model("linh_loc", 128, (3, 3), 1.0)
    FUR = skin((190, 144, 92)); WH = skin((242, 234, 216)); DK = skin((98, 70, 46)); HN = crystal((90, 190, 170), (150, 255, 220))
    m.bone("body", None, (0, 13, 0))
    m.cube("body", (-3.5, 12, -6), (7, 7, 13), FUR)
    m.cube("body", (-3, 11.5, -5), (6, 1, 11), WH)
    for i, (x, z) in enumerate(((-2, -3), (1, 0), (-1, 3), (2, -1))):
        m.cube("body", (x, 19, z), (1, 1, 1), WH)
    m.cube("body", (-2, 16, -8), (4, 8, 4), FUR)
    m.cube("body", (-1, 15, 6.5), (2, 3, 2), WH)
    m.bone("head", "body", (0, 22, -8))
    m.cube("head", (-2.5, 21, -12), (5, 5, 6), FUR, eyes=[(0, 2, 1, 1, (20, 14, 10)), (4, 2, 1, 1, (20, 14, 10))])
    m.cube("head", (-1, 21, -13), (2, 2, 1), DK)
    m.cube("head", (-4.5, 25, -9), (2, 1, 3), FUR); m.cube("head", (2.5, 25, -9), (2, 1, 3), FUR)
    for sx in (-2.5, 1.5):
        m.cube("head", (sx, 26, -9), (1, 6, 1), HN)
    m.cube("head", (-3.5, 30, -9), (2, 1, 1), HN); m.cube("head", (1.5, 30, -9), (2, 1, 1), HN)
    q = quad_legs(m, "body", 11, (-2.5, 2.5), (-4.5, 4.5), 2, 2, FUR, DK)
    return m, cfg(quad=q, legs=False, sway=[("head", 0, 3, 1, 0)])


def hac_tien():
    """Hac Tien: hac trang cao, mao den canh, mao do dinh dau, mo cam dai."""
    m = Model("hac_tien", 128, (3, 3), 1.0)
    WH = skin((246, 246, 250)); BK = skin((30, 30, 40)); RD = skin((220, 40, 40)); OR = bone((240, 150, 40)); LG = bone((210, 120, 50))
    m.bone("body", None, (0, 14, 0))
    m.cube("body", (-3, 14, -4), (6, 7, 9), WH)
    m.cube("body", (-2, 15, 5), (4, 2, 5), WH); m.cube("body", (-2, 15, 9.5), (4, 2, 2), BK)
    m.cube("body", (-1, 21, -5), (2, 9, 2), WH)
    for sgn, nm in ((-1, "wing_r"), (1, "wing_l")):
        m.bone(nm, "body", (sgn * 3, 20, -1))
        o = 3 if sgn > 0 else -11
        m.cube(nm, (o, 19, -3), (8, 1, 7), WH)
        m.cube(nm, (o + (5 if sgn > 0 else 0), 19, -3.2), (3, 1, 7), BK)
    m.bone("head", "body", (0, 30, -4))
    m.cube("head", (-1.5, 29, -6), (3, 3, 3), WH, eyes=[(0, 1, 1, 1, (20, 14, 10)), (2, 1, 1, 1, (20, 14, 10))])
    m.cube("head", (-1, 32, -5), (2, 1, 2), RD)
    m.cube("head", (-.5, 29.5, -10), (1, 1, 4), OR)
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, "body", (sgn * 1.5, 14, 0))
        m.cube(nm, (sgn * 1.5 - .5, 0, -.5), (1, 14, 1), LG)
        m.cube(nm, (sgn * 1.5 - 1.5, 0, -2), (3, 1, 3), LG)
    return m, cfg(legs=True, stride=22, sway=[("wing_r", 2, 22, .5, 0), ("wing_l", 2, -22, .5, 0)])


def ngoc_tho():
    """Ngoc Tho: tho ngoc trang xanh, tai dai long ben trong hong, vien ngoc tren tran."""
    m = Model("ngoc_tho", 64, (2, 2), 0.5)
    JD = skin((206, 238, 220)); WH = skin((248, 250, 248)); PK = skin((255, 170, 200)); GM = crystal((60, 200, 150), (140, 255, 210))
    m.bone("body", None, (0, 4, 0))
    m.cube("body", (-2, 4, -3), (4, 4, 6), JD)
    m.cube("body", (-1.5, 3.8, -2.5), (3, 1, 5), WH)
    m.cube("body", (-1, 5, 3), (2, 2, 2), WH)
    m.bone("head", "body", (0, 7, -3))
    m.cube("head", (-1.5, 6, -6), (3, 3, 3), JD, eyes=[(0, 1, 1, 1, (255, 90, 150)), (2, 1, 1, 1, (255, 90, 150))])
    m.cube("head", (-1.5, 9, -4.5), (1, 5, 1), JD); m.cube("head", (.5, 9, -4.5), (1, 5, 1), JD)
    m.cube("head", (-1.2, 9, -5), (1, 3, 1), PK); m.cube("head", (.2, 9, -5), (1, 3, 1), PK)
    m.cube("head", (-.5, 8.5, -6.4), (1, 1, 1), GM)
    q = quad_legs(m, "body", 3, (-1.5, 1.5), (-2, 2), 1, 1, JD, None, wb=2, db=2)
    return m, cfg(quad=q, legs=False)


def hoa_huynh():
    """Hoa Huynh: dom dom lon, bung phat sang vang luc."""
    m = Model("hoa_huynh", 32, (1, 1), 0.2)
    DK = skin((40, 40, 52)); GL = glow((190, 255, 120)); WG = cloth((220, 235, 245))
    m.bone("body", None, (0, 3, 0))
    m.cube("body", (-1, 3, -1.5), (2, 2, 3), DK)
    m.cube("body", (-1, 3, 1.5), (2, 2, 3), GL)
    m.bone("head", "body", (0, 4, -2))
    m.cube("head", (-1, 3, -3.5), (2, 2, 2), DK, eyes=[(0, 0, 1, 1, (255, 255, 200)), (1, 0, 1, 1, (255, 255, 200))])
    for sgn, nm in ((-1, "wing_r"), (1, "wing_l")):
        m.bone(nm, "body", (sgn * 1, 5, 0))
        m.cube(nm, (1 if sgn > 0 else -4, 5, -1), (3, 1, 3), WG)
    return m, cfg(legs=False, bob=.5, sway=[("wing_r", 2, 40, .25, 0), ("wing_l", 2, -40, .25, 0)])


def linh_ngu():
    """Linh Ngu: ca vay xanh ngoc, vay anh bac, vi cam."""
    m = Model("linh_ngu", 64, (1, 1), 0.3)
    SC = skin((70, 160, 200), (150, 240, 255)); WH = skin((220, 240, 250)); OR = skin((255, 150, 60)); HN = crystal((60, 130, 190), (150, 240, 255))
    m.bone("body", None, (0, 3, 0))
    m.cube("body", (-1.5, 1.5, -3), (3, 4, 6), SC)
    m.cube("body", (-1, 1.2, -2.5), (2, 1, 5), WH)
    m.cube("body", (-.5, 5.5, -1), (1, 2, 3), OR)
    m.bone("head", "body", (0, 3.5, -3))
    m.cube("head", (-1.5, 2, -5), (3, 3, 2), SC, eyes=[(0, 1, 1, 1, (10, 10, 20)), (2, 1, 1, 1, (10, 10, 20))])
    m.cube("head", (-.5, 5, -4.5), (1, 1, 1), HN)
    m.bone("tail", "body", (0, 3.5, 3))
    m.cube("tail", (-.5, 2, 3), (1, 3, 3), SC)
    m.cube("tail", (-.5, 1, 6), (1, 5, 2), OR)
    for sgn, nm in ((-1, "fin_r"), (1, "fin_l")):
        m.bone(nm, "body", (sgn * 1.5, 3, -1))
        m.cube(nm, (sgn * 2 - .5, 2.5, -1.5), (1, 1, 2), OR)
    return m, cfg(legs=False, bob=.3, sway=[("tail", 1, 28, .5, 0), ("fin_r", 2, 20, .5, 0), ("fin_l", 2, -20, .5, 0)])


def linh_ho():
    """Linh Ho: cao 3 duoi long bac, dau duoi phat sang xanh, dau an tinh the tren tran."""
    m = Model("linh_ho", 128, (3, 3), 0.8)
    SV = skin((214, 218, 234)); WH = skin((250, 250, 255)); DK = skin((60, 62, 84)); PK = skin((255, 200, 220)); GL = glow((120, 220, 255))
    CR = crystal((90, 150, 230), (150, 230, 255))
    m.bone("body", None, (0, 8, 0))
    m.cube("body", (-2.5, 6, -5), (5, 5, 10), SV)
    m.cube("body", (-2, 5.5, -4), (4, 1, 8), WH)
    m.bone("head", "body", (0, 10, -5))
    m.cube("head", (-2.5, 8, -9), (5, 4, 4), SV, eyes=[(0, 1, 1, 1, (90, 200, 255)), (4, 1, 1, 1, (90, 200, 255))])
    m.cube("head", (-1, 8, -11), (2, 2, 2), WH); m.cube("head", (-.5, 9, -11.3), (1, 1, 1), DK)
    m.cube("head", (-2.5, 12, -7), (1, 3, 1), SV); m.cube("head", (1.5, 12, -7), (1, 3, 1), SV)
    m.cube("head", (-2.4, 12, -7.1), (1, 2, 1), PK); m.cube("head", (1.6, 12, -7.1), (1, 2, 1), PK)
    m.cube("head", (-.5, 11.5, -8.3), (1, 1, 1), CR)
    for i, ry in enumerate((-28, 0, 28)):
        nm = f"tail_{i+1}"
        m.bone(nm, "body", (0, 9, 5))
        m.cube(nm, (-1.5, 8, 5), (3, 3, 7), SV, rot=[0, ry, 0], pivot=[0, 9, 5])
        m.cube(nm, (-1, 8.5, 11.5), (2, 2, 2), GL, rot=[0, ry, 0], pivot=[0, 9, 5])
    q = quad_legs(m, "body", 6, (-1.8, 1.8), (-3.5, 3.5), 2, 2, SV, DK)
    return m, cfg(quad=q, legs=False, sway=[("tail_1", 1, 10, 1, 0), ("tail_2", 1, 12, 1, 1), ("tail_3", 1, 10, 1, 2)])


def bear(id, k, tw):
    m = Model(id, tw, (3 * k + 1, 3 * k + 1), 1.0 * k)
    FR = skin((86, 82, 90)); LT = skin((130, 124, 130)); PL = plate((118, 122, 134), (210, 222, 240)); DK = skin((40, 38, 44))
    GL = glow((255, 190, 90)); WH = bone((225, 220, 205))
    s = lambda v: v * k
    m.bone("body", None, (0, s(13), 0))
    m.cube("body", (-s(6), s(13), -s(9)), (s(12), s(12), s(18)), FR)
    for i, z in enumerate((-7, -1, 5)):
        m.cube("body", (-s(5), s(24.5), s(z)), (s(10), max(1, s(2.5)), s(5)), PL, rune=(i == 1 and k >= 1))
    m.cube("body", (-s(2), s(18), -s(9.6)), (s(4), s(4), max(1, s(1))), GL)
    m.bone("head", "body", (0, s(21), -s(9)))
    I = lambda v: max(1, int(round(v)))
    hw = I(s(7))
    m.cube("head", (-s(3.5), s(17), -s(14)), (s(7), s(7), s(6)), FR,
           eyes=[(I(s(1)), I(s(2)), I(s(1.4)), I(s(1)), (255, 190, 90)), (max(I(s(1)) + 2, hw - I(s(1)) - I(s(1.4))), I(s(2)), I(s(1.4)), I(s(1)), (255, 190, 90))])
    m.cube("head", (-s(2), s(17), -s(17)), (s(4), s(3), s(3)), LT)
    m.cube("head", (-s(1), s(18.5), -s(17.4)), (max(1, s(2)), max(1, s(1)), max(1, s(1))), DK)
    m.cube("head", (-s(4), s(24), -s(11)), (s(2), s(2), max(1, s(1))), FR); m.cube("head", (s(2), s(24), -s(11)), (s(2), s(2), max(1, s(1))), FR)
    q = quad_legs(m, "body", s(13), (-s(4.5), s(4.5)), (-s(6.5), s(6.5)), s(4.5), s(4.5), FR, None)
    c = cfg(quad=q, legs=False, skills=dict(attack="strike", skill="roar"))
    return m, c


def thiet_boi_hung(): return bear("thiet_boi_hung", 1.0, 128)


def thiet_boi_hung_con(): return bear("thiet_boi_hung_con", 0.55, 64)


# ----------------------------------------------------------------------------- HOSTILE
def doc_van_xa():
    """Doc Van Xa: ran dau tam giac, than xanh dom tim, nanh va luoi do."""
    m = Model("doc_van_xa", 64, (2, 2), 0.3)
    GR = skin((70, 120, 60), (170, 255, 90)); PU = skin((86, 54, 110)); WH = bone((240, 236, 220)); RD = skin((220, 40, 60)); GL = glow((190, 255, 80))
    m.bone("body", None, (0, 2, 0))
    m.cube("body", (-1.5, 0, -2), (3, 3, 5), GR)
    prev = "body"
    for i in range(6):
        nm = f"seg_{i+1}"
        m.bone(nm, prev, (0, 2, 3 + i * 4))
        w = max(1, 3 - (1 if i > 3 else 0))
        m.cube(nm, (-w / 2, 0, 3 + i * 4), (w, 3, 4), PU if i % 2 == 0 else GR)
        if i % 2 == 0: m.cube(nm, (-.5, 3, 3 + i * 4), (1, 1, 2), GL)
        prev = nm
    m.bone("head", "body", (0, 2, -2))
    m.cube("head", (-2, 0, -7), (4, 3, 5), GR, eyes=[(0, 0, 1, 1, (255, 230, 60)), (3, 0, 1, 1, (255, 230, 60))])
    m.cube("head", (-1.5, 0, -7.6), (1, 1, 1), WH); m.cube("head", (.5, 0, -7.6), (1, 1, 1), WH)
    m.cube("head", (-.5, 0, -9), (1, 1, 2), RD)
    sw = [(f"seg_{i+1}", 1, 16, 1, i * .8) for i in range(6)] + [("head", 1, 8, 1, -.5)]
    return m, cfg(legs=False, bob=0, sway=sw, skills=dict(attack="strike", skill="strike"))


def sa_trung():
    """Sa Trung: sau cat khong lo, giap cat vang, ham kep xuong trang, mieng do tham."""
    m = Model("sa_trung", 128, (3, 3), 0.5)
    SD = plate((214, 188, 130), (150, 110, 60)); BR = plate((150, 112, 62), (230, 200, 120)); WH = bone((235, 228, 205)); RD = skin((120, 30, 40)); GL = glow((255, 190, 80))
    m.bone("body", None, (0, 3, 6))
    m.cube("body", (-4, 0, 0), (8, 7, 8), SD)
    prev = "body"
    for i, (w, h) in enumerate(((7, 6), (6, 5), (5, 4))):
        nm = f"seg_{i+1}"
        m.bone(nm, prev, (0, 3, 8 + i * 8))
        m.cube(nm, (-w / 2, 0, 8 + i * 8), (w, h, 8), SD if i % 2 else BR)
        m.cube(nm, (-1, h, 9 + i * 8), (2, 2, 5), BR)
        prev = nm
    m.cube("body", (-1, 7, 1), (2, 2, 5), BR)
    m.bone("head", "body", (0, 3, 0))
    m.cube("head", (-5, 0, -8), (10, 8, 8), SD, eyes=[(1, 2, 2, 2, (255, 190, 80)), (7, 2, 2, 2, (255, 190, 80))])
    m.cube("head", (-3, 1, -8.6), (6, 4, 1), RD)
    m.cube("head", (-2, 8, -7), (4, 2, 5), BR)
    for sgn, nm in ((-1, "jaw_r"), (1, "jaw_l")):
        m.bone(nm, "head", (sgn * 4, 2, -8))
        m.cube(nm, (sgn * 4 - 1, 1, -14), (2, 2, 6), WH, rot=[0, -sgn * 14, 0], pivot=[sgn * 4, 2, -8])
        m.cube(nm, (sgn * 4 - .5, 1.5, -15), (1, 1, 2), WH, rot=[0, -sgn * 30, 0], pivot=[sgn * 4, 2, -8])
    sw = [("jaw_r", 1, -16, 2, 0), ("jaw_l", 1, 16, 2, 0)] + [(f"seg_{i+1}", 1, 8, 1, i * .9) for i in range(3)]
    return m, cfg(legs=False, bob=0, sway=sw, skills=dict(attack="strike", skill="roar"))


def bang_hon():
    """Bang Hon: bong ma bang, ao choang rach xanh bang, dau so nu pha le, manh bang quay quanh."""
    m = Model("bang_hon", 128, (3, 3), 1.0)
    CL = cloth((140, 200, 235), (230, 250, 255)); DK = cloth((70, 120, 170), (190, 235, 255)); SK = skin((200, 232, 248), (190, 245, 255))
    CR = crystal((90, 170, 230), (200, 245, 255)); GL = glow((200, 250, 255)); WH = bone((240, 248, 255), (200, 240, 255))
    m.bone("body", None, (0, 12, 0))
    m.cube("body", (-3, 10, -2), (6, 10, 4), CL)
    m.cube("body", (-1.5, 14, -2.6), (3, 3, 1), GL)
    for i, x in enumerate((-2.5, -.5, 1.5)):
        b = m.bone(f"rag_{i+1}", "body", (x, 10, 0))
        m.cube(b, (x - 1, 3 - i, -1), (2, 7 + i, 2), DK)
    m.bone("head", "body", (0, 20, 0))
    m.cube("head", (-3, 19, -3), (6, 6, 6), SK, eyes=[(1, 3, 1, 1, (120, 220, 255)), (4, 3, 1, 1, (120, 220, 255))])
    m.cube("head", (-3.5, 25, -3.5), (7, 1, 7), DK)
    for x in (-2.5, -.5, 1.5):
        m.cube("head", (x, 26, -.5), (1, 4 + int(abs(x)), 1), CR)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 4, 19, 0))
        m.cube(nm, (sgn * 4 - 1, 10, -1), (2, 9, 2), SK)
        for k in (-1, 0, 1):
            m.cube(nm, (sgn * 4 - .5 + k * .9, 6, -.5), (1, 4, 1), WH)
    for i in range(3):
        b = m.bone(f"shard_{i+1}", "body", (0, 14, 0))
        a = TAU * i / 3
        m.cube(b, (math.cos(a) * 9 - 1, 12 + i * 2, math.sin(a) * 9 - 1), (2, 5, 2), CR)
    return m, cfg(legs=False, bob=1.2, stride=8,
                  sway=[(f"rag_{i+1}", 0, 10, 1, i * .8) for i in range(3)],
                  spin=[(f"shard_{i+1}", 1, 1, 1 if i % 2 else -1) for i in range(3)],
                  skills=dict(attack="strike", skill="cast"))


def moc_yeu():
    """Moc Yeu: cay yeu than go sam, mat ham hoa phat sang ham, tan la rong, canh tay re cay."""
    m = Model("moc_yeu", 256, (3, 4), 1.2)
    BK = plate((98, 70, 46), (170, 120, 70)); MS = skin((62, 112, 52)); LF = cloth((50, 130, 52), (140, 220, 90)); DKB = plate((60, 42, 28), (120, 80, 50))
    GL = glow((240, 170, 50))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 4, 10, 0))
        m.cube(nm, (sgn * 4 - 2.5, 2, -2.5), (5, 8, 5), BK)
        m.cube(nm, (sgn * 4 - 4, 0, -4), (8, 2, 8), DKB)
    m.bone("body", None, (0, 10, 0))
    m.cube("body", (-6, 10, -4), (12, 14, 8), BK, rune=True)
    m.cube("body", (-5, 20, -4.6), (5, 3, 1), MS); m.cube("body", (1, 13, -4.6), (4, 3, 1), MS)
    m.bone("head", "body", (0, 24, 0))
    m.cube("head", (-4, 24, -4), (8, 8, 8), BK, eyes=[(1, 4, 2, 2, (240, 170, 50)), (5, 4, 2, 2, (240, 170, 50))])
    m.cube("head", (-1, 25, -5.5), (2, 3, 2), DKB)
    m.cube("head", (-2, 23.5, -4.5), (4, 1, 1), GL)
    for i, (w, y) in enumerate(((16, 32), (12, 36), (6, 40))):
        m.cube("head", (-w / 2, y, -w / 2), (w, 4 if i < 2 else 3, w), LF)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 8, 22, 0))
        m.cube(nm, (sgn * 8 - 2, 8, -2), (4, 14, 4), BK)
        for k in (-1, 0, 1):
            m.cube(nm, (sgn * 8 - .5 + k * 1.6, 3, -1), (1, 5, 1), DKB, rot=[-12, 0, k * 8])
        m.cube(nm, (sgn * 8 - 3, 14, -3), (6, 3, 6), LF)
    return m, cfg(legs=True, stride=14, sway=[("head", 2, 2, 1, 0)], skills=dict(attack="strike", skill="slam"))


def doc_thiem():
    """Doc Thiem: coc doc beo, bao hong cuong phong tui doc vang, bou beu tim."""
    m = Model("doc_thiem", 128, (2, 2), 0.4)
    SK = skin((92, 110, 56)); DK = skin((56, 66, 40)); PU = skin((110, 64, 130), (200, 120, 255)); GL = glow((200, 255, 90)); BL = skin((200, 190, 120))
    m.bone("body", None, (0, 4, 0))
    m.cube("body", (-4, 2, -4), (8, 5, 8), SK)
    m.cube("body", (-3, 1.5, -3), (6, 1, 6), BL)
    for i, (x, z) in enumerate(((-2, -2), (1, 0), (-1, 2), (2, -1), (0, -3))):
        m.cube("body", (x, 7, z), (1, 1, 1), PU if i % 2 else DK)
    m.bone("head", "body", (0, 5, -4))
    m.cube("head", (-4, 3, -8), (8, 4, 5), SK)
    m.cube("head", (-3.5, 7, -6), (2, 2, 2), SK, eyes=[(0, 0, 1, 1, (255, 220, 60))]); m.cube("head", (1.5, 7, -6), (2, 2, 2), SK, eyes=[(1, 0, 1, 1, (255, 220, 60))])
    m.cube("head", (-4, 3, -8.4), (8, 1, 1), DK)
    m.cube("head", (-2, 1, -7), (4, 2, 3), GL)
    q = quad_legs(m, "body", 4, (-3.5, 3.5), (-3, 3), 2, 2, SK, DK, wb=3, db=4)
    return m, cfg(quad=q, legs=False, sway=[("head", 0, 4, 1, 0)], skills=dict(attack="strike", skill="cast"))


def quan_hon():
    """Quan Hon: linh co chien truong, giap xanh den rach, so nguoi sang mat lanh, tay cam thuong."""
    m = Model("quan_hon", 128, (2, 3), 1.0)
    AR = plate((58, 68, 92), (110, 170, 230)); DK = plate((30, 34, 48), (90, 130, 190)); BN = bone((200, 210, 222), (120, 200, 255))
    CL = cloth((70, 84, 110), (120, 180, 240)); WD = skin((110, 84, 54)); CR = crystal((80, 150, 230), (150, 220, 255)); GL = glow((140, 215, 255))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 2, 12, 0))
        m.cube(nm, (sgn * 2 - 2, 2, -2), (4, 10, 4), DK)
        m.cube(nm, (sgn * 2 - 2.5, 0, -3), (5, 2, 6), AR)
    m.bone("body", None, (0, 12, 0))
    m.cube("body", (-4, 12, -2), (8, 12, 4), AR, rune=True)
    m.cube("body", (-4.5, 11, -2.5), (9, 3, 5), DK)
    m.cube("body", (-4, 7, -2.5), (8, 5, 5), CL)
    m.cube("body", (-1, 17, -2.6), (2, 3, 1), GL)
    m.bone("head", "body", (0, 24, 0))
    m.cube("head", (-4, 24, -4), (8, 8, 8), BN, eyes=[(1, 3, 2, 1, (130, 215, 255)), (5, 3, 2, 1, (130, 215, 255))])
    m.cube("head", (-4.5, 29, -4.5), (9, 3, 9), AR)
    m.cube("head", (-.5, 32, -4), (1, 3, 8), CR)
    m.bone("right_arm", "body", (-6, 22, 0))
    m.cube("right_arm", (-8, 10, -2), (3, 12, 3), CL)
    m.cube("right_arm", (-7.5, 2, -1.5), (2, 28, 2), WD)
    m.cube("right_arm", (-8.5, 30, -2.5), (3, 5, 3), CR)
    m.bone("left_arm", "body", (6, 22, 0))
    m.cube("left_arm", (5, 10, -2), (3, 12, 3), CL)
    m.cube("left_arm", (7, 10, -4), (2, 10, 8), AR)
    return m, cfg(legs=True, stride=22, skills=dict(attack="strike", skill="sweep"))


JOBS = [("linh_loc", linh_loc), ("hac_tien", hac_tien), ("ngoc_tho", ngoc_tho), ("hoa_huynh", hoa_huynh), ("linh_ngu", linh_ngu),
        ("linh_ho", linh_ho), ("thiet_boi_hung", thiet_boi_hung), ("thiet_boi_hung_con", thiet_boi_hung_con),
        ("doc_van_xa", doc_van_xa), ("sa_trung", sa_trung), ("bang_hon", bang_hon), ("moc_yeu", moc_yeu),
        ("doc_thiem", doc_thiem), ("quan_hon", quan_hon)]


def build_all(only=()):
    (A / "geo/entity").mkdir(parents=True, exist_ok=True)
    (A / "animations/entity").mkdir(parents=True, exist_ok=True)
    (A / "textures/entity").mkdir(parents=True, exist_ok=True)
    for id, fn in JOBS:
        if only and id not in only: continue
        m, c = fn()
        m.build()  # ghi geo + texture + glowmask
        geo = json.loads((A / "geo/entity" / f"{id}.geo.json").read_text())
        build_anims(id, geo, c)


if __name__ == "__main__":
    build_all(set(sys.argv[1:]))
