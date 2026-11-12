# PLAN-v1: Cách chế tạo của 3 nghề phụ (Luyện Đan · Phù Văn · Đoán Tạo)

**Created**: 2026-11-12
**Last Updated**: 2026-11-12 — Opus lập kế hoạch; Haiku thực hiện theo 4 bước
**Status**: 🔄 Đang làm
**Liên quan**: PLAN-v1-story-pills-professions.md (E, F), GUIDE-v1-professions.md

---

## Key Findings (hiện trạng, đã đọc code)

| # | Nghề | Hiện có | Thiếu | Bằng chứng |
|---|------|---------|-------|-----------|
| 1 | Luyện Đan | Đan Lô có **minigame giữ lửa** trong dải nhiệt 400 tick → phẩm Hạ/Trung/Thượng/Cực + Đan Độc | Sổ đan phương, học đan phương, nguyên liệu có phẩm, thất bại nổ lò, đan chiến đấu | `alchemy/AlchemyMath.java:7-50`, `DanLoBlockEntity.java` |
| 2 | Phù Văn | Phù Án: 3 ô + nút, ra phù **ngay lập tức** | Kỹ năng vẽ, phẩm chất, nguyên liệu riêng (chu sa, phù chỉ, linh mặc), thất bại | `profession/station/ProfessionStationBlockEntity.java:20-35` |
| 3 | Đoán Tạo | Đoán Tạo Đài: 3 ô + nút, ra giáp **ngay lập tức** | Nung – rèn – tôi, phẩm chất ảnh hưởng chỉ số, tinh luyện/nâng phẩm, sửa, phân rã | như trên |
| 4 | Chung | Công thức "biết sẵn" hết, không có UI xem công thức | Sổ công thức + học công thức + nút tự xếp nguyên liệu | — |

---

## 0. Nền tảng chung (làm trước, các bước sau dùng)

### 0.1 Phẩm chất sản phẩm (dùng chung 3 nghề)

| Phẩm | Màu | Hiệu lực |
|------|-----|---------|
| Hạ phẩm | xám | ×0.85 |
| Trung phẩm | trắng | ×1.00 |
| Thượng phẩm | xanh | ×1.15 |
| Cực phẩm | vàng | ×1.30 |

- Data component `hukhong:craft_quality` (int 0..3) + tên người chế `hukhong:crafter` (String) trên đan/phù/giáp. Tooltip hiển thị "Thượng phẩm · do <tên> chế".
- Đan: hệ số nhân hiệu quả; phù: sát thương/khiên ×hệ số, số lần dùng +0/+0/+1/+2; giáp: mọi chỉ số ×hệ số.
- Tái dùng enum `AlchemyMath.Quality` nếu khớp, không tạo enum trùng.

### 0.2 Sổ công thức & học công thức

- Attachment `KnownRecipes` (Set<String> id công thức, copyOnDeath).
- Công thức **cơ bản** (bậc 1 mỗi nghề) tự biết khi chọn nghề. Bậc cao học bằng **quyển phổ**: "Đan Phương", "Phù Phổ", "Khí Phổ" (item, chuột phải = học, tiêu hao; đã biết thì báo). Mỗi quyển phổ ghi 1 id công thức (data component).
- Nguồn quyển phổ: rơi từ boss/Bí Cảnh (bậc 2–5), mua ở cửa hàng nghề (bậc 2–3), phần thưởng lên cấp nghề (mỗi 4 cấp tặng 1 phổ ngẫu nhiên đúng nghề).
- **UI sổ công thức**: tab bên trái mọi màn trạm (Đan Lô, Phù Án, Đoán Tạo Đài): danh sách công thức đã biết (icon, tên, bậc, cấp nghề cần), công thức chưa đủ cấp xám đi; bấm 1 công thức → hiện nguyên liệu cần + nút **"Xếp nguyên liệu"** tự chuyển đồ từ túi vào ô (nếu đủ).
- Chưa biết công thức thì không chế được (kể cả khi xếp đúng nguyên liệu) → báo "Chưa lĩnh hội công thức".

### 0.3 Nguyên liệu mới (dùng chung)

| Item | Lấy ở đâu | Dùng cho |
|------|----------|---------|
| Chu Sa | Đập quặng redstone có 30% rơi thêm; mua cửa hàng | Phù |
| Phù Chỉ (Hạ/Trung/Thượng) | Chế ở bàn thường: giấy + Linh Thảo (Hạ), + Huyết Tham (Trung), + Hỏa Liên (Thượng) | Phù |
| Linh Mặc | Chu Sa + mực + Linh Thạch Hạ (bàn thường) | Phù |
| Tôi Dịch (Linh Tuyền Thủy) | Chai nước + Thanh Diệp (bàn thường) | Đoán Tạo (tôi) |
| Linh Thán (than linh) | Than + Linh Thạch Hạ | Nhiên liệu Đoán Tạo, Đan Lô |

Nguyên liệu có **phẩm** (Hạ/Trung/Thượng) thì phẩm trung bình nguyên liệu cộng thêm vào điểm phẩm chất sản phẩm.

---

## 1. Luyện Đan Sư — nâng cấp Đan Lô (giữ minigame lửa)

| # | Việc | Chi tiết |
|---|------|---------|
| 1.1 | Dải lửa theo cấp nghề | Nửa dải nhiệt `bandHalf` +1 mỗi 4 cấp nghề (cấp cao dễ giữ lửa) |
| 1.2 | Nổ lò | Lửa vượt 95 quá 3 giây → nổ lò: mất nguyên liệu, nổ nhỏ không phá khối (sát thương 4), khói đen. Cấp nghề ≥ 10 giảm còn 50% xác suất nổ |
| 1.3 | Phẩm chất | Kết quả phẩm hiện có + phẩm nguyên liệu + 1 bậc nếu cấp nghề ≥ bậc công thức × 3 (tối đa Cực) → ghi `craft_quality` |
| 1.4 | Đan chiến đấu (mới, dùng trong thế G) | **Hồi Khí Đan** (hồi linh lực/tài nguyên đạo 30%), **Cuồng Huyết Đan** (+15% sát thương 20s, sau đó Suy Yếu 10s), **Kim Cương Đan** (Kháng cự II 15s), **Phong Hành Đan** (hồi đầy Thể Lực + Tốc độ 10s). Bậc 2–4, hồi chiêu dùng chung nhóm 30s. Thêm vào tag `stance_consumable` |
| 1.5 | Sổ đan phương | Theo 0.2 cho mọi đan phương hiện có + mới |
| 1.6 | XP | Thành công: 5×bậc ×(1 + 0.25×phẩm); nổ lò: 1×bậc |

## 2. Phù Văn Sư — Phù Án có **minigame vẽ phù**

| # | Việc | Chi tiết |
|---|------|---------|
| 2.1 | Nguyên liệu | Ô 1: Phù Chỉ (phẩm), Ô 2: Linh Mặc, Ô 3: vật phẩm nguyên tố theo loại phù (giữ như hiện có) |
| 2.2 | Minigame | Bấm "Vẽ phù" → khung vẽ hiện **đường phù văn** (6–14 điểm theo bậc, nối thành nét). Người chơi **giữ chuột trái kéo theo nét** trong 6–12 giây. Độ chính xác = % quãng đường nằm trong ống ±R px (R lớn hơn theo cấp nghề). Lệch khỏi ống > 1 giây liên tục → **phù hỏng** |
| 2.3 | Kết quả | Chính xác ≥90% Cực, ≥75% Thượng, ≥55% Trung, ≥35% Hạ, <35% hỏng (mất Phù Chỉ + Linh Mặc, giữ vật phẩm nguyên tố). Phẩm Phù Chỉ +1 bậc nếu Thượng |
| 2.4 | Mẫu nét | 15 mẫu nét theo 15 loại phù, sinh từ seed theo id loại (ổn định), hiển thị xem trước trong sổ phù phổ |
| 2.5 | Server xác thực | Client gửi danh sách điểm đã vẽ (rút gọn ≤ 200 điểm) + thời gian; server tính lại độ chính xác (chống gian) |
| 2.6 | Vẽ hàng loạt | Cấp ≥ 12: tùy chọn "Vẽ nhanh" ra phẩm Trung không cần minigame, tốn gấp 1.5 nguyên liệu |
| 2.7 | XP | 4×bậc ×(1 + 0.25×phẩm); hỏng: 1 |

## 3. Đoán Tạo Sư — Đoán Tạo Đài có **nung – rèn – tôi**

| # | Giai đoạn | Chi tiết |
|---|----------|---------|
| 3.1 | Nung | Bỏ nguyên liệu + Linh Thán vào; thanh nhiệt tăng dần, người chơi bấm **"Rút phôi"** khi nhiệt nằm trong vùng vàng (vùng hẹp dần theo bậc). Rút sớm/muộn → trừ điểm |
| 3.2 | Rèn | Thanh chạy qua lại; bấm **"Đập"** khi kim nằm trong vùng sáng — N nhát (bậc 1: 5 nhát … bậc 5: 9 nhát), vùng sáng hẹp theo bậc, rộng theo cấp nghề. Mỗi nhát: Hoàn hảo / Tốt / Trượt |
| 3.3 | Tôi | Chọn **Nước** (an toàn, không thưởng), **Tôi Dịch** (+1 điểm phẩm), **Dầu thú** (rủi ro: 20% nứt → hạ 1 phẩm, 30% +2 điểm phẩm) |
| 3.4 | Kết quả | Điểm = nung + rèn + tôi + phẩm nguyên liệu → phẩm Hạ..Cực; Kim hệ bonus giữ như cũ (+1 bậc tier 30%) |
| 3.5 | Tinh luyện | Đặt giáp hộ thể + 2 phôi cùng bậc → nâng **+1..+5** (mỗi + thêm 4% chỉ số), thất bại từ +3 trở lên có thể tụt 1 |
| 3.6 | Sửa & phân rã | Sửa độ bền bằng Linh Thiết Phôi; phân rã giáp → trả 50% nguyên liệu |
| 3.7 | Server xác thực | Thời điểm bấm do client gửi kèm tick; server tự mô phỏng kim theo seed và chấm lại |
| 3.8 | XP | 20 + 15×bậc ×(1 + 0.25×phẩm); tinh luyện: 10×cấp + |

---

## 4. Thứ tự làm (Haiku làm, Opus review sau mỗi bước)

| Bước | Nội dung | Phụ thuộc | Chạy song song? |
|------|---------|-----------|-----------------|
| B0 | Nền tảng: §0.1 phẩm chất, §0.2 sổ công thức + quyển phổ + UI tab, §0.3 nguyên liệu | — | Không (làm trước) |
| B1 | Luyện Đan §1 | B0 | Có |
| B2 | Phù Văn §2 (minigame vẽ) | B0 | Có |
| B3 | Đoán Tạo §3 (nung – rèn – tôi, tinh luyện) | B0 | Có |
| B4 | Gộp, build, đẩy, chép jar | B1–B3 | — |

Mỗi bước: worktree riêng, `./gradlew build -q` phải qua, **không viết test**, không mở game; mọi tính năng phải có chỗ gọi thật (không code treo).

## 5. Rủi ro

| Rủi ro | Giảm thiểu |
|--------|-----------|
| Minigame client-side bị gian | Server chấm lại (2.5, 3.7) |
| 3 agent cùng sửa StationMenu/StationScreen | B0 tách sẵn khung tab + hook theo từng trạm; B2/B3 chỉ sửa màn của trạm mình |
| Người chơi cũ mất công thức "biết sẵn" | Lần đầu đăng nhập sau cập nhật: tự học mọi công thức bậc ≤ (cấp nghề/4 + 1) |

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-profession-crafting.md (this file) | 🔄 Current |
