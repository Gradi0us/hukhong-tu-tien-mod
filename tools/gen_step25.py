#!/usr/bin/env python3
"""Buoc 25-26: tai nguyen Nguyen Thuy Tich Diet (boss cuoi), Da Khuyet, cong endgame_portal. Dung lai gen_step20.
Chay: python tools/gen_step25.py  (tu thu muc goc repo)"""
import json, sys
from pathlib import Path
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_step20 as g

A = g.A
K = 1.8  # boss cao ~6 block

# Da Khuyet: ao choang xanh dem, liem trang khuyet tren dau
DAK = ((24, 28, 58), (190, 195, 225), (235, 240, 255), [
    ("crescent", "head", [0, 58, 0], [-6, 56, -2], [12, 2, 3]),
    ("crescent_tip_r", "head", [-6, 58, 0], [-7, 52, -2], [2, 6, 3]),
    ("cloak", "body", [0, 42, 5], [-11, 6, 5], [22, 36, 1]),
    ("hood", "head", [0, 54, 0], [-7, 50, -7], [14, 4, 14])])

# Nguyen Thuy Tich Diet: loi vo dinh + Vong Tron Ho (cung tron thieu net) phia sau
BOSS_BASE = [
    ("body", None, [0, 42, 0], [-9, 20, -5], [18, 22, 10]),
    ("head", None, [0, 42, 0], [-6, 42, -6], [12, 12, 12]),
    ("right_arm", None, [-12, 40, 0], [-15, 18, -4], [6, 24, 8]),
    ("left_arm", None, [12, 40, 0], [9, 18, -4], [6, 24, 8]),
    ("right_leg", None, [-4.5, 20, 0], [-8, 0, -4], [7, 20, 8]),
    ("left_leg", None, [4.5, 20, 0], [1, 0, -4], [7, 20, 8]),
]
BOSS = ((14, 8, 30), (70, 30, 120), (200, 140, 255), [
    ("ring_top", "body", [0, 42, 8], [-16, 66, 8], [32, 4, 3]),
    ("ring_ul", "body", [0, 42, 8], [-26, 50, 8], [4, 18, 3]),
    ("ring_ur", "body", [0, 42, 8], [22, 50, 8], [4, 18, 3]),
    ("ring_ll", "body", [0, 42, 8], [-26, 20, 8], [4, 18, 3]),
    ("ring_lr", "body", [0, 42, 8], [22, 20, 8], [4, 18, 3]),
    ("shard_l", "body", [-9, 36, 0], [-20, 30, -3], [3, 14, 3]),
    ("shard_r", "body", [9, 36, 0], [17, 30, -3], [3, 14, 3])])
g.ACCENT_BONES.update({"crescent", "crescent_tip_r", "cloak", "hood", "ring_top", "ring_ul", "ring_ur",
                       "ring_ll", "ring_lr", "shard_l", "shard_r"})


def scaled(extra, k):
    f = lambda v: [round(x * k, 2) for x in v]
    base = [(n, p, f(pv), f(o), f(s)) for n, p, pv, o, s in BOSS_BASE]
    return base, [(n, p, f(pv), f(o), f(s)) for n, p, pv, o, s in extra]


def emit(vid, body, accent, glow, extra, base=None, tex=256):
    g.TEX = tex
    if base is not None:
        g.BASE[:] = base
    geo, img, gl = g.build(vid, body, accent, glow, extra)
    (A / "geo/entity" / (vid + ".geo.json")).write_text(json.dumps(geo, indent=2), encoding="utf-8")
    (A / "animations/entity" / (vid + ".animation.json")).write_text(json.dumps(g.anims(vid), indent=2), encoding="utf-8")
    img.save(A / "textures/entity" / (vid + ".png")); gl.save(A / "textures/entity" / (vid + "_glowmask.png"))


def portal_tex():
    im = Image.new("RGBA", (16, 16), (20, 8, 40, 255)); d = ImageDraw.Draw(im)
    import random
    r = random.Random(2500)
    for _ in range(40):
        d.point((r.randrange(16), r.randrange(16)), fill=(r.randrange(90, 200), 60, r.randrange(150, 255), 255))
    d.ellipse([3, 3, 12, 12], outline=(235, 220, 255, 255))
    d.line([(8, 3), (12, 3)], fill=(20, 8, 40, 255))  # Vong Tron Ho
    return im


def main():
    for sub in ("geo/entity", "animations/entity", "textures/entity", "textures/block", "models/block", "models/item", "blockstates"):
        (A / sub).mkdir(parents=True, exist_ok=True)
    emit("da_khuyet", *DAK)
    orig = list(g.BASE)
    base, extra = scaled(BOSS[3], K)
    emit("nguyen_thuy_tich_diet", BOSS[0], BOSS[1], BOSS[2], extra, base=base, tex=512)
    g.BASE[:] = orig; g.TEX = 256
    portal_tex().save(A / "textures/block/endgame_portal.png")
    (A / "blockstates/endgame_portal.json").write_text(json.dumps({"variants": {"": {"model": "hukhong:block/endgame_portal"}}}), encoding="utf-8")
    (A / "models/block/endgame_portal.json").write_text(json.dumps(
        {"parent": "minecraft:block/cube_all", "textures": {"all": "hukhong:block/endgame_portal"}}), encoding="utf-8")
    (A / "models/item/endgame_portal.json").write_text(json.dumps({"parent": "hukhong:block/endgame_portal"}), encoding="utf-8")
    print("step25 assets written")


if __name__ == "__main__":
    main()
