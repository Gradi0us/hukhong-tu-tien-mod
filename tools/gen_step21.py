#!/usr/bin/env python3
"""Buoc 21: texture/model/blockstate cho Cong Bi Canh (5 cap) va khoi thoat.
Chay: python tools/gen_step21.py (tu thu muc goc repo)"""
import json, math, random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "src/main/resources/assets/hukhong"
TIERS = [(235, 235, 245), (235, 200, 90), (170, 90, 230), (60, 50, 90), (230, 60, 70)]


def swirl(base, name, seed):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            v = 0.5 + 0.5 * math.sin(r * 1.4 - ang * 2)
            k = 0.25 + 0.6 * v if r <= 6.2 else 0.15 + 0.2 * rnd.random()
            if 5.0 < r <= 6.2:
                k = 0.95
            px[x, y] = tuple(min(255, int(c * k + 12 * rnd.random())) for c in base) + (255,)
    im.save(A / "textures/block" / (name + ".png"))


def put(rel, obj):
    (A / rel).write_text(json.dumps(obj), encoding="utf-8")


for i, c in enumerate(TIERS):
    swirl(c, "bicanh_portal_%d" % i, 2100 + i)
    put("models/block/bicanh_portal_%d.json" % i,
        {"parent": "minecraft:block/cube_all", "textures": {"all": "hukhong:block/bicanh_portal_%d" % i}})
swirl((90, 200, 170), "bicanh_exit", 2199)

put("blockstates/bicanh_portal.json",
    {"variants": {"tier=%d" % i: {"model": "hukhong:block/bicanh_portal_%d" % i} for i in range(5)}})
put("blockstates/bicanh_exit.json", {"variants": {"": {"model": "hukhong:block/bicanh_exit"}}})
put("models/block/bicanh_exit.json",
    {"parent": "minecraft:block/cube_all", "textures": {"all": "hukhong:block/bicanh_exit"}})
put("models/item/bicanh_portal.json", {"parent": "hukhong:block/bicanh_portal_0"})
put("models/item/bicanh_exit.json", {"parent": "hukhong:block/bicanh_exit"})
print("ok")
