"""gen_step10.py - pixel art cho mod hukhong (16x16 RGBA, khong anti-alias).

A) 8 Ban Menh Kiem  -> textures/item/ban_menh/*.png
B) 6 Icon ky nang   -> textures/gui/skill/*.png
Shading 3-4 tong, vien toi 1px (OUTLINE) bao quanh hinh.
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = r"D:\mc_mod\src\main\resources\assets\hukhong\textures"
ITEM_DIR = os.path.join(ROOT, "item", "ban_menh")
SKILL_DIR = os.path.join(ROOT, "gui", "skill")
W = H = 16
OUTLINE = (20, 16, 28, 255)


def rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def region(pts):
    """Pixel nguyen (khong AA) ben trong da giac."""
    m = Image.new("L", (W, H), 0)
    ImageDraw.Draw(m).polygon(pts, fill=255)
    return {(x, y) for y in range(H) for x in range(W) if m.getpixel((x, y))}


def wedge(p0, p1, w):
    """Vet chem thon: rong w o p0, nhon dan den p1."""
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    L = math.hypot(dx, dy)
    nx, ny = -dy / L * w, dx / L * w
    return [(p0[0] + nx, p0[1] + ny), (p0[0] - nx, p0[1] - ny), p1]


class Art:
    def __init__(self):
        self.c = {}

    def paint(self, cells, col):
        if isinstance(col, str):
            col = rgb(col)
        for p in cells:
            if 0 <= p[0] < W and 0 <= p[1] < H:
                self.c[p] = col

    def outline(self):
        add = set()
        for (x, y) in self.c:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + dx, y + dy)
                if 0 <= q[0] < W and 0 <= q[1] < H and q not in self.c:
                    add.add(q)
        for q in add:
            self.c[q] = OUTLINE

    def save(self, path):
        img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        px = img.load()
        for (x, y), col in self.c.items():
            px[x, y] = col
        img.save(path)
        return self.preview()

    def preview(self):
        keys = {}
        rows = []
        for y in range(H):
            line = ""
            for x in range(W):
                col = self.c.get((x, y))
                if col is None:
                    line += "."
                else:
                    if col not in keys:
                        keys[col] = "kabcdefghijlmnopqrstuvwxyz"[len(keys) % 25]
                    line += keys[col]
            rows.append(line)
        return "\n".join(rows)


# ---------------------------------------------------------------- A) Ban Menh
# Kiem cheo: luoi chay theo duong x+y = S (tu goc duoi-trai len tren-phai),
# tay cam + co kiem + pommel. Moi nguyen to chi thay doi palette va diem nhan.
SWORDS = {
    "thanh_phong": dict(  # xanh gio, nhe
        hi="e6fff7", mid="8fe3d0", lo="3fa894", deep="1f6e63",
        gl="a8f0e0", gd="2e8a7a", hd="3a2a1c", hl="6b4a2b", pm="2e8a7a",
        spots=[(10, 0, "d9fff6"), (8, 1, "d9fff6"), (6, 2, "bff5e8"),
               (13, 1, "ffffff")]),
    "liet_diem": dict(  # do cam lua
        hi="fff6b0", mid="ffb020", lo="e8501a", deep="8a1e10",
        gl="ff7a2a", gd="7a2410", hd="3a2a1c", hl="6b4a2b", pm="8a1e10",
        spots=[(11, 0, "ffd27a"), (7, 3, "ffd27a"), (2, 7, "ffb020"),
               (13, 1, "ffffff")]),
    "han_bang": dict(  # bang lam trang
        hi="f4feff", mid="9fe6ff", lo="3d8fd6", deep="1b3f7a",
        gl="cfefff", gd="2d5f9a", hd="2a3a4a", hl="5a7a9a", pm="2d5f9a",
        spots=[(9, 2, "ffffff"), (6, 5, "ffffff"), (12, 8, "ffffff"),
               (3, 7, "9fe6ff")]),
    "loi_dinh": dict(  # tim vang set
        hi="b98cff", mid="7a3fd0", lo="4a2490", deep="2b145e",
        gl="ffd23f", gd="a8741a", hd="2a1a3a", hl="5a3a6a", pm="ffd23f",
        spots=[(13, 2, "ffe866"), (11, 4, "ffe866"), (9, 6, "ffe866"),
               (7, 8, "ffe866"), (4, 6, "ffe866"), (12, 0, "ffe866")]),
    "huyet_anh": dict(  # do mau sam
        hi="ff6b6b", mid="a3121c", lo="5c0a12", deep="2a0508",
        gl="c62828", gd="4a0d12", hd="2a0a0a", hl="4a1a1a", pm="7a0f18",
        spots=[(8, 11, "ff2d2d"), (8, 12, "c4141c"), (12, 9, "ff2d2d"),
               (10, 3, "ff2d2d")]),
    "trong_nhac": dict(  # luoi day nang, da/dong
        hi="e8e4dc", mid="9a9590", lo="5e5a57", deep="34312f",
        gl="c08a3e", gd="7a5322", hd="3a2a1a", hl="5a3d24", pm="5e5a57",
        spots=[(2, 9, "f2d7a6"), (6, 13, "f2d7a6"), (10, 4, "5e5a57"),
               (8, 6, "5e5a57")],
        heavy=True),
    "ngoc_tam": dict(  # ngoc bich xanh luc
        hi="d8ffe6", mid="2fbf71", lo="17744a", deep="0c4a2e",
        gl="7fe0a8", gd="2b7a52", hd="1f3a2a", hl="3a5a44", pm="2b7a52",
        spots=[(4, 11, "39ff8a"), (10, 2, "d8ffe6"), (7, 5, "d8ffe6")]),
    "tinh_than": dict(  # den tim, dom sao
        hi="b8a8ff", mid="4a3a7a", lo="2a1f4d", deep="15102b",
        gl="8a7ad0", gd="2a1f4d", hd="1a1426", hl="3a2a4a", pm="8a7ad0",
        spots=[(13, 2, "ffffff"), (12, 4, "9ff6ff"), (9, 7, "ffffff"),
               (6, 9, "9ff6ff")]),
}


def sword(key, p):
    a = Art()
    hw = 2 if p.get("heavy") else 1
    S = 15
    guard = {(x, y) for x in range(W) for y in range(H)
             if -8 <= x - y <= -6 and 9 <= x + y <= 21}
    grip = {(x, y) for x in range(W) for y in range(H)
            if 12 <= y <= 14 and 14 <= x + y <= 16}
    blade = {(x, y) for x in range(W) for y in range(0, 11)
             if abs(x + y - S) <= hw}

    a.paint(guard, p["gd"])
    a.paint({c for c in guard if c[0] - c[1] == -7}, p["gl"])
    for c in grip:
        a.paint([c], p["hl"] if c[0] + c[1] == S else p["hd"])
    a.paint([(0, 15), (1, 15)], p["pm"])
    for c in blade:
        off = c[0] + c[1] - S
        if off < 0:
            col = p["hi"]
        elif off == 0:
            col = p["mid"]
        elif off == 1:
            col = p["lo"]
        else:
            col = p["deep"]
        a.paint([c], col)
    for x, y, h in p["spots"]:
        a.paint([(x, y)], h)
    a.outline()
    return a.save(os.path.join(ITEM_DIR, key + ".png"))


# ---------------------------------------------------------------- B) Skill
def icon_kiem_1():  # vet kiem khi bac lam
    a = Art()
    a.paint(region(wedge((14, 1), (2, 13), 2.6)), "4f7fc2")
    a.paint(region(wedge((13, 2), (3, 12), 1.4)), "9fd4ff")
    a.paint(region(wedge((12, 3), (5, 10), 0.6)), "f2fbff")
    a.outline()
    return a.save(os.path.join(SKILL_DIR, "kiem_1.png"))


def icon_kiem_2():  # 3 vet chem
    a = Art()
    for p0, p1 in [((6, 0), (0, 6)), ((11, 1), (3, 9)), ((15, 3), (7, 11))]:
        a.paint(region(wedge(p0, p1, 1.1)), "4f7fc2")
        a.paint(region(wedge(p0, p1, 0.4)), "bfe6ff")
    a.outline()
    return a.save(os.path.join(SKILL_DIR, "kiem_2.png"))


def icon_kiem_ult():  # vong kiem vuc
    a = Art()
    cx = cy = 7.5
    ring = {(x, y) for x in range(W) for y in range(H)
            if 4.6 <= math.hypot(x - cx, y - cy) <= 6.8}
    for c in ring:
        s = c[0] + c[1]
        a.paint([c], "bfe6ff" if s <= 14 else "5a8fd0" if s < 19 else "2d4f8a")
    a.paint({(7, 7), (8, 7), (7, 8), (8, 8)}, "e6f7ff")
    a.paint([(1, 1), (14, 1), (1, 14), (14, 14)], "e6f7ff")
    a.outline()
    return a.save(os.path.join(SKILL_DIR, "kiem_ult.png"))


def icon_ma_1():  # vuot mau
    a = Art()
    for p0, p1 in [((9, 0), (2, 7)), ((12, 3), (5, 10)), ((15, 6), (8, 13))]:
        a.paint(region(wedge(p0, p1, 1.9)), "8e0c18")
        a.paint(region(wedge(p0, p1, 1.0)), "d6283a")
        a.paint(region(wedge(p0, p1, 0.4)), "ff6b6b")
        a.paint([(p1[0], p1[1] + 1)], "ff3b3b")  # giot mau
    a.outline()
    return a.save(os.path.join(SKILL_DIR, "ma_1.png"))


def icon_ma_2():  # khien ma khi do den
    a = Art()
    outer = region([(2, 1), (13, 1), (14, 7), (8, 15), (1, 7)])
    inner = region([(4, 3), (11, 3), (12, 7), (8, 13), (4, 7)])
    a.paint(outer, "5a0a14")
    a.paint({c for c in inner if c[0] <= 7}, "b3202a")
    a.paint({c for c in inner if c[0] >= 8}, "3a0a10")
    a.paint({c for c in inner if c[0] <= 6 and c[1] <= 4}, "e0505a")
    a.paint({(7, 5), (8, 5), (7, 8), (8, 8)}, "ff5a3c")
    a.paint({(7, 6), (8, 6), (7, 7), (8, 7)}, "ff9a3c")
    a.outline()
    return a.save(os.path.join(SKILL_DIR, "ma_2.png"))


def icon_ma_ult():  # ma than sung do
    a = Art()
    horn_l = region([(3, 5), (0, 0), (4, 2), (6, 5)])
    horn_r = region([(12, 5), (15, 0), (11, 2), (9, 5)])
    a.paint(horn_l, "c9302c")
    a.paint(horn_r, "c9302c")
    a.paint({c for c in horn_r if c[1] <= 2}, "ff8a6a")
    head = region([(4, 3), (11, 3), (13, 7), (12, 12), (9, 15),
                   (6, 15), (3, 12), (2, 7)])
    a.paint(head, "b3202a")
    a.paint({c for c in head if c[0] >= 9}, "6e0f18")
    a.paint({c for c in head if c[1] <= 4 and c[0] <= 6}, "e0505a")
    a.paint(region([(3, 6), (7, 7), (6, 10), (4, 9)]), "ffd23a")
    a.paint(region([(12, 6), (8, 7), (9, 10), (11, 9)]), "ffd23a")
    a.paint(region([(5, 12), (10, 12), (10, 13), (5, 13)]), "2a0508")
    a.paint([(6, 12), (9, 12)], "ffffff")
    a.outline()
    return a.save(os.path.join(SKILL_DIR, "ma_ult.png"))


def main():
    os.makedirs(ITEM_DIR, exist_ok=True)
    os.makedirs(SKILL_DIR, exist_ok=True)
    for key, p in SWORDS.items():
        print(key)
        print(sword(key, p))
    icons = [icon_kiem_1, icon_kiem_2, icon_kiem_ult,
             icon_ma_1, icon_ma_2, icon_ma_ult]
    for fn in icons:
        print(fn.__name__)
        print(fn())


if __name__ == "__main__":
    main()
