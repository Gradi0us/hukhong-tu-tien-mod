#!/usr/bin/env python3
"""
balance_sim.py - mo phong can bang 5 dao (PLAN-v1 S1/S2). Python 3, chi dung stdlib.

Moi hang so duoc DOC TRUC TIEP tu ma Java bang regex (xem class Params). Khong chep tay con so.
Cong thuc (logic) duoc port lai bang Python va ghi chu file:line trong REF[]; neu Java doi
hang so thi chay lai se thay doi theo. Neu doi *logic* thi phai sua port.

Chay:   python tools/balance_sim.py            -> in bang + ghi docs/REPORT-v1-balance-baseline.md
        python tools/balance_sim.py --quick    -> chi in, khong ghi file, bo qua what-if
"""
import itertools
import json
import math
import re
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path as FsPath

ROOT = FsPath(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/hukhong"
DATA = ROOT / "src/main/resources/data/hukhong/hukhong/core"
REPORT = ROOT / "docs/REPORT-v1-balance-baseline.md"

PATHS4 = ["KIEM", "DAO", "THE", "MA"]
ELEMENTS = ["QUANG", "LOI", "HOA", "THUY", "THO", "KIM"]
LEVELS = [1, 10, 30, 60, 80, 100]
APTS = ["B", "S"]

# ----------------------------------------------------------------------------- doc Java


def rd(rel):
    return (JAVA / rel).read_text(encoding="utf-8")


def line_of(rel, pattern):
    """So dong (1-based) dau tien khop regex trong file Java -> dung de trich dan file:line."""
    for i, ln in enumerate(rd(rel).splitlines(), 1):
        if re.search(pattern, ln):
            return i
    raise KeyError(f"{rel}: {pattern}")


def cite(rel, pattern):
    return f"{rel}:{line_of(rel, pattern)}"


def num(s):
    s = s.strip().rstrip("fFdD") if not s.strip().lower().startswith("0x") else s
    return float(eval(s, {"__builtins__": {}}, {})) if re.fullmatch(r"[\d.\s/*+()-]+", s) else float(s)


def const(rel, name):
    m = re.search(r"\b" + re.escape(name) + r"\s*=\s*(-?[\d.]+(?:\s*/\s*[\d.]+)?)", rd(rel))
    if not m:
        raise KeyError(f"{rel}: {name}")
    return num(m.group(1))


def segment(text, start, end=None):
    i = text.index(start)
    j = text.index(end, i + 1) if end else len(text)
    return text[i:j]


class Params:
    """Toan bo hang so doc tu Java. Co the ghi de (what-if) qua self.ov truoc khi dung."""

    def __init__(self):
        self.load()

    def load(self):
        P = self
        # ---- StatService
        t = rd("stats/StatService.java")
        P.table, P.vitals = {}, {}
        for nm, seg in (("table", segment(t, "double[] table(", "public static double aptFactor")),
                        ("vitals", segment(t, "double[] vitals(", "public static final double VANILLA_HP"))):
            for m in re.finditer(r"case (\w+) -> new double\[\]\{([^}]*)\}", seg):
                getattr(P, nm)[m.group(1)] = [num(x) for x in m.group(2).split(",")]
        apt_seg = segment(t, "public static double aptFactor", "public static Stats compute")
        P.apt_names, P.apt_f = [], {}
        for m in re.finditer(r"case (\w+) -> ([\d.]+);", apt_seg):
            P.apt_names.append(m.group(1))
            P.apt_f[m.group(1)] = float(m.group(2))
        m = re.search(r"double off = n >= 3 \? ([\d.]+) : n == 2 \? ([\d.]+) : 1\.0, tank = n >= 3 \? ([\d.]+) : n == 2 \? ([\d.]+)", t)
        P.off3, P.off2, P.tank3, P.tank2 = (float(x) for x in m.groups())
        P.REGEN_PER_LEVEL = const("stats/StatService.java", "REGEN_PER_LEVEL")
        P.THAN_LEVEL = int(const("stats/StatService.java", "THAN_LEVEL"))
        P.THAN_BONUS = const("stats/StatService.java", "THAN_BONUS")
        P.DEFENSE_CAP = const("stats/StatService.java", "DEFENSE_CAP")
        P.VANILLA_HP = const("stats/StatService.java", "VANILLA_HP")
        P.MAX_HP_CAP = const("stats/StatService.java", "MAX_HP_CAP")
        P.CRIT_DMG = const("stats/ModAttributes.java", "DEFAULT_CRIT_DAMAGE")
        # ---- Skill
        s = rd("skill/Skill.java")
        P.skill = {}
        for m in re.finditer(r'(\w+)\(Path\.(\w+), (\d), "(\w+)", (\d+), (\d+)\)', s):
            P.skill[m.group(1)] = dict(path=m.group(2), slot=int(m.group(3)), cost=int(m.group(5)), cd=int(m.group(6)))
        P.coef = {}
        for nm in ("DAO_1_COEF", "THE_1_COEF", "KIEM_1_COEF", "KIEM_2_COEF", "KIEM_ULT_COEF", "MA_1_COEF"):
            P.coef[nm] = const("skill/Skill.java", nm)
        # ---- SkillEffects
        se = rd("skill/SkillEffects.java")
        P.DOMAIN_DRAIN = const("skill/SkillEffects.java", "DOMAIN_DRAIN_PCT")
        P.DOMAIN_PULSE = int(const("skill/SkillEffects.java", "DOMAIN_PULSE_TICKS"))
        P.KIEM2_HITS = int(re.search(r"private static void lienHoanTram\(.*?for \(int i = 0; i < (\d+)", se, re.S).group(1))
        P.MATHAN_PCT = float(re.search(r"thanTicks > 0 \? ([\d.]+)", rd("pathrules/MaRules.java")).group(1))
        P.MATHAN_TICKS = int(const("pathrules/MaRules.java", "THAN_TICKS"))
        sa = re.search(r"stateAtk\(double a\) \{ return a >= 80 \? ([\d.]+) : a >= 60 \? ([\d.]+) : a >= 30 \? ([\d.]+)", rd("pathrules/MaRules.java"))
        P.MA_ACTINH_ATK = float(sa.group(3))  # gia dinh: Ac Tinh 30..59
        # Thể: Thiet Bo Sam / Kim Than = DAMAGE_RESISTANCE (vanilla: 20% moi bac), thoi gian tick
        P.THE2_RES = (int(re.search(r"private static void thietBoSam.*?DAMAGE_RESISTANCE, (\d+), (\d+)", se, re.S).group(1)),
                      int(re.search(r"private static void thietBoSam.*?DAMAGE_RESISTANCE, (\d+), (\d+)", se, re.S).group(2)))
        P.THEULT_RES = (int(re.search(r"private static void kimThan.*?DAMAGE_RESISTANCE, (\d+), (\d+)", se, re.S).group(1)),
                        int(re.search(r"private static void kimThan.*?DAMAGE_RESISTANCE, (\d+), (\d+)", se, re.S).group(2)))
        P.MAHOTHE_PCT = float(re.search(r"getMaxHealth\(\) \* ([\d.]+)f;\s*\n\s*int amp", se).group(1))
        P.DAOULT_HEAL = float(re.search(r"Math\.max\(4f, sp \* \(self \? ([\d.]+)f", se).group(1))
        # ---- DamageRules
        P.UNARMED = const("gates/DamageRules.java", "UNARMED_FIXED_DAMAGE")
        P.FIST_AURA = const("gates/DamageRules.java", "FIST_AURA_COEF")
        # ---- PassiveMath
        pm = "passive/PassiveMath.java"
        P.KIEM_HEAL_MAX = const(pm, "KIEM_HEAL_MAX")
        P.KIEM_EVERY = int(const(pm, "KIEM_EVERY"))
        P.DAO_MAX = const(pm, "DAO_MAX")
        P.BOSS_CAP = const(pm, "BOSS_CAP")
        P.DAO_EVERY = int(const(pm, "DAO_EVERY"))
        P.DAO_SKILL_MULT = const(pm, "DAO_SKILL_MULT")
        P.MA_PCT_MAX = const(pm, "MA_PCT_MAX")
        P.MA_EVERY = int(const(pm, "MA_EVERY"))
        P.MA_CD_TICKS = int(const(pm, "MA_COOLDOWN_TICKS"))
        P.THE_ARMOR_MAX = const(pm, "THE_ARMOR_MAX")
        P.THE_HP_MAX = const(pm, "THE_HP_MAX")
        P.THE_PER_TIER = int(const(pm, "THE_PER_TIER"))
        P.THE_STACK_CAP = int(const(pm, "THE_STACK_CAP"))
        # ---- PathRules (hoi tai nguyen)
        pr = "pathrules/PathRules.java"
        P.DAO_REGEN = const(pr, "DAO_REGEN")
        P.KIEM_REGEN_COMBAT = const(pr, "KIEM_REGEN_COMBAT")
        P.KIEM_HIT_PCT = const(pr, "KIEM_HIT_MANA_PCT")
        P.KIEM_HIT_ICD = const(pr, "KIEM_HIT_MANA_ICD_TICKS")
        P.THE_PASSIVE = const(pr, "THE_PASSIVE_REGEN")
        P.THE_MELEE_GAIN = const(pr, "THE_MELEE_GAIN")
        P.PHAP_REGEN = const(pr, "PHAP_REGEN")
        P.MA_HIT_GAIN = const(pr, "MA_HIT_GAIN")
        P.MA_HIT_CAP = int(re.search(r"m\.hitSecond >= (\d+)", rd("pathrules/MaRules.java")).group(1))
        # ---- SkillBookMath
        sb = "skillbook/SkillBookMath.java"
        P.SB_DMG = const(sb, "DMG_PER_LEVEL")
        P.SB_CD = const(sb, "CD_PER_LEVEL")
        P.SB_CDCAP = const(sb, "CD_CAP")
        P.SB_MAX = int(const(sb, "MAX_LEVEL"))
        P.SB_CAP_DIV = int(re.search(r"clampLevel\(1 \+ Math\.max\(0, playerLevel\) / (\d+)\)", rd(sb)).group(1))
        P.SB_TYP_DIV = int(re.search(r"clampLevel\(1 \+ \(levelCap\(playerLevel\) - 1\) / (\d+)\)", rd(sb)).group(1))
        P.grade_min = {m.group(1): int(m.group(2)) for m in re.finditer(r'(\w+)\("[^"]+", (\d+), ', rd("skillbook/SkillGrade.java"))}
        # ---- BookSkill
        bt = rd("skillbook/BookSkill.java")
        P.book = {}
        for m in re.finditer(r'(\w+)\("(\w+)", (Path\.\w+|null), SkillGrade\.(\w+), (\d+), (\d+), ([\d.]+), "([^"]*)",\s*"([^"]*)"\)', bt):
            desc = m.group(9)
            hits = 1
            for pat in (r"(\d+) nh\S+ x", r"(\d+) thanh phi"):
                mm = re.search(pat, desc)
                if mm:
                    hits = int(mm.group(1))
            P.book[m.group(1)] = dict(id=m.group(2), path=None if m.group(3) == "null" else m.group(3)[5:], grade=m.group(4),
                                      cost=int(m.group(5)), cd=int(m.group(6)), coef=float(m.group(7)), name=m.group(8), hits=hits)
        # kiem tra cheo so nhip voi ma hieu ung
        be = rd("skillbook/BookSkillEffects.java")
        assert re.search(r"quangTranPulse\(.*?\{\s*if \(n >= %d" % P.book["QUANG_TRAN"]["hits"], be, re.S), "QUANG_TRAN nhip lech"
        assert re.search(r"huyetDaiPulse\(.*?\{\s*if \(n >= %d" % P.book["HUYET_TE_DAI"]["hits"], be, re.S), "HUYET_TE_DAI nhip lech"
        # ---- BanMenhSkill
        P.bm = {}
        for m in re.finditer(r'(\w+)\(Path\.(\w+), "(\w+)", (\d+), (\d+), ([\d.]+), ([\d.]+), ([\d.]+)\)', rd("skill/BanMenhSkill.java")):
            P.bm[m.group(2)] = dict(cd=int(m.group(4)), cost=int(m.group(5)), base=float(m.group(6)), per=float(m.group(7)), cap=float(m.group(8)))
        P.BM_UNLOCK = int(const("skill/BanMenhSkill.java", "UNLOCK_LEVEL"))
        bme = rd("skill/BanMenhSkillEffects.java")
        P.PHANXET_HU = float(re.search(r"t instanceof HuMob \? ([\d.]+)", bme).group(1))
        P.HUYETHAI_N = int(re.search(r"private static void huyetHai\(.*?for \(int i = 0; i < (\d+)", bme, re.S).group(1))
        # ---- Ban Menh kiem (don danh thuong)
        P.art = False   # Linh Bao (Dao Linh / Linh The / Ma Cong Ba The Quyet); bat bang ov["_art"]
        lb = "linhbao/LinhBaoMath.java"
        P.LB_DAO_TICKS = int(const(lb, "DAO_BOLT_INTERVAL_TICKS"))
        P.LB_COEF = {k: [float(x) for x in re.search(nm + r"\s*=\s*\{([^}]*)\}", rd(lb)).group(1).split(",")]
                     for k, nm in (("DAO", "DAO_BOLT_COEF"), ("THE", "THE_HIT_COEF"), ("MA", "MA_HIT_COEF"))}
        P.QUALITY = [float(x) for x in re.search(r"QUALITY = \{([^}]*)\}", rd("banmenh/BanMenhService.java")).group(1).replace("f", "").split(",")]
        P.STAGE_LEVELS = [int(x) for x in re.search(r"STAGE_LEVELS = \{([^}]*)\}", rd("banmenh/BanMenhMath.java")).group(1).split(",")]
        ms = re.search(r"createAttributes\(Tiers\.IRON, (\d+), (-?[\d.]+)F\)", rd("banmenh/BanMenhKiemItem.java"))
        P.SWORD_DMG = 1.0 + float(ms.group(1)) + 2.0   # 1 (tay) + 2 (item) + 2 (Tiers.IRON attackDamageBonus - hang so vanilla)
        P.SWORD_RATE = 4.0 + float(ms.group(2))        # vanilla attack speed 4.0 + modifier
        # ---- PhapMath
        ph = rd("phaptu/PhapMath.java")
        P.phap_coef = {}
        for m in re.finditer(r"/\* (\w+)\s*\*/\s*\{([^}]*)\}", ph):
            P.phap_coef[m.group(1)] = [num(x) for x in m.group(2).split(",")]
        for nm in ("QUANG_EXPLODE_MAX", "HOA_BURN_COEF", "THO_SHOT_COEF", "KIM_BONUS_MAX", "KIM_COOLDOWN_MULT", "THO_DMG_MAX",
                   "LOI_HP_PCT_MAX", "LOI_DMG_CAP_MULT", "THUY_IMMUNE_MAX", "THO_DMG_FLOOR", "THO_UPTIME", "THO_CROWD_TICKS"):
            setattr(P, "PH_" + nm, const("phaptu/PhapMath.java", nm))
        P.PH_HOA_STACKS = int(const("phaptu/PhapMath.java", "HOA_STACKS"))
        P.PH_THO_MAXT = int(const("phaptu/PhapMath.java", "THO_MAX_TURRETS"))
        P.PH_THO_SHOT = int(const("phaptu/PhapMath.java", "THO_SHOT_TICKS"))
        # ---- HuHoa (quai)
        hh = "entity/HuHoa.java"
        P.HP_BASE, P.HP_LIN, P.HP_QUAD, P.HP_CUBE, P.THAN_HP_MUL = (const(hh, n) for n in ("HP_BASE", "HP_LIN", "HP_QUAD", "HP_CUBE", "THAN_HP_MUL"))
        P.DMG_QUAD = const(hh, "DMG_QUAD")
        P.DMG_LIN = float(re.search(r"return 1 \+ ([\d.]+) \* g \+ DMG_QUAD", rd(hh)).group(1))
        P.MOB_ARMOR_PER = float(re.search(r"Math\.min\(20, ([\d.]+) \* \(level - 1\)\)", rd(hh)).group(1))
        P.VANILLA_HP_CAP = const(hh, "VANILLA_HP_CAP")
        P.mobs = self._mobs()
        P.boss = self._boss()
        # ---- Loi (cores)
        P.cores = self._cores()
        cr = rd("cores/CoreRoller.java")
        rows = re.search(r"BASE = \{(.*?)\n    \};", cr, re.S).group(1)
        P.core_base = [[int(x) for x in r.split(",")] for r in re.findall(r"\{([\d, ]+)\}", rows)]
        cc = rd("cores/CoreService.java")
        assert "level / 10" in cc and "level >= 95" in cc  # milestoneOfLevel
        P.ov_log = []
        P.THAN_CRIT = True

    def _mobs(self):
        out = {}
        ent = "entity/"

        def mm(f, hp_re, dmg_re):
            t = rd(ent + f)
            return t

        # (ten, file, base hp, base dmg, base armor, hpMul, dmgMul) - base vanilla neu ke thua Zombie/Skeleton/Spider
        vanilla = {"HuThi": (20.0, 3.0, 0.0, "Zombie"), "HuCot": (20.0, 2.0, 0.0, "Skeleton"), "HuChu": (16.0, 2.0, 0.0, "Spider")}
        for nm, (hp, dmg, ar, par) in vanilla.items():
            t = rd(f"{ent}{nm}.java")
            assert f"extends {par}" in t
            m = re.search(r"applyStats\(this, lv, ([\d.]+) \* cm, (?:([\d.]+) \* cm|cm)\)", t)
            out[nm] = dict(hp=hp, dmg=dmg, armor=ar, hpmul=float(m.group(1)), dmgmul=float(m.group(2) or 1.0), src="vanilla " + par)
        for nm in ("HuAnh", "HuGiap", "HuTrung", "HuTrieuSu"):
            t = rd(f"{ent}{nm}.java")
            hp = float(re.search(r"MAX_HEALTH, ([\d.]+)\)", t).group(1))
            dmg = float(re.search(r"ATTACK_DAMAGE, ([\d.]+)\)", t).group(1))
            ar = float(re.search(r"ARMOR, ([\d.]+)\)", t).group(1))
            out[nm] = dict(hp=hp, dmg=dmg, armor=ar, hpmul=float(re.search(r"hpMul\(\) \{ return ([\d.]+)", t).group(1)),
                           dmgmul=float(re.search(r"dmgMul\(\) \{ return ([\d.]+)", t).group(1)), src="attrs()")
        return out

    def _boss(self):
        t = rd("entity/WorldBoss.java")
        return dict(hp=float(re.search(r"MAX_HEALTH, ([\d.]+)\)", t).group(1)), dmg=float(re.search(r"ATTACK_DAMAGE, ([\d.]+)\)", t).group(1)),
                    armor=float(re.search(r"ARMOR, ([\d.]+)\)", t).group(1)), hpmul=float(re.search(r"hpMul\(\) \{ return ([\d.]+)", t).group(1)),
                    dmgmul=float(re.search(r"dmgMul\(\) \{ return ([\d.]+)", t).group(1)))

    def _cores(self):
        out = []
        for f in sorted(DATA.glob("*.json")):
            j = json.loads(f.read_text(encoding="utf-8"))
            ps = []
            for p in j.get("passives", []):
                ps.append((p["attribute"].split(":")[-1], p["operation"], p["values"]))
            out.append(dict(id=f.stem, path=j["path"], system=j.get("system"), min_tier=j.get("min_tier"), passives=ps))
        return out

    # ------------------------------------------------------------------ cong thuc port
    def aptf(self, a):
        return self.apt_f[a]

    def frac(self, a):
        return (self.apt_names.index(a) + 1) / len(self.apt_names)

    def t(self, a):
        return self.apt_names.index(a) / (len(self.apt_names) - 1)

    def hp_scale(self, lv):
        g = max(0, lv - 1)
        s = self.HP_BASE + self.HP_LIN * g + self.HP_QUAD * g * g + self.HP_CUBE * g ** 3
        return s * self.THAN_HP_MUL if lv >= self.THAN_LEVEL else s

    def dmg_scale(self, lv):
        g = max(0, lv - 1)
        return 1 + self.DMG_LIN * g + self.DMG_QUAD * g * g

    def mob_armor(self, lv):
        return min(20, self.MOB_ARMOR_PER * (lv - 1))

    def sb_dmg(self, lv):
        return 1.0 + self.SB_DMG * (max(1, min(self.SB_MAX, lv)) - 1)

    def sb_cd_ticks(self, base, lv):
        lv = max(1, min(self.SB_MAX, lv))
        mult = 1.0 - min(self.SB_CDCAP, self.SB_CD * (lv - 1))
        return max(1, int(math.floor(base * mult + 0.5)))

    def sb_typical(self, plv):
        cap = max(1, min(self.SB_MAX, 1 + max(0, plv) // self.SB_CAP_DIV))
        return max(1, min(self.SB_MAX, 1 + (cap - 1) // self.SB_TYP_DIV))

    def bm_coef(self, path, lv):
        d = self.bm[path]
        l = max(self.BM_UNLOCK, min(100, lv))
        return min(d["cap"], d["base"] + d["per"] * (l - self.BM_UNLOCK))

    def stage(self, lv):
        s = 0
        for i, x in enumerate(self.STAGE_LEVELS):
            if lv >= x:
                s = i
        return s

    def milestone(self, lv):
        if lv >= 99:
            return 11 if lv == 99 else 12
        if lv >= 95:
            return 10
        return lv // 10


# ----------------------------------------------------------------------------- Loi: "Loi trung binh"
ATTR_FOR = {"attack_power": "atk", "spell_power": "sp", "defense": "def", "lifesteal": "ls"}


def core_bonus(P, lv, pathnames, mode):
    """Tra ve {'atk':(flat,frac),'sp':..,'def':..} ky vong tu cac moc Loi.
    mode 'none' = khong Loi; 'avg' = moi moc nhan 1 Loi, chon deu trong cac Loi cua (cac) dao + 'any';
    'focus' = moi moc chon Loi tan cong/phong thu tot nhat co the (can tren cua nguoi chinh)."""
    res = {k: [0.0, 0.0] for k in ("atk", "sp", "def", "ls")}
    if mode == "none":
        return res
    ms = P.milestone(lv)
    names = {p.lower() for p in pathnames} | {"any"}
    cand = [c for c in P.cores if c["path"] in names and c["system"] != "than"]
    for m in range(1, min(ms, 11) + 1):
        w = P.core_base[m - 1]
        tot = sum(w)
        # gia tri ky vong theo phan bo pham
        per_core = []
        for c in cand:
            acc = {k: [0.0, 0.0] for k in res}
            for attr, op, vals in c["passives"]:
                k = ATTR_FOR.get(attr)
                if not k:
                    continue
                ev = sum(w[i] * vals[min(i, len(vals) - 1)] for i in range(len(w))) / tot
                acc[k][0 if op == "add_value" else 1] += ev
            per_core.append(acc)
        if not per_core:
            continue
        if mode == "avg":
            for acc in per_core:
                for k in res:
                    res[k][0] += acc[k][0] / len(per_core)
                    res[k][1] += acc[k][1] / len(per_core)
        else:  # focus: lay Loi co tong (flat+frac*~base) lon nhat cho moi nhom, xem nhu 1 Loi ATK + 1 Loi DEF xen ke
            key = "off" if m % 2 else "def"
            def score(acc):
                return (acc["atk"][1] + acc["sp"][1] + 0.05 * (acc["atk"][0] + acc["sp"][0])) if key == "off" else (acc["def"][1] + 0.05 * acc["def"][0])
            best = max(per_core, key=score)
            for k in res:
                res[k][0] += best[k][0]
                res[k][1] += best[k][1]
    if lv >= 100:  # Than Loi (min_tier kim) +15% cho dao tuong ung (add_multiplied_base)
        for c in P.cores:
            if c["system"] == "than" and c["path"] in names:
                for attr, op, vals in c["passives"]:
                    k = ATTR_FOR.get(attr)
                    if k:
                        res[k][1] += vals[0] if mode != "none" else 0
    return res


# ----------------------------------------------------------------------------- Player build
class Build:
    pass


def build(P, lv, paths, apt, core_mode="avg", element=None):
    """paths: list ten dao; apt: dict dao->tu chat. Cong thuc StatService.compute + than + loi."""
    B = Build()
    B.lv, B.paths, B.apt, B.element = lv, list(paths), apt, element
    g = max(0, lv - 1)
    atk = sp = de = cr = ls = 0.0
    res = 0.0
    for i, p in enumerate(paths):
        t = P.table[p]
        f = P.aptf(apt.get(p, "B"))
        atk = max(atk, (t[0] + t[1] * g) * f)
        sp = max(sp, (t[2] + t[3] * g) * f)
        de = max(de, (t[4] + t[5] * g) * f)
        cr = max(cr, t[6] + t[7] * g)
        ls = max(ls, (t[8] + t[9] * g) * f)
        r = t[10] + t[11] * g
        res = r if i == 0 else max(res, r)
    n = len(paths)
    off = P.off3 if n >= 3 else P.off2 if n == 2 else 1.0
    tank = P.tank3 if n >= 3 else P.tank2 if n == 2 else 1.0
    atk, sp, ls = atk * off, sp * off, ls * off
    de *= tank
    regen = 1.0 + P.REGEN_PER_LEVEL * g
    cb = core_bonus(P, lv, paths, core_mode)
    atk = atk * (1 + cb["atk"][1]) + cb["atk"][0]
    sp = sp * (1 + cb["sp"][1]) + cb["sp"][0]
    de = de * (1 + cb["def"][1]) + cb["def"][0]
    ls = ls + cb["ls"][0]
    cd = P.CRIT_DMG
    thanm = 1.0 + P.THAN_BONUS if lv >= P.THAN_LEVEL else 1.0
    B.atk, B.sp, B.defense = atk * thanm, sp * thanm, de * thanm
    ct = thanm if getattr(P, "THAN_CRIT", True) else 1.0   # True = Than x1.5 cung ap len crit_chance/crit_damage (hien trang)
    B.crit = min(1.0, min(1.0, cr) * ct)
    B.critd = cd * ct
    B.ls = ls * thanm
    B.regen = regen * thanm
    B.resmax = max(1, math.floor(res * thanm + 0.5))
    # sinh luc
    hp = P.VANILLA_HP
    for p in paths:
        v = P.vitals[p]
        hp = max(hp, (v[0] + v[1] * g) * P.aptf(apt.get(p, "B")))
    B.hp = min(P.MAX_HP_CAP, max(P.VANILLA_HP, hp * tank))
    if "THE" in paths:  # Kien Giap (PassiveMath.theMaxHp) khi du tang (gia dinh danh lien tuc)
        a = apt.get("THE", "B")
        tiers = P.THE_STACK_CAP // P.THE_PER_TIER
        B.hp += tiers * P.THE_HP_MAX * P.frac(a)
    B.critmul = 1 + B.crit * (B.critd - 1)
    B.mit = min(P.DEFENSE_CAP, B.defense / (B.defense + 100.0)) if B.defense > 0 else 0.0
    B.sl = P.sb_typical(lv)
    B.sd = P.sb_dmg(B.sl)
    B.aura_cap = 1.0
    return B


# ----------------------------------------------------------------------------- Kit
def phap_dmg_mult(P, e, a):
    if e == "QUANG":
        return 1 + P.PH_QUANG_EXPLODE_MAX * P.frac(a) * 0.9      # PhapMath.damageMult
    if e == "KIM":
        return 1 + P.PH_KIM_BONUS_MAX * P.frac(a)
    return 1.0


def phap_passive_dps(P, e, a, sp):
    """PhapMath.passiveDps (port 1-1)."""
    if e == "HOA":
        return P.PH_HOA_BURN_COEF * (0.35 + 0.65 * P.frac(a)) * P.PH_HOA_STACKS * sp * 0.7
    if e == "THO":
        turrets = 1 + int(math.floor((P.PH_THO_MAXT - 1) * P.t(a) + 0.5))
        shot = P.PH_THO_DMG_MAX * (P.PH_THO_DMG_FLOOR + (1 - P.PH_THO_DMG_FLOOR) * P.frac(a)) * P.PH_THO_SHOT_COEF * sp
        return turrets * shot * (20.0 / (P.PH_THO_SHOT + P.PH_THO_CROWD_TICKS * (turrets - 1))) * P.PH_THO_UPTIME
    if e == "LOI":
        return min(P.PH_LOI_HP_PCT_MAX * P.frac(a) * 400, P.PH_LOI_DMG_CAP_MULT * sp) / 3.0
    if e == "THUY":
        return 0.30 * sp * P.frac(a)
    return 0.0


def mk(name, stat, coef, hits, cd_ticks, cost, kind="cast", mult=1.0, enh=True, **kw):
    return dict(name=name, stat=stat, coef=coef, hits=hits, cd_ticks=cd_ticks, cost=cost, kind=kind, mult=mult, enh=enh, **kw)


def base_kit(P, B, path, element):
    """Cac phim Q/E/R/T mac dinh cua 1 dao (Skill.java + SkillEffects + BanMenhSkill)."""
    S, C = P.skill, P.coef
    k = []
    if path == "KIEM":
        k.append(mk("Tram Khi (Q)", "atk", C["KIEM_1_COEF"], 1, S["KIEM_1"]["cd"], S["KIEM_1"]["cost"]))
        k.append(mk("Lien Hoan Tram (E)", "atk", C["KIEM_2_COEF"], P.KIEM2_HITS, S["KIEM_2"]["cd"], S["KIEM_2"]["cost"]))
        k.append(mk("Kiem Vuc (T)", "atk", C["KIEM_ULT_COEF"], 1, 0, 0, kind="domain"))
    elif path == "MA":
        k.append(mk("Huyet Trao (Q)", "atk", C["MA_1_COEF"], 1, S["MA_1"]["cd"], S["MA_1"]["cost"]))
        k.append(mk("Ma Khi Ho The (E)", "atk", 0, 0, S["MA_2"]["cd"], S["MA_2"]["cost"], kind="shield"))
        k.append(mk("Ma Than Giang The (T)", "atk", 0, 0, S["MA_ULT"]["cd"], S["MA_ULT"]["cost"], kind="mabuff"))
    elif path == "DAO":
        k.append(mk("Thanh Quang Tien (Q)", "sp", C["DAO_1_COEF"], 1, S["DAO_1"]["cd"], S["DAO_1"]["cost"], mult=P.DAO_SKILL_MULT))
        k.append(mk("Ban Phuc (E)", "sp", 0, 0, S["DAO_2"]["cd"], S["DAO_2"]["cost"], kind="util"))
        k.append(mk("Thanh Giang (T)", "sp", 0, 0, S["DAO_ULT"]["cd"], S["DAO_ULT"]["cost"], kind="heal", heal_sp=P.DAOULT_HEAL))
    elif path == "THE":
        k.append(mk("Chan Dia (Q)", "atk", C["THE_1_COEF"], 1, S["THE_1"]["cd"], S["THE_1"]["cost"]))
        k.append(mk("Thiet Bo Sam (E)", "atk", 0, 0, S["THE_2"]["cd"], S["THE_2"]["cost"], kind="resist", res=(0.2, P.THE2_RES[0] / 20.0)))
        k.append(mk("Kim Than (T)", "atk", 0, 0, S["THE_ULT"]["cd"], S["THE_ULT"]["cost"], kind="resist", res=(0.4, P.THEULT_RES[0] / 20.0)))
    elif path == "PHAP":
        e = element
        a = B.apt.get("PHAP", "B")
        dm = phap_dmg_mult(P, e, a)
        cdm = P.PH_KIM_COOLDOWN_MULT if e == "KIM" else 1.0
        for i, key in enumerate(("PHAP_1", "PHAP_2", "PHAP_3", "PHAP_ULT")):
            c = P.phap_coef[e][i]
            if c > 0:
                k.append(mk(f"{e} {'QERT'[i]}", "sp", c, 1, S[key]["cd"], S[key]["cost"], mult=dm, cdmult=cdm))
    if path != "PHAP" and B.lv >= P.BM_UNLOCK:
        bmd = P.bm[path]
        c = P.bm_coef(path, B.lv)
        if path == "KIEM":
            k.append(mk("Nhat Kiem (R)", "atk", c, 1, bmd["cd"], bmd["cost"], enh=False))
        elif path == "DAO":
            k.append(mk("Phan Xet (R)", "sp", c, 1, bmd["cd"], bmd["cost"], enh=False, mult=P.PHANXET_HU))
        elif path == "THE":
            k.append(mk("Dia Liet (R)", "atk", c, 1, bmd["cd"], bmd["cost"], enh=False))
        else:
            k.append(mk("Huyet Hai (R)", "atk", c, P.HUYETHAI_N, bmd["cd"], bmd["cost"], enh=False))
    return k


def book_candidates(P, B, path):
    out = []
    for key, b in P.book.items():
        if b["path"] != path:
            continue
        if B.lv < P.grade_min[b["grade"]]:
            continue
        spell = path == "DAO"
        if key == "THANH_TAM_CHU":
            continue
        out.append(mk(b["name"] + " [sach]", "sp" if spell else "atk", b["coef"], b["hits"], b["cd"], b["cost"]))
    return out


# ----------------------------------------------------------------------------- Mo phong
class Target:
    def __init__(self, P, name, lv):
        if name == "BOSS":
            d = P.boss
        else:
            d = P.mobs[name]
        self.name, self.lv = name, lv
        self.boss = name == "BOSS"
        self.design = d["hp"] * d["hpmul"] * P.hp_scale(lv)
        self.real = min(P.VANILLA_HP_CAP, self.design)
        self.s = self.real / self.design if self.design > self.real else 1.0
        self.armor = d["armor"] + min(20, P.MOB_ARMOR_PER * (lv - 1))   # HuHoa.applyStats: ARMOR add_value min(20, 0.25(L-1)) tren giap goc
        self.atk = d["dmg"] * d["dmgmul"] * P.dmg_scale(lv)
        self.hu = True


def armor_factor(armor, dmg):
    red = min(20.0, max(armor / 5.0, armor - dmg / 2.0))
    return 1 - red / 25.0


MELEE_UPTIME = 0.55       # ty le thoi gian dang vung kiem (cung gia dinh BalanceSimTest)
SKILL_EFF = 0.85          # hieu suat thao tac ky nang
BOLT_UPTIME = 0.85


def simulate(P, B, kit, tgt, T=60.0, dt=0.05, stop_hp=None, mmode=None, melee=True):
    """Chay luan phien. Tra ve dict: total (HP thiet ke bo di), burst5, heal, tkill, skill_dmg, casts."""
    path_set = set(B.paths)
    n = int(T / dt)
    mana = float(B.resmax)
    ready = [0.0] * len(kit)
    total = burst5 = heal = skill_dmg = 0.0
    casts = 0
    ccasts = 0
    cdmg = 0.0
    cm = B.critmul
    ls = B.ls
    hp_cap_heal = 0.04 * B.hp
    ma_buff_until = -1.0
    domain_on = False
    domain_next_pulse = 0.0
    domain_next_drain = 0.0
    kiem_hits = 0
    dao_stacks = 0
    ma_stacks = 0
    melee_acc = 0.0
    bolt_acc = 0.0
    mana_hit_gain_sec = 0
    next_sec = 1.0
    last_kiem_mana = -99.0
    t_kill = None
    regen_ps = 0.0
    q = P.QUALITY[P.stage(B.lv)]
    apt = B.apt
    # tai nguyen hoi moi giay (lien tuc)
    if "KIEM" in path_set:
        regen_ps += P.KIEM_REGEN_COMBAT * B.regen
    if "DAO" in path_set:
        regen_ps += P.DAO_REGEN * B.regen
    if "PHAP" in path_set:
        regen_ps += P.PHAP_REGEN * B.regen
    if "THE" in path_set:
        regen_ps += max(1, round(P.THE_PASSIVE * B.regen))
    melee_rate = P.SWORD_RATE * MELEE_UPTIME
    # phap tu bi dong
    pdps = phap_passive_dps(P, B.element, apt.get("PHAP", "B"), B.sp) * cm if "PHAP" in path_set and B.element else 0.0
    # Ma: tay khong (tia ma) neu co MA va khong co KIEM; neu co vu khi thi dung kiem
    ma_bolt = "MA" in path_set
    # phan tram HP muc tieu (HP that, tran 1024); boss bi chia 1/3 (PassiveMath.pctOfMaxHp)
    def pct_dmg(pct):
        return pct * tgt.real * (P.BOSS_CAP if tgt.boss else 1.0)
    dao_pct = P.DAO_MAX * P.frac(apt.get("DAO", "B"))
    ma_pct = P.MA_PCT_MAX * P.frac(apt.get("MA", "B"))
    kiem_heal = P.KIEM_HEAL_MAX * P.frac(apt.get("KIEM", "B"))

    srcs = {"melee": 0.0, "skill": 0.0, "pass": 0.0}
    art = getattr(P, "art", False)
    lb_bolt_next = P.LB_DAO_TICKS / 20.0

    def add(d, t_, src="melee"):
        nonlocal total, burst5, t_kill
        srcs[src] += d
        total += d
        if t_ < 5.0:
            burst5 += d
        if stop_hp is not None and t_kill is None and total >= stop_hp:
            t_kill = t_

    def hit_skill(dmg_raw, t_, is_skill_event=True):
        """1 don ky nang da gom he so. Ap Bao Kich + hut mau + noi tai Dao/Ma dem don."""
        nonlocal heal, skill_dmg, dao_stacks, mana_hit_gain_sec, mana
        d = dmg_raw * cm
        add(d, t_, "skill")
        skill_dmg += d
        if ls > 0:
            heal += min(d * ls, hp_cap_heal)
        if "MA" in path_set and mana_hit_gain_sec < P.MA_HIT_CAP:
            mana_hit_gain_sec += 1
            mana = min(B.resmax, mana + P.MA_HIT_GAIN)

    def dao_tick(t_):
        nonlocal dao_stacks, heal
        if "DAO" not in path_set:
            return
        dao_stacks += 1
        if dao_stacks >= P.DAO_EVERY:
            dao_stacks = 0
            heal += B.hp * dao_pct
            add(pct_dmg(dao_pct), t_, "pass")

    def melee_side(t_, d):
        """Hieu ung phu cua mot don can chien trung: Kiem (hoi tai nguyen, don thu 4), hut mau, Ma (+1 tai nguyen), Dao dem don."""
        nonlocal kiem_hits, last_kiem_mana, mana, heal, mana_hit_gain_sec
        if "KIEM" in path_set:
            if t_ - last_kiem_mana >= P.KIEM_HIT_ICD / 20.0:
                last_kiem_mana = t_
                mana = min(B.resmax, mana + max(1, round(B.resmax * P.KIEM_HIT_PCT)))
            kiem_hits += 1
            if kiem_hits >= P.KIEM_EVERY:
                kiem_hits = 0
                ex = d * B.critd
                add(ex, t_, "pass")
                heal += ex * kiem_heal
        if ls > 0:
            heal += min(d * ls, hp_cap_heal)
        if "MA" in path_set and mana_hit_gain_sec < P.MA_HIT_CAP:
            mana_hit_gain_sec += 1
            mana = min(B.resmax, mana + P.MA_HIT_GAIN)
        dao_tick(t_)

    def atkv(t_):
        return B.atk * ((1 + P.MATHAN_PCT) if t_ < ma_buff_until else 1.0) * (1 + (P.MA_ACTINH_ATK if "MA" in path_set else 0.0))

    # Ma nhan Ac Tinh bonus vao Cong (MaRules.attackBonus) - tinh gop vao atk
    order = sorted(range(len(kit)), key=lambda i: -(kit[i]["coef"] * kit[i]["hits"] * kit[i]["mult"] * (B.atk if kit[i]["stat"] == "atk" else B.sp)))
    cdsec = []
    for s_ in kit:
        base = s_["cd_ticks"]
        if s_["enh"] and base > 0:
            base = P.sb_cd_ticks(base, B.sl)
        cdsec.append(base / 20.0 * s_.get("cdmult", 1.0))
    for step in range(n):
        t_ = step * dt
        if t_ >= next_sec:
            next_sec += 1.0
            mana_hit_gain_sec = 0
        mana = min(B.resmax, mana + regen_ps * dt)
        # ky nang
        for i in order:
            s_ = kit[i]
            kind = s_["kind"]
            if kind == "domain":
                if not domain_on:
                    drain = max(1, round(B.resmax * P.DOMAIN_DRAIN))
                    if mana >= drain * 2:
                        domain_on = True
                        domain_next_pulse = t_
                        domain_next_drain = t_
                if domain_on:
                    drain = max(1, round(B.resmax * P.DOMAIN_DRAIN))
                    if t_ >= domain_next_drain:
                        if mana < drain:
                            domain_on = False
                        else:
                            mana -= drain
                            domain_next_drain += 1.0
                    if domain_on and t_ >= domain_next_pulse:
                        domain_next_pulse += P.DOMAIN_PULSE / 20.0
                        casts += 1
                        hit_skill(s_["coef"] * atkv(t_) * B.sd * SKILL_EFF, t_)
                continue
            if t_ < ready[i] or mana < s_["cost"]:
                continue
            mana -= s_["cost"]
            ready[i] = t_ + cdsec[i]
            casts += 1
            if kind == "cast":
                statv = atkv(t_) if s_["stat"] == "atk" else B.sp
                per = s_["coef"] * statv * s_["mult"] * (B.sd if s_["enh"] else 1.0) * SKILL_EFF
                ccasts += 1
                cdmg += per * s_["hits"] * cm
                for _ in range(s_["hits"]):
                    hit_skill(per, t_)
                dao_tick(t_)
                if s_["name"].startswith("Huyet Trao") and ls > 0:
                    heal += min(per * s_["hits"] * cm * ls, hp_cap_heal)
            elif kind == "mabuff":
                ma_buff_until = t_ + P.MATHAN_TICKS / 20.0
            elif kind == "shield":
                heal += B.hp * P.MAHOTHE_PCT
            elif kind == "heal":
                heal += max(4.0, B.sp * s_["heal_sp"])
        if art and "DAO" in path_set and t_ >= lb_bolt_next:   # Dao Linh: tia sang tu dong (hurtBySkill -> bao kich, khong tinh SKILL_EFF)
            lb_bolt_next += P.LB_DAO_TICKS / 20.0 / BOLT_UPTIME
            hit_skill(B.sp * P.LB_COEF["DAO"][P.stage(B.lv)] * q * B.sd, t_)
            dao_tick(t_)
        # tay: Ban Menh Kiem (vu khi) / nam dam The Tu / tia ma tay khong cua Ma Tu
        if melee:
            if "THE" in path_set or mmode == "hand":
                if "THE" in path_set:
                    melee_acc += dt
                    period = 1.0 / melee_rate
                    while melee_acc >= period:
                        melee_acc -= period
                        aura = mana / B.resmax
                        d = atkv(t_) * (1 + P.FIST_AURA * aura) * cm
                        if art: d += atkv(t_) * P.LB_COEF["THE"][P.stage(B.lv)] * q   # Linh The: cong sau bao kich
                        add(d * armor_factor(tgt.armor, d * tgt.s), t_)
                        mana = min(B.resmax, mana + P.THE_MELEE_GAIN)
                        melee_side(t_, d)
                if "MA" in path_set:  # tia ma tay khong (PassiveService.maBolt): can tay trong, tuc khong cam kiem
                    bolt_acc += dt
                    period = P.MA_CD_TICKS / 20.0 / BOLT_UPTIME
                    while bolt_acc >= period:
                        bolt_acc -= period
                        add(P.UNARMED + (atkv(t_) * P.LB_COEF["MA"][P.stage(B.lv)] * q if art else 0.0), t_)
                        if mana_hit_gain_sec < P.MA_HIT_CAP:
                            mana_hit_gain_sec += 1
                            mana = min(B.resmax, mana + P.MA_HIT_GAIN)
                        ma_stacks += 1
                        dao_tick(t_)
                        if ma_stacks >= P.MA_EVERY:
                            ma_stacks = 0
                            add(pct_dmg(ma_pct), t_, "pass")
            else:
                melee_acc += dt
                period = 1.0 / melee_rate
                while melee_acc >= period:
                    melee_acc -= period
                    if "DAO" in path_set and len(path_set) == 1:
                        dao_tick(t_)  # Dao dem don nhung sat thuong = 0 (DamageRules.resolve)
                        continue
                    ap = atkv(t_)
                    d = (P.SWORD_DMG + ap + ap * q) * cm
                    if art and "MA" in path_set: d += ap * P.LB_COEF["MA"][P.stage(B.lv)] * q
                    add(d * armor_factor(tgt.armor, d * tgt.s), t_)
                    melee_side(t_, d)
        if pdps:
            add(pdps * dt, t_, "pass")
        if t_kill is not None and stop_hp is not None:
            break
    return dict(total=total, dps=total / T, burst5=burst5, heal=heal / T, skill=skill_dmg, casts=casts, ccasts=ccasts, cdmg=cdmg, tkill=t_kill, T=T, mmode=mmode, srcs=srcs)


# ----------------------------------------------------------------------------- Chon kit toi uu
def melee_modes(B):
    ps = set(B.paths)
    if "THE" in ps:
        return ["hand"]            # The Tu: cam vu khi -> 0 sat thuong (DamageRules.resolve), chi dam
    if "MA" in ps:
        return ["sword", "hand"]   # Ma Tu chon: Ban Menh Kiem (sat thuong tu Cong) hoac tay khong + tia ma
    return ["sword"]


def best_kit(P, B, kit_base_list, books, tgt, slots):
    """Tim tap con (<= slots ky nang chu dong + R Ban Menh luon co) va kieu danh tay cho DPS ben vung cao nhat."""
    fixed = [k for k in kit_base_list if k["name"].endswith("(R)")]
    opt = [k for k in kit_base_list if not k["name"].endswith("(R)")]
    cands = opt + books
    best = None
    for r in range(min(slots, len(cands)), 0, -1):
        for combo in itertools.combinations(cands, r):
            kit = list(combo) + fixed
            if not any(k["kind"] in ("cast", "domain") for k in kit):
                continue
            for mm in melee_modes(B):
                st = simulate(P, B, kit, tgt, mmode=mm)
                if best is None or st["dps"] > best[0]["dps"]:
                    best = (st, kit, mm)
        if best is not None and r <= max(1, slots - 1) and len(cands) > slots:
            break  # chi xet kich thuoc slots va slots-1
    return best


def eval_cell(args):
    """Mot o (dao/nguyen to, cap, tu chat) -> mot dict ket qua. Chay trong worker."""
    ov, path, element, lv, apt, core_mode, paths = args
    P = make_params(ov)
    pset = paths if paths else [path]
    a = {p: apt for p in pset}
    B = build(P, lv, pset, a, core_mode, element)
    thi = Target(P, "HuThi", lv)
    boss = Target(P, "BOSS", lv)
    out = dict(path=path, element=element, lv=lv, apt=apt, paths=pset)
    results = []
    for page in pset:
        kit = base_kit(P, B, page, element if page == "PHAP" else None)
        slots = 4 if (page == "PHAP" or lv < P.BM_UNLOCK) else 3
        books = book_candidates(P, B, page) if (page != "PHAP" and not (ov or {}).get("_fast")) else []
        base = best_kit(P, B, kit, [], thi, slots)
        bk = best_kit(P, B, kit, books, thi, slots) if books else base
        results.append((page, kit, base, bk))
    page, kit, base, bk = max(results, key=lambda x: x[2][0]["dps"])
    out["page"] = page
    out["mean_page_dps"] = sum(r[2][0]["dps"] for r in results) / len(results)
    out["dps_base"], out["dps_book"] = base[0]["dps"], bk[0]["dps"]
    out["burst_base"], out["burst_book"] = base[0]["burst5"], bk[0]["burst5"]
    out["kit_base"], out["kit_book"] = [k["name"] for k in base[1]], [k["name"] for k in bk[1]]
    out["mmode"] = base[2]
    # TTK quai thuong (chay den khi chet) va boss
    s = simulate(P, B, base[1], thi, T=120.0, dt=0.05, stop_hp=thi.design, mmode=base[2])
    out["ttk_mob"] = s["tkill"] if s["tkill"] is not None else 9999.0
    s = simulate(P, B, base[1], boss, T=900.0, dt=0.1, stop_hp=boss.design, mmode=base[2])
    out["ttk_boss"] = s["tkill"] if s["tkill"] is not None else 9999.0
    sb = simulate(P, B, bk[1], boss, T=900.0, dt=0.1, stop_hp=boss.design, mmode=bk[2])
    out["ttkb_boss"] = sb["tkill"] if sb["tkill"] is not None else 9999.0
    # so don ky nang (bam phim) de ha quai thuong: HP / sat thuong trung binh moi lan bam (khong tinh xung Kiem Vuc)
    full = simulate(P, B, kit, thi, mmode=base[2])
    avg_cast = full["cdmg"] / full["ccasts"] if full["ccasts"] else 0.0
    out["avg_cast"] = avg_cast
    out["hits_mob"] = math.ceil(thi.design / avg_cast) if avg_cast > 0 else 9999
    tot = max(1e-9, sum(base[0]["srcs"].values()))
    out["src"] = {k: v / tot for k, v in base[0]["srcs"].items()}
    # song sot: quai cung cap danh 1 don/s; bo 3 con; giam sat thuong (The) + hoi (tu mo phong bo day du)
    res_avg = 1.0
    if "THE" in pset:
        for pct, dur, cdt in ((0.2, P.THE2_RES[0] / 20.0, P.skill["THE_2"]["cd"]), (0.4, P.THEULT_RES[0] / 20.0, P.skill["THE_ULT"]["cd"])):
            res_avg *= 1 - pct * min(1.0, dur / (P.sb_cd_ticks(cdt, B.sl) / 20.0))
    healps = full["heal"]
    inc1 = thi.atk * (1 - B.mit) * res_avg
    out["inc"], out["heal"], out["hp"], out["mit"] = inc1, healps, B.hp, B.mit
    for tag, nmob in (("1", 1), ("4", 4)):
        net = inc1 * nmob - healps
        out["ttd" + tag] = min(999.0, B.hp / net) if net > 0 else 999.0
    binc = P.boss["dmg"] * P.boss["dmgmul"] * P.dmg_scale(lv) * (1 - B.mit) * res_avg
    bnet = binc - healps
    out["ttd_boss"] = min(999.0, B.hp / bnet) if bnet > 0 else 999.0
    out["atk"], out["sp"], out["defense"], out["resmax"] = B.atk, B.sp, B.defense, B.resmax
    out["mob_hp"], out["boss_hp"] = thi.design, boss.design
    return out


# ----------------------------------------------------------------------------- Ghi de (what-if)
def make_params(ov):
    P = Params()
    for k, v in (ov or {}).items():
        if not k.startswith("_"):
            apply_override(P, k, v)
    P.art = bool((ov or {}).get("_art"))
    if (ov or {}).get("_lbc"):
        P.LB_COEF = ov["_lbc"]
    return P


def apply_override(P, key, val):
    """key dang 'coef.KIEM_1_COEF' | 'skill.DAO_1.cd' | 'table.THE.0' | 'phap.QUANG.0' | 'book.PHA_KHONG_TRAM.coef' | 'attr.HP_CUBE'."""
    parts = key.split(".")
    if parts[0] == "coef":
        P.coef[parts[1]] = val
    elif parts[0] == "skill":
        P.skill[parts[1]][parts[2]] = val
    elif parts[0] == "table":
        P.table[parts[1]][int(parts[2])] = val
    elif parts[0] == "phap":
        P.phap_coef[parts[1]][int(parts[2])] = val
    elif parts[0] == "book":
        P.book[parts[1]][parts[2]] = val
    elif parts[0] == "bm":
        P.bm[parts[1]][parts[2]] = val
    elif parts[0] == "bmscale":   # nhan ca base, per, cap cua 1 ky nang Ban Menh
        for f in ("base", "per", "cap"):
            P.bm[parts[1]][f] *= val
    elif parts[0] == "phapscale":   # nhan toan bo he so PhapMath.COEF (rieng slot neu co chi so thu 2)
        for e, row in P.phap_coef.items():
            for i in range(4):
                if len(parts) < 3 or int(parts[2]) == i:
                    row[i] = row[i] * val
    elif parts[0] == "phapel":      # nhan he so 1 nguyen to
        P.phap_coef[parts[1]] = [x * val for x in P.phap_coef[parts[1]]]
    elif parts[0] == "boss":
        P.boss[parts[1]] = val
    elif parts[0] == "vitals":
        P.vitals[parts[1]][int(parts[2])] = val
    elif parts[0] == "hpk":          # nhan 4 he so HP quai (HuHoa.HP_*)
        P.HP_BASE *= val; P.HP_LIN *= val; P.HP_QUAD *= val; P.HP_CUBE *= val
    elif parts[0] == "quality":
        P.QUALITY[int(parts[1])] = val
    elif parts[0] == "attr":
        setattr(P, parts[1], val)
    else:
        raise KeyError(key)


# ----------------------------------------------------------------------------- Dai muc tieu (PLAN 1.1)
BAND_DPS = {"KIEM": (100, 100), "MA": (95, 105), "THE": (75, 85), "DAO": (65, 75), "PHAP": (85, 95)}  # Phap don muc tieu ~90
BAND_TANK = {"KIEM": (1.0, 1.0), "MA": (1.0, 1.25), "THE": (1.5, 2.1), "DAO": (0.85, 1.15), "PHAP": (0.75, 1.05)}
BAND_HITS = (4, 8)
BAND_BOSS = (90, 180)


def run_grid(ov, core_mode="avg", multi=False, workers=None, levels=None, apts=None, singles=True):
    jobs = []
    for lv in (levels or LEVELS) if singles else []:
        for apt in (apts or APTS):
            for p in PATHS4:
                jobs.append((ov, p, None, lv, apt, core_mode, None))
            for e in ELEMENTS:
                jobs.append((ov, "PHAP", e, lv, apt, core_mode, None))
    if multi:
        for lv in (levels or LEVELS):
            for n in (2, 3):
                for combo in itertools.combinations(PATHS4 + ["PHAP"], n):
                    el = "QUANG" if "PHAP" in combo else None
                    jobs.append((ov, "+".join(combo), el, lv, "B", core_mode, list(combo)))
    with ProcessPoolExecutor(max_workers=workers) as ex:
        res = list(ex.map(eval_cell, jobs, chunksize=2))
    return res


# ============================================================================= BAO CAO
PROPOSAL_VALUES = {}   # khoa_ghi_de -> gia_tri_moi (dien sau khi toi uu)


def k_of(r):
    return r["path"] if r["path"] != "PHAP" else "P-" + r["element"]


def index(res):
    return {(k_of(r), r["apt"], r["lv"]): r for r in res}


NAMES = {"KIEM": "Kiếm", "MA": "Ma", "THE": "Thể", "DAO": "Đạo"}
COLS = ["KIEM", "MA", "THE", "DAO"] + ["P-" + e for e in ELEMENTS]
COLN = ["Kiếm", "Ma", "Thể", "Đạo"] + ["Pháp-" + e.title() for e in ELEMENTS]


def band_of(name):
    return "PHAP" if name.startswith("P-") else name


def flag(x, lo, hi, fmt="{:.0f}"):
    s = fmt.format(x)
    return f"**{s}!**" if (x < lo or x > hi) else s


def table_md(head, rows):
    out = ["| " + " | ".join(head) + " |", "|" + "|".join(["---"] * len(head)) + "|"]
    out += ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    return "\n".join(out)


def rel_dps_table(ix, apt, lvls):
    rows, bad = [], 0
    for lv in lvls:
        k = ix[("KIEM", apt, lv)]["dps_base"]
        row = [lv, f"{k:.0f}"]
        for nm in COLS[1:]:
            r = ix[(nm, apt, lv)]
            x = 100.0 * r["dps_base"] / k
            lo, hi = BAND_DPS[band_of(nm)]
            bad += x < lo or x > hi
            row.append(f"{flag(x, lo, hi)} ({r['dps_base']:.0f})")
        rows.append(row)
    return table_md(["Cấp", "Kiếm DPS (=100)"] + COLN[1:], rows), bad


def metric_table(ix, apt, lvls, key, lo, hi, fmt="{:.0f}", rel=False, bands=None):
    rows, bad = [], 0
    for lv in lvls:
        row = [lv]
        kref = ix[("KIEM", apt, lv)][key]
        for nm in COLS:
            r = ix[(nm, apt, lv)]
            x = r[key] / max(1e-9, kref) if rel else r[key]
            l, h = (bands[band_of(nm)] if bands else (lo, hi))
            if rel and nm == "KIEM":
                row.append("1.00")
                continue
            bad += x < l or x > h
            row.append(flag(x, l, h, fmt))
        rows.append(row)
    return table_md(["Cấp"] + COLN, rows), bad


def qonly_table(P):
    """Tai hien so 'sat thuong/giay ky nang 1' trong PLAN 1.2 (khong Lõi, khong Cuong Hoa, khong bao kich, bo qua mana) de doi chieu."""
    rows = []
    for lv in (1, 30, 60, 100):
        row = [lv]
        vals = {}
        for p in ("KIEM", "DAO", "THE", "MA"):
            B = build(P, lv, [p], {p: "B"}, "none")
            sk = {"KIEM": ("KIEM_1", P.coef["KIEM_1_COEF"], B.atk, 1.0), "DAO": ("DAO_1", P.coef["DAO_1_COEF"], B.sp, P.DAO_SKILL_MULT),
                  "THE": ("THE_1", P.coef["THE_1_COEF"], B.atk, 1.0), "MA": ("MA_1", P.coef["MA_1_COEF"], B.atk, 1.0)}[p]
            vals[p] = sk[1] * sk[2] * sk[3] / (P.skill[sk[0]]["cd"] / 20.0)
        base = vals["KIEM"]
        rows.append([lv] + [f"{vals[p]:.0f} ({vals[p]/base:.1f}x)" for p in ("KIEM", "DAO", "THE", "MA")])
    return table_md(["Cấp", "Kiếm Q", "Đạo Q", "Thể Q", "Ma Q"], rows)


def srcs_table(ix, apt, lvls):
    rows = []
    for lv in lvls:
        row = [lv]
        for nm in COLS[:4] + ["P-QUANG"]:
            r = ix[(nm, apt, lv)]
            sr = r["src"]
            row.append(f"{100*sr['melee']:.0f}/{100*sr['skill']:.0f}/{100*sr['pass']:.0f} ({r['mmode']})")
        rows.append(row)
    return table_md(["Cấp", "Kiếm", "Ma", "Thể", "Đạo", "Pháp-Quang"], rows)


def mob_table(P, lvls, core_mode):
    rows = []
    for lv in lvls:
        for pth, el in (("KIEM", None), ("THE", None)):
            B = build(P, lv, [pth], {pth: "B"}, core_mode, el)
            kit = base_kit(P, B, pth, el)
            row = [lv, NAMES[pth]]
            for mn in list(P.mobs):
                tg = Target(P, mn, lv)
                slots = 3 if lv >= P.BM_UNLOCK else 4
                best = best_kit(P, B, kit, [], tg, slots)
                s2 = simulate(P, B, best[1], tg, T=120.0, stop_hp=tg.design, mmode=best[2])
                row.append(f"{tg.design:.0f} / {s2['tkill'] if s2['tkill'] is not None else 999:.1f}s")
            rows.append(row)
    return table_md(["Cấp", "Đạo"] + [f"{m} (HP/TTK)" for m in P.mobs], rows)


def single_ref(ix, c, lv):
    return ix[(("P-QUANG" if c == "PHAP" else c), "B", lv)]


def multi_table(ix, mres, lvls):
    rows, over = [], 0
    for lv in lvls:
        worst = None
        cnt = tot = 0
        for r in mres:
            if r["lv"] != lv:
                continue
            ref = max(single_ref(ix, c, lv)["dps_base"] for c in r["paths"])
            ratio = r["dps_base"] / ref
            tot += 1
            cnt += ratio > 1.0
            if worst is None or ratio > worst[0]:
                worst = (ratio, r["path"], r["page"], r["mean_page_dps"] / ref)
        over += cnt
        rows.append([lv, f"{cnt}/{tot}", f"{worst[0]:.2f}", worst[1], worst[2], f"{worst[3]:.2f}"])
    return table_md(["Cấp", "Tổ hợp mạnh hơn đơn tu tốt nhất", "Tỉ lệ cao nhất (đa/đơn)", "Tổ hợp", "Trang dùng", "TB các trang/đơn"], rows), over


def multi_detail(ix, mres, lv):
    rows = []
    for r in sorted((x for x in mres if x["lv"] == lv), key=lambda x: x["path"]):
        ref = max(single_ref(ix, c, lv)["dps_base"] for c in r["paths"])
        t4 = max(single_ref(ix, c, lv)["ttd4"] for c in r["paths"])
        rows.append([r["path"], f"{r['dps_base']:.0f}", f"{ref:.0f}", f"{r['dps_base']/ref:.2f}", r["page"], f"{r['hp']:.0f}",
                     f"{r['ttd4']:.1f}", f"{t4:.1f}"])
    return table_md(["Tổ hợp", "DPS đa đạo", "DPS đơn tốt nhất", "Tỉ lệ", "Trang", "HP", "Sống sót (4 quái)", "Đơn tu tốt nhất"], rows)


def source_table(P):
    j = "stats/StatService.java"
    rows = [
        ("table()/vitals()", j, r"private static double\[\] table", "Công/Phép/Phòng/Bạo/Hút/Tài nguyên + Sinh lực theo cấp"),
        ("aptFactor", j, r"public static double aptFactor", "tư chất ×0.90..1.20"),
        ("Phân Tâm", j, r"double off = n >= 3", "song ×0.8/0.9, tam ×0.7/0.85"),
        ("mitigate (Phòng)", j, r"public static float mitigate", "def/(def+100), trần 80%"),
        ("Thần (cấp 100)", j, r"THAN_BONUS =", "×1.5 mọi attribute mod (refresh)"),
        ("Skill cost/cd", "skill/Skill.java", r"KIEM_1\(Path", "tài nguyên + hồi chiêu (tick)"),
        ("*_COEF", "skill/Skill.java", r"DAO_1_COEF =", "hệ số Q của Đạo/Thể/Kiếm/Ma"),
        ("Kiếm Vực", "skill/SkillEffects.java", r"DOMAIN_DRAIN_PCT =", "10% max/s, xung 0,5s × 0.55 Công"),
        ("Liên Hoàn Trảm 3 nhát", "skill/SkillEffects.java", r"private static void lienHoanTram", "vòng for 3 nhát"),
        ("Ma Thần Giáng Thế +40%", "pathrules/MaRules.java", r"thanTicks > 0 \? 0\.40", "12 s"),
        ("Bản Mệnh R", "skill/BanMenhSkill.java", r"NHAT_KIEM\(", "coef = min(cap, base + per × (cấp-40))"),
        ("Phán Xét ×2 vs HuMob", "skill/BanMenhSkillEffects.java", r"t instanceof HuMob", "× 2.0"),
        ("Bản Mệnh Kiếm (đòn thường)", "banmenh/BanMenhEvents.java", r"QUALITY\[st\]", "e.getAmount() + Công × QUALITY[giai đoạn]"),
        ("QUALITY", "banmenh/BanMenhService.java", r"QUALITY =", "{1.0 .. 3.0}"),
        ("Kiếm gốc vanilla", "banmenh/BanMenhKiemItem.java", r"createAttributes\(Tiers\.IRON", "1 + 2 + 2 (IRON) = 5, tốc 1.6/s"),
        ("Đòn thường: +Công nếu có vũ khí", "gates/DamageRules.java", r"out \+= \(float\) com\.hukhong\.stats\.StatService\.attackPower", "Kiếm/Ma/Pháp"),
        ("Thể: nắm đấm", "gates/DamageRules.java", r"FIST_AURA_COEF =", "Công × (1 + 1.8 × Bá Khí%)"),
        ("Bạo kích áp cả kỹ năng", "gates/DamageRules.java", r"double cc =", "ngoài khối !skillActive"),
        ("Nội tại (hằng số)", "passive/PassiveMath.java", r"KIEM_SWEEP_MAX", "Kiếm/Đạo/Thể/Ma"),
        ("Nội tại (logic)", "passive/PassiveService.java", r"private static void kiemHit", "mỗi 4 đòn / 5 đòn / 5 tầng"),
        ("Hồi tài nguyên", "pathrules/PathRules.java", r"DAO_REGEN =", "Kiếm 2,25/s; Đạo 5/s; Thể 2/s; Pháp 5/s; Ma +1/đòn"),
        ("Cường Hóa kỹ năng", "skillbook/SkillBookMath.java", r"DMG_PER_LEVEL =", "+8% dmg, −3% CD/cấp; typicalLevel"),
        ("BookSkill coef/cost/cd", "skillbook/BookSkill.java", r"PHA_KHONG_TRAM\(", "19 kỹ năng sách"),
        ("PhapMath COEF", "phaptu/PhapMath.java", r"private static final double\[\]\[\] COEF", "tổng hệ số mỗi phím"),
        ("PhapMath passiveDps", "phaptu/PhapMath.java", r"public static double passiveDps", "Hỏa/Thổ/Lôi/Thủy"),
        ("HP quái", "entity/HuHoa.java", r"HP_BASE = ", "hpScale + THAN_HP_MUL"),
        ("Sát thương quái", "entity/HuHoa.java", r"public static double dmgScale", "1 + 0.10g + 0.0017g²"),
        ("ehpScale / VANILLA_HP_CAP", "entity/HuHoa.java", r"public static float ehpScale", "HP thiết kế > 1024 → giảm sát thương nhận"),
        ("Hệ số quái", "entity/HuThi.java", r"applyStats", "HuThi 1.45/1.0; các quái khác tương tự"),
        ("WorldBoss", "entity/WorldBoss.java", r"MAX_HEALTH, 140", "140 HP × hpMul 2.0 × hpScale"),
        ("Lõi (CoreRoller)", "cores/CoreRoller.java", r"BASE = ", "phân bố phẩm theo mốc"),
    ]
    out = [(a, f, cite(f, rx).split(":")[-1], d) for a, f, rx, d in rows]
    out.append(("Lõi (JSON)", "data/hukhong/hukhong/core/*.json", "-", f"{len(P.cores)} file; passives attack_power/spell_power/defense"))
    return table_md(["Hạng mục", "File", "Dòng", "Dùng cho"], out)


def summary_findings(P, ix, apt="B"):
    """Sinh danh sách phát hiện từ số liệu (để không lệch khi chạy lại)."""
    out = []

    def g(nm, lv, k):
        return ix[(nm, apt, lv)][k]
    k100, k80 = g("KIEM", 100, "dps_base"), g("KIEM", 80, "dps_base")
    out.append(f"Kiếm nhảy DPS {k100/k80:.1f}× từ cấp 80 lên 100 ({k80:.0f} → {k100:.0f}); Ma {g('MA',100,'dps_base')/g('MA',80,'dps_base'):.1f}×, Thể {g('THE',100,'dps_base')/g('THE',80,'dps_base'):.1f}×, Đạo {g('DAO',100,'dps_base')/g('DAO',80,'dps_base'):.1f}×. "
               f"Ở cấp 100 so với Kiếm: Ma {100*g('MA',100,'dps_base')/k100:.0f}, Thể {100*g('THE',100,'dps_base')/k100:.0f}, Đạo {100*g('DAO',100,'dps_base')/k100:.0f}, Pháp-Quang {100*g('P-QUANG',100,'dps_base')/k100:.0f}.")
    out.append(f"Số đòn kỹ năng để hạ quái cùng cấp (HuThi, cấp 60): Kiếm {g('KIEM',60,'hits_mob')}, Ma {g('MA',60,'hits_mob')}, Đạo {g('DAO',60,'hits_mob')}, Pháp-Quang {g('P-QUANG',60,'hits_mob')}, Thể {g('THE',60,'hits_mob')} (dải 4-8).")
    out.append(f"Boss thế giới cùng cấp 60: Kiếm {g('KIEM',60,'ttk_boss'):.0f}s, Ma {g('MA',60,'ttk_boss'):.0f}s, Thể {g('THE',60,'ttk_boss'):.0f}s, Đạo {g('DAO',60,'ttk_boss'):.0f}s, Pháp-Quang {g('P-QUANG',60,'ttk_boss'):.0f}s (dải 90-180s).")
    out.append(f"Độ trâu cấp 60 (sống sót trước 4 quái, Kiếm = 1.0): Ma {g('MA',60,'ttd4')/g('KIEM',60,'ttd4'):.1f}, Thể {g('THE',60,'ttd4')/g('KIEM',60,'ttd4'):.1f}, Đạo {g('DAO',60,'ttd4')/g('KIEM',60,'ttd4'):.1f}, Pháp {g('P-QUANG',60,'ttd4')/g('KIEM',60,'ttd4'):.1f}.")
    return out


def main(quick=False):
    import time
    t0 = time.time()
    P = Params()
    print("Đang mô phỏng baseline...", flush=True)
    base = run_grid({}, "avg")
    ix = index(base)
    mres = [r for r in run_grid({}, "avg", multi=True, singles=False) if len(r["paths"]) > 1]
    sens = {}
    for mode in ("none", "focus"):
        sens[mode] = index(run_grid({"_fast": 1}, mode, apts=["B"]))
    print(f"baseline xong ({time.time()-t0:.0f}s)", flush=True)
    after = after_multi = None
    if PROPOSAL_VALUES and not quick:
        ov = dict(PROPOSAL_VALUES)
        after = index(run_grid(ov, "avg"))
        after_multi = [r for r in run_grid(ov, "avg", multi=True, singles=False) if len(r["paths"]) > 1]
    if quick:
        for lv in LEVELS:
            print(lv, {nm: round(ix[(nm, 'B', lv)]['dps_base']) for nm in COLS})
        return
    REPORT.write_text(render(P, ix, mres, sens, after, after_multi), encoding="utf-8", newline="\n")
    print(f"Đã ghi {REPORT}", flush=True)


def render(P, ix, mres, sens, after, after_multi):
    L = []
    w = L.append
    w("# REPORT-v1: Cân bằng chỉ số 5 đạo — đo baseline (PLAN-v1 S1/S2)")
    w("")
    w("**Tạo**: 2026-10-29 (sinh tự động bởi `tools/balance_sim.py`; chạy lại: `python tools/balance_sim.py`)")
    w("**Phạm vi**: chỉ phân tích. Không sửa Java. Mọi đề xuất ở §10 CHƯA áp dụng.")
    w("**Quy ước ô**: số **in đậm có dấu `!`** = nằm ngoài dải mục tiêu của PLAN §1.1. Tư chất B trừ khi ghi khác; Lõi trung bình (§1.2); quái = HuThi cùng cấp.")
    w("")
    w("## 0. Tóm tắt (kết luận trước)")
    w("")
    for f in summary_findings(P, ix, "B"):
        w(f"- {f}")
    w("")
    w("Chi tiết các lệch cấu trúc nằm ở §9. Bảng đề xuất hệ số và số liệu chạy lại nằm ở §10.")
    w("")
    w("## 1. Phương pháp và giả định")
    w("")
    w("### 1.1 Mô hình")
    w("")
    w("- Mô phỏng theo bước 0,05s trong 60s. Mỗi tick: hồi tài nguyên, thả kỹ năng theo thứ tự ưu tiên (sát thương lớn trước) khi hết hồi chiêu và đủ tài nguyên, rồi đòn thường.")
    w("- **Công thức được port từ Java** (không đọc runtime): StatService.compute, mitigate, Thần ×1.5, PhapMath, PassiveMath, Cường Hóa kỹ năng. Mọi **hằng số** đọc trực tiếp bằng regex (nguồn ở §2). Đổi hằng số trong Java thì chạy lại là số đổi theo; đổi *logic* thì phải sửa phần port.")
    w("- Bộ kỹ năng: \"kit tối ưu\" thử mọi tập con ≤ số phím (3 phím + R Bản Mệnh khi ≥ cấp 40, 4 phím trước đó) và chọn DPS bền vững cao nhất. Cột \"+sách\" cho phép thêm kỹ năng sách đã mở theo phẩm (BookSkill.java).")
    w(f"- Cường Hóa kỹ năng ở mức \"điển hình\" (`SkillBookMath.typicalLevel`): cấp tu {', '.join(str(l) for l in LEVELS)} → cấp kỹ năng {', '.join(str(P.sb_typical(l)) for l in LEVELS)}.")
    w(f"- Hiệu suất thao tác kỹ năng {SKILL_EFF:.2f}; thời gian vung kiếm/đấm chiếm {MELEE_UPTIME:.2f}; tia ma tay không {BOLT_UPTIME:.2f} (cùng giả định với `BalanceSimTest` cũ).")
    w("- Đòn thường có vũ khí = `(kiếm vanilla 5) + Công + Công × QUALITY[giai đoạn Bản Mệnh Kiếm]`, nhân bạo kích trung bình, qua giáp quái vanilla. Giai đoạn kiếm = tối đa theo cấp (người chơi đã tiến hóa hết). Thể: quyền Công×(1+1.8×Bá Khí%) theo Bá Khí mô phỏng từng tick. Đạo: đòn thường = 0 với quái.")
    w("- Kỹ năng xuyên giáp (indirectMagic, giả định vanilla `bypasses_armor`); sát thương kỹ năng cũng bạo kích và hút máu (DamageRules ngoài khối `!skillActive`).")
    w("- HP thiết kế của quái vượt 1024 bị quy thành giảm sát thương nhận (`HuHoa.ehpScale`); sát thương % máu tối đa (nội tại Đạo/Ma) tính trên HP thật ≤ 1024, boss chia 1/3.")
    w("")
    w("### 1.2 Lõi trung bình (\"avg\")")
    w("")
    w("Mỗi mốc cấp (10, 20, ..., 90, 95, 99) người chơi nhận 1 Lõi. Giả định: Lõi được chọn đều trong tập Lõi của đạo mình + Lõi `any` (JSON `core/*.json`), phẩm kỳ vọng theo `CoreRoller.BASE` (Vận = 0). Giá trị `add_value` cộng thẳng vào Công/Phép/Phòng, `add_multiplied_base` nhân cơ sở. Hai kịch bản nhạy cảm: **none** (không Lõi) và **focus** (mốc lẻ luôn lấy Lõi tấn công tốt nhất, mốc chẵn lấy Lõi Phòng tốt nhất) — xem §8.1. Pháp Tu không có Lõi riêng (không có file `path: phap`), chỉ nhận Lõi `any` có `spell_power`.")
    w("")
    w("### 1.3 Cố tình không mô hình hóa")
    w("")
    for t in (
        "Hồn Cốt, Pháp Bảo, trang bị, loại Bản Mệnh Kiếm (Thanh Phong, Liệt Diễm, ...) và bị động Ngọc Tâm/Trọng Nhạc: chưa đọc được dạng số gọn từ mã. Ước lượng định tính: làm tăng DPS đều, không đổi nhiều tỉ lệ giữa các đạo.",
        "Sát thương diện rộng (AoE) và đám đông: PLAN §1.1 yêu cầu Pháp = 140 khi đám đông. Mô phỏng này chỉ có 1 mục tiêu; **cột đám đông của Pháp chưa đo được**.",
        "Kỹ năng quái (`HuSkill`), AI, né tránh, khoảng cách: quái chỉ cận chiến 1 đòn/giây. Thời gian sống sót vì thế là cận trên cho giao tranh thật.",
        "Buff/hồi cho đồng đội (Ban Phúc, Thánh Giáng, Thanh Tâm Chú): vai trò hỗ trợ của Đạo không đo ở đây; chỉ phần tự hồi được tính.",
        "Kiếm Vực: mưa kiếm (8 thanh) không gây sát thương trong mã đọc được (chỉ hạt), nên chỉ tính nhịp 0.55 Công.",
        "Thứ tự xử lý `LivingIncomingDamageEvent` giữa `PathRules.onIncoming` và `BanMenhEvents.onDamage` (cùng mức ưu tiên) không xác định từ mã: nếu kiếm Bản Mệnh cộng sau khi Đạo bị gán 0, Đạo cầm kiếm sẽ có đòn thường = Công × QUALITY (khác 0). Mô phỏng lấy 0 theo ý đồ thiết kế (DamageRules.java:77).",
        "Tốc độ đấm của Thể: `fistDamage` không phụ thuộc thanh hồi đòn nên có thể bấm nhanh hơn 1,6/s (chỉ còn khung bất tử 10 tick). Mô phỏng dùng 1,6/s; thực tế có thể cao hơn, chưa đo.",
        "Kiếp Số (`scaling.Difficulty`) và `WorldTier` lấy bằng 1.0.",
    ):
        w(f"- {t}")
    w("")
    w("## 2. Nguồn hằng số (đọc bằng regex lúc chạy)")
    w("")
    w(source_table(P))
    w("")
    w("## 3. Chỉ số cơ bản (đã gồm tư chất B, Lõi trung bình, Thần ở cấp 100)")
    w("")
    rows = []
    for lv in LEVELS:
        for nm in ("KIEM", "MA", "THE", "DAO", "P-QUANG"):
            r = ix[(nm, "B", lv)]
            rows.append([lv, NAMES.get(nm, "Pháp"), f"{r['atk']:.0f}", f"{r['sp']:.0f}", f"{r['defense']:.0f}", f"{100*r['mit']:.0f}%", f"{r['hp']:.0f}", r["resmax"]])
    w(table_md(["Cấp", "Đạo", "Công", "Phép", "Phòng", "Giảm", "HP", "Tài nguyên"], rows))
    w("")
    w("## 4. Sát thương bền vững (DPS 60s, đơn mục tiêu, so với Kiếm = 100)")
    w("")
    bad_tot = 0
    for apt in APTS:
        t, bad = rel_dps_table(ix, apt, LEVELS)
        bad_tot += bad
        w(f"### Tư chất {apt}: tỉ lệ % so Kiếm (trong ngoặc là DPS tuyệt đối)")
        w("")
        w(t)
        w("")
    w("Dải mục tiêu: Ma 95-105; Thể 75-85; Đạo 65-75; Pháp 85-95 (đơn mục tiêu; PLAN ghi một con số 90 nên lấy ±5).")
    w("")
    w("### Đối chiếu PLAN §1.2: sát thương/giây của kỹ năng 1 (Q) thô — không Lõi, không Cường Hóa, không bạo kích, bỏ qua mana")
    w("")
    w(qonly_table(P))
    w("")
    w("Các số 82 / 223 / 116 / 25 của PLAN ở cấp 60 được tái hiện đúng (Kiếm / Đạo / Ma / Thể). Giả thuyết (a) \"Đạo quá mạnh\" **chỉ đúng khi nhìn riêng kỹ năng Q**; khi tính cả đòn thường (Kiếm đánh kiếm, Đạo = 0), nội tại và Bản Mệnh R, Đạo chỉ đạt ~70% Kiếm ở cấp 60 (bảng trên). Giả thuyết (b) \"Thể quá yếu khi dùng kỹ năng\" đúng nhưng Thể lấy DPS từ nắm đấm (≥ 80% DPS, §4 nguồn sát thương), nên tổng DPS Thể gần dải hơn.")
    w("")
    w("### Nguồn sát thương (% đòn thường / kỹ năng / nội tại; kiểu đánh tay) — tư chất B")
    w("")
    w(srcs_table(ix, "B", LEVELS))
    w("")
    w("### DPS khi kit tối ưu có thêm kỹ năng sách (tư chất B)")
    w("")
    rows = []
    for lv in LEVELS:
        row = [lv]
        for nm in COLS[:4]:
            r = ix[(nm, "B", lv)]
            row.append(f"{r['dps_book']:.0f} ({100*r['dps_book']/r['dps_base']-100:+.0f}%)")
        rows.append(row)
    w(table_md(["Cấp", "Kiếm", "Ma", "Thể", "Đạo"], rows))
    w("")
    w("Kỹ năng sách chỉ thêm vài phần trăm so với kit mặc định vì giới hạn 3 phím + Bản Mệnh R và hồi chiêu dài của sách phẩm cao. Pháp Tu không có kỹ năng sách (`BookSkill` không có `Path.PHAP`).")
    w("")
    w("## 5. Burst 5 giây đầu (mọi kỹ năng sẵn sàng, tài nguyên đầy), tư chất B")
    w("")
    rows = []
    for lv in LEVELS:
        k = ix[("KIEM", "B", lv)]["burst_base"]
        rows.append([lv] + [f"{ix[(nm, 'B', lv)]['burst_base']:.0f} ({100*ix[(nm, 'B', lv)]['burst_base']/k:.0f}%)" for nm in COLS])
    w(table_md(["Cấp"] + COLN, rows))
    w("")
    w("## 6. Hạ quái thường cùng cấp")
    w("")
    w("### 6.1 Số đòn kỹ năng để hạ HuThi (HP quái / sát thương trung bình một lần bấm phím, theo tần suất của rotation, không tính xung Kiếm Vực). Dải 4-8")
    w("")
    for apt in APTS:
        t, bad = metric_table(ix, apt, LEVELS, "hits_mob", *BAND_HITS)
        bad_tot += bad
        w(f"Tư chất {apt}:")
        w("")
        w(t)
        w("")
    w("### 6.2 Thời gian hạ HuThi cùng cấp (giây), toàn bộ rotation, tư chất B")
    w("")
    rows = [[lv] + [f"{ix[(nm, 'B', lv)]['ttk_mob']:.1f}" for nm in COLS] for lv in LEVELS]
    w(table_md(["Cấp"] + COLN, rows))
    w("")
    w("0,0s = chuỗi kỹ năng đầu tiên (mọi kỹ năng đang sẵn sàng) đã đủ hạ quái ngay (one-shot rotation).")
    w("")
    w("### 6.3 HP và TTK của 7 loại quái (Kiếm và Thể, tư chất B)")
    w("")
    w(mob_table(P, [10, 60, 100], "avg"))
    w("")
    w("## 7. Boss thế giới cùng cấp (một người chơi)")
    w("")
    w(f"WorldBoss: HP gốc {P.boss['hp']:.0f} × hpMul {P.boss['hpmul']} × hpScale(cấp); giáp {P.boss['armor']:.0f} + 0,25/cấp (tối đa 20). Dải 90-180s.")
    w("")
    for apt in APTS:
        t, bad = metric_table(ix, apt, LEVELS, "ttk_boss", *BAND_BOSS)
        bad_tot += bad
        w(f"Tư chất {apt} — thời gian hạ boss (s), kit mặc định:")
        w("")
        w(t)
        w("")
    w("Tư chất B — thời gian sống sót trước boss (s; đòn 1/s; đã trừ hồi):")
    w("")
    rows = [[lv] + [f"{ix[(nm, 'B', lv)]['ttd_boss']:.0f}" for nm in COLS] for lv in LEVELS]
    w(table_md(["Cấp"] + COLN, rows))
    w("")
    w("## 8. Thời gian sống sót (độ trâu)")
    w("")
    w("Mô hình: 4 quái HuThi cùng cấp, mỗi con 1 đòn/giây, trừ Phòng, trừ giảm sát thương của Thể (Thiết Bố Sam, Kim Thân theo thời lượng/hồi chiêu), trừ hồi của rotation đầy đủ (hút máu, nội tại Đạo/Kiếm, Hộ Thể của Ma, Thánh Giáng). Chỉ số = thời gian sống của đạo / thời gian sống của Kiếm.")
    w("")
    for apt in APTS:
        t, bad = metric_table(ix, apt, LEVELS, "ttd4", 0, 0, "{:.2f}", rel=True, bands=BAND_TANK)
        bad_tot += bad
        w(f"Tư chất {apt} — độ trâu tương đối (Kiếm = 1.00; dải: Ma 1.0-1.25, Thể 1.5-2.1, Đạo 0.85-1.15, Pháp 0.75-1.05):")
        w("")
        w(t)
        w("")
    w("Thời gian sống tuyệt đối (s), tư chất B:")
    w("")
    rows = [[lv] + [f"{ix[(nm, 'B', lv)]['ttd4']:.1f}" for nm in COLS] for lv in LEVELS]
    w(table_md(["Cấp"] + COLN, rows))
    w("")
    w("### 8.1 Nhạy cảm theo Lõi (tư chất B, DPS % so Kiếm)")
    w("")
    rows = []
    for mode, ixm in (("avg", ix), ("none", sens["none"]), ("focus", sens["focus"])):
        for lv in (10, 60, 100):
            k = ixm[("KIEM", "B", lv)]["dps_base"]
            rows.append([mode, lv, f"{k:.0f}"] + [f"{100*ixm[(nm, 'B', lv)]['dps_base']/k:.0f}" for nm in ("MA", "THE", "DAO", "P-QUANG")])
    w(table_md(["Lõi", "Cấp", "Kiếm DPS", "Ma", "Thể", "Đạo", "Pháp-Quang"], rows))
    w("")
    w("### 8.2 Đa đạo (S7): song tu và tam tu có mạnh hơn đơn tu không? (tư chất B)")
    w("")
    t, over = multi_table(ix, mres, LEVELS)
    w(t)
    w("")
    w("Chi tiết cấp 60:")
    w("")
    w(multi_detail(ix, mres, 60))
    w("")
    if over == 0:
        w("**Không tổ hợp nào vượt đơn tu về DPS bền vững**: Phân Tâm (×0.8/×0.7) làm sát thương thấp hơn.")
    else:
        w(f"**{over} ô tổ hợp/cấp có DPS lớn hơn đơn tu tốt nhất** (tỉ lệ > 1.00). Phân Tâm giảm Công/Phép/Hút 20-30% nhưng không giảm nội tại (hồi máu Đạo, đòn thứ 4 của Kiếm, quyền Thể dựa vào Công cao nhất trong các đạo), và tài nguyên các đạo cộng dồn.")
    w("")
    w(f"## 9. Phát hiện cấu trúc ({len(STRUCT)} mục, có dẫn chứng)")
    w("")
    for i, (title, body) in enumerate(STRUCT, 1):
        w(f"### 9.{i} {title}")
        w("")
        w(body(P, ix) if callable(body) else body)
        w("")
    w("## 10. Đề xuất hệ số (CHƯA áp dụng) và số liệu chạy lại")
    w("")
    if not after:
        w("(chưa có đề xuất)")
    else:
        w("Các thay đổi dưới đây được chạy lại bằng chính mô phỏng này (ghi đè hằng số trong bộ nhớ, không đụng file Java).")
        w("")
        rows = proposal_rows(P, PROPOSAL_VALUES)
        w(table_md(["File:dòng", "Thay đổi", "Cũ", "Mới", "Lý do"], rows))
        w("")
        bad_after = 0
        w("### 10.1 Sau đề xuất — DPS % so Kiếm")
        w("")
        for apt in APTS:
            t, bad = rel_dps_table(after, apt, LEVELS)
            bad_after += bad
            w(f"Tư chất {apt}:")
            w("")
            w(t)
            w("")
        w("### 10.2 Sau đề xuất — số đòn kỹ năng hạ quái (dải 4-8)")
        w("")
        for apt in APTS:
            t, bad = metric_table(after, apt, LEVELS, "hits_mob", *BAND_HITS)
            bad_after += bad
            w(f"Tư chất {apt}:")
            w("")
            w(t)
            w("")
        w("### 10.3 Sau đề xuất — boss thế giới (s, dải 90-180)")
        w("")
        for apt in APTS:
            t, bad = metric_table(after, apt, LEVELS, "ttk_boss", *BAND_BOSS)
            bad_after += bad
            w(f"Tư chất {apt}:")
            w("")
            w(t)
            w("")
        w("### 10.4 Sau đề xuất — độ trâu tương đối")
        w("")
        for apt in APTS:
            t, bad = metric_table(after, apt, LEVELS, "ttd4", 0, 0, "{:.2f}", rel=True, bands=BAND_TANK)
            bad_after += bad
            w(f"Tư chất {apt}:")
            w("")
            w(t)
            w("")
        w("### 10.5 Sau đề xuất — thời gian hạ HuThi (s), tư chất B")
        w("")
        rows = [[lv] + [f"{after[(nm, 'B', lv)]['ttk_mob']:.1f}" for nm in COLS] for lv in LEVELS]
        w(table_md(["Cấp"] + COLN, rows))
        w("")
        t2, over2 = multi_table(after, after_multi, LEVELS)
        w("### 10.6 Sau đề xuất — đa đạo")
        w("")
        w(t2)
        w("")
        w(f"**Tổng số ô ngoài dải (DPS, đòn kỹ năng, boss, độ trâu; hai tư chất): trước {bad_tot}, sau {bad_after}.** Đa đạo: {over2} ô vượt đơn tu sau đề xuất (trước: {over}).")
        w("")
    w("## Ghi chú về độ tin cậy")
    w("")
    w("- Đây là mô hình, không phải log thật. `CONFIG-BASED`: hằng số đọc từ Java (file:dòng ở §2). `MODEL-BASED`: rotation, uptime, quái 1 đòn/s, Lõi trung bình.")
    w("- RULE-v1-agent-anti-patterns AP4: mới là mô phỏng, chưa có xác nhận bằng GameTest hoặc chơi thật. Đề xuất ở §10 cần đi qua `BalanceBandTest` (S6) và một lượt chơi thử 3 đạo ở cấp 20-30 trước khi áp dụng.")
    w("- Giả định (hiệu suất 0.85, uptime vung kiếm 0.55, kiếm Bản Mệnh đã tiến hóa tối đa) làm dịch cả bảng nhưng ít đổi thứ hạng giữa các đạo.")
    return "\n".join(L) + "\n"


def _old_value(P, key):
    p = key.split(".")
    if p[0] == "coef": return P.coef[p[1]]
    if p[0] == "table": return P.table[p[1]][int(p[2])]
    if p[0] == "bm": return P.bm[p[1]][p[2]]
    if p[0] == "attr": return getattr(P, p[1])
    if p[0] == "quality": return P.QUALITY[int(p[1])]
    if p[0] == "boss": return P.boss[p[1]]
    if p[0] == "vitals": return P.vitals[p[1]][int(p[2])]
    if p[0] == "skill": return P.skill[p[1]][p[2]]
    if p[0] == "book": return P.book[p[1]][p[2]]
    if p[0] == "phap": return P.phap_coef[p[1]][int(p[2])]
    return 1.0


def _fmt(x):
    if isinstance(x, bool):
        return "có" if x else "không"
    return f"{x:g}" if isinstance(x, (int, float)) else str(x)


def cite_abs(rel_from_root, pattern):
    p = ROOT / rel_from_root
    for i, ln in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(pattern, ln):
            return f"{rel_from_root.replace('src/main/java/com/hukhong/', '')}:{i}"
    raise KeyError(rel_from_root + pattern)


TEST = "src/test/java/com/hukhong/balance/BalanceSimTest.java"


def _s_quality(P, ix):
    rows = []
    for lv in (10, 30, 60, 80, 100):
        B = build(P, lv, ["KIEM"], {"KIEM": "B"}, "avg")
        q = P.QUALITY[P.stage(lv)]
        sw = P.SWORD_DMG + B.atk + B.atk * q
        rows.append([lv, f"{q:.2f}", f"{B.atk:.0f}", f"{sw:.0f}", f"{sw/B.atk:.2f}", f"{1 + P.FIST_AURA * 0.9:.2f}"])
    t = table_md(["Cấp", "QUALITY", "Công Kiếm", "Sát thương 1 nhát kiếm", "Nhát / Công", "Quyền Thể / Công (Bá Khí 90%)"], rows)
    return (
        f"`BanMenhEvents.onDamage` cộng `Công × QUALITY[giai đoạn]` vào đòn cầm Bản Mệnh Kiếm (`{cite_abs('src/main/java/com/hukhong/banmenh/BanMenhEvents.java', r'float base = ')}`), "
        f"và `DamageRules.resolve` cộng thêm một lần `Công` nữa cho mọi đòn cận chiến có vũ khí (`{cite_abs('src/main/java/com/hukhong/gates/DamageRules.java', r'out \+= .*attackPower')}`). "
        f"`BalanceSimTest.meleeHit` chỉ tính `weapon + Công` (`{cite_abs(TEST, r'default -> \(weapon')}`), nên số QUALITY không nằm trong lần cân chỉnh Bước 27. "
        f"Kết quả: đòn thường của Kiếm/Ma/Pháp tăng theo cấp (nhân với Công từ 2.3 lên 4.0), còn quyền của Thể không có hệ số tương ứng (`{cite_abs('src/main/java/com/hukhong/gates/DamageRules.java', r'FIST_AURA_COEF =')}`: hằng 1 + 1.8 × Bá Khí%).\n\n"
        + t + "\n\nĐòn thường chiếm 43-65% DPS của Kiếm/Ma/Pháp (bảng nguồn sát thương ở §4) nên đây là nguyên nhân chính khiến Thể, Đạo và Pháp tụt dần so với Kiếm khi cấp tăng. "
        "Giả định: kiếm đã tiến hóa tới giai đoạn tối đa theo cấp; nếu người chơi tiến hóa chậm, lệch này nhỏ hơn.")


def _s_than(P, ix):
    k = lambda nm, lv: ix[(nm, "B", lv)]["dps_base"]
    g99, g100 = P.hp_scale(99), P.hp_scale(100)
    return (
        f"Cấp 100 gộp 3 hiệu ứng cùng lúc: (a) Thần ×{1 + P.THAN_BONUS:g} lên mọi attribute mod, kể cả `crit_chance` và `crit_damage` (`{cite('stats/StatService.java', r'than\) inst.addOrUpdateTransientModifier')}`); "
        f"(b) QUALITY[6] = {P.QUALITY[6]:g} so với {P.QUALITY[5]:g} ở cấp 90 (`{cite('banmenh/BanMenhService.java', r'QUALITY =')}`); (c) nội tại Kiếm đòn thứ {P.KIEM_EVERY} nhân `CRIT_DAMAGE` (đã ×1.5 nên 2.25) và hồi máu theo sát thương đó (`{cite('passive/PassiveService.java', r'float dmg = ')}`).\n\n"
        f"DPS Kiếm B: cấp 80 = {k('KIEM',80):.0f}, cấp 100 = {k('KIEM',100):.0f} (×{k('KIEM',100)/k('KIEM',80):.1f}); Ma ×{k('MA',100)/k('MA',80):.1f}; Thể ×{k('THE',100)/k('THE',80):.1f}; Đạo ×{k('DAO',100)/k('DAO',80):.1f}; Pháp-Quang ×{k('P-QUANG',100)/k('P-QUANG',80):.1f}. "
        f"HP quái nhảy ×{g100/g99:.2f} tại cấp 100 (`hpScale(99)={g99:.0f}` → `hpScale(100)={g100:.0f}`; `{cite('entity/HuHoa.java', r'THAN_HP_MUL')}` là `THAN_HP_MUL = {P.THAN_HP_MUL:g}`). "
        f"Như vậy PLAN §0 mục 4 đúng về cú nhảy của quái, nhưng sức mạnh người chơi cấp 100 còn nhảy mạnh hơn và lệch theo đạo, nên thời gian hạ quái cùng cấp của Kiếm rơi từ ~4.5s (cấp ≤ 30) xuống ~{ix[('KIEM','B',100)]['ttk_mob']:.1f}s.")


def _s_hits(P, ix):
    k = lambda nm, lv: ix[(nm, "B", lv)]
    return (
        f"Chú thích `{cite('skill/Skill.java', r'Trảm Khí 120%')}` ghi Trảm Khí 120% Công, Liên Hoàn Trảm 3 × 70%, nhưng hằng số cùng dòng kế tiếp là `KIEM_1_COEF = {P.coef['KIEM_1_COEF']:g}`, `KIEM_2_COEF = {P.coef['KIEM_2_COEF']:g}` (`{cite('skill/Skill.java', r'KIEM_1_COEF =')}`); "
        f"`SkillEffects` dùng đúng hằng số (không có hệ số ẩn), tức chú thích sai (PLAN S5). Sát thương 1 lần bấm Q của Kiếm cấp 60 chỉ ≈ {k('KIEM',60)['avg_cast']:.0f} so với HP HuThi {k('KIEM',60)['mob_hp']:.0f}. "
        f"Số đòn kỹ năng để hạ quái cùng cấp (cấp 60, B): Kiếm {k('KIEM',60)['hits_mob']}, Ma {k('MA',60)['hits_mob']}, Thể {k('THE',60)['hits_mob']}, Đạo {k('DAO',60)['hits_mob']}, Pháp {k('P-QUANG',60)['hits_mob']} (dải 4-8). "
        "Kỹ năng của Kiếm, Ma, Pháp, Đạo đều yếu hơn đòn thường/nội tại nên người chơi không có động cơ dùng chúng làm lực lượng chính.")


def _s_oneshot(P, ix):
    k = lambda nm, lv: ix[(nm, "B", lv)]
    B = build(P, 60, ["DAO"], {"DAO": "B"}, "avg")
    c = P.bm_coef("DAO", 60)
    px = c * B.sp * P.PHANXET_HU
    mob = Target(P, "HuThi", 60)
    Bp = build(P, 10, ["PHAP"], {"PHAP": "B"}, "avg", "QUANG")
    tot = sum(P.phap_coef["QUANG"]) * Bp.sp
    mob10 = Target(P, "HuThi", 10)
    return (
        f"Phán Xét của Đạo nhân thêm ×{P.PHANXET_HU:g} với quái Hư Hóa (`{cite('skill/BanMenhSkillEffects.java', r't instanceof HuMob')}`) trên hệ số {P.bm['DAO']['base']:g}→{P.bm['DAO']['cap']:g} Phép lực (`{cite('skill/BanMenhSkill.java', r'PHAN_XET')}`). "
        f"Cấp 60: 1 nhát = {c:.1f} × {B.sp:.0f} × {P.PHANXET_HU:g} = {px:.0f} sát thương > HP HuThi {mob.design:.0f}. Đạo hạ quái thường trong một phím R (thời gian hạ rotation = {k('DAO',60)['ttk_mob']:.1f}s, cấp 60/80/100 đều 0.0s). "
        f"Pháp: tổng hệ số 4 phím Quang = {sum(P.phap_coef['QUANG']):.2f} × Phép lực cấp 10 ({Bp.sp:.0f}) = {tot:.0f} so với HP HuThi cấp 10 = {mob10.design:.0f}: một lượt 4 phím đủ hạ quái, trong khi hầu hết thời gian mô phỏng Pháp dựa nhiều vào đòn kiếm.")


def _s_heal(P, ix):
    k = lambda nm, lv: ix[(nm, "B", lv)]
    return (
        f"Nội tại Kiếm hồi `kiemHealPct × sát thương` của nhát thứ {P.KIEM_EVERY} (`{cite('passive/PassiveService.java', r'queue\(atk, target, 8')}`; hồi ở `{cite('passive/PassiveService.java', r'pd.owner\(\).heal\(pd.amount')}`) và không có trần theo HP. "
        f"Hút máu Ma/Kiếm bị chặn ở 4% HP tối đa mỗi nhát (`{cite('gates/DamageRules.java', r'atk.heal')}`) nhưng số nhát/giây lớn. Tự hồi trung bình mô phỏng so với sát thương nhận từ 1 quái (B): "
        f"cấp 60 Kiếm {k('KIEM',60)['heal']:.0f}/{k('KIEM',60)['inc']:.0f}, Ma {k('MA',60)['heal']:.0f}/{k('MA',60)['inc']:.0f}, Đạo {k('DAO',60)['heal']:.0f}/{k('DAO',60)['inc']:.0f}; "
        f"cấp 100 Kiếm {k('KIEM',100)['heal']:.0f}/{k('KIEM',100)['inc']:.0f}, Ma {k('MA',100)['heal']:.0f}/{k('MA',100)['inc']:.0f}. "
        "Vì hồi tăng theo sát thương gây ra, Kiếm và Ma không thể chết trước 1 quái ở cấp cao (xem thời gian sống §8, 4 quái cùng lúc), trong khi Pháp và Thể không có nguồn hồi tương ứng.")


def _s_boss(P, ix):
    k = lambda nm, lv: ix[(nm, "B", lv)]["ttk_boss"]
    return (
        f"WorldBoss HP gốc {P.boss['hp']:.0f} × `hpMul() = {P.boss['hpmul']:g}` (`{cite('entity/WorldBoss.java', r'protected double hpMul')}`) × `hpScale(cấp)`. "
        f"Thời gian hạ boss cùng cấp (B, kit mặc định): cấp 10 {k('KIEM',10):.0f}s (Kiếm) / {k('DAO',10):.0f}s (Đạo); cấp 60 {k('KIEM',60):.0f}s / {k('THE',60):.0f}s (Thể) / {k('MA',60):.0f}s (Ma); cấp 100 {k('KIEM',100):.0f}s (Kiếm). "
        "Chỉ Thể ở cấp ≥ 80 và Ma ở cấp 100 chạm dải 90-180s. Nội tại % máu tối đa của Đạo/Ma bị chia 1/3 với boss (`PassiveMath.BOSS_CAP`) nhưng với HP thiết kế vượt 1024 bị `ehpScale` làm yếu thêm, nên không giải thích được sự chênh; nguyên nhân là hpMul 2.0 quá thấp so với DPS hiện có.")


def _s_phap(P, ix):
    r = ix[("P-QUANG", "B", 60)]
    sr = r["src"]
    n_phap_core = sum(1 for c in P.cores if c["path"] == "phap")
    return (
        f"Pháp-Quang cấp 60: {100*sr['melee']:.0f}% DPS đến từ đòn kiếm (Công 2.5 + 0.9/cấp, `StatService.java` bảng PHAP), {100*sr['skill']:.0f}% từ phép. Có {n_phap_core} file Lõi `path: phap` trong `data/hukhong/hukhong/core/` "
        f"(Pháp chỉ nhận Lõi `any`), và `BookSkill` không có kỹ năng cho Pháp. Độ trâu Pháp ≈ {ix[('P-QUANG','B',60)]['ttd4']/ix[('KIEM','B',60)]['ttd4']:.2f}× Kiếm (dải 0.75-1.05). Nguyên tố hỗ trợ (Thủy/Thổ) có bộ hệ số riêng trong `PhapMath.COEF` nhưng DPS đơn mục tiêu chỉ chênh vài phần trăm so với nguyên tố sát thương.")


def _s_ma(P, ix):
    modes = {lv: ix[("MA", "B", lv)]["mmode"] for lv in LEVELS}
    return (
        f"Mô phỏng chọn 'sword' (cầm Bản Mệnh Kiếm) cho Ma ở các cấp {', '.join(f'{l}:{m}' for l, m in modes.items())}. "
        f"Tia ma tay không (nội tại, `{cite('passive/PassiveService.java', r'public static void maBolt')}`) chỉ gây {P.UNARMED:g} sát thương cố định + 1 tầng, cần tay trống; đổi sang cầm kiếm cho DPS cao hơn ở mọi cấp. "
        "Vì vậy nội tại 'đặc trưng' của Ma không bao giờ là lựa chọn tối ưu. Thêm vào đó Ma được hưởng hút máu sau 4% HP/nhát lên cả đòn kiếm (thành nhóm 'trâu' nhất).")


def _s_order(P, ix):
    return (
        f"`PathRules.onIncoming` (`{cite('pathrules/PathRules.java', r'public static void onIncoming')}`) gán đòn thường của Đạo = 0 (`{cite('gates/DamageRules.java', r'out = 0f;$')}`), còn `BanMenhEvents.onDamage` (`{cite('banmenh/BanMenhEvents.java', r'public static void onDamage')}`) tính `e.getAmount() + Công × QUALITY`. "
        "Cả hai đều ở mức ưu tiên NORMAL và nằm ở hai lớp khác nhau, nên thứ tự phụ thuộc đăng ký sự kiện. Nếu Bản Mệnh chạy sau, đòn kiếm của Đạo = Công × QUALITY (khác 0). Mô phỏng không thể xác nhận từ mã; cần GameTest.")


def cite_in(rel, start, rx):
    lines = rd(rel).splitlines()
    i0 = next(i for i, ln in enumerate(lines) if re.search(start, ln))
    for i in range(i0, len(lines)):
        if re.search(rx, lines[i]):
            return f"{rel}:{i + 1}"
    raise KeyError(rel + rx)


TABLE_FIELDS = ["Công cơ bản", "Công/cấp", "Phép cơ bản", "Phép/cấp", "Phòng cơ bản", "Phòng/cấp", "Bạo kích cơ bản", "Bạo kích/cấp",
                "Hút máu cơ bản", "Hút máu/cấp", "Tài nguyên cơ bản", "Tài nguyên/cấp"]
VITAL_FIELDS = ["Sinh lực cơ bản", "Sinh lực/cấp", "Tốc độ/cấp"]
ATTR_NOTE = {
    "FIST_AURA": ("gates/DamageRules.java", r"FIST_AURA_COEF =", "DamageRules.FIST_AURA_COEF: quyền Thể không còn tụt so với đòn kiếm (QUALITY)"),
    "KIEM_HEAL_MAX": ("passive/PassiveMath.java", r"KIEM_HEAL_MAX =", "PassiveMath.KIEM_HEAL_MAX: hồi của nội tại Kiếm"),
    "DAO_MAX": ("passive/PassiveMath.java", r"DAO_MAX =", "PassiveMath.DAO_MAX: nội tại Đạo (hồi và % máu)"),
    "KIEM_EVERY": ("passive/PassiveMath.java", r"KIEM_EVERY =", "PassiveMath.KIEM_EVERY: nhịp đòn thứ N của Kiếm"),
    "MA_PCT_MAX": ("passive/PassiveMath.java", r"MA_PCT_MAX =", "PassiveMath.MA_PCT_MAX: nổ % máu của Ma"),
    "THAN_BONUS": ("stats/StatService.java", r"THAN_BONUS =", "StatService.THAN_BONUS: bộ chỉ số Thần"),
    "MOB_ARMOR_PER": ("entity/HuHoa.java", r"Math\.min\(20, 0\.", "HuHoa.applyStats: giáp quái/cấp"),
    "THAN_CRIT": ("stats/StatService.java", r"than\) inst\.addOrUpdateTransientModifier",
                  "StatService.refresh: LOẠI crit_chance và crit_damage khỏi vòng nhân Thần (đổi logic 1 dòng, không phải hằng số)"),
}


def proposal_rows(P, ov):
    rows = []
    for key, new in ov.items():
        p = key.split(".")
        k0 = p[0]
        if k0 == "quality":
            cit, ch, old = cite("banmenh/BanMenhService.java", r"QUALITY ="), f"QUALITY[{p[1]}]", P.QUALITY[int(p[1])]
            note = "BanMenhService.QUALITY: nén đường cong để đòn thường Kiếm/Ma/Pháp không vượt Thể/Đạo theo cấp"
        elif k0 == "coef":
            cit, ch, old = cite("skill/Skill.java", p[1] + " ="), "Skill." + p[1], P.coef[p[1]]
            note = "Skill.*_COEF: kỹ năng 1 đủ lớn để hạ quái trong 4-8 nhát; đồng thời sửa chú thích"
        elif k0 == "table":
            cit = cite_in("stats/StatService.java", r"double\[\] table", rf"case {p[1]} -> new double")
            ch, old = f"table() {p[1]}: {TABLE_FIELDS[int(p[2])]}", P.table[p[1]][int(p[2])]
            note = "StatService.table(): cân Công/Phép theo vai trò"
        elif k0 == "vitals":
            cit = cite_in("stats/StatService.java", r"double\[\] vitals", rf"case {p[1]} -> new double")
            ch, old = f"vitals() {p[1]}: {VITAL_FIELDS[int(p[2])]}", P.vitals[p[1]][int(p[2])]
            note = "StatService.vitals(): độ trâu theo vai trò"
        elif k0 == "phapscale":
            cit, ch, old = cite("phaptu/PhapMath.java", r"COEF = \{"), "PhapMath.COEF: nhân mọi hệ số", 1.0
            note = "PhapMath.COEF: phép đủ lớn so với đòn kiếm"
        elif k0 == "phapel":
            cit, ch, old = cite("phaptu/PhapMath.java", rf"/\* {p[1]}"), f"PhapMath.COEF hàng {p[1]}: nhân 4 hệ số", 1.0
            note = "san nguyên tố lệch"
        elif k0 == "bmscale":
            cit = cite("skill/BanMenhSkill.java", rf"Path\.{p[1]}, \"bm_")
            b = P.bm[p[1]]
            ch, old = f"BanMenhSkill {p[1]}: base/perLevel/cap nhân {new:g}", f"{b['base']:g}/{b['per']:g}/{b['cap']:g}"
            new = f"{b['base']*new:.3g}/{b['per']*new:.3g}/{b['cap']*new:.3g}"
            note = "hệ số Bản Mệnh R"
        elif k0 == "boss":
            if p[1] == "hpmul":
                cit, ch, old = cite("entity/WorldBoss.java", r"protected double hpMul"), "WorldBoss.hpMul()", P.boss["hpmul"]
                note = "boss thế giới hạ trong 90-180s"
            else:
                cit, ch, old = cite("entity/WorldBoss.java", r"MAX_HEALTH, 140"), "WorldBoss ARMOR gốc", P.boss["armor"]
                note = "giáp boss: đòn thường (Thể) đỡ bị nuốt"
        elif k0 == "attr":
            f, rx, note = ATTR_NOTE[p[1]]
            cit, ch = cite(f, rx), note.split(":")[0]
            old = getattr(P, p[1])
            if p[1] == "MOB_ARMOR_PER":
                cit = cite("entity/HuHoa.java", r"Math\.min\(20, ")
        else:
            continue
        if isinstance(new, float) and isinstance(old, float) and abs(old - new) < 1e-9:
            continue
        rows.append([f"`{cit}`", ch, _fmt(old), _fmt(new), note])
    return rows


STRUCT = [
    ("Số QUALITY của kiếm Bản Mệnh nằm ngoài lần cân chỉnh Bước 27, làm Kiếm/Ma/Pháp tăng nhanh hơn Thể/Đạo theo cấp", _s_quality),
    ("Cú nhảy ở cấp 100: Thần ×1.5 lên cả bạo kích + QUALITY[6]=3.0 + nội tại Kiếm", _s_than),
    ("Kỹ năng đơn yếu hơn đòn thường; chú thích Skill.java sai", _s_hits),
    ("Phán Xét ×2 và combo Pháp hạ quái thường ngay lập tức", _s_oneshot),
    ("Hồi máu tỉ lệ theo sát thương gây ra, không theo HP", _s_heal),
    ("Boss thế giới quá yếu so với DPS hiện có", _s_boss),
    ("Pháp Tu: một nửa DPS đến từ đòn kiếm, không có Lõi/sách riêng", _s_phap),
    ("Ma Tu: nội tại tia ma tay không không bao giờ tối ưu", _s_ma),
    ("Rủi ro thứ tự sự kiện giữa PathRules và BanMenhEvents", _s_order),
]


# ============================================================================= Linh Bao (bao cao rieng)
def linhbao_report(write=False, workers=None):
    """DPS ben vung (kit toi uu) cua Kiem/Dao/The/Ma truoc/sau Linh Bao, % so voi Kiem. Giai doan = tran theo cap."""
    lvls, apts = [1, 10, 30, 60, 80, 100], ["B", "SSS"]
    jobs = []
    for art in (0, 1):
        for lv in lvls:
            for apt in apts:
                for pth in PATHS4:
                    jobs.append(({"_art": 1} if art else {}, pth, None, lv, apt, "avg", None))
    with ProcessPoolExecutor(max_workers=workers) as ex:
        res = list(ex.map(eval_cell, jobs, chunksize=2))
    it = iter(res)
    tab = {}
    for art in (0, 1):
        for lv in lvls:
            for apt in apts:
                for pth in PATHS4:
                    tab[(art, lv, apt, pth)] = next(it)["dps_base"]
    out = []
    w = out.append
    w("# REPORT Linh Bao - DPS ben vung truoc/sau (balance_sim.py --linhbao)\n")
    w("Kiem khong doi (Ban Menh Kiem giu nguyen). Giai doan = tran theo cap. Don gian: nguoi choi tien hoa het co the. % = DPS / DPS Kiem cung cap + tu chat.\n")
    for apt in apts:
        w(f"\n## Tu chat {apt}\n")
        w("| Cap | Kiem | Dao truoc | Dao sau | The truoc | The sau | Ma truoc | Ma sau |")
        w("|---|---|---|---|---|---|---|---|")
        for lv in lvls:
            k = tab[(0, lv, apt, "KIEM")]
            cells = [f"{k:.0f}"]
            for pth in ("DAO", "THE", "MA"):
                b, a = tab[(0, lv, apt, pth)], tab[(1, lv, apt, pth)]
                cells += [f"{b:.0f} ({b / k * 100:.0f}%)", f"{a:.0f} ({a / k * 100:.0f}%)"]
            w(f"| {lv} | " + " | ".join(cells) + " |")
    text = "\n".join(out)
    print(text)
    if write:
        (ROOT / "docs/REPORT-v1-linhbao-balance.md").write_text(text + "\n", encoding="utf-8", newline="\n")
    return tab


if __name__ == "__main__":
    import time
    t0 = time.time()
    if "--linhbao" in sys.argv:
        linhbao_report("--write" in sys.argv)
    else:
        main("--quick" in sys.argv)
    print(f"done in {time.time() - t0:.0f}s")
