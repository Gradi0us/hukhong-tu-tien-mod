# DIALOG — Nguyên Thủy Tịch Diệt (NTTD) cutscene keys

**Last Updated**: 2026-11-10 — initial list for two-form rework (form 1 Hoá Thân → form 2 Nguyên Thủy Tịch Diệt)
**Status**: placeholder text in `vi_vn.json` + `en_us.json`; rewrite freely, keep keys.

Timing = tick window inside the cutscene (20 tick = 1 s). Lines longer than the window are cut off visually — keep each ≤ ~90 chars.
Code: `src/main/java/com/hukhong/endgame/NguyenThuyTichDiet.java` (intro/ult/transform/phase), `EndgameService.onBossDefeated` (death).

| Scene | Key | Window (ticks) | Context |
|---|---|---|---|
| Intro (130 t) | `entity.hukhong.nguyen_thuy_tich_diet.form1` | 8–40 | Name card of form 1 (also title + boss bar name) |
| Intro | `hukhong.dialog.nttd.intro.1` | 40–85 | Narration: possessed cultivator stands in the Open Circle |
| Intro | `hukhong.dialog.nttd.intro.2` | 85–126 | Boss line as camera closes in; ground cracks at t=85 |
| Form-1 ultimate "Hư Chùy Thiên Phạt" (50 t) | `hukhong.dialog.nttd.ult1.1` | 4–25 | Vessel raises hammer, void gathers in the sky (at <50% and <20% form-1 HP) |
| Form-1 ultimate | `hukhong.dialog.nttd.ult1.2` | 25–48 | Shouted move name |
| Transformation (180 t) | `hukhong.dialog.nttd.transform.1` | 6–45 | Body cracks, seams of light |
| Transformation | `hukhong.dialog.nttd.transform.2` | 45–85 | Boss voice (still form 1) |
| Transformation | `hukhong.dialog.nttd.transform.3` | 85–125 | Husk crumbles; form 2 revealed at t=110 |
| Transformation | `hukhong.dialog.nttd.transform.4` | 125–176 | Narration: true form revealed, camera pulls wide |
| Form-2 phase 2/3/4 (90 t each) | `hukhong.dialog.nttd.phase.{2,3,4}.1` | 6–45 | Narration on phase change |
| Form-2 phase 2/3/4 | `hukhong.dialog.nttd.phase.{2,3,4}.2` | 45–86 | Boss line (phase 3 = Dạ Khuyết appears right after) |
| Form-2 ultimate "Kim Hoàn Diệt" (50 t) | `hukhong.dialog.nttd.nova.1` | 4–25 | Golden ring kindles (phase 4 only) |
| Form-2 ultimate | `hukhong.dialog.nttd.nova.2` | 25–48 | Shouted move name |
| Death (190 t) | `hukhong.dialog.nttd.death.1` | 6–50 | Boss falls |
| Death, Dạ Khuyết ally alive | `hukhong.dialog.nttd.death.dk.1` | 50–95 | Dạ Khuyết speaks |
| Death, Dạ Khuyết ally alive | `hukhong.dialog.nttd.death.dk.2` | 95–140 | Dạ Khuyết speaks |
| Death, no ally | `hukhong.dialog.nttd.death.flee` | 50–110 | Narration: Dạ Khuyết fled |
| Death (both branches) | `hukhong.dialog.nttd.death.end` | 110/140–186 | Lead-in to the ending choice screen |

Non-dialogue keys added: `hukhong.endgame.boss.bar_form1` (boss bar format, `%s` = name).
Old keys `hukhong.endgame.boss.intro.1`, `hukhong.endgame.boss.phase.N.x`, `hukhong.endgame.outro.dk.*` are no longer used by cutscenes (left in lang files).
