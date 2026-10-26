#!/usr/bin/env python3
"""Buoc 20: tai nguyen 4 Chap Hanh Gia (chg_1..4) + 4 vat lieu (chg_core_1..4). Mau theo gen_step15.py.
Chay: python tools/gen_step20.py  (tu thu muc goc repo)"""
import json, random
from pathlib import Path
from PIL import Image, ImageDraw

TEX = 256
ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "src/main/resources/assets/hukhong"
SHADE = {"top": 1.15, "front": 1.0, "right": 0.85, "left": 0.85, "back": 0.75, "bottom": 0.6}
# bone, parent, pivot, origin, size   (thanh nguoi cao ~3.4 block, nhu Hu Vuong)
BASE = [
    ("body", None, [0, 42, 0], [-9, 20, -5], [18, 22, 10]),
    ("head", None, [0, 42, 0], [-6, 42, -6], [12, 12, 12]),
    ("right_arm", None, [-12, 40, 0], [-15, 18, -4], [6, 24, 8]),
    ("left_arm", None, [12, 40, 0], [9, 18, -4], [6, 24, 8]),
    ("right_leg", None, [-4.5, 20, 0], [-8, 0, -4], [7, 20, 8]),
    ("left_leg", None, [4.5, 20, 0], [1, 0, -4], [7, 20, 8]),
]
# id: (body, accent, glow, extra cubes)
V = {
    "chg_1": ((92, 70, 56), (150, 60, 30), (255, 130, 40), [  # Thach Hong: giap vai da khoi
        ("pauldron_r", "body", [-12, 40, 0], [-18, 38, -6], [10, 8, 12]),
        ("pauldron_l", "body", [12, 40, 0], [8, 38, -6], [10, 8, 12]),
        ("brow", "head", [0, 54, 0], [-7, 52, -7], [14, 3, 14])]),
    "chg_2": ((225, 220, 200), (240, 200, 90), (255, 240, 150), [  # Bach Tu Am: vong sang + ao dai
        ("halo", "head", [0, 58, 0], [-8, 57, -8], [16, 1, 16]),
        ("robe", "body", [0, 20, 0], [-10, 4, -6], [20, 16, 12]),
        ("wing_r", "body", [-9, 36, 5], [-26, 22, 5], [18, 20, 1]),
        ("wing_l", "body", [9, 36, 5], [8, 22, 5], [18, 20, 1])]),
    "chg_3": ((200, 215, 235), (80, 140, 220), (170, 230, 255), [  # Lang Uyen: kiem lon
        ("blade", "right_arm", [-12, 20, 0], [-14, -14, -1], [2, 34, 6]),
        ("guard", "right_arm", [-12, 20, 0], [-17, 18, -3], [8, 2, 4]),
        ("scarf", "body", [0, 40, 5], [-3, 14, 5], [6, 26, 1])]),
    "chg_4": ((58, 14, 30), (170, 30, 60), (255, 60, 90), [  # Co Vo Tan: sung + ao choang
        ("horn_r", "head", [-4, 54, 0], [-6, 54, -1], [2, 11, 2]),
        ("horn_l", "head", [4, 54, 0], [4, 54, -1], [2, 11, 2]),
        ("cape", "body", [0, 42, 5], [-10, 8, 5], [20, 34, 1]),
        ("spike_r", "right_arm", [-12, 40, 0], [-17, 36, -2], [3, 8, 4]),
        ("spike_l", "left_arm", [12, 40, 0], [14, 36, -2], [3, 8, 4])]),
}
ACCENT_BONES = {"pauldron_r", "pauldron_l", "brow", "halo", "wing_r", "wing_l", "blade", "guard", "scarf",
                "horn_r", "horn_l", "cape", "spike_r", "spike_l"}
CORE_COL = {1: ((120, 60, 30), (255, 140, 40)), 2: ((230, 215, 150), (255, 250, 200)),
            3: ((120, 170, 220), (220, 245, 255)), 4: ((110, 20, 45), (255, 70, 100))}


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


def build(vid, body, accent, glow, extra):
    cubes = list(BASE) + list(extra)
    uv = pack(cubes)
    bones = []
    for n, p, pv, o, s in cubes:
        b = {"name": n, "pivot": pv, "cubes": [{"origin": o, "size": s, "uv": list(uv[n])}]}
        if p: b["parent"] = p
        bones.append(b)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
        "identifier": "geometry." + vid, "texture_width": TEX, "texture_height": TEX,
        "visible_bounds_width": 5, "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.8, 0]}, "bones": bones}]}
    rnd = random.Random(sum(map(ord, vid)))
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0)); gl = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    px, gp = img.load(), gl.load()
    for n, _, _, _, (w, h, d) in cubes:
        col = accent if n in ACCENT_BONES else tuple(int(c * 0.7) for c in body) if "leg" in n else body
        u, v = uv[n]
        for f, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for x in range(fx, fx + fw):
                for y in range(fy, fy + fh):
                    s = SHADE[f] * rnd.uniform(0.85, 1.1)
                    px[x, y] = tuple(max(0, min(255, int(c * s))) for c in col) + (255,)
        if n == "head":
            fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
            for ex in (fx + 2, fx + fw - 4):
                for dx in range(2):
                    for dy in range(2):
                        px[ex + dx, fy + 4 + dy] = glow + (255,); gp[ex + dx, fy + 4 + dy] = glow + (255,)
        if n == "body":  # Vong Tron Ho ngoai: dau an Hu Khong tren nguc
            fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
            cx, cy = fx + fw // 2, fy + 6
            for dx, dy in ((-3, 0), (-3, 1), (-2, 2), (-1, 3), (0, 3), (1, 3), (2, 2), (3, 1), (3, 0), (2, -1), (1, -2), (0, -2)):
                px[cx + dx, cy + dy] = glow + (255,); gp[cx + dx, cy + dy] = glow + (255,)
        if n in ("blade", "halo", "horn_r", "horn_l", "brow"):
            for f, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
                for x in range(fx, fx + fw):
                    for y in range(fy, fy + fh, 2):
                        gp[x, y] = glow + (255,)
    return geo, img, gl


def kf(p): return {str(t): v for t, v in p}
def rot(p): return {"rotation": kf(p)}


def anims(vid):
    n = "animation." + vid
    R = {}
    R[n + ".idle"] = {"loop": True, "animation_length": 4.0, "bones": {
        "body": {"position": kf([(0, [0, 0, 0]), (2.0, [0, 0.4, 0]), (4.0, [0, 0, 0])])},
        "right_arm": rot([(0, [0, 0, 3]), (2.0, [2, 0, 5]), (4.0, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (2.0, [2, 0, -5]), (4.0, [0, 0, -3])])}}
    R[n + ".walk"] = {"loop": True, "animation_length": 1.2, "bones": {
        "right_leg": rot([(0, [30, 0, 0]), (0.6, [-30, 0, 0]), (1.2, [30, 0, 0])]),
        "left_leg": rot([(0, [-30, 0, 0]), (0.6, [30, 0, 0]), (1.2, [-30, 0, 0])]),
        "right_arm": rot([(0, [-20, 0, 4]), (0.6, [20, 0, 4]), (1.2, [-20, 0, 4])]),
        "left_arm": rot([(0, [20, 0, -4]), (0.6, [-20, 0, -4]), (1.2, [20, 0, -4])])}}
    R[n + ".attack"] = {"loop": False, "animation_length": 0.6, "bones": {
        "right_arm": rot([(0, [0, 0, 3]), (0.2, [-110, 0, 0]), (0.35, [40, 0, 0]), (0.6, [0, 0, 3])]),
        "body": rot([(0, [0, 0, 0]), (0.2, [-8, 0, 0]), (0.35, [12, 0, 0]), (0.6, [0, 0, 0])])}}
    # skill_a: dap manh (ngan) ; skill_b: dang tay ; skill_c: gio cao hai tay (windup dai)
    R[n + ".skill_a"] = {"loop": False, "animation_length": 1.0, "bones": {
        "right_arm": rot([(0, [0, 0, 3]), (0.5, [-150, 0, 0]), (0.7, [30, 0, 0]), (1.0, [0, 0, 3])]),
        "body": rot([(0, [0, 0, 0]), (0.5, [-14, 0, 0]), (0.7, [20, 0, 0]), (1.0, [0, 0, 0])])}}
    R[n + ".skill_b"] = {"loop": False, "animation_length": 1.6, "bones": {
        "right_arm": rot([(0, [0, 0, 3]), (0.6, [-20, 0, 85]), (1.2, [-20, 0, 85]), (1.6, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (0.6, [-20, 0, -85]), (1.2, [-20, 0, -85]), (1.6, [0, 0, -3])]),
        "head": rot([(0, [0, 0, 0]), (0.6, [-20, 0, 0]), (1.2, [-20, 0, 0]), (1.6, [0, 0, 0])])}}
    R[n + ".skill_c"] = {"loop": False, "animation_length": 2.4, "bones": {
        "right_arm": rot([(0, [0, 0, 3]), (1.4, [-170, 0, 10]), (1.8, [-170, 0, 10]), (2.1, [20, 0, 0]), (2.4, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (1.4, [-170, 0, -10]), (1.8, [-170, 0, -10]), (2.1, [20, 0, 0]), (2.4, [0, 0, -3])]),
        "body": rot([(0, [0, 0, 0]), (1.4, [-18, 0, 0]), (1.8, [-18, 0, 0]), (2.1, [25, 0, 0]), (2.4, [0, 0, 0])])}}
    R[n + ".phase"] = {"loop": False, "animation_length": 2.0, "bones": {
        "head": rot([(0, [0, 0, 0]), (0.4, [-35, 0, 0]), (1.6, [-35, 0, 0]), (2.0, [0, 0, 0])]),
        "body": rot([(0, [0, 0, 0]), (0.4, [-12, 0, 0]), (1.6, [-12, 0, 0]), (2.0, [0, 0, 0])]),
        "right_arm": rot([(0, [0, 0, 3]), (0.4, [-30, 0, 60]), (1.6, [-30, 0, 60]), (2.0, [0, 0, 3])]),
        "left_arm": rot([(0, [0, 0, -3]), (0.4, [-30, 0, -60]), (1.6, [-30, 0, -60]), (2.0, [0, 0, -3])])}}
    return {"format_version": "1.8.0", "animations": R}


def core_icon(i):
    base, glow = CORE_COL[i]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.polygon([(8, 1), (14, 6), (12, 14), (4, 14), (2, 6)], fill=base + (255,), outline=(20, 12, 28, 255))
    d.ellipse([5, 5, 10, 10], fill=glow + (255,))
    d.ellipse([7, 7, 8, 8], fill=base + (255,))  # vong tron ho
    d.line([(8, 7), (9, 7)], fill=(0, 0, 0, 0))
    return im


def main():
    for sub in ("geo/entity", "animations/entity", "textures/entity", "textures/item", "models/item"):
        (A / sub).mkdir(parents=True, exist_ok=True)
    for vid, (body, accent, glow, extra) in V.items():
        geo, img, gl = build(vid, body, accent, glow, extra)
        (A / "geo/entity" / (vid + ".geo.json")).write_text(json.dumps(geo, indent=2), encoding="utf-8")
        (A / "animations/entity" / (vid + ".animation.json")).write_text(json.dumps(anims(vid), indent=2), encoding="utf-8")
        img.save(A / "textures/entity" / (vid + ".png")); gl.save(A / "textures/entity" / (vid + "_glowmask.png"))
    for i in range(1, 5):
        core_icon(i).save(A / f"textures/item/chg_core_{i}.png")
        (A / f"models/item/chg_core_{i}.json").write_text(
            json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"hukhong:item/chg_core_{i}"}}), encoding="utf-8")
    print("step20 assets written")


if __name__ == "__main__":
    main()
