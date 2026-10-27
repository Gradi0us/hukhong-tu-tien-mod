# DESIGN-v1 — Luồng truyện chính (gameplay ↔ cốt truyện)

**Created**: 2026-10-27
**Last Updated**: 2026-10-27 — Bản đầu: chương 0 (mở đầu theo xuất thân) + chương 1–10 gắn mốc cảnh giới
**Status**: Implemented (server) — lang chờ gộp từ `docs/research/lang-q2.txt`
**Phụ thuộc**: S1-lore v2.1 (§4 Chấp Hành Giả, §7 Hồi, §9 xuất thân), `story/flow/*`, `story/objective/Objectives`

---

## Key Findings

| # | Điểm chốt | Hệ quả |
|---|-----------|--------|
| 1 | Cảm giác "rời rạc" đến từ việc các hệ thống (Ấn Hồn Đài, tông, Bí Cảnh, tập kích, Sứ Đồ, CHG) không ai chỉ đường | Mỗi bước truyện luôn có **mục tiêu** (`Objectives.set`) trỏ vào đúng hệ thống |
| 2 | Cảnh giới đã có nhịp 10 cấp (mốc khóa → Ấn Hồn Đài) | **Chương N ↔ dải cấp 10(N−1)…10N−1**; đột phá mốc = sang chương kế |
| 3 | Rival Hạng Chiêu đã có trong S1 §7.1 và dialogue (`hang_chieu`) | Dùng lại, không tạo nhân vật mới; xuất hiện ở chương **2, 5, 8** (S1: Hồi 2 thi đấu, Hồi 3 cướp cơ duyên, Hồi 5 bị Từ Âm mê hoặc) |
| 4 | Chế độ Tự do không có tuyến | `StoryFlow` chỉ chạy ở `MODE_STORY`; Tự do: không objective chính |

---

## 1. Ràng buộc chương ↔ cảnh giới

```
 Chương : 0      1      2      3      4      5      6      7      8      9      10
 Cấp    : (mở)  1-9   10-19  20-29  30-39  40-49  50-59  60-69  70-79  80-89  90-100
 Hồi S1 : -      1      2      2/3    3      3      4      4      5      6      7-8
 Chuyển : chọn mode → Ấn Hồn Đài (pro_altar) → mở khóa mốc 10 → 20 → … → 90
```

- Quy tắc: `chapterFor(level, locked)` = `level/10 + 1`, trừ 1 nếu đang đứng đúng mốc khóa (10,20…90) chưa mở. Tối đa 10.
- Chuyển chương xảy ra khi **mở khóa mốc** (hook ở `CultivationService.update` → `StoryFlow.onProgress`; Khảo Hạch mốc 90 cũng qua `forceUnlockMilestone`). Nhảy cấp bằng lệnh/đột phá khác vẫn được đồng bộ mỗi giây (poll).
- Mỗi chương kết thúc bằng 2 bước chung: `cN_grind` (đạt cấp 10N) → `cN_ms` (đến Ấn Hồn Đài, chọn Lõi). Nên người chơi **không bao giờ không có việc**.

## 2. Bảng chương

| Ch | Hồi | Mục tiêu | NPC chính | Địa điểm | Bước (id → kiểu hoàn thành) | Hệ thống gắn |
|----|-----|----------|-----------|----------|------------------------------|--------------|
| 0 | Mở đầu | Sống sót, đến Ấn Hồn Đài | Mộ Dao, Sư Phụ (sect) | Nơi xuất thân → Lưu Ly Cảng | xem §3 | Hư Hóa, Linh Khí Đài, Ấn Hồn Đài |
| 1 | Hạt Bụi | Biết Hư Không; lên cấp 10 | Mộ Dao | Lưu Ly Cảng | `c1_talk` (thoại) → grind → ms | Ấn Hồn Đài, Lõi |
| 2 | Nhập Đạo | Vào tông; gặp Hạng Chiêu | Hạng Chiêu (thành), tông chủ | Sect gần nhất; Hắc Thạch Quan | `c2_sect` (gia nhập tông) → `c2_rival` (thoại) | Gia nhập tông, bảng tông |
| 3 | Bí Cảnh đầu | Vào Bí Cảnh; thu Văn Kiện đầu | Mộ Dao | Cổ Tiên Thành | `c3_bicanh` (`BiCanhManager.inRun`) → `c3_docs` (≥1 Văn Kiện) | Bí Cảnh, Sứ Đồ rơi Văn Kiện |
| 4 | Bóng Dạ | Săn Ảnh Tuyến, gặp Hứa Cổ | Hứa Cổ | Cổ Tiên Thành | `c4_docs` (≥2) → `c4_hua_co` (thoại, dialogue cũ ch3) | Văn Kiện A/B, bóc mặt nạ |
| 5 | Cơ Duyên Bị Cướp | Giữ thành; đối đầu Hạng Chiêu | Hạng Chiêu | Lưu Ly Cảng, đường | `c5_invasion` (thắng tập kích; thành RUIN thì bỏ qua) → `c5_rival` (**đấu**) | `InvasionManager.warn`, Ấn Hồn Đài |
| 6 | Địa Môn | Lộ hang ổ; Mộ Dao thú nhận | Mộ Dao | Lưu Ly Cảng | `c6_lair` (`StoryData.lair`≠0) → `c6_modao` (thoại, dialogue cũ ch4: tin/nghi) | Văn Kiện → hang ổ CHG |
| 7 | Thạch Hống | Hạ Chấp Hành Giả I (67) | — | Viêm Sơn (`ChgService.lairXZ(1)`) | `c7_chg1` (`ChgKit.defeated`) | CHG I |
| 8 | Từ Bi | Hạng Chiêu bị Từ Âm mê hoặc; hạ CHG II (78) | Hạng Chiêu | Trên đường Kim Sa; hang II | `c8_rival` (**đấu**, lựa chọn cờ `rival_saved`/`rival_lost`) → `c8_chg2` | CHG II |
| 9 | Kiếm Vong | Hạ CHG III (86); Mộ Dao rời/hy sinh | Mộ Dao | Tuyết Kiếm Lĩnh → Lưu Ly Cảng | `c9_chg3` → `c9_modao` (thoại: `trust_mo_dao`→rời đi; `doubt_mo_dao`→hy sinh) | CHG III |
| 10 | Khảo Hạch | Hạ CHG IV (98), ráp 4 Mảnh, Khảo Hạch, kết thúc | Dạ Khuyết (ảo ảnh) | Huyết Trạch; cổng Hư Không | `c10_grind`(98) → `c10_chg4` → `c10_gate` (đủ 4 Mảnh + cổng) → `c10_end` (cờ `ending_seal/absorb`) | Khảo Hạch, Endgame |

Cầu nối lore: Văn Kiện A/B → hang ổ (S1 §3.4); Dạ Khuyết lộ diện ở chương 10 qua banner (S1 §7.2 Hồi 7); Hạng Chiêu có thể về phe Dạ Khuyết khi `rival_lost`.

## 3. Chương 0 — mở đầu 10–15 phút theo xuất thân

Khởi động khi chọn **Tuyến truyện** (`StoryService.chooseMode` → `StoryFlow.startPrologue`). Xuất thân → nhóm: `city/noble → city`, `sect → sect`, còn lại (`village`, `bastard`, `ruins`) → `village`.

```
city   : pro_city_raid (KILL 5 Hư Thi, thành bị tập kích) → pro_city_road (REACH Lưu Ly Cảng, bỏ qua nếu đã gần)
sect   : pro_sect_talk (TALK Sư Phụ) → pro_sect_meditate (bấm phải Linh Khí Đài)
         → pro_sect_kill (KILL 5) → pro_sect_road (REACH Lưu Ly Cảng)
village: pro_vil_raid (KILL 5, làng bị tập kích) → pro_vil_road (REACH Lưu Ly Cảng, waypoint đường)
                      │
 hội tụ:  pro_meet (TALK Mộ Dao) → pro_altar (bấm phải Ấn Hồn Đài) → CHƯƠNG 1
```

- Raid kịch bản: sinh `n+2` Hư Thi (tag `hk_flow`, chủ sở hữu = người chơi) quanh người chơi; mỗi giây kiểm tra, thiếu thì bổ sung (đảm bảo hoàn thành được).
- KILL đếm khi người chơi giết (hoặc thực thể gắn tag bị giết gần người chơi). Hoàn thành → bước kế → objective mới.
- Dùng cùng `InvasionManager` ở chương 5 (thật), chương 0 dùng đợt nhỏ riêng để không phụ thuộc thành đã dựng.

## 4. Mộ Dao — các nhịp

| Nhịp | Chương | Nội dung | Cơ chế |
|------|--------|----------|--------|
| Gặp | 0 | Cứu/đón người chơi, dẫn tới Ấn Hồn Đài | node `pro_meet` |
| Dạy | 1 | Lore Hư Không, nhận kỹ năng Phàm | node `ch1` (cũ) |
| Dặn dò | 2–5 | Banner dòng chat gợi ý hệ thống kế tiếp | `hukhong.flow.ch.N.lX` |
| Thú nhận | 6 | Cha mẹ là Sứ Đồ; chọn tin/nghi (`trust_mo_dao`/`doubt_mo_dao`) | node `ch4` (cũ, nay ở bước `c6_modao`) |
| Rời đi / hy sinh | 9 | Theo lựa chọn chương 6 (S1 §7.3) | node `ch9_leave` / `ch9_sacrifice` |
| Cuối | 10 | Node cũ `ch5` (cấp 99) và `ch6_*` (kết thúc) | dialogue cũ |

## 5. Đối thủ định kỳ — Hạng Chiêu

| Chương | Gặp | Hình thức | Kết quả |
|--------|-----|-----------|---------|
| 2 | Hắc Thạch Quan | Thoại (node `ch2` cũ) | `c2_rival`; cùng điều tra Hư Tu mang tín vật tông |
| 5 | Xuất hiện cạnh người chơi (`hang_chieu_r`) | Thoại → **đấu** (SuDo Dạ Kiếm gắn tên + tag `hk_rival`, cấp = cấp người chơi) | Rút lui khi còn 20% máu → thưởng EXP, hoàn thành `c5_rival` |
| 8 | Trên đường Kim Sa | Như trên; hai lựa chọn đặt cờ `rival_saved` / `rival_lost` | Cờ quyết định phe ở chương 10 (kéo về / thành Thượng Sứ, S1 §7.3) |

Nếu NPC bị hủy/lạc, hệ thống tự sinh lại gần người chơi (poll 5 giây).

## 6. Mục tiêu & dữ liệu

- `Objectives.set(p, stepId, text, target, dim)` mỗi khi vào bước/đếm tiến độ; `target` lấy từ API công khai: `CityService.site`, `CitySavedData.altarPos`, `SectEvents.site`, `ChgService.lairXZ`, `WorldTierData.gateway`.
- Lưu trạng thái trong `QuestData.flags`: `flow2`, `step:<id>`, `prog:<n>`, `d:<id>` (đã xong), `flow_done`. `QuestData.chapter` = 0..10 (`LAST_CHAPTER` 10).
- Dialogue JSON có thêm điều kiện `"steps": [...]` và hành động `step_done`, `duel`; hành động `chapter` cũ thành no-op (chương theo cảnh giới).
- Lang: `hukhong.flow.{pro.<group>,ch.N,end}.{title,sub,l1..l3}`, `hukhong.flow.obj.<stepId>`; thoại `hukhong.dlg.*` (xem `docs/research/lang-q2.txt`).

## 7. Di trú save cũ

Save Tuyến truyện chưa có cờ `flow2` (đăng nhập):
1. Chương mới = `chapterFor(level, locked)`.
2. Cờ cũ → bước đã xong: `ch1_done→c1_talk`, `ch2_done→c2_rival`, `ch3_done→c4_hua_co`, `ch4_done→c6_modao`.
3. Vào bước đầu của chương đó, tự bỏ qua các bước đã thỏa; **không** phát banner.
4. Điều kiện thoại cũ `chapter_min`: 3→4 (Hứa Cổ), 4→6 và 5→10 (Mộ Dao).

Bảng ánh xạ chương cũ → mới:

| Cũ | Ý nghĩa | Mới |
|----|---------|-----|
| 1 | Gặp Mộ Dao | 1 |
| 2 | Cấp 10, Hạng Chiêu | 2 |
| 3 | Cấp 30, Hứa Cổ | 4 |
| 4 | Cấp 50, Mộ Dao thú nhận | 6 |
| 5 | Săn Chấp Hành Giả / cấp 99 | 7–10 |
| 6 | Kết thúc | 10 (`c10_end`) |

## 8. Kiểm chứng & tồn đọng

- GameTest (`gametest/StoryFlowGameTests`): mở đầu city/village/sect đặt objective; Tự do không objective; mở khóa mốc 10 → chương 2 + objective `c2_sect`; save cũ di trú theo cấp.
- Chưa kiểm bằng real-run (AP4): cần chơi thử một lượt mở đầu thật và trận đấu Hạng Chiêu.
- Chưa làm: NPC Mộ Dao đi cùng thật (hiện là NPC trong thành + banner), tố cáo nội gián tông (S1 §11-3), cảnh cắt Dạ Khuyết (chỉ banner chương 10).

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | DESIGN-v1-story-flow.md (this file) | ✅ Current |
