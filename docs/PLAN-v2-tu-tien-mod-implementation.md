# PLAN-v2-tu-tien-mod-implementation

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2: gộp toàn bộ thiết kế (canon §14 R1–R22, S1–S8) vào quy trình xây dựng, thêm luồng UI/UX + Animation/VFX song song luồng logic
**Status**: 📝 Chờ user duyệt các bước (§3) trước khi bắt đầu Bước 1
**Thay thế**: PLAN-v1-tu-tien-mod-implementation.md (Superseded)

---

## Key Findings

| # | Finding | Chi tiết | Độ tin cậy |
|---|---------|----------|------------|
| 1 | **Mỗi bước có 3 luồng**: Logic · UI/UX · Animation/VFX | Logic làm trước, UI nối vào, hiệu ứng placeholder trước rồi thay bản đẹp | CAO |
| 2 | **Placeholder trước, tài nguyên đẹp sau** | P0 dùng particle vanilla + model GeckoLib đơn giản; bản đẹp làm ở bước "Đánh bóng" của mỗi phase | CAO |
| 3 | **AI làm được**: code, JSON (particle, animation, model khối đơn giản), lang vi/en, âm thanh placeholder; **người nên làm**: texture/model đẹp (Blockbench), âm thanh chất lượng | S8 §5 | TRUNG BÌNH |
| 4 | **P0 = 9 bước, ~35–50 vòng build-test (≈6–9 tuần)** | Tăng so với v1 do thêm UI/animation | THẤP (ước lượng) |

---

## 1. Nguồn thiết kế cho từng luồng

| Luồng | Tài liệu |
|------|---------|
| Luật chung | PLAN-v2-tu-tien-mod-canon.md (§0–13, §14 R1–R22) |
| Logic | S2 (đạo), S3 (EXP/Lõi/Linh Khí Đài), S4 (thế giới/quái), S6 (xã hội), S7 (đan), S5 (kiến trúc + code mẫu) |
| UI/UX | S8 §1–3 (phong cách, màn hình + mockup, onboarding, accessibility, vi/en) |
| Animation/VFX | S8 §4–5 (kỹ năng 3+1 từng đạo, quái/boss, đột phá, pipeline Blockbench/GeckoLib) |
| Truyện | S1 |

## 2. Quy trình mỗi bước (multi-agent)

```
Opus: spec bước (logic + màn hình + hiệu ứng cần có, checklist thử)
  → Haiku: tra API chưa chắc ([KIỂM TRA])
  → Sonnet A: code logic + test           ┐ song song khi
  → Sonnet B: UI screen/HUD + lang vi/en  ├ khác package
  → Sonnet C: particle/animation JSON,    ┘
              model placeholder, âm thanh
  → Opus: review (canon, cổng gates, UI đã nối, không treo hàm)
  → USER: chạy game, làm checklist, xem UI/hiệu ứng → PASS/FAIL + ảnh chụp nếu xấu
```
Agent không commit/push trừ khi user cho phép.

---

## 3. CÁC BƯỚC — để user duyệt

### P0 — Bản chơi được đầu tiên (Đạo tu + Thể tu, cấp 1–30)

| Bước | Logic | UI/UX | Animation / VFX | User thấy được |
|------|------|-------|-----------------|----------------|
| **1. Khởi tạo** | Project NeoForge 1.21.1 + GeckoLib ở `D:\mc_mod`, git init, spike 5 API | Mod hiện trong danh sách Mods (logo, mô tả vi/en) | — | Game chạy, thấy mod |
| **2. Dữ liệu tu sĩ** | Attachment cấp/EXP/tài nguyên/tuổi, sync, lệnh debug | **HUD**: cảnh giới, thanh EXP, thanh tài nguyên theo màu đạo, tuổi | Hiệu ứng lên cấp (particle + âm) | HUD đúng, lên cấp có hiệu ứng |
| **3. Tạo nhân vật** | Roll tư chất D→SSS, đơn/song/tam năng, xuất thân cơ bản | **Màn tạo nhân vật** (bảng 4 đạo + tư chất, nút chọn) | Hiệu ứng roll tư chất (sáng theo bậc) | Vào thế giới mới → màn tạo nhân vật |
| **4. Luật đạo + kỹ năng** | Đạo tu (không đói, đòn thường 0 lên quái, buff hỗ trợ), Thể tu (cấm vũ khí, Bá Khí), cổng `gates`, spawner = 0, `KillRateGate` | **Ô kỹ năng 3+1** + cooldown, phím tắt, tooltip | **Mỗi đạo 2 kỹ năng + 1 tối thượng có hiệu ứng** (placeholder particle/beam), tư thế thiền, hào quang Bá Khí | Dùng kỹ năng thấy hiệu ứng; luật cấm đúng |
| **5. EXP + Bình Cảnh** | Đường cong EXP, khóa mốc, Bình Cảnh, Khai Mạch Đan + Ngưng Nguyên Đan | Thanh EXP đổi trạng thái "Bình Cảnh", tooltip đan | **Hiệu ứng đột phá** (cột sáng, rung màn hình nhẹ, âm) | Qua mốc 10 có cảm giác "phá cảnh" |
| **6. Ấn Hồn Đài** | 10 Lõi JSON, 4 ô (Phó Mặc Số Phận), roll seed lưu trước | **Màn Ấn Hồn Đài** 4 thẻ, màu theo phẩm; **Bảng tu sĩ** (chỉ số, Lõi) | **Lật thẻ** Phó Mặc Số Phận, vòng Lõi màu phẩm quanh người khi chọn | Chọn Lõi đẹp, xem lại trong Bảng tu sĩ |
| **7. Quái Hư Hóa** | 3 quái + loot Hồn Tinh + EXP | Thanh máu/tên quái có cấp | **Model + animation GeckoLib** (idle/walk/attack) bản placeholder, particle hư không | Gặp quái Hư Hóa ngoài tự nhiên |
| **8. Thành đầu tiên** | 1 thành có Ấn Hồn Đài, SavedData, lệnh tập kích thử | **Thông báo sự kiện** (title/bossbar), onboarding (hướng dẫn 5 bước cho người mới) | Hiệu ứng cổng tập kích | Đi từ spawn tới thành, được hướng dẫn |
| **9. Đánh bóng + nghiệm thu P0** | Sửa lỗi, cân số, multiplayer 2 client | Rà toàn bộ UI vi/en, cỡ chữ, màu cho mù màu | Thay placeholder quan trọng nhất (HUD, Ấn Hồn Đài, 3 quái) bằng bản đẹp | **Checklist P0 8/8 PASS** + bạn duyệt giao diện |

### P1 — Đủ 4 đạo, cấp 31–60, xã hội (tóm tắt)

| Bước | Nội dung chính | UI/UX | Animation/VFX |
|------|---------------|-------|---------------|
| 10 | Kiếm tu + Bản Mệnh Kiếm, Ma tu (Ác Tính, Huyết Tế quota) | Thanh Ác Tính, màn nhận Bản Mệnh Kiếm | Kiếm khí, ma khí, Mất Kiểm Soát |
| 11 | Đa đạo (chi phí ×2/×3, Phân Tâm), bay 4 đạo | HUD đa tài nguyên | **Ngự kiếm**, **cánh thiên sứ** (1 đôi), đạp không Thể tu |
| 12 | Đan Lô, luyện đan, Đan Độc, Phá Cảnh Đan 3–6 | **Màn Đan Lô** (minigame nhiệt) | Lửa lô, Dị Hỏa, thành đan / nổ lò |
| 13 | Tuổi/Thọ Nguyên đầy đủ, Ngũ Suy | Hiển thị tuổi/thọ, cảnh báo | Hiệu ứng già yếu |
| 14 | 10 thành + chức vị, 3–4 tông, **Linh Khí Đài**, nhiệm vụ tông | **Màn Tông môn**, **Thành/chức vị**, bảng nhiệm vụ | Thiền tại đài, linh khí tụ |
| 15 | Tập kích + Hư Hóa lan, boss scaling, 1–2 boss thế giới, worldborder | Bossbar, cảnh báo tập kích | Animation boss (phase), lan Hư Hóa |
| 16 | Đánh bóng P1 | Rà UI | Thay placeholder P1 |

### P2 — Cấp 61–99, cốt truyện (tóm tắt)
17 Hồn Cốt + Bản Mệnh skill · 18 7 tông đủ + Huyết Ảnh ẩn · 19 Sứ Đồ + Văn Kiện (**màn Nhật ký truyện**) · 20 4 Chấp Hành Giả (animation boss đầy đủ, giáng lâm) · 21 Bí Cảnh · 22 Tuyến truyện chính (hội thoại NPC) · 23 Đánh bóng P2 (cánh 2–3 đôi, kỹ năng cấp cao).

### P3 — Thăng Thần & kết thúc (tóm tắt)
24 Khảo Hạch (Tâm Ma, **Lôi Kiếp** VFX) · 25 Nguyên Thủy Tịch Diệt + dimension Hư Không Bản Nguyên · 26 Kết thúc Phong Ấn/Hấp Thụ (cutscene đơn giản) → Tầng Thế Giới II · 27 Cân bằng, tối ưu, đóng gói phát hành.

---

## 4. Definition of Done (mỗi bước)
1. Spec · 2. Code · 3. Test xanh · 4. **UI/phím/lệnh đã nối** (không hàm treo) · 5. **Bạn chạy thật PASS**, kể cả xem UI và hiệu ứng. Thiếu mục nào → 🔄 PARTIAL.

## 5. Điểm dừng
- Lỗi lặp 3 vòng → dừng, tìm nguyên nhân gốc.
- GeckoLib lỗi 2 vòng → tạm dùng model vanilla đổi texture.
- Một bước P0 vượt gấp đôi số vòng ước lượng → cắt hiệu ứng về placeholder, giữ logic.

## 6. Việc cần bạn trước Bước 1
1. Duyệt danh sách bước ở §3 (thêm/bớt/đổi thứ tự).
2. Cho agent commit sau mỗi bước không? Tên/email tác giả?
3. Xác nhận 4 giả định (tc, mắc kẹt, 15k, tẩu hỏa) + 3 câu mở của S8 (phím kỹ năng mặc định R/V/B/X, tắt hiệu ứng toàn màn hình ở server, font pixel).
4. Ai làm texture/model đẹp: bạn tự vẽ (Blockbench), thuê, hay để AI làm placeholder suốt?

## Document Lineage
| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-tu-tien-mod-implementation.md | Superseded |
| v2 | PLAN-v2-tu-tien-mod-implementation.md | ✅ Current |
