#!/usr/bin/env python3
"""2026-11-06: them 12 linh thao + 8 linh duoc. Chay tu D:\\mc_mod: python tools/gen_c10_herbs.py
Dung lai ham cua gen_c7 (icon, blockstate, worldgen, biome modifier). Id phai khop resource.HerbDef.IDS va resource.LinhDuoc."""
import json, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_c7 as g

HERBS = {
    "thien_son_tuyet_lien": ("Thiên Sơn Tuyết Liên", "Heaven-Peak Snow Lotus", False, ["dong"], ["minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:grove"], 11),
    "long_huyet_thao": ("Long Huyết Thảo", "Dragon-Blood Grass", False, ["ha", "thu"], ["#minecraft:is_badlands", "minecraft:savanna_plateau"], 10),
    "u_minh_hoa": ("U Minh Hoa", "Netherdark Bloom", False, ["thu", "dong"], ["minecraft:dark_forest", "minecraft:swamp", "minecraft:mangrove_swamp"], 10),
    "xich_duong_chi": ("Xích Dương Chi", "Crimson-Sun Reishi", False, ["ha"], ["minecraft:desert", "#minecraft:is_savanna"], 9),
    "am_duong_thao": ("Âm Dương Thảo", "Yin-Yang Grass", False, ["xuan", "thu"], ["minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains"], 9),
    "ngoc_tuy_chi": ("Ngọc Tủy Chi", "Jade-Marrow Mushroom", False, ["xuan", "ha"], ["#minecraft:is_jungle", "minecraft:lush_caves"], 10),
    "van_nien_linh_chi": ("Vạn Niên Linh Chi", "Myriad-Year Reishi", False, ["thu"], ["minecraft:old_growth_spruce_taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_birch_forest"], 12),
    "bich_hai_thao": ("Bích Hải Thảo", "Azure-Sea Grass", False, ["xuan", "ha", "thu"], ["minecraft:beach", "minecraft:stony_shore", "minecraft:river"], 8),
    "tinh_than_hoa": ("Tinh Thần Hoa", "Starspirit Flower", False, ["xuan", "thu"], ["minecraft:meadow", "minecraft:cherry_grove", "minecraft:flower_forest"], 11),
    "hoang_tinh": ("Hoàng Tinh", "Golden Essence Root", False, ["xuan", "ha", "thu"], ["#minecraft:is_forest", "minecraft:plains"], 7),
    "ha_thu_o": ("Hà Thủ Ô", "Fleeceflower Root", False, ["thu", "dong"], ["#minecraft:is_taiga", "#minecraft:is_forest"], 8),
    "loi_kich_moc": ("Lôi Kích Mộc", "Thunderstruck Sprig", False, ["ha"], ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "#minecraft:is_mountain"], 10),
}
STYLE = {
    "thien_son_tuyet_lien": ((120, 170, 190), (235, 248, 255), (180, 230, 255), "lotus"),
    "long_huyet_thao": ((110, 60, 40), (220, 30, 40), (255, 170, 60), "tuft"),
    "u_minh_hoa": ((50, 40, 70), (90, 40, 140), (200, 120, 255), "flower"),
    "xich_duong_chi": ((110, 50, 30), (240, 70, 20), (255, 230, 120), "shroom"),
    "am_duong_thao": ((70, 90, 70), (240, 240, 240), (30, 30, 30), "leaf"),
    "ngoc_tuy_chi": ((60, 110, 80), (110, 230, 170), (230, 255, 240), "shroom"),
    "van_nien_linh_chi": ((90, 60, 40), (150, 70, 120), (255, 215, 90), "shroom"),
    "bich_hai_thao": ((40, 110, 120), (60, 210, 200), (200, 255, 250), "leaf"),
    "tinh_than_hoa": ((60, 80, 140), (110, 150, 255), (255, 255, 200), "star"),
    "hoang_tinh": ((110, 120, 50), (230, 190, 60), (255, 240, 170), "root"),
    "ha_thu_o": ((80, 90, 50), (130, 80, 50), (210, 160, 110), "root"),
    "loi_kich_moc": ((90, 80, 60), (250, 240, 90), (180, 220, 255), "leaf"),
}
# id: (vi, en, mau, mo ta vi, mo ta en)
PILLS = {
    "hoi_linh_dan": ("Hồi Linh Đan", "Spirit-Restoring Pill", (90, 170, 255), "Hồi 35% Linh Khí.", "Restores 35% Spirit Energy."),
    "dai_hoi_linh_dan": ("Đại Hồi Linh Đan", "Greater Spirit-Restoring Pill", (40, 110, 255), "Hồi 80% Linh Khí.", "Restores 80% Spirit Energy."),
    "thanh_tam_dan": ("Thanh Tâm Đan", "Heart-Clearing Pill", (220, 240, 255), "Ác Tính -30, Đan Độc -20.", "Malice -30, Pill Toxin -20."),
    "tay_tuy_dan": ("Tẩy Tủy Đan", "Marrow-Cleansing Pill", (230, 230, 200), "Xóa sạch Đan Độc và mọi hiệu ứng xấu.", "Clears all Pill Toxin and harmful effects."),
    "kim_cuong_dan": ("Kim Cương Đan", "Diamond-Body Pill", (180, 200, 210), "Kháng Cự II trong 2 phút.", "Resistance II for 2 minutes."),
    "bao_huyet_dan": ("Bạo Huyết Đan", "Raging-Blood Pill", (200, 30, 30), "Sức Mạnh II trong 90 giây.", "Strength II for 90 seconds."),
    "phong_hanh_dan": ("Phong Hành Đan", "Wind-Walk Pill", (170, 240, 200), "Tốc Độ II và Nhảy Cao II trong 2 phút.", "Speed II and Jump Boost II for 2 minutes."),
    "van_nien_linh_dich": ("Vạn Niên Linh Dịch", "Myriad-Year Elixir", (255, 200, 80), "Hồi 60% máu, Hồi Phục II 20 giây.", "Heals 60% HP, Regeneration II for 20 seconds."),
}

if __name__ == "__main__":
    g.HERBS = HERBS
    g.HERB_STYLE = STYLE
    g.gen_herbs()
    for pid, (vi, en, col, dvi, den) in PILLS.items():
        g.tex_item(pid, g.pill_icon(col))
        g.item_model(pid)
        g.L(f"item.hukhong.{pid}", vi, en)
        g.L(f"hukhong.linh_duoc.{pid}", dvi, den)
    g.L("hukhong.linh_duoc.ac_tinh", "Ác Tính còn %s, Đan Độc còn %s.", "Malice now %s, Pill Toxin now %s.")
    for f, idx in (("vi_vn", 1), ("en_us", 2)):
        p = g.AS / "lang" / f"{f}.json"
        s = p.read_text(encoding="utf-8")
        cur = json.loads(s)
        body = s.rstrip()[:-1].rstrip()
        add = [(t[0], t[idx]) for t in g.LANG if t[0] not in cur]
        body += "".join(",\n  %s: %s" % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in add)
        p.write_text(body + "\n}\n", encoding="utf-8")
        json.loads(p.read_text(encoding="utf-8"))
        print(f, len(add))
