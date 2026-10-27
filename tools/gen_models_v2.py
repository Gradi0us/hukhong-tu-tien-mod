#!/usr/bin/env python3
"""Sinh model GeckoLib goc (geo 1.12.0 + animation + texture + glowmask) cho moi/boss hukhong.
Tham my lay cam hung "thu ve vuc sau": giap den, loi sang, vuong mien sung, vong rune troi,
ao choang rach, luoi nang luong. Thiet ke ORIGINAL, khong dung asset cua hang nao.
Ten animation giu nguyen theo code Java (idle/walk/attack/skill*/phase/cast/slam/summon/roar).
Chay: python tools/gen_models_v2.py   (tu D:\\mc_mod)"""
import json, math, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *

V = (200, 140, 255)


def lens_of(id):
    f = A / "animations/entity" / f"{id}.animation.json"
    d = json.loads(f.read_text())["animations"]
    return {k.split(".")[-1]: v["animation_length"] for k, v in d.items() if not k.endswith(".death")}


def legs(m, hip, x, mat, boot, w=4, d=4, parent=None, bw=None, foot=3):
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, parent, (sgn * x, hip, 0))
        o = sgn * x - w / 2
        m.cube(nm, (o, foot, -d / 2), (w, hip - foot, d), mat)
        m.cube(nm, (o - .5, 0, -d / 2 - 1.5), ((bw or w) + 1, foot, d + 3), boot)


def torso_core(m, bone, x, y, z, s, mat):
    m.cube(bone, (x - s / 2, y, z - 1), (s, s, 1), mat)


# ----------------------------------------------------------------------------- MOBS
def hu_thi():
    """Xac khong lung gu, tay phai dai, tay trai sung tinh the vuc, rach ao."""
    m = Model("hu_thi", 128, (3.5, 3.2), 1.2)
    SK = skin((90, 118, 86), (175, 110, 255)); CL = cloth((74, 56, 84), (150, 90, 220))
    WR = cloth((150, 138, 118)); BN = bone((205, 196, 172), (180, 120, 255))
    CR = crystal((88, 40, 160), (205, 140, 255)); GL = glow((215, 150, 255)); DK = plate((34, 28, 44), (120, 70, 200))
    legs(m, 12, 2.5, CL, DK)
    m.cube("left_leg", (.5, 3, -2.1), (4, 9, 4), WR)  # quan bang trai thay mat phang
    m.cube("left_leg", (1.5, 5, -3), (1, 5, 1), GL)
    m.bone("body", None, (0, 12, 0), [14, 0, 0])
    m.cube("body", (-4, 12, -2), (8, 12, 4), CL)
    m.cube("body", (-4.5, 12, -2.5), (9, 3, 5), DK)
    for i in range(3):  # xuong suon ho
        m.cube("body", (-3, 15.5 + i * 2.6, -3), (6, 1, 1), BN)
    m.cube("body", (-2, 15, -2.4), (4, 7, 1), GL)
    for i, (x, rz) in enumerate(((-2, 18), (1, -12), (-0.5, 4))):  # tinh the lung
        m.cube("body", (x - 1, 18 + i * 2, 2), (2, 6 - i, 2), CR, rot=[22, 0, rz])
    for i, (x, h) in enumerate(((-3, 7), (0, 9), (3, 6))):  # rach ao treo
        b = m.bone(f"rag_{i+1}", "body", (x, 13, -2))
        m.cube(b, (x - 1.5, 13 - h, -2.6), (3, h, 1), CL)
    m.bone("head", "body", (0, 24, -.5))
    m.cube("head", (-3.5, 24, -4.5), (7, 7, 7), SK, eyes=[(1, 3, 2, 2, (225, 160, 255)), (4, 4, 1, 1, (225, 160, 255))])
    m.cube("head", (-3.5, 31, -4.5), (7, 1, 7), DK)  # vet nut so
    m.cube("head", (1, 30.5, -1), (2, 4, 2), CR, rot=[0, 0, 22])
    m.cube("head", (-3.4, 24.8, -3.6), (6, 1, 3), BN)  # ham tren hoc
    m.bone("jaw", "head", (0, 25, -3.5), [18, 0, 0])
    m.cube("jaw", (-3, 22.5, -4.6), (6, 2, 5), BN)
    m.cube("jaw", (-2.5, 24.4, -4.3), (5, 1, 1), GL)
    m.bone("right_arm", "body", (-5.5, 23, 0))  # tay phai dai
    m.cube("right_arm", (-8, 9, -1.5), (3, 14, 3), SK)
    m.cube("right_arm", (-8.5, 6, -2), (4, 3, 4), BN)
    for i in range(3): m.cube("right_arm", (-8.3 + i * 1.3, 2.5, -1.8), (1, 4, 1), BN, rot=[-8, 0, 0])
    m.bone("left_arm", "body", (5.5, 23, 0))   # tay trai sung to
    m.cube("left_arm", (4.5, 11, -2.5), (6, 12, 5), SK)
    m.cube("left_arm", (5, 7, -2.5), (5, 4, 5), BN)
    m.cube("left_arm", (8, 15, -1), (2, 7, 2), CR, rot=[0, 0, -25])
    m.cube("left_arm", (7, 19, -2), (2, 5, 2), CR, rot=[10, 0, -50])
    for i in range(3): m.cube("left_arm", (5.3 + i * 1.5, 3, -2), (1, 4, 1), BN)
    m.build()
    return m, dict(skills=dict(attack="strike", skill="slam"), sway=[("rag_1", 0, 6, 1, 0), ("rag_2", 0, 7, 1, 1), ("rag_3", 0, 6, 1, 2), ("jaw", 0, 5, 1, 0)])


def hu_cot():
    """Thu xuong: chan chim, so keo dai, long nguc loi vuc, duoi cot song."""
    m = Model("hu_cot", 128, (4, 3.2), 1.2)
    BN = bone((214, 206, 182), (180, 120, 255)); BD = bone((150, 140, 124)); CR = crystal((88, 40, 160), (205, 140, 255))
    GL = glow((215, 150, 255)); DK = plate((26, 22, 36), (120, 70, 200))
    for sgn, nm, sn in ((-1, "right_leg", "shin_r"), (1, "left_leg", "shin_l")):
        m.bone(nm, None, (sgn * 3, 15, 1))
        m.cube(nm, (sgn * 3 - 1.5, 8, -1), (3, 8, 3), BN, rot=[-22, 0, 0])
        m.bone(sn, nm, (sgn * 3, 9, -1.5))
        m.cube(sn, (sgn * 3 - 1, 1, -1.5), (2, 8, 2), BD, rot=[28, 0, 0])
        m.cube(sn, (sgn * 3 - 1.5, 0, -4), (3, 1, 5), BN)
        for k in (-1, 0, 1): m.cube(sn, (sgn * 3 + k - .5, 0, -6), (1, 1, 2), BN)
    m.bone("body", None, (0, 15, 1), [18, 0, 0])
    m.cube("body", (-3, 14, -1.5), (6, 4, 5), BN)           # xuong chau
    for i in range(5):                                       # dot song
        m.cube("body", (-1, 18 + i * 2.6, .6), (2, 2, 2), BD)
    for i in range(4):                                       # long nguc
        w = 7 - abs(i - 1.5) * 1.2
        m.cube("body", (-w / 2, 18 + i * 2.6, -3.2), (w, 1, 1), BN)
        m.cube("body", (-w / 2 - .5, 18 + i * 2.6, -1.5), (1, 1, 3), BN)
        m.cube("body", (w / 2 - .5, 18 + i * 2.6, -1.5), (1, 1, 3), BN)
    m.cube("body", (-1.5, 20, -2), (3, 5, 2), GL)           # loi vuc
    m.cube("body", (-3, 26.5, -2.5), (6, 2, 4), BN)
    m.bone("head", "body", (0, 27, -1))
    m.cube("head", (-2.5, 26.5, -9), (5, 5, 9), BN, eyes=[(0, 1, 1, 2, (215, 150, 255)), (4, 1, 1, 2, (215, 150, 255))])
    m.cube("head", (-2.5, 26.5, -9), (5, 5, 9), BN) if False else None
    m.cube("head", (-1.5, 25.5, -8.5), (3, 1, 7), DK)
    for k in range(4): m.cube("head", (-1.8 + (k % 2) * 3, 24.5, -8.5 + (k // 2) * 3), (1, 2, 1), BN)  # nanh
    m.cube("head", (-4, 30, -2), (1, 6, 1), BD, rot=[0, 0, 25]); m.cube("head", (3, 30, -2), (1, 6, 1), BD, rot=[0, 0, -25])
    m.cube("head", (-.5, 31.5, -3), (1, 5, 1), CR)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 4, 25, -.5))
        m.cube(nm, (sgn * 4 - 1 + (-1 if sgn < 0 else 0), 14, -1), (2, 11, 2), BN)
        m.cube(nm, (sgn * 4 - 1.5 + (-1 if sgn < 0 else 0), 11, -1.5), (3, 3, 3), BD)
        for k in (-1, 0, 1): m.cube(nm, (sgn * 4 + k * 1.3 - .5 + (-1 if sgn < 0 else 0), 6, -1.2), (1, 6, 1), BN, rot=[-12, 0, 0])
    prev = "body"
    for i in range(4):                                       # duoi cot song
        b = m.bone(f"tail_{i+1}", prev, (0, 15.5 - i * .6, 3.5 + i * 4))
        s = 3 - i * .5
        m.cube(b, (-s / 2, 14.5 - i * .6, 3 + i * 4), (s, s, 4), BN)
        if i == 3: m.cube(b, (-.5, 14, 14), (1, 2, 4), CR)
        prev = b
    for i, (x, y) in enumerate(((-6, 28), (7, 27), (0, 34))):  # manh troi
        b = m.bone(f"shard_{i+1}", "body", (x, y, 4))
        m.cube(b, (x - 1, y, 3), (2, 5, 2), CR, rot=[15, 0, 20 * (i - 1)])
    m.build()
    return m, dict(skills=dict(attack="strike", skill="cast"), stride=22,
                   sway=[("tail_1", 1, 10, 1, 0), ("tail_2", 1, 12, 1, .6), ("tail_3", 1, 14, 1, 1.2), ("tail_4", 1, 16, 1, 1.8)],
                   spin=[("shard_1", 1, 1, 1), ("shard_2", 1, 1, -1), ("shard_3", 1, 1, 1)],
                   roles=dict())


def hu_chu():
    """Nhen caster: bung trung phat sang, 8 chan 2 khop, mat chum, vong rune quay tren lung."""
    m = Model("hu_chu", 128, (4, 3), .6)
    CH = plate((52, 38, 66), (130, 70, 210)); CD = plate((34, 24, 44), (110, 60, 190)); EG = crystal((90, 48, 150), (210, 140, 255))
    GL = glow((215, 150, 255)); BN = bone((190, 175, 200))
    m.bone("body", None, (0, 8, 0))
    m.cube("body", (-4, 5, -5), (8, 6, 8), CH)                       # nguc
    m.cube("body", (-5, 4, 2), (10, 9, 12), CD)                      # bung
    m.cube("body", (-3, 5, 12), (6, 5, 4), CD)
    m.cube("body", (-3.5, 8.5, 3), (7, 5, 8), EG)                    # tui trung (dung bone rieng ben duoi)
    m.bone("egg_sac", "body", (0, 9, 8))
    m.cube("egg_sac", (-2, 12.5, 4), (4, 3, 6), GL)
    for i in range(3):
        b = m.bone(f"spine_{i+1}", "body", (0, 13, 2 + i * 3.5))
        m.cube(b, (-.5, 13, 1.5 + i * 3.5), (1, 5 - i, 1), BN, rot=[-12, 0, 0])
    m.bone("head", "body", (0, 7, -5))
    m.cube("head", (-3, 5.5, -9), (6, 5, 5), CH, eyes=[(0, 1, 1, 1, (225, 160, 255)), (1, 2, 1, 1, (225, 160, 255)), (4, 1, 1, 1, (225, 160, 255)), (5, 2, 1, 1, (225, 160, 255)), (2, 1, 2, 2, (255, 120, 255))])
    for sgn in (-1, 1):
        m.cube("head", (sgn * 1.5 - .5, 3, -9.5), (1, 3, 1), BN, rot=[-10, 0, 0])   # nanh
        m.cube("head", (sgn * 2.5 - .5, 5.5, -11), (1, 2, 3), BN)                    # xuc tu
    for k in range(8):
        sgn = -1 if k < 4 else 1; z = [-3.5, -1, 1.5, 4][k % 4]
        b = m.bone(f"leg{k}", "body", (sgn * 4, 8, z))
        up = 3 if sgn < 0 else 3
        m.cube(b, (sgn * 4 + (0 if sgn > 0 else -6), 7, z - 1), (6, 2, 2), CD, rot=[0, 0, sgn * -28])
        m.cube(b, (sgn * 9 - 1, 0, z - 1), (2, 9, 2), CH, rot=[0, 0, sgn * 8])
        m.cube(b, (sgn * 9.4 - .5, 0, z - 1.4), (1, 2, 1), BN)
    m.bone("sigil", "body", (0, 18, 0))                               # vong rune
    m.ring("sigil", (0, 17, 3), 5, glow((200, 120, 255)), plate((60, 30, 100), (200, 120, 255)), bead=2, every=3)
    m.build()
    return m, dict(skills=dict(attack="strike", skill="cast"), legs=False, lens_extra=None,
                   quad=[(f"leg{k}", (0, 3.14, 1.57, 4.7)[k % 4] + (0 if k < 4 else 3.14)) for k in range(8)],
                   spin=[("sigil", 1, 1, 1)], sway=[("egg_sac", 0, 3, 1, 0), ("spine_1", 0, 4, 1, 0), ("spine_2", 0, 4, 1, .5), ("spine_3", 0, 4, 1, 1)],
                   roles=dict(body="body", head="head", ra="none", la="none"))


# ----------------------------------------------------------------------------- SU DO (3)
def herald(id, theme, variant):
    """Su do kieu thu-ve-vuc: giap den, mu trum co sung vuong mien, loi nguc, vong rune lo lung."""
    ARM, ACC, GLC, CLO = theme
    P = plate(ARM, ACC, GLC); T = cloth(mul(ARM, .8), ACC, GLC); GL = glow(GLC); CR = crystal(mix(ARM, GLC, .3), GLC)
    DK = plate(mul(ARM, .55), ACC); GD = plate(CLO, GLC)
    big = variant == "te"
    m = Model(id, 128, (3.2, 3.2), 1.2)
    hip = 12
    legs(m, hip, 2.2 + (.4 if big else 0), DK, P, w=4, d=4)
    m.bone("body", None, (0, hip, 0))
    m.cube("body", (-4, hip, -2), (8, 12, 4), P, rune=True)
    m.cube("body", (-3, hip + 6, -2.6), (6, 5, 1), DK)
    m.cube("body", (-1.5, hip + 6.5, -3.4), (3, 3, 1), GL)                 # loi nguc
    m.cube("body", (-4.5, hip - 1, -2.5), (9, 2, 5), GD)                   # dai
    # vay ao (dang nang)
    for nm, z, sg in (("skirt_f", -2.6, 1), ("skirt_b", 1.6, -1)):
        b = m.bone(nm, "body", (0, hip, z))
        m.cube(b, (-3.5, hip - 9, z), (7, 10, 1), T)
        if variant != "kiem": m.cube(b, (-5, hip - 8, z), (10, 8, 1), T)
    # ao choang rach sau lung
    for i, x in enumerate((-3, 0, 3)):
        b = m.bone(f"cape_{i+1}", "body", (x, hip + 11, 2.2))
        m.cube(b, (x - 1.5, hip - 6 - i * 2, 2.2), (3, 17 + i * 2, 1), T)
    # vong rune troi sau lung
    ringbone = m.bone("rune_ring", "body", (0, hip + 8, 6))
    m.ring("rune_ring", (0, hip + 8, 6), 8 if big else 7, GL, DK, bead=2, every=3)
    m.bone("head", "body", (0, hip + 12, 0))
    m.bone("hood", "head", (0, hip + 12, 0))
    m.cube("head", (-3.5, hip + 12, -3.5), (7, 7, 7), DK, eyes=[(1, 3, 2, 1, GLC), (4, 3, 2, 1, GLC)])
    m.cube("hood", (-4, hip + 12.5, -4), (8, 8, 8), P) if False else None
    m.cube("hood", (-4, hip + 17, -4), (8, 3, 8), P)                          # mu
    m.cube("hood", (-4, hip + 12, 3.5), (8, 8, 1), P)
    # vuong mien sung
    for sgn in (-1, 1):
        m.horn("hood", (sgn * 3, hip + 19, 0), sgn, GD, segs=3, w0=2, up=3, out=1.5, curl=25)
        m.cube("hood", (sgn * 4.5 - 1, hip + 13, -1), (2, 6, 2), P, rot=[0, 0, sgn * -18])
    m.cube("hood", (-.5, hip + 19.5, -1.5), (1, 6, 1), GD)
    if variant == "kiem":
        for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
            m.bone(nm, "body", (sgn * 5.5, hip + 11, 0))
            m.cube(nm, (sgn * 5.5 - 1.5 + (-1 if sgn < 0 else 0), hip - 1, -1.5), (3, 12, 3), DK)
            m.cube(nm, (sgn * 5.5 - 2 + (-1 if sgn < 0 else 0), hip - 3, -2), (4, 3, 4), P)
        m.cube("right_arm", (-9.5, hip + 8, -3), (6, 4, 6), P); m.cube("right_arm", (-8.5, hip + 12, -2.5), (2, 4, 5), GD, rot=[0, 0, -20])
        m.cube("left_arm", (3.5, hip + 8, -3), (5, 3, 5), P)
        m.bone("blade", "right_arm", (-7, hip - 2, -1))                          # luoi nang luong
        m.cube("blade", (-8, hip - 3, -3), (2, 2, 5), GD)
        m.cube("blade", (-8.5, hip - 2.5, -3.3), (3, 1, 2), P)
        m.cube("blade", (-7.5, hip - 3, -26), (1, 3, 22), GL)
        m.cube("blade", (-7.5, hip - 2, -29), (1, 1, 4), GL)
        extra = dict(sway=[("cape_1", 0, 5, 1, 0), ("cape_2", 0, 6, 1, .8), ("cape_3", 0, 6, 1, 1.6), ("skirt_f", 0, 4, 1, 0), ("skirt_b", 0, 4, 1, 1)], spin=[("rune_ring", 1, 1, 1)])
    elif variant == "chu":
        for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
            m.bone(nm, "body", (sgn * 5.5, hip + 11, 0))
            m.cube(nm, (sgn * 5.5 - 1.5 + (-1 if sgn < 0 else 0), hip - 1, -1.5), (3, 12, 3), T)
            m.cube(nm, (sgn * 5.5 - 2 + (-1 if sgn < 0 else 0), hip - 3, -2), (4, 3, 4), DK)
        m.cube("right_arm", (-8.5, hip + 9, -2.5), (5, 3, 5), P)
        m.bone("staff", "right_arm", (-7, hip - 1, -1))                          # gay
        m.cube("staff", (-7.5, hip - 14, -4), (1, 34, 1), DK)
        m.cube("staff", (-9.5, hip + 15, -4.5), (5, 1, 2), GD)
        m.cube("staff", (-8, hip + 16, -4), (2, 2, 1), GL)
        m.cube("staff", (-8.5, hip - 13, -4.5), (3, 2, 2), GD)
        for sg, rr, tilt in ((0, 10, [60, 0, 0]), (1, 8, [0, 0, 60])):
            m.bone(f"orbit_{sg}", "body", (0, hip + 8, 0), tilt)
            m.bone(f"orbit_{sg}_s", f"orbit_{sg}", (0, hip + 8, 0))
            m.ring(f"orbit_{sg}_s", (0, hip + 8, 0), rr, GL, DK, bead=2, every=4)
        extra = dict(sway=[("cape_1", 0, 5, 1, 0), ("cape_2", 0, 6, 1, .8), ("cape_3", 0, 5, 1, 1.6), ("skirt_f", 0, 3, 1, 0)],
                     spin=[("rune_ring", 1, 1, 1), ("orbit_0_s", 1, 2, 1), ("orbit_1_s", 1, 2, -1)], pulse=["orbit_0_s", "orbit_1_s"], bob=.5, float_with=["rune_ring"])
    else:  # te: giao chu, co ao xoe, 3 cau
        for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
            m.bone(nm, "body", (sgn * 6, hip + 11, 0))
            m.cube(nm, (sgn * 6 - 2 + (-1 if sgn < 0 else 0), hip - 1, -2), (4, 12, 4), T)
            m.cube(nm, (sgn * 6 - 2.5 + (-1 if sgn < 0 else 0), hip - 3, -2.5), (5, 3, 5), GD)
            m.cube(nm, (sgn * 6 - 3 + (-1 if sgn < 0 else 0), hip + 9, -3), (6, 4, 6), P)
        m.bone("collar", "body", (0, hip + 11, 1))
        for i in range(7):
            a = -60 + i * 20
            m.cube("collar", (-1.5 + math.sin(math.radians(a)) * 6, hip + 12 + (1 - abs(a) / 90) * 4, 2), (3, 8, 1), GD, rot=[0, 0, -a * .9])
        m.cube("collar", (-6, hip + 10, -2.5), (12, 2, 5), P)
        for i in range(3):
            b = m.bone(f"orb_{i+1}", "body", (0, hip + 6, 0))
            a = TAU * i / 3
            m.cube(b, (math.cos(a) * 9 - 1.5, hip + 5 + i, math.sin(a) * 9 - 1.5), (3, 3, 3), GL)
        extra = dict(sway=[("cape_1", 0, 4, 1, 0), ("cape_2", 0, 5, 1, .8), ("cape_3", 0, 4, 1, 1.6), ("collar", 0, 2, 1, 0)],
                     spin=[("rune_ring", 1, 1, 1), ("orb_1", 1, 2, 1), ("orb_2", 1, 2, 1), ("orb_3", 1, 2, 1)], pulse=["orb_1", "orb_2", "orb_3"])
    m.build()
    cfg = dict(skills=dict(attack="strike", cast="cast"), **extra)
    return m, cfg


# ----------------------------------------------------------------------------- HU VUONG
def hu_vuong():
    m = Model("hu_vuong", 256, (5, 5), 2.2)
    ARM = (30, 24, 42); AC = (170, 80, 255)
    P = plate(ARM, AC); DK = plate((18, 14, 26), (110, 60, 190)); BN = bone((190, 180, 164), (180, 120, 255))
    CR = crystal((80, 36, 150), (210, 140, 255)); GL = glow((220, 160, 255)); T = cloth((44, 26, 62), (180, 90, 255)); GD = plate((90, 70, 40), (255, 200, 90))
    legs(m, 17, 5.5, DK, P, w=7, d=7, bw=7, foot=5)
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.cube(nm, (sgn * 5.5 - 4, 9, -4.5), (8, 4, 2), P)          # mu goi
        m.cube(nm, (sgn * 5.5 - .5, 8, -7), (1, 5, 3), BN, rot=[-20, 0, 0])
    m.bone("body", None, (0, 17, 0))
    m.cube("body", (-8, 17, -5), (16, 5, 10), DK)
    m.cube("body", (-10, 22, -6), (20, 16, 12), P, rune=True)
    m.cube("body", (-5, 24, -7), (10, 10, 2), DK)
    m.cube("body", (-3, 27, -8), (6, 6, 2), GL)                       # loi
    for i in range(4):
        m.cube("body", (-9, 23 + i * 3.5, -7), (4, 1, 2), BN); m.cube("body", (5, 23 + i * 3.5, -7), (4, 1, 2), BN)
    m.cube("body", (-11, 36, -6), (22, 3, 12), GD)
    for i in range(6):                                                  # tinh the lung
        m.cube("body", (-8 + i * 3, 30 + (i % 2) * 3, 6), (2, 8 - (i % 2) * 2, 2), CR, rot=[15, 0, (i - 2.5) * 8])
    b = m.bone("mantle", "body", (0, 38, 6))
    m.cube(b, (-9, 8, 6), (18, 30, 1), T)
    m.cube(b, (-7, 0, 6), (14, 8, 1), T)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 13, 36, 0))
        o = sgn * 13 - 3.5
        m.cube(nm, (o, 22, -3.5), (7, 16, 7), DK)
        m.cube(nm, (o - 1.5, 36, -5), (10, 6, 10), P)               # giap vai khung
        m.cube(nm, (o - .5 + sgn * .5, 37, -1), (2, 6, 2), BN, rot=[0, 0, sgn * -30]) if False else None
        m.cube(nm, (o - 1, 14, -4.5), (9, 8, 9), P)
        for k in (-1, 0, 1): m.cube(nm, (o + 3.5 + k * 2.6 - .5, 7, -3), (1, 8, 1), BN, rot=[-10, 0, 0])
        m.cube(nm, (o + 3 + sgn * 4, 20, 1), (1, 12, 1), GL, rot=[0, 0, sgn * -8])  # luoi nang luong cang tay
        for k in range(3):
            m.cube(nm, (sgn * 13 - 1.5, 37 + k * 2, -1.5 + (k - 1) * 3), (3, 6 - k, 3), BN, rot=[0, 0, sgn * (-15 - k * 10)])
    m.bone("head", "body", (0, 39, 0))
    m.cube("head", (-5, 39, -5), (10, 9, 10), DK, eyes=[(1, 4, 3, 1, (255, 200, 255)), (6, 4, 3, 1, (255, 200, 255))])
    m.cube("head", (-5.5, 38, -5.5), (11, 3, 3), P)
    m.cube("head", (-4, 38.5, -4.5), (8, 2, 1), GL)
    for sgn, nm in ((-1, "horn_r"), (1, "horn_l")):
        m.bone(nm, "head", (sgn * 4, 46, 0))
        m.horn(nm, (sgn * 4, 46, 0), sgn, BN, segs=4, w0=5, grow=-.9, up=5, out=3.2, curl=16)
    m.bone("crown", "head", (0, 50, 0))                                 # vuong mien troi
    m.ring("crown", (0, 53, 0), 8, GD, GL, bead=2, every=3)
    for i in range(5):
        a = TAU * i / 5
        m.cube("crown", (math.cos(a) * 8 - .5, 54, math.sin(a) * 8 - .5), (1, 4, 1), GD)
    m.build()
    return m, dict(skills=dict(attack="strike", slam="slam", summon="cast", roar="roar"), stride=22,
                   sway=[("mantle", 0, 4, 1, 0)], spin=[("crown", 1, 1, 1)], pulse=["crown"],
                   roles=dict())


# ----------------------------------------------------------------------------- CHG 1-4
def chg1():
    """Ky si khong lo - do thau/vang: vai khung, khien thap, nua mat toi, tim magma."""
    m = Model("chg_1", 256, (6, 6), 2.5)
    AR = (46, 20, 22); AC = (255, 120, 40)
    P = plate(AR, AC); DK = plate((22, 12, 14), (200, 80, 30)); GD = plate((130, 100, 40), (255, 210, 90)); GL = glow((255, 150, 60))
    CR = crystal((120, 30, 20), (255, 140, 60)); T = cloth((60, 22, 22), (255, 120, 40)); BN = bone((200, 190, 170))
    legs(m, 20, 6, DK, P, w=8, d=8, bw=8, foot=6)
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.cube(nm, (sgn * 6 - 5, 10, -5), (10, 6, 3), P)
        m.cube(nm, (sgn * 6 - 4, 18, -4.5), (8, 6, 9), GD)
    m.bone("body", None, (0, 20, 0))
    m.cube("body", (-8, 20, -5), (16, 6, 10), DK)
    m.cube("body", (-12, 26, -7), (24, 17, 14), P, rune=True)
    m.cube("body", (-4, 29, -8.5), (8, 9, 2), DK)
    m.cube("body", (-3, 31, -9.5), (6, 6, 2), GL)
    m.cube("body", (-13, 42, -7), (26, 3, 14), GD)
    for i in range(3): m.cube("body", (-8 + i * 7, 20 + i % 2, -6), (3, 5, 1), GD)
    b = m.bone("cape_stub", "body", (0, 44, 7))
    m.cube(b, (-9, 12, 7), (18, 32, 1), T)
    for i, (x, h) in enumerate(((-6, 6), (0, 9), (6, 6))): m.cube(b, (x - 1.5, 6 - h * .3, 8), (3, h, 1), CR)
    for sgn, nm, pn in ((-1, "right_arm", "pauldron_r"), (1, "left_arm", "pauldron_l")):
        m.bone(nm, "body", (sgn * 16, 42, 0))
        o = sgn * 16 - 4.5
        m.cube(nm, (o, 26, -4.5), (9, 16, 9), DK)
        m.cube(nm, (o - 1, 14, -5.5), (11, 12, 11), P)
        m.bone(pn, nm, (sgn * 16, 44, 0))
        m.cube(pn, (o - 2, 42, -7), (13, 6, 14), P)
        for k in range(3): m.cube(pn, (sgn * 16 - 1.5, 48 + k * .5, -4 + k * 4), (3, 7 - k, 3), GD, rot=[0, 0, sgn * (-12 - k * 6)])
    m.cube("right_arm", (-24, 3, -6), (11, 11, 11), GD)                # nam dam ro
    m.cube("right_arm", (-22, 5, -7), (7, 7, 1), GL)
    m.cube("left_arm", (11, 4, -4), (4, 22, 12), DK)                   # khien thap
    m.cube("left_arm", (15, 3, -5), (2, 24, 14), P)
    m.cube("left_arm", (16.5, 10, -1), (1, 10, 6), GL, rune=False)
    m.bone("head", "body", (0, 45, 0))
    m.cube("head", (-5, 45, -5), (10, 10, 10), P, eyes=[(1, 4, 3, 2, (255, 190, 90)), (6, 4, 3, 2, (255, 190, 90))])
    m.cube("head", (-5.5, 54, -5.5), (11, 3, 11), DK)
    m.bone("brow", "head", (0, 51, -5))
    m.cube("brow", (-6, 50, -8), (12, 3, 4), GD, rot=[-10, 0, 0])
    for sgn in (-1, 1):
        m.horn("head", (sgn * 4.5, 53, 0), sgn, BN, segs=3, w0=3, up=4, out=2.2, curl=25)
    m.cube("head", (-1, 57, -3), (2, 6, 6), GD)
    m.build()
    return m, dict(skills=chg_skills(), stride=20, sway=[("cape_stub", 0, 3, 1, 0)], pulse=["pauldron_l", "pauldron_r"])


def chg2():
    """Thien than sa nga - trang/xanh bang: ao choang dai, hao quang, 8 canh long troi."""
    m = Model("chg_2", 256, (6, 6), 2.5)
    AR = (214, 224, 236); AC = (110, 210, 255)
    P = plate((70, 84, 110), AC); W = plate(AR, AC); GL = glow((140, 235, 255)); T = cloth((200, 214, 232), AC, (110, 210, 255))
    GD = plate((200, 170, 90), (255, 230, 140)); DK = plate((30, 40, 66), AC); CR = crystal((90, 160, 220), (170, 245, 255))
    legs(m, 14, 2.5, DK, P, w=4, d=4)
    m.bone("body", None, (0, 14, 0))
    m.cube("body", (-4, 26, -2.5), (8, 14, 5), W, rune=True)            # than tren manh
    m.cube("body", (-3, 33, -3.5), (6, 3, 1), GD)
    m.cube("body", (-1.5, 29, -3.5), (3, 4, 1), GL)
    m.cube("body", (-5, 22, -3), (10, 5, 6), P)
    for i, (w, h, d, y) in enumerate(((10, 8, 8, 16), (14, 8, 10, 8), (18, 8, 12, 0))):   # vay tang
        b = m.bone(f"robe_{i+1}", "body", (0, y + h, 0))
        m.cube(b, (-w / 2, y, -d / 2), (w, h, d), T)
        m.cube(b, (-w / 2 - .5, y + 6, -d / 2 - .5), (w + 1, 2, d + 1), GD)
    m.bone("robe", "body", (0, 18, 0))
    for sgn in (-1, 1): m.cube("robe", (sgn * 8 - .5, 2, -1), (1, 12, 3), CR)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 6, 38, 0))
        o = sgn * 6 - 1.5 + (-1 if sgn < 0 else 0)
        m.cube(nm, (o, 23, -1.5), (3, 15, 3), T)
        m.cube(nm, (o - .5, 20, -2), (4, 4, 4), GD)
        m.cube(nm, (o - 1, 36, -3), (5, 3, 6), W)
        m.cube(nm, (o + 1, 19, -.5), (1, 4, 1), GL)
    m.bone("head", "body", (0, 40, 0))
    m.cube("head", (-3.5, 40, -3.5), (7, 8, 7), W, eyes=[(1, 4, 2, 1, (120, 245, 255)), (4, 4, 2, 1, (120, 245, 255))])
    m.cube("head", (-4, 46.5, -4), (8, 3, 8), GD)
    for sgn in (-1, 1): m.cube("head", (sgn * 5 - .5, 42, -3), (1, 12, 5), W, rot=[0, 0, sgn * -15])
    m.cube("head", (-.5, 49, -4.5), (1, 5, 1), GL)
    m.bone("halo", "body", (0, 56, 0))
    m.ring("halo", (0, 55, 0), 9, GL, GD, bead=2, every=3)
    m.cube("halo", (-1, 52, 8.5), (2, 5, 1), GL)
    for sgn, nm in ((-1, "wing_r"), (1, "wing_l")):
        m.bone(nm, "body", (sgn * 3, 34, 3.5))
        for k in range(5):
            L = 22 - k * 2.5
            m.cube(nm, (sgn * (6 + k * 3.5) - 1, 34 - (k * 1.5) + (4 - k) * 2, 4), (2, L, 1), W if k % 2 == 0 else CR, rot=[0, 0, sgn * (-35 - k * 15)],
                   pivot=[sgn * 3, 34, 4])
        for k in range(3):
            m.cube(nm, (sgn * (5 + k * 3) - .5, 32 + k * 4, 4.6), (1, 10 - k * 2, 1), GL, rot=[0, 0, sgn * (-50 - k * 14)], pivot=[sgn * 3, 34, 4])
    m.build()
    return m, dict(skills=chg_skills(), bob=.8, legs=False, float_with=[],
                   sway=[("wing_r", 2, 5, 1, 0), ("wing_l", 2, -5, 1, 0), ("robe_1", 0, 2, 1, 0), ("robe_2", 0, 3, 1, .5), ("robe_3", 0, 4, 1, 1)],
                   spin=[("halo", 1, 1, 1)], pulse=["halo"])


def chg3():
    """Kiem khach ngoc luc cao gay: non cuoi, mat na 1 mat, dai kiem 40px, khan dai troi."""
    m = Model("chg_3", 256, (6, 6), 2.5)
    AR = (24, 40, 36); AC = (80, 255, 170)
    P = plate(AR, AC); DK = plate((14, 22, 20), (60, 200, 140)); T = cloth((26, 58, 48), AC); GL = glow((110, 255, 190)); GD = plate((150, 140, 100), (255, 240, 170))
    CR = crystal((30, 100, 80), (130, 255, 200)); W = plate((220, 226, 220), AC)
    legs(m, 17, 3, T, DK, w=5, d=5, bw=5, foot=2)
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")): m.cube(nm, (sgn * 3 - 3.5, 8, -3.5), (7, 9, 7), T)   # hakama rong
    m.bone("body", None, (0, 17, 0))
    m.cube("body", (-4, 17, -2), (8, 8, 4), DK)
    m.cube("body", (-5, 25, -3), (10, 14, 6), P, rune=True)
    m.cube("body", (-4.5, 17, -3.5), (9, 2, 7), GD)
    m.cube("body", (-1, 29, -3.5), (2, 5, 1), GL)
    m.cube("body", (-3, 34, -3.5), (6, 1, 1), W)
    for i in range(3):
        b = m.bone(f"scarf_{i+1}", "body", (0, 38, 3 + i * 6))
        m.cube(b, (-3, 30 - i * 3, 3 + i * 6), (6, 8, 6), T if i % 2 == 0 else CR) if False else None
        m.cube(b, (-3, 36 - i * 2, 3 + i * 6), (6, 2, 6), T)
        m.cube(b, (-3, 28 - i * 2, 3 + i * 6), (6, 1, 5), T)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 6.5, 38, 0))
        o = sgn * 6.5 - 1.5 + (-1 if sgn < 0 else 0)
        m.cube(nm, (o, 23, -1.5), (3, 15, 3), T)
        m.cube(nm, (o - .5, 21, -2), (4, 5, 4), DK)
        m.cube(nm, (o - 1.5, 36, -3.5), (6, 3, 7), P)
    m.bone("blade", "right_arm", (-7, 22, -2))
    m.cube("blade", (-8.5, 21, -5), (3, 3, 6), GD)
    m.cube("blade", (-10, 21.5, -6), (6, 2, 2), GD)
    m.cube("blade", (-8, 22, -48), (2, 4, 42), GL)                    # luoi kiem to
    m.cube("blade", (-7.5, 22.5, -50), (1, 3, 3), W)
    m.cube("blade", (-8.3, 25, -40), (2, 1, 30), W)
    m.bone("guard", "left_arm", (7, 22, -2))
    m.cube("guard", (5.5, 18, -3), (3, 8, 6), P)
    m.cube("guard", (6, 19, -2), (2, 6, 4), GL)
    m.bone("head", "body", (0, 39, 0))
    m.cube("head", (-3.5, 39, -3.5), (7, 7, 7), DK, eyes=[(1, 3, 2, 1, (130, 255, 210)), (4, 3, 2, 1, (60, 90, 80))])
    m.cube("head", (-3, 40, -4), (6, 4, 1), W)                          # mat na
    m.cube("head", (-4, 46, -4), (8, 2, 8), P)                          # non cuoi (kasa) nhieu tang
    m.cube("head", (-7, 45, -7), (14, 1, 14), T)
    m.cube("head", (-5.5, 44.5, -5.5), (11, 1, 11), DK)
    m.cube("head", (-2, 48, -2), (4, 3, 4), P)
    m.cube("head", (-.5, 51, -.5), (1, 3, 1), GL)
    m.build()
    return m, dict(skills=chg_skills(), stride=34, sway=[("scarf_1", 0, 8, 1, 0), ("scarf_2", 0, 10, 1, .8), ("scarf_3", 0, 12, 1, 1.6), ("blade", 2, 2, 1, 0)], pulse=[])


def chg4():
    """Phap su quy sung xoan: sung 5 khuc, ao choang doi doi, gai vai, cau nang luong tren tay."""
    m = Model("chg_4", 256, (6, 6), 2.5)
    AR = (40, 18, 56); AC = (255, 70, 200)
    P = plate(AR, AC); DK = plate((20, 10, 30), (200, 50, 160)); T = cloth((56, 20, 76), AC); GL = glow((255, 110, 220)); GD = plate((120, 90, 70), (255, 190, 120))
    CR = crystal((110, 30, 130), (255, 110, 230)); BN = bone((228, 214, 210))
    legs(m, 18, 3.5, DK, P, w=5, d=5, bw=5, foot=4)
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.cube(nm, (sgn * 3.5 - 3, 8, -3.5), (6, 3, 2), P)
        m.cube(nm, (sgn * 3.5 - .5, 3, -6), (1, 6, 2), BN, rot=[-30, 0, 0])
    m.bone("body", None, (0, 18, 0))
    m.cube("body", (-5, 18, -3), (10, 6, 6), DK)
    m.cube("body", (-7, 24, -4), (14, 14, 8), P, rune=True)
    m.cube("body", (-2, 27, -5), (4, 6, 1), GL)
    m.cube("body", (-6, 37, -4), (12, 3, 8), GD)
    b = m.bone("cape", "body", (0, 40, 4))                                 # ao choang rong
    m.cube(b, (-11, 4, 4), (22, 34, 1), T)
    m.cube(b, (-14, 2, 5), (28, 20, 1), T)
    m.cube(b, (-8, -3, 5), (16, 6, 1), T)
    for sgn, nm, sn in ((-1, "right_arm", "spike_r"), (1, "left_arm", "spike_l")):
        m.bone(nm, "body", (sgn * 9.5, 37, 0))
        o = sgn * 9.5 - 2 + (-1 if sgn < 0 else 0)
        m.cube(nm, (o, 21, -2), (4, 16, 4), T)
        m.cube(nm, (o - .5, 18, -2.5), (5, 5, 5), DK)
        for k in (-1, 0, 1): m.cube(nm, (o + 2 + k * 1.5 - .5, 12, -1.5), (1, 7, 1), BN, rot=[-14, 0, 0])
        m.bone(sn, nm, (sgn * 9.5, 38, 0))
        m.cube(sn, (sgn * 9.5 - 1.5, 38, -1.5), (3, 8, 3), BN, rot=[0, 0, sgn * -30])
        m.cube(sn, (sgn * 12 - 1, 40, -1.5), (2, 7, 3), P, rot=[0, 0, sgn * -55])
        m.cube(nm, (o - 1.5, 36, -4), (7, 3, 8), P)
    m.cube("left_arm", (9.5, 8, -9), (6, 6, 6), CR)                       # cau nang luong tren tay
    m.cube("left_arm", (10.5, 9, -8), (4, 4, 4), GL)
    m.bone("head", "body", (0, 40, 0))
    m.cube("head", (-4, 40, -4), (8, 8, 8), DK, eyes=[(1, 4, 2, 2, (255, 130, 230)), (5, 4, 2, 2, (255, 130, 230))])
    m.cube("head", (-4.5, 38.5, -4.5), (9, 2, 9), P)
    for sgn, nm in ((-1, "horn_r"), (1, "horn_l")):
        m.bone(nm, "head", (sgn * 4, 46, 0))
        m.horn(nm, (sgn * 4, 45, 0), sgn, BN, segs=4, w0=4, grow=-.7, up=4.5, out=3.2, curl=22)
    m.cube("head", (-3.5, 47.5, -4), (7, 2, 3), GD)
    m.cube("head", (-.5, 49, -4), (1, 4, 1), GL)
    m.build()
    return m, dict(skills=chg_skills(), stride=24, sway=[("cape", 0, 4, 1, 0), ("horn_r", 2, 3, 1, 0), ("horn_l", 2, -3, 1, 0)], pulse=["spike_r", "spike_l"])


def chg_skills(): return dict(attack="strike", skill_a="slam", skill_b="cast", skill_c="sweep", phase="phase")


# ----------------------------------------------------------------------------- DA KHUYET
def da_khuyet():
    """Than sa nga: ao trum xam bac, mat na tron, vanh sang vo, luoi liem trang non, xich dut."""
    m = Model("da_khuyet", 256, (6, 6), 2.2)
    NV = (22, 28, 58); SV = (200, 210, 230); GC = (255, 215, 120)
    P = plate(NV, GC); W = plate(SV, GC); DK = plate((12, 14, 30), (120, 150, 255)); T = cloth((30, 38, 78), (140, 170, 255), (160, 190, 255))
    GL = glow((255, 225, 140)); BL = glow((150, 190, 255)); GD = plate((170, 140, 70), GC); CR = crystal((60, 80, 150), (170, 200, 255))
    legs(m, 12, 2.5, DK, W, w=4, d=4)
    m.bone("body", None, (0, 12, 0))
    m.cube("body", (-4, 12, -2), (8, 22, 4), P, rune=True)
    m.cube("body", (-1, 22, -3), (2, 8, 1), GL)                         # vet nut vang
    m.cube("body", (-3, 30, -3), (6, 1, 1), GD)
    m.cube("body", (-5, 32, -2.5), (10, 4, 5), W)
    cl = m.bone("cloak", "body", (0, 36, 3))                            # ao choang dai toi dat
    m.cube(cl, (-8, 0, 3), (16, 32, 1), T)
    m.cube(cl, (-11, 0, 4), (22, 14, 1), T)
    for i, x in enumerate((-9, -3, 3, 9)):
        b = m.bone(f"tatter_{i+1}", cl, (x, 12, 4))
        m.cube(b, (x - 1.5, 0, 4.6), (3, 9, 1), DK)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 6.5, 35, 0))
        o = sgn * 6.5 - 1.5 + (-1 if sgn < 0 else 0)
        m.cube(nm, (o, 20, -1.5), (3, 16, 3), T)
        m.cube(nm, (o - .5, 18, -2), (4, 3, 4), W)
        m.cube(nm, (o - 1, 34, -3), (5, 3, 6), P)
        m.cube(nm, (o + 1.5 - .5, 14, -1.5), (1, 5, 1), W)
        for k in range(4):                                              # xich dut
            m.cube(nm, (o + 3 * sgn - .5 + (0 if k % 2 else 0), 30 - k * 3.2, -1 + (k % 2)), (1, 2, 1 + (k % 2)), GD)
    m.bone("crescent", "right_arm", (-6.5, 19, -2))                     # luoi liem
    m.cube("crescent", (-8, 3, -2), (1, 40, 1), DK)
    for i in range(7):
        a = -70 + i * 23
        x = -8 - 14 * (1 - math.cos(math.radians(a * .8))) ; y = 41 + 14 * math.sin(math.radians(a * .8))
        m.cube("crescent", (x - 1, y, -1.5), (2, 4, 2), BL, rot=[0, 0, a * .5 + 10])
    m.bone("crescent_tip_r", "crescent", (-8, 56, 0))
    m.cube("crescent_tip_r", (-10, 54, -1.5), (2, 5, 2), GL, rot=[0, 0, 40])
    m.bone("hood", "body", (0, 36, 0))
    m.bone("head", "body", (0, 36, 0))
    m.cube("head", (-3.5, 36, -3.5), (7, 8, 7), W, eyes=[(2, 4, 3, 1, GC)])
    m.cube("hood", (-4.5, 35.5, -4.5), (9, 9, 9), T)
    m.cube("hood", (-4, 44, -4), (8, 3, 8), T)
    m.cube("hood", (-1, 47, -3), (2, 5, 3), T, rot=[-20, 0, 0])
    m.bone("halo", "body", (0, 55, 0))                                  # vanh sang vo (thieu doan)
    n = max(8, int(TAU * 8 / 3.5))
    for i in range(n):
        if 5 <= i % n <= 8: continue
        a = TAU * i / n
        m.cube("halo", (math.cos(a) * 8 - 1, 54 + (i % 3) * .5, math.sin(a) * 8 - 1), (2, 1 + (i % 2), 2), GL if i % 3 == 0 else GD)
    m.bone("wing", "body", (3, 32, 3))                                  # mot canh cut
    for k in range(4):
        m.cube("wing", (5 + k * 2.2, 30 + k * 1.5, 4), (2, 16 - k * 3, 1), W if k % 2 == 0 else CR, rot=[0, 0, -30 - k * 10], pivot=[3, 32, 4])
    m.build()
    return m, dict(skills=chg_skills() if False else dict(attack="strike", skill_a="sweep", skill_b="cast", skill_c="slam", phase="phase"),
                   bob=.9, stride=18, legs=False,
                   sway=[("cloak", 0, 3, 1, 0), ("tatter_1", 0, 7, 1, 0), ("tatter_2", 0, 8, 1, 1), ("tatter_3", 0, 8, 1, 2), ("tatter_4", 0, 7, 1, 3), ("wing", 2, 4, 1, 0)],
                   spin=[("halo", 1, 1, 1)], pulse=["halo", "crescent_tip_r"], float_with=["halo", "crescent"])


# ----------------------------------------------------------------------------- NGUYEN THUY TICH DIET
def nttd():
    """Hinh thu cuoi: khong lo mau den-trang-tia tim, dau troi, vong rune khong lo, tay u linh, manh vo."""
    m = Model("nguyen_thuy_tich_diet", 256, (8, 8), 4)
    AR = (14, 12, 24); AC = (190, 110, 255)
    P = plate(AR, AC); DK = plate((7, 6, 14), (110, 60, 190)); WH = plate((226, 224, 240), (200, 170, 255)); GL = glow((236, 220, 255))
    VG = glow((190, 110, 255)); CR = crystal((60, 24, 110), (220, 170, 255)); GD = plate((130, 100, 50), (255, 220, 120)); T = cloth((26, 16, 44), AC)
    legs(m, 28, 8, DK, P, w=12, d=12, bw=12, foot=8)
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.cube(nm, (sgn * 8 - 7, 14, -7), (14, 8, 4), P)
        m.cube(nm, (sgn * 8 - 7, 24, -7), (14, 6, 14), WH)
    m.bone("body", None, (0, 28, 0))
    m.cube("body", (-13, 28, -8), (26, 8, 16), DK)
    m.cube("body", (-18, 36, -10), (36, 22, 20), P, rune=True)
    m.cube("body", (-10, 40, -12), (20, 14, 3), DK)
    m.cube("body", (-6, 43, -14), (12, 8, 3), GL)                        # loi chinh
    m.cube("body", (-4, 45, -15), (8, 4, 2), VG)
    for i in range(5): m.cube("body", (-16, 38 + i * 4, -11), (4, 1, 2), WH); m.cube("body", (12, 38 + i * 4, -11), (4, 1, 2), WH)
    m.cube("body", (-19, 56, -10), (38, 4, 20), GD)
    cp = m.bone("cape", "body", (0, 60, 10))
    for i, x in enumerate(range(-18, 19, 9)):
        m.cube(cp, (x - 4, 10 - (i % 2) * 6, 10), (8, 50 - (i % 2) * 8, 1), T)
    for i in range(7):
        m.cube("body", (-14 + i * 4.5, 50 + (i % 3) * 3, 10), (3, 12 + (i % 2) * 6, 3), CR, rot=[12, 0, (i - 3) * 8])
    for sgn, nm, pn in ((-1, "right_arm", "pauldron_r"), (1, "left_arm", "pauldron_l")):
        m.bone(nm, "body", (sgn * 24, 58, 0))
        o = sgn * 24 - 5
        m.cube(nm, (o, 36, -5), (10, 22, 10), DK)
        m.cube(nm, (o - 2, 18, -7), (14, 18, 14), P)
        m.cube(nm, (o - 1, 10, -6), (12, 8, 12), WH)
        for k in (-1, 0, 1): m.cube(nm, (o + 5 + k * 3.8 - .5, 1, -2), (2, 10, 2), WH, rot=[-8, 0, 0])
        m.cube(nm, (o + sgn * 5, 22, 3), (1, 22, 1), GL)
        m.bone(pn, nm, (sgn * 24, 60, 0))
        m.cube(pn, (o - 3, 56, -9), (16, 8, 18), P)
        for k in range(3): m.cube(pn, (sgn * 24 - 2, 63 + k, -6 + k * 6), (4, 14 - k * 3, 4), WH, rot=[0, 0, sgn * (-12 - k * 8)])
    for sgn, nm in ((-1, "ghost_arm_r"), (1, "ghost_arm_l")):                # tay u linh
        m.bone(nm, "body", (sgn * 22, 70, 6))
        m.cube(nm, (sgn * 22 - 2, 64, 6), (4, 16, 4), CR, rot=[0, 0, sgn * -25])
        m.cube(nm, (sgn * 30 - 3, 78, 5), (6, 6, 6), VG)
        for k in (-1, 0, 1): m.cube(nm, (sgn * 30 + k * 2 - .5, 84, 6), (1, 6, 1), GL, rot=[0, 0, k * 10])
    m.bone("head", "body", (0, 64, 0))
    m.cube("head", (-7, 64, -7), (14, 14, 14), P, eyes=[(2, 6, 4, 2, (255, 240, 255)), (8, 6, 4, 2, (255, 240, 255))])
    m.cube("head", (-8, 62, -8), (16, 4, 4), WH)
    m.cube("head", (-5, 63, -7.5), (10, 2, 1), GL)
    m.cube("head", (-8, 76, -8), (16, 3, 16), DK)
    for i in range(7):                                                  # vuong mien gai
        a = TAU * i / 7
        m.cube("head", (math.cos(a) * 8 - 1, 79, math.sin(a) * 8 - 1), (2, 8 + (i % 2) * 4, 2), WH if i % 2 else GD, rot=[0, 0, 0])
    m.cube("head", (-1, 79, -9), (2, 14, 2), GD)
    rings = [("ring_top", (0, 95, 0), 18, None, GL, DK, 0), ("ring_ul", (-22, 60, -4), 20, [70, 0, 25], GL, DK, 0),
             ("ring_ur", (22, 60, -4), 20, [70, 0, -25], VG, DK, 0), ("ring_ll", (-14, 20, 0), 16, [80, 0, 15], VG, DK, 0),
             ("ring_lr", (14, 20, 0), 16, [80, 0, -15], GL, DK, 0)]
    spins = []
    for nm, c, R, tilt, mt, mt2, _ in rings:
        if tilt:
            tb = m.bone(nm + "_t", "body", c, tilt)
            m.bone(nm, tb, c)
        else:
            m.bone(nm, "body", c)
        m.ring(nm, (c[0], c[1], c[2]), R, mt, mt2 if False else plate((70, 40, 130), (220, 170, 255)), bead=3, thick=2, every=3)
        for k in range(4):                                              # rune tren vong
            a = TAU * k / 4 + .4
            m.cube(nm, (c[0] + math.cos(a) * R - 1, c[1] + 2, c[2] + math.sin(a) * R - 1), (2, 5, 2), WH)
    for sgn, nm in ((-1, "shard_r"), (1, "shard_l")):
        m.bone(nm, "body", (sgn * 36, 40, 0))
        m.cube(nm, (sgn * 36 - 2, 36, -2), (4, 14, 4), CR, rot=[0, 0, 15])
        m.cube(nm, (sgn * 36 - 1, 50, -1), (2, 6, 2), GL)
    for i in range(4):
        b = m.bone(f"shard_{i+1}", "body", (0, 70, 0))
        a = TAU * i / 4 + .8
        m.cube(b, (math.cos(a) * 30 - 2, 74 + (i % 2) * 8, math.sin(a) * 30 - 2), (4, 12, 4), CR, rot=[10, 0, 10])
    m.build()
    cfg = dict(skills=chg_skills(), stride=14, bob=.9, legs=True,
               sway=[("cape", 0, 3, 1, 0), ("ghost_arm_r", 2, 5, 1, 0), ("ghost_arm_l", 2, -5, 1, 1)],
               spin=[("ring_top", 1, 1, 1), ("ring_ul", 1, 1, 1), ("ring_ur", 1, 1, -1), ("ring_ll", 1, 2, -1), ("ring_lr", 1, 2, 1)] +
                    [(f"shard_{i+1}", 1, 1, 1 if i % 2 else -1) for i in range(4)] + [("shard_r", 1, 1, 1), ("shard_l", 1, 1, -1)],
               pulse=["ring_top", "ring_ul", "ring_ur"], float_with=[])
    return m, cfg


JOBS = [("hu_thi", hu_thi), ("hu_cot", hu_cot), ("hu_chu", hu_chu),
        ("su_do_kiem", lambda: herald("su_do_kiem", ((30, 26, 52), (150, 90, 255), (190, 110, 255), (130, 100, 60)), "kiem")),
        ("su_do_chu", lambda: herald("su_do_chu", ((18, 40, 52), (70, 220, 230), (90, 235, 245), (110, 120, 130)), "chu")),
        ("su_do_te", lambda: herald("su_do_te", ((46, 22, 28), (255, 190, 90), (255, 215, 130), (150, 110, 50)), "te")),
        ("hu_vuong", hu_vuong), ("chg_1", chg1), ("chg_2", chg2), ("chg_3", chg3), ("chg_4", chg4),
        ("nguyen_thuy_tich_diet", nttd), ("da_khuyet", da_khuyet)]

if __name__ == "__main__":
    only = set(sys.argv[1:])
    for id, fn in JOBS:
        if only and id not in only: continue
        L = lens_of(id)
        m, cfg = fn()
        cfg["lengths"] = L
        geo = json.loads((A / "geo/entity" / f"{id}.geo.json").read_text())
        build_anims(id, geo, cfg)
