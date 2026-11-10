# DIALOG — CHG III (Lăng Uyên) keys

**Last Updated**: 2026-11-10 — Morgott-style moveset rework (Chg3Moves)

All keys exist in both `vi_vn.json` and `en_us.json`.

| Cutscene | Trigger (code) | Keys |
|----------|----------------|------|
| Intro | `BossPhaseDirector.startIntro` → `ChapHanhGia.introLines` → `Chg3Moves.introSubs` | `entity.hukhong.chg_3`, `hukhong.dialog.chg3.intro.1..2` |
| Phase 2 (Uyên Môn Khai) | `BossPhases.chg3()` phase 2 subs | `hukhong.dialog.chg3.p2.1..3` |
| Phase 3 (Huyết Uyên Trào) | `BossPhases.chg3()` phase 3 subs | `hukhong.dialog.chg3.p3.1..3` |
| Ultimate (Vạn Mâu Uyên Giáng) | `Chg3Moves.ultScene` | `hukhong.dialog.chg3.ult.1..2` |
| Death | `ChapHanhGia.die` → `Chg3Moves.deathScene` | `hukhong.dialog.chg3.death.1..2` |

## Move names (`hukhong.boss.move.<id>`)

| id | VI | EN |
|----|----|----|
| c3_phi_dao | Uyên Nhận Trì Phát | Abyssal Blades, Delayed |
| c3_cu_kiem | Uyên Kiếm Hiện | Abyss Greatsword Manifest |
| c3_huyet_tri | Huyết Trì Uyên Thứ | Blood Pool Abyss Thorns |
| c3_uyen_chuy | Uyên Chùy Giáng | Abyss Hammer Descent |
| c3_huyet_kiem | Huyết Kiếm Uyên Lãng | Cursed Blood Sword Waves |
| c3_phi_dao_vu | Uyên Nhận Vũ | Abyssal Blade Storm |
| c3_song_chuy | Song Chùy Uyên Chấn | Twin Hammer Abyss Quake |
| c3_van_mau | Vạn Mâu Uyên Giáng (ultimate) | Myriad Spears of the Abyss |

Legacy keys `hukhong.boss.sub.chg_3.*`, `hukhong.boss.move.kiem_*` are no longer referenced (left in lang, harmless).
