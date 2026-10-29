# TASK-v2-world-story-backlog

**Last Updated**: 2026-10-29 — Gom toàn bộ việc đã chốt, làm khi người chơi nói "bắt đầu"
**Status**: ⬜ Chưa bắt đầu (0/8 đợt)
**Nguồn**: PLAN-v2-world-story-integration.md (§2–§12), PLAN-v1-world-enrichment.md, REVIEW-v1 (gap còn mở)
**Luật mỗi đợt**: agent không commit; controller build + GameTest + ảnh chụp + merge_lang + commit/push; đủ DoD 5 mục (PLAN-v2 §8) mới ✅

```
ĐỢT 1  S0+W0  Nối cốt truyện + Lịch K        ⬜  (người chơi đã đồng ý làm trước)
ĐỢT 2  T1     Ấn Hồn Đài / Khu Chợ / Uy Danh  ⬜
ĐỢT 3  T2     Hồn Tinh vào ví + giá nâng cấp  ⬜
ĐỢT 4  T3     Vùng xâm thực + 8 Hư Vương      ⬜
ĐỢT 4b B      Cutscene + biến hình + chiêu mới ⬜
ĐỢT 5  S1+W1  Sự kiện có phe, tập kích, PvP   ⬜
ĐỢT 6  S2/S3  Cung truyện 7 thành + Siêu Độ   ⬜
ĐỢT 7  W2–W8  Phần còn lại PLAN-v1            ⬜
```

## Đợt 1 — S0 + W0: Nối cốt truyện + Lịch K
- [ ] `story/world/StoryHooks`: `allowed()` (chương, cờ, chế độ Tự do bỏ lọc) + `complete()` (đặt cờ `wl.*`, trao Văn Kiện, xếp lời Mộ Dao 1 lần)
- [ ] Trường `lore` chung cho JSON: faction (6 phe), chapter, requires_flags, sets_flags, clue, rumor, modao_line
- [ ] Văn Kiện bộ C "Di thư Tiền Nhân" ×10 (lore Dạ Khuyết = Thủ Hộ Giả, 4 CHG = 4 đệ tử) + mở rộng A/B lên 10 mỗi bộ
- [ ] Lịch Thiên Cơ (tab K): **1 ngày Lịch = 4 ngày Minecraft (80 phút)**, tuần = 7 ngày Lịch; 2 làn **Thế giới** + **Thiên Cơ Dự Ngôn** (sự kiện truyện chương hiện tại, màu tím)
- [ ] Nút **"Đến"** dịch chuyển miễn phí (1 lần / sự kiện / người, không khi đang đánh)
- [ ] Báo trước 2 phút + 30 giây; hiện phe + Hư Ảnh vùng
- [ ] Khung event JSON `data/hukhong/hukhong/events/*.json`
- [ ] Thanh **Hư Ảnh** theo vùng (lưu trong CorruptionData), hiện trên Lịch
- [ ] JUnit: mọi `requires_flags` có nguồn đặt; GameTest: sự kiện → cờ → Văn Kiện → Mộ Dao nói

## Đợt 2 — T1: Ấn Hồn Đài + Khu Chợ + Uy Danh
- [ ] Ấn Hồn Đài to gấp ~2 (bán kính ~18), 3 bậc thềm, 8 trụ lửa hồn, vòng sáng xoay, hồ + cầu; không thương nhân trong 32 block
- [ ] Khu Chợ riêng mỗi thành (40–70 block từ tâm, đất khô), cổng chào + sạp, điểm dịch chuyển riêng; sửa CityMarket + CityProtection
- [ ] Chợ chỉ bán đồ mod + vật phẩm tiến hóa (bản mệnh / Linh Bảo / Linh Trượng)
- [ ] Uy Danh Chợ 5 bậc theo Tử Tinh đã tiêu (giảm 5% / 10%, bậc 4 Quầy Đặc Biệt: Hồn Cốt xoay vòng + Trục Nâng Cấp Kỹ Năng; bậc 5 hàng cao)
- [ ] Vật phẩm mới **Trục Nâng Cấp Kỹ Năng** (+1 cấp kỹ năng; rơi hiếm Bí Cảnh cấp cao)
- [ ] Thanh Uy Danh trong tab K + thông báo lên bậc

## Đợt 3 — T2: Hồn Tinh thành tiền trong ví
- [ ] Số dư Hồn Tinh trong `currency/Wallet`; `Wallet.grantHonTinh()` + popup icon
- [ ] Đổi ~8 chỗ rơi (ChapHanhGia:479, HuVuong:312, WorldBoss:354, VoidRaid:301, InvasionManager:230, DaKhuyet:168, NguyenThuyTichDiet:438, BiCanhRewards:48) + ShopCatalog
- [ ] Tự đổi Hồn Tinh trong túi / save cũ vào ví; hiện trong K
- [ ] `UpgradeCost` chung: `base × n^1.6`, ×1.5 mỗi mốc 10 — áp cho bản mệnh, Linh Bảo, Linh Trượng, Hồn Cốt

## Đợt 4 — T3: Vùng xâm thực + Hư Vương
- [ ] Tạo map: 8–14 vùng, bán kính ngẫu nhiên 120–700, xa spawn thì to hơn, không đè thành; chỉ đánh dấu dữ liệu, biến đổi khi chunk nạp
- [ ] Cấp quái theo bán kính: 20–40 / 40–65 / 65–**85**
- [ ] Vào vùng → tự nhận nhiệm vụ "Giải Phóng", mục tiêu trỏ tâm vùng; hiện trên bản đồ + Lịch
- [ ] Hạ boss → vùng sạch dần (dùng Liberation), giảm Hư Ảnh, thưởng Hồn Tinh + Văn Kiện/Di thư
- [ ] **8 dạng Hư Vương** trên 4 khung model (khổng lồ / thú 4 chân / rắn / linh thể): Huyết Ma, Cốt Long, Mộc Yêu, Băng Phách, Sa Trần, Độc Chướng, Hải Uyên, **Thiên Diện** (vùng ≥600, 3 phase)
- [ ] Mỗi dạng 2–3 chiêu riêng + VFX riêng (FxElem)
- [ ] Tên riêng ngẫu nhiên theo seed: `[Danh hiệu] Tên dạng — Tên riêng`; hiện bossbar / bản đồ / Lịch
- [ ] **Rơi Hồn Cốt 100%**: khe hợp dạng, phẩm theo kích thước (Thiên Diện phẩm cao nhất + 30% thêm cái thứ hai), mỗi người ≥10% sát thương nhận riêng, vùng đã giải phóng không rơi lại
- [ ] GameTest mỗi dạng chạy đủ chiêu + rơi đúng Hồn Cốt; ảnh chụp 8 dạng

## Đợt 4b — Khung boss: cutscene chuyển phase + biến đổi ngoại hình + chiêu mới
Thiết kế gốc: PLAN-v1-vfx-boss-mob-scaling §5 (B1–B7). Hiện chuyển phase chỉ hiện chữ "Phase 2" + 1 animation (`WorldBoss.enterPhase()`, `HuVuong.enterPhase()`). **Làm khung trước khi làm 8 Hư Vương** (Đợt 4 dùng khung này).
- [ ] `BossPhaseDirector`: Đánh → Chuyển phase (boss bất tử, khóa AI 3–6s) → Đánh; cấu hình JSON mỗi boss: ngưỡng HP, kịch bản cutscene, bộ chiêu, biến đổi ngoại hình, biến đổi đấu trường
- [ ] **Cutscene (client)**: camera keyframe (vị trí/góc/FOV, nội suy mượt), thanh đen điện ảnh, **phụ đề** (không lồng tiếng — người chơi đã chốt), bấm phím bỏ qua; người trong 60 block xem, mỗi người tự bỏ qua; server không khóa người chơi
- [ ] **Biến đổi ngoại hình** mỗi phase: đổi texture + glowmask, bật/tắt xương phụ kiện (sừng, cánh, giáp vỡ, mặt thứ 2/3), to ra (scale 1.0 → 1.25 → 1.5), hào quang / hạt riêng, đổi nhạc lớp
- [ ] **Chiêu mới mỗi phase**: phase 2 thêm 2 chiêu, phase 3 thêm 1 chiêu tối thượng (có vòng cảnh báo); giữ chiêu cũ nhưng nhanh hơn
- [ ] Biến đổi đấu trường: trời tím / mưa máu / sàn nứt / cột Hư Không mọc
- [ ] Áp dụng: **8 Hư Vương** (Thiên Diện đổi "mặt" mỗi phase), **4 Chấp Hành Giả**, **Dạ Khuyết** (+ cutscene Tâm Chiến của kết thúc Siêu Độ), **3 Yêu Vương** (Sa Hạt / Băng Nhân / Độc Mẫu), boss Bí Cảnh
- [ ] Cutscene giải phóng vùng: trời tím tan, ánh sáng trở lại
- [ ] GameTest: boss qua đủ phase, đúng chiêu mới từng phase, bất tử trong lúc chuyển; ảnh chụp từng phase
- Fermi: khung ≈ 1 lượt; áp cho ~17 boss ≈ 3 lượt → ≈ 1M token

## Đợt 5 — S1 + W1: Sự kiện có phe + tập kích + PvP
- [ ] Nguyệt Thực (Sứ Đồ chỉ huy từ ch.4), Thú Triều, Lôi Vũ (gần hang CHG), Yêu Vương theo lịch — đều có `lore`
- [ ] Tập kích **mọi thành**: 60% theo chương, 40% ngẫu nhiên trong thành đã mở dịch chuyển
- [ ] Thành RUIN → nhiệm vụ Tái chiếm (thay vì bỏ qua `c5_invasion`)
- [ ] **Đấu Trường Sinh Tử** ở Thiên Khải Hoàng Đô (mở sau khi tái chiếm): 2 bên cược Tử Tinh bằng nhau, đốt 5%, khán giả cược pari-mutuel, 3 hiệp × 3 phút, không chết / mất đồ, cấm đan hồi, cân bằng cấp tùy chọn, 3 trận / cặp / ngày Lịch, giải 8 người mỗi tuần Lịch

## Đợt 6 — S2 + S3: Cung truyện 7 thành + kết thúc Siêu Độ
- [ ] Huyết Nguyệt Phường "Chợ Máu" (ch.4) · Đan Hà "Hắc Triều" (ch.5) · Cổ Xảo "Đấu Giá Đẫm Máu" (ch.5–6) · Lạc Nhật "Lôi Vũ Viêm Sơn" (ch.7) · Thanh Vân "Đại Hội Tông Môn" (ch.8, duel Hạng Chiêu tại võ đài) · Bắc Hàn "Kiếm Trủng Tuyết" (ch.9) · Thiên Khải "Hoàng Đô Thất Thủ" (ch.10)
- [ ] Kết thúc **Siêu Độ** (`ending_truth`): cần 10 Di thư C + Hư Ảnh TB <30% + 4 CHG + đủ số vùng giải phóng; Tâm Chiến 4 tàn ảnh CHG; giữ 4 Mảnh 60s; Mộ Dao về / tàn hồn; Hạng Chiêu tỉnh; Thế Giới Tầng 2 "Linh Khí Phục Hưng" (Yêu Tộc trỗi dậy); danh hiệu + bản mệnh dạng Thủ Hộ (ngự bay đường dài)

## Đợt 7 — Phần còn lại PLAN-v1 (W2–W8), mỗi JSON có `lore`
- [ ] Địa điểm (Bia Đá, Khách Điếm + tin đồn, Động Phủ, Cổ Chiến Trường, dungeon theo đạo, Tiên Đảo vào bằng ngự kiếm…)
- [ ] Linh thảo, khoáng mạch, mùa; NPC sống, thiện cảm, nhiệm vụ ngẫu nhiên; linh sủng / thú chiến đấu (**không thú cưỡi**)
- [ ] Đấu Giá Hành (sàn + Đấu Giá Hội giờ thực, lô truyện, người mua bí ẩn = Sứ Đồ) + Phi Kiếm Truyền Thư
- [ ] Rà soát hiệu năng, cân bằng, ảnh chụp, multiplayer 2 client

## Đã bỏ / không làm
- Thú cưỡi (Tuyết Lang, Hỏa Kỳ Lân, Phong Điêu) — thay bằng ngự vũ khí bản mệnh sau này

## Tồn đọng cũ (REVIEW-v1, chưa xếp đợt)
- Mắc Kẹt soft-lock (Phá Chấp), đan bậc 7–9 / Thần Đan / Tụ Linh Đan, PillDrops chết, 44 key lang thiếu, click marker bản đồ để đi, boss phase cutscene, shader PLAN-v2

## Timeline
| Ngày | Việc | Kết quả |
|---|---|---|
| 2026-10-29 | Chốt PLAN-v2 + các bổ sung (commit 7ea85ca, a73fa47, 0b73bbe) | Backlog này |
