#!/usr/bin/env python3
"""Buoc 19: tai nguyen Su Do Da Khuyet (3 bien the) + Van Kien. Mau theo gen_step15.py (HuVuong).
Chay: python tools/gen_step19.py  (tu thu muc goc repo)"""
import json, random
from pathlib import Path
from PIL import Image, ImageDraw

TEX = 128
ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "src/main/resources/assets/hukhong"
SHADE = {"top": 1.15, "front": 1.0, "right": 0.85, "left": 0.85, "back": 0.75, "bottom": 0.6}
BASE = [  # bone, parent, pivot, origin, size
    ("body", None, [0, 24, 0], [-4, 12, -2], [8, 12, 4]),
    ("head", None, [0, 24, 0], [-4, 24, -4], [8, 8, 8]),
    ("hood", "head", [0, 24, 0], [-4.5, 23.5, -4.5], [9, 9, 9]),
    ("right_arm", None, [-5, 22, 0], [-8, 12, -2], [3, 12, 4]),
    ("left_arm", None, [5, 22, 0], [5, 12, -2], [3, 12, 4]),
    ("right_leg", None, [-2, 12, 0], [-4, 0, -2], [4, 12, 4]),
    ("left_leg", None, [2, 12, 0], [0, 0, -2], [4, 12, 4]),
]
V = {  # id: (robe, trim, glow, extra cubes)
    "su_do_kiem": ((28, 22, 44), (200, 200, 215), (230, 230, 255), [("blade", "right_arm", [-7, 4, 0], [-7.5, -6, -1], [1, 18, 3])]),
    "su_do_chu": ((40, 24, 70), (150, 90, 220), (200, 120, 255), [("staff", "left_arm", [6, 4, 0], [5.5, -4, -0.5], [1, 28, 1])]),
    "su_do_te": ((62, 16, 34), (210, 70, 90), (255, 90, 120), [("collar", "body", [0, 24, 0], [-5, 22, -3], [10, 3, 6])]),
}


def pack(cubes):
    uv, x, y, rh = {}, 0, 0, 0
    for n, _, _, _, (w, h, d) in cubes:
        bw, bh = int(2 * (w + d)) + 1, int(d + h) + 1
        if x + bw > TEX: x, y, rh = 0, y + rh, 0
        uv[n] = (x, y); x += bw; rh = max(rh, bh)
    assert y + rh <= TEX, (y, rh)
    return uv


def faces(u, v, w, h, d):
    w, h, d = int(w), int(h), int(d)
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def build(vid, robe, trim, glow, extra):
    cubes = [c for c in BASE] + [(n, p, pv, o, s) for n, p, pv, o, s in extra]
    uv = pack(cubes)
    bones = []
    for n, p, pv, o, s in cubes:
        b = {"name": n, "pivot": pv, "cubes": [{"origin": o, "size": s, "uv": list(uv[n])}]}
        if p: b["parent"] = p
        bones.append(b)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
        "identifier": "geometry." + vid, "texture_width": TEX, "texture_height": TEX,
        "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}
    rnd = random.Random(sum(map(ord, vid)))
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0)); gl = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    px, gp = img.load(), gl.load()
    for n, _, _, _, (w, h, d) in cubes:
        col = trim if n in ("blade", "staff", "collar") else (18, 12, 28) if n == "head" else robe
        u, v = uv[n]
        for f, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for x in range(fx, fx + fw):
                for y in range(fy, fy + fh):
                    s = SHADE[f] * rnd.uniform(0.85, 1.1)
                    px[x, y] = tuple(max(0, min(255, int(c * s))) for c in col) + (255,)
        if n == "head":  # mat sang trong mu
            fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
            for ex in (fx + 1, fx + fw - 3):
                for dx in range(2):
                    px[ex + dx, fy + 3] = glow + (255,); gp[ex + dx, fy + 3] = glow + (255,)
        if n == "body":  # Nguyet Khuyet an: vong tron ho
            fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
            cx, cy = fx + fw // 2, fy + 4
            for dx, dy in ((-2, 0), (-2, 1), (-1, 2), (0, 2), (1, 2), (2, 1), (2, 0), (1, -1), (0, -1)):
                px[cx + dx, cy + dy] = glow + (255,); gp[cx + dx, cy + dy] = glow + (255,)
        if n in ("blade", "staff", "collar"):
            for f, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
                if f == "front":
                    for y in range(fy, fy + fh, 3):
                        gp[fx, y] = glow + (255,)
    return geo, img, gl


def kf(p): return {str(t): v for t, v in p}
def rot(p): return {"rotation": kf(p)}


def anims(vid):
    n = "animation." + vid
    R = {}
    R[n + ".idle"] = {"loop": True, "animation_length": 3.0, "bones": {
        "body": {"position": kf([(0, [0, 0, 0]), (1.5, [0, 0.3, 0]), (3.0, [0, 0, 0])])},
        "right_arm": rot([(0, [0, 0, 3]), (1.5, [2, 0, 5]), (3.0, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (1.5, [2, 0, -5]), (3.0, [0, 0, -3])])}}
    R[n + ".walk"] = {"loop": True, "animation_length": 1.0, "bones": {
        "right_leg": rot([(0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1.0, [30, 0, 0])]),
        "left_leg": rot([(0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1.0, [-30, 0, 0])]),
        "right_arm": rot([(0, [-20, 0, 3]), (0.5, [20, 0, 3]), (1.0, [-20, 0, 3])]),
        "left_arm": rot([(0, [20, 0, -3]), (0.5, [-20, 0, -3]), (1.0, [20, 0, -3])])}}
    R[n + ".attack"] = {"loop": False, "animation_length": 0.5, "bones": {
        "right_arm": rot([(0, [0, 0, 3]), (0.15, [-120, 0, 0]), (0.3, [30, 0, 0]), (0.5, [0, 0, 3])]),
        "body": rot([(0, [0, 0, 0]), (0.15, [-8, 0, 0]), (0.3, [10, 0, 0]), (0.5, [0, 0, 0])])}}
    R[n + ".cast"] = {"loop": False, "animation_length": 1.0, "bones": {
        "right_arm": rot([(0, [0, 0, 3]), (0.3, [-70, 0, 20]), (0.8, [-70, 0, 20]), (1.0, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (0.3, [-70, 0, -20]), (0.8, [-70, 0, -20]), (1.0, [0, 0, -3])]),
        "head": rot([(0, [0, 0, 0]), (0.3, [-15, 0, 0]), (0.8, [-15, 0, 0]), (1.0, [0, 0, 0])])}}
    return {"format_version": "1.8.0", "animations": R}


def doc_icon(base, seal):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([3, 1, 12, 14], fill=base + (255,), outline=(70, 50, 40, 255))
    for y in (4, 6, 8):
        d.line([5, y, 10, y], fill=(90, 70, 60, 255))
    d.ellipse([8, 10, 12, 14], fill=seal + (255,))
    d.rectangle([10, 11, 11, 12], fill=base + (255,))  # vong tron ho
    return im


def main():
    for vid, (robe, trim, glow, extra) in V.items():
        geo, img, gl = build(vid, robe, trim, glow, extra)
        for sub, name in (("geo", vid + ".geo.json"), ("animations", vid + ".animation.json")):
            (A / sub / "entity").mkdir(parents=True, exist_ok=True)
        (A / "geo/entity" / (vid + ".geo.json")).write_text(json.dumps(geo, indent=2), encoding="utf-8")
        (A / "animations/entity" / (vid + ".animation.json")).write_text(json.dumps(anims(vid), indent=2), encoding="utf-8")
        img.save(A / "textures/entity" / (vid + ".png")); gl.save(A / "textures/entity" / (vid + "_glowmask.png"))
        (A / "models/item" / (vid + "_spawn_egg.json")).write_text('{"parent":"minecraft:item/template_spawn_egg"}', encoding="utf-8")
    doc_icon((226, 210, 170), (150, 70, 200)).save(A / "textures/item/van_kien_a.png")
    doc_icon((205, 215, 225), (200, 60, 90)).save(A / "textures/item/van_kien_b.png")
    for c in "ab":
        for i in range(1, 7):
            (A / "models/item" / f"van_kien_{c}{i}.json").write_text(
                json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"hukhong:item/van_kien_{c}"}}), encoding="utf-8")
    print("step19 assets written")


if __name__ == "__main__":
    main()
