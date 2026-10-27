#!/usr/bin/env python3
"""Sinh model GeckoLib (geo + animation + texture + glowmask) cho quai/boss v3:
  quai : hu_anh (bong ma bay), hu_giap (quai khien), hu_trung (bay sau), hu_trieu_su (phap su trieu hoi)
  boss : sa_hat_vuong (sa mac), bang_nhan (tuyet/nui), doc_mau (dam lay/rung ram)
Cung engine/phong cach voi tools/gen_models_v2.py (xem _gm2_engine.py). Thiet ke ORIGINAL.
Ten animation khop code Java: idle/walk/attack/skill (quai), attack/skill_a/skill_b/skill_c/phase (boss).
Chay: python tools/gen_mobs_v3.py [id ...]   (tu D:\\mc_mod)"""
import json, math, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from gen_models_v2 import legs

TAU = math.tau
VOID = (200, 140, 255)


# ----------------------------------------------------------------------------- MOBS
def hu_anh():
    """Bong ma Hu Khong: ao choang rach troi lo lung, mu trum, canh mang rach, mat sang."""
    m = Model("hu_anh", 128, (3.2, 3.2), 1.0)
    CL = cloth((46, 34, 70), (150, 90, 230)); DK = plate((22, 18, 38), (120, 70, 200)); GL = glow((215, 150, 255))
    BN = bone((200, 190, 215), (180, 120, 255)); CR = crystal((88, 40, 160), (205, 140, 255)); SK = skin((70, 60, 110), (175, 110, 255))
    m.bone("body", None, (0, 12, 0))
    m.cube("body", (-4, 16, -2), (8, 9, 4), CL)
    m.cube("body", (-3, 12, -1.5), (6, 4, 3), CL)
    m.cube("body", (-1.5, 19, -2.6), (3, 4, 1), GL)
    for i, (x, h) in enumerate(((-3, 10), (-1, 12), (1, 11), (3, 9))):  # ao rach troi
        b = m.bone(f"rag_{i+1}", "body", (x, 13, 0))
        m.cube(b, (x - 1.2, 13 - h, -1), (2, h, 2), CL)
    m.bone("head", "body", (0, 25, 0))
    m.cube("head", (-3.5, 25, -3.5), (7, 7, 7), DK, eyes=[(1, 3, 2, 1, VOID), (4, 3, 2, 1, VOID)])
    m.cube("head", (-4, 31, -4), (8, 2, 8), CL)
    m.cube("head", (-4, 25, 3), (8, 7, 1), CL)
    for sgn in (-1, 1):
        m.cube("head", (sgn * 2.5 - .5, 32, -1), (1, 5, 1), CR, rot=[0, 0, sgn * -18])
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 5, 24, 0))
        m.cube(nm, (sgn * 5 - 1.5 + (-1 if sgn < 0 else 0), 12, -1.5), (3, 12, 3), SK)
        for k in (-1, 0, 1):
            m.cube(nm, (sgn * 5 + k * 1.1 - .5 + (-1 if sgn < 0 else 0), 8, -1), (1, 5, 1), BN, rot=[-10, 0, 0])
    for sgn, nm in ((-1, "wing_r"), (1, "wing_l")):
        m.bone(nm, "body", (sgn * 3, 22, 2.5))
        o = 0 if sgn > 0 else -13
        m.cube(nm, (o + (3 if sgn > 0 else 0), 21, 2), (10, 1, 1), BN)
        m.cube(nm, (o + (3 if sgn > 0 else 0), 14, 2.2), (10, 7, 1), CL)
        m.cube(nm, (o + (9 if sgn > 0 else 3), 17, 2), (1, 5, 1), CR)
    m.build()
    return m, dict(skills=dict(attack="strike", skill="cast"), legs=False, bob=1.2,
                   sway=[("wing_r", 2, 24, .5, 0), ("wing_l", 2, -24, .5, 0), ("rag_1", 0, 8, 1, 0), ("rag_2", 0, 9, 1, .8),
                         ("rag_3", 0, 8, 1, 1.6), ("rag_4", 0, 9, 1, 2.4)])


def hu_giap():
    """Quai giap khien: than gap, khien khong lo tren tay trai, mu giap co sung, nut sang."""
    m = Model("hu_giap", 256, (4, 4), 1.4)
    P = plate((52, 46, 70), (150, 90, 255)); DK = plate((26, 22, 40), (120, 70, 200)); SK = skin((88, 100, 84), (175, 110, 255))
    GL = glow((215, 150, 255)); CR = crystal((88, 40, 160), (205, 140, 255)); WH = bone((205, 196, 172), (180, 120, 255))
    legs(m, 14, 3.5, DK, P, w=6, d=6, foot=4)
    m.bone("body", None, (0, 14, 0), [8, 0, 0])
    m.cube("body", (-7, 14, -3.5), (14, 16, 7), P, rune=True)
    m.cube("body", (-7.5, 13, -4), (15, 3, 8), DK)
    m.cube("body", (-2, 19, -4.4), (4, 6, 1), GL)
    for i in range(3):
        m.cube("body", (-9 + i * 7.5, 28, 2.5), (3, 7 - i, 3), CR, rot=[15, 0, (i - 1) * 14])
    m.bone("head", "body", (0, 30, 0))
    m.cube("head", (-4, 30, -4), (8, 8, 8), DK, eyes=[(1, 3, 2, 1, VOID), (5, 3, 2, 1, VOID)])
    m.cube("head", (-4.5, 36, -4.5), (9, 3, 9), P)
    m.cube("head", (-.5, 39, -4), (1, 5, 8), WH)
    m.bone("right_arm", "body", (-9, 28, 0))
    m.cube("right_arm", (-13, 15, -2.5), (5, 14, 5), SK)
    m.cube("right_arm", (-14, 9, -3.5), (7, 6, 7), P)
    for k in range(3):
        m.cube("right_arm", (-13.5 + k * 2.3, 6, -3), (1, 4, 1), WH)
    m.cube("right_arm", (-14.5, 27, -3), (7, 3, 6), DK)
    m.bone("left_arm", "body", (9, 28, 0))
    m.cube("left_arm", (8, 15, -2.5), (5, 14, 5), SK)
    m.cube("left_arm", (8, 9, -3.5), (6, 6, 7), P)
    m.cube("left_arm", (4, 7, -10), (12, 28, 3), P, rune=True)         # khien
    m.cube("left_arm", (6, 18, -11.2), (8, 8, 2), GL)
    m.cube("left_arm", (3.5, 6, -10.5), (13, 1, 3), DK); m.cube("left_arm", (3.5, 34, -10.5), (13, 1, 3), DK)
    m.build()
    return m, dict(skills=dict(attack="strike", skill="slam"), stride=18)


def hu_trung():
    """Bay sau nho: than dep, 6 chan, ham nho, bung phat sang."""
    m = Model("hu_trung", 64, (1.8, 1.4), 0.3)
    CH = plate((52, 38, 66), (130, 70, 210)); CD = plate((34, 24, 44), (110, 60, 190)); GL = glow((215, 150, 255)); BN = bone((190, 175, 200))
    EG = crystal((90, 48, 150), (140, 230, 120))
    m.bone("body", None, (0, 5, 0))
    m.cube("body", (-3, 3, -4), (6, 4, 7), CH)
    m.cube("body", (-3.5, 3, 2), (7, 5, 6), CD)
    m.cube("body", (-2, 7.5, 3), (4, 1, 4), GL)
    m.cube("body", (-1, 8, -1), (2, 3, 2), EG, rot=[-15, 0, 0])
    m.bone("head", "body", (0, 5, -4))
    m.cube("head", (-2.5, 3, -8), (5, 4, 4), CH, eyes=[(0, 1, 1, 1, VOID), (4, 1, 1, 1, VOID), (2, 0, 1, 1, (140, 255, 120))])
    for sgn in (-1, 1):
        m.cube("head", (sgn * 1.5 - .5, 3, -10), (1, 1, 3), BN)
    for k in range(6):
        sgn = -1 if k < 3 else 1; z = (-3, 0, 3)[k % 3]
        b = m.bone(f"leg{k}", "body", (sgn * 3, 5, z))
        m.cube(b, (sgn * 3 + (0 if sgn > 0 else -3), 4, z - .5), (3, 1, 1), CD, rot=[0, 0, sgn * -20])
        m.cube(b, (sgn * 6 - .5, 0, z - .5), (1, 5, 1), CH)
    m.build()
    return m, dict(skills=dict(attack="strike", skill="cast"), legs=False, stride=22,
                   quad=[(f"leg{k}", (0, 3.14, 1.57)[k % 3] + (0 if k < 3 else 3.14)) for k in range(6)],
                   roles=dict(body="body", head="head", ra="none", la="none"))


def hu_trieu_su():
    """Phap su trieu hoi: ao choang dai, mu trum, truong tinh the, 3 manh troi quanh nguoi."""
    m = Model("hu_trieu_su", 128, (3.2, 3.2), 1.2)
    CL = cloth((54, 36, 86), (150, 90, 230)); DK = plate((26, 20, 44), (120, 70, 200)); SK = skin((110, 100, 140), (175, 110, 255))
    GL = glow((225, 130, 255)); CR = crystal((110, 40, 180), (225, 130, 255)); BD = bone((120, 100, 90))
    legs(m, 9, 2, DK, DK, w=3, d=3, foot=2)
    m.bone("body", None, (0, 9, 0))
    m.cube("body", (-5, 0, -3.5), (10, 12, 7), CL)
    m.cube("body", (-4, 10, -2), (8, 12, 4), CL, rune=True)
    m.cube("body", (-4.5, 9, -2.5), (9, 2, 5), DK)
    m.cube("body", (-1.5, 15, -2.6), (3, 3, 1), GL)
    m.bone("head", "body", (0, 22, 0))
    m.cube("head", (-3.5, 22, -3.5), (7, 7, 7), SK, eyes=[(1, 3, 2, 1, VOID), (4, 3, 2, 1, VOID)])
    m.cube("head", (-4, 28, -4), (8, 3, 8), CL)
    m.cube("head", (-4, 22, 3), (8, 7, 1), CL)
    for sgn in (-1, 1):
        m.cube("head", (sgn * 3 - .5, 30, -1), (1, 6, 1), CR, rot=[0, 0, sgn * -22])
    m.bone("right_arm", "body", (-5, 21, 0))
    m.cube("right_arm", (-7.5, 10, -1.5), (3, 12, 3), CL)
    m.cube("right_arm", (-9.5, 0, -.5), (1, 30, 1), BD)               # truong
    m.cube("right_arm", (-11, 29, -2), (4, 4, 4), GL)
    m.cube("right_arm", (-10.5, 33, -1.5), (3, 3, 3), CR)
    m.bone("left_arm", "body", (5, 21, 0))
    m.cube("left_arm", (4.5, 10, -1.5), (3, 12, 3), CL)
    m.cube("left_arm", (4.5, 8, -2), (3, 2, 4), GL)
    for i in range(3):
        b = m.bone(f"orb_{i+1}", "body", (0, 18, 0))
        a = TAU * i / 3
        m.cube(b, (math.cos(a) * 9 - 1, 16 + i * 2, math.sin(a) * 9 - 1), (2, 4, 2), CR, rot=[10, 0, 10])
    m.build()
    return m, dict(skills=dict(attack="strike", skill="cast"), stride=12,
                   spin=[(f"orb_{i+1}", 1, 1, 1 if i % 2 == 0 else -1) for i in range(3)])


# ----------------------------------------------------------------------------- WORLD BOSSES
def boss_skills(): return dict(attack="strike", skill_a="cast", skill_b="slam", skill_c="sweep", phase="phase")


def sa_hat_vuong():
    """Vua bo cap sa mac: mai giap cat, hai kep khong lo, duoi nhieu doan co nanh doc, tinh the Hu Khong tren lung."""
    m = Model("sa_hat_vuong", 256, (6, 5), 1.2)
    SH = plate((150, 120, 70), (255, 200, 90)); DK = plate((90, 66, 40), (220, 160, 70)); BN = bone((215, 195, 150), (255, 210, 120))
    CR = crystal((120, 50, 190), (205, 140, 255)); GL = glow((255, 190, 90))
    m.bone("body", None, (0, 14, 0))
    m.cube("body", (-12, 12, -14), (24, 12, 28), SH, rune=True)
    m.cube("body", (-10, 24, -10), (20, 3, 20), DK)
    m.cube("body", (-4, 20, -14.6), (8, 4, 1), GL)
    for i in range(5):
        m.cube("body", (-9 + i * 4.5, 27, -4 + (i % 2) * 8), (3, 8 + (i % 3) * 3, 3), CR, rot=[10, 0, (i - 2) * 9])
    m.bone("head", "body", (0, 20, -14))
    m.cube("head", (-6, 15, -22), (12, 9, 8), DK, eyes=[(2, 4, 2, 2, (255, 230, 120)), (8, 4, 2, 2, (255, 230, 120))])
    for sgn in (-1, 1):
        m.cube("head", (sgn * 3 - 1, 13, -23), (2, 4, 2), BN, rot=[-10, 0, 0])
    for sgn, nm in ((-1, "claw_r"), (1, "claw_l")):
        m.bone(nm, "body", (sgn * 12, 18, -12))
        ox = sgn * 12 - 3
        m.cube(nm, (ox, 15, -24), (6, 6, 12), SH)
        m.cube(nm, (ox - 2, 13, -36), (10, 9, 12), SH, rune=True)
        m.cube(nm, (ox - 1, 15, -42), (3, 5, 7), BN); m.cube(nm, (ox + 4, 15, -42), (3, 5, 7), BN)
    for k in range(8):
        sgn = -1 if k < 4 else 1; z = (-9, -3, 3, 9)[k % 4]
        b = m.bone(f"leg{k}", "body", (sgn * 12, 14, z))
        m.cube(b, (sgn * 12 + (0 if sgn > 0 else -9), 13, z - 1.5), (9, 3, 3), DK, rot=[0, 0, sgn * -22])
        m.cube(b, (sgn * 20 - 1.5, 0, z - 1.5), (3, 14, 3), SH)
    prev = "body"
    segs = ((0, 22, 14), (0, 30, 19), (0, 36, 18), (0, 40, 12), (0, 38, 5))
    for i, (x, y, z) in enumerate(segs):
        b = m.bone(f"tail_{i+1}", prev, (x, y, z))
        s = 7 - i
        m.cube(b, (-s / 2, y - 3, z - 3), (s, 7, 7), SH if i % 2 == 0 else DK, rot=[-25 * (i - 1), 0, 0])
        prev = b
    m.cube("tail_5", (-1.5, 32, 1), (3, 8, 3), CR, rot=[30, 0, 0])
    m.build()
    return m, dict(skills=boss_skills(), stride=22, legs=False,
                   quad=[(f"leg{k}", (0, 3.14, 1.57, 4.7)[k % 4] + (0 if k < 4 else 3.14)) for k in range(8)],
                   roles=dict(body="body", head="head", ra="claw_r", la="claw_l"),
                   sway=[("tail_1", 0, 6, 1, 0), ("tail_2", 0, 8, 1, .6), ("tail_3", 0, 10, 1, 1.2), ("tail_4", 0, 12, 1, 1.8), ("tail_5", 0, 14, 1, 2.4)])


def bang_nhan():
    """Cu nhan bang: than bang xanh, giap tinh the bang, vuong mien gai, loi nguc phat sang, tinh the Hu Khong tren vai."""
    m = Model("bang_nhan", 256, (6, 6), 2.2)
    IC = plate((130, 180, 215), (220, 245, 255)); DK = plate((60, 95, 140), (170, 220, 255)); WH = bone((235, 245, 255), (200, 240, 255))
    CR = crystal((70, 130, 200), (170, 230, 255)); GL = glow((190, 240, 255)); SK = skin((100, 140, 175), (190, 235, 255))
    legs(m, 26, 6, DK, IC, w=10, d=10, foot=5)
    m.bone("body", None, (0, 26, 0))
    m.cube("body", (-14, 26, -8), (28, 24, 16), IC, rune=True)
    m.cube("body", (-15, 25, -9), (30, 4, 18), DK)
    m.cube("body", (-5, 34, -9.6), (10, 8, 2), GL)
    for i in range(5):
        m.cube("body", (-12 + i * 6, 40, 8), (4, 14 + (i % 2) * 6, 4), CR, rot=[12, 0, (i - 2) * 8])
    m.bone("head", "body", (0, 50, 0))
    m.cube("head", (-7, 50, -7), (14, 14, 14), IC, eyes=[(2, 7, 4, 2, (230, 250, 255)), (8, 7, 4, 2, (230, 250, 255))])
    m.cube("head", (-8, 63, -8), (16, 3, 16), DK)
    for i in range(5):
        a = TAU * i / 5
        m.cube("head", (math.cos(a) * 7 - 1.5, 66, math.sin(a) * 7 - 1.5), (3, 8 + (i % 2) * 4, 3), WH)
    for sgn, nm, pn in ((-1, "right_arm", "pauldron_r"), (1, "left_arm", "pauldron_l")):
        m.bone(nm, "body", (sgn * 19, 48, 0))
        o = sgn * 19 - 5
        m.cube(nm, (o, 26, -5), (10, 22, 10), SK)
        m.cube(nm, (o - 2, 12, -7), (14, 14, 14), IC)
        for k in (-1, 0, 1): m.cube(nm, (o + 5 + k * 3.6 - .5, 3, -2), (2, 10, 2), WH, rot=[-8, 0, 0])
        m.bone(pn, nm, (sgn * 19, 50, 0))
        m.cube(pn, (o - 3, 46, -8), (16, 7, 16), DK)
        for k in range(3): m.cube(pn, (sgn * 19 - 2, 53, -5 + k * 5), (4, 12 - k * 2, 4), CR, rot=[0, 0, sgn * (-10 - k * 8)])
    for i in range(3):
        b = m.bone(f"shard_{i+1}", "body", (0, 60, 0))
        a = TAU * i / 3 + .5
        m.cube(b, (math.cos(a) * 24 - 2, 64 + (i % 2) * 8, math.sin(a) * 24 - 2), (4, 12, 4), CR, rot=[10, 0, 10])
    m.build()
    return m, dict(skills=boss_skills(), stride=14, bob=.6,
                   sway=[("pauldron_r", 2, 3, 1, 0), ("pauldron_l", 2, -3, 1, 1)],
                   spin=[(f"shard_{i+1}", 1, 1, 1 if i % 2 else -1) for i in range(3)])


def doc_mau():
    """Me doc dam lay: than phinh day u doc phat sang, chan re, dau hoa nhieu mat, day leo quat quanh lung."""
    m = Model("doc_mau", 256, (5, 5), 1.8)
    GR = skin((60, 96, 52), (160, 255, 90)); DG = plate((38, 60, 40), (130, 200, 80)); PU = cloth((70, 40, 100), (190, 110, 255))
    BN = bone((190, 200, 160), (170, 255, 110)); CR = crystal((60, 140, 50), (170, 255, 110)); GL = glow((190, 255, 110))
    VG = glow((200, 120, 255))
    legs(m, 14, 5, DG, GR, w=8, d=8, foot=4)
    m.bone("body", None, (0, 14, 0), [6, 0, 0])
    m.cube("body", (-11, 14, -8), (22, 22, 16), GR, rune=True)
    m.cube("body", (-12, 13, -9), (24, 3, 18), DG)
    for i, (x, y) in enumerate(((-6, 20), (2, 25), (-2, 30), (7, 18))):          # u doc phat sang
        m.cube("body", (x - 2, y, -9.6), (4, 4, 2), GL)
    for i in range(4):
        m.cube("body", (-9 + i * 5.5, 36, 3), (4, 8 + (i % 2) * 4, 4), CR, rot=[10, 0, (i - 1.5) * 8])
    m.bone("head", "body", (0, 36, -1))
    m.cube("head", (-6, 36, -7), (12, 11, 12), GR, eyes=[(2, 6, 2, 2, VOID), (8, 6, 2, 2, VOID), (5, 3, 2, 2, (190, 255, 110)), (5, 8, 2, 1, (190, 255, 110))])
    for i in range(6):                                                          # canh hoa
        a = TAU * i / 6
        m.cube("head", (math.cos(a) * 6 - 2, 47, math.sin(a) * 6 - 1), (4, 9, 2), PU, rot=[0, 0, 0])
    m.cube("head", (-2, 46, -2), (4, 3, 4), GL)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 13, 34, 0))
        o = sgn * 13 - 3.5
        m.cube(nm, (o, 16, -4), (7, 20, 8), GR)
        for k in (-1, 0, 1): m.cube(nm, (o + 3 + k * 2.4 - .5, 6, -3), (1, 12, 1), BN, rot=[-14, 0, 0])
    for i, (x, z) in enumerate(((-6, 8), (6, 8), (-10, 6), (10, 6))):
        b = m.bone(f"vine_{i+1}", "body", (x, 30, z))
        m.cube(b, (x - 1.5, 8, z - 1), (3, 24, 3), DG)
        m.cube(b, (x - 2, 5, z - 1.5), (4, 4, 4), CR)
    m.build()
    return m, dict(skills=boss_skills(), stride=16, bob=.4,
                   sway=[("vine_1", 0, 10, 1, 0), ("vine_2", 0, 10, 1, 1), ("vine_3", 0, 12, 1, 2), ("vine_4", 0, 12, 1, 3)])


MOB_L = dict(idle=2.0, walk=0.8, attack=0.5, skill=0.8)
BOSS_L = dict(idle=4.0, walk=1.2, attack=0.6, skill_a=1.0, skill_b=1.6, skill_c=1.6, phase=2.0)
JOBS = [("hu_anh", hu_anh, dict(MOB_L, idle=2.4, walk=1.0)), ("hu_giap", hu_giap, MOB_L), ("hu_trung", hu_trung, dict(MOB_L, walk=0.5)),
        ("hu_trieu_su", hu_trieu_su, MOB_L),
        ("sa_hat_vuong", sa_hat_vuong, BOSS_L), ("bang_nhan", bang_nhan, BOSS_L), ("doc_mau", doc_mau, BOSS_L)]

if __name__ == "__main__":
    only = set(sys.argv[1:])
    for id, fn, L in JOBS:
        if only and id not in only: continue
        m, cfg = fn()
        cfg["lengths"] = L
        geo = json.loads((A / "geo/entity" / f"{id}.geo.json").read_text())
        build_anims(id, geo, cfg)
