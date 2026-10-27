"""Engine cho gen_models_v2.py: geo/texture/glowmask/animation GeckoLib (Bedrock geo 1.12.0).
Thiet kich ORIGINAL; khong dung asset cua hang nao."""
import json, math, random
from pathlib import Path
from PIL import Image

TAU = 2 * math.pi
ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "src/main/resources/assets/hukhong"
SHADE = {"north": .92, "south": .72, "east": .8, "west": .8, "up": 1.05, "down": .55}


def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def mul(c, f): return tuple(max(0, min(255, int(v * f))) for v in c)


class Mat:
    def __init__(s, kind, base, accent=None, glow=None, noise=.07):
        s.kind, s.base, s.noise = kind, base, noise
        s.hi, s.lo = mix(base, (255, 255, 255), .22), mul(base, .5)
        s.accent = accent
        s.glow = glow or accent or (200, 140, 255)
        s.glow_hi = mix(s.glow, (255, 255, 255), .6)


def plate(b, a=None, g=None): return Mat("plate", b, a, g)
def cloth(b, a=None, g=None): return Mat("cloth", b, a, g, .1)
def skin(b, g=None): return Mat("skin", b, None, g, .08)
def bone(b, g=None): return Mat("bone", b, None, g, .06)
def crystal(b, g): return Mat("crystal", b, g, g, .05)
def glow(g): return Mat("glow", g, None, g, 0)


def px(m, face, x, y, w, h, rng):
    """tra ve (rgba, emit_rgba|None)"""
    if m.kind == "glow":
        d = max(abs(x - (w - 1) / 2) / (w / 2 + .01), abs(y - (h - 1) / 2) / (h / 2 + .01))
        c = mix(m.glow_hi, m.glow, min(1, d))
        return c + (255,), c + (255,)
    c = mul(m.base, SHADE[face] * (1 + (rng.random() - .5) * 2 * m.noise))
    emit = None
    k = m.kind
    if k == "plate":
        if x == 0 or y == 0: c = mix(c, m.hi, .65)
        elif x == w - 1 or y == h - 1: c = mix(c, m.lo, .7)
        if w >= 6 and h >= 6 and (x * 3 + y * 5) % 13 == 0: c = mix(c, m.lo, .55)
        if face in ("north", "south") and h >= 8 and y == h // 2: c = mix(c, m.lo, .6)
        if m.accent and h >= 5 and w >= 4 and (x == 1 or x == w - 2) and 1 <= y < h - 1:
            c = mix(c, m.accent, .5)
    elif k == "cloth":
        if x % 3 == 0: c = mul(c, .86)
        if y >= h - 2: c = mul(c, .8)
        if y == h - 1 and rng.random() < .38 and h > 3: return (0, 0, 0, 0), None
        if m.accent and y == 1 and w > 3: c = mix(c, m.accent, .6)
    elif k == "skin":
        if (x * 7 + y * 11) % 19 == 0: c = mix(c, m.lo, .6)
        if m.glow and (x * 5 + y * 3) % 23 == 0 and w > 2 and h > 2:
            c = m.glow; emit = c + (255,)
    elif k == "bone":
        if (x + y * 2) % 9 == 0 or (x * 3 - y) % 11 == 0: c = mix(c, m.lo, .5)
        if x == 0 or y == 0: c = mix(c, m.hi, .5)
        if m.glow and (x * 5 + y * 7) % 29 == 0: c = m.glow; emit = c + (255,)
    elif k == "crystal":
        t = ((x + y) % 5) / 4
        c = mix(m.base, m.hi, t)
        if (x + y) % 5 == 0: c = m.glow; emit = c + (255,)
        elif (x + y) % 5 == 1: emit = mix(m.glow, m.base, .4) + (200,)
    return c + (255,), emit


class Model:
    def __init__(s, id, tw, bounds=(4, 4), off=1.5):
        s.id, s.tw, s.bounds, s.off = id, tw, bounds, off
        s.bones, s.cubes, s.names = [], [], set()
        s.seed = sum(map(ord, id))

    def bone(s, name, parent=None, pivot=(0, 0, 0), rot=None):
        assert name not in s.names, name
        s.names.add(name)
        s.bones.append(dict(name=name, parent=parent, pivot=list(pivot), rot=rot))
        return name

    def cube(s, bone, o, sz, mat, rot=None, pivot=None, rune=False, eyes=(), share=False):
        w, h, d = [max(1, int(round(v))) for v in sz]
        assert bone in s.names, bone
        s.cubes.append(dict(bone=bone, o=list(o), sz=(w, h, d), mat=mat, rot=rot, pivot=pivot,
                            rune=rune, eyes=eyes, idx=len(s.cubes), key=(id(mat), w, h, d) if share else None))

    def ring(s, bone, c, R, mat, mat2=None, bead=3, thick=1, every=4):
        """vong rune bang hat khoi (khong xoay cube -> khong lech dau)"""
        n = max(8, int(TAU * R / (bead + .5)))
        for i in range(n):
            a = TAU * i / n
            mt = mat2 if (mat2 and i % every == 0) else mat
            s.cube(bone, (c[0] + R * math.cos(a) - bead / 2, c[1], c[2] + R * math.sin(a) - bead / 2),
                   (bead, thick, bead), mt, share=True)

    def horn(s, bone, base, dirx, mat, segs=4, w0=4, grow=-0.8, up=5, out=3, curl=18):
        """sung cong: chuoi cube nho dan, xoay dan theo z"""
        x, y, z = base
        for i in range(segs):
            w = max(1, w0 + grow * i)
            s.cube(bone, (x - w / 2, y, z - w / 2), (w, up, w), mat, rot=[0, 0, -dirx * curl * (i + 1) * .6])
            x += dirx * out * (1 - i * .15); y += up * .85

    def build(s):
        items = sorted(s.cubes, key=lambda c: (-(c["sz"][1] + c["sz"][2]), -(c["sz"][0] + c["sz"][2])))
        tw = s.tw; x = y = sh = 0
        owner = {}
        for c in items:
            if c["key"] is not None and c["key"] in owner:
                c["uv"] = owner[c["key"]]["uv"]; c["dup"] = True; continue
            if c["key"] is not None: owner[c["key"]] = c
            w, h, d = c["sz"]; bw, bh = 2 * (w + d), h + d
            if x + bw > tw: x, y, sh = 0, y + sh, 0
            c["uv"] = (x, y); x += bw; sh = max(sh, bh)
            assert y + sh <= tw, f"{s.id}: texture {tw} overflow ({len(s.cubes)} cubes)"
        used = y + sh
        tex = Image.new("RGBA", (tw, tw), (0, 0, 0, 0)); gm = Image.new("RGBA", (tw, tw), (0, 0, 0, 0))
        T, G = tex.load(), gm.load()
        for c in s.cubes:
            if c.get("dup"): continue
            rng = random.Random(s.seed * 1000 + c["idx"])
            w, h, d = c["sz"]; u, v = c["uv"]; m = c["mat"]
            faces = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
                     "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}
            for f, (fx, fy, fw, fh) in faces.items():
                for yy in range(fh):
                    for xx in range(fw):
                        col, em = px(m, f, xx, yy, fw, fh, rng)
                        T[fx + xx, fy + yy] = col
                        if em: G[fx + xx, fy + yy] = em
                if c["rune"] and f in ("north", "south") and fw >= 5 and fh >= 5:
                    rc = (m.accent or m.glow) + (255,); cx = fw // 2
                    for yy in range(1, fh - 1):
                        if yy % 4 != 3: T[fx + cx, fy + yy] = rc; G[fx + cx, fy + yy] = rc
                    for dx in (-2, 2):
                        if 0 <= cx + dx < fw:
                            T[fx + cx + dx, fy + fh // 2] = rc; G[fx + cx + dx, fy + fh // 2] = rc
            fx, fy = u + d, v + d
            for (ex, ey, ew, eh, ec) in c["eyes"]:
                for yy in range(-1, eh + 1):
                    for xx in range(-1, ew + 1):
                        X, Y = ex + xx, ey + yy
                        if not (0 <= X < w and 0 <= Y < h): continue
                        if 0 <= xx < ew and 0 <= yy < eh:
                            T[fx + X, fy + Y] = ec + (255,); G[fx + X, fy + Y] = ec + (255,)
                        elif G[fx + X, fy + Y][3] == 0:
                            T[fx + X, fy + Y] = (8, 6, 14, 255)
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
                if c["rot"]:
                    j["pivot"] = c["pivot"] or [round(c["o"][0] + w / 2, 3), round(c["o"][1] + h / 2, 3), round(c["o"][2] + d / 2, 3)]
                    j["rotation"] = c["rot"]
                cs.append(j)
            if cs: e["cubes"] = cs
            bl.append(e)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
            "identifier": f"geometry.{s.id}", "texture_width": s.tw, "texture_height": s.tw,
            "visible_bounds_width": s.bounds[0], "visible_bounds_height": s.bounds[1],
            "visible_bounds_offset": [0, s.off, 0]}, "bones": bl}]}
        (A / "geo/entity" / f"{s.id}.geo.json").write_text(json.dumps(geo, indent=1))
        tex.save(A / "textures/entity" / f"{s.id}.png"); gm.save(A / "textures/entity" / f"{s.id}_glowmask.png")
        top = max((c["o"][1] + c["sz"][1]) for c in s.cubes)
        print(f"{s.id}: {len(s.bones)} bones {len(s.cubes)} cubes tex{tw} used_h={used} top_y={top:.0f}px")
        return geo


# ================================================================= animation
def fmt(t): return f"{round(t, 4):g}"


class Anim:
    def __init__(s, id, name, length, loop):
        s.n, s.len, s.loop, s.b = f"animation.{id}.{name}", length, loop, {}

    def kf(s, bone, ch, frames):
        d = s.b.setdefault(bone, {}).setdefault(ch, {})
        for t, v in frames: d[fmt(t)] = [round(a, 3) for a in v]

    def wave(s, bone, ch, fn, n=16):
        s.kf(bone, ch, [(s.len * i / n, fn(s.len * i / n)) for i in range(n + 1)])

    def json(s):
        return s.n, {"loop": s.loop, "animation_length": s.len, "bones": s.b}


def ax(i, v):
    z = [0, 0, 0]; z[i] = v; return z


def build_anims(id, geo, cfg):
    """cfg: lengths{name:len}, skills{name:kind}, roles, sway, spin, bob, stride, pulse, quad, legs"""
    names = {b["name"] for b in geo["minecraft:geometry"][0]["bones"]}
    R = dict(body="body", head="head", ra="right_arm", la="left_arm", rl="right_leg", ll="left_leg")
    R.update(cfg.get("roles", {}))
    L = cfg["lengths"]
    sway = cfg.get("sway", [])    # (bone, axis, amp, period_mul, phase)
    spin = cfg.get("spin", [])    # (bone, axis, turns, dir)
    bob = cfg.get("bob", 0.0)
    stride = cfg.get("stride", 28)
    pulse = cfg.get("pulse", [])
    legs = cfg.get("legs", True) and R["rl"] in names
    quad = cfg.get("quad")
    hasarms = R["ra"] in names
    ra, la, bd, hd = R["ra"], R["la"], R["body"], R["head"]
    out = []

    def extras(a, per):
        for (b, i, amp, pm, ph) in sway:
            a.wave(b, "rotation", lambda t, i=i, amp=amp, pm=pm, ph=ph: ax(i, amp * math.sin(TAU * t / (per * pm) + ph)))

    # ---- idle
    a = Anim(id, "idle", L["idle"], True); P = a.len
    if bob:
        a.wave(bd, "position", lambda t: [0, bob * math.sin(TAU * t / P), 0])
    else:
        a.wave(bd, "position", lambda t: [0, -.25 * (1 - math.cos(TAU * t / P)), 0])
    a.wave(bd, "rotation", lambda t: [1.2 * math.sin(TAU * t / P), 0, .8 * math.sin(TAU * t / P + 1)])
    a.wave(hd, "rotation", lambda t: [2.5 * math.sin(TAU * t / P + .8), 3 * math.sin(TAU * t / P), 0])
    if hasarms:
        a.wave(ra, "rotation", lambda t: [2 + 3 * math.sin(TAU * t / P + .3), 0, -2 - 1.5 * math.sin(TAU * t / P)])
        a.wave(la, "rotation", lambda t: [2 + 3 * math.sin(TAU * t / P + 1.3), 0, 2 + 1.5 * math.sin(TAU * t / P)])
    for (b, i, turns, d) in spin:
        a.wave(b, "rotation", lambda t, i=i, turns=turns, d=d: ax(i, d * 360 * turns * t / P), 24)
    extras(a, P)
    for b in cfg.get("float_with", []):
        a.wave(b, "position", lambda t: [0, bob * math.sin(TAU * t / P), 0])
    out.append(a)
    # ---- walk
    a = Anim(id, "walk", L["walk"], True); P = a.len
    if quad:
        for (b, ph) in quad:
            a.wave(b, "rotation", lambda t, ph=ph: [0, 22 * math.sin(TAU * t / P + ph), 8 * max(0, math.sin(TAU * t / P + ph + 1.57))])
        a.wave(bd, "position", lambda t: [0, .5 * math.sin(2 * TAU * t / P), 0])
    else:
        if legs:
            a.wave(R["rl"], "rotation", lambda t: [stride * math.cos(TAU * t / P), 0, 0])
            a.wave(R["ll"], "rotation", lambda t: [-stride * math.cos(TAU * t / P), 0, 0])
        if bob: a.wave(bd, "position", lambda t: [0, bob * math.sin(TAU * t / P), 0])
        else: a.wave(bd, "position", lambda t: [0, -.45 * abs(math.cos(TAU * t / P)), 0])
    if hasarms:
        a.wave(ra, "rotation", lambda t: [-stride * .9 * math.cos(TAU * t / P), 0, -3])
        a.wave(la, "rotation", lambda t: [stride * .9 * math.cos(TAU * t / P), 0, 3])
    a.wave(bd, "rotation", lambda t: [3, 3 * math.cos(TAU * t / P), 0])
    a.wave(hd, "rotation", lambda t: [-1.5, -3 * math.cos(TAU * t / P), 0])
    for (b, i, turns, d) in spin:
        a.wave(b, "rotation", lambda t, i=i, turns=turns, d=d: ax(i, d * 360 * turns * 2 * t / P), 24)
    extras(a, P / 2)
    out.append(a)
    # ---- skills (one-shot)
    for name, kind in cfg["skills"].items():
        a = Anim(id, name, L[name], False); T = a.len
        f = lambda *p, T=T: [(T * u, v) for u, v in p]
        if kind == "strike":
            if hasarms:
                a.kf(ra, "rotation", f((0, [0, 0, 0]), (.2, [20, 0, -10]), (.45, [-140, 0, 0]), (.6, [20, 0, 0]), (1, [0, 0, 0])))
                a.kf(la, "rotation", f((0, [0, 0, 0]), (.4, [-40, 0, 0]), (1, [0, 0, 0])))
            a.kf(bd, "rotation", f((0, [0, 0, 0]), (.2, [6, 12, 0]), (.5, [-10, -14, 0]), (1, [0, 0, 0])))
            a.kf(hd, "rotation", f((0, [0, 0, 0]), (.45, [-14, 0, 0]), (1, [0, 0, 0])))
        elif kind == "slam":
            if hasarms:
                a.kf(ra, "rotation", f((0, [0, 0, 0]), (.4, [-170, 0, -15]), (.5, [-176, 0, -15]), (.6, [-5, 0, 0]), (1, [0, 0, 0])))
                a.kf(la, "rotation", f((0, [0, 0, 0]), (.4, [-165, 0, 15]), (.5, [-172, 0, 15]), (.6, [-5, 0, 0]), (1, [0, 0, 0])))
            a.kf(bd, "rotation", f((0, [0, 0, 0]), (.45, [-12, 0, 0]), (.6, [18, 0, 0]), (1, [0, 0, 0])))
            a.kf(bd, "position", f((0, [0, 0, 0]), (.45, [0, 1.5, 0]), (.6, [0, -2.5, 0]), (1, [0, 0, 0])))
            a.kf(hd, "rotation", f((0, [0, 0, 0]), (.45, [-16, 0, 0]), (.6, [14, 0, 0]), (1, [0, 0, 0])))
        elif kind == "cast":
            if hasarms:
                a.kf(ra, "rotation", f((0, [0, 0, 0]), (.3, [-100, 0, -50]), (.6, [-115, 0, -25]), (.8, [-60, 0, -8]), (1, [0, 0, 0])))
                a.kf(la, "rotation", f((0, [0, 0, 0]), (.3, [-100, 0, 50]), (.6, [-115, 0, 25]), (.8, [-60, 0, 8]), (1, [0, 0, 0])))
            a.kf(bd, "rotation", f((0, [0, 0, 0]), (.3, [-8, 0, 0]), (.7, [-4, 0, 0]), (1, [0, 0, 0])))
            a.kf(bd, "position", f((0, [0, 0, 0]), (.4, [0, 1.2, 0]), (.8, [0, 1.2, 0]), (1, [0, 0, 0])))
            a.kf(hd, "rotation", f((0, [0, 0, 0]), (.35, [-18, 0, 0]), (.8, [-10, 0, 0]), (1, [0, 0, 0])))
        elif kind == "sweep":
            if hasarms:
                a.kf(ra, "rotation", f((0, [0, 0, 0]), (.25, [-70, 20, -70]), (.55, [-80, -20, 60]), (1, [0, 0, 0])))
                a.kf(la, "rotation", f((0, [0, 0, 0]), (.3, [-30, 0, 20]), (1, [0, 0, 0])))
            a.kf(bd, "rotation", f((0, [0, 0, 0]), (.25, [0, -45, 0]), (.55, [0, 55, 0]), (1, [0, 0, 0])))
            a.kf(hd, "rotation", f((0, [0, 0, 0]), (.25, [0, 20, 0]), (.55, [0, -25, 0]), (1, [0, 0, 0])))
        elif kind == "roar":
            if hasarms:
                a.kf(ra, "rotation", f((0, [0, 0, 0]), (.2, [-30, 0, -45]), (.7, [-30, 0, -50]), (1, [0, 0, 0])))
                a.kf(la, "rotation", f((0, [0, 0, 0]), (.2, [-30, 0, 45]), (.7, [-30, 0, 50]), (1, [0, 0, 0])))
            a.kf(bd, "rotation", f((0, [0, 0, 0]), (.2, [-14, 0, 0]), (.8, [-12, 0, 0]), (1, [0, 0, 0])))
            a.kf(hd, "rotation", f((0, [0, 0, 0]), (.2, [-32, 0, 0]), (.3, [-26, 3, 0]), (.4, [-32, -3, 0]),
                                   (.5, [-26, 3, 0]), (.6, [-32, -3, 0]), (.8, [-30, 0, 0]), (1, [0, 0, 0])))
        elif kind == "phase":
            if hasarms:
                a.kf(ra, "rotation", f((0, [0, 0, 0]), (.25, [-20, 0, -80]), (.7, [-30, 0, -90]), (1, [0, 0, 0])))
                a.kf(la, "rotation", f((0, [0, 0, 0]), (.25, [-20, 0, 80]), (.7, [-30, 0, 90]), (1, [0, 0, 0])))
            a.kf(bd, "position", f((0, [0, 0, 0]), (.3, [0, 3, 0]), (.75, [0, 3.5, 0]), (1, [0, 0, 0])))
            a.kf(bd, "rotation", f((0, [0, 0, 0]), (.3, [-10, 0, 0]), (.4, [-10, 2, 1]), (.5, [-10, -2, -1]),
                                   (.6, [-10, 2, 1]), (.75, [-8, 0, 0]), (1, [0, 0, 0])))
            a.kf(hd, "rotation", f((0, [0, 0, 0]), (.3, [-25, 0, 0]), (.75, [-20, 0, 0]), (1, [0, 0, 0])))
        else:
            raise ValueError(kind)
        if legs and not quad:
            a.kf(R["rl"], "rotation", f((0, [0, 0, 0]), (.4, [-8, 0, 0]), (1, [0, 0, 0])))
            a.kf(R["ll"], "rotation", f((0, [0, 0, 0]), (.4, [8, 0, 0]), (1, [0, 0, 0])))
        if quad:
            for (b, ph) in quad[:2]:
                a.kf(b, "rotation", f((0, [0, 0, 0]), (.4, [-30, 0, -20]), (.8, [-20, 0, -10]), (1, [0, 0, 0])))
        for (b, i, turns, d) in spin:
            a.wave(b, "rotation", lambda t, i=i, d=d, T=T: ax(i, d * 720 * (t / T) ** 1.5), 12)
        for b in pulse:
            a.kf(b, "scale", f((0, [1, 1, 1]), (.4, [1.35, 1.35, 1.35]), (.7, [1.2, 1.2, 1.2]), (1, [1, 1, 1])))
        for (b, i, amp, pm, ph) in sway:
            a.kf(b, "rotation", f((0, [0, 0, 0]), (.4, ax(i, amp * 3)), (1, [0, 0, 0])))
        out.append(a)
    # ---- death (khong dung trong code; an toan)
    a = Anim(id, "death", 1.6, "hold_on_last_frame"); T = a.len
    f = lambda *p: [(T * u, v) for u, v in p]
    a.kf(bd, "rotation", f((0, [0, 0, 0]), (.4, [-12, 0, 6]), (1, [-80, 0, 10])))
    a.kf(bd, "position", f((0, [0, 0, 0]), (.5, [0, -2, 0]), (1, [0, -14 if bob else -10, 0])))
    a.kf(hd, "rotation", f((0, [0, 0, 0]), (.5, [20, 0, 10]), (1, [35, 0, 15])))
    if hasarms:
        a.kf(ra, "rotation", f((0, [0, 0, 0]), (.5, [-30, 0, -30]), (1, [30, 0, -50])))
        a.kf(la, "rotation", f((0, [0, 0, 0]), (.5, [-20, 0, 30]), (1, [20, 0, 60])))
    for (b, i, turns, d) in spin:
        a.kf(b, "scale", f((0, [1, 1, 1]), (1, [.2, .2, .2])))
    out.append(a)
    anims = dict(o.json() for o in out)
    for an, d in anims.items():
        for b in d["bones"]: assert b in names, f"{id}: anim bone {b} missing in geo"
    (A / "animations/entity" / f"{id}.animation.json").write_text(
        json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1))
