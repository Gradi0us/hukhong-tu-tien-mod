#!/usr/bin/env python3
"""Dot 7b (F4 sinh vat, F6 tai nguyen, mua): sinh toan bo du lieu + asset. Chay: python tools/gen_c7.py (tu D:\\mc_mod).
Ghi: model sinh vat (goi gen_c7_models), icon vat pham / khoi, blockstate/model, loot, worldgen datapack (configured/placed feature +
biome modifier), du lieu hukhong_herbs / hukhong_creatures, tag, recipe nen, docs/research/lang-c7.txt.
Ten id phai khop Java: resource.HerbDef.IDS, resource.ResourceRegistry.ORES, creature.CreatureKind (+ MATERIALS)."""
import json, math, random, sys
from pathlib import Path
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_c7_models

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
AS = RES / "assets/hukhong"
DT = RES / "data/hukhong"
MC = RES / "data/minecraft"


def wj(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")


LANG = []  # (key, vi, en)


def L(key, vi, en): LANG.append((key, vi, en))


# ============================================================ HERBS (12)
# id: (vi, en, existing_item, seasons, biomes, rarity(worldgen chance), colors(stem, petal, center), style)
HERBS = {
    "linh_thao": ("Linh Thảo", "Spirit Grass", True, ["xuan", "ha"], ["#minecraft:is_forest", "minecraft:plains", "minecraft:meadow"], 6),
    "thanh_diep": ("Thanh Diệp", "Verdant Leaf", True, ["xuan", "ha", "thu"], ["#minecraft:is_forest", "#minecraft:is_jungle"], 7),
    "hoa_lien": ("Hỏa Liên", "Fire Lotus", True, ["ha", "thu"], ["minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_jungle"], 9),
    "huyet_tham": ("Huyết Sâm", "Blood Ginseng", True, ["thu", "dong"], ["#minecraft:is_mountain", "#minecraft:is_badlands", "#minecraft:is_taiga"], 9),
    "bang_lien": ("Băng Liên", "Frost Lotus", False, ["dong"], ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:grove", "minecraft:snowy_slopes"], 7),
    "hoa_linh_chi": ("Hỏa Linh Chi", "Flame Reishi", False, ["ha"], ["minecraft:desert", "#minecraft:is_badlands", "#minecraft:is_savanna"], 8),
    "tu_linh_thao": ("Tử Linh Thảo", "Violet Spirit Herb", False, ["thu", "dong"], ["minecraft:dark_forest", "minecraft:swamp", "minecraft:old_growth_birch_forest"], 8),
    "ngung_hon_hoa": ("Ngưng Hồn Hoa", "Soul-Condensing Bloom", False, ["xuan", "thu"], ["minecraft:flower_forest", "minecraft:cherry_grove", "minecraft:meadow"], 10),
    "kim_tinh_thao": ("Kim Tinh Thảo", "Golden Star Herb", False, ["thu"], ["#minecraft:is_savanna", "minecraft:plains", "minecraft:sunflower_plains"], 8),
    "thuy_tam_thao": ("Thủy Tâm Thảo", "Water-Heart Herb", False, ["xuan", "ha"], ["minecraft:river", "minecraft:swamp", "minecraft:beach"], 7),
    "phong_linh_diep": ("Phong Linh Diệp", "Wind-Chime Leaf", False, ["ha", "thu"], ["#minecraft:is_mountain", "minecraft:windswept_hills"], 9),
    "dia_long_can": ("Địa Long Căn", "Earth-Dragon Root", False, ["dong", "xuan"], ["#minecraft:is_taiga", "minecraft:old_growth_pine_taiga", "minecraft:windswept_forest"], 9),
}
HERB_STYLE = {
    "linh_thao": ((70, 160, 80), (190, 255, 190), (255, 255, 160), "tuft"),
    "thanh_diep": ((40, 130, 60), (110, 220, 110), (200, 255, 150), "leaf"),
    "hoa_lien": ((50, 120, 70), (255, 110, 50), (255, 220, 90), "lotus"),
    "huyet_tham": ((90, 130, 60), (200, 40, 50), (255, 120, 120), "root"),
    "bang_lien": ((100, 170, 190), (200, 240, 255), (255, 255, 255), "lotus"),
    "hoa_linh_chi": ((120, 70, 40), (230, 90, 30), (255, 200, 80), "shroom"),
    "tu_linh_thao": ((70, 60, 120), (170, 90, 230), (240, 200, 255), "tuft"),
    "ngung_hon_hoa": ((60, 140, 130), (150, 200, 255), (255, 255, 255), "flower"),
    "kim_tinh_thao": ((110, 150, 60), (255, 215, 60), (255, 250, 190), "star"),
    "thuy_tam_thao": ((40, 120, 150), (90, 200, 255), (200, 250, 255), "leaf"),
    "phong_linh_diep": ((90, 150, 120), (200, 235, 220), (255, 255, 255), "leaf"),
    "dia_long_can": ((100, 80, 50), (150, 110, 70), (220, 180, 110), "root"),
}

# ============================================================ ORES (5)
# ore id: (vi, en, drop, drop vi, drop en, blob color, count, size, ymin, ymax, shape, biome tag(s), dropmin, dropmax)
ORES = {
    "linh_thach_ha_ore": ("Quặng Hạ Phẩm Linh Thạch", "Low-Grade Spirit Stone Ore", "linh_thach_ha", "Hạ Phẩm Linh Thạch", "Low-Grade Spirit Stone",
                          (120, 220, 160), 10, 7, -16, 128, "trapezoid", ["#minecraft:is_overworld"], 1, 3),
    "linh_thach_trung_ore": ("Quặng Trung Phẩm Linh Thạch", "Mid-Grade Spirit Stone Ore", "linh_thach_trung", "Trung Phẩm Linh Thạch", "Mid-Grade Spirit Stone",
                             (90, 190, 255), 5, 6, -48, 32, "trapezoid", ["#minecraft:is_overworld"], 1, 2),
    "linh_thach_thuong_ore": ("Quặng Thượng Phẩm Linh Thạch", "High-Grade Spirit Stone Ore", "linh_thach_thuong", "Thượng Phẩm Linh Thạch", "High-Grade Spirit Stone",
                              (230, 150, 255), 2, 4, -64, -16, "uniform", ["#minecraft:is_overworld"], 1, 1),
    "huyen_thiet_ore": ("Quặng Huyền Thiết", "Dark Iron Ore", "huyen_thiet", "Huyền Thiết", "Dark Iron",
                        (70, 70, 96), 6, 6, 32, 160, "trapezoid", ["#minecraft:is_mountain", "#minecraft:is_hill"], 1, 2),
    "tinh_than_sa_ore": ("Quặng Tinh Thần Sa", "Star-Sand Ore", "tinh_than_sa", "Tinh Thần Sa", "Star Sand",
                         (255, 235, 140), 3, 5, -64, 16, "uniform", ["#minecraft:is_overworld"], 1, 2),
}

# ============================================================ CREATURES
# id: (vi, en, role, hp_mul, dmg_mul, exp_weight, hon_tinh, drops[(item, chance, min, max)], spawn(biomes, weight, min, max, time) | None, tame | None)
FOREST = ["#minecraft:is_forest"]
C = {
    "linh_loc": ("Linh Lộc", "Spirit Deer", "passive", 0.6, 0.5, 0.15, 0.0,
                 [("linh_loc_giac", 0.7, 1, 2)], (FOREST + ["minecraft:meadow", "minecraft:cherry_grove"], 10, 2, 4, "any"), None),
    "hac_tien": ("Hạc Tiên", "Immortal Crane", "passive", 0.5, 0.4, 0.15, 0.0,
                 [("hac_tien_vu", 0.8, 1, 3)], (["minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_mountain"], 6, 1, 2, "day"), None),
    "ngoc_tho": ("Ngọc Thố", "Jade Rabbit", "passive", 0.5, 0.4, 0.1, 0.0,
                 [("ngoc_tho_mao", 0.6, 1, 2)], (["minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains", "#minecraft:is_savanna"], 10, 2, 3, "any"),
                 ("hukhong:linh_thao", 0.3, "spirit")),
    "hoa_huynh": ("Hỏa Huỳnh", "Fire Firefly", "ambient", 0.3, 0.1, 0.05, 0.0,
                  [("hoa_huynh_quang", 0.5, 1, 1)], (["minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:forest", "minecraft:flower_forest"], 8, 3, 6, "night"), None),
    "linh_ngu": ("Linh Ngư", "Spirit Fish", "aquatic", 0.4, 0.2, 0.1, 0.0,
                 [("linh_ngu_lan", 0.7, 1, 2)], (["#minecraft:is_river", "minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_ocean"], 12, 2, 5, "any"), None),
    "linh_ho": ("Linh Hồ", "Spirit Fox", "neutral", 0.8, 0.7, 0.4, 0.0,
                [("linh_ho_vi", 0.5, 1, 1)], (["minecraft:snowy_taiga", "minecraft:taiga", "minecraft:grove", "minecraft:snowy_plains", "minecraft:old_growth_pine_taiga"], 6, 1, 2, "any"),
                ("hukhong:hoa_lien", 0.3, "spirit")),
    "thiet_boi_hung": ("Thiết Bối Hùng", "Ironback Bear", "elite", 3.0, 1.4, 3.0, 0.15,
                       [("thiet_boi_hung_trao", 0.8, 1, 2), ("hukhong:huyen_thiet", 0.3, 1, 2), ("hukhong:huyet_tham", 0.25, 1, 1)],
                       (["#minecraft:is_mountain", "#minecraft:is_hill", "#minecraft:is_taiga", "minecraft:old_growth_birch_forest"], 3, 1, 1, "any"), None),
    "thiet_boi_hung_con": ("Thiết Bối Hùng Con", "Ironback Cub", "pet", 0.8, 0.6, 0.5, 0.0,
                           [], (["#minecraft:is_mountain", "#minecraft:is_taiga"], 2, 1, 1, "any"), ("hukhong:huyet_tham", 0.3, "battle")),
    "doc_van_xa": ("Độc Văn Xà", "Venom-Stripe Snake", "hostile", 0.8, 1.0, 0.7, 0.08,
                   [("doc_van_xa_nha", 0.5, 1, 2), ("hukhong:thanh_diep", 0.15, 1, 1)],
                   (["minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_jungle"], 12, 1, 2, "any"), None),
    "sa_trung": ("Sa Trùng", "Sand Wyrm", "hostile", 1.2, 1.3, 1.0, 0.1,
                 [("sa_trung_giap", 0.6, 1, 2), ("hukhong:tinh_than_sa", 0.1, 1, 1)],
                 (["minecraft:desert", "#minecraft:is_badlands"], 12, 1, 2, "any"), None),
    "bang_hon": ("Băng Hồn", "Frost Wraith", "hostile", 1.0, 1.0, 0.9, 0.1,
                 [("bang_hon_tinh", 0.6, 1, 1), ("hukhong:bang_lien", 0.1, 1, 1)],
                 (["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:frozen_peaks", "minecraft:snowy_slopes"], 12, 1, 2, "any"), None),
    "moc_yeu": ("Mộc Yêu", "Wood Fiend", "hostile", 1.6, 1.2, 1.2, 0.12,
                [("moc_yeu_loi", 0.6, 1, 1), ("hukhong:linh_thao", 0.3, 1, 2)],
                (["minecraft:dark_forest", "minecraft:old_growth_birch_forest", "minecraft:old_growth_spruce_taiga", "minecraft:jungle"], 8, 1, 1, "any"), None),
    "doc_thiem": ("Độc Thiềm", "Venom Toad", "hostile", 0.9, 0.9, 0.7, 0.08,
                  [("doc_thiem_noc", 0.6, 1, 2)],
                  (["minecraft:swamp", "minecraft:mangrove_swamp"], 12, 1, 3, "any"), None),
    "quan_hon": ("Quân Hồn", "Phantom Soldier", "hostile", 1.1, 1.1, 0.8, 0.1,
                 [("quan_hon_manh", 0.5, 1, 2), ("hukhong:hon_tinh", 0.05, 1, 1)],
                 (["minecraft:plains", "minecraft:savanna", "#minecraft:is_badlands", "minecraft:meadow"], 10, 3, 5, "night"), None),
}
MATERIAL_NAMES = {
    "linh_loc_giac": ("Sừng Linh Lộc", "Spirit Deer Antler"), "hac_tien_vu": ("Lông Hạc Tiên", "Immortal Crane Feather"),
    "ngoc_tho_mao": ("Lông Ngọc Thố", "Jade Rabbit Fur"), "hoa_huynh_quang": ("Quang Hỏa Huỳnh", "Firefly Glow"),
    "linh_ngu_lan": ("Vảy Linh Ngư", "Spirit Fish Scale"), "linh_ho_vi": ("Đuôi Linh Hồ", "Spirit Fox Tail"),
    "thiet_boi_hung_trao": ("Vuốt Thiết Bối Hùng", "Ironback Claw"), "doc_van_xa_nha": ("Nanh Độc Văn Xà", "Venom Fang"),
    "sa_trung_giap": ("Giáp Sa Trùng", "Sand Wyrm Carapace"), "bang_hon_tinh": ("Băng Hồn Tinh", "Frost Wraith Essence"),
    "moc_yeu_loi": ("Lõi Mộc Yêu", "Wood Fiend Core"), "doc_thiem_noc": ("Nọc Độc Thiềm", "Toad Venom"),
    "quan_hon_manh": ("Mảnh Quân Hồn", "Soldier Soul Shard"),
}
MAT_STYLE = {
    "linh_loc_giac": ((120, 210, 180), "horn"), "hac_tien_vu": ((245, 245, 250), "feather"), "ngoc_tho_mao": ((206, 238, 220), "tuftball"),
    "hoa_huynh_quang": ((200, 255, 120), "orb"), "linh_ngu_lan": ((90, 190, 230), "scale"), "linh_ho_vi": ((220, 225, 245), "tail"),
    "thiet_boi_hung_trao": ((150, 154, 166), "claw"), "doc_van_xa_nha": ((235, 235, 215), "fang"), "sa_trung_giap": ((214, 188, 130), "plate"),
    "bang_hon_tinh": ((150, 220, 255), "gem"), "moc_yeu_loi": ((110, 170, 70), "orb"), "doc_thiem_noc": ((150, 230, 90), "drop"),
    "quan_hon_manh": ((130, 190, 240), "gem"),
    "linh_thach_ha": ((120, 220, 160), "gem"), "linh_thach_trung": ((90, 190, 255), "gem"), "linh_thach_thuong": ((230, 150, 255), "gem"),
    "huyen_thiet": ((80, 80, 110), "ingot"), "tinh_than_sa": ((255, 235, 140), "dust"),
}
PILLS = {"tu_linh_dan": ("Tụ Linh Đan", "Spirit-Gathering Pill", (120, 220, 255)),
         "huyen_ton_dan": ("Huyền Tôn Đan", "Mystic Venerable Pill", (170, 120, 255)),
         "thanh_co_dan": ("Thánh Cơ Đan", "Sacred Foundation Pill", (255, 200, 90)),
         "than_dan": ("Thần Đan", "Divine Pill", (255, 90, 120))}


# ============================================================ drawing
def blank(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def sh(c, f): return tuple(max(0, min(255, int(v * f))) for v in c)


def outline(im):
    px = im.load(); out = im.copy(); op = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3]: continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                X, Y = x + dx, y + dy
                if 0 <= X < 16 and 0 <= Y < 16 and px[X, Y][3]:
                    c = px[X, Y]; op[x, y] = (sh(c[:3], .35) + (255,)); break
    return out


def herb_icon(stem, petal, center, style):
    im = blank(); d = ImageDraw.Draw(im)
    st = stem + (255,); pt = petal + (255,); ct = center + (255,)
    d.line([(8, 15), (8, 8)], fill=st)
    if style == "tuft":
        for dx in (-4, -2, 0, 2, 4):
            d.line([(8, 14), (8 + dx, 5 + abs(dx))], fill=st if dx % 4 else pt)
        d.point((8, 4), fill=ct)
    elif style == "leaf":
        for (x, y) in ((4, 6), (12, 7), (5, 11), (11, 12)):
            d.ellipse([x - 2, y - 1, x + 2, y + 1], fill=pt)
            d.line([(8, 13), (x, y)], fill=st)
        d.ellipse([6, 2, 10, 6], fill=pt); d.point((8, 4), fill=ct)
    elif style == "lotus":
        for dx in (-4, -2, 2, 4):
            d.polygon([(8, 9), (8 + dx, 4 + abs(dx) // 2), (8 + dx // 2 + (1 if dx > 0 else -1), 9)], fill=pt)
        d.polygon([(8, 9), (6, 4), (10, 4)], fill=sh(petal, 1.15) + (255,))
        d.point((8, 7), fill=ct); d.line([(5, 13), (11, 13)], fill=st)
    elif style == "root":
        d.ellipse([5, 7, 11, 13], fill=pt); d.line([(8, 13), (6, 15)], fill=pt); d.line([(8, 13), (10, 15)], fill=pt)
        d.line([(8, 7), (6, 3)], fill=st); d.line([(8, 7), (10, 3)], fill=st); d.line([(8, 7), (8, 2)], fill=st); d.point((8, 9), fill=ct)
    elif style == "shroom":
        d.line([(8, 15), (8, 10)], fill=ct)
        d.pieslice([2, 3, 14, 13], 180, 360, fill=pt); d.line([(3, 8), (13, 8)], fill=sh(petal, .7) + (255,))
        d.point((6, 6), fill=ct); d.point((10, 5), fill=ct); d.point((8, 4), fill=ct)
    elif style == "flower":
        for (x, y) in ((8, 3), (5, 5), (11, 5), (6, 8), (10, 8)):
            d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=pt)
        d.point((8, 5), fill=ct); d.point((8, 6), fill=ct)
    elif style == "star":
        d.polygon([(8, 1), (9, 5), (13, 5), (10, 8), (11, 12), (8, 9), (5, 12), (6, 8), (3, 5), (7, 5)], fill=pt)
        d.point((8, 6), fill=ct)
    return outline(im)


def item_icon(color, style):
    im = blank(); d = ImageDraw.Draw(im); c = color + (255,); hi = sh(color, 1.3) + (255,); lo = sh(color, .65) + (255,)
    if style == "gem":
        d.polygon([(8, 2), (13, 6), (11, 13), (5, 13), (3, 6)], fill=c); d.polygon([(8, 2), (13, 6), (8, 8), (3, 6)], fill=hi)
        d.polygon([(8, 8), (13, 6), (11, 13), (8, 13)], fill=lo)
    elif style == "horn":
        d.polygon([(4, 14), (6, 6), (8, 2), (10, 7), (8, 14)], fill=c); d.line([(6, 7), (4, 4)], fill=hi); d.line([(9, 6), (12, 3)], fill=hi); d.line([(7, 5), (7, 11)], fill=lo)
    elif style == "feather":
        d.polygon([(3, 14), (6, 7), (11, 2), (13, 3), (12, 9), (6, 13)], fill=c); d.line([(3, 14), (12, 3)], fill=lo)
        d.line([(7, 7), (11, 8)], fill=hi)
    elif style == "tuftball":
        d.ellipse([3, 4, 12, 13], fill=c); d.ellipse([4, 5, 8, 8], fill=hi); d.ellipse([9, 8, 12, 11], fill=lo)
    elif style == "orb":
        d.ellipse([3, 3, 12, 12], fill=c); d.ellipse([5, 4, 7, 6], fill=(255, 255, 255, 255)); d.arc([3, 3, 12, 12], 20, 160, fill=lo)
    elif style == "scale":
        for (x, y) in ((5, 5), (10, 5), (7, 9), (12, 9), (4, 11)):
            d.pieslice([x - 3, y - 3, x + 3, y + 3], 0, 180, fill=c); d.arc([x - 3, y - 3, x + 3, y + 3], 0, 180, fill=hi)
    elif style == "tail":
        d.polygon([(3, 13), (5, 6), (9, 2), (13, 4), (11, 9), (6, 14)], fill=c); d.polygon([(9, 2), (13, 4), (12, 6), (10, 4)], fill=(120, 220, 255, 255))
    elif style == "claw":
        d.polygon([(5, 14), (6, 7), (10, 2), (11, 3), (10, 9), (8, 14)], fill=c); d.line([(6, 8), (10, 3)], fill=hi); d.line([(8, 13), (10, 8)], fill=lo)
    elif style == "fang":
        d.polygon([(6, 3), (11, 3), (9, 14)], fill=c); d.line([(7, 4), (9, 11)], fill=hi); d.line([(10, 4), (9, 11)], fill=lo)
    elif style == "plate":
        d.polygon([(2, 5), (8, 2), (14, 5), (13, 12), (8, 14), (3, 12)], fill=c); d.line([(8, 2), (8, 14)], fill=lo); d.line([(3, 8), (13, 8)], fill=lo); d.line([(3, 5), (8, 3)], fill=hi)
    elif style == "drop":
        d.polygon([(8, 2), (12, 9), (8, 14), (4, 9)], fill=c); d.ellipse([6, 8, 8, 10], fill=(255, 255, 255, 255))
    elif style == "ingot":
        d.polygon([(2, 10), (5, 5), (14, 5), (11, 10)], fill=hi); d.polygon([(2, 10), (11, 10), (11, 13), (2, 13)], fill=c); d.polygon([(11, 10), (14, 5), (14, 8), (11, 13)], fill=lo)
    elif style == "dust":
        random.seed(5)
        for _ in range(26):
            x = int(8 + random.gauss(0, 2.6)); y = int(11 + random.gauss(0, 2.0))
            if 0 <= x < 16 and 0 <= y < 16: d.point((x, y), fill=c if random.random() < .6 else hi)
    return outline(im)


def pill_icon(color):
    im = blank(); d = ImageDraw.Draw(im); c = color + (255,)
    d.ellipse([3, 3, 12, 12], fill=c); d.ellipse([4, 4, 7, 7], fill=sh(color, 1.4) + (255,)); d.arc([3, 3, 12, 12], 200, 340, fill=sh(color, .6) + (255,))
    d.point((9, 9), fill=(255, 255, 255, 255))
    return outline(im)


def ore_tex(blob, seed):
    rng = random.Random(seed)
    im = Image.new("RGBA", (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            v = 118 + rng.randint(-14, 14)
            if (x * 7 + y * 3) % 11 == 0: v -= 16
            px[x, y] = (v, v, v + 4, 255)
    for _ in range(5):
        cx, cy = rng.randint(2, 13), rng.randint(2, 13)
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                if abs(dx) + abs(dy) <= 1 + rng.randint(0, 1):
                    X, Y = cx + dx, cy + dy
                    if 0 <= X < 16 and 0 <= Y < 16:
                        px[X, Y] = (sh(blob, 1.25) if dx + dy < 0 else blob) + (255,)
    return im


def stage_tex(kind):
    """Mam / non xam-xanh de tint theo mua."""
    im = blank(); d = ImageDraw.Draw(im); g = (200, 215, 200, 255); g2 = (160, 180, 160, 255)
    if kind == 0:
        d.line([(8, 15), (8, 12)], fill=g2)
        d.line([(8, 12), (5, 9)], fill=g); d.line([(8, 12), (11, 9)], fill=g); d.point((5, 8), fill=g); d.point((11, 8), fill=g)
    else:
        d.line([(8, 15), (8, 7)], fill=g2)
        for (x, y) in ((4, 8), (12, 8), (5, 5), (11, 5)):
            d.line([(8, 11 if y > 6 else 8), (x, y)], fill=g)
        d.ellipse([6, 3, 10, 7], fill=g)
    return outline(im)


# ============================================================ writers
def item_model(name, parent="minecraft:item/generated", tex=None):
    wj(AS / "models/item" / f"{name}.json", {"parent": parent, "textures": {"layer0": tex or f"hukhong:item/{name}"}})


def tex_item(name, im):
    p = AS / "textures/item" / f"{name}.png"; p.parent.mkdir(parents=True, exist_ok=True); im.save(p)


def holder_set(entries):
    """Danh sach 'tag hoac id' -> list cac HolderSet hop le cho biomes (tag rieng le, id gop thanh mang)."""
    out = []; ids = []
    for e in entries:
        if e.startswith("#"): out.append(e)
        else: ids.append(e)
    if ids: out.append(ids if len(ids) > 1 else ids[0])
    return out


def biome_mods(prefix, entries, body):
    if body.get("type") == "neoforge:add_features":
        # Mot feature chi duoc them MOT lan vao moi biome, neu khong se "Feature order cycle" khi tao the gioi.
        # Gop moi biome/tag vao mot biome tag rieng (tag tu loai trung) + mot modifier duy nhat.
        name = body["features"].split(":")[1]
        vals = []
        for e in entries:
            if e not in vals: vals.append(e)
        wj(DT / "tags/worldgen/biome" / f"gen_{name}.json",
           {"replace": False, "values": [{"id": v, "required": False} for v in vals]})
        d = dict(body); d["biomes"] = f"#hukhong:gen_{name}"
        wj(DT / "neoforge/biome_modifier" / f"{name}.json", d)
        return
    for i, hs in enumerate(holder_set(entries)):
        d = dict(body); d["biomes"] = hs
        wj(DT / "neoforge/biome_modifier" / f"{prefix}_{i}.json", d)


def gen_herbs():
    sprout, bud = stage_tex(0), stage_tex(1)
    (AS / "textures/block").mkdir(parents=True, exist_ok=True)
    sprout.save(AS / "textures/block/herb_sprout.png"); bud.save(AS / "textures/block/herb_bud.png")
    wj(AS / "models/block/herb_stage0.json", {"parent": "minecraft:block/tinted_cross", "textures": {"cross": "hukhong:block/herb_sprout"}, "render_type": "minecraft:cutout"})
    wj(AS / "models/block/herb_stage1.json", {"parent": "minecraft:block/tinted_cross", "textures": {"cross": "hukhong:block/herb_bud"}, "render_type": "minecraft:cutout"})
    for hid, (vi, en, existing, seasons, biomes, rarity) in HERBS.items():
        stem, petal, center, style = HERB_STYLE[hid]
        if not existing:
            tex_item(hid, herb_icon(stem, petal, center, style))
            item_model(hid)
            L(f"item.hukhong.{hid}", vi, en)
        L(f"block.hukhong.herb_{hid}", vi, en)
        wj(AS / "models/block" / f"herb_{hid}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"hukhong:item/{hid}"}, "render_type": "minecraft:cutout"})
        variants = {}
        for cult in ("false", "true"):
            variants[f"age=0,cultivated={cult}"] = {"model": "hukhong:block/herb_stage0"}
            variants[f"age=1,cultivated={cult}"] = {"model": "hukhong:block/herb_stage1"}
            variants[f"age=2,cultivated={cult}"] = {"model": f"hukhong:block/herb_{hid}"}
        wj(AS / "blockstates" / f"herb_{hid}.json", {"variants": variants})
        wj(DT / "hukhong_herbs" / f"{hid}.json", {"item": f"hukhong:{hid}", "seasons": seasons, "biomes": biomes, "rarity": rarity})
        # worldgen: dam thao nho rai luc tao chunk (tuoi 1)
        ground = ["minecraft:grass_block", "minecraft:dirt", "minecraft:podzol", "minecraft:coarse_dirt", "minecraft:sand", "minecraft:red_sand",
                  "minecraft:terracotta", "minecraft:snow_block", "minecraft:stone", "minecraft:mud", "minecraft:moss_block"]
        wj(DT / "worldgen/configured_feature" / f"herb_{hid}.json", {
            "type": "minecraft:random_patch",
            "config": {"tries": 5, "xz_spread": 4, "y_spread": 2, "feature": {
                "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": {
                    "Name": f"hukhong:herb_{hid}", "Properties": {"age": "1", "cultivated": "false"}}}}},
                "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                    {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": ground}]}}]}}})
        wj(DT / "worldgen/placed_feature" / f"herb_{hid}.json", {"feature": f"hukhong:herb_{hid}", "placement": [
            {"type": "minecraft:rarity_filter", "chance": rarity * 3}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
        biome_mods(f"herb_{hid}", biomes, {"type": "neoforge:add_features", "features": f"hukhong:herb_{hid}", "step": "vegetal_decoration"})
        (AS / "textures/block").mkdir(parents=True, exist_ok=True)


def gen_ores():
    for i, (oid, (vi, en, drop, dvi, den, blob, count, size, ymin, ymax, shape, biomes, dmin, dmax)) in enumerate(ORES.items()):
        ore_tex(blob, 100 + i).save(AS / "textures/block" / f"{oid}.png")
        wj(AS / "models/block" / f"{oid}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"hukhong:block/{oid}"}})
        wj(AS / "blockstates" / f"{oid}.json", {"variants": {"": {"model": f"hukhong:block/{oid}"}}})
        item_model(oid, parent=f"hukhong:block/{oid}")
        L(f"block.hukhong.{oid}", vi, en)
        L(f"item.hukhong.{drop}", dvi, den)
        col, style = MAT_STYLE[drop]
        tex_item(drop, item_icon(col, style)); item_model(drop)
        wj(DT / "loot_table/blocks" / f"{oid}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"hukhong:{oid}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": f"hukhong:{drop}", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": dmin, "max": dmax}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})
        target = lambda tag: {"target": {"predicate_type": "minecraft:tag_match", "tag": tag}, "state": {"Name": f"hukhong:{oid}"}}
        wj(DT / "worldgen/configured_feature" / f"ore_{oid}.json", {"type": "minecraft:ore", "config": {
            "discard_chance_on_air_exposure": 0.3, "size": size,
            "targets": [target("minecraft:stone_ore_replaceables"), target("minecraft:deepslate_ore_replaceables")]}})
        if shape == "trapezoid":
            height = {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": ymin}, "max_inclusive": {"absolute": ymax}}
        else:
            height = {"type": "minecraft:uniform", "min_inclusive": {"absolute": ymin}, "max_inclusive": {"absolute": ymax}}
        wj(DT / "worldgen/placed_feature" / f"ore_{oid}.json", {"feature": f"hukhong:ore_{oid}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": height}, {"type": "minecraft:biome"}]})
        biome_mods(f"ore_{oid}", biomes, {"type": "neoforge:add_features", "features": f"hukhong:ore_{oid}", "step": "underground_ores"})
    wj(MC / "tags/block/needs_stone_tool.json", {"values": ["hukhong:linh_thach_ha_ore", "hukhong:linh_thach_trung_ore"]})
    wj(MC / "tags/block/needs_iron_tool.json", {"values": ["hukhong:linh_thach_thuong_ore", "hukhong:huyen_thiet_ore", "hukhong:tinh_than_sa_ore"]})
    # nen linh thach 9 -> 1 (thang bac)
    for lo, hi in (("linh_thach_ha", "linh_thach_trung"), ("linh_thach_trung", "linh_thach_thuong")):
        wj(DT / "recipe" / f"{hi}_from_{lo}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                     "ingredients": [{"item": f"hukhong:{lo}"}] * 9, "result": {"id": f"hukhong:{hi}", "count": 1}})
        wj(DT / "recipe" / f"{lo}_from_{hi}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                     "ingredients": [{"item": f"hukhong:{hi}"}], "result": {"id": f"hukhong:{lo}", "count": 9}})


def gen_materials_and_pills():
    for mid, (vi, en) in MATERIAL_NAMES.items():
        col, style = MAT_STYLE[mid]
        tex_item(mid, item_icon(col, style)); item_model(mid); L(f"item.hukhong.{mid}", vi, en)
    for pid, (vi, en, col) in PILLS.items():
        tex_item(pid, pill_icon(col)); item_model(pid); L(f"item.hukhong.{pid}", vi, en)


def gen_creatures():
    gen_c7_models.build_all()
    for cid, (vi, en, role, hm, dm, ew, ht, drops, spawn, tame) in C.items():
        d = {"role": role, "hp_mul": hm, "dmg_mul": dm, "exp_weight": ew, "hon_tinh": ht,
             "drops": [{"item": it if ":" in it else f"hukhong:{it}", "chance": ch, "min": mn, "max": mx} for it, ch, mn, mx in drops]}
        if spawn:
            b, w, mn, mx, tm = spawn
            d["spawn"] = {"biomes": b, "weight": w, "min": mn, "max": mx, "time": tm}
        if tame:
            d["tame"] = {"item": tame[0], "hp_frac": tame[1], "slot": tame[2]}
        wj(DT / "hukhong_creatures" / f"{cid}.json", d)
        L(f"entity.hukhong.{cid}", vi, en)
        L(f"item.hukhong.{cid}_spawn_egg", f"Trứng {vi}", f"{en} Spawn Egg")
        wj(AS / "models/item" / f"{cid}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
        if spawn:
            b, w, mn, mx, tm = spawn
            biome_mods(f"creature_{cid}", b, {"type": "neoforge:add_spawns", "spawners": {"type": f"hukhong:{cid}", "weight": w, "minCount": mn, "maxCount": mx}})


def gen_lang():
    for s, vi, en in (("xuan", "Xuân", "Spring"), ("ha", "Hạ", "Summer"), ("thu", "Thu", "Autumn"), ("dong", "Đông", "Winter")):
        L(f"hukhong.season.{s}", f"Mùa {vi}", en)
    L("hukhong.pill.tu_linh", "Tụ Linh Đan: +%s EXP.", "Spirit-Gathering Pill: +%s EXP.")
    L("hukhong.pet.died", "%s đã ngã xuống.", "%s has fallen.")
    L("hukhong.pet.slot.battle", "Thú chiến đấu: %s", "Battle pet: %s")
    L("hukhong.pet.slot.spirit", "Linh sủng: %s", "Spirit pet: %s")
    L("hukhong.pet.empty", "(trống)", "(empty)")
    L("hukhong.pet.unloaded", "(chưa tải)", "(not loaded)")
    L("hukhong.pet.released", "Đã thả linh thú ở ô %s.", "Released the pet in slot %s.")
    L("hukhong.pet.none", "Ô %s đang trống.", "Slot %s is empty.")
    L("hukhong.pet.sit", "%s ngồi yên.", "%s stays.")
    L("hukhong.pet.follow", "%s đi theo bạn.", "%s follows you.")
    L("hukhong.pet.tame.ok", "%s đã quy phục bạn!", "%s is tamed!")
    L("hukhong.pet.tame.wrong_item", "%s không muốn thứ này.", "%s does not want that.")
    L("hukhong.pet.tame.too_healthy", "%s còn quá khỏe, cần đánh yếu dưới 30%% máu.", "%s is too healthy: weaken it below 30%% HP.")
    L("hukhong.pet.tame.slot_full", "Bạn đã có linh thú ở ô này (mỗi người 1 thú chiến đấu + 1 linh sủng). Dùng /linhsu release.",
      "Your slot is taken (1 battle pet + 1 spirit pet each). Use /linhsu release.")
    L("hukhong.pet.tame.not_tameable", "%s không thể thuần phục.", "%s cannot be tamed.")
    L("hukhong.pet.tame.already_tamed", "%s đã có chủ.", "%s already has an owner.")
    path = ROOT / "docs/research/lang-c7.txt"
    path.write_text("".join(f"{k} | vi: {vi} | en: {en}\n" for k, vi, en in LANG), encoding="utf-8")
    print("lang entries:", len(LANG))


if __name__ == "__main__":
    gen_herbs(); gen_ores(); gen_materials_and_pills(); gen_creatures(); gen_lang()
