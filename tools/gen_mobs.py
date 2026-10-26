#!/usr/bin/env python3
"""Sinh tai nguyen GeckoLib 4 (Minecraft 1.21.1) cho 3 quai "Hu Hoa" cua mod hukhong.

Moi id sinh ra:
  geo/entity/<id>.geo.json              (Bedrock geometry 1.12.0; giu nguyen ten bone cu,
                                         chi them bone con trang tri)
  animations/entity/<id>.animation.json (format 1.8.0: idle / walk / attack / skill)
  textures/entity/<id>.png              (64x64 RGBA: shading 4 tong, chi tiet theo mat cube)
  textures/entity/<id>_glowmask.png     (64x64 RGBA: chi mat + vet sang, nen trong suot)

Chay: python tools/gen_mobs.py   (tu D:\\mc_mod)
"""
import json
import math
import random
import zlib
from pathlib import Path

from PIL import Image

TEX = 64
TAU = 2 * math.pi
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hukhong"

GLOW = (196, 118, 255)       # mat tim sang
GLOW_VEIN = (176, 112, 255)  # vet hu khong sang
VOID_MID = (92, 40, 150)     # vet tim lop giua
VOID_DARK = (18, 6, 38)
SOCKET = (14, 10, 20)        # hoc mat

TONES = (0.62, 0.80, 0.94, 1.06)  # 4 tong shading
SHADE = {"top": 1.0, "front": 0.9, "right": 0.78, "left": 0.78, "back": 0.7, "bottom": 0.55}

PALETTE = {
    "hu_thi": {"skin": (96, 143, 76), "shirt": (44, 95, 91), "pants": (45, 53, 97), "spike": (62, 40, 92)},
    "hu_cot": {"bone": (216, 210, 188), "shard": (186, 178, 156)},
    "hu_chu": {"carapace": (58, 42, 68), "leg": (40, 30, 50), "egg": (74, 44, 98)},
}
# (ten bone hoac tien to) -> nhom mau
PARTS = {
    "hu_thi": [("head", "skin"), ("body", "shirt"), ("right_arm", "skin"), ("left_arm", "skin"),
               ("right_leg", "pants"), ("left_leg", "pants"), ("spike", "spike")],
    "hu_cot": [("head", "bone"), ("body", "bone"), ("right_arm", "bone"), ("left_arm", "bone"),
               ("right_leg", "bone"), ("left_leg", "bone"), ("shard", "shard")],
    "hu_chu": [("head", "carapace"), ("body", "carapace"), ("leg", "leg"),
               ("spine", "carapace"), ("egg", "egg")],
}
# (x, y, w, h, loai) trong mat truoc cua dau; "glow" = mat sang, "socket" = hoc mat toi
EYES = {
    "hu_thi": [(9, 10, 2, 2, "glow"), (13, 10, 2, 2, "glow"),
               (9, 9, 2, 1, "socket"), (13, 9, 2, 1, "socket")],
    "hu_cot": [(9, 9, 2, 3, "socket"), (13, 9, 2, 3, "socket"),
               (10, 10, 1, 2, "glow"), (13, 10, 1, 2, "glow")],
    "hu_chu": [(11, 44, 2, 2, "glow"), (15, 44, 2, 2, "glow"),
               (12, 47, 1, 1, "glow"), (16, 47, 1, 1, "glow")],
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


def bone(name, pivot, *cubes, parent=None):
    b = {"name": name, "pivot": pivot, "cubes": list(cubes)}
    if parent:
        b["parent"] = parent
    return b


def humanoid_bones(thin):
    """Zombie (thin=False): than 8x12x4, tay/chan 4x12x4 + gai vai.
    Xuong  (thin=True):  than 6x12x3, tay/chan 2x12x2 + manh xuong lo lung."""
    bw, bd, aw, ad = (6, 3, 2, 2) if thin else (8, 4, 4, 4)
    bones = [
        bone("body", [0, 24, 0], cube([-bw / 2, 12, -bd / 2], [bw, 12, bd], [16, 16])),
        bone("head", [0, 24, 0], cube([-4, 24, -4], [8, 8, 8], [0, 0])),
        bone("right_arm", [-5, 22, 0], cube([-4 - aw, 12, -ad / 2], [aw, 12, ad], [40, 16])),
        bone("left_arm", [5, 22, 0], cube([4, 12, -ad / 2], [aw, 12, ad], [32, 48])),
        bone("right_leg", [-1.9, 12, 0], cube([-aw, 0, -ad / 2], [aw, 12, ad], [0, 16])),
        bone("left_leg", [1.9, 12, 0], cube([0, 0, -ad / 2], [aw, 12, ad], [16, 48])),
    ]
    if thin:
        bones += [
            bone("shard_1", [-4, 22, 3.5], cube([-4.5, 20, 3], [1, 4, 1], [0, 32], group="shard"), parent="body"),
            bone("shard_2", [4, 15, 4], cube([3.5, 13, 3.5], [1, 4, 1], [0, 32], group="shard"), parent="body"),
            bone("shard_3", [5, 29, -1.5], cube([4.5, 27, -2], [1, 4, 1], [0, 32], group="shard"), parent="body"),
        ]
    else:
        bones += [
            bone("spike_r_a", [-6, 24, 0], cube([-7, 24, -1], [2, 5, 2], [32, 0], group="spike"), parent="right_arm"),
            bone("spike_r_b", [-7, 23, -2], cube([-7.5, 23, -3], [1, 3, 1], [32, 0], group="spike"), parent="right_arm"),
            bone("spike_l_a", [6, 24, 0], cube([5, 24, -1], [2, 5, 2], [32, 0], group="spike"), parent="left_arm"),
            bone("spike_l_b", [7, 23, -2], cube([6.5, 23, -3], [1, 3, 1], [32, 0], group="spike"), parent="left_arm"),
        ]
    return bones


SPIDER_ZS = [-4, -1, 2, 5]


def spider_bones():
    """Nhen to, than thap rong: body 16x8x12, dau 10x8x10, 8 chan 10x2x2 (chung UV),
    túi trung o lung + 3 gai lung."""
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
    bones.append(bone("egg_sac", [0, 6, 6], cube([-3, 3, 6], [6, 6, 6], [0, 20]), parent="body"))
    for i, z in enumerate((-4, 0, 4), 1):
        bones.append(bone(f"spine_{i}", [0, 12, z], cube([-1, 12, z], [2, 3, 2], [24, 20], group="spike"),
                          parent="body"))
    return bones


def check_uv(mob, bones):
    """Moi mat UV nam trong 64x64 va khong chong cube khac (cho phep dung chung group)."""
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
    out_bones = []
    for b in bones:
        entry = {"name": b["name"]}
        if b.get("parent"):
            entry["parent"] = b["parent"]
        entry["pivot"] = b["pivot"]
        entry["cubes"] = [{"origin": c["origin"], "size": c["size"], "uv": c["uv"]} for c in b["cubes"]]
        out_bones.append(entry)
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
            "bones": out_bones,
        }],
    }


# ---------------------------------------------------------------- animations
def ease(u):
    return (1 - math.cos(math.pi * u)) / 2


def keys(*pairs, step=0.05):
    """Mốc (t, [x,y,z]) noi suy ease-in-out, lay mau moi `step` giay -> chuyen dong muot, khong giat."""
    out = {}
    for (t0, v0), (t1, v1) in zip(pairs, pairs[1:]):
        n = max(1, round((t1 - t0) / step))
        for k in range(n):
            u = k / n
            s = ease(u)
            out[round(t0 + (t1 - t0) * u, 4)] = [a + (b - a) * s for a, b in zip(v0, v1)]
    t_last, v_last = pairs[-1]
    out[round(t_last, 4)] = list(v_last)
    return {str(float(t)): [round(c, 3) for c in v] for t, v in sorted(out.items())}


def wave(fn, length, step=0.05):
    """Mau tu ham tuan hoan fn(t) -> [x,y,z]; t=0 va t=length trung nhau de lap muot."""
    n = round(length / step)
    res = {}
    for i in range(n + 1):
        t = length * i / n
        res[str(float(round(t, 4)))] = [round(c, 3) for c in fn(t)]
    return res


def humanoid_anims(idle_extra, skill):
    idle = {
        "body": {"position": wave(lambda t: [0, -0.15 * (1 - math.cos(TAU * t / 3)), 0], 3.0)},
        "head": {"rotation": wave(lambda t: [-0.75 * (1 - math.cos(TAU * t / 3)),
                                             2 * math.sin(TAU * t / 3), 0], 3.0)},
        "right_arm": {"rotation": wave(lambda t: [-1.0 * (1 - math.cos(TAU * t / 3)), 0, 0], 3.0)},
        "left_arm": {"rotation": wave(lambda t: [-1.0 * (1 - math.cos(TAU * t / 3)), 0, 0], 3.0)},
    }
    idle.update(idle_extra)
    walk = {
        "right_arm": {"rotation": wave(lambda t: [-30 * math.cos(TAU * t), 0, 0], 1.0)},
        "left_arm": {"rotation": wave(lambda t: [30 * math.cos(TAU * t), 0, 0], 1.0)},
        "right_leg": {"rotation": wave(lambda t: [30 * math.cos(TAU * t), 0, 0], 1.0)},
        "left_leg": {"rotation": wave(lambda t: [-30 * math.cos(TAU * t), 0, 0], 1.0)},
        "head": {"position": wave(lambda t: [0, -0.25 * (1 - math.cos(2 * TAU * t)), 0], 1.0)},
        "body": {"position": wave(lambda t: [0, -0.2 * (1 - math.cos(2 * TAU * t)), 0], 1.0)},
    }
    attack = {
        "right_arm": {"rotation": keys((0, [-20, 0, 0]), (0.1, [15, 0, 0]), (0.22, [-135, 0, 0]),
                                       (0.32, [-110, 0, 0]), (0.5, [-20, 0, 0]))},
        "left_arm": {"rotation": keys((0, [-20, 0, 0]), (0.22, [-70, 0, 0]), (0.5, [-20, 0, 0]))},
        "head": {"rotation": keys((0, [0, 0, 0]), (0.22, [-12, 0, 0]), (0.5, [0, 0, 0]))},
        "body": {"rotation": keys((0, [0, 0, 0]), (0.1, [4, 0, 0]), (0.22, [-8, 0, 0]), (0.5, [0, 0, 0]))},
    }
    return {"idle": (True, 3.0, idle), "walk": (True, 1.0, walk),
            "attack": (False, 0.5, attack), "skill": (False, 0.8, skill)}


def thi_anims():
    spikes = {}
    for name, sign in (("spike_r_a", 1), ("spike_r_b", 1), ("spike_l_a", -1), ("spike_l_b", -1)):
        spikes[name] = {"rotation": wave(lambda t, s=sign: [0, 0, 3 * s * math.sin(TAU * t / 3)], 3.0)}
    skill = {  # vung 2 tay len cao roi dap xuong, nguoi cui theo
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (0.25, [-160, 0, 0]), (0.4, [-170, 0, 0]),
                                       (0.55, [30, 0, 0]), (0.8, [0, 0, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (0.3, [-150, 0, 0]), (0.45, [-160, 0, 0]),
                                      (0.6, [25, 0, 0]), (0.8, [0, 0, 0]))},
        "body": {"rotation": keys((0, [0, 0, 0]), (0.25, [-8, 0, 0]), (0.4, [-6, 0, 0]),
                                  (0.55, [18, 0, 0]), (0.8, [0, 0, 0])),
                 "position": keys((0, [0, 0, 0]), (0.4, [0, 0, 0]), (0.55, [0, -0.5, 0]), (0.8, [0, 0, 0]))},
        "head": {"rotation": keys((0, [0, 0, 0]), (0.4, [-10, 0, 0]), (0.55, [12, 0, 0]), (0.8, [0, 0, 0]))},
    }
    return humanoid_anims(spikes, skill)


def cot_anims():
    shards = {}
    for i, (name, ph) in enumerate((("shard_1", 0.0), ("shard_2", 2.1), ("shard_3", 4.2)), 1):
        shards[name] = {
            "position": wave(lambda t, p=ph: [0, 0.4 * math.sin(TAU * t / 3 + p), 0], 3.0),
            "rotation": wave(lambda t, p=ph: [0, 0, 6 * math.sin(TAU * t / 3 + p)], 3.0),
        }
    skill = {  # quet 2 tay ngang kem xoay than, cac manh xuong bay quanh
        "right_arm": {"rotation": keys((0, [0, 0, 0]), (0.3, [-90, 0, -60]), (0.5, [-90, 0, 40]),
                                       (0.8, [0, 0, 0]))},
        "left_arm": {"rotation": keys((0, [0, 0, 0]), (0.3, [-90, 0, 60]), (0.5, [-90, 0, -40]),
                                      (0.8, [0, 0, 0]))},
        "body": {"rotation": keys((0, [0, 0, 0]), (0.3, [0, -25, 0]), (0.5, [0, 30, 0]), (0.8, [0, 0, 0]))},
        "head": {"rotation": keys((0, [0, 0, 0]), (0.3, [0, -15, 0]), (0.5, [0, 20, 0]), (0.8, [0, 0, 0]))},
        "shard_1": {"rotation": keys((0, [0, 0, 0]), (0.8, [0, 360, 0]))},
        "shard_2": {"rotation": keys((0, [0, 0, 0]), (0.8, [0, -360, 0]))},
        "shard_3": {"rotation": keys((0, [0, 0, 0]), (0.8, [0, 360, 0]))},
    }
    return humanoid_anims(shards, skill)


def spider_anims():
    idle = {
        "body": {"position": wave(lambda t: [0, -0.2 * (1 - math.cos(math.pi * t)), 0], 2.0)},
        "head": {"rotation": wave(lambda t: [1.0 * (1 - math.cos(math.pi * t)), 0, 0], 2.0)},
    }
    walk = {"body": {"position": wave(lambda t: [0, -0.3 * (1 - math.cos(TAU * t / 0.8)), 0], 0.8)}}
    attack = {
        "head": {"position": keys((0, [0, 0, 0]), (0.15, [0, 0, -3]), (0.5, [0, 0, 0])),
                 "rotation": keys((0, [0, 0, 0]), (0.15, [-8, 0, 0]), (0.5, [0, 0, 0]))},
        "body": {"position": keys((0, [0, 0, 0]), (0.15, [0, 0, -1]), (0.5, [0, 0, 0]))},
    }
    skill = {  # ngoc dau len, nha to tuoi - phun to
        "head": {"rotation": keys((0, [0, 0, 0]), (0.3, [-30, 0, 0]), (0.55, [-30, 0, 0]), (0.8, [0, 0, 0])),
                 "position": keys((0, [0, 0, 0]), (0.3, [0, 0, -2]), (0.55, [0, 0, -4]), (0.8, [0, 0, 0]))},
        "body": {"rotation": keys((0, [0, 0, 0]), (0.3, [-12, 0, 0]), (0.8, [0, 0, 0])),
                 "position": keys((0, [0, 0, 0]), (0.3, [0, 0.4, 0]), (0.55, [0, 0.4, 0]), (0.8, [0, 0, 0]))},
    }
    for i in range(8):
        side = 1 if i % 2 == 0 else -1
        phase = 1 if (i // 2) % 2 else -1          # hai nhom chan xen ke kieu tripod
        a = 25 * side * phase
        idle[f"leg{i}"] = {"rotation": wave(lambda t, s=side: [0, 3 * s * math.cos(math.pi * t), 0], 2.0)}
        walk[f"leg{i}"] = {"rotation": wave(lambda t, a=a: [0, a * math.cos(TAU * t / 0.8), 0], 0.8)}
        skill[f"leg{i}"] = {"rotation": keys((0, [0, 0, 0]), (0.3, [0, 28 * side, 0]),
                                             (0.55, [0, 17 * side, 0]), (0.8, [0, 0, 0]))}
    return {"idle": (True, 2.0, idle), "walk": (True, 0.8, walk),
            "attack": (False, 0.5, attack), "skill": (False, 0.8, skill)}


def anim_json(mob, animset):
    return {
        "format_version": "1.8.0",
        "animations": {
            f"animation.{mob}.{name}": {"loop": loop, "animation_length": length, "bones": bones}
            for name, (loop, length, bones) in animset.items()
        },
    }


# ---------------------------------------------------------------- texture
class Noise:
    """Value noise tuan hoan 16 o luoi, noi suy smoothstep. cell = kich thuoc o (px)."""

    def __init__(self, seed, cell):
        rnd = random.Random(seed)
        self.g = [[rnd.random() for _ in range(16)] for _ in range(16)]
        self.cell = cell

    def __call__(self, x, y):
        fx, fy = x / self.cell, y / self.cell
        ix, iy = math.floor(fx), math.floor(fy)
        tx, ty = fx - ix, fy - iy
        tx = tx * tx * (3 - 2 * tx)
        ty = ty * ty * (3 - 2 * ty)
        g = self.g
        a = g[iy % 16][ix % 16]
        b = g[iy % 16][(ix + 1) % 16]
        c = g[(iy + 1) % 16][ix % 16]
        d = g[(iy + 1) % 16][(ix + 1) % 16]
        return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty


def hash01(x, y, seed):
    h = ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 0x5BD1E995) & 0xFFFFFFFF
    h ^= h >> 15
    return (h & 0xFFFF) / 65536


def mix(c, target, t):
    return tuple(a + (b - a) * t for a, b in zip(c, target))


def shade_pixel(mob, part, face, rect, x, y, base, phase, hseed, nb, ns, nv):
    """Tra ve (rgb da shading, mau glow hoac None) cho 1 texel."""
    x0, y0, x1, y1 = rect
    n = 0.6 * nb(x, y) + 0.4 * ns(x, y)              # 0..1
    lum = SHADE[face] * TONES[min(3, int(n * 4))]    # 4 tong
    if x in (x0, x1 - 1) or y in (y0, y1 - 1):       # vien cube cho ro khoi
        lum *= 0.72
    col = base
    h = hash01(x, y, hseed)
    glow = None

    if mob == "hu_thi":
        if part == "skin":
            if n < 0.24:                               # mang thit thoi
                col = mix(col, (66, 78, 40), 0.55)
            if h < 0.03:                               # dom tan nhang
                lum *= 0.8
        elif part == "shirt":
            if (y - y0) % 4 == 2 and (x - x0) % 2 == 0:  # duong chi khau
                col = (170, 190, 180)
                lum = 1.0
            if n > 0.72:                               # ao rach
                lum *= 0.85
        elif part == "pants":
            if x % 4 == 0:                             # soc quan
                lum *= 0.85
            if h < 0.02:
                col = (20, 20, 30)
        elif part == "spike":
            if (y - y0) % 2 == 0:
                lum *= 0.85
    elif mob == "hu_cot":
        if n > 0.55 and abs(math.sin(0.23 * x + 1.1 * y + phase)) < 0.05:
            col = (120, 108, 90)                       # vet nut xuong
        if n > 0.82 and h < 0.5:                       # lo xuong
            lum *= 0.8
    elif mob == "hu_chu":
        if part == "carapace":
            if face == "top":
                lum *= 1.1                             # vet bong ban
            if h < 0.05:                               # long nhen
                col = (165, 130, 190)
        elif part == "leg":
            if (y - y0) % 3 == 0:                      # dot chan
                lum *= 0.7
        elif part == "egg" and h < 0.06:
            glow = (150, 90, 220)                      # dom sang tren tui trung

    rgb = tuple(min(255, int(c * lum)) for c in col)
    # van hu khong: vet sang (glowmask) + vet tim lop giua
    s = math.sin(0.8 * x + 0.45 * y + phase) + 0.6 * math.sin(0.31 * x - 0.9 * y + 1.7 * phase)
    at = abs(s)
    if nv(x, y) > 0.5:
        if at < 0.09:
            glow = GLOW_VEIN
            rgb = tuple(int(c) for c in mix(rgb, GLOW_VEIN, 0.8))
        elif at < 0.2:
            rgb = tuple(int(c) for c in mix(rgb, VOID_MID, 0.55))
    return rgb, glow


def paint_mob(mob, bones, eyes):
    base = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    pb, pg = base.load(), glow.load()
    seed = zlib.crc32(mob.encode())
    nb, ns, nv = Noise(seed, 8), Noise(seed + 1, 4), Noise(seed + 2, 6)

    def part_of(name):
        for pre, part in PARTS[mob]:
            if name == pre or name.startswith(pre):
                return part
        raise KeyError(f"{mob}: chua co nhom mau cho bone {name}")

    for b in bones:
        pal = PALETTE[mob][part_of(b["name"])]
        part = part_of(b["name"])
        for c in b["cubes"]:
            u, v = c["uv"]
            w, h, d = c["size"]
            for face, rect in face_rects(u, v, w, h, d).items():
                key = f"{mob}|{b['name']}|{face}|{u},{v}"
                k = zlib.crc32(key.encode())
                phase, hseed = (k % 10000) / 10000 * TAU, k & 0xFFFF
                x0, y0, x1, y1 = rect
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        rgb, g = shade_pixel(mob, part, face, rect, x, y, pal, phase, hseed, nb, ns, nv)
                        pb[x, y] = (*rgb, 255)
                        pg[x, y] = (0, 0, 0, 0) if g is None else (*g, 255)

    for x0, y0, w, h, kind in eyes:
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if kind == "glow":
                    pb[x, y] = (*GLOW, 255)
                    pg[x, y] = (*GLOW, 255)
                else:
                    pb[x, y] = (*SOCKET, 255)
                    pg[x, y] = (0, 0, 0, 0)
    return base, glow


# ---------------------------------------------------------------- main
MOBS = {
    "hu_thi": {"bones": lambda: humanoid_bones(thin=False), "anims": thi_anims, "vb": (2.5, 2.5, 1.0)},
    "hu_cot": {"bones": lambda: humanoid_bones(thin=True), "anims": cot_anims, "vb": (2.5, 2.5, 1.0)},
    "hu_chu": {"bones": spider_bones, "anims": spider_anims, "vb": (3.0, 1.5, 0.5)},
}


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    tex_dir = ASSETS / "textures" / "entity"
    tex_dir.mkdir(parents=True, exist_ok=True)
    for mob, spec in MOBS.items():
        bones = spec["bones"]()
        check_uv(mob, bones)

        head = next(b for b in bones if b["name"] == "head")["cubes"][0]
        fx0, fy0, fx1, fy1 = face_rects(*head["uv"], *head["size"])["front"]
        for x, y, w, h, _ in EYES[mob]:
            assert fx0 <= x and x + w <= fx1 and fy0 <= y and y + h <= fy1, f"{mob} mat {(x, y)} ngoai mat truoc dau"

        write_json(ASSETS / "geo" / "entity" / f"{mob}.geo.json", geo_json(mob, bones, spec["vb"]))
        write_json(ASSETS / "animations" / "entity" / f"{mob}.animation.json",
                   anim_json(mob, spec["anims"]()))

        base, glow = paint_mob(mob, bones, EYES[mob])
        base.save(tex_dir / f"{mob}.png", "PNG")
        glow.save(tex_dir / f"{mob}_glowmask.png", "PNG")
        print(f"[OK] {mob}: geo, animation, texture + glowmask")


if __name__ == "__main__":
    main()
