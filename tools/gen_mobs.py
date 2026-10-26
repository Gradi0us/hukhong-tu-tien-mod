#!/usr/bin/env python3
"""Sinh tai nguyen GeckoLib 4 (Minecraft 1.21.1) cho 3 quai "Hu Hoa" cua mod hukhong.

Moi id sinh ra:
  geo/entity/<id>.geo.json            (Bedrock geometry, format 1.12.0)
  animations/entity/<id>.animation.json (format 1.8.0: idle / walk / attack)
  textures/entity/<id>.png            (64x64 RGBA, Pillow)

Chay: python tools/gen_mobs.py   (tu D:\\mc_mod)
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

TEX = 64
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hukhong"

GLOW = (196, 118, 255)        # mat tim phat sang
VOID_DARK = (18, 6, 38)       # vet tim den
VOID_BRIGHT = (168, 80, 255)  # vet tim sang
SOCKET = (20, 16, 28)         # hoc mat

SHADE = {"top": 1.0, "front": 0.92, "right": 0.8, "left": 0.8, "back": 0.7, "bottom": 0.6}

PALETTE = {
    "hu_thi": {  # zombie: da xanh xam, ao thun teal rach, quan xanh dam
        "head": (96, 143, 76), "body": (44, 95, 91),
        "right_arm": (96, 143, 76), "left_arm": (96, 143, 76),
        "right_leg": (45, 53, 97), "left_leg": (45, 53, 97),
    },
    "hu_cot": {  # skeleton: xuong trang ngà, than co hep
        "head": (216, 210, 188), "body": (200, 193, 170),
        "right_arm": (216, 210, 188), "left_arm": (216, 210, 188),
        "right_leg": (216, 210, 188), "left_leg": (216, 210, 188),
    },
    "hu_chu": {  # nhen: vo den tim, dau tim than
        "head": (58, 42, 68), "body": (42, 32, 48), "leg": (34, 26, 42),
    },
}

EYES = {
    "hu_thi": [(10, 11, GLOW), (11, 11, GLOW), (13, 11, GLOW), (14, 11, GLOW)],
    "hu_cot": [(9, 10, SOCKET), (10, 10, SOCKET), (9, 11, SOCKET),
               (13, 10, SOCKET), (14, 10, SOCKET), (14, 11, SOCKET),
               (10, 11, GLOW), (13, 11, GLOW)],
    "hu_chu": [(12, 44, GLOW), (15, 44, GLOW), (13, 46, GLOW), (16, 46, GLOW)],
}


# ---------------------------------------------------------------- geometry
def face_rects(u, v, w, h, d):
    """Box-UV cua cube (w=X, h=Y, d=Z): rect (x0, y0, x1, y1), x1/y1 exclusive."""
    return {
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
    }


def cube(origin, size, uv, group=None):
    c = {"origin": origin, "size": size, "uv": uv}
    if group:
        c["uv_group"] = group
    return c


def bone(name, pivot, *cubes):
    return {"name": name, "pivot": pivot, "cubes": list(cubes)}


def humanoid_bones(thin):
    """Zombie (thin=False): body 8x12x4, tay/chan 4x12x4.
    Xuong  (thin=True):  than 6x12x3, tay/chan 2x12x2 (gay)."""
    bw, bd, aw, ad = (6, 3, 2, 2) if thin else (8, 4, 4, 4)
    return [
        bone("body", [0, 24, 0], cube([-bw / 2, 12, -bd / 2], [bw, 12, bd], [16, 16])),
        bone("head", [0, 24, 0], cube([-4, 24, -4], [8, 8, 8], [0, 0])),
        bone("right_arm", [-5, 22, 0], cube([-4 - aw, 12, -ad / 2], [aw, 12, ad], [40, 16])),
        bone("left_arm", [5, 22, 0], cube([4, 12, -ad / 2], [aw, 12, ad], [32, 48])),
        bone("right_leg", [-1.9, 12, 0], cube([-aw, 0, -ad / 2], [aw, 12, ad], [0, 16])),
        bone("left_leg", [1.9, 12, 0], cube([0, 0, -ad / 2], [aw, 12, ad], [16, 48])),
    ]


SPIDER_ZS = [-4, -1, 2, 5]


def spider_bones():
    """Nhen to, than thap rong: body 16x8x12, dau 10x8x10, 8 chan 10x2x2 (chung UV)."""
    bones = [
        bone("body", [0, 8, 0], cube([-8, 4, -6], [16, 8, 12], [0, 0])),
        bone("head", [0, 8, -6], cube([-5, 6, -16], [10, 8, 10], [0, 32])),
    ]
    for i in range(8):
        k, side = i // 2, i % 2          # side 0 = ben phai, 1 = ben trai
        z = SPIDER_ZS[k]
        x = -4 if side == 0 else 4
        ox = x - 10 if side == 0 else x
        bones.append(bone(f"leg{i}", [x, 6, z],
                          cube([ox, 5, z - 1], [10, 2, 2], [0, 50], group="leg")))
    return bones


def check_uv(mob, bones):
    """Kiem tra moi mat UV nam trong 64x64 va khong chong cube khac (cho phep dung chung group)."""
    owner = {}
    for b in bones:
        for c in b["cubes"]:
            u, v = c["uv"]
            w, h, d = c["size"]
            tag = c.get("uv_group", b["name"])
            for face, (x0, y0, x1, y1) in face_rects(u, v, w, h, d).items():
                assert 0 <= x0 < x1 <= TEX and 0 <= y0 < y1 <= TEX, f"{mob} {b['name']}.{face} ngoai UV"
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        prev = owner.get((x, y))
                        assert prev is None or prev == tag, f"{mob} UV chong tai {(x, y)} ({b['name']}.{face})"
                        owner[(x, y)] = tag


def geo_json(mob, bones, vb):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{mob}",
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": vb[0],
                "visible_bounds_height": vb[1],
                "visible_bounds_offset": [0, vb[2], 0],
            },
            "bones": [{
                "name": b["name"],
                "pivot": b["pivot"],
                "cubes": [{"origin": c["origin"], "size": c["size"], "uv": c["uv"]}
                          for c in b["cubes"]],
            } for b in bones],
        }],
    }


# ---------------------------------------------------------------- animations
def kf(*pairs):
    return {str(float(t)): list(val) for t, val in pairs}


def humanoid_anims():
    idle = {
        "body": {"position": kf((0, [0, 0, 0]), (1.5, [0, -0.3, 0]), (3, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (1.5, [-1.5, 0, 0]), (3, [0, 0, 0]))},
        "right_arm": {"rotation": kf((0, [0, 0, 0]), (1.5, [-2, 0, 0]), (3, [0, 0, 0]))},
        "left_arm": {"rotation": kf((0, [0, 0, 0]), (1.5, [-2, 0, 0]), (3, [0, 0, 0]))},
    }
    walk = {
        "right_arm": {"rotation": kf((0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1, [-30, 0, 0]))},
        "left_arm": {"rotation": kf((0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1, [30, 0, 0]))},
        "right_leg": {"rotation": kf((0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1, [30, 0, 0]))},
        "left_leg": {"rotation": kf((0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1, [-30, 0, 0]))},
        "head": {"position": kf((0, [0, 0, 0]), (0.25, [0, -0.5, 0]), (0.5, [0, 0, 0]),
                                (0.75, [0, -0.5, 0]), (1, [0, 0, 0]))},
    }
    attack = {
        "right_arm": {"rotation": kf((0, [-40, 0, 0]), (0.15, [-130, 0, 0]),
                                     (0.3, [-110, 0, 0]), (0.5, [-40, 0, 0]))},
        "left_arm": {"rotation": kf((0, [-20, 0, 0]), (0.15, [-70, 0, 0]), (0.5, [-20, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (0.15, [-12, 0, 0]), (0.5, [0, 0, 0]))},
        "body": {"rotation": kf((0, [0, 0, 0]), (0.15, [-6, 0, 0]), (0.5, [0, 0, 0]))},
    }
    return {"idle": (True, 3.0, idle), "walk": (True, 1.0, walk), "attack": (False, 0.5, attack)}


def spider_anims():
    idle = {
        "body": {"position": kf((0, [0, 0, 0]), (1, [0, -0.4, 0]), (2, [0, 0, 0]))},
        "head": {"rotation": kf((0, [0, 0, 0]), (1, [2, 0, 0]), (2, [0, 0, 0]))},
    }
    walk = {"body": {"position": kf((0, [0, 0, 0]), (0.4, [0, -0.6, 0]), (0.8, [0, 0, 0]))}}
    for i in range(8):
        side_sign = 1 if i % 2 == 0 else -1
        phase = 1 if (i // 2) % 2 else -1          # hai nhom chan xen ke (kieu tripod)
        a = 25 * side_sign * phase
        walk[f"leg{i}"] = {"rotation": kf((0, [0, a, 0]), (0.4, [0, -a, 0]), (0.8, [0, a, 0]))}
        idle[f"leg{i}"] = {"rotation": kf((0, [0, 3 * side_sign, 0]), (1, [0, -3 * side_sign, 0]),
                                          (2, [0, 3 * side_sign, 0]))}
    attack = {
        "head": {"position": kf((0, [0, 0, 0]), (0.15, [0, 0, -3]), (0.5, [0, 0, 0]))},
        "body": {"position": kf((0, [0, 0, 0]), (0.15, [0, 0, -1]), (0.5, [0, 0, 0]))},
    }
    return {"idle": (True, 2.0, idle), "walk": (True, 0.8, walk), "attack": (False, 0.5, attack)}


def anim_json(mob, animset):
    return {
        "format_version": "1.8.0",
        "animations": {
            f"animation.{mob}.{name}": {"loop": loop, "animation_length": length, "bones": bones}
            for name, (loop, length, bones) in animset.items()
        },
    }


# ---------------------------------------------------------------- texture
def pixel_color(base, face, x, y, rng, seed):
    k = SHADE[face] * (0.93 + 0.14 * rng.random())
    r, g, b = (min(255, int(c * k)) for c in base)
    if rng.random() < 0.03:                       # dom den hu khong
        return (*VOID_DARK, 255)
    t = math.sin(0.8 * x + 0.45 * y + seed) + 0.6 * math.sin(0.31 * x - 0.9 * y + 1.7 * seed)
    at = abs(t)
    if at < 0.10:                                 # vet tim sang
        return (*VOID_BRIGHT, 255)
    if at < 0.30:                                 # vet tim den (pha tron)
        return (int(r * 0.45 + VOID_DARK[0] * 0.55), int(g * 0.45 + VOID_DARK[1] * 0.55),
                int(b * 0.45 + VOID_DARK[2] * 0.55), 255)
    return (r, g, b, 255)


def paint_mob(mob, bones, eyes):
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    pal = PALETTE[mob]
    for b in bones:
        name = b["name"]
        if name in pal:
            base = pal[name]
        elif name.startswith("leg"):
            base = pal["leg"]
        else:
            base = pal["body"]
        for c in b["cubes"]:
            u, v = c["uv"]
            w, h, d = c["size"]
            for face, (x0, y0, x1, y1) in face_rects(u, v, w, h, d).items():
                rng = random.Random(f"{mob}|{name}|{face}|{u},{v}")
                seed = rng.uniform(0, 2 * math.pi)
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        img.putpixel((x, y), pixel_color(base, face, x, y, rng, seed))
    for x, y, col in eyes:
        img.putpixel((x, y), (*col, 255))
    return img


# ---------------------------------------------------------------- main
MOBS = {
    "hu_thi": {"bones": lambda: humanoid_bones(thin=False), "anims": humanoid_anims, "vb": (2.5, 2.5, 1.0)},
    "hu_cot": {"bones": lambda: humanoid_bones(thin=True), "anims": humanoid_anims, "vb": (2.5, 2.5, 1.0)},
    "hu_chu": {"bones": spider_bones, "anims": spider_anims, "vb": (3.0, 1.5, 0.5)},
}


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    for mob, spec in MOBS.items():
        bones = spec["bones"]()
        check_uv(mob, bones)

        head = next(b for b in bones if b["name"] == "head")["cubes"][0]
        fx0, fy0, fx1, fy1 = face_rects(*head["uv"], *head["size"])["front"]
        for x, y, _ in EYES[mob]:
            assert fx0 <= x < fx1 and fy0 <= y < fy1, f"{mob} mat {(x, y)} ngoai mat truoc dau"

        write_json(ASSETS / "geo" / "entity" / f"{mob}.geo.json", geo_json(mob, bones, spec["vb"]))
        write_json(ASSETS / "animations" / "entity" / f"{mob}.animation.json",
                   anim_json(mob, spec["anims"]()))

        tex_dir = ASSETS / "textures" / "entity"
        tex_dir.mkdir(parents=True, exist_ok=True)
        paint_mob(mob, bones, EYES[mob]).save(tex_dir / f"{mob}.png", "PNG")
        print(f"[OK] {mob}: geo, animation, texture")


if __name__ == "__main__":
    main()
