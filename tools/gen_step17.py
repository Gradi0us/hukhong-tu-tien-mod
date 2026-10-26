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


def bone_sprite(col, seed):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    pts = []
    for i in range(3, 13):
        pts += [(i, 15 - i), (i, 14 - i)]
    pts += [(2, 12), (3, 13), (2, 13), (3, 12), (12, 2), (13, 3), (12, 3), (13, 2)]
    for x, y in pts:
        px[x, y] = shade(col, 1.0 if (x + y) % 3 else 1.2)
    rnd = random.Random(seed)
    for _ in range(4):
        x, y = rnd.randint(4, 11), rnd.randint(4, 11)
        if px[x, y][3]:
            px[x, y] = shade(col, 0.6)
    return outline(im, N4)


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
        bone_sprite(col, 1700 + i).save(A / "textures" / "item" / f"hon_cot_{bid}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"hukhong:item/hon_cot_{bid}"}}
        (A / "models" / "item" / f"hon_cot_{bid}.json").write_text(json.dumps(model), encoding="utf-8")
    for k in ("kiem", "dao", "the", "ma", "bone"):
        icon(k).save(A / "textures" / "gui" / "skill" / f"bm_{k}.png")
    print("ok", len(BONES), "bones + 5 icons")


if __name__ == "__main__":
    main()
