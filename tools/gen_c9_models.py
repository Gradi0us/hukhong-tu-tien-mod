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


def huyet_nha_tru():
    """Lợn rừng nanh máu: lông nâu sẫm bờm dựng, cặp nanh cong đỏ thẫm, mắt đỏ, sẹo trên lưng."""
    m = Model("huyet_nha_tru", 128, (3, 3), 1.0)
    FR = skin((92, 60, 44)); DK = skin((50, 32, 26)); MN = skin((30, 22, 20)); TU = bone((236, 224, 200)); RD = skin((170, 30, 30)); SN = skin((200, 140, 130))
    m.bone("body", None, (0, 11, 0))
    m.cube("body", (-4.5, 8, -8), (9, 9, 16), FR)
    m.cube("body", (-4, 7.5, -7), (8, 1, 14), DK)
    for i, z in enumerate((-7, -4, -1, 2, 5)):  # bờm dựng
        m.cube("body", (-1, 17, z), (2, 2 + (i % 2), 2.5), MN)
    m.cube("body", (2, 15, -2), (2.5, 1, 4), RD)  # sẹo
    m.bone("head", "body", (0, 13, -8))
    m.cube("head", (-3.5, 8, -14), (7, 7, 6), FR, eyes=[(1, 4, 1, 1, (255, 50, 40)), (5, 4, 1, 1, (255, 50, 40))])
    m.cube("head", (-2.5, 8, -17), (5, 4, 3), SN)
    m.cube("head", (-1.5, 9, -17.2), (1, 1, 1), DK); m.cube("head", (.5, 9, -17.2), (1, 1, 1), DK)
    for sx in (-3.5, 2.5):  # nanh cong
        m.cube("head", (sx, 9, -16.5), (1, 4, 1), TU, rot=[-25, 0, 0], pivot=[sx + .5, 9, -16])
        m.cube("head", (sx, 12, -18), (1, 1.5, 1), RD)
    m.cube("head", (-3.5, 15, -10), (2, 2, 1), FR); m.cube("head", (1.5, 15, -10), (2, 2, 1), FR)
    q = quad_legs(m, "body", 8, (-2.8, 2.8), (-5, 5), 2.5, 2.5, DK, MN)
    return m, cfg(quad=q, legs=False, sway=[("head", 0, 4, 1, 0)], skills=dict(attack="strike", skill="roar"))


def u_anh_bao():
    """Báo bóng tối: thân thon đen tím, đốm mờ, mắt tím phát sáng, đuôi dài có khói ở chót."""
    m = Model("u_anh_bao", 128, (3, 3), 0.8)
    BK = skin((36, 32, 48)); SP = skin((70, 60, 96)); GL = glow((180, 120, 255)); TO = bone((230, 226, 240))
    m.bone("body", None, (0, 10, 0))
    m.cube("body", (-2.8, 8, -8), (5.6, 5.5, 16), BK)
    for x, z in ((-2.9, -5), (1.4, -2), (-2.9, 1), (1.4, 4), (-.5, -6.5), (-.5, 3)):
        m.cube("body", (x, 11.5, z), (1.5, 1.5, 1.5), SP)
    m.bone("head", "body", (0, 12, -8))
    m.cube("head", (-2.8, 10, -13), (5.6, 5, 5), BK, eyes=[(1, 3, 1, 1, (190, 140, 255)), (4, 3, 1, 1, (190, 140, 255))])
    m.cube("head", (-1.8, 10, -15), (3.6, 2.5, 2), BK)
    m.cube("head", (-1, 9.5, -14.6), (.8, 1.5, .8), TO); m.cube("head", (.2, 9.5, -14.6), (.8, 1.5, .8), TO)
    m.cube("head", (-2.8, 15, -10), (1.5, 2, 1), BK); m.cube("head", (1.3, 15, -10), (1.5, 2, 1), BK)
    m.bone("tail", "body", (0, 12, 8))
    m.cube("tail", (-.8, 11.2, 8), (1.6, 1.6, 13), BK, rot=[-15, 0, 0], pivot=[0, 12, 8])
    m.cube("tail", (-1, 11, 19.5), (2, 2, 2), GL, rot=[-15, 0, 0], pivot=[0, 12, 8])
    q = quad_legs(m, "body", 8, (-1.9, 1.9), (-5.5, 5.5), 2, 2, BK, SP)
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 18, 1, 0), ("head", 0, 3, 1, 0)], skills=dict(attack="strike", skill="strike"))


def thiet_giap_tri_chu():
    """Nhện giáp sắt: mai kim loại xám bạc có vân, bụng to đốm đỏ, 8 chân dài khớp nhọn, 6 mắt đỏ."""
    m = Model("thiet_giap_tri_chu", 128, (4, 4), 0.8)
    PL = plate((110, 114, 126), (200, 210, 225)); DK = skin((40, 40, 48)); RD = glow((220, 40, 40)); LG = skin((60, 62, 72))
    m.bone("body", None, (0, 7, 0))
    m.cube("body", (-4, 5, -5), (8, 5, 8), PL, rune=True)
    m.cube("body", (-5, 5, 3), (10, 7, 10), DK)
    m.cube("body", (-2, 12, 6), (4, 1, 4), RD)
    m.cube("body", (-3, 12, 9.5), (1.5, 1, 1.5), RD); m.cube("body", (1.5, 12, 9.5), (1.5, 1, 1.5), RD)
    m.bone("head", "body", (0, 8, -5))
    m.cube("head", (-3, 5, -9), (6, 4, 4), PL, eyes=[(1, 2, 1, 1, (255, 40, 40)), (4, 2, 1, 1, (255, 40, 40)), (0, 0, 1, 1, (255, 40, 40)), (5, 0, 1, 1, (255, 40, 40))])
    m.cube("head", (-2, 4, -10), (1, 2, 1), LG); m.cube("head", (1, 4, -10), (1, 2, 1), LG)
    sw = []
    for i, z in enumerate((-4, -1.5, 1, 3.5)):
        for sgn, side in ((-1, "r"), (1, "l")):
            nm = f"leg{i+1}{side}"
            m.bone(nm, "body", (sgn * 4, 8, z))
            m.cube(nm, (sgn * 4 + (0 if sgn > 0 else -9), 7, z - .6), (9, 1.2, 1.2), LG, rot=[0, (i - 1.5) * 12 * sgn, -sgn * 25], pivot=[sgn * 4, 8, z])
            m.cube(nm, (sgn * 12 + (0 if sgn > 0 else -1), 0, z - .5), (1, 7, 1), DK, rot=[0, (i - 1.5) * 12 * sgn, -sgn * 25], pivot=[sgn * 4, 8, z])
            sw.append((nm, 1, 14, 1, i * .9 + (0 if sgn > 0 else 3.14)))
    return m, cfg(legs=False, bob=.6, sway=sw, skills=dict(attack="strike", skill="cast"))


def bao_viem_tich():
    """Thằn lằn lửa: da đỏ cam vảy, gai lưng vàng nóng chảy, họng phát sáng, đuôi dài vằn đen."""
    m = Model("bao_viem_tich", 128, (3, 3), 0.6)
    SK = skin((196, 74, 30)); DK = skin((80, 30, 20)); GL = glow((255, 190, 70)); SP = crystal((255, 140, 40), (255, 220, 120))
    m.bone("body", None, (0, 5, 0))
    m.cube("body", (-3.5, 3, -7), (7, 4.5, 14), SK)
    m.cube("body", (-3, 2.5, -6), (6, 1, 12), DK)
    for z in (-5, -2, 1, 4):
        m.cube("body", (-.5, 7.5, z), (1, 2.5, 1.5), SP)
    m.bone("head", "body", (0, 6, -7))
    m.cube("head", (-3, 3.5, -13), (6, 4, 6), SK, eyes=[(0, 2, 1, 1, (255, 220, 60)), (5, 2, 1, 1, (255, 220, 60))])
    m.cube("head", (-2.5, 3, -13.3), (5, 1, 5), GL)
    m.cube("head", (-1, 7.5, -10), (2, 1.5, 3), SP)
    m.bone("tail", "body", (0, 5, 7))
    m.cube("tail", (-2, 3.5, 7), (4, 3, 7), SK)
    m.bone("tail2", "tail", (0, 5, 14))
    m.cube("tail2", (-1.2, 4, 14), (2.4, 2, 8), DK)
    m.cube("tail2", (-.5, 6, 16), (1, 1.5, 1), SP)
    q = quad_legs(m, "body", 3, (-4, 4), (-4.5, 4.5), 2, 2, SK, DK)
    return m, cfg(quad=q, legs=False, sway=[("tail", 1, 14, 1, 0), ("tail2", 1, 20, 1, .8), ("head", 0, 4, 1, 0)], skills=dict(attack="strike", skill="cast"))


JOBS = [("huyet_nha_tru", huyet_nha_tru), ("u_anh_bao", u_anh_bao), ("thiet_giap_tri_chu", thiet_giap_tri_chu), ("bao_viem_tich", bao_viem_tich),
        ("xich_van_ho", xich_van_ho), ("hac_giap_te", hac_giap_te), ("cuong_luc_vien", cuong_luc_vien),
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
