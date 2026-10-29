"""Icon 16x16 cho 19 ky nang hoc tu Bi Tich / Cong Phap (BookSkill id = ten file). Cung phong cach gen_skill_icons.py.
Chay: python tools/gen_book_skill_icons.py  (cung duoc goi tu gen_skill_icons.main)"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixel_shade import mask, render  # noqa: E402

OUT_DIR = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, "src", "main",
                                         "resources", "assets", "hukhong", "textures", "gui", "skill"))
W = (255, 255, 255)
STEEL = (170, 200, 235); THUNDER = (120, 160, 255); GOLD = (245, 200, 70); HOLY = (255, 230, 120)
BRONZE = (200, 130, 70); RED = (200, 40, 50); PURPLE = (130, 50, 170); GREEN = (80, 200, 100); CYAN = (90, 220, 220)


def line(d, pts, w=1):
    d.line(pts, fill=255, width=w)


ICONS = {}


def icon(name, col, seed=1, accents=()):
    def deco(fn):
        ICONS[name] = lambda: render(mask(fn), col, seed=seed, accents=list(accents))
        return fn
    return deco


@icon("pha_khong_tram", STEEL, 1, [(13, 2, W), (12, 3, W)])
def _(d):  # vet chem cheo xuyen
    d.polygon([(1, 14), (3, 15), (15, 3), (13, 1)], fill=255)
    d.rectangle([0, 12, 3, 15], fill=255)

@icon("phi_kiem_truy_hon", STEEL, 2, [(8, 2, W), (4, 6, W)])
def _(d):  # 3 thanh phi kiem huong len
    for ox, oy in ((0, 0), (-4, 4), (4, 4)):
        d.polygon([(8 + ox, 1 + oy), (10 + ox, 8 + oy), (6 + ox, 8 + oy)], fill=255)
    d.rectangle([7, 9, 8, 14], fill=255)

@icon("kiem_vu_hoa", STEEL, 3, [(3, 3, W), (12, 5, W)])
def _(d):  # mua kiem roi thang
    for x in (2, 7, 12):
        d.rectangle([x, 1, x + 1, 9], fill=255); d.polygon([(x, 10), (x + 1, 10), (x, 14)], fill=255)
    d.rectangle([4, 4, 5, 12], fill=255); d.rectangle([9, 5, 10, 13], fill=255)

@icon("thien_kiem_giang", GOLD, 4, [(7, 2, W), (8, 5, W)])
def _(d):  # kiem khong lo + chum sang
    d.polygon([(7, 0), (9, 0), (10, 11), (8, 15), (6, 11)], fill=255)
    d.rectangle([3, 11, 12, 12], fill=255)
    d.polygon([(0, 4), (4, 7), (0, 9)], fill=255); d.polygon([(15, 4), (11, 7), (15, 9)], fill=255)

@icon("thanh_tam_chu", GREEN, 5, [(8, 7, W)])
def _(d):  # hoa sen/thap tu
    d.rectangle([6, 2, 9, 13], fill=255); d.rectangle([2, 6, 13, 9], fill=255)

@icon("thien_loi_chu", THUNDER, 6, [(8, 4, W), (7, 8, W)])
def _(d):  # tia set
    d.polygon([(9, 0), (3, 8), (7, 8), (5, 15), (13, 6), (9, 6), (11, 0)], fill=255)

@icon("quang_tran", HOLY, 7, [(8, 8, W)])
def _(d):  # tran phap vong + sao
    d.ellipse([1, 1, 14, 14], fill=255); d.ellipse([4, 4, 11, 11], fill=0)
    d.rectangle([7, 0, 8, 15], fill=255); d.rectangle([0, 7, 15, 8], fill=255)

@icon("thien_phat", HOLY, 8, [(7, 3, W), (8, 6, W)])
def _(d):  # cot sang tu tren xuong + dia
    d.rectangle([5, 0, 10, 10], fill=255); d.ellipse([1, 10, 14, 15], fill=255)

@icon("long_ho_quyen", BRONZE, 9, [(5, 6, W), (6, 6, W)])
def _(d):  # nam dam
    d.rounded_rectangle([3, 4, 12, 11], radius=2, fill=255)
    d.rectangle([4, 11, 11, 14], fill=255); d.rectangle([1, 6, 3, 9], fill=255)

@icon("thiet_son_khao", (150, 150, 165), 10, [(5, 4, W)])
def _(d):  # nui/khien da
    d.polygon([(0, 14), (6, 2), (9, 7), (11, 4), (15, 14)], fill=255)

@icon("ba_the_chan", (170, 130, 80), 11, [(3, 13, W), (12, 13, W)])
def _(d):  # dam dat nut ra
    d.rectangle([5, 1, 10, 7], fill=255); d.polygon([(4, 8), (11, 8), (8, 12)], fill=255)
    line(d, [(0, 14), (5, 12), (4, 15)], 1); line(d, [(15, 14), (10, 12), (11, 15)], 1)
    d.rectangle([0, 14, 15, 15], fill=255)

@icon("pha_thien_quyen", (230, 120, 50), 12, [(8, 5, W), (8, 8, W)])
def _(d):  # nhay len dam xuong: mui ten xuong + vong chan dong
    d.polygon([(3, 1), (12, 1), (12, 7), (15, 7), (8, 14), (1, 7), (3, 7)], fill=255)

@icon("huyet_te_chu", RED, 13, [(7, 4, (255, 170, 170)), (6, 6, (255, 170, 170))])
def _(d):  # giot mau
    d.polygon([(8, 0), (13, 8), (3, 8)], fill=255); d.ellipse([2, 5, 13, 15], fill=255)

@icon("ma_anh", PURPLE, 14, [(6, 6, (230, 160, 255)), (10, 6, (230, 160, 255))])
def _(d):  # bong ma
    d.ellipse([3, 1, 12, 9], fill=255); d.rectangle([3, 5, 12, 14], fill=255)
    for x in (3, 7, 11): d.polygon([(x, 14), (x + 2, 14), (x + 1, 15)], fill=255)

@icon("huyet_te_dai", RED, 15, [(8, 6, (255, 190, 190))])
def _(d):  # dai te: ban tho + lua mau
    d.rectangle([2, 9, 13, 11], fill=255); d.rectangle([4, 12, 5, 15], fill=255); d.rectangle([10, 12, 11, 15], fill=255)
    d.polygon([(8, 0), (12, 8), (4, 8)], fill=255)

@icon("thon_thien_ma_cong", PURPLE, 16, [(8, 7, (240, 170, 255))])
def _(d):  # xoay nuot
    d.ellipse([1, 1, 14, 14], fill=255); d.ellipse([4, 4, 11, 11], fill=0); d.ellipse([6, 6, 9, 9], fill=255)

@icon("lang_ba_vi_bo", CYAN, 17, [(9, 7, W)])
def _(d):  # dau chan / song luot
    d.polygon([(0, 8), (6, 3), (6, 6), (15, 6), (15, 10), (6, 10), (6, 13)], fill=255)

@icon("ho_than_cuong_khi", CYAN, 18, [(5, 4, W), (6, 5, W)])
def _(d):  # khien
    d.polygon([(1, 2), (14, 2), (14, 8), (8, 15), (1, 8)], fill=255)

@icon("hoi_xuan_thuat", GREEN, 19, [(8, 6, W)])
def _(d):  # la + dau cong
    d.polygon([(8, 0), (14, 6), (11, 14), (8, 15), (5, 14), (2, 6)], fill=255)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, fn in ICONS.items():
        fn().save(os.path.join(OUT_DIR, name + ".png"), "PNG")
    print("wrote", len(ICONS), "book skill icons ->", OUT_DIR)


if __name__ == "__main__":
    main()
