# DIALOG — Chấp Hành Giả I "Thạch Hống" (kiểu Godfrey / Hoarah Loux) — khoá thoại

**Last Updated**: 2026-11-10 — tạo khoá thoại + tên chiêu cho bộ chiêu riêng CHG I (ChgOneMoves)
**Status**: Active (vi_vn văn phong tu tiên + en_us)

Mọi câu nằm trong `src/main/resources/assets/hukhong/lang/{vi_vn,en_us}.json`, hiện dưới dạng phụ đề cutscene (`BossPhaseConfig.SubLine`).

## Cutscene & khoá

| Khoá | Cảnh | Khung (tick) | Nguồn |
|------|------|--------------|-------|
| `entity.hukhong.chg_1` | Giới thiệu (tên) | 6–36 / 110 | `ChgOneMoves.introSubs` |
| `hukhong.dialog.chg1.intro.1` | Giới thiệu | 36–72 | `ChgOneMoves.introSubs` |
| `hukhong.dialog.chg1.intro.2` | Giới thiệu | 72–106 | `ChgOneMoves.introSubs` |
| `hukhong.dialog.chg1.p2.1` | Chuyển pha 2 (HP < 66%) | 8–44 / 120 | `BossPhases.chgOne` |
| `hukhong.dialog.chg1.p2.2` | Chuyển pha 2 — trút giáp (lộ diện t60) | 44–84 | `BossPhases.chgOne` |
| `hukhong.dialog.chg1.p2.3` | Chuyển pha 2 | 84–116 | `BossPhases.chgOne` |
| `hukhong.dialog.chg1.p3.1` | Chuyển pha 3 (HP < 33%) | 8–44 / 120 | `BossPhases.chgOne` |
| `hukhong.dialog.chg1.p3.2` | Chuyển pha 3 — đất nứt dung nham (lộ diện t70) | 44–84 | `BossPhases.chgOne` |
| `hukhong.dialog.chg1.p3.3` | Chuyển pha 3 | 84–116 | `BossPhases.chgOne` |
| `hukhong.dialog.chg1.ult.1` | Tối thượng Chấn Thiên Liệt Địa | 2–28 / 60 | `ChgOneMoves.ultScene` |
| `hukhong.dialog.chg1.ult.2` | Tối thượng | 28–58 | `ChgOneMoves.ultScene` |
| `hukhong.dialog.chg1.death.1` | Tử trận | 6–46 / 90 | `ChgOneMoves.deathScene` |
| `hukhong.dialog.chg1.death.2` | Tử trận | 46–86 | `ChgOneMoves.deathScene` |

## Tên chiêu (action bar) + gợi ý

| Khoá | vi | Ghi chú né |
|------|----|-----------|
| `hukhong.boss.move.chg1_riu_giang` | Phủ Giáng | vạch thẳng 10 block, 22 tick |
| `hukhong.boss.move.chg1_dap_dat` | Chấn Địa | vòng r6 rồi vành 6–12 |
| `hukhong.boss.move.chg1_phi_phu` | Phi Thân Phủ Kích | vòng r4.5 tại mục tiêu, 30 tick |
| `hukhong.boss.move.chg1_hoanh_tao` | Hoành Tảo Thiên Quân | 2 quạt + vạch bổ dọc |
| `hukhong.boss.move.chg1_dia_liet` | Địa Liệt Tuyến | 3 vạch quạt ±30°, 24 tick |
| `hukhong.boss.move.chg1_cam_na` | Hống Thủ Cầm Nã | vạch vồ 7 block; trúng => nhấc + quật |
| `hukhong.boss.move.chg1_ho_bo` | Hổ Bộ Liên Chấn | 3 vòng lan r5/r10/r15 |
| `hukhong.boss.move.chg1_nem_da` | Liệt Thạch Phao | 2 vòng r3.5 |
| `hukhong.boss.move.chg1_chan_thien` | Chấn Thiên Liệt Địa | vòng r18, 80 tick — nhảy lúc giáng |
| `hukhong.chg1.jump_hint` | gợi ý action bar trong lúc gồng tối thượng | |

Khoá cũ `hukhong.boss.sub.chg_1.*` và `hukhong.boss.move.{da_trao,chan_dong_song,hong_thien}` không còn được CHG I dùng (giữ nguyên, không xoá).
