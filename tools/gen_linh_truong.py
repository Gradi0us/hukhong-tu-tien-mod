"""Sinh texture + model 3D cua Linh Truong (ban menh truong Phap Tu) cho mod hukhong.

- 6 nguyen to (quang, loi, hoa, thuy, tho, kim) x 3 bac hinh dang (T0: giai doan 0-1, T1: 2-3, T2: 4-6).
- Texture 32x32/nguyen to: 4 o 16x16 (go, vong kim loai, tinh the, loi sang).
- Model: truc truong + dau tinh the chu thap, dat theo duong cheo 45 do giong kiem handheld (dung display chuan "handheld").
- Ghi: assets/hukhong/textures/item/linh_truong/<el>.png, models/item/linh_truong/<el>_t<k>.json, models/item/linh_truong.json

Chay: python D:\\mc_mod\\tools\\gen_linh_truong.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir,
                                     "src", "main", "resources", "assets", "hukhong"))
TEX_DIR = os.path.join(ROOT, "textures", "item", "linh_truong")
MODEL_DIR = os.path.join(ROOT, "models", "item", "linh_truong")
ELEMENTS = ["quang", "loi", "hoa", "thuy", "tho", "kim"]  # thu tu = PhapElement.ordinal -> custom_model_data 1..6
STAGE_TIERS = [0, 2, 4]  # nguong hukhong:stage cho T0/T1/T2

PAL = {  # (toi, chinh, sang)
    "quang": ((0xC9, 0x8A, 0x14), (0xFF, 0xD8, 0x4A), (0xFF, 0xFB, 0xE0)),
    "loi": ((0x4A, 0x62, 0xC8), (0x8F, 0xB8, 0xFF), (0xF0, 0xF6, 0xFF)),
    "hoa": ((0xB8, 0x2A, 0x10), (0xFF, 0x7A, 0x2A), (0xFF, 0xE9, 0xA0)),
    "thuy": ((0x1C, 0x5C, 0xB0), (0x3F, 0xA9, 0xFF), (0xD8, 0xF4, 0xFF)),
    "tho": ((0x5E, 0x44, 0x20), (0xA8, 0x82, 0x3F), (0xE0, 0xC8, 0x9A)),
    "kim": ((0x6A, 0x78, 0x86), (0xC4, 0xCE, 0xD8), (0xFF, 0xFF, 0xFF)),
}


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def make_texture(el):
    dark, main, light = PAL[el]
    rnd = random.Random(el)
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
    px = im.load()
    for y in range(16):          # go (o tren-trai)
        for x in range(16):
            g = 0.5 + 0.25 * math.sin(x * 0.9 + rnd.random() * 0.6) + 0.1 * (y % 4 == 0)
            px[x, y] = lerp((58, 38, 22), (122, 84, 48), max(0, min(1, g))) + (255,)
    for y in range(16):          # vong kim loai (o tren-phai): kim loai ngả mau nguyen to
        for x in range(16, 32):
            t = 0.5 + 0.5 * math.sin((x + y) * 0.7)
            px[x, y] = lerp(lerp(dark, (40, 36, 44), 0.55), lerp(light, (200, 190, 150), 0.5), t * 0.55) + (255,)
    for y in range(16, 32):      # tinh the (o duoi-trai): cac mat cat sang/toi
        for x in range(16):
            facet = ((x // 4) + (y // 4)) % 3
            t = [0.15, 0.55, 0.85][facet] + rnd.random() * 0.08
            px[x, y] = lerp(dark, main, min(1, t)) + (255,) if t < 0.75 else lerp(main, light, (t - 0.75) * 3) + (255,)
    for y in range(16, 32):      # loi sang (o duoi-phai)
        for x in range(16, 32):
            d = math.hypot(x - 24, y - 24) / 11.0
            px[x, y] = lerp(light, main, min(1, d)) + (255,)
    os.makedirs(TEX_DIR, exist_ok=True)
    im.save(os.path.join(TEX_DIR, el + ".png"))


# vung UV (don vi model 0..16 = ca texture 32px)
UV_WOOD, UV_BAND, UV_CRYS, UV_CORE = (1, 1, 7, 7), (9, 1, 15, 7), (1, 9, 7, 15), (9, 9, 15, 15)
S2 = math.sqrt(0.5)


def part(d, off, w, h, depth, uv, el_name, angle_extra=0):
    """Mot khoi dat tai tam (d doc truc truong, off ngang), w ngang, h doc truc, depth sau; xoay -45 theo z quanh tam."""
    cx = 2.0 + (d * S2) + (off * S2)
    cy = 2.0 + (d * S2) - (off * S2)
    faces = {f: {"uv": list(uv), "texture": "#t"} for f in ("north", "south", "east", "west", "up", "down")}
    return {
        "from": [round(cx - w / 2, 3), round(cy - h / 2, 3), round(8 - depth / 2, 3)],
        "to": [round(cx + w / 2, 3), round(cy + h / 2, 3), round(8 + depth / 2, 3)],
        "rotation": {"angle": -45 + angle_extra, "axis": "z", "origin": [round(cx, 3), round(cy, 3), 8]},
        "faces": faces,
    }


def make_model(el, tier):
    els = [
        part(8.5, 0, 1.3, 17.5, 1.3, UV_WOOD, el),        # truc
        part(3.5, 0, 1.9, 2.2, 1.9, UV_BAND, el),         # day quan tay cam
        part(15.0, 0, 2.5, 1.3, 2.5, UV_BAND, el),        # vong co
        part(18.2, -1.5, 0.8, 3.4 + tier * 0.6, 0.8, UV_BAND, el),   # ngam trai
        part(18.2, 1.5, 0.8, 3.4 + tier * 0.6, 0.8, UV_BAND, el),    # ngam phai
        part(19.6 + tier * 0.3, 0, 2.6, 4.6 + tier * 0.8, 2.6, UV_CRYS, el),    # tinh the doc
        part(19.6 + tier * 0.3, 0, 4.4 + tier * 0.8, 2.6, 2.6, UV_CRYS, el),    # tinh the ngang (hinh chu thap)
        part(19.6 + tier * 0.3, 0, 1.6, 1.6, 1.6, UV_CORE, el),                 # loi sang
    ]
    if tier >= 1:   # manh tinh the troi quanh dau
        for off, dd in ((-2.6, 21.2), (2.6, 18.0)):
            els.append(part(dd, off, 1.0, 1.0, 1.0, UV_CORE, el))
    if tier >= 2:
        for off, dd in ((-3.4, 19.4), (3.4, 20.6), (0, 23.6)):
            els.append(part(dd, off, 1.1, 1.8, 1.1, UV_CRYS, el))
        els.append(part(11.5, 0, 2.0, 1.0, 2.0, UV_BAND, el))
    return {
        "textures": {"t": "hukhong:item/linh_truong/" + el, "particle": "hukhong:item/linh_truong/" + el},
        "elements": els,
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.78, 0.78, 0.78]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.78, 0.78, 0.78]},
        },
    }


def main():
    os.makedirs(MODEL_DIR, exist_ok=True)
    overrides = []
    for i, el in enumerate(ELEMENTS):
        make_texture(el)
        for k, thr in enumerate(STAGE_TIERS):
            with open(os.path.join(MODEL_DIR, "%s_t%d.json" % (el, k)), "w", encoding="utf-8") as f:
                json.dump(make_model(el, k), f, indent=1)
            overrides.append({"predicate": {"custom_model_data": i + 1, "hukhong:stage": thr},
                              "model": "hukhong:item/linh_truong/%s_t%d" % (el, k)})
    base = make_model("quang", 0)
    base["overrides"] = overrides   # goc = Quang T0; override theo custom_model_data (nguyen to) + stage
    with open(os.path.join(ROOT, "models", "item", "linh_truong.json"), "w", encoding="utf-8") as f:
        json.dump(base, f, indent=1)
    print("ok", len(overrides), "overrides")


if __name__ == "__main__":
    main()
