#!/usr/bin/env python3
"""Bộ dựng nhân vật hình người GeckoLib có VẼ TỪNG MẶT (mặt, râu, kính, viền áo...).
Mỗi cube có painter(face, x, y, w, h) -> (r,g,b[,a]) hoặc None; có thể đánh dấu phát sáng (glowmask).
Bố cục UV giống _gm2_engine (box UV): up/down/east/north/west/south; north = mặt trước (-z)."""
import json, random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "src/main/resources/assets/hukhong"


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


FACE_SHADE = {"up": 1.08, "down": .7, "north": 1.0, "south": .86, "east": .92, "west": .92}


class Paint:
    """painter tiện dụng: màu nền + nhiễu + viền + ảnh pixel cho từng mặt."""
    def __init__(s, base, noise=.06, trim=None, trim_w=0, trim_faces=None, bottom=None, bottom_h=0,
                 art=None, pal=None, glow=None, pattern=None, seed=0, alpha_fringe=0):
        s.base = hexc(base) if isinstance(base, str) else base
        s.noise, s.trim, s.trim_w, s.trim_faces = noise, (hexc(trim) if isinstance(trim, str) else trim), trim_w, trim_faces
        s.bottom, s.bottom_h = (hexc(bottom) if isinstance(bottom, str) else bottom), bottom_h
        s.art, s.pal = art or {}, {k: (hexc(v) if isinstance(v, str) else v) for k, v in (pal or {}).items()}
        s.glow = set(glow or ())
        s.pattern = pattern
        s.rng = random.Random(seed)
        s.alpha_fringe = alpha_fringe

    def __call__(s, face, x, y, w, h):
        art = s.art.get(face)
        if art:
            rows = art
            ah, aw = len(rows), len(rows[0])
            ay = y - (h - ah) // 2 if ah < h else int(y * ah / h)
            ax = x - (w - aw) // 2 if aw < w else int(x * aw / w)
            if 0 <= ay < ah and 0 <= ax < aw:
                ch = rows[ay][ax]
                if ch != ".":
                    if ch == " ": return (0, 0, 0, 0), False
                    c = s.pal[ch]
                    return shade(c, FACE_SHADE[face] if face != "north" else 1), ch in s.glow
        c = s.base
        if s.pattern:
            pc = s.pattern(face, x, y, w, h)
            if pc is not None: c = pc
        if s.bottom and y >= h - s.bottom_h and face not in ("up", "down"): c = s.bottom
        if s.trim and s.trim_w and (s.trim_faces is None or face in s.trim_faces):
            if face not in ("up", "down") and (y >= h - s.trim_w):
                c = s.trim
        if s.alpha_fringe and face not in ("up", "down") and y == h - 1 and (x * 7 + 3) % 5 < s.alpha_fringe:
            return (0, 0, 0, 0), False
        f = FACE_SHADE[face] * (1 + (s.rng.random() - .5) * 2 * s.noise)
        return shade(c, f), False


class Model:
    def __init__(s, id, tw=128, th=None, bounds=(3, 3), off=1.2):
        s.id, s.tw, s.th, s.bounds, s.off = id, tw, th or tw, bounds, off
        s.bones, s.cubes, s.names = [], [], set()

    def bone(s, name, parent=None, pivot=(0, 0, 0), rot=None):
        assert name not in s.names, name
        s.names.add(name)
        s.bones.append(dict(name=name, parent=parent, pivot=list(pivot), rot=rot))

    def cube(s, bone, o, sz, paint, rot=None, pivot=None, inflate=0):
        w, h, d = [max(1, int(round(v))) for v in sz]
        assert bone in s.names, bone
        s.cubes.append(dict(bone=bone, o=list(o), sz=(w, h, d), paint=paint, rot=rot, pivot=pivot, inflate=inflate, idx=len(s.cubes)))

    def build(s):
        items = sorted(s.cubes, key=lambda c: (-(c["sz"][1] + c["sz"][2]), -(c["sz"][0] + c["sz"][2])))
        tw, th = s.tw, s.th
        x = y = sh = 0
        for c in items:
            w, h, d = c["sz"]; bw, bh = 2 * (w + d), h + d
            if x + bw > tw: x, y, sh = 0, y + sh, 0
            c["uv"] = (x, y); x += bw; sh = max(sh, bh)
            assert y + sh <= th, f"{s.id}: texture {tw}x{th} overflow"
        tex = Image.new("RGBA", (tw, th), (0, 0, 0, 0)); gm = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
        T, G = tex.load(), gm.load()
        for c in s.cubes:
            w, h, d = c["sz"]; u, v = c["uv"]
            faces = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
                     "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}
            for f, (fx, fy, fw, fh) in faces.items():
                for yy in range(fh):
                    for xx in range(fw):
                        r = c["paint"](f, xx, yy, fw, fh)
                        col, gl = r if isinstance(r, tuple) and len(r) == 2 and isinstance(r[0], tuple) else (r, False)
                        if len(col) == 3: col = col + (255,)
                        T[fx + xx, fy + yy] = col
                        if gl and col[3] > 0: G[fx + xx, fy + yy] = col
        bl = []
        for b in s.bones:
            e = {"name": b["name"]}
            if b["parent"]: e["parent"] = b["parent"]
            e["pivot"] = b["pivot"]
            if b["rot"]: e["rotation"] = b["rot"]
            cs = []
            for c in s.cubes:
                if c["bone"] != b["name"]: continue
                w, h, d = c["sz"]
                j = {"origin": [round(v, 3) for v in c["o"]], "size": [w, h, d], "uv": list(c["uv"])}
                if c["inflate"]: j["inflate"] = c["inflate"]
                if c["rot"]:
                    j["pivot"] = c["pivot"] or [round(c["o"][0] + w / 2, 3), round(c["o"][1] + h / 2, 3), round(c["o"][2] + d / 2, 3)]
                    j["rotation"] = c["rot"]
                cs.append(j)
            if cs: e["cubes"] = cs
            bl.append(e)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
            "identifier": f"geometry.{s.id}", "texture_width": tw, "texture_height": th,
            "visible_bounds_width": s.bounds[0], "visible_bounds_height": s.bounds[1],
            "visible_bounds_offset": [0, s.off, 0]}, "bones": bl}]}
        (A / "geo/entity").mkdir(parents=True, exist_ok=True); (A / "textures/entity").mkdir(parents=True, exist_ok=True)
        (A / "geo/entity" / f"{s.id}.geo.json").write_text(json.dumps(geo, indent=1))
        tex.save(A / "textures/entity" / f"{s.id}.png")
        if any(gm.getpixel((i, j))[3] for i in range(tw) for j in range(th)):
            gm.save(A / "textures/entity" / f"{s.id}_glowmask.png")
        print(f"{s.id}: {len(s.bones)} bones {len(s.cubes)} cubes")
        return geo


def human_anims(id, extra_idle=None):
    """idle (thở, tay đung đưa) + walk (vung tay chân) cho khung người chuẩn."""
    def kf(vals, length):
        return {f"{round(t, 3):g}": v for t, v in vals}
    L = 2.0
    idle = {"loop": True, "animation_length": L, "bones": {
        "body": {"position": {"0": [0, 0, 0], "1": [0, 0.3, 0], "2": [0, 0, 0]}},
        "head": {"rotation": {"0": [0, 0, 0], "1": [2, 3, 0], "2": [0, 0, 0]}},
        "right_arm": {"rotation": {"0": [0, 0, 2], "1": [-3, 0, 3], "2": [0, 0, 2]}},
        "left_arm": {"rotation": {"0": [0, 0, -2], "1": [-3, 0, -3], "2": [0, 0, -2]}}}}
    if extra_idle: idle["bones"].update(extra_idle)
    W = 1.0
    walk = {"loop": True, "animation_length": W, "bones": {
        "right_leg": {"rotation": {"0": [25, 0, 0], "0.5": [-25, 0, 0], "1": [25, 0, 0]}},
        "left_leg": {"rotation": {"0": [-25, 0, 0], "0.5": [25, 0, 0], "1": [-25, 0, 0]}},
        "right_arm": {"rotation": {"0": [-20, 0, 2], "0.5": [20, 0, 2], "1": [-20, 0, 2]}},
        "left_arm": {"rotation": {"0": [20, 0, -2], "0.5": [-20, 0, -2], "1": [20, 0, -2]}},
        "body": {"position": {"0": [0, 0, 0], "0.25": [0, 0.4, 0], "0.5": [0, 0, 0], "0.75": [0, 0.4, 0], "1": [0, 0, 0]}}}}
    talk = {"loop": False, "animation_length": 1.2, "bones": {
        "right_arm": {"rotation": {"0": [0, 0, 0], "0.3": [-50, -10, 10], "0.9": [-45, -10, 8], "1.2": [0, 0, 0]}},
        "head": {"rotation": {"0": [0, 0, 0], "0.4": [6, 0, 0], "0.8": [0, 0, 0]}}}}
    anim = {"format_version": "1.8.0", "animations": {
        f"animation.{id}.idle": idle, f"animation.{id}.walk": walk, f"animation.{id}.talk": talk}}
    (A / "animations/entity").mkdir(parents=True, exist_ok=True)
    (A / "animations/entity" / f"{id}.animation.json").write_text(json.dumps(anim, indent=1))
