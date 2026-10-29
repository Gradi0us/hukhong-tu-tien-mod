#!/usr/bin/env python3
"""Dot 4b: tai nguyen bien doi ngoai hinh boss theo pha (idempotent, chay lai bat cu luc nao, SAU cac gen_* khac).

  1. geo/entity/<id>.geo.json : them 2 xuong phu kien (xoa roi them lai neu da co)
        ph2_spikes (parent body, hien pha 2-3): 5 gai lung + 2 gai vai
        ph3_halo   (parent head, hien pha 3)  : vanh 8 manh noi quanh dau
     UV tro vao vung trong cua atlas (v >= 200): (0,200) = vat lieu dac, (64,200) = vung phat sang.
  2. textures/entity/<id>_p2.png, _p3.png (+ _glowmask) : ban tint theo mau chu de cua boss (giu do sang), ve them 2 vung UV tren.
     Renderer (PhasedBossRenderer) tu chon <id>_p<pha>.png neu co; glowmask suy ra tu ten texture.
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "hukhong")
GEO = os.path.join(ROOT, "geo", "entity")
TEX = os.path.join(ROOT, "textures", "entity")

# id -> (mau chu de rgb)  -- khop BossPhases.java (Pal)
BOSSES = {
    "hu_vuong": (0xA0, 0x50, 0xFF),
    "sa_hat_vuong": (0xE0, 0xB0, 0x60),
    "bang_nhan": (0x9F, 0xE8, 0xFF),
    "doc_mau": (0x7B, 0xD0, 0x4A),
    "chg_1": (0xE0, 0xB0, 0x60),
    "chg_2": (0xFF, 0xE9, 0xA0),
    "chg_3": (0x8F, 0xA8, 0xFF),
    "chg_4": (0xC0, 0x10, 0x28),
    "da_khuyet": (0x8F, 0xA8, 0xFF),
}
SOLID_UV = [0, 200]
GLOW_UV = [64, 200]


def bbox(bone):
    xs, ys, zs = [], [], []
    for c in bone.get("cubes", []):
        o, s = c["origin"], c["size"]
        xs += [o[0], o[0] + s[0]]
        ys += [o[1], o[1] + s[1]]
        zs += [o[2], o[2] + s[2]]
    return (min(xs), max(xs), min(ys), max(ys), min(zs), max(zs))


def spikes(bone):
    x0, x1, y0, y1, z0, z1 = bbox(bone)
    cx = (x0 + x1) / 2
    cubes = []
    for i in range(5):  # 5 gai lung, nhon ve phia sau (+z)
        y = y0 + (y1 - y0) * (0.18 + 0.16 * i)
        cubes.append({"origin": [cx - 1, round(y, 2), z1 - 0.5], "size": [2, 2, 5], "uv": SOLID_UV})
    top = y1 - 3
    cubes.append({"origin": [x0 - 4, round(top, 2), (z0 + z1) / 2 - 1], "size": [4, 2, 2], "uv": SOLID_UV})  # vai trai (-x)
    cubes.append({"origin": [x1, round(top, 2), (z0 + z1) / 2 - 1], "size": [4, 2, 2], "uv": SOLID_UV})      # vai phai (+x)
    return {"name": "ph2_spikes", "parent": "body", "pivot": [cx, round(y0, 2), z1], "cubes": cubes}


def halo(bone, parent):
    x0, x1, y0, y1, z0, z1 = bbox(bone)
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    r = max(x1 - x0, z1 - z0) / 2 + 5
    y = y1 + 3
    cubes = []
    for i in range(8):
        a = i * math.tau / 8
        px, pz = cx + r * math.cos(a), cz + r * math.sin(a)
        cubes.append({"origin": [round(px - 1.5, 3), round(y, 2), round(pz - 0.5, 3)], "size": [3, 7, 1], "uv": GLOW_UV,
                      "pivot": [round(px, 3), round(y + 3.5, 2), round(pz, 3)],
                      "rotation": [0, round(90 - math.degrees(a), 2), 0]})
    return {"name": "ph3_halo", "parent": parent, "pivot": [cx, round(y, 2), cz], "cubes": cubes}


def patch_geo(bid):
    path = os.path.join(GEO, bid + ".geo.json")
    with open(path, encoding="utf-8") as f:
        d = json.load(f)
    g = d["minecraft:geometry"][0]
    bones = [b for b in g["bones"] if b["name"] not in ("ph2_spikes", "ph3_halo")]
    byname = {b["name"]: b for b in bones}
    body = byname["body"]
    head = byname.get("head", body)
    bones.append(spikes(body))
    bones.append(halo(head, "head" if "head" in byname else "body"))
    g["bones"] = bones
    with open(path, "w", encoding="utf-8") as f:
        json.dump(d, f, indent=1, ensure_ascii=False)
        f.write("\n")


def colorize(img, tint, m, dark=1.0):
    px = img.load()
    w, h = img.size
    tr, tg, tb = (c / 255.0 for c in tint)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            luma = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
            k = min(1.6, luma * 1.35 + 0.08)
            cr, cg, cb = tr * k * 255, tg * k * 255, tb * k * 255
            r = (1 - m) * r + m * cr
            g = (1 - m) * g + m * cg
            b = (1 - m) * b + m * cb
            px[x, y] = (int(min(255, r * dark)), int(min(255, g * dark)), int(min(255, b * dark)), a)


def paint(img, box, color):
    px = img.load()
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            edge = x in (x0, x1 - 1) or y in (y0, y1 - 1)
            f = 0.75 if edge else (1.0 if (x + y) % 5 else 0.9)
            px[x, y] = (int(color[0] * f), int(color[1] * f), int(color[2] * f), 255)


def make_tex(bid, tint):
    base = Image.open(os.path.join(TEX, bid + ".png")).convert("RGBA")
    glow = Image.open(os.path.join(TEX, bid + "_glowmask.png")).convert("RGBA")
    for ph, m, dark in ((2, 0.30, 1.0), (3, 0.55, 0.92)):
        t = base.copy()
        colorize(t, tint, m, dark)
        paint(t, (0, 200, 64, 256), tuple(int(c * 0.35) for c in tint))
        paint(t, (64, 200, 128, 256), tint)
        t.save(os.path.join(TEX, f"{bid}_p{ph}.png"))
        gm = glow.copy()
        colorize(gm, tint, m + 0.15, 1.0)
        paint(gm, (64, 200, 128, 256), tuple(min(255, int(c * 1.15)) for c in tint))
        gm.save(os.path.join(TEX, f"{bid}_p{ph}_glowmask.png"))


def main():
    for bid, tint in BOSSES.items():
        patch_geo(bid)
        make_tex(bid, tint)
        print("ok", bid)


if __name__ == "__main__":
    sys.exit(main())
