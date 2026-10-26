#!/usr/bin/env python3
"""Buoc 17: texture + model item cho 12 Hon Cot va 5 icon ky nang R (Ban Menh x4 + Van Cot).

  textures/item/hon_cot_<id>.png (16x16), models/item/hon_cot_<id>.json
  textures/gui/skill/bm_{kiem,dao,the,ma,bone}.png (16x16)

Chay: python tools/gen_step17.py   (tu D:\\mc_mod)
"""
import json
import random
from pathlib import Path

from PIL import Image

import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixel_shade import mask, render

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "src" / "main" / "resources" / "assets" / "hukhong"
OUTLINE = (20, 14, 22, 255)

BONES = {
    "thien_muc": (150, 120, 220), "tinh_tam": (110, 150, 230),
    "long_hon": (230, 180, 60), "hu_khong_hach": (120, 60, 170),
    "loi_chuong": (90, 200, 240), "ho_the": (170, 170, 190),
    "huyet_trao": (200, 40, 50), "kiem_y": (230, 230, 245),
    "phong_hanh": (110, 220, 150), "cu_tuong": (150, 110, 70),
    "anh_thiem": (80, 80, 120), "hoi_sinh": (240, 130, 160),
}
N4 = [(-1, 0), (1, 0), (0, -1), (0, 1)]
N8 = N4 + [(-1, -1), (1, -1), (-1, 1), (1, 1)]


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c) + (255,)


def outline(im, neigh):
    px = im.load()
    out = im.copy()
    op = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0:
                for dx, dy in neigh:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3]:
                        op[x, y] = OUTLINE
                        break
    return out


def _poly(pts):
    return lambda d: d.polygon(pts, fill=255)


def _ell(*boxes):
    def f(d):
        for bx in boxes:
            d.ellipse(bx, fill=255)
    return f


def _crescent(d):
    d.ellipse((2, 2, 13, 13), fill=255)
    d.ellipse((6, 0, 17, 11), fill=0)


def _heart(d):
    d.ellipse((2, 3, 8, 9), fill=255)
    d.ellipse((7, 3, 13, 9), fill=255)
    d.polygon([(2, 7), (13, 7), (8, 14)], fill=255)


def _femur(d):
    d.rectangle((4, 5, 11, 10), fill=255)
    for bx in ((1, 3, 5, 7), (1, 8, 5, 12), (10, 3, 14, 7), (10, 8, 14, 12)):
        d.ellipse(bx, fill=255)


def _claws(d):
    for pts in ([(1, 2), (4, 2), (3, 13)], [(6, 1), (9, 1), (8, 15)], [(11, 2), (14, 2), (13, 13)]):
        d.polygon(pts, fill=255)


def _blade(d):
    d.polygon([(15, 0), (15, 3), (8, 10), (5, 7)], fill=255)
    d.line([(2, 14), (6, 10)], fill=255, width=2)
    d.line([(3, 9), (9, 15)], fill=255, width=1)


# id -> (ham ve mat na, accents) : moi Hon Cot mot hinh dang rieng
WHITE, DARK, GOLD = (255, 255, 255), (20, 10, 30), (255, 220, 120)
SHAPES = {
    "thien_muc": (_poly([(0, 8), (4, 4), (8, 2), (12, 4), (15, 8), (12, 12), (8, 14), (4, 12)]),
                  [(7, 7, DARK), (8, 7, DARK), (7, 8, DARK), (8, 8, DARK), (6, 6, WHITE), (9, 6, (230, 200, 255))]),
    "tinh_tam": (_ell((2, 2, 13, 13)), [(7, 5, WHITE), (8, 5, WHITE), (6, 6, WHITE), (7, 8, GOLD), (8, 8, GOLD), (7, 9, GOLD), (8, 9, GOLD)]),
    "long_hon": (_poly([(3, 15), (4, 9), (6, 5), (9, 2), (13, 0), (12, 4), (11, 8), (9, 15)]),
                 [(5, 12, GOLD), (6, 9, GOLD), (8, 6, WHITE), (10, 3, WHITE)]),
    "hu_khong_hach": (_ell((1, 1, 14, 14)), [(x, y, DARK) for x in range(6, 10) for y in range(6, 10)]
                      + [(5, 5, (190, 130, 255)), (10, 10, (190, 130, 255)), (4, 8, (190, 130, 255)), (11, 7, (190, 130, 255))]),
    "loi_chuong": (_poly([(9, 0), (3, 8), (7, 8), (5, 15), (13, 6), (8, 6), (11, 0)]),
                   [(9, 1, WHITE), (7, 7, WHITE), (11, 2, WHITE)]),
    "ho_the": (_poly([(3, 1), (12, 1), (14, 8), (8, 15), (1, 8)]),
               [(x, 8, WHITE) for x in range(5, 11)] + [(7, y, WHITE) for y in range(4, 12)] + [(8, y, WHITE) for y in range(4, 12)]),
    "huyet_trao": (_claws, [(3, 3, (255, 150, 150)), (8, 2, (255, 150, 150)), (13, 3, (255, 150, 150))]),
    "kiem_y": (_blade, [(14, 1, WHITE), (13, 2, WHITE), (12, 3, WHITE), (2, 14, (150, 100, 60)), (3, 13, (150, 100, 60))]),
    "phong_hanh": (_poly([(0, 11), (2, 5), (7, 1), (14, 2), (10, 5), (14, 7), (9, 9), (11, 13), (4, 13)]),
                   [(4, 6, WHITE), (7, 4, WHITE), (10, 3, WHITE)]),
    "cu_tuong": (_femur, [(3, 6, (220, 200, 160)), (2, 4, (220, 200, 160)), (12, 5, (220, 200, 160))]),
    "anh_thiem": (_crescent, [(4, 5, (170, 170, 255)), (3, 8, (170, 170, 255)), (4, 11, (170, 170, 255))]),
    "hoi_sinh": (_heart, [(4, 5, WHITE), (5, 4, WHITE), (9, 4, (255, 220, 230))]),
}


def bone_sprite(col, seed, bid):
    fn, acc = SHAPES[bid]
    return render(mask(fn), col, seed, accents=acc)


def icon(kind):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    palette = {"kiem": (240, 240, 255), "dao": (255, 230, 120), "the": (210, 130, 60),
               "ma": (200, 30, 50), "bone": (120, 220, 255)}[kind]
    c1, c2 = shade(palette, 1.0), shade(palette, 0.6)
    if kind == "kiem":
        for i in range(2, 14):
            px[i, 15 - i] = c1
            px[i, 14 - i] = c2
    elif kind == "dao":
        for y in range(1, 15):
            px[7, y] = c1
            px[8, y] = c1
        for x in range(4, 12):
            px[x, 6] = c2
            px[x, 7] = c2
    elif kind == "the":
        for y in range(5, 14):
            for x in range(3, 13):
                if (x - 7.5) ** 2 / 25 + (y - 9) ** 2 / 20 <= 1:
                    px[x, y] = c1 if (x + y) % 2 else c2
    elif kind == "ma":
        for y in range(3, 14):
            for x in range(3, 13):
                if (x - 7.5) ** 2 + (y - 8.5) ** 2 <= 20 and (x + y) % 5:
                    px[x, y] = c1 if y < 8 else c2
    else:
        for i in range(3, 13):
            px[i, 15 - i] = c1
            px[i, 14 - i] = c2
        for x, y in [(2, 12), (3, 13), (12, 2), (13, 3)]:
            px[x, y] = c1
    return outline(im, N8)


def main():
    (A / "textures" / "item").mkdir(parents=True, exist_ok=True)
    (A / "models" / "item").mkdir(parents=True, exist_ok=True)
    (A / "textures" / "gui" / "skill").mkdir(parents=True, exist_ok=True)
    for i, (bid, col) in enumerate(BONES.items()):
        bone_sprite(col, 1700 + i, bid).save(A / "textures" / "item" / f"hon_cot_{bid}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"hukhong:item/hon_cot_{bid}"}}
        (A / "models" / "item" / f"hon_cot_{bid}.json").write_text(json.dumps(model), encoding="utf-8")
    for k in ("kiem", "dao", "the", "ma", "bone"):
        icon(k).save(A / "textures" / "gui" / "skill" / f"bm_{k}.png")
    print("ok", len(BONES), "bones + 5 icons")


if __name__ == "__main__":
    main()
