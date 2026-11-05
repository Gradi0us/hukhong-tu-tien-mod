"""Sinh texture + model item cho Phap Khi (75 mon = 15 loai x 5 pham).

- 16x16 RGBA, pixel-art, khong anti-alias, vien 1px toi.
- Phu Cong (7): phu giay dung, mau giay theo nguyen to, vien theo pham.
- Phu Ho (4): bua deo tron co day deo, vien theo pham.
- Tran Ban (4): dia tron, vong mau theo loai, vien theo pham.
- Model: models/item/<id>.json (item/generated).

Chay: python D:\\mc_mod\\tools\\gen_phapkhi_textures.py
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir,
                                     "src", "main", "resources", "assets", "hukhong"))
TEX = os.path.join(ROOT, "textures", "item")
MOD = os.path.join(ROOT, "models", "item")

TIERS = [("pham", 0xAAAAAA), ("linh", 0x55FF77), ("huyen", 0x5599FF), ("dia", 0xB060FF), ("thien", 0xFFAA00)]
OUT = (20, 14, 6, 255)

# (id, cat, rgb, glyph 5x5)
KINDS = [
    ("kiem_tinh_phu", "A", 0xBFE4FF, ["....#", "...#.", "#.#..", ".#...", "#.#.."]),
    ("hoa_diem_phu", "A", 0xFF6A2A, ["..#..", ".##..", ".###.", "#####", ".###."]),
    ("loi_dinh_phu", "A", 0x9CC4FF, ["..###", ".##..", ".###.", "..##.", ".#..."]),
    ("han_bang_phu", "A", 0x8FE6FF, ["#.#.#", ".###.", "#####", ".###.", "#.#.#"]),
    ("tho_nham_phu", "A", 0xB08A4F, ["..#..", ".###.", ".###.", "#####", "#####"]),
    ("ma_sat_phu", "A", 0xC01028, [".###.", "#.#.#", ".###.", ".#.#.", "#...#"]),
    ("thanh_quang_phu", "A", 0xFFE066, ["..#..", "#.#.#", ".###.", "#.#.#", "..#.."]),
    ("ho_than_phu", "D", 0xFFD866, ["#####", "#####", "#####", ".###.", "..#.."]),
    ("kim_cang_phu", "D", 0xE0B040, ["..#..", ".###.", "#####", ".###.", "..#.."]),
    ("don_khong_phu", "D", 0x8FE6FF, ["..#..", "...#.", "#####", "...#.", "..#.."]),
    ("hoi_xuan_phu", "D", 0x7FE06A, ["..#..", "..#..", "#####", "..#..", "..#.."]),
    ("tu_linh_tran_ban", "Z", 0x7FE0FF, [".###.", "#...#", "#.#.#", "#...#", ".###."]),
    ("khon_tran_ban", "Z", 0xA050FF, ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"]),
    ("sat_tran_ban", "Z", 0xFF4030, ["#...#", "##.##", ".###.", "##.##", "#...#"]),
    ("ho_tran_ban", "Z", 0xFFD866, ["#####", "#...#", "#.#.#", "#...#", "#####"]),
]


def rgba(v, k=1.0):
    r, g, b = (v >> 16) & 255, (v >> 8) & 255, v & 255
    return (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), 255)


def outline(img):
    px = img.load()
    base = [[px[x, y][3] > 0 for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if base[y][x]:
                continue
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and base[ny][nx]:
                        px[x, y] = OUT


def glyph(img, g, cx, cy, col):
    px = img.load()
    for j, row in enumerate(g):
        for i, c in enumerate(row):
            if c == "#":
                px[cx - 2 + i, cy - 2 + j] = col


def talisman(kind, tier_rgb):
    _, _, rgb, g = kind
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    paper = rgba(0xF2E6B8)
    for y in range(1, 15):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (1, 14)
            px[x, y] = rgba(tier_rgb) if edge else paper
    for x in (6, 9):  # day phu van tren/duoi
        px[x, 2] = rgba(rgb, 0.7)
        px[x, 13] = rgba(rgb, 0.7)
    glyph(img, g, 8, 8, rgba(rgb, 0.85))
    px[5, 3] = rgba(tier_rgb, 0.8)
    px[10, 12] = rgba(tier_rgb, 0.8)
    outline(img)
    return img


def amulet(kind, tier_rgb):
    _, _, rgb, g = kind
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x, y in ((7, 0), (8, 0), (6, 1), (9, 1)):  # day deo
        px[x, y] = rgba(0xC8A060)
    cx, cy, R = 7.5, 8.5, 5.6
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= R:
                px[x, y] = rgba(tier_rgb) if d > R - 1.2 else rgba(rgb, 0.45)
    glyph(img, g, 8, 9, rgba(0xFFFFFF, 0.95))
    outline(img)
    return img


def disc(kind, tier_rgb):
    _, _, rgb, g = kind
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 7.2:
                continue
            if d > 6.0:
                px[x, y] = rgba(tier_rgb)
            elif d > 5.0:
                px[x, y] = rgba(rgb, 0.55)
            elif d > 4.2:
                px[x, y] = rgba(0x303040)
            else:
                px[x, y] = rgba(0x1C1C2A)
    for ang in range(0, 360, 90):  # 4 nut ruong
        x = int(round(cx + 5.5 * math.cos(math.radians(ang))))
        y = int(round(cy + 5.5 * math.sin(math.radians(ang))))
        px[x, y] = rgba(0xFFFFFF, 0.95)
    glyph(img, g, 8, 8, rgba(rgb, 1.0))
    outline(img)
    return img


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MOD, exist_ok=True)
    n = 0
    for kind in KINDS:
        for tid, trgb in TIERS:
            fn = {"A": talisman, "D": amulet, "Z": disc}[kind[1]]
            name = "%s_%s" % (kind[0], tid)
            fn(kind, trgb).save(os.path.join(TEX, name + ".png"))
            with open(os.path.join(MOD, name + ".json"), "w", encoding="utf8") as f:
                json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "hukhong:item/" + name}}, f)
            n += 1
    print("wrote", n, "textures + models to", TEX)


if __name__ == "__main__":
    main()
