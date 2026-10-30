"""Trich cac loi goi Tx.t("key", "vi", "en", ...) (Dot 7c) -> docs/research/lang-a7.txt  (dong: key | vi: ... | en: ...).
Chay: python tools/extract_lang_a7.py   (tu thu muc goc du an). Bo qua key da co trong vi_vn.json."""
import json
import pathlib
import re

root = pathlib.Path(__file__).resolve().parent.parent
src = root / "src" / "main" / "java"
existing = json.loads((root / "src/main/resources/assets/hukhong/lang/vi_vn.json").read_text(encoding="utf8"))
S = r'"((?:[^"\\]|\\.)*)"'
pat = re.compile(r"Tx\.t\(\s*" + S + r"\s*,\s*" + S + r"\s*,\s*" + S)
out = {}


def unesc(s):
    return s.encode("utf8").decode("unicode_escape").encode("latin1").decode("utf8") if "\\" in s else s


for f in sorted(src.rglob("*.java")):
    text = f.read_text(encoding="utf8")
    for m in pat.finditer(text):
        k, vi, en = (unesc(x) for x in m.groups())
        if k not in out and k not in existing:
            out[k] = (vi, en)

# khoa dong / bieu thuc dieu kien (khong bat duoc bang regex)
manual = {
    "hukhong.auction.sign": ("Đấu Giá Hành", "Auction House"),
    "hukhong.npc.auctioneer": ("Đấu Giá Sư", "Auctioneer"),
    "hukhong.npc.seer": ("Thầy Bói", "Fortune Teller"),
    "hukhong.auction.f.all": ("Tất cả", "All"),
    "hukhong.auction.f.honcot": ("Hồn Cốt", "Soul Bones"),
    "hukhong.auction.f.pill": ("Đan", "Pills"),
    "hukhong.auction.f.book": ("Sách", "Books"),
    "hukhong.auction.f.mat": ("Liệu", "Materials"),
    "hukhong.auction.f.gear": ("Trang bị", "Gear"),
    "hukhong.auction.f.mine": ("Của tôi", "Mine"),
}
for i, (vi, en) in enumerate([("Lão Trương", "Old Truong"), ("Thím Hai", "Aunt Hai"), ("Chú Ba Cảng", "Uncle Ba the Docker"), ("Cô Liễu", "Miss Lieu"),
                              ("Anh Hổ", "Brother Ho"), ("Bà Lục", "Granny Luc"), ("Thợ Rèn Thạch", "Smith Thach"), ("Tiểu Mai", "Little Mai")]):
    manual["hukhong.living.name.%d" % i] = (vi, en)
for k, v in manual.items():
    if k not in existing:
        out[k] = v

lines = ["%s | vi: %s | en: %s" % (k, vi, en) for k, (vi, en) in sorted(out.items())]
dest = root / "docs" / "research" / "lang-a7.txt"
dest.write_text("\n".join(lines) + "\n", encoding="utf8")
print("wrote", dest, len(lines), "keys")
