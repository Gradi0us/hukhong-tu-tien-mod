#!/usr/bin/env python3
"""Đợt 8 (2026-11-05): model GeckoLib cho 4 quái mới + 3 mini boss. Dùng _gm2_engine (giống gen_c7_models).
Mini boss = cùng khung với quái thường nhưng phóng to (lớp SModel nhân toạ độ / kích thước) + chi tiết riêng.
Chạy: python -X utf8 tools/gen_c8_models.py [id ...]   (từ D:\\mc_mod)"""
import json, math, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from gen_c7_models import quad_legs, cfg

TAU = math.tau


class SModel(Model):
    """Model nhân mọi toạ độ / kích thước với k (mini boss to gấp k lần)."""
    def __init__(s, id, tw, bounds, off, k):
        super().__init__(id, tw, bounds, off)
        s.k = k

    def bone(s, name, parent=None, pivot=(0, 0, 0), rot=None):
        return super().bone(name, parent, tuple(v * s.k for v in pivot), rot)

    def cube(s, bone, o, sz, mat, rot=None, pivot=None, rune=False, eyes=(), share=False):
        pv = None if pivot is None else [v * s.k for v in pivot]
        return super().cube(bone, tuple(v * s.k for v in o), tuple(v * s.k for v in sz), mat, rot, pv, rune, eyes, share)


# ----------------------------------------------------------------------------- Hư Lang / Hư Lang Vương
def wolf(id, k, tw, king=False):
    """Sói hư không: lông đen tím, sống lưng gai tinh thể tím phát sáng, mắt tím; Vương có bờm tinh thể + vương miện gai."""
    m = SModel(id, tw, (3 * k, 3 * k), 0.8 * k, k)
    FUR = skin((44, 40, 62)); DK = skin((22, 20, 34)); BL = skin((80, 70, 110)); GL = glow((170, 120, 255))
    CR = crystal((110, 70, 200), (190, 150, 255)); TO = bone((230, 225, 240))
    m.bone("body", None, (0, 9, 0))
    m.cube("body", (-3, 7, -6), (6, 6, 12), FUR)
    m.cube("body", (-2.5, 6.5, -5), (5, 1, 10), BL)
    for i, z in enumerate((-4, -1.5, 1, 3.5)):  # gai sống lưng
        m.cube("body", (-.5, 13, z), (1, 2 + (i % 2), 1.5), CR)
    if king:
        for sgn in (-1, 1):  # bờm tinh thể hai bên vai
            for i in range(3):
                m.cube("body", (sgn * 3 - .5, 10 + i, -5 + i * 1.2), (1, 3, 1), CR, rot=[0, 0, -sgn * 25])
    m.bone("head", "body", (0, 12, -6))
    m.cube("head", (-3, 9, -11), (6, 5, 5), FUR, eyes=[(1, 2, 1, 1, (190, 140, 255)), (4, 2, 1, 1, (190, 140, 255))])
    m.cube("head", (-1.5, 9, -14), (3, 3, 3), DK)
    m.cube("head", (-1, 8.5, -13.5), (2, 1, 2), TO)
    m.cube("head", (-3, 14, -8), (1.5, 3, 1.5), FUR); m.cube("head", (1.5, 14, -8), (1.5, 3, 1.5), FUR)
    if king:
        for i, x in enumerate((-2.5, -1, .5, 2)):
            m.cube("head", (x, 14 + (1 if i in (1, 2) else 0), -10), (1, 3 + (1 if i in (1, 2) else 0), 1), CR)
        m.cube("head", (-.5, 12.5, -11.3), (1, 1, 1), GL)
    m.bone("tail", "body", (0, 11, 6))
    m.cube("tail", (-1, 9, 6), (2, 2, 7), FUR, rot=[-25, 0, 0], pivot=[0, 11, 6])
    m.cube("tail", (-.5, 8, 12), (1, 1, 2), GL, rot=[-25, 0, 0], pivot=[0, 11, 6])
    q = quad_legs(m, "body", 7, (-2, 2), (-4, 4), 2, 2, FUR, DK)
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 12, 1, 0), ("head", 0, 3, 1, 0)],
                  skills=dict(attack="strike", skill="slam" if king else "strike"))


# ----------------------------------------------------------------------------- Thạch Quỷ / Thạch Cự Nhân
def golem(id, k, tw, king=False):
    """Người đá: thân đá xám nứt mạch dung nham, tay to, mắt cam; Cự Nhân có tinh thể vàng mọc trên vai + rêu."""
    m = SModel(id, tw, (3 * k, 4 * k), 1.0 * k, k)
    ST = plate((112, 106, 96), (150, 140, 120)); DK = plate((66, 62, 56), (100, 92, 80)); LV = glow((255, 150, 60))
    MS = skin((70, 110, 60)); CR = crystal((210, 160, 60), (255, 230, 140))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 3, 10, 0))
        m.cube(nm, (sgn * 3 - 2.5, 0, -2.5), (5, 10, 5), DK)
    m.bone("body", None, (0, 10, 0))
    m.cube("body", (-6, 10, -3.5), (12, 12, 7), ST, rune=True)
    m.cube("body", (-1, 13, -3.8), (2, 6, 1), LV)
    m.cube("body", (-4, 16, -3.7), (3, 1, 1), LV); m.cube("body", (1, 18, -3.7), (3, 1, 1), LV)
    if king:
        m.cube("body", (-6.5, 21, -3), (13, 2, 6), MS)
        for sgn in (-1, 1):
            for i in range(3):
                m.cube("body", (sgn * (5 + i) - .5, 22 + i, -1 + i * .5), (1.5, 3 + i, 1.5), CR)
    m.bone("head", "body", (0, 22, 0))
    m.cube("head", (-3, 22, -3.5), (6, 6, 6), ST, eyes=[(1, 3, 1, 1, (255, 170, 60)), (4, 3, 1, 1, (255, 170, 60))])
    m.cube("head", (-3.5, 27, -4), (7, 2, 7), DK)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 7, 20, 0))
        m.cube(nm, (sgn * 7 - 2.5, 6, -2.5), (5, 14, 5), ST)
        m.cube(nm, (sgn * 7 - 3, 2, -3), (6, 5, 6), DK)
        m.cube(nm, (sgn * 7 - .5, 10, -2.8), (1, 6, 1), LV)
    return m, cfg(legs=True, stride=10, skills=dict(attack="strike", skill="slam"))


# ----------------------------------------------------------------------------- Âm Hồn Sư / Hỏa Tinh / Huyết Yêu Vương
def wraith(id, k, tw, pal, queen=False):
    """Bóng ma pháp sư: áo choàng rách, đầu đội mũ trùm / vương miện, quả cầu năng lượng xoay quanh."""
    cl, dk, sk, cr, gl, eye = pal
    m = SModel(id, tw, (3 * k, 3 * k), 1.0 * k, k)
    CL = cloth(cl, gl); DK = cloth(dk, gl); SK = skin(sk, gl); CR = crystal(cr, gl); GL = glow(gl)
    m.bone("body", None, (0, 12, 0))
    m.cube("body", (-3.5, 10, -2.5), (7, 11, 5), CL)
    m.cube("body", (-1.5, 15, -2.8), (3, 3, 1), GL)
    for i, x in enumerate((-3, -1, 1)):
        b = m.bone(f"rag_{i+1}", "body", (x + 1, 10, 0))
        m.cube(b, (x, 3 - i, -1.5), (2, 7 + i, 3), DK)
    m.bone("head", "body", (0, 21, 0))
    m.cube("head", (-3, 21, -3), (6, 6, 6), SK, eyes=[(1, 3, 1, 1, eye), (4, 3, 1, 1, eye)])
    if queen:
        for i, x in enumerate((-3, -1.5, 0, 1.5)):
            m.cube("head", (x, 27, -2), (1.5, 2 + (2 if i in (1, 2) else 0), 1.5), CR)
        m.cube("head", (-4, 22, 1), (8, 6, 2), DK)  # tóc dài sau gáy
    else:
        m.cube("head", (-3.5, 25, -3.5), (7, 4, 7), DK)  # mũ trùm
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 4.5, 20, 0))
        m.cube(nm, (sgn * 4.5 - 1.5, 11, -1.5), (3, 9, 3), CL)
        m.cube(nm, (sgn * 4.5 - 1, 9, -1), (2, 2, 2), SK)
    n = 4 if queen else 3
    for i in range(n):
        b = m.bone(f"orb_{i+1}", "body", (0, 16, 0))
        a = TAU * i / n
        m.cube(b, (math.cos(a) * 8 - 1, 15 + (i % 2) * 2, math.sin(a) * 8 - 1), (2, 2, 2), CR)
    return m, cfg(legs=False, bob=1.2, stride=8,
                  sway=[(f"rag_{i+1}", 0, 10, 1, i * .8) for i in range(3)],
                  spin=[(f"orb_{i+1}", 1, 1, 1 if i % 2 else -1) for i in range(n)],
                  skills=dict(attack="strike", skill="cast"))


SHADE = ((58, 40, 78), (30, 20, 44), (150, 140, 170), (120, 70, 190), (190, 140, 255), (200, 150, 255))
FIRE = ((200, 70, 20), (110, 30, 10), (255, 170, 80), (255, 120, 30), (255, 210, 110), (255, 240, 150))
BLOOD = ((120, 10, 24), (60, 4, 12), (230, 200, 200), (180, 10, 30), (255, 60, 70), (255, 40, 40))

JOBS = [("hu_lang", lambda: wolf("hu_lang", 1.0, 128)),
        ("hu_lang_vuong", lambda: wolf("hu_lang_vuong", 2.4, 256, True)),
        ("thach_quy", lambda: golem("thach_quy", 1.0, 128)),
        ("thach_cu_nhan", lambda: golem("thach_cu_nhan", 2.4, 256, True)),
        ("am_hon_su", lambda: wraith("am_hon_su", 1.0, 128, SHADE)),
        ("hoa_tinh", lambda: wraith("hoa_tinh", 0.75, 128, FIRE)),
        ("huyet_yeu_vuong", lambda: wraith("huyet_yeu_vuong", 2.2, 256, BLOOD, True))]


def build_all(only=()):
    (A / "geo/entity").mkdir(parents=True, exist_ok=True)
    (A / "animations/entity").mkdir(parents=True, exist_ok=True)
    (A / "textures/entity").mkdir(parents=True, exist_ok=True)
    for id, fn in JOBS:
        if only and id not in only: continue
        m, c = fn()
        m.build()
        geo = json.loads((A / "geo/entity" / f"{id}.geo.json").read_text())
        build_anims(id, geo, c)
        print("built", id)


if __name__ == "__main__":
    build_all(set(sys.argv[1:]))
