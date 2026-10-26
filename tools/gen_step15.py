#!/usr/bin/env python3
"""Buoc 15: sinh tai nguyen GeckoLib 4 cho boss the gioi "Hu Vuong" (hu_vuong).

  geo/entity/hu_vuong.geo.json              (Bedrock geometry 1.12.0)
  animations/entity/hu_vuong.animation.json (1.8.0: idle / walk / attack / slam / summon / roar)
  textures/entity/hu_vuong.png              (128x128)
  textures/entity/hu_vuong_glowmask.png     (128x128: mat + vet nut sang)

Chay: python tools/gen_step15.py   (tu D:\\mc_mod)
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

TEX = 128
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hukhong"
GLOW = (214, 120, 255)
SOCKET = (12, 6, 22)

# (bone, parent, pivot, origin, size, colour)  -- don vi model (16 = 1 block)
CUBES = [
    ("body", None, [0, 42, 0], [-9, 20, -5], [18, 22, 10], (52, 34, 78)),
    ("head", None, [0, 42, 0], [-6, 42, -6], [12, 12, 12], (70, 48, 100)),
    ("horn_r", "head", [-4, 54, 0], [-5, 54, -1], [2, 9, 2], (200, 190, 210)),
    ("horn_l", "head", [4, 54, 0], [3, 54, -1], [2, 9, 2], (200, 190, 210)),
    ("right_arm", None, [-12, 40, 0], [-15, 18, -4], [6, 24, 8], (60, 40, 88)),
    ("left_arm", None, [12, 40, 0], [9, 18, -4], [6, 24, 8], (60, 40, 88)),
    ("right_leg", None, [-4.5, 20, 0], [-8, 0, -4], [7, 20, 8], (40, 26, 62)),
    ("left_leg", None, [4.5, 20, 0], [1, 0, -4], [7, 20, 8], (40, 26, 62)),
]


def pack():
    """Xep UV hop (shelf packing). Tra ve {bone: (u, v)}."""
    uv, x, y, rowh = {}, 0, 0, 0
    for name, _, _, _, (w, h, d), _ in CUBES:
        bw, bh = int(2 * (w + d)), int(d + h)
        if x + bw > TEX:
            x, y, rowh = 0, y + rowh, 0
        uv[name] = (x, y)
        x += bw
        rowh = max(rowh, bh)
    assert y + rowh <= TEX
    return uv


def faces(u, v, w, h, d):
    """Bedrock box UV -> {face: (x, y, fw, fh)}."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


SHADE = {"top": 1.15, "front": 1.0, "right": 0.85, "left": 0.85, "back": 0.75, "bottom": 0.6}


def geo():
    uv = pack()
    bones = []
    for name, parent, pivot, origin, size, _ in CUBES:
        b = {"name": name, "pivot": pivot, "cubes": [{"origin": origin, "size": size, "uv": list(uv[name])}]}
        if parent:
            b["parent"] = parent
        bones.append(b)
    return uv, {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": "geometry.hu_vuong", "texture_width": TEX, "texture_height": TEX,
                            "visible_bounds_width": 4.5, "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.8, 0]},
            "bones": bones}],
    }


def textures(uv):
    rnd = random.Random(15)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    px, gp = img.load(), glow.load()
    for name, _, _, _, (w, h, d), col in CUBES:
        u, v = uv[name]
        for face, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for x in range(fx, fx + fw):
                for y in range(fy, fy + fh):
                    n = rnd.uniform(0.82, 1.1)
                    s = SHADE[face] * n
                    px[x, y] = tuple(max(0, min(255, int(c * s))) for c in col) + (255,)
        # vet nut hu khong sang tren than + tay (mat truoc)
        if name in ("body", "right_arm", "left_arm"):
            fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
            x, y = fx + fw // 2, fy
            for _ in range(fh):
                x = max(fx, min(fx + fw - 1, x + rnd.choice((-1, 0, 0, 1))))
                px[x, y] = (150, 80, 220, 255)
                gp[x, y] = GLOW + (255,)
                y += 1
                if y >= fy + fh:
                    break
        if name == "head":
            fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
            for ex in (fx + 2, fx + fw - 4):
                for dx in range(2):
                    for dy in range(2):
                        px[ex + dx, fy + 4 + dy] = GLOW + (255,)
                        gp[ex + dx, fy + 4 + dy] = GLOW + (255,)
                px[ex, fy + 3] = SOCKET + (255,)
                px[ex + 1, fy + 3] = SOCKET + (255,)
    return img, glow


# ---------------------------------------------------------------- animations
def kf(pairs):
    return {str(t): v for t, v in pairs}


def anim(length, loop, bones):
    return {"loop": loop, "animation_length": length, "bones": bones}


def rot(pairs):
    return {"rotation": kf(pairs)}


def build_animations():
    A = {}
    A["animation.hu_vuong.idle"] = anim(4.0, True, {
        "body": {"position": kf([(0, [0, 0, 0]), (2.0, [0, 0.4, 0]), (4.0, [0, 0, 0])])},
        "head": rot([(0, [0, 0, 0]), (2.0, [2, 0, 0]), (4.0, [0, 0, 0])]),
        "right_arm": rot([(0, [0, 0, 3]), (2.0, [2, 0, 5]), (4.0, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (2.0, [2, 0, -5]), (4.0, [0, 0, -3])]),
    })
    A["animation.hu_vuong.walk"] = anim(1.2, True, {
        "right_leg": rot([(0, [30, 0, 0]), (0.6, [-30, 0, 0]), (1.2, [30, 0, 0])]),
        "left_leg": rot([(0, [-30, 0, 0]), (0.6, [30, 0, 0]), (1.2, [-30, 0, 0])]),
        "right_arm": rot([(0, [-20, 0, 4]), (0.6, [20, 0, 4]), (1.2, [-20, 0, 4])]),
        "left_arm": rot([(0, [20, 0, -4]), (0.6, [-20, 0, -4]), (1.2, [20, 0, -4])]),
        "body": {"position": kf([(0, [0, 0, 0]), (0.3, [0, 0.8, 0]), (0.6, [0, 0, 0]), (0.9, [0, 0.8, 0]), (1.2, [0, 0, 0])])},
    })
    A["animation.hu_vuong.attack"] = anim(0.6, False, {
        "right_arm": rot([(0, [0, 0, 3]), (0.2, [-110, 0, 0]), (0.35, [40, 0, 0]), (0.6, [0, 0, 3])]),
        "body": rot([(0, [0, 0, 0]), (0.2, [-8, 0, 0]), (0.35, [12, 0, 0]), (0.6, [0, 0, 0])]),
    })
    # slam: gio hai tay len (windup 1.5s ~ 30 tick) roi nen xuong
    A["animation.hu_vuong.slam"] = anim(1.6, False, {
        "right_arm": rot([(0, [0, 0, 3]), (1.0, [-170, 0, 10]), (1.15, [-170, 0, 10]), (1.35, [20, 0, 0]), (1.6, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (1.0, [-170, 0, -10]), (1.15, [-170, 0, -10]), (1.35, [20, 0, 0]), (1.6, [0, 0, -3])]),
        "body": rot([(0, [0, 0, 0]), (1.0, [-18, 0, 0]), (1.15, [-18, 0, 0]), (1.35, [25, 0, 0]), (1.6, [0, 0, 0])]),
        "head": rot([(0, [0, 0, 0]), (1.0, [-20, 0, 0]), (1.35, [20, 0, 0]), (1.6, [0, 0, 0])]),
    })
    A["animation.hu_vuong.summon"] = anim(1.6, False, {
        "right_arm": rot([(0, [0, 0, 3]), (0.6, [-20, 0, 85]), (1.2, [-20, 0, 85]), (1.6, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (0.6, [-20, 0, -85]), (1.2, [-20, 0, -85]), (1.6, [0, 0, -3])]),
        "head": rot([(0, [0, 0, 0]), (0.6, [-25, 0, 0]), (1.2, [-25, 0, 0]), (1.6, [0, 0, 0])]),
        "body": {"position": kf([(0, [0, 0, 0]), (0.8, [0, 1.5, 0]), (1.6, [0, 0, 0])])},
    })
    A["animation.hu_vuong.roar"] = anim(2.0, False, {
        "head": rot([(0, [0, 0, 0]), (0.4, [-35, 0, 0]), (1.6, [-35, 0, 0]), (2.0, [0, 0, 0])]),
        "body": rot([(0, [0, 0, 0]), (0.4, [-12, 0, 0]), (1.6, [-12, 0, 0]), (2.0, [0, 0, 0])]),
        "right_arm": rot([(0, [0, 0, 3]), (0.4, [-30, 0, 60]), (1.6, [-30, 0, 60]), (2.0, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (0.4, [-30, 0, -60]), (1.6, [-30, 0, -60]), (2.0, [0, 0, -3])]),
    })
    return {"format_version": "1.8.0", "animations": A}


def main():
    uv, g = geo()
    (ASSETS / "geo" / "entity").mkdir(parents=True, exist_ok=True)
    (ASSETS / "animations" / "entity").mkdir(parents=True, exist_ok=True)
    (ASSETS / "textures" / "entity").mkdir(parents=True, exist_ok=True)
    (ASSETS / "geo" / "entity" / "hu_vuong.geo.json").write_text(json.dumps(g, indent=2), encoding="utf-8")
    (ASSETS / "animations" / "entity" / "hu_vuong.animation.json").write_text(
        json.dumps(build_animations(), indent=2), encoding="utf-8")
    img, glow = textures(uv)
    img.save(ASSETS / "textures" / "entity" / "hu_vuong.png")
    glow.save(ASSETS / "textures" / "entity" / "hu_vuong_glowmask.png")
    print("hu_vuong assets written")


if __name__ == "__main__":
    main()
