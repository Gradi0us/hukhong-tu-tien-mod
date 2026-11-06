#!/usr/bin/env python3
"""Đợt 9 (2026-11-06): model GeckoLib cho 5 Yêu Thú rừng (hổ, tê giác, vượn, cóc, trăn). Dùng _gm2_engine như gen_c8.
Chạy: python -X utf8 tools/gen_c9_models.py [id ...]   (từ D:\\mc_mod)"""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from gen_c7_models import quad_legs, cfg


def xich_van_ho():
    """Hổ lông cam đỏ, sọc đen, bụng trắng, mắt vàng, đuôi dài."""
    m = Model("xich_van_ho", 128, (3, 3), 0.9)
    OR = skin((214, 96, 36)); BK = skin((34, 22, 18)); WH = skin((240, 232, 214)); TO = bone((245, 240, 225)); PK = skin((200, 110, 110))
    m.bone("body", None, (0, 11, 0))
    m.cube("body", (-3.5, 9, -8), (7, 7, 16), OR, rune=False)
    m.cube("body", (-3, 8.5, -7), (6, 1, 14), WH)
    for z in (-6, -3, 0, 3, 6):  # sọc lưng
        m.cube("body", (-3.6, 13, z), (7.2, 3, 1), BK)
    m.bone("head", "body", (0, 14, -8))
    m.cube("head", (-3.5, 11, -14), (7, 6, 6), OR, eyes=[(1, 3, 1, 1, (255, 210, 40)), (5, 3, 1, 1, (255, 210, 40))])
    m.cube("head", (-2, 11, -16), (4, 3, 2), WH)
    m.cube("head", (-.5, 13, -16.3), (1, 1, 1), PK)
    m.cube("head", (-1.5, 10.5, -15.5), (1, 1.5, 1), TO); m.cube("head", (.5, 10.5, -15.5), (1, 1.5, 1), TO)
    m.cube("head", (-1.5, 16, -14.1), (3, 1, 1), BK); m.cube("head", (-.5, 15, -14.1), (1, 1, 1), BK)
    m.cube("head", (-3.5, 17, -11), (2, 2, 1), OR); m.cube("head", (1.5, 17, -11), (2, 2, 1), OR)
    m.bone("tail", "body", (0, 14, 8))
    m.cube("tail", (-1, 13, 8), (2, 2, 10), OR, rot=[-30, 0, 0], pivot=[0, 14, 8])
    m.cube("tail", (-1.1, 13, 13), (2.2, 2.2, 1), BK, rot=[-30, 0, 0], pivot=[0, 14, 8])
    m.cube("tail", (-1.1, 13, 16), (2.2, 2.2, 2), BK, rot=[-30, 0, 0], pivot=[0, 14, 8])
    q = quad_legs(m, "body", 9, (-2.3, 2.3), (-5.5, 5.5), 2.5, 2.5, OR, WH)
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 14, 1, 0), ("head", 0, 3, 1, 0)], skills=dict(attack="strike", skill="roar"))


def hac_giap_te():
    """Tê giác giáp đen: thân to, các tấm giáp xám xanh, sừng trắng ngà 2 chiếc, mắt đỏ."""
    m = Model("hac_giap_te", 128, (4, 4), 1.2)
    SK = skin((70, 70, 78)); PL = plate((48, 50, 60), (120, 130, 150)); HN = bone((236, 228, 206)); DK = skin((30, 30, 34)); GL = glow((255, 80, 60))
    m.bone("body", None, (0, 14, 0))
    m.cube("body", (-6, 12, -10), (12, 11, 20), SK)
    for i, z in enumerate((-9, -3, 3)):
        m.cube("body", (-6.5, 18, z), (13, 6, 5), PL, rune=(i == 1))
    m.cube("body", (-1, 23.5, -8), (2, 2, 14), PL)
    m.bone("head", "body", (0, 18, -10))
    m.cube("head", (-4, 13, -18), (8, 8, 8), SK, eyes=[(1, 4, 1, 1, (255, 70, 50)), (6, 4, 1, 1, (255, 70, 50))])
    m.cube("head", (-3.5, 12, -22), (7, 6, 4), SK)
    m.cube("head", (-1.5, 17, -23), (3, 6, 3), HN, rot=[-20, 0, 0], pivot=[0, 17, -21])
    m.cube("head", (-1, 20, -20), (2, 4, 2), HN)
    m.cube("head", (-4.5, 21, -12), (2, 3, 1), SK); m.cube("head", (2.5, 21, -12), (2, 3, 1), SK)
    m.cube("head", (-.5, 15, -18.2), (1, 1, 1), GL)
    m.bone("tail", "body", (0, 20, 10))
    m.cube("tail", (-.5, 15, 10), (1, 5, 1), DK, rot=[20, 0, 0], pivot=[0, 20, 10])
    q = quad_legs(m, "body", 12, (-3.5, 3.5), (-7, 7), 4, 4, SK, DK)
    return m, cfg(quad=q, legs=False, sway=[("tail", 0, 15, 1, 0)], skills=dict(attack="strike", skill="roar"))


def cuong_luc_vien():
    """Vượn khổng lồ lông nâu đỏ, ngực đen, mặt da, tay dài chạm đất, vết rune đỏ trên vai."""
    m = Model("cuong_luc_vien", 128, (3, 4), 1.0)
    FR = skin((120, 50, 38)); DK = skin((40, 26, 24)); FC = skin((190, 150, 130)); GL = glow((255, 90, 50))
    for sgn, nm in ((-1, "right_leg"), (1, "left_leg")):
        m.bone(nm, None, (sgn * 3, 9, 0))
        m.cube(nm, (sgn * 3 - 2.5, 0, -2.5), (5, 9, 5), FR)
    m.bone("body", None, (0, 9, 0))
    m.cube("body", (-7, 9, -4), (14, 15, 9), FR)
    m.cube("body", (-5, 11, -4.3), (10, 10, 1), DK)
    m.cube("body", (-7.5, 21, -4.5), (15, 4, 10), FR)
    m.cube("body", (-6, 23, -2), (2, 1, 2), GL); m.cube("body", (4, 23, -2), (2, 1, 2), GL)
    m.bone("head", "body", (0, 25, -1))
    m.cube("head", (-3.5, 24, -6), (7, 7, 6), FR, eyes=[(1, 3, 1, 1, (255, 200, 60)), (5, 3, 1, 1, (255, 200, 60))])
    m.cube("head", (-2.5, 24.5, -6.4), (5, 4, 1), FC)
    m.cube("head", (-4, 30, -5), (8, 2, 6), DK)
    for sgn, nm in ((-1, "right_arm"), (1, "left_arm")):
        m.bone(nm, "body", (sgn * 8.5, 23, 0))
        m.cube(nm, (sgn * 8.5 - 2.5, 4, -2.5), (5, 19, 5), FR)
        m.cube(nm, (sgn * 8.5 - 3, 0, -3), (6, 5, 6), DK)
    return m, cfg(legs=True, stride=10, skills=dict(attack="strike", skill="slam"))


def bich_nhan_thiem():
    """Cóc độc to: da xanh rêu sần, mắt lục bảo phát sáng lồi, túi độc vàng ở họng, lưng có mụn độc."""
    m = Model("bich_nhan_thiem", 128, (3, 3), 0.9)
    SK = skin((70, 120, 60)); DK = skin((40, 70, 36)); BL = skin((210, 200, 120)); GL = glow((90, 255, 160)); PU = crystal((120, 200, 60), (190, 255, 120))
    m.bone("body", None, (0, 6, 0))
    m.cube("body", (-6, 4, -6), (12, 8, 12), SK)
    m.cube("body", (-5, 3.5, -5), (10, 1, 10), BL)
    for x, z in ((-3, -2), (2, 0), (-1, 3), (3, -3), (-4, 2)):
        m.cube("body", (x, 12, z), (1.5, 1, 1.5), PU)
    m.bone("head", "body", (0, 9, -6))
    m.cube("head", (-5, 5, -10), (10, 5, 5), SK)
    m.cube("head", (-4, 4.5, -10.3), (8, 2, 4), BL)
    m.cube("head", (-5, 10, -9), (3, 3, 3), DK); m.cube("head", (2, 10, -9), (3, 3, 3), DK)
    m.cube("head", (-4.5, 10.5, -9.3), (2, 2, 1), GL); m.cube("head", (2.5, 10.5, -9.3), (2, 2, 1), GL)
    q = quad_legs(m, "body", 4, (-4.5, 4.5), (-3.5, 4.5), 3, 3, DK, None, wb=4, db=5)
    return m, cfg(quad=q, legs=False, bob=1.0, skills=dict(attack="strike", skill="cast"))


def ngan_lan_mang():
    """Trăn vảy bạc: thân dài nhiều đốt, vảy bạc ánh xanh, sọc lam phát sáng, đầu to có mào."""
    m = Model("ngan_lan_mang", 128, (3, 3), 0.5)
    SV = plate((180, 190, 205), (240, 250, 255)); DK = skin((90, 100, 120)); GL = glow((120, 200, 255)); WH = bone((240, 236, 220)); RD = skin((200, 40, 60))
    m.bone("body", None, (0, 3, 0))
    m.cube("body", (-3, 0, -3), (6, 5, 7), SV)
    prev = "body"
    for i in range(9):
        nm = f"seg_{i+1}"
        m.bone(nm, prev, (0, 3, 4 + i * 5))
        w = max(2, 6 - i // 2)
        m.cube(nm, (-w / 2, 0, 4 + i * 5), (w, max(2, 5 - i // 3), 5), SV if i % 2 == 0 else DK)
        if i % 2 == 0: m.cube(nm, (-.5, max(2, 5 - i // 3), 5 + i * 5), (1, 1, 3), GL)
        prev = nm
    m.bone("head", "body", (0, 3, -3))
    m.cube("head", (-3.5, 0, -10), (7, 5, 7), SV, eyes=[(0, 3, 1, 1, (120, 220, 255)), (6, 3, 1, 1, (120, 220, 255))])
    m.cube("head", (-2.5, 5, -9), (5, 2, 5), DK)
    m.cube("head", (-2, 0, -10.6), (1, 1.5, 1), WH); m.cube("head", (1, 0, -10.6), (1, 1.5, 1), WH)
    m.cube("head", (-.5, 1, -12), (1, 1, 2), RD)
    sw = [(f"seg_{i+1}", 1, 14, 1, i * .7) for i in range(9)] + [("head", 1, 6, 1, -.5)]
    return m, cfg(legs=False, bob=0, sway=sw, skills=dict(attack="strike", skill="strike"))


JOBS = [("xich_van_ho", xich_van_ho), ("hac_giap_te", hac_giap_te), ("cuong_luc_vien", cuong_luc_vien),
        ("bich_nhan_thiem", bich_nhan_thiem), ("ngan_lan_mang", ngan_lan_mang)]


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
