"""Audit hukhong translation keys and item model textures.

Read-only with respect to Java and lang JSON. Writes:
  docs/research/lang-audit.txt
  docs/research/asset-audit.txt
Run:  python tools/audit_lang.py [report|write]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src" / "main" / "java"
DIALOGUE = ROOT / "src" / "main" / "resources" / "data" / "hukhong" / "hukhong" / "dialogue"
LANG_DIR = ROOT / "src" / "main" / "resources" / "assets" / "hukhong" / "lang"
ITEM_MODELS = ROOT / "src" / "main" / "resources" / "assets" / "hukhong" / "models" / "item"
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "hukhong" / "textures"
OUT = ROOT / "docs" / "research"
MODID = "hukhong"

DECL_RE = re.compile(
    r"DeferredRegister(?:\.(Blocks|Items)|<\s*([\w.<>?]+?)\s*>)\s+(\w+)\s*=\s*"
    r"(?:DeferredRegister\.(createBlocks|createItems)|DeferredRegister\.create\(\s*Registries\.(\w+))"
)
REG_RE = re.compile(r"\b(\w+)\.(register\w*)\(\s*\"([a-z0-9_]+)\"\s*[,)]")
BUILD_RE = re.compile(r"\.build\(\s*\"([a-z0-9_]+)\"\s*\)")
LITERAL_RE = re.compile(r"\"((?:key\.categories|key|item|block|entity)?\.?hukhong\.[a-z0-9_.]*[a-z0-9_])\"")
TRANSLATABLE_RE = re.compile(r"translatable\(\s*\"([a-z0-9_.]+)\"\s*(\+?)")
# loop-generated ids in ChgRegistry: for (int i = 1; i <= 4; i++) "chg_" + i / "chg_core_" + i
LOOP_IDS = {"chg_": ["chg_1", "chg_2", "chg_3", "chg_4"],
            "chg_core_": ["chg_core_1", "chg_core_2", "chg_core_3", "chg_core_4"]}
KEY_LIT_RE = re.compile(r"\"(key\.(?:hukhong|categories\.hukhong)\.?[a-z0-9_.]*)\"")


def kind_of(decl_type, create_fn, registries_name):
    if create_fn == "createBlocks" or decl_type == "Blocks":
        return "block"
    if create_fn == "createItems" or decl_type == "Items":
        return "item"
    # exact match: BlockEntityType<?> must NOT be classed as an entity type
    if registries_name == "ENTITY_TYPE" or (decl_type and re.match(r"^EntityType\b", decl_type)):
        return "entity"
    return None


def scan_java():
    needed = {}      # key -> set of locations
    prefixes = {}    # dynamic prefix -> locations
    ids = {"item": set(), "block": set(), "entity": set()}
    unresolved = []
    for path in sorted(JAVA.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        rel = path.relative_to(ROOT).as_posix()
        recv = {}
        for m in DECL_RE.finditer(text):
            decl_type, generic, name, create_fn, reg_name = m.groups()
            k = kind_of(decl_type or generic, create_fn, reg_name)
            if k:
                recv[name] = k
        for m in REG_RE.finditer(text):
            name, method, rid = m.groups()
            k = recv.get(name)
            if k is None:
                if name not in ("ATTACHMENTS", "EFFECTS", "MENUS", "PARTICLES", "SOUNDS", "BE_TYPES",
                                "COMPONENTS", "NeoForgeRegistries"):
                    unresolved.append(f"{rid} via {name} in {rel}")
                continue
            # BlockItem display name is inherited from the block, so its key is block.hukhong.<id>
            if k == "item" and method == "registerSimpleBlockItem":
                k = "block"
            ids[k].add(rid)
            needed.setdefault(f"{k}.{MODID}.{rid}", set()).add(rel + " (registration)")
        for m in BUILD_RE.finditer(text):
            ids["entity"].add(m.group(1))
            needed.setdefault(f"entity.{MODID}.{m.group(1)}", set()).add(rel + " (registration)")
        for m in TRANSLATABLE_RE.finditer(text):
            key, concat = m.group(1), m.group(2)
            if concat or key.endswith(".") or key.endswith("_"):
                # dynamic: key is a prefix completed at runtime
                prefixes.setdefault(key, set()).add(rel)
            else:
                needed.setdefault(key, set()).add(rel + " (translatable)")
        for m in KEY_LIT_RE.finditer(text):
            needed.setdefault(m.group(1), set()).add(rel + " (keybind)")
        for m in LITERAL_RE.finditer(text):
            needed.setdefault(m.group(1), set()).add(rel + " (literal)")
    for pfx, locs in prefixes.items():
        if pfx in LOOP_IDS:
            for full_id in LOOP_IDS[pfx]:
                kind = "item" if "core" in pfx else "entity"
                needed.setdefault(f"{kind}.{MODID}.{full_id}", set()).update(locs)
                needed.setdefault(f"{kind}.{MODID}.{full_id}", set()).add("(loop)")
        else:
            # unenumerable runtime prefix: coverage is checked against lang keys in report()
            prefixes_dynamic.setdefault(pfx, set()).update(locs)
    return needed, ids, unresolved


prefixes_dynamic = {}


def scan_dialogue():
    keys = {}
    key_re = re.compile(r"^hukhong\.[a-z0-9_]+(\.[a-z0-9_]+)+$")

    def walk(node, rel):
        if isinstance(node, dict):
            for v in node.values():
                walk(v, rel)
        elif isinstance(node, list):
            for v in node:
                walk(v, rel)
        elif isinstance(node, str) and key_re.match(node):
            keys.setdefault(node, set()).add(rel + " (dialogue)")

    for path in sorted(DIALOGUE.rglob("*.json")):
        rel = path.relative_to(ROOT).as_posix()
        walk(json.loads(path.read_text(encoding="utf-8")), rel)
    return keys


def load_lang(name):
    return json.loads((LANG_DIR / name).read_text(encoding="utf-8"))


def check_models():
    missing = []
    checked = 0
    for path in sorted(ITEM_MODELS.rglob("*.json")):
        checked += 1
        data = json.loads(path.read_text(encoding="utf-8"))
        rel = path.relative_to(ROOT).as_posix()
        refs = list((data.get("textures") or {}).values())
        parent = data.get("parent", "")
        if not refs and parent.startswith(MODID + ":"):
            pfile = ROOT / "src" / "main" / "resources" / "assets" / MODID / "models" / (parent.split(":", 1)[1] + ".json")
            if pfile.exists():
                refs = list((json.loads(pfile.read_text(encoding="utf-8")).get("textures") or {}).values())
        for ref in refs:
            if ref.startswith("#"):
                continue
            if ":" in ref and not ref.startswith(MODID + ":"):
                continue  # vanilla texture, not our asset
            tex = ref.split(":", 1)[-1]
            target = TEXTURES / (tex + ".png")
            if not target.exists():
                missing.append((rel, ref, target.relative_to(ROOT).as_posix()))
    return checked, missing


def classify(all_keys, vi, en):
    """Return (missing_vi, missing_en, only_vi, only_en, literal_unknown)."""
    def is_lit(k):
        # pure literal = only seen as a plain string, never as a translatable/registration/dialogue/keybind key
        return all(loc.endswith("(literal)") for loc in all_keys[k])
    missing_vi = sorted(k for k in all_keys if k not in vi and not is_lit(k))
    missing_en = sorted(k for k in all_keys if k not in en and not is_lit(k))
    literal_unknown = sorted(k for k in all_keys if is_lit(k) and k not in vi and k not in en)
    return missing_vi, missing_en, sorted(set(vi) - set(en)), sorted(set(en) - set(vi)), literal_unknown


def dynamic_coverage(vi, en):
    rows = []
    for pfx, locs in sorted(prefixes_dynamic.items()):
        vk = {k for k in vi if k.startswith(pfx)}
        ek = {k for k in en if k.startswith(pfx)}
        rows.append((pfx, len(vk), len(ek), sorted(vk ^ ek)))
    return rows


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "report"
    needed, ids, unresolved = scan_java()
    dlg = scan_dialogue()
    vi = load_lang("vi_vn.json")
    en = load_lang("en_us.json")
    all_keys = {}
    for src in (needed, dlg):
        for k, v in src.items():
            all_keys.setdefault(k, set()).update(v)
    mvi, men, ovi, oen, lit = classify(all_keys, vi, en)
    checked, miss = check_models()
    if mode == "report":
        print("registered ids:", {k: len(v) for k, v in ids.items()})
        print("distinct keys collected:", len(all_keys))
        print("missing in vi:", len(mvi))
        print("missing in en:", len(men))
        print("only in vi:", ovi)
        print("only in en:", oen)
        print("literal strings not in either lang (review):", lit)
        print("dynamic prefix coverage (prefix, vi_count, en_count, vi/en diff):")
        for row in dynamic_coverage(vi, en):
            print("  ", row)
        print("unresolved registrations:", unresolved)
        print("item models:", checked, "missing textures:", len(miss))
        for m in miss:
            print("  TEX", m)
        print("MISSING VI:")
        for k in mvi:
            print("  ", k)
        print("MISSING EN:")
        for k in men:
            print("  ", k)
    if mode == "write":
        # Hand-written translations for keys that are missing from the lang files.
        TRANSLATIONS = {
            "entity.hukhong.story_npc": ("Truyện Nhân", "Storyteller"),
        }
        OUT.mkdir(parents=True, exist_ok=True)
        lines = []
        for k in sorted(set(mvi) | set(men) | set(ovi) | set(oen)):
            if k not in TRANSLATIONS:
                raise SystemExit(f"no translation prepared for {k}")
            vi_txt, en_txt = TRANSLATIONS[k]
            lines.append(f"{k} | vi: {vi_txt} | en: {en_txt}")
        (OUT / "lang-audit.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
        asset = [f"item models checked: {checked}", f"missing textures: {len(miss)}"]
        asset += [f"{rel} | ref {ref} | expected {exp}" for rel, ref, exp in miss]
        (OUT / "asset-audit.txt").write_text("\n".join(asset) + "\n", encoding="utf-8")
        print("wrote", len(lines), "lang lines and", len(asset), "asset lines")
