#!/usr/bin/env python3
"""2026-11-06: biến thể Hư Không xâm thực cho Yêu Thú thực vật (id_hu): cùng khung xương (dùng chung animation gốc),
thêm tinh thể hư không mọc trên thân/đầu, texture nhuộm tím đen + vân nứt tím phát sáng (vào cả glowmask).
Chạy: python -X utf8 tools/gen_c12_void_models.py   (từ D:\\mc_mod)"""
import random, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _gm2_engine import *
from PIL import Image, ImageDraw
import gen_c11_models as base

VOID = crystal((60, 20, 100), (200, 110, 255))
VOID2 = glow((170, 80, 255))


def add_crystals(m, rng):
    for bone in ("body", "head", "stem"):
        cubes = [c for c in m.cubes if c["bone"] == bone and c["sz"][0] >= 3 and c["sz"][2] >= 3]
        if not cubes: continue
        for i in range(4 if bone == "body" else 3):
            c = rng.choice(cubes)
            (ox, oy, oz), (w, h, d) = c["o"], c["sz"]
            x = ox + rng.uniform(0.5, max(0.6, w - 2)); z = oz + rng.uniform(0.5, max(0.6, d - 2)); y = oy + h - 0.5
            hh = rng.choice((2, 3, 4))
            tilt = [rng.uniform(-25, 25), 0, rng.uniform(-25, 25)]
            m.cube(bone, (x, y, z), (1.5, hh, 1.5), VOID, rot=tilt, pivot=[x + .75, y, z + .75])
            m.cube(bone, (x + .25, y + hh - .5, z + .25), (1, 1, 1), VOID2, rot=tilt, pivot=[x + .75, y, z + .75])


def corrupt_tex(id):
    tp = A / "textures/entity" / f"{id}_hu.png"; gp = A / "textures/entity" / f"{id}_hu_glowmask.png"
    tex = Image.open(tp).convert("RGBA"); W, H = tex.size
    gm = Image.open(gp).convert("RGBA") if gp.exists() else Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = tex.load()
    for y in range(H):
        for x in range(W):
            r, g, b, a = px[x, y]
            if a == 0: continue
            l = (r + g + b) / 3
            px[x, y] = (int(r * .55 + l * .15 + 30), int(g * .45 + l * .1 + 6), int(b * .55 + l * .2 + 45), a)  # nhuộm tím đen
    rng = random.Random(sum(map(ord, id)) * 7)
    d = ImageDraw.Draw(tex); dg = ImageDraw.Draw(gm)
    for _ in range(W // 3):  # vân nứt hư không
        x, y = rng.randrange(W), rng.randrange(H)
        if tex.getpixel((x, y))[3] == 0: continue
        pts = [(x, y)]
        for _ in range(rng.randint(3, 7)):
            x = max(0, min(W - 1, x + rng.choice((-1, 0, 1)))); y = max(0, min(H - 1, y + rng.choice((-1, 1))))
            if tex.getpixel((x, y))[3] == 0: break
            pts.append((x, y))
        if len(pts) > 1:
            d.line(pts, fill=(185, 90, 255, 255)); dg.line(pts, fill=(185, 90, 255, 255))
    tex.save(tp); gm.save(gp)


if __name__ == "__main__":
    for id, fn in base.JOBS:
        m, c = fn()
        m.id = id + "_hu"
        add_crystals(m, random.Random(sum(map(ord, id))))
        m.build()
        corrupt_tex(id)
        print("built", m.id, len(m.cubes), "cubes")
