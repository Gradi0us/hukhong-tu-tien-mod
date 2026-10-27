#!/usr/bin/env python
"""world_to_schem.py - Java world folder (1.18+, DataVersion >= 2860) -> Sponge Schematic v2 (.schem)
+ <name>.schem.markers.json sidecar (read by com.hukhong.structures.StructureLibrary).

  python tools/world_to_schem.py <worldDir> <category> [--name N] [--bounds x1 y1 z1 x2 y2 z2]
  python tools/world_to_schem.py --all D:/mc_mod/import
  python tools/world_to_schem.py --verify file.schem

Bounds: --bounds, else <worldDir>/toado.txt ("x1 y1 z1 x2 y2 z2"), else auto-detect.
Deps: nbtlib (+numpy). Venv: tools/.venv.
"""
import argparse, collections, gzip, io, json, math, os, re, struct, sys, zlib
import numpy as np
import nbtlib
from nbtlib import tag as T

for _s in (sys.stdout, sys.stderr):
    try: _s.reconfigure(errors="replace")
    except Exception: pass
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_ROOT = os.path.join(ROOT, "run", "config", "hukhong", "structures")
MAX_XZ, MAX_Y, PAD, MIN_CHUNK_DENSITY = 256, 192, 4, 40
SKIP_NAMES = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

# ---------------- block classification (crafted / non-natural) ----------------
_ART = re.compile(r"(planks|bricks?$|_bricks|glass|_wool$|concrete|terracotta|stairs|_slab|_wall$|fence|lantern|"
                  r"_door$|trapdoor|polished_|cut_|smooth_|chiseled|bookshelf|_carpet|_bed$|_banner|_sign$|"
                  r"iron_bars|^minecraft:chain$|campfire|barrel|stripped_|_pane$|crafting_table|furnace|"
                  r"lectern|cauldron|flower_pot|bell$|^minecraft:(sea_lantern|glowstone|shroomlight|torch|wall_torch|"
                  r"soul_torch|ladder|scaffolding|hay_block|bamboo_mosaic|lightning_rod|copper_block))")

def is_crafted(name):
    return bool(_ART.search(name))

# ---------------- region / chunk reading ----------------
def read_region(path):
    """yield (cx, cz, nbt Compound) for every chunk in an .mca"""
    m = re.match(r"r\.(-?\d+)\.(-?\d+)\.mca$", os.path.basename(path))
    if not m:
        return
    rx, rz = int(m.group(1)), int(m.group(2))
    with open(path, "rb") as f:
        data = f.read()
    if len(data) < 8192:
        return
    for i in range(1024):
        v = struct.unpack_from(">I", data, i * 4)[0]
        off = (v >> 8) * 4096
        if not v or off + 5 > len(data):
            continue
        ln, comp = struct.unpack_from(">IB", data, off)
        raw = data[off + 5: off + 4 + ln]
        try:
            if comp == 2: raw = zlib.decompress(raw)
            elif comp == 1: raw = gzip.decompress(raw)
            elif comp != 3: continue
            nbt = nbtlib.File.parse(io.BytesIO(raw))
        except Exception:
            continue
        yield rx * 32 + (i & 31), rz * 32 + (i >> 5), nbt

def block_key(entry):
    name = str(entry["Name"])
    props = entry.get("Properties")
    if props:
        name += "[" + ",".join("%s=%s" % (k, props[k]) for k in sorted(props)) + "]"
    return name

def section_indices(sec):
    """-> (keys list, ndarray[4096] palette idx, or None when single-valued) ; index = y*256+z*16+x"""
    bs = sec.get("block_states")
    if bs is None: return None
    keys = [block_key(e) for e in bs["palette"]]
    if len(keys) == 1 or "data" not in bs:
        return keys, None
    longs = np.asarray(bs["data"], dtype=np.int64).view(np.uint64)
    bits = max(4, (len(keys) - 1).bit_length())
    per = 64 // bits                                  # entries never span longs (1.16+)
    i = np.arange(4096)
    vals = (longs[i // per] >> ((i % per) * bits).astype(np.uint64)) & np.uint64((1 << bits) - 1)
    return keys, vals.astype(np.int32)

class World:
    def __init__(self, d):
        self.dir = d
        rd = os.path.join(d, "region")
        self.regions = sorted(os.path.join(rd, f) for f in os.listdir(rd) if f.endswith(".mca"))
        self.data_version = 0
        lv = os.path.join(d, "level.dat")
        if os.path.exists(lv):
            try:
                self.data_version = int(nbtlib.load(lv)["Data"].get("DataVersion", 0))
            except Exception:
                pass

    def chunks(self, only=None):
        for r in self.regions:
            m = re.match(r"r\.(-?\d+)\.(-?\d+)\.mca$", os.path.basename(r))
            if not m: continue
            if only is not None:
                rx, rz = int(m.group(1)), int(m.group(2))
                if not any((cx >> 5) == rx and (cz >> 5) == rz for cx, cz in only): continue
            for cx, cz, nbt in read_region(r):
                if only is None or (cx, cz) in only:
                    yield cx, cz, nbt

# ---------------- auto-detect ----------------
def scan(world):
    """-> {(cx,cz): [count, minx,miny,minz,maxx,maxy,maxz]} of crafted blocks"""
    info, skipped = {}, 0
    for cx, cz, nbt in world.chunks():
        if int(nbt.get("DataVersion", 0)) < 2860 or "sections" not in nbt:
            skipped += 1; continue
        st = None
        for sec in nbt["sections"]:
            r = section_indices(sec)
            if r is None: continue
            keys, idx = r
            crafted = np.array([is_crafted(k.split("[")[0]) for k in keys])
            if not crafted.any(): continue
            sy = int(sec["Y"])
            if idx is None: idx = np.zeros(4096, np.int32)
            hit = np.nonzero(crafted[idx])[0]
            if not len(hit): continue
            ys, zs, xs = hit >> 8, (hit >> 4) & 15, hit & 15
            if st is None: st = [0, 1 << 30, 1 << 30, 1 << 30, -(1 << 30), -(1 << 30), -(1 << 30)]
            st[0] += len(hit)
            st[1] = min(st[1], cx * 16 + int(xs.min())); st[4] = max(st[4], cx * 16 + int(xs.max()))
            st[2] = min(st[2], sy * 16 + int(ys.min())); st[5] = max(st[5], sy * 16 + int(ys.max()))
            st[3] = min(st[3], cz * 16 + int(zs.min())); st[6] = max(st[6], cz * 16 + int(zs.max()))
        if st: info[(cx, cz)] = st
    if skipped: print("  warn: skipped %d chunks (DataVersion < 2860 or no 'sections')" % skipped)
    return info

def detect_bounds(world):
    info = scan(world)
    dense = {c for c, s in info.items() if s[0] >= MIN_CHUNK_DENSITY}
    if not dense:
        raise SystemExit("auto-detect: no dense built area found (use --bounds or toado.txt)")
    seen, best = set(), None
    for c in dense:                      # flood fill on chunks, 8-connected
        if c in seen: continue
        comp, q = [], collections.deque([c]); seen.add(c)
        while q:
            a = q.popleft(); comp.append(a)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    n = (a[0] + dx, a[1] + dz)
                    if n in dense and n not in seen: seen.add(n); q.append(n)
        tot = sum(info[k][0] for k in comp)
        if best is None or tot > best[0]: best = (tot, comp)
    tot, comp = best
    print("  cluster: %d chunks, %d crafted blocks" % (len(comp), tot))
    W = MAX_XZ // 16
    xs = [c[0] for c in comp]; zs = [c[1] for c in comp]
    wx, wz = max(xs) - min(xs) + 1, max(zs) - min(zs) + 1
    if wx > W or wz > W:
        print("  warn: cluster larger than %dx%d, choosing densest window" % (MAX_XZ, MAX_XZ))
        cs = set(comp); bw, bc = -1, None
        for x0 in range(min(xs), max(xs) - min(W, wx) + 2):
            for z0 in range(min(zs), max(zs) - min(W, wz) + 2):
                s = sum(info[(x, z)][0] for x in range(x0, x0 + W) for z in range(z0, z0 + W) if (x, z) in cs)
                if s > bw: bw, bc = s, (x0, z0)
        x0, z0 = bc
        comp = [c for c in comp if x0 <= c[0] < x0 + W and z0 <= c[1] < z0 + W]
    S = [info[c] for c in comp]
    b = [min(s[1] for s in S), min(s[2] for s in S), min(s[3] for s in S),
         max(s[4] for s in S), max(s[5] for s in S), max(s[6] for s in S)]
    return [b[0] - PAD, b[1] - PAD, b[2] - PAD, b[3] + PAD, b[4] + PAD, b[5] + PAD]

def normalize_bounds(b):
    x1, y1, z1, x2, y2, z2 = b
    b = [min(x1, x2), min(y1, y2), min(z1, z2), max(x1, x2), max(y1, y2), max(z1, z2)]
    for lo, hi, lim, nm in ((0, 3, MAX_XZ, "X"), (2, 5, MAX_XZ, "Z"), (1, 4, MAX_Y, "Y")):
        if b[hi] - b[lo] + 1 > lim:
            c = (b[lo] + b[hi]) // 2
            print("  warn: %s size %d > %d, trimmed around center" % (nm, b[hi] - b[lo] + 1, lim))
            b[lo] = c - lim // 2; b[hi] = b[lo] + lim - 1
    return b

# ---------------- extraction ----------------
def extract(world, b):
    x1, y1, z1, x2, y2, z2 = b
    W, H, L = x2 - x1 + 1, y2 - y1 + 1, z2 - z1 + 1
    grid = np.zeros((H, L, W), np.int32)             # [y][z][x] -> palette idx
    palette = {"minecraft:air": 0}
    bes = []
    need = {(cx, cz) for cx in range(x1 >> 4, (x2 >> 4) + 1) for cz in range(z1 >> 4, (z2 >> 4) + 1)}
    found = 0
    for cx, cz, nbt in world.chunks(need):
        if "sections" not in nbt: continue
        found += 1
        for sec in nbt["sections"]:
            r = section_indices(sec)
            if r is None: continue
            keys, idx = r
            sy = int(sec["Y"])
            if sy * 16 > y2 or sy * 16 + 15 < y1: continue
            gidx = np.array([palette.setdefault("minecraft:air" if k.split("[")[0] in SKIP_NAMES else k, len(palette))
                             for k in keys], np.int32)
            blk = gidx[idx].reshape(16, 16, 16) if idx is not None else np.full((16, 16, 16), gidx[0], np.int32)
            wx0, wy0, wz0 = cx * 16, sy * 16, cz * 16
            ax0, ax1 = max(x1, wx0), min(x2, wx0 + 15)
            ay0, ay1 = max(y1, wy0), min(y2, wy0 + 15)
            az0, az1 = max(z1, wz0), min(z2, wz0 + 15)
            if ax0 > ax1 or ay0 > ay1 or az0 > az1: continue
            grid[ay0 - y1:ay1 - y1 + 1, az0 - z1:az1 - z1 + 1, ax0 - x1:ax1 - x1 + 1] = \
                blk[ay0 - wy0:ay1 - wy0 + 1, az0 - wz0:az1 - wz0 + 1, ax0 - wx0:ax1 - wx0 + 1]
        for be in nbt.get("block_entities", []):
            x, y, z = int(be["x"]), int(be["y"]), int(be["z"])
            if x1 <= x <= x2 and y1 <= y <= y2 and z1 <= z <= z2:
                d = T.Compound({k: v for k, v in be.items() if k not in ("x", "y", "z", "id", "keepPacked")})
                d["Id"] = T.String(str(be.get("id", "")))
                d["Pos"] = T.IntArray([x - x1, y - y1, z - z1])
                bes.append(d)
    print("  chunks read: %d/%d, block entities: %d" % (found, len(need), len(bes)))
    return grid, palette, bes

def varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F; n >>= 7
        if n: out.append(b | 0x80)
        else:
            out.append(b); return out

def write_schem(path, grid, palette, bes, dv):
    H, L, W = grid.shape
    used = sorted(set(np.unique(grid).tolist()) | {0})
    inv = {v: k for k, v in palette.items()}
    remap = {old: new for new, old in enumerate(used)}
    lut = np.zeros(max(used) + 1, np.int32)
    for o, n in remap.items(): lut[o] = n
    flat = lut[grid].reshape(-1)                    # y*L*W + z*W + x == Sponge order
    data = bytearray()
    for v in flat.tolist(): data += varint(v)
    pal = T.Compound({inv[o]: T.Int(n) for o, n in remap.items()})
    root = T.Compound({
        "Version": T.Int(2), "DataVersion": T.Int(dv or 3955),
        "Width": T.Short(W), "Height": T.Short(H), "Length": T.Short(L),
        "Offset": T.IntArray([0, 0, 0]),
        "PaletteMax": T.Int(len(pal)), "Palette": pal,
        "BlockData": T.ByteArray(np.frombuffer(bytes(data), np.int8)),
        "BlockEntities": T.List[T.Compound](bes),
        "Metadata": T.Compound({"Name": T.String(os.path.basename(path)), "Author": T.String("world_to_schem")}),
    })
    os.makedirs(os.path.dirname(path), exist_ok=True)
    nbtlib.File(root, gzipped=True, root_name="Schematic").save(path)

# ---------------- reader (verify / round-trip) ----------------
def read_schem(path):
    f = nbtlib.load(path)
    r = f.get("Schematic", f)
    W, H, L = int(r["Width"]), int(r["Height"]), int(r["Length"])
    pal = {int(v): k for k, v in r["Palette"].items()}
    raw = bytes(np.asarray(r["BlockData"], np.int8).view(np.uint8)); out, p = [], 0
    while p < len(raw):
        v = s = 0
        while True:
            b = raw[p]; p += 1; v |= (b & 0x7F) << s; s += 7
            if not b & 0x80: break
        out.append(v)
    assert len(out) == W * H * L, "BlockData length %d != %d" % (len(out), W * H * L)
    return W, H, L, pal, np.array(out, np.int32).reshape(H, L, W), r["BlockEntities"]

# ---------------- marker suggestions ----------------
def suggest_markers(grid, pal):
    H, L, W = grid.shape
    names = [pal.get(i, "minecraft:air").split("[")[0] for i in range(max(pal) + 1)]
    air = np.array([n == "minecraft:air" for n in names])
    bad = np.array([("leaves" in n or n.endswith(("_log", "_wood", "_stairs", "_slab", "_fence", "_wall"))
                     or "water" in n or "lava" in n) for n in names])
    solid = ~air[grid]                                       # [H][L][W]
    has = solid.any(axis=0)
    top = np.where(has, H - 1 - np.argmax(solid[::-1], axis=0), -1)
    topid = np.take_along_axis(grid, np.clip(top, 0, H - 1)[None], axis=0)[0]
    ok = has & ~bad[topid]
    cx, cz = W / 2, L / 2
    best = None
    for y in set(top[ok].tolist()):
        m = ok & (top == y)
        if m.sum() < 9: continue
        dp = np.zeros((L + 1, W + 1), np.int32)
        for z in range(1, L + 1):
            for x in range(1, W + 1):
                if m[z - 1, x - 1]:
                    dp[z, x] = 1 + min(dp[z - 1, x], dp[z, x - 1], dp[z - 1, x - 1])
        zz, xx = np.nonzero(dp >= 3)
        for z, x in zip(zz.tolist(), xx.tolist()):
            s = int(dp[z, x]); mx, mz = x - 1 - (s - 1) / 2, z - 1 - (s - 1) / 2   # square centre
            score = min(s, 15) ** 2 - 0.5 * math.hypot(mx - cx, mz - cz)
            if best is None or score > best[0]: best = (score, int(round(mx)), int(y), int(round(mz)), s)
    if best is None:
        return {"markers": [], "note": "no open plaza found"}
    _, ax, ay, az, s = best
    ay += 1                                                  # marker = block placed above ground

    def pt(i, dx, dz):
        return {"id": i, "x": max(0, min(W - 1, ax + dx)), "y": min(H - 1, ay), "z": max(0, min(L - 1, az + dz))}
    k = max(2, min(3, s // 2))
    mk = [pt("altar", 0, 0), pt("board", k, 0), pt("npc", -k, k), pt("npc", k, k), pt("npc", -k, -k),
          pt("linhkhi", 0, 0), pt("sect_board", k, 0)]
    return {"markers": mk, "note": "auto-suggested; plaza %dx%d; coords local to the .schem" % (s, s)}

# ---------------- drivers ----------------
def convert(world_dir, category, name=None, bounds=None):
    name = re.sub(r"[^A-Za-z0-9_.-]+", "_", name or os.path.basename(os.path.normpath(world_dir)))
    print("[%s/%s] %s" % (category, name, world_dir))
    w = World(world_dir)
    if w.data_version and w.data_version < 2860:
        print("  warn: DataVersion %d < 2860, old chunk format unsupported" % w.data_version)
    if bounds is None:
        tf = os.path.join(world_dir, "toado.txt")
        if os.path.exists(tf):
            bounds = [int(float(t)) for t in open(tf, encoding="utf-8").read().replace(",", " ").split()[:6]]
            print("  bounds from toado.txt")
        else:
            bounds = detect_bounds(w)
            print("  auto bounds")
    bounds = normalize_bounds(bounds)
    print("  bounds:", bounds)
    grid, palette, bes = extract(w, bounds)
    out = os.path.join(OUT_ROOT, category, name + ".schem")
    write_schem(out, grid, palette, bes, w.data_version)
    W, H, L, pal, g2, _ = read_schem(out)
    inv = {v: k for k, v in palette.items()}
    flat_a, flat_b = grid.reshape(-1), g2.reshape(-1)
    assert all(inv[int(flat_a[i])] == pal[int(flat_b[i])] for i in range(0, len(flat_a), 97)), "round-trip mismatch"
    mk = suggest_markers(g2, pal)
    with open(out + ".markers.json", "w", encoding="utf-8") as f:
        json.dump(mk, f, indent=1)
    print("  wrote %s (%dx%dx%d, palette %d, markers %d)" % (out, W, H, L, len(pal), len(mk["markers"])))
    return out

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("world", nargs="?"); ap.add_argument("category", nargs="?")
    ap.add_argument("--name"); ap.add_argument("--bounds", nargs=6, type=int)
    ap.add_argument("--all"); ap.add_argument("--verify")
    a = ap.parse_args()
    if a.verify:
        W, H, L, pal, g, be = read_schem(a.verify)
        nonair = np.array([pal[i] != "minecraft:air" for i in range(max(pal) + 1)])
        print("OK %dx%dx%d palette=%d non-air=%d BE=%d" % (W, H, L, len(pal), int(nonair[g].sum()), len(be)))
    elif a.all:
        for cat in sorted(os.listdir(a.all)):
            cd = os.path.join(a.all, cat)
            if not os.path.isdir(cd): continue
            for wn in sorted(os.listdir(cd)):
                wd = os.path.join(cd, wn)
                if os.path.isdir(os.path.join(wd, "region")):
                    try: convert(wd, cat, wn)
                    except SystemExit as e: print("  skipped:", e)
    elif a.world and a.category:
        convert(a.world, a.category, a.name, a.bounds)
    else:
        ap.error("need <worldDir> <category> or --all or --verify")

if __name__ == "__main__":
    main()
