# DIALOG — Chấp Hành Giả IV "Cơ Vô Tận" (kiểu Mohg)

**Theme**: "kẻ cuối cùng, sau ta không còn gì". Keys in `vi_vn.json` + `en_us.json`.

| Key | Cutscene | Timing (tick) | vi_vn |
|-----|----------|---------------|-------|
| entity.hukhong.chg_4 | Intro (110t, `Chg4Moves.introSubs`) | 6-36 | (tên boss) |
| hukhong.dialog.chg4.intro.1 | Intro | 36-72 | Ba vị sư huynh đã ngã. Ta là kẻ cuối cùng. |
| hukhong.dialog.chg4.intro.2 | Intro | 72-106 | Sau ta, không còn gì cả. Đến đây, để máu ngươi lấp chỗ trống ấy. |
| hukhong.dialog.chg4.p2.1..3 | Phase 2 (120t, `BossPhases.chg4`) | 8-44 / 44-84 / 84-116 | Máu ngươi... ấm thật. / Huyết dực, khai! / Đạo của ta không cần trời xanh. Chỉ cần máu. |
| hukhong.dialog.chg4.p3.1..3 | Phase 3 (120t) | 8-44 / 44-84 / 84-116 | Sư tôn... đệ tử cuối cùng của người vẫn còn đứng đây. / Huyết diễm đốt tận đạo cơ, đốt cả hậu lộ. / Ta không còn đường lui. Ngươi cũng vậy. |
| hukhong.dialog.chg4.ult.1..3 | Ultimate Hư Vô (70t, `Chg4Moves.ultScene`) | 2-24 / 24-48 / 48-68 | Huyết chú đã khắc... / Mọi sinh khí, quy về một. / Hư Vô! |
| hukhong.dialog.chg4.death.1..3 | Death (100t, `Chg4Moves.deathScene`) | 6-36 / 36-68 / 68-96 | Máu... nguội rồi. / Sau ta không còn ai canh cửa. Con đường phía trước... là của ngươi. / Sư tôn, đệ tử về nhà. |

## Move names (`hukhong.boss.move.<id>`)

| id | vi | en | Phase |
|----|----|----|-------|
| c4_quet | Huyết Xoa Hoành Tảo | Blood Trident Sweep | 1+ |
| c4_dam | Huyết Xoa Đột Thích | Blood Trident Thrust | 1+ |
| c4_huyet_diem | Huyết Diễm Lê Địa | Bloodflame Furrow | 1+ |
| c4_tam_lien | Tam Liên Huyết Kích | Threefold Blood Strike | 1+ |
| c4_huyet_luan | Huyết Diễm Hoàn | Bloodflame Ring | 2+ |
| c4_lao_canh | Huyết Dực Giáng | Blood Wing Descent | 2+ |
| c4_huyet_tuyen | Huyết Tuyền Phún | Blood Geysers | 3 |
| c4_nihil | Hư Vô | Nihil | 3 (ultimate) |

Legacy keys `hukhong.boss.sub.chg_4.*` and `hukhong.boss.move.huyet_*` are now unused (left in place).
