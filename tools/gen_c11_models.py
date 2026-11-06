#!/usr/bin/env python3
"""2026-11-06: model GeckoLib cho Yêu Thú thực vật (Thụ Yêu, Thực Nhân Hoa, Đằng Xà, Quỷ Cô) + boss Vạn Niên Thụ Vương.
Chạy: python -X utf8 tools/gen_c11_models.py [id ...]   (từ D:\\mc_mod)"""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from gen_c7_models import quad_legs, cfg


def thu_yeu():
    """Thụ Yêu: cây cổ thụ biết đi, thân vỏ nứt rêu phủ, mặt hốc cây mắt xanh lục phát sáng, tán lá dày, rễ làm chân."""
    m = Model("thu_yeu", 256, (3, 4), 1.2)
    BK = plate((86, 62, 40), (150, 110, 70)); MS = skin((70, 120, 56)); LF = cloth((44, 110, 44), (120, 200, 80)); DK = plate((50, 36, 24), (110, 76, 46))
    GL = glow((120, 255, 120))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 4.5, 11, 0))
        m.cube(nm, (sgn * 4.5 - 3, 2, -3), (6, 9, 6), BK)
        for dx, dz in ((-3, -3), (2, -3), (-3, 2), (2, 2)):
            m.cube(nm, (sgn * 4.5 + dx, 0, dz), (1.5, 2.5, 1.5), DK)
    m.bone("body", None, (0, 11, 0))
    m.cube("body", (-7, 11, -5), (14, 16, 10), BK, rune=True)
    m.cube("body", (-6, 22, -5.6), (6, 3, 1), MS); m.cube("body", (1, 14, -5.6), (5, 4, 1), MS)
    m.cube("body", (-3, 16, -5.8), (6, 5, 1), DK, eyes=[(1, 2, 1, 1, (120, 255, 120)), (4, 2, 1, 1, (120, 255, 120))])
    m.cube("body", (-2, 14, -5.9), (4, 1, 1), GL)
    m.bone("head", "body", (0, 27, 0))
    for i, (w, y) in enumerate(((20, 27), (16, 32), (10, 37), (5, 41))):
        m.cube("head", (-w / 2, y, -w / 2), (w, 5 if i < 3 else 3, w), LF)
    m.cube("head", (-1, 30, -10.5), (2, 2, 1), GL); m.cube("head", (6, 33, -8.5), (2, 2, 1), GL)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 9, 25, 0))
        m.cube(nm, (sgn * 9 - 2.5, 8, -2.5), (5, 17, 5), BK)
        for k in (-1, 0, 1):
            m.cube(nm, (sgn * 9 - .5 + k * 1.8, 2, -1), (1.2, 6, 1.2), DK, rot=[-12, 0, k * 10])
        m.cube(nm, (sgn * 9 - 3.5, 19, -3.5), (7, 4, 7), LF)
    return m, cfg(legs=True, stride=12, sway=[("head", 2, 2, 1, 0)], skills=dict(attack="strike", skill="slam"))


def thuc_nhan_hoa():
    """Thực Nhân Hoa: hoa ăn thịt khổng lồ, thân cuống to, đầu hoa đỏ đốm vàng có hàm răng, lá lớn xòe, rễ bò."""
    m = Model("thuc_nhan_hoa", 128, (3, 3), 1.0)
    ST = skin((60, 130, 50)); LF = cloth((50, 150, 60), (140, 230, 100)); PT = skin((200, 40, 60)); SP = skin((250, 210, 70)); TH = bone((240, 236, 210)); IN = skin((90, 10, 30))
    m.bone("body", None, (0, 4, 0))
    m.cube("body", (-4, 0, -4), (8, 4, 8), ST)
    for sgn in (-1, 1):
        m.cube("body", (sgn * 4 + (0 if sgn > 0 else -8), 1, -3), (8, 1, 6), LF, rot=[0, 0, sgn * 15], pivot=[sgn * 4, 2, 0])
        m.cube("body", (-3, 1, sgn * 4 + (0 if sgn > 0 else -8)), (6, 1, 8), LF, rot=[sgn * -15, 0, 0], pivot=[0, 2, sgn * 4])
    m.bone("stem", "body", (0, 4, 0))
    m.cube("stem", (-1.5, 4, -1.5), (3, 12, 3), ST)
    m.cube("stem", (1.5, 9, -1), (4, 1, 2), LF)
    m.bone("head", "stem", (0, 16, 0))
    m.cube("head", (-5, 16, -5), (10, 4, 10), PT)
    for x, z in ((-4, -3), (2, -4), (-3, 2), (3, 3)):
        m.cube("head", (x, 19.6, z), (1.5, .5, 1.5), SP)
    m.cube("head", (-3.5, 15.5, -6), (7, 4, 1), IN)
    for x in (-3, -1, 1, 3):
        m.cube("head", (x - .5, 17, -6.3), (1, 2, .5), TH)
    m.bone("jaw", "head", (0, 16, -2))
    m.cube("jaw", (-4.5, 13, -6), (9, 3, 8), PT)
    for x in (-3, -1, 1, 3):
        m.cube("jaw", (x - .5, 15.5, -6.3), (1, 1.5, .5), TH)
    for a, (x, z) in enumerate(((-5.5, 0), (5.5, 0), (0, 5.5), (0, -5.5))):
        m.cube("head", (x - 1.5, 17, z - 1.5), (3, 1, 3), PT, rot=[0, a * 45, 0])
    return m, cfg(legs=False, bob=.4, sway=[("stem", 2, 6, 1, 0), ("jaw", 0, 10, 2, 0), ("head", 1, 5, 1, .5)], skills=dict(attack="strike", skill="cast"))


def dang_xa():
    """Đằng Xà: rắn dây leo, thân xoắn từ dây mây xanh sẫm có lá nhỏ, đầu là nụ hoa tím hé, lưỡi gai."""
    m = Model("dang_xa", 128, (3, 3), 0.8)
    VN = skin((50, 96, 40)); DK = skin((32, 64, 28)); LF = cloth((70, 160, 60), (150, 230, 110)); FL = skin((140, 70, 170)); GL = glow((220, 160, 255)); TH = bone((200, 220, 160))
    m.bone("body", None, (0, 2, 0))
    m.cube("body", (-2.2, 0, -6), (4.4, 3.5, 12), VN)
    m.cube("body", (-1.5, 3.5, -4), (1.5, 1, 2), LF); m.cube("body", (.5, 3.5, 1), (1.5, 1, 2), LF)
    m.bone("head", "body", (0, 2, -6))
    m.cube("head", (-2.8, 0, -12), (5.6, 4.5, 6), FL, eyes=[(1, 3, 1, 1, (220, 160, 255)), (4, 3, 1, 1, (220, 160, 255))])
    for sgn in (-1, 1):
        m.cube("head", (sgn * 2.8 + (0 if sgn > 0 else -2), 1, -11), (2, 3.5, 4), FL, rot=[0, sgn * 20, 0], pivot=[sgn * 2.8, 2, -9])
    m.cube("head", (-.5, 1, -14), (1, .6, 2.5), TH)
    m.cube("head", (-1, 4.5, -10), (2, 1, 2), GL)
    prev = "body"
    for i, (w, l) in enumerate(((4, 9), (3.4, 9), (2.6, 8), (1.6, 7))):
        nm = f"tail{i+1}"; z0 = 6 + sum(x[1] for x in ((4, 9), (3.4, 9), (2.6, 8), (1.6, 7))[:i])
        m.bone(nm, prev, (0, 2, z0))
        m.cube(nm, (-w / 2, 0, z0), (w, w * .8, l), VN if i % 2 == 0 else DK)
        m.cube(nm, (w / 2 - .2, w * .8 - .2, z0 + l / 2), (1.5, .8, 1.5), LF)
        prev = nm
    sw = [("head", 1, 12, 1, 0)] + [(f"tail{i+1}", 1, 18 + i * 4, 1, (i + 1) * .9) for i in range(4)]
    return m, cfg(legs=False, sway=sw, skills=dict(attack="strike", skill="strike"))


def quy_co():
    """Quỷ Cô: nấm yêu thân mập trắng ngà, mũ nấm tím đỏ đốm trắng phát quang, mắt vàng, bào tử bay."""
    m = Model("quy_co", 128, (3, 3), 1.0)
    BD = skin((226, 214, 190)); CP = skin((150, 40, 90)); SP = glow((250, 240, 200)); GL = glow((210, 255, 120)); DK = skin((150, 130, 110))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 2.5, 5, 0))
        m.cube(nm, (sgn * 2.5 - 2, 0, -2), (4, 5, 4), DK)
    m.bone("body", None, (0, 5, 0))
    m.cube("body", (-5, 5, -4), (10, 10, 8), BD, eyes=[(2, 6, 2, 1, (255, 210, 60)), (6, 6, 2, 1, (255, 210, 60))])
    m.cube("body", (-2, 7, -4.4), (4, 1.5, 1), DK)
    m.bone("head", "body", (0, 15, 0))
    m.cube("head", (-9, 15, -9), (18, 4, 18), CP)
    m.cube("head", (-6, 19, -6), (12, 3, 12), CP)
    for x, z in ((-7, -5), (4, -7), (-2, 2), (5, 4), (-6, 5), (1, -2)):
        m.cube("head", (x, 19.2 if abs(x) > 5 or abs(z) > 5 else 22, z), (2, .6, 2), SP)
    m.cube("head", (-8.5, 14.5, -8.5), (17, .6, 17), GL)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 5.5, 13, 0))
        m.cube(nm, (sgn * 5.5 - 1.5, 6, -1.5), (3, 7, 3), BD)
    return m, cfg(legs=True, stride=16, sway=[("head", 2, 3, 1, 0)], skills=dict(attack="strike", skill="cast"))


def van_nien_thu_vuong():
    """Vạn Niên Thụ Vương (boss): cổ thụ vương khổng lồ, vương miện cành có quả linh phát sáng, lõi ngực phát quang vàng lục, rễ tay dài."""
    m = Model("van_nien_thu_vuong", 256, (4, 4), 1.6)
    BK = plate((70, 50, 34), (140, 100, 60)); DK = plate((40, 28, 20), (90, 60, 36)); LF = cloth((30, 96, 40), (110, 200, 80)); MS = skin((80, 130, 60))
    GL = glow((200, 255, 120)); FR = glow((255, 200, 80)); GD = crystal((230, 190, 60), (255, 240, 150))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 6, 14, 0))
        m.cube(nm, (sgn * 6 - 4, 2, -4), (8, 12, 8), BK)
        for dx, dz in ((-5, -5), (3, -5), (-5, 3), (3, 3), (-1, -6)):
            m.cube(nm, (sgn * 6 + dx, 0, dz), (2, 3, 2), DK)
    m.bone("body", None, (0, 14, 0))
    m.cube("body", (-10, 14, -7), (20, 22, 14), BK, rune=True)
    m.cube("body", (-4, 22, -7.8), (8, 8, 1), GD)
    m.cube("body", (-2.5, 23.5, -8.2), (5, 5, 1), GL)
    m.cube("body", (-9, 30, -7.6), (6, 4, 1), MS); m.cube("body", (4, 16, -7.6), (5, 5, 1), MS)
    m.bone("head", "body", (0, 36, 0))
    m.cube("head", (-6, 36, -6), (12, 10, 12), BK, eyes=[(2, 5, 2, 2, (200, 255, 120)), (8, 5, 2, 2, (200, 255, 120))])
    m.cube("head", (-3, 37, -6.6), (6, 1, 1), GL)
    for i, (x, z, h) in enumerate(((-6, -6, 10), (4, -6, 12), (-6, 4, 9), (4, 4, 11), (-1, -7, 14))):
        m.cube("head", (x, 46, z), (2, h, 2), DK, rot=[0, 0, (i - 2) * 8], pivot=[x + 1, 46, z + 1])
        m.cube("head", (x - 1, 46 + h - 1, z - 1), (4, 3, 4), LF)
        m.cube("head", (x, 46 + h - 3, z - 1.5), (2, 2, 1), FR)
    for i, (w, y) in enumerate(((28, 46), (22, 51))):
        m.cube("head", (-w / 2, y, -w / 2), (w, 5, w), LF)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 13, 34, 0))
        m.cube(nm, (sgn * 13 - 3.5, 10, -3.5), (7, 24, 7), BK)
        for k in (-1, 0, 1):
            m.cube(nm, (sgn * 13 - .8 + k * 2.4, 2, -1), (1.6, 8, 1.6), DK, rot=[-14, 0, k * 12])
        m.cube(nm, (sgn * 13 - 5, 28, -5), (10, 5, 10), LF)
        m.cube(nm, (sgn * 13 - 1, 30, -5.6), (2, 2, 1), FR)
    return m, cfg(legs=True, stride=10, sway=[("head", 2, 2, 1, 0)], skills=dict(attack="strike", skill="slam"))


JOBS = [("thu_yeu", thu_yeu), ("thuc_nhan_hoa", thuc_nhan_hoa), ("dang_xa", dang_xa), ("quy_co", quy_co), ("van_nien_thu_vuong", van_nien_thu_vuong)]


def build_all(only=()):
    for id, fn in JOBS:
        if only and id not in only: continue
        m, c = fn()
        m.build()
        geo = json.loads((A / "geo/entity" / f"{id}.geo.json").read_text())
        build_anims(id, geo, c)
        print("built", id)


if __name__ == "__main__":
    build_all(set(sys.argv[1:]))
