# REVIEW-v1-tu-tien-mod-implementation-gap

**Created**: 2026-10-29
**Research Target**: So sánh toàn bộ kế hoạch ban đầu và các vòng phản hồi người chơi với mã nguồn hiện tại (D:/mc_mod, commit 248a6e2)
**Mode**: Planning
**Level**: L3-Analyze
**Language**: vi
**Context**: DESIGN-v2-master, PLAN-v2-implementation, PLAN-v2-canon, TASK-v1-journey, DESIGN-v1-story-flow, DESIGN-v2-quest-chains, PLAN-v1-vfx-boss-mob-scaling, PLAN-v2-shader-resourcepack, cùng 36 yêu cầu phản hồi trong chat
**Status**: Completed
**Researcher**: Claude Agent (deep-research skill). 3 agent kiểm tra song song (A/B/C); controller tổng hợp.

---

## Key Findings

| # | Finding | Chi tiết | Bằng chứng | Độ tin cậy |
|---|---------|----------|------------|------------|
| 1 | **Khung chính đã chạy được** | 252 hạng mục: 115 DONE, 91 PARTIAL, 38 MISSING, 8 CHANGED. Truyện chương 0–10 và 4 chuỗi nhiệm vụ đều hoàn thành được. 140 key hội thoại có đủ vi/en | AUDIT-A/B/C | CAO |
| 2 | **Có 1 soft-lock cứng** | Mắc Kẹt (1% khi chọn ô 4 ở Ấn Hồn Đài) không có quest "Phá Chấp" để gỡ, chỉ lệnh debug `core_unstuck` gỡ được | AUDIT-A #1, AUDIT-B SL1 | CAO |
| 3 | **Tiến trình cuối game bị nghẽn** | Không có Phá Cảnh Đan bậc 7–9 và Thông Thiên Thần Đan (`FreeQuest.pillFor` trả về null). Qua cấp 69/79/89 chỉ còn đường EXP ×0.25 | AUDIT-A #2, AUDIT-B #3 | CAO |
| 4 | **Nhịp EXP lệch thiết kế** | Chưa áp hệ số EXP theo tư chất (D 0.6 → SSS 1.6), chưa phạt đa đạo, chưa có bù tư chất, chưa có Tụ Linh Đan | AUDIT-A #3, AUDIT-B #2, AUDIT-C #20 | CAO |
| 5 | **Thế giới phản ứng hẹp** | Hư Không chỉ đánh thành #0 (Lưu Ly Cảng); thành đã sụp không chiếm lại được; Thành Chủ không có quyền | AUDIT-A #4–5, AUDIT-B #4, SL4 | CAO |
| 6 | **Hệ đan thiếu khoảng 70%** | Thiết kế ~45 loại, code ~13. Thiếu Tẩy Tủy, Thanh Tâm, Tịnh Hư, Tẩy Hồn, nên không đổi được Lõi và không chữa được Tâm Ma / Ác Tính. `PillDrops` bị `SpawnRules` vô hiệu | AUDIT-A #8–9, AUDIT-B #8 | CAO |
| 7 | **Boss kiểu Elden Ring chưa làm** | Chuyển phase chỉ có animation + chữ "Phase N". Chưa có cutscene, phụ đề, bất tử khi chuyển phase, đấu trường biến đổi. Chưa có boss biển | AUDIT-C #29–30 | CAO |
| 8 | **Lựa chọn trong truyện ít hệ quả** | 8 cờ lựa chọn không có tác dụng (`rival_saved/lost`, `mo_dao_left/died`…). Hai kết thúc chỉ khác chữ. Thiếu Bia Công Trạng | AUDIT-B #5, #10, AUDIT-A #7 | CAO |
| 9 | **Nhiều fix gần đây chưa có test** | Bí Cảnh (EXP ×2, cấp, ra khỏi hang, chuỗi bản đồ), DrySites, Prebuild, buff Thể, phím G, ModDrops, lính thủ thành, tháp Thổ (đang lỗi không hiện) | AUDIT-C #2–3, #21–28 | CAO |
| 10 | **Hiển thị còn sót** | 44 lang key bị thiếu (bản đồ hiện mã thô). Shader: `AmbientPost` chưa xét `ShaderCompat`. Bấm marker trên bản đồ chưa dịch chuyển được | AUDIT-A #10, AUDIT-C #4, #31 | CAO |

**Không phát hiện AP1 nặng:** không có event subscriber, payload, keybind hay screen nào bị viết mà không nối vào game. Các mục PARTIAL chủ yếu thuộc 3 loại: thiếu test, thiếu một phần spec, hoặc số liệu lệch tài liệu.

---

## Table of Contents
1. [Scan Scope & Stats](#1-scan-scope--stats)
2. [Overview](#2-overview)
3. [Gap Map (MECE)](#3-gap-map-mece)
4. [Soft-lock Risks](#4-soft-lock-risks)
5. [Inversion](#5-inversion)
6. [Priority — RICE](#6-priority--rice)
7. [Roadmap đề xuất](#7-roadmap-đề-xuất)
8. [UC5 Viewpoints](#8-uc5-viewpoints)
9. [Kết luận + Synthesis Diagram](#9-kết-luận--synthesis-diagram)
10. [Sources, Lineage, Related, Token](#10-sources--references)

---

## 1. Scan Scope & Stats

| Nhóm | Tài liệu | Hạng mục | DONE | PARTIAL | MISSING | CHANGED |
|---|---|---|---|---|---|---|
| A | DESIGN-v2-master, PLAN-v2-implementation, TASK-v1 | 107 | 46 | 39 | 17 | 5 |
| B | PLAN-v2-canon, story flow, quest chains | 109 | 51 | 36 | 19 | 3 |
| C | PLAN-v1-vfx-boss, PLAN-v2-shader, 36 yêu cầu phản hồi | 36 | 18 | 16 | 2 | — |
| **Tổng** | 8 tài liệu + phản hồi | **252** | **115 (46%)** | **91 (36%)** | **38 (15%)** | **8 (3%)** |

**Tiêu chí DONE:** có code, có callsite từ gameplay (đã grep), và có test (unit hoặc GameTest).

**Phương pháp:** đọc code và grep callsite. Trong lần kiểm tra này **không chạy** build hay test. Lần build gần nhất (commit 248a6e2) có 112/112 GameTest pass.

```
DONE     ███████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░  46%
PARTIAL  ██████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  36%
MISSING  ███████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  15%
CHANGED  █░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   3%
```

---

## 2. Overview

```
                     HƯ KHÔNG TU TIÊN LỤC — trạng thái theo trụ cột
┌──────────────────┬──────────────────┬──────────────────┬──────────────────┐
│ CORE LOOP        │ CONTENT          │ WORLD            │ PRESENTATION     │
│ tu luyện/chiến   │ truyện/quest/đan │ thành/Hư Không   │ VFX/boss/UI      │
├──────────────────┼──────────────────┼──────────────────┼──────────────────┤
│ 5 đạo + bảo vật  │ Ch0–10 xong ✅    │ 17 khu prebuild ✅│ VFX nền + kỹ năng │
│ bản mệnh ✅       │ 4 chuỗi quest ✅  │ đột kích ✅       │ sách ✅           │
│ Nội tại + K ✅    │ Lựa chọn ít hệ   │ Chỉ thành #0 bị  │ Boss phase ❌     │
│ EXP lệch thiết kế│ quả ⚠            │ đánh ⚠           │ Shader compat ⚠  │
│ Đan 7-9 thiếu ❌  │ Đan ~13/45 ❌     │ Thành Chủ rỗng ❌ │ 44 lang key ❌    │
│ Mắc Kẹt kẹt ❌    │ Kết thúc giống ⚠ │ Không chiếm lại ❌│ Boss biển ❌      │
└──────────────────┴──────────────────┴──────────────────┴──────────────────┘
```

---

## 3. Gap Map (MECE)

Bảng chi tiết từng dòng nằm trong 3 file AUDIT (xem §10). Dưới đây là các gap đã gộp và bỏ trùng giữa 3 nhóm.

### 3.1 CORE LOOP (tu luyện / chiến đấu)

| ID | Gap | Status | Bằng chứng | Tác động |
|---|---|---|---|---|
| G1 | Mắc Kẹt không có quest Phá Chấp | MISSING | AUDIT-A #1, AUDIT-B SL1 | Soft-lock cứng |
| G2 | Phá Cảnh Đan bậc 7–9 + Thông Thiên Thần Đan | MISSING | `FreeQuest.pillFor` → null (AUDIT-A #2) | Nghẽn ở cấp 69/79/89 |
| G3 | Hệ số EXP theo tư chất, phạt đa đạo, bù tư chất; công thức Bình Cảnh | PARTIAL | AUDIT-A #3, AUDIT-B #2 | Nhịp 110h/500h sai |
| G4 | Tụ Linh Đan (đan EXP) | MISSING | AUDIT-C #20 | Người chơi đã yêu cầu |
| G5 | Cấp 100 không bị khóa theo truyện; thiếu Mảnh Thiên Môn | PARTIAL | AUDIT-A #6 | Bỏ qua được cốt truyện |
| G6 | Tháp Thổ không xuất hiện; Thổ yếu | BUG | Phản hồi người chơi; agent đang làm RCA | Một hệ Pháp hỏng |
| G7 | Cân bằng: Kiếm vượt trội ở cấp 100 (Bản Mệnh ×3.0 + Thần) | PARTIAL | balance_sim, REPORT-v1-linhbao | Các đạo khác đạt ~82% |

### 3.2 CONTENT (truyện / quest / đan)

| ID | Gap | Status | Bằng chứng |
|---|---|---|---|
| G8 | 8 cờ lựa chọn không có hệ quả (`rival_*`, `mo_dao_*`…) | PARTIAL | AUDIT-B #5 |
| G9 | Hai kết thúc chỉ khác chữ; Tầng II chỉ là hệ số toàn server; thiếu Bia Công Trạng | PARTIAL | AUDIT-A #7, AUDIT-B #10 |
| G10 | Thiếu nhóm đan Tẩy Tủy, Tăng Tu Vi, Thanh Tâm, Tịnh Hư, Độc Đan, Thiên Địa Linh Vật, Tẩy Hồn | MISSING | AUDIT-A #8, AUDIT-B #8 |
| G11 | `PillDrops` chết vì `SpawnRules` chặn zombie/skeleton | PARTIAL (AP1) | AUDIT-A #9 |
| G12 | Xuất thân chỉ đổi điểm spawn: thiếu Thư Tiến Cử, debuff Con Hoang; tuổi khởi đầu ngẫu nhiên | PARTIAL | AUDIT-B #9 |
| G13 | Hệ manh mối bị bỏ qua (HUD chỉ thẳng hang); CHG III/IV không có manh mối; Văn Kiện hiếm | PARTIAL | AUDIT-B #10, SL5 |
| G14 | Objective sai ở mốc 95/99 và sau Tẩu Hỏa | BUG | AUDIT-B SL2, SL3 |

### 3.3 WORLD (thành / Hư Không / multiplayer)

| ID | Gap | Status | Bằng chứng |
|---|---|---|---|
| G15 | Đột kích / công thành chỉ nhắm thành #0 | PARTIAL | AUDIT-A #5, AUDIT-B #4 |
| G16 | Thành sụp không chiếm lại được (hub Lưu Ly Cảng thì soft-lock mềm) | MISSING | AUDIT-B SL4 |
| G17 | Thành Chủ: thuế, ngân khố, trục xuất, bầu cử; chức vị thành không có quyền lợi | MISSING | AUDIT-A #4, AUDIT-B #9 |
| G18 | Scaling multiplayer lệch spec (8 vs 10 người, bán kính 64 vs 48, quy tắc 5% đóng góp, roll loot chung) | PARTIAL | AUDIT-A #9, AUDIT-B #7 |
| G19 | Boss biển và quái biển | MISSING | AUDIT-C #30 |
| G20 | Vùng xâm thực chưa có nhạc riêng | PARTIAL | `LiberationManager.java:58` |
| G21 | Hang CHG IV có thể nằm ngoài world border (~12.000 block tính từ gốc tọa độ) | RISK | AUDIT-B |

### 3.4 PRESENTATION (VFX / boss / UI)

| ID | Gap | Status | Bằng chứng |
|---|---|---|---|
| G22 | Boss chuyển phase kiểu Elden Ring (PLAN-v1 B1–B7) | MISSING | AUDIT-C #29 |
| G23 | Hiệu ứng các hệ Pháp na ná nhau | BUG | Phản hồi; agent đang làm |
| G24 | Shader: `AmbientPost` chưa xét `ShaderCompat` (PLAN-v2 A2/B); chưa chạy ma trận Iris | PARTIAL | AUDIT-C #4 |
| G25 | 44 lang key bị thiếu (bản đồ nhỏ / bản đồ lớn hiện mã thô) | MISSING | AUDIT-A #10 |
| G26 | Bấm marker trên bản đồ lớn chỉ đặt waypoint, không dịch chuyển | PARTIAL | AUDIT-C #31 |
| G27 | Mở khóa tính năng dần (R21); pixel font | MISSING | AUDIT-A #10 |
| G28 | Chưa có ảnh kiểm tra VFX / shader (`docs/vfx-review`, `docs/shader-review` trống) | PARTIAL | AUDIT-C #10 |

### 3.5 QUALITY (test / tài liệu)

| ID | Gap | Status |
|---|---|---|
| G29 | Thiếu GameTest cho: Bí Cảnh (EXP ×2, cấp, thoát hang, chuỗi bản đồ), DrySites, Prebuild, TheBody, StanceHands, ModDrops, Defend của lính | PARTIAL |
| G30 | Chưa thử multiplayer trên 2 client | MISSING |
| G31 | Header TASK-v1 vẫn ghi "Code ⬜ 0/7" (AP11 tracker drift) | PARTIAL |

---

## 4. Soft-lock Risks

```
┌────────┬──────────────────────────────────────────────┬──────────┬─────────────┐
│ ID     │ Kịch bản                                     │ Mức      │ Fix         │
├────────┼──────────────────────────────────────────────┼──────────┼─────────────┤
│ SL1/G1 │ Mắc Kẹt ở Ấn Hồn Đài (1%)                    │ CỨNG     │ quest Phá   │
│        │                                              │          │ Chấp        │
│ SL2/G14│ Tẩu Hỏa reset nhân vật, truyện thì không     │ TRUNG    │ reset story │
│        │ → objective sai                              │          │ / re-route  │
│ SL3/G14│ Mốc 95/99 khóa EXP, chương 10 không chỉ      │ TRUNG    │ step trỏ    │
│        │ về Ấn Hồn Đài                                │          │ altar       │
│ SL4/G16│ Lưu Ly Cảng sụp → hội thoại Mộ Dao nằm giữa  │ TRUNG    │ chiếm lại / │
│        │ quái cấp 55–85                               │          │ dời NPC     │
│ SL5/G13│ Văn Kiện hiếm → hội thoại Hứa Cổ / Mộ Dao    │ THẤP     │ nguồn khác  │
│        │ đóng                                         │          │             │
│ G2     │ Thiếu đan 7–9 → cày ×0.25                    │ MỀM      │ thêm đan    │
└────────┴──────────────────────────────────────────────┴──────────┴─────────────┘
```

---

## 5. Inversion

- **Giả định sai #1:** "Có code + test pass = xong."
  **Lật ngược:** tháp Thổ có code, có callsite (`PhapSkills` slot 1 → `PhapRuntime.summonTurret`) và build xanh, nhưng người chơi không bao giờ thấy tháp.
  **Chân lý:** cần test dạng "đi qua đường thật của người chơi" (cast qua `SkillService`), cộng ảnh chụp trong game (AP4).

- **Giả định sai #2:** "Đọc dữ liệu người chơi ở client là được."
  **Lật ngược:** `CULTIVATION` không đồng bộ xuống client. Lỗi này đã gây ra 3 bug trong phiên: tab Nội Tại Pháp trống, chiêu T không vận khí, cấp sai ở màn Hiệp Hội.
  **Chân lý:** client phải dùng `ClientCache.cultivation()` hoặc attachment có sync. Nên thêm kiểm tra tĩnh (grep CI) chặn `getData(CULTIVATION)` trong `client/`.

- **Giả định sai #3:** "Fix theo phản hồi là đủ."
  **Lật ngược:** mỗi vòng phản hồi chỉ chạm triệu chứng người chơi thấy. Audit tìm ra soft-lock (SL1–SL4) mà chưa người chơi nào báo.
  **Chân lý:** cứ khoảng 3 vòng phản hồi nên audit lại toàn bộ kế hoạch một lần.

- **Giả định sai #4:** "Kiểm tra bằng con số mô phỏng là đủ cân bằng."
  **Lật ngược:** sim báo Thổ ở mức 90–100% các hệ khác, trong khi thực tế phím E của Thổ gây 0 sát thương và tháp không hiện.
  **Chân lý:** sim chỉ đúng khi cơ chế đã chạy đúng trong game.

---

## 6. Priority — RICE

Reach = tỉ lệ người chơi gặp (%). Impact 1–5. Confidence = độ chắc của bằng chứng. Effort = số đợt agent (1 đợt ≈ 1 agent Sonnet).

| ID | Gap | Reach | Impact | Conf | Effort | RICE | Ưu tiên |
|---|---|---|---|---|---|---|---|
| G6 | Tháp Thổ + buff Thổ | 17% (1/6 hệ Pháp) | 5 | 100% | 0 (đang làm) | — | **ĐANG LÀM** |
| G25 | 44 lang key | 100% | 2 | 100% | 0.2 | 1000 | **P0** |
| G1 | Phá Chấp quest | 1% | 5 | 100% | 0.5 | 10 (cứng) | **P0** (soft-lock) |
| G14 | Objective 95/99 / Tẩu Hỏa | 30% | 4 | 90% | 0.5 | 216 | **P0** |
| G2+G4 | Đan 7–9 + Thần Đan + Tụ Linh Đan | 60% | 4 | 100% | 1 | 240 | **P0** |
| G3 | Hệ số EXP tư chất / đa đạo | 100% | 3 | 90% | 0.5 | 540 | **P1** |
| G11 | PillDrops chết | 100% | 2 | 90% | 0.2 | 900 | **P1** |
| G15+G16 | Đột kích mọi thành + chiếm lại | 80% | 4 | 90% | 1.5 | 192 | **P1** |
| G22 | Boss phase Elden Ring (khung + Hư Vương mẫu) | 90% | 5 | 100% | 3 | 150 | **P1** |
| G29 | Bộ GameTest còn thiếu | — | 3 | 100% | 1 | — | **P1** (chất lượng) |
| G24 | Shader compat (PLAN-v2) | 50% | 3 | 90% | 1.5 | 90 | **P2** |
| G10 | Nhóm đan còn thiếu | 70% | 3 | 100% | 2 | 105 | **P2** |
| G8+G9 | Hệ quả lựa chọn + 2 kết thúc | 40% | 4 | 80% | 2 | 64 | **P2** |
| G17 | Quyền Thành Chủ | 30% | 3 | 90% | 2 | 40 | **P3** |
| G18 | Scaling multiplayer theo spec | 20% | 3 | 90% | 1 | 54 | **P3** |
| G19+G20 | Boss biển + nhạc vùng xâm thực | 50% | 3 | 100% | 2 | 75 | **P3** |
| G26 | Bấm marker → dịch chuyển | 60% | 2 | 100% | 0.3 | 400 | **P2** (nhanh) |
| G12+G13 | Xuất thân, manh mối | 50% | 2 | 80% | 1.5 | 53 | **P3** |
| G27 | Mở khóa dần, pixel font | 100% | 1 | 70% | 1 | 70 | **P4** |

**Fermi effort tổng:** ~22 đợt agent. Với 2 agent song song mỗi lượt, khoảng 11 lượt. Mỗi lượt Sonnet tốn ~200–350k token, tổng khoảng 4–7M token. Cần chia theo tuần vì giới hạn sử dụng của người chơi.

---

## 7. Roadmap đề xuất

```
Lượt 1 (P0, sửa nhanh, không cần thiết kế)
  ├─ A: G25 44 lang key + G26 bấm marker dịch chuyển + G11 PillDrops + G31 header TASK
  └─ B: G1 quest Phá Chấp + G14 objective 95/99/Tẩu Hỏa
Lượt 2 (P0 tiến trình)
  ├─ A: G2+G4 đan bậc 7–9, Thông Thiên Thần Đan, Tụ Linh Đan (công thức + rơi + nhiệm vụ)
  └─ B: G3 hệ số EXP tư chất / đa đạo / bù tư chất (+ chạy lại balance_sim)
Lượt 3 (P1 thế giới + chất lượng)
  ├─ A: G15+G16 đột kích mọi thành + nhiệm vụ chiếm lại thành
  └─ B: G29 bộ GameTest còn thiếu (Bí Cảnh, DrySites, Thể, phím G, drops, lính)
Lượt 4–6 (P1 boss)
  └─ G22 khung boss (BossPhaseDirector, camera cutscene, phụ đề, nhạc lớp, gãy thế)
     → Hư Vương mẫu → người chơi thử → các boss còn lại
Lượt 7+ (P2–P4)
  └─ Shader (PLAN-v2) → đan còn thiếu → hệ quả lựa chọn + kết thúc → Thành Chủ
     → boss biển → multiplayer → xuất thân / manh mối → mở khóa dần
```

---

## 8. UC5 Viewpoints

| Góc nhìn | Mối quan tâm chính | Kết luận |
|---|---|---|
| Người chơi mới (cấp 1–30) | Không kẹt, hiểu mục tiêu, chiến đấu đã tay | Ổn. Truyện hoàn thành được, VFX đã nâng. Còn lỗi tháp Thổ và lang key |
| Người chơi cuối game (60–100) | Có đường lên cấp, có thử thách và phần thưởng | **Yếu.** Thiếu đan 7–9, boss phase đơn điệu, kết thúc giống nhau |
| Người chơi nhóm / server | Scaling, thành, chính trị | **Yếu.** Chỉ 1 thành bị đánh, Thành Chủ rỗng, chưa thử 2 client |
| Kỹ thuật (Engineering) | Nợ test, đồng bộ client | Trung bình. Nhiều fix thiếu test; lỗi `CULTIVATION` client lặp 3 lần |
| Tác giả nội dung (Design) | Lựa chọn có ý nghĩa, đan phong phú | Yếu. 8 cờ lựa chọn vô tác dụng; đan ~13/45 |

**Tổng hợp:** mod đã có vòng chơi đầu–giữa game hoàn chỉnh và nhiều hệ thống độc đáo. Rủi ro lớn nhất là **cuối game và tính bền**: một soft-lock cứng, nghẽn tiến trình ở cấp 69+, và nhiều sửa chữa chưa có test giữ lại. Nên dành 2 lượt P0 để "bít lỗ" trước khi làm tính năng lớn (boss phase, shader).

---

## 9. Kết luận + Synthesis Diagram

```
┌──────────────────── HƯ KHÔNG — TỪ KẾ HOẠCH ĐẾN GAME ────────────────────────┐
│                                                                            │
│  8 tài liệu kế hoạch + 36 phản hồi ──→ 252 hạng mục                        │
│                                         │                                  │
│            ┌────────────────────────────┼────────────────────────────┐     │
│            ▼                            ▼                            ▼     │
│      DONE 46%                     PARTIAL 36%                 MISSING 15%  │
│   (core loop, truyện       (thiếu test, lệch số,        (đan 7–9, Phá Chấp,│
│    ch0–10, VFX, bảo vật)    1 thành, lựa chọn)           boss phase, biển) │
│            │                            │                            │     │
│            └──────────────┬─────────────┴──────────────┬─────────────┘     │
│                           ▼                            ▼                   │
│             Soft-lock SL1–SL4 (G1, G14, G16)    Nghẽn cuối game (G2–G4)    │
│                           │                            │                   │
│                           └─────────────┬──────────────┘                   │
│                                         ▼                                  │
│              P0 Lượt 1–2: bít lỗ  ──→  P1 Lượt 3–6: thế giới + boss        │
│                                         ──→  P2+: shader, đan, kết thúc    │
│                                                                            │
│  Bài học: test theo đường người chơi + ảnh chụp (Inversion #1)             │
│           client không đọc CULTIVATION (Inversion #2)                      │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Sources & References

### Source 1: Local
| File | Relevance |
|---|---|
| docs/research/AUDIT-A-design-impl.md | 107 dòng chi tiết, có file:line |
| docs/research/AUDIT-B-canon-story.md | 109 dòng + soft-lock |
| docs/research/AUDIT-C-vfx-boss-feedback.md | 36 yêu cầu phản hồi |
| src/main/java/com/hukhong/** | Mã nguồn được đối chiếu |

### Source 2 / 3
Không dùng (đây là kiểm tra nội bộ codebase). Citation Mandate không áp dụng.

## Document Lineage
| Version | Document | Focus | Status |
|---|---|---|---|
| v1 | REVIEW-v1-tu-tien-mod-design.md | Review thiết kế trước khi code | Superseded (khác phạm vi) |
| **v1** | **REVIEW-v1-tu-tien-mod-implementation-gap.md** | **Gap giữa kế hoạch và code** | ✅ Current |

## Related Documents
| Document | Relationship |
|---|---|
| PLAN-v1-vfx-boss-mob-scaling.md | Nguồn của G22, G7 |
| PLAN-v2-shader-resourcepack.md | Nguồn của G24 |
| TASK-v1-tu-tien-mod-journey.md | Cần cập nhật header (G31) |

**Suggested next:** `PLAN-v3-tu-tien-mod-gap-closure.md` (biến §7 thành task tracker).

## Token Summary
```
Files scanned:    ~350 Java + 8 docs (qua 3 agent)
Agents:           3 × Sonnet (~890k token) + controller tổng hợp
External queries: 0
Output:           ~6k token
Budget status:    HIGH (do quy mô repo)
```
