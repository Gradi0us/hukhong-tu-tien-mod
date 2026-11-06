#!/usr/bin/env python3
"""2026-11-06: du lieu Yeu Thu thuc vat (spawn, roi do, nguyen lieu, lang). Chay sau gen_c11_models.py, tu D:\\mc_mod."""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_c7 as g

FOREST = ["#minecraft:is_forest", "#minecraft:is_jungle", "minecraft:dark_forest", "minecraft:swamp", "minecraft:mangrove_swamp"]
C = {
    "thu_yeu": ("Thụ Yêu", "Tree Demon", "hostile", 1.0, 1.0, 1.3, 0.12,
                [("thu_yeu_tam", .6, 1, 2), ("hon_tinh", .08, 1, 2)], (FOREST + ["#minecraft:is_taiga"], 7, 1, 1, "any"), None),
    "thuc_nhan_hoa": ("Thực Nhân Hoa", "Man-Eater Flower", "hostile", 1.0, 1.0, 1.2, 0.12,
                      [("thuc_nhan_hoa_nhuy", .6, 1, 2), ("hon_tinh", .08, 1, 2)], (["#minecraft:is_jungle", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:lush_caves"], 8, 1, 2, "any"), None),
    "dang_xa": ("Đằng Xà", "Vine Serpent", "hostile", 1.0, 1.0, 1.1, 0.10,
                [("dang_xa_dang", .6, 1, 3), ("hon_tinh", .06, 1, 2)], (FOREST, 9, 1, 2, "any"), None),
    "quy_co": ("Quỷ Cô", "Fungal Fiend", "hostile", 1.0, 1.0, 1.1, 0.10,
               [("quy_co_bao_tu", .6, 1, 3), ("hon_tinh", .06, 1, 2)], (["minecraft:dark_forest", "minecraft:swamp", "minecraft:mushroom_fields", "minecraft:old_growth_spruce_taiga"], 8, 1, 2, "any"), None),
    "van_nien_thu_vuong": ("Vạn Niên Thụ Vương", "Myriad-Year Tree King", "hostile", 1.0, 1.0, 4.0, 1.0,
                           [("thu_vuong_linh_qua", 1.0, 1, 2), ("hon_tinh", 1.0, 2, 4), ("tiem_nang_thach", .25, 1, 1), ("van_nien_linh_chi", .6, 1, 2)],
                           (["minecraft:dark_forest", "minecraft:old_growth_spruce_taiga", "minecraft:old_growth_birch_forest", "#minecraft:is_jungle"], 1, 1, 1, "night"), None),
}
MATS = {
    "thu_yeu_tam": ("Thụ Yêu Tâm Mộc", "Tree Demon Heartwood", (150, 100, 60), "ingot"),
    "thuc_nhan_hoa_nhuy": ("Thực Nhân Hoa Nhụy", "Man-Eater Pistil", (220, 60, 80), "drop"),
    "dang_xa_dang": ("Đằng Xà Linh Đằng", "Vine Serpent Spirit Vine", (80, 160, 60), "tail"),
    "quy_co_bao_tu": ("Quỷ Cô Bào Tử", "Fiend Spores", (210, 120, 200), "dust"),
    "thu_vuong_linh_qua": ("Thụ Vương Linh Quả", "Tree King Spirit Fruit", (255, 200, 80), "orb"),
}

if __name__ == "__main__":
    g.C = C
    for cid, (vi, en, role, hm, dm, ew, ht, drops, spawn, tame) in C.items():
        d = {"role": role, "hp_mul": hm, "dmg_mul": dm, "exp_weight": ew, "hon_tinh": ht,
             "drops": [{"item": it if ":" in it else f"hukhong:{it}", "chance": ch, "min": mn, "max": mx} for it, ch, mn, mx in drops]}
        b, w, mn, mx, tm = spawn
        d["spawn"] = {"biomes": b, "weight": w, "min": mn, "max": mx, "time": tm}
        g.wj(g.DT / "hukhong_creatures" / f"{cid}.json", d)
        g.L(f"entity.hukhong.{cid}", vi, en)
        g.L(f"item.hukhong.{cid}_spawn_egg", f"Trứng {vi}", f"{en} Spawn Egg")
        g.wj(g.AS / "models/item" / f"{cid}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
        g.biome_mods(f"creature_{cid}", b, {"type": "neoforge:add_spawns", "spawners": {"type": f"hukhong:{cid}", "weight": w, "minCount": mn, "maxCount": mx}})
    for mid, (vi, en, col, style) in MATS.items():
        g.tex_item(mid, g.item_icon(col, style)); g.item_model(mid); g.L(f"item.hukhong.{mid}", vi, en)
    g.L("hukhong.thu_vuong.roots", "Vạn Đằng Triền! Rễ cây trồi lên quấn chặt!", "Myriad Roots! Roots burst out and bind!")
    g.L("hukhong.thu_vuong.sprout", "Thụ Vương gọi mầm yêu lên!", "The Tree King calls forth demon sprouts!")
    for f, idx in (("vi_vn", 1), ("en_us", 2)):
        p = g.AS / "lang" / f"{f}.json"
        s = p.read_text(encoding="utf-8"); cur = json.loads(s)
        body = s.rstrip()[:-1].rstrip()
        add = [(t[0], t[idx]) for t in g.LANG if t[0] not in cur]
        body += "".join(",\n  %s: %s" % (json.dumps(k, ensure_ascii=False), json.dumps(v, ensure_ascii=False)) for k, v in add)
        p.write_text(body + "\n}\n", encoding="utf-8"); json.loads(p.read_text(encoding="utf-8"))
        print(f, len(add))
