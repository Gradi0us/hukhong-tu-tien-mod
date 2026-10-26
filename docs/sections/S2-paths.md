# S2 — Bốn Đạo, Tư Chất, Xuất Thân, Kỹ Năng Bản Mệnh, Pháp Bảo (v2)

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2: bám PLAN-v2 canon (Thể bay chỉ Bá Khí; đa đạo đồng thời + bảng xung đột theo nguồn đòn; Đạo tu hỗ trợ; Ma tu viết lại; bộ phím 3+1)
**Status**: 🔄 Draft v2 — chờ user chốt mục 7
**Phạm vi**: chỉ số 4 đạo, tài nguyên, luật cứng, Bản Mệnh Kiếm, Bá Khí, Ác Tính/Tâm Ma, tư chất, Kỹ Năng Bản Mệnh, xuất thân, pháp bảo, đa đạo.
**Thang số**: HP vanilla = 20 (10 tim). Quái Hư Hóa cùng cấp: `HP ≈ 25 + 12×cấp` (cấp 50 ≈ 625, cấp 100 ≈ 1225; Boss ×8–40). Mọi số dưới đây là **giá trị khởi điểm** để đưa vào JSON/datapack, sẽ chỉnh bằng playtest. Các chỉ số TTK/TTD/R trong mục 3.3 là **ước lượng lý thuyết, chưa playtest**.

---

## Thay đổi v2.1 (theo PLAN-v2 §14)

- R3: Ma tu bỏ luật chống farm riêng (§2.4.5), dùng chung `KillRateGate` (>30 con cùng loài / 64 block / 10 phút, thưởng giảm đến ×0,1); giữ Ác Danh.
- R17: Huyết Tế từ NPC có quota toàn cục/người/ngày; dân làng nhân giống gắn tag → Huyết Tế ×0,1; giết dân làng bất kỳ → Ác Danh.
- R10: tiền tệ duy nhất = Linh Thạch; quà khởi đầu tính bằng Linh Thạch hạ phẩm.
- R11: Căn Cơ Tổn Thương theo S3 (≤3 tầng, −15%/tầng, 10 phút/tầng; không mất EXP; riêng Tẩu Hỏa đã thay bằng phế tu vĩnh viễn, R23).
- R13: Đệ Tử Tông Môn nhận Thư Tiến Cử; Hậu Duệ Thế Gia sàn tư chất C; Con Hoang có debuff thật.
- R21: thêm §5.1 Onboarding mở khóa dần theo cảnh giới.
- R23: Tẩu Hỏa Nhập Ma = phế tu vĩnh viễn, làm lại từ đầu (thay R11 cho Tẩu Hỏa) + 3 tầng cảnh báo (§2.4.3). R25: phím kỹ năng Q/E/R/T, phím G bật/tắt "Thế Tu Luyện", chuyển trang đa đạo đổi sang `V` (§2.5).

## Thay đổi v2 (so với v1)

| # | Thay đổi | Vị trí |
|---|----------|--------|
| 1 | **Thể tu bay chỉ tốn Bá Khí**: bỏ điều kiện Hunger. Cất cánh cần Bá Khí ≥20, bay 1.0 BK/s (lơ lửng 0.5, lướt 2.0). Thêm hồi Bá Khí bằng thiền (+5/s, tới 60) và sàn Bá Khí 20 ngoài combat từ cấp 60 để luôn cất cánh được | §2.3 |
| 2 | **Đa đạo dùng đồng thời**, chi phí ×2 (song) / ×3 (tam); **bỏ đạo chủ/phụ 40%** và bỏ "kích hoạt đạo CD 5 phút". Thêm: luật "theo nguồn đòn" (8 nguồn), bảng xung đột cho cả 6 cặp song + 4 tổ hợp tam, kiểm tra combo mạnh (Ma+Thể mạnh nhất) và **6 trần** (Phân Tâm, khóa Tối Thượng chung, trần giảm sát thương, trần hồi Hấp Huyết, trần cộng dồn Công, quy tắc buff hỗ trợ) | §3.1–3.3 |
| 3 | **Đạo tu**: đòn thường lên người chơi **×1** (v1: ×0.3); thêm **bộ kỹ năng HỖ TRỢ** "Ban Phúc" (buff Máu / Tấn Công / Tốc Độ / Linh Lực theo cảnh giới, thời gian, trần, không cộng dồn) + Đại Phúc ở Tối Thượng | §2.2 |
| 4 | **Ma tu viết lại**: Huyết Tế/hấp thu **chỉ từ NPC** (không boss, không người chơi); 2 lộ **Hấp Thu** vs **Tu Luyện thường** (có so sánh tốc độ); thanh **Ác Tính** thay Tâm Ma với ngưỡng **Mất Kiểm Soát (≥80)** và **Tẩu Hỏa Nhập Ma (100)**; giảm Ác Tính bằng Thanh Tâm Đan/thiền; **không có kỹ năng hỗ trợ người khác**; chống farm dân làng (Chuỗi Cùng Loại giảm dần + giới hạn dân/ngày + phạt Danh Tiếng Thành) | §2.4 |
| 5 | **Kỹ năng = 3 phím + 1 Tối Thượng / đạo**: danh sách 6 kỹ năng nền v1 rút gọn; phần còn lại thành bị động/nâng cấp (bảng ánh xạ cũ→mới) | §2.5 |
| 6 | **Bản Mệnh Kiếm không rơi khi chết** (giữ nguyên, làm rõ thêm: kể cả khi chết do Tẩu Hỏa Nhập Ma, PvP, hay Ma Kiếm) | §2.1 |
| 7 | Cập nhật "Câu hỏi cho user" (chỉ 3 câu còn mở) + "Đề xuất đổi canon"; Document Lineage thêm v2 | §7, cuối file |
| – | Căn chỉnh theo PLAN-v2: hệ số EXP tư chất (A1.15/S1.3/SS1.45), không dùng Curios (slot tự làm), tuổi khởi đầu do S6 quy định | §3, §5, §6 |

---

## Key Findings

| # | Kết luận | Độ tin cậy |
|---|----------|-----------|
| 1 | Cân bằng theo bộ ba **TTK / TTD / rủi ro**: Kiếm giết nhanh nhất nhưng chết nhanh nhất; Thể bền nhất nhưng chậm nhất | CAO (số lý thuyết, chưa playtest) |
| 2 | Luật cứng (Đạo: đòn thường = 0 lên quái; Thể: cầm vũ khí = ×0) làm bằng **event chặn sát thương + tag nguồn đòn**, không sửa vanilla | CAO |
| 3 | Đa đạo đồng thời xử lý bằng **một nguyên tắc duy nhất: theo nguồn đòn** (đạo SỞ HỮU nguồn thì mở cấm; không đạo nào sở hữu thì cấm thắng). Không cần đạo chủ/phụ | CAO |
| 4 | Combo nguy hiểm nhất là **Ma+Thể** (tank + hút máu); nếu không có trần hồi Hấp Huyết thì R ≈ 5.7 (1.43× Thể solo). Các trần đưa mọi combo về ≤ 1.25× đạo mạnh nhất thành phần | TRUNG BÌNH (ước lượng, cần playtest) |
| 5 | Ma tu mạnh nhờ Hấp Thu nhưng bị **Ác Tính** kìm: chạm 80 có nguy cơ Mất Kiểm Soát, chạm 100 là chết; farm dân làng bị chặn bằng giảm lợi ích dần + phạt danh tiếng (lợi ích về 0 nhưng hình phạt vẫn còn) | CAO |
| 6 | Phẩm Bản Mệnh Kiếm, pháp bảo, Lõi dùng chung **7 bậc** Phàm→Thần để dễ tra cứu | CAO |

---

## 1. Chỉ số cơ sở & tăng trưởng (cấp 1 → cấp 100)

Công thức: `Chỉ số(L) = Base + Growth × (L−1)`, nhân thêm hệ số tư chất (mục 3). Phòng là điểm giáp tùy chỉnh (không bị trần 20 của vanilla), giảm sát thương theo `Phòng/(Phòng+100)`. Bảng này là **chỉ số của đạo đơn**; đa đạo xem §3.1 (Chỉ số hợp nhất).

| Chỉ số | Kiếm Tu (base / +cấp → Lv100) | Đạo Tu | Thể Tu | Ma Tu |
|--------|-------------------------------|--------|--------|-------|
| Sinh Lực (HP) | 20 / +4.5 → **465** | 20 / +3.5 → **367** | 24 / +7.0 → **717** | 22 / +5.0 → **517** |
| Công (cận chiến/kiếm) | 4 / +1.6 → **162** | 0 vs quái*; 2 / +0.8 → **81** vs người chơi (×1.0) | 3 / +1.3 → **132** | 3.5 / +1.2 → **122** |
| Thần Thức (Spell Power) | 2 / +0.6 → 61 | 6 / +2.0 → **204** | 1 / +0.2 → 21 | 3 / +1.0 → 102 |
| Phòng | 0 / +0.35 → 35 | 0 / +0.30 → 30 | 2 / +0.85 → **86** | 1 / +0.50 → 50 |
| Tốc (% so vanilla) | 100% / +0.10% → 110% | 100% / +0.05% → 105% | 100% / +0.02% → 102% | 100% / +0.07% → 107% |
| Bạo Kích (xác suất) | 5% / +0.30% → **35%** | 3% / +0.15% → 18% | 3% / +0.10% → 13% | 5% / +0.20% → 25% |
| Hấp Huyết | 0% | 0% | 0% | **6% / +0.04% → 10%** (+ Huyết Tế, xem 2.4) |
| Tài nguyên tối đa | Linh Lực 100 / +5 → 595 | Thánh Lực 120 / +6 → 714 | Bá Khí 100 (cố định) | Ô Uế Lực 100 / +3 → 397 |
| Hồi tài nguyên (/s) | 4 (ngoài combat 8) | 3 (thiền 14) | xem mục 2.3 | 0 tự nhiên (xem 2.4) |

\* Đạo Tu: **đòn thường lên quái/NPC = 0** (chỉ phép và kỹ năng giết quái). **Đòn thường lên người chơi ×1.0** Công Đạo (81 ở cấp 100, thấp hơn Kiếm 162 nên Đạo vẫn thua cận chiến PvP; ×1 chỉ nghĩa là không bị phạt thêm).

### 1.1 Radar so sánh (thang 1–10)

```
            CÔNG                      Kiếm  Đạo  Thể  Ma
             10                CÔNG    10    9*   6    6
          ┌───┼───┐            PHÒNG    3    3    10   5
   TỐC ───┤   │   ├─── HP      HP      6    5    10   7
          │ K─┼─Đ  │           PHÉP     3   10    1    5
   AoE/HT─┤   │   ├─── PHÒNG   AoE/HT   2   10    3    4
          └───┼───┘            HÚT MÁU  0    0    1    9
          THẦN THỨC            TỐC      8    6    4    7
(* Đạo: công = phép, đánh thường lên quái = 0)

 Kiếm  CÔNG ██████████ PHÒNG ███░░░░░░░ HP ██████░░░░ TỐC ████████░░
 Đạo   PHÉP ██████████ AoE   ██████████ PHÒNG ███░░░░░░░ HP █████░░░░░
 Thể   PHÒNG██████████ HP    ██████████ CÔNG ██████░░░░ TỐC ████░░░░░░
 Ma    HÚT  █████████░ CÔNG  ██████░░░░ HP ███████░░░ Cân bằng
```

### 1.2 Logic cân bằng — TTK / TTD / rủi ro (quái Hư Hóa cùng cấp, thường, 1v1, chỉ kỹ năng nền)

| Đạo | TTK (giây) | TTD (giây, khi bị 1 quái đánh liên tục) | R = TTD/TTK | Rủi ro | Phần thưởng |
|-----|-----------|------------------------------------------|-------------|--------|-------------|
| Kiếm | **4** | 8 | 2.0 | Mỏng máu, hết Linh Lực = mất sát thương, quái đông thì nguy | Giết nhanh, farm EXP/giờ cao, boss 1v1 nhanh |
| Đạo | 6 (đơn) / 2 mỗi con khi 3+ con | 10 | 1.7 → 5 (đám) | Phải đứng niệm phép, cạn Thánh Lực, cận chiến vô dụng | AoE, hồi máu/buff đồng đội, không cần ăn |
| Thể | 7 | **28** | 4.0 | Tầm ngắn, không vũ khí, cần "dính" quái để tích Bá Khí, bay yếu | Gần như không chết, tank cho nhóm |
| Ma (Hấp Thu) | 6 | 14 → ~22 (hút máu + Huyết Tế) | 2.3–3.7 | Ác Tính (Mất Kiểm Soát / Tẩu Hỏa), Ác Danh, Danh Tiếng Thành, không hỗ trợ đồng đội | Tự hồi phục, càng hấp thu càng mạnh |
| Ma (Tu Luyện thường) | 6 | 14 → ~17 (chỉ Hấp Huyết nền, không Huyết Tế) | 2.3–2.8 | Gần như không rủi ro Ác Tính | An toàn, chậm |

Nguyên tắc: **tích R × rủi ro xã hội/vận hành ≈ ngang nhau**. EXP/giờ tham chiếu: Kiếm 100%, Đạo 85% solo / 120% nhóm, Thể 75%, **Ma Hấp Thu 130% danh nghĩa (~108% hiệu dụng)**, **Ma Tu Luyện thường 85%**.

---

## 2. Từng đạo

### 2.1 Kiếm Tu

| Hạng mục | Chi tiết |
|----------|----------|
| Tài nguyên | **Linh Lực** 100 +5/cấp. Kiếm chiêu tốn 8–40; hồi 4/s (ngoài combat 8/s, sau 3s không đánh) |
| Luật cứng | Nhập đạo → roll **Bản Mệnh Kiếm** (item ràng buộc, **không rơi khi chết**, không trao đổi). Mất kiếm = triệu hồi lại ở Ấn Hồn Đài (phí nhỏ) |
| Bản Mệnh Kiếm khi chết | Không rơi trong **mọi** trường hợp chết: mob, PvP, Tẩu Hỏa Nhập Ma (nếu Ma Kiếm / Kiếm+Ma), hư không. Người chơi giết Kiếm tu không nhặt được kiếm |
| Triển khai | Item `hukhong:ban_menh_kiem` + Data Attachment lưu {loại, phẩm, EXP kiếm}. Sát thương kiếm = Công × hệ số phẩm. Kiếm khác (vanilla) dùng được nhưng **không** nhận bonus Công của đạo (×0.5) |
| Phím kỹ năng | 3 phím + 1 Tối Thượng: **Trảm Khí** (nâng cấp thành Liên Hoàn Trảm), **Kiếm Vực**, **Phi Kiếm Truy Hồn**, Tối Thượng **Kiếm Ý Hóa Hình** → chi tiết §2.5 |
| Bay | Ngự kiếm từ **cấp 20** (bị động): **4 Linh Lực/s** (bay nhanh 6/s, tốc độ ×1.5). Hết Linh Lực = rơi (có 3s giảm rơi). Không bay được khi đang trúng hiệu ứng Hư Hóa |
| Điểm yếu | HP thấp nhất ngoài Đạo; phụ thuộc Linh Lực; tầm ngắn khi kiếm bị khóa (Hư Không phản phệ vô hiệu kiếm 5s); không có hồi máu nội tại |

#### Bảng Bản Mệnh Kiếm (roll lúc nhập đạo; tư chất S/SS/SSS dịch tỉ lệ sang hàng dưới của bảng ~+30%/+60%/+100% cho kiếm hiếm)

| # | Tên | Hệ | Thuộc tính chính (phẩm Phàm) | Tiến hóa theo cảnh giới | Tỉ lệ |
|---|-----|----|------------------------------|--------------------------|-------|
| 1 | Thanh Phong Kiếm | Phong | Tốc +8%, Công +0% | Lv40: kiếm khí xuyên 2 mục tiêu; Lv80: tốc đánh +15% | 20% |
| 2 | Liệt Diễm Kiếm | Hỏa | 20% sát thương thiêu 3s | Lv40: nổ lửa 3 block; Lv80: thiêu lan quái kề nhau | 15% |
| 3 | Hàn Băng Kiếm | Băng | 15% làm chậm 30% 2s | Lv40: đóng băng 1s (CD 8s); Lv80: băng vực 5 block | 15% |
| 4 | Lôi Đình Kiếm | Lôi | 10% sét thêm 60% Công | Lv40: sét nảy 3 mục tiêu; Lv80: gọi lôi kiếp nhỏ (AoE) | 12% |
| 5 | Huyết Ảnh Kiếm | Huyết | Hấp Huyết +3% | Lv40: +6%, Lv80: +9% + hồi Linh Lực 2% sát thương | 10% |
| 6 | Trọng Nhạc Kiếm | Địa | Công +12%, Tốc −6% | Lv40: choáng 0.5s khi bạo kích; Lv80: phá giáp 15% | 10% |
| 7 | Ngọc Tâm Kiếm | Linh | Hồi Linh +25%, Linh Lực tối đa +15% | Lv40: kỹ năng −10% tiêu hao; Lv80: ngự kiếm miễn phí khi đứng trên kiếm trong combat | 8% |
| 8 | Tinh Thần Kiếm | Tinh | Bạo Kích +6%, sát thương bạo +25% | Lv40: mỗi 5 đòn → tinh quang 200% Công; Lv80: đổi "tinh tượng" tăng Công theo ban đêm | 5% |
| 9 | Hư Không Kiếm | Hư Không | Xuyên giáp 20%; miễn nhiễm Hư Hóa nhẹ | Lv40: đòn thứ 4 mở vết nứt hút quái; Lv80: dịch chuyển 8 block | 3% |
| 10 | Hỗn Nguyên Kiếm | Hỗn | Nhận ngẫu nhiên **2** hệ trên (50% hiệu lực mỗi cái) | Mỗi mốc tiến hóa nâng cả 2 hệ; Lv100 hợp nhất thành hệ riêng | 2% |

**7 giai đoạn phẩm kiếm** (đồng bộ pháp bảo): Phàm (Lv1) → Linh (Lv20) → Huyền (Lv40) → Địa (Lv60) → Thiên (Lv80) → Tiên (Lv90) → Thần (Lv100). Mỗi bậc: Công kiếm ×1.0/1.15/1.35/1.6/1.9/2.3/3.0 (đã gồm trong bảng Công ở mục 1). Tiến hóa cần **Hồn Tinh cùng phẩm** + 1 vật liệu hệ + hoàn thành "Tôi Kiếm" ở Lò Luyện Khí.

### 2.2 Đạo Tu (đạo hỗ trợ)

| Hạng mục | Chi tiết |
|----------|----------|
| Tài nguyên | **Thánh Lực** 120 +6/cấp. Hồi 3/s; **thiền** (ngồi/sneak yên 3s) 14/s; trong Thánh Địa/Thành +50% |
| Luật cứng | (a) Hunger không giảm, ăn vẫn hồi máu nhẹ. (b) **Đòn thường** (tay, vũ khí, cung, trượng, TNT, lava…) lên **quái/NPC** = 0; chỉ phép và kỹ năng giết quái. (c) **Đòn thường lên người chơi ×1** (Công Đạo, không phạt). (d) Vẫn phá block và đào bình thường. (e) Là đạo **hỗ trợ**: bộ Ban Phúc bên dưới |
| Triển khai "chỉ giết bằng phép" | `LivingIncomingDamageEvent`: nếu attacker đang có luật Đạo, target không phải người chơi, và nguồn đòn thuộc N1/N2/N3 (xem §3.2) mà không được đạo khác SỞ HỮU → `setAmount(0)`. Kỹ năng và phép (N4–N8) không bị chặn. Block break không đi qua event này. Thêm hiển thị "Vô Hiệu". PvP: không phạt (×1) |
| Phím kỹ năng | **Thánh Quang Tiễn** (nâng cấp thành Quang Trận → Thiên Phạt), **Trị Liệu**, **Ban Phúc** (4 chế độ buff), Tối Thượng **Thánh Giáng** (Đại Phúc) → §2.5 |
| Bay & cánh | Từ **cấp 60**: **2.5 TL/s** (bay đứng yên 1/s). Cánh thiên sứ tiến hóa: **Cấp 60: 1 đôi (tốc ×1.3)** → **Cấp 80: 2 đôi (×1.5, lướt 15 block)** → **Cấp 95: 3 đôi (×1.8, miễn rơi, aura +10% phép đồng đội)** → Cấp 100: **6 cánh** (Thần Dực: 1 lần hồi sinh/ngày thực). Cánh chỉ là hiệu ứng + bonus, nâng cấp tự động theo cảnh giới |
| Điểm yếu | Không đánh được quái bằng tay; cạn Thánh Lực = vô hại; mỏng máu; phép có thời gian niệm (0.3–1.2s) bị ngắt khi bị choáng; sát thương vs quái Hư Không "kháng thánh" thấp hơn ×0.85 |

#### Bộ kỹ năng HỖ TRỢ — "Ban Phúc" (phím 3, mở cấp 20) và "Đại Phúc" (Tối Thượng cấp 70)

Ban Phúc có **4 chế độ**, mỗi chế độ là một buff riêng, đổi chế độ bằng phím cuộn (0 giây). Mở khóa dần: **Máu (cấp 20) → Tấn Công (cấp 30) → Tốc Độ (cấp 40) → Linh Lực (cấp 50)**. Mỗi lần cast: **25 Thánh Lực**, **hồi chiêu 20s riêng từng chế độ**, tầm 20 block, 1 mục tiêu đồng đội (hoặc bản thân với **hiệu lực ×0.5**).

| Cảnh giới (cấp) | Máu (+% HP tối đa, lá chắn máu tạm) | Tấn Công (+% Công và Thần Thức) | Tốc Độ (+% tốc chạy & tốc đánh) | Linh Lực (hồi tức thì % tài nguyên tối đa / +% tốc hồi tài nguyên) | Thời gian |
|---|---|---|---|---|---|
| Tông Sư (20–29) | +6% | +5% | +5% | 10% / +15% | 20s |
| Đại Tông Sư (30–39) | +8% | +7% | +6% | 12% / +20% | 22s |
| Vương Giả (40–49) | +10% | +9% | +8% | 14% / +25% | 24s |
| Hoàng Giả (50–59) | +12% | +11% | +9% | 16% / +30% | 26s |
| Đế Giả (60–69) | +14% | +13% | +11% | 18% / +35% | 28s |
| Thánh Giả (70–79) | +17% | +15% | +12% | 20% / +40% | 30s |
| Đấu Giả (80–89) | +20% | +17% | +14% | 22% / +45% | 32s |
| Đấu Kình (90–99) | +23% | +19% | +16% | 24% / +50% | 35s |
| Thần (100) | +26% | +22% | +18% | 25% / +55% | 40s |

"Linh Lực" áp cho tài nguyên chính của người nhận (Linh Lực / Thánh Lực / Bá Khí gain mỗi đòn / Ô Uế Lực).

**Đại Phúc (Tối Thượng, cấp 70, Thánh Giáng)**: 100 Thánh Lực, CD 90s, AoE 12 block quanh Đạo tu: áp **cả 4 buff** lên đồng đội ở **×1.2** giá trị bảng trên, thời gian +50%; kèm hồi 3% HP/s trong 10s. Bản thân Đạo tu nhận ×0.5 phần buff (hồi máu nhận đủ).

**Luật chống cộng dồn vô hạn** (bắt buộc):
1. **Cùng loại không cộng dồn**: nhiều nguồn Ban Phúc cùng loại (kể cả nhiều Đạo tu trong tổ đội) → lấy **giá trị cao nhất**, làm mới thời gian, không cộng.
2. Mỗi mục tiêu tối đa **4 loại** buff hỗ trợ (1 mỗi loại).
3. **Trần tổng mỗi mục tiêu** (gồm Đại Phúc, Lõi, pháp bảo cùng loại): Máu ≤ +30%, Tấn Công ≤ +25%, Tốc Độ ≤ +20%, tốc hồi tài nguyên ≤ +60%.
4. Không tác dụng lên quái/boss; không giữ khi người nhận chết; buff tắt khi người nhận vào Tâm Kiếp.
5. Trị Liệu không thuộc bộ buff (xem §2.5, có CD 12s riêng).
6. Đạo tu **không giảm được Ác Tính** của Ma tu bằng bất kỳ phép nào (Tịnh Hóa/Thánh Khiết chỉ gỡ Hư Hóa/độc, không gỡ Ác Tính).

### 2.3 Thể Tu

| Hạng mục | Chi tiết |
|----------|----------|
| Tài nguyên | **Bá Khí** thang 0–100. **+2** mỗi đòn trúng, **+3 mỗi lần bị đánh** (+1 nếu sát thương <3% HP). Ngoài combat 5s: −4/s. Không bị trừ khi dùng đòn thường; kỹ năng tiêu 20–60. **Thiền** (ngồi yên 3s): **+5/s tới tối đa 60** (không lên tầng IV) |
| Luật cứng | Cầm vũ khí (tag `hukhong:weapons`: kiếm, rìu, cung, nỏ, trident...) → sát thương ×0; không dùng khiên để tấn công. Cầm công cụ đào, thức ăn, pháp bảo hộ trợ thì bình thường. Giáp mặc được (nhưng giáp nặng giảm Tốc 3%/mảnh vanilla Netherite) |
| Triển khai | Event so sánh `mainHand` với tag; ×0 kèm cảnh báo. Đòn tay không = `Công × 1.0` + tốc đánh cố định 1.6 đòn/s (không cooldown vanilla) |
| Phím kỹ năng | **Quyền Phá** (đòn thứ 4 thành Long Hổ Quyền từ cấp 50), **Thiết Bố Sam** (gộp Phản Chấn từ cấp 30), **Chấn Địa**, Tối Thượng **Kim Thân Bất Hoại** → §2.5 |
| **Bay (v2: chỉ Bá Khí)** | Từ **cấp 60** "Đạp Không": cất cánh cần **Bá Khí ≥20**; bay thường **1.0 BK/s**, lơ lửng **0.5 BK/s**, lướt nhanh (×1.5 tốc) **2.0 BK/s**. Bá Khí <5 → hạ cánh (3s giảm rơi). Khi đang bay, hao mòn −4/s ngoài combat **bị thay bằng chi phí bay** (không trừ hai lần). **Không còn điều kiện Hunger**. Bị đánh / đánh trúng khi đang bay vẫn cộng Bá Khí bình thường. Tốc ×1.2 (chậm nhất 3 đạo) |
| Bị động "Khí Hải" (cấp 60) | Ngoài combat, Bá Khí **không rơi dưới 20** → luôn đủ để cất cánh ~15s; muốn bay xa phải thiền (60 BK ≈ 57s bay thường) hoặc dùng pháp bảo di chuyển (Phi Vân Chu) |
| Điểm yếu | Tầm ngắn (2.8 block); không thể kite; thiếu Bá Khí = đòn thường yếu 20%; kém phép/AoE; bay phụ thuộc Bá Khí (thời gian bay ngắn nhất); không tận dụng được hầu hết Lõi kiểu "vũ khí" |

Cân bằng bay (chỉ Bá Khí): bay đứng chân trong combat là chủ ý (hủy kite từ trên cao: boss thế giới vẫn cấm bay); đường dài chậm hơn Kiếm (4 LL/s hồi liên tục) và Đạo (thiền 14/s), nên Thể không thành "du hành giả tốt nhất".

#### Hệ Bá Khí — các tầng (theo Bá Khí hiện tại)

| Tầng | Bá Khí | Uy áp (aura 8 block) | Chuyển hóa |
|------|--------|---------------------|-----------|
| I Khí Thế | 25–49 | Quái cấp ≤ (cấp mình −15): **chậm 20%** | +10% Phòng |
| II Uy Áp | 50–74 | Quái ≤ (−10): chậm 30% + **sợ hãi** (bỏ chạy 3s mỗi 10s) | +20% Phòng, +10% Công |
| III Bá Đạo | 75–99 | Quái ≤ (−5): chậm 40%, sợ hãi, −15% Công quái | +30% Phòng, +20% Công |
| IV Cực Hạn | 100 | Quái ≤ cấp mình: choáng 1s lúc kích hoạt; −25% Công quái | +40% Phòng, +30% Công, 6s rồi về 50 |

Boss và quái **cao hơn cấp** miễn uy áp. Hai **thế**: **Hộ Thể** (Bá Khí→giảm sát thương tuyến tính, tối đa −50% ở 100) hoặc **Bạo Phát** (Bá Khí→Công, +0.3%/điểm); đổi thế 3s, CD 2s.

### 2.4 Ma Tu (viết lại v2)

#### 2.4.1 Luật cứng

| Luật | Nội dung |
|------|----------|
| Hấp Huyết nội tại | Nền **6%** (+0.04%/cấp) sát thương gây ra → HP. Áp dụng **mọi mục tiêu** (quái, boss, người chơi) và **mọi nguồn đòn**. Trần hồi: **4% HP tối đa/giây** (solo) |
| Hấp thu sức mạnh **chỉ từ NPC** | "NPC" = quái thường & elite (không boss, không từ spawner), dân làng, NPC tu sĩ (không boss). **Boss và người chơi: 0 Huyết Tế, 0 EXP Hấp Thu, 0 Ô Uế thưởng giết** (nhưng Hấp Huyết hồi máu vẫn hoạt động). Quái spawner vẫn 0 mọi thứ (canon §1.1) |
| **Không hỗ trợ người khác** | Cấm: hồi máu/lá chắn/buff/giải khống chế/hồi sinh lên đồng đội; Hấp Huyết không chia sẻ; không aura. Skill tự thân (lá chắn, tăng Công bản thân) vẫn hợp lệ. Vẫn **nhận** buff từ người khác. Kỹ năng hỗ trợ của đạo khác (đa đạo) → xem §3.2 (chỉ tác dụng lên bản thân) |
| Ô Uế Lực | Tối đa 100 +3/cấp. Không tự hồi. **+1 mỗi đòn trúng** (mọi mục tiêu, tối đa +10/giây); **+8 giết quái NPC**, **+15 giết dân/NPC tu sĩ** (chỉ khi hấp thu hợp lệ, xem 2.4.4); −2/s ngoài combat |
| Nhập đạo | Chọn **nhánh** (Thuần/Kiếm/Thể/Đạo — giữ nguyên bảng 2.4.6) và chọn **lộ** (Hấp Thu / Tu Luyện thường) |

#### 2.4.2 Hai lộ: Hấp Thu vs Tu Luyện thường

| | **Lộ Hấp Thu** | **Lộ Tu Luyện thường** |
|---|---|---|
| Nguồn EXP | EXP giết NPC ×1.45 (phần +0.45 là "tinh hoa hấp thu") | Như Đạo tu: EXP giết ×1.0, thiền, nhiệm vụ |
| Huyết Tế | Có (+1 stack mỗi NPC hấp thu, elite +3), tối đa 100; mỗi stack **+0.5% Công, +0.1% Hấp Huyết** (tối đa +50% Công, +10% HH); −1 stack/phút ngoài combat | **Không** có Huyết Tế |
| Ác Tính | Có (xem 2.4.3) | Chỉ tăng khi dùng đồ ma (+0.1/s) hoặc Đan Độc; **không** tăng khi giết |
| EXP/giờ (Kiếm = 100%) | **130% danh nghĩa** → ~115% sau KillRateGate → **~108% hiệu dụng** (trừ thời gian thiền hạ Ác Tính) | **85%** (ngang Đạo tu solo) |
| Thời gian tới Thần (tư chất B) | ~**100 giờ** (có vật liệu) / ~**455 giờ** (không) | ~**129 giờ** / ~**588 giờ** |
| Tỉ lệ nhanh hơn | **×1.25 hiệu dụng** (×1.53 danh nghĩa) so với Tu Luyện thường | chuẩn so sánh |
| Rủi ro | Mất Kiểm Soát, Tẩu Hỏa Nhập Ma, Ác Danh, Danh Tiếng Thành | Gần như không |
| Đổi lộ | Ở **Ma Môn**: phí 1 Thanh Tâm Đan Tam phẩm, điều kiện Ác Tính <30; sang Tu Luyện thường **xóa Huyết Tế**; CD 3 ngày chơi thực | như trái |

Lý do thiết kế: Hấp Thu phải **nhanh hơn rõ** để đáng rủi ro, nhưng chỉ hơn ~1.25× sau khi trừ downtime và giảm dần, tránh trở thành lựa chọn mặc định bắt buộc; Tu Luyện thường tồn tại cho server thân thiện và người chơi sợ mất kiểm soát.

#### 2.4.3 Thanh Ác Tính (0–100)

Ác Tính thay Tâm Ma cho Ma tu (Tâm Ma vẫn dành cho đạo khác khi dùng đồ ma/Đan Độc).

**Tăng Ác Tính** (lộ Hấp Thu; mỗi hấp thu hợp lệ):

| Đối tượng bị hấp thu | Ác Tính |
|----------------------|---------|
| Quái thường | +1 |
| Elite | +3 |
| Dân làng / NPC thường | +5 |
| NPC tu sĩ | +8 |
| Giết người chơi (không hấp thu được) | +10 (không lợi ích) |
| Kỹ năng Tối Thượng Ma Thần Giáng Thế | +15 khi kích hoạt |
| Giết trong lúc Mất Kiểm Soát | +8 mỗi mạng |
| Đồ ma / Đan Độc | +0.1/s / theo đan |

Phạt Ác Tính **không** bị giảm bởi `KillRateGate`: chỉ **lợi ích** bị giảm, hình phạt vẫn nguyên (xem 2.4.5).

**Ngưỡng và trạng thái**:

| Ác Tính | Trạng thái | Hiệu ứng |
|---------|-----------|----------|
| 0–29 | Tĩnh Tâm | Không |
| 30–59 | Huyết Khát | +8% Công, +1% Hấp Huyết, thị giác đỏ nhẹ |
| 60–79 | Cuồng Huyết | +15% Công, +2% Hấp Huyết, ảo thanh; **ngừng tự giảm** |
| 80–94 | **Mất Kiểm Soát (nguy cơ)** | +20% Công, +3% HH. Mỗi 20s xác suất `(Ác Tính − 75)%` (80→5%, 90→15%) kích hoạt **Mất Kiểm Soát 4s**. **Khóa Hấp Thu từ 90** (UI "Bão hòa") |
| 95–99 | **Tẩu Hỏa Cảnh Báo** | Cảnh báo tầng 3 (xem dưới); **đếm ngược 8s**: nếu vẫn ≥95 sau 8s → Tẩu Hỏa Nhập Ma |
| 100 | **Tẩu Hỏa Nhập Ma** | Chết ngay + **phế tu vĩnh viễn** (R23) |

**3 tầng cảnh báo bắt buộc (R23)** — mỗi tầng dùng đủ 3 kênh âm thanh + màn hình + chat:

| Tầng | Ngưỡng | Âm thanh | Màn hình | Chat |
|------|--------|----------|----------|------|
| 1 | Ác Tính ≥ 80 (Mất Kiểm Soát nguy cơ) | Tiếng tim đập chậm | Viền đỏ nhẹ, thanh Ác Tính rung | "Ma khí dâng, Tâm Ma bất ổn" |
| 2 | Ác Tính ≥ 90 (khóa Hấp Thu) | Tim đập nhanh + chuông trầm | Viền đỏ đậm, title cảnh báo | "Sắp Tẩu Hỏa. Dừng hấp thu, thiền hoặc dùng Thanh Tâm Đan" |
| 3 | Ác Tính ≥ 95 (đếm ngược 8s) | Còi liên tục | Màn hình đỏ đen + số đếm ngược | "Tẩu Hỏa Nhập Ma sau N giây: sẽ phế tu vĩnh viễn" |

Mất Kiểm Soát vẫn là bước đệm. Cảnh báo chỉ tắt được phần hiệu ứng hình (R26), không tắt chat/âm.

**Mất Kiểm Soát (4s)**: nhân vật tự đánh mục tiêu gần nhất (kể cả đồng đội, dân làng, NPC thân thiện), **khóa phím kỹ năng**, +25% Công, không hồi máu (tắt Hấp Huyết); kết thúc: **Ác Tính −10**. Giết người/dân làng trong lúc này có hậu quả đầy đủ (Ác Danh, Danh Tiếng Thành).

**Tẩu Hỏa Nhập Ma** (canon): chết ngay (bỏ qua Hộ Thân Phù, Thần Dực, mọi hồi sinh), **mất toàn bộ Huyết Tế**, và nhân vật thành **phế tu vĩnh viễn (R23)**: không thể tu luyện tiếp, chỉ có thể **làm lại từ đầu**. Reset toàn bộ: cấp về 1, Lõi, Hồn Cốt, đạo, tư chất, Ác Tính, Huyết Tế; quay lại màn tạo nhân vật. Giữ túi đồ; đồ bind tu tiên bị vô hiệu. Không còn Căn Cơ Tổn Thương cho Tẩu Hỏa, không có Đan gỡ. Bản Mệnh Kiếm (Ma Kiếm) không rơi.

**Giảm Ác Tính**:

| Cách | Hiệu quả | Ghi chú |
|------|----------|---------|
| **Thiền** (ngồi/sneak yên 3s, không bị đánh 5s) | **−1.5/s** (Ma Môn: −3/s) | Từ 60 về 0 mất ~40s |
| **Thanh Tâm Đan** Nhất phẩm | −15 | CD dùng chung 45s; Đan Độc +2 |
| Tam phẩm | −30 | CD 60s; Đan Độc +4 |
| Lục phẩm | −50 | CD 90s; Đan Độc +8 |
| Cửu phẩm | −80 | CD 120s; Đan Độc +14 |
| Tự giảm | −0.2/s khi ngoài combat 10s **và** Ác Tính <60 | Trên 60 không tự giảm |
| Chết thường (không phải Tẩu Hỏa) | Ác Tính −50% | Tẩu Hỏa thì reset toàn bộ (R23) |

Phép Thánh, Tịnh Hóa, pháp bảo giảm Tâm Ma **không** giảm Ác Tính (trừ Hồn Linh Đỉnh: −20 mỗi 180s, xem §6.2).

#### 2.4.4 Kỹ năng Ma (bộ 3+1 xem §2.5)

Không kỹ năng nào hỗ trợ người khác. Lộ Hấp Thu và Tu Luyện thường dùng chung bộ kỹ năng; khác nhau ở EXP, Huyết Tế, Ác Tính.

#### 2.4.5 Chống farm: dùng chung `KillRateGate` (R3)

Ma tu **không còn luật chống farm riêng**. Mọi đạo (kể cả Ma Hấp Thu) dùng **một cổng duy nhất `KillRateGate`** (khai báo trong `gates`, chuẩn ở S3/S5): **>30 con cùng loài trong bán kính 64 block / 10 phút** thì thưởng (EXP, Huyết Tế, Ô Uế) giảm dần đến **×0,1**. Chỉ giảm **lợi ích**; **hình phạt (Ác Tính, Ác Danh, Danh Tiếng Thành) giữ nguyên hoặc tăng**.

**Huyết Tế từ NPC có quota (R17)**: quota toàn cục + theo người + theo ngày (số cụ thể do S5 đặt). Dân làng **sinh từ nhân giống** gắn tag, Huyết Tế từ chúng chỉ **×0,1**. **Giết dân làng bất kỳ → Ác Danh** (bên dưới).

**Danh Tiếng Thành** (−1000…+1000 theo từng thành, bắt đầu 0; phối hợp S4/S6):

| Hành động | Danh tiếng |
|-----------|-----------|
| Giết dân làng thuộc thành | −20 (thành đó), −5 (các thành Địa/Thiên gần) |
| Giết NPC tu sĩ | −40 |
| Giết lính thành | −60 |

| Danh tiếng | Hậu quả |
|------------|---------|
| ≤ −200 | Giá chợ ×1.3, dân làng bỏ chạy |
| ≤ −400 | Lính thành thù địch khi thấy |
| ≤ −700 | **Cấm vào thành 7 ngày thực** + truy nã của thành đó |
| ≤ −900 | Toàn thành thù địch, kích hoạt "Chính Đạo Thảo Phạt" |

Hồi: +5/ngày chơi (không giết NPC thành đó), hoặc nhiệm vụ chuộc tội/quyên góp đan dược. Danh Tiếng Thành là **cục bộ theo thành**; Ác Danh (sao) bên dưới là **toàn cục**.

#### 2.4.6 Nhánh Ma và hậu quả xã hội

**4 nhánh** (chọn khi nhập đạo, hoặc chuyển nhánh ở Ma Môn có phí lớn). **Khi đa đạo có Ma, nhánh Ma cố định = Ma Thuần** (vì đạo kia đã tồn tại thật):

| Nhánh | Luật nới lỏng | Điều chỉnh | Bay |
|-------|---------------|-----------|-----|
| Ma Thuần | Không hạn chế; dùng mọi vũ khí; phép Ma | Chỉ số bảng mục 1 | Cấp 60 (3 ÔUL/s) |
| Ma Kiếm | Bản Mệnh Kiếm (phẩm "Huyết Ma", roll bảng 2.1 nhưng chỉ hệ Huyết/Hỏa/Hư Không/Tinh) | Công +10%, HP −10%, Hấp Huyết nền 5% | **Cấp 20** ngự kiếm (4 LL/s dùng Linh Lực song song) |
| Ma Thể | Cho phép vũ khí nhưng chỉ ×0.6 (đòn tay ×1.0); có Bá Khí rút gọn (tối đa 60) | HP +20%, Phòng +15%, Công −10% | Cấp 60 (**1.0 BK/s**, chỉ Bá Khí, cùng luật Thể) |
| Ma Đạo | Đánh thường lên quái ×0.25 (thay vì 0); cần ăn; phép Ma tiêu ÔUL | Thần Thức +25%, Công −25% | Cấp 60 (2.5 TL/s thay bằng ÔUL) |

**Hậu quả xã hội — Ác Danh (toàn cục)** 0–5 sao (giết dân làng +0.5 sao, giết NPC thành +1, giết người chơi +1.5; rửa 1 sao/1 giờ ngoại tuyến hoặc Thanh Tâm Đan lớn). ≥1 sao: dân làng bỏ chạy/giá ×1.5; ≥2: lính thành thù địch, bị tấn công khi vào; ≥3: **truy nã** (đội săn lùng Ma tu mỗi 20 phút, bản đồ đánh dấu cho người chơi khác); ≥4: không vào được thành chính đạo, hành lang tông môn thù địch; 5: sự kiện "Chính Đạo Thảo Phạt". Ma Môn là nơi an toàn duy nhất.

**Giết người chơi (v2)**: nạn nhân rơi đồ như thường nhưng được tùy chọn giữ Bản Mệnh; kẻ giết **không** nhận Huyết Tế/EXP/Ô Uế; chỉ nhận **Ác Tính +10, Ác Danh +1.5 sao** và bị bounty hệ thống. Server có công tắc `pvp_demon_enabled`.

### 2.5 Bộ phím kỹ năng — 3 phím chủ động + 1 Tối Thượng mỗi đạo

Mỗi đạo có **1 trang** gồm Phím 1 / Phím 2 / Phím 3 và 1 phím **Tối Thượng**. Bị động và nâng cấp luôn bật, không chiếm phím. **Phím mặc định (R25): Q = Phím 1, E = Phím 2, R = Phím 3, T = Tối Thượng.** Q (vứt đồ) và E (túi đồ) trùng vanilla nên **phím `G` bật/tắt "Thế Tu Luyện"**: bật thì Q/E/R/T là kỹ năng (vứt đồ/túi đồ vanilla chuyển sang Ctrl+Q / mở túi bằng phím khác hoặc tắt thế), tắt thì Q/E trả về vanilla; mọi phím đổi được trong Controls. Đa đạo: mỗi đạo một trang, **phím `V` chuyển trang** (khóa 0.5s); bị động, luật cứng, chỉ số hợp nhất luôn hoạt động đồng thời (§3.1).

#### Kiếm Tu

| Phím | Kỹ năng | Mở | CD | Chi tiết và tiến hóa |
|------|---------|----|----|----------------------|
| 1 | **Trảm Khí** | Cấp 1 | 0.6s | Kiếm khí tầm 8: 120% Công, 10 LL. **Cấp 20 → Liên Hoàn Trảm**: 3 nhát × 70% Công, 25 LL. **Cấp 50**: 4 nhát × 75%, 30 LL, nhát cuối chắc chắn bạo kích |
| 2 | **Kiếm Vực** | Cấp 10 | 14s | 40 LL, 4s: cấp 10 = 3 block 25% Công/0.5s; **cấp 30** = 5 block 40%; **cấp 60** = 6 block 55% |
| 3 | **Phi Kiếm Truy Hồn** | Cấp 30 | 18s | 60 LL, kiếm tự tìm mục tiêu 90% Công mỗi cái: 4 kiếm; **cấp 60** = 6 kiếm; **cấp 85** = 8 kiếm |
| Tối Thượng | **Kiếm Ý Hóa Hình** | Cấp 70 | 60s | 80 LL: +30% Công, +15% Bạo Kích 12s; Phi Kiếm CD −50% trong thời gian này |
| Bị động | **Ngự Kiếm** (bay) | Cấp 20 | – | Xem 2.1 |

#### Đạo Tu

| Phím | Kỹ năng | Mở | CD | Chi tiết và tiến hóa |
|------|---------|----|----|----------------------|
| 1 | **Thánh Quang Tiễn** | Cấp 1 | 0.8s | 110% Thần Thức, 8 TL. **Cấp 20**: mỗi 15s, tiễn trúng tạo **Thánh Quang Trận** 6 block 35% TT/s 5s (không tốn thêm). **Cấp 50**: mỗi 20s, sau 4 tiễn liên tiếp gọi **Thiên Phạt** AoE 280% TT (không tốn TL) |
| 2 | **Trị Liệu** | Cấp 10 | 12s | 30 TL: hồi 25% HP đồng đội, **15% bản thân**; cấp 40: tầm 20 block + hồi lan 50% sang 1 người gần nhất |
| 3 | **Ban Phúc** | Cấp 20 | 20s/chế độ | 4 chế độ buff (Máu 20 / Tấn Công 30 / Tốc Độ 40 / Linh Lực 50), 25 TL — số liệu và luật chống cộng dồn ở §2.2 |
| Tối Thượng | **Thánh Giáng** (Đại Phúc) | Cấp 70 | 90s | 100 TL, AoE 12 block, 4 buff ×1.2 + hồi 3% HP/s 10s (§2.2) |
| Bị động | **Thánh Khiết** | Cấp 30 | 12s | Tự gỡ 1 debuff Hư Hóa/độc trên bản thân + đồng đội trong 4 block (không gỡ Ác Tính) |
| Bị động | **Cánh thiên sứ** (bay) | Cấp 60 | – | Xem 2.2 |

#### Thể Tu

| Phím | Kỹ năng | Mở | CD | Chi tiết và tiến hóa |
|------|---------|----|----|----------------------|
| 1 | **Quyền Phá** | Cấp 1 | – | 130% Công, 0 BK. **Cấp 50**: đòn thứ 4 liên tiếp = **Long Hổ Quyền** 350% Công (không tốn BK) |
| 2 | **Thiết Bố Sam** | Cấp 10 | 18s | 25 BK: −25% sát thương nhận 6s. **Cấp 30** (gộp Phản Chấn): thêm phản 40% sát thương nhận, 40 BK |
| 3 | **Chấn Địa** | Cấp 20 | 15s | 30 BK: hất tung 4 block, 100% Công |
| Tối Thượng | **Kim Thân Bất Hoại** | Cấp 70 | 100s | 100 BK: miễn khống chế, −45% sát thương 8s |
| Bị động | **Khí Hải / Đạp Không** | Cấp 60 | – | Xem 2.3 |

#### Ma Tu

| Phím | Kỹ năng | Mở | CD | Chi tiết và tiến hóa |
|------|---------|----|----|----------------------|
| 1 | **Huyết Trảo** | Cấp 1 | 0.8s | 125% Công, Hấp Huyết nhân đôi trên đòn này |
| 2 | **Ma Khí Hộ Thể** | Cấp 10 | 20s | 20 ÔUL: lá chắn 15% HP (chỉ bản thân). **Cấp 50 (gộp Huyết Tế Đài)**: khi lá chắn vỡ nổ vùng 6 block rút máu 25% TT/s 5s, hồi cho mình |
| 3 | **Ma Ảnh** | Cấp 30 | 10s | 25 ÔUL: lướt 6 block, bất tử 0.4s |
| Tối Thượng | **Ma Thần Giáng Thế** | Cấp 70 | 120s | 60 ÔUL: biến hình 12s +40% Công/Hấp Huyết (chịu trần); **+15 Ác Tính** khi kích hoạt |
| Bị động | **Huyết Nộ** | Cấp 20 | 40s | Khi HP <40% tự +25% Công 8s |

#### Ánh xạ danh sách kỹ năng nền v1 (6/đạo) → v2

| Đạo | Kỹ năng v1 | Nơi ở v2 |
|-----|-----------|----------|
| Kiếm | Trảm Khí / Liên Hoàn Trảm / Ngự Kiếm / Kiếm Vực / Phi Kiếm Truy Hồn / Kiếm Ý Hóa Hình | P1 / nâng cấp P1 cấp 20 / bị động cấp 20 / P2 / P3 / Tối Thượng |
| Đạo | Thánh Quang Tiễn / Trị Liệu / Thánh Quang Trận / Tịnh Hóa / Thiên Phạt / Thánh Giáng | P1 / P2 / nâng cấp P1 cấp 20 / bị động Thánh Khiết / nâng cấp P1 cấp 50 / Tối Thượng |
| Thể | Quyền Phá / Thiết Bố Sam / Chấn Địa / Phản Chấn / Long Hổ Quyền / Kim Thân Bất Hoại | P1 / P2 / P3 / nâng cấp P2 cấp 30 / nâng cấp P1 cấp 50 / Tối Thượng |
| Ma | Huyết Trảo / Ma Khí Hộ Thể / Huyết Nộ / Ma Ảnh / Huyết Tế Đài / Ma Thần Giáng Thế | P1 / P2 / bị động cấp 20 / P3 / nâng cấp P2 cấp 50 / Tối Thượng |

---

## 3. Tư Chất D→SSS

Hệ số EXP theo **PLAN-v2 §4** (A1.15 · S1.3 · SS1.45 · SSS1.6).

| Bậc | Tỉ lệ (mỗi đạo roll riêng) | Hệ số EXP | Tăng trưởng stat | Chỉ số khởi đầu | Độ hiếm Lõi (kéo phẩm +) | Kỹ Năng Bản Mệnh |
|-----|----|----|----|----|----|----|
| D | 8% | ×0.6 | −10% | −10% | −10% xác suất phẩm cao | Không |
| C | 24% | ×0.8 | −5% | −5% | −5% | Không |
| B | 28% | ×1.0 | 0% | 0% | 0% | Không |
| A | 22% | ×1.15 | +5% | +5% | +10% | A (1 skill) |
| S | 13% | ×1.3 | +10% | +10% | +20% | Bản S |
| SS | 4% | ×1.45 | +15% | +15% | +35% | Bản SS |
| SSS | 1% | ×1.6 | +20% | +20% | +50% | Bản SSS |

Kỳ vọng EXP ≈ ×1.02 (không lạm phát). Reroll tư chất ngay màn tạo nhân vật: **1 lần**, sau đó khóa (Type 1 decision).

### 3.1 Đa đạo (Song/Tam năng) — dùng đồng thời

**Nguyên tắc (canon §3.1)**: mọi đạo đã có **hoạt động cùng lúc**; không còn đạo chủ/phụ, không còn kích hoạt đạo / CD 5 phút.

| Hạng mục | Quy tắc |
|----------|---------|
| Phân bổ | Song năng 20%, Tam năng 5% (roll riêng tư chất từng đạo) |
| Chi phí tài nguyên | Mọi chi phí kỹ năng, bay, duy trì: **×2 (song) / ×3 (tam)**, trừ từ **kho tài nguyên của chính đạo sở hữu kỹ năng** (kho từng đạo độc lập) |
| Phạt EXP | EXP cần ×1.6 (song) / ×2.5 (tam) |
| Hệ số EXP tư chất | **Trung bình cộng** hệ số các đạo [ĐỀ XUẤT] |
| Phím | Mỗi đạo 1 trang 3+1; `V` chuyển trang, `G` bật/tắt Thế Tu Luyện (R25); **Tối Thượng dùng chung 1 khóa 45s** (§3.3, T2) |
| Bay | Chọn một kiểu bay đã mở của mọi đạo; chi phí ×2/×3 theo đạo sở hữu kiểu bay đó |
| Bản Mệnh Kiếm | Giữ nguyên (Kiếm); Ma trong đa đạo luôn là Ma Thuần, **không** nhận kiếm Huyết Ma riêng |
| Lõi | 1 Lõi/mốc (canon). Pool 3 Lõi có **≥1 Lõi cho mỗi đạo bạn có**; vì mọi đạo luôn bật, Lõi riêng-đạo **luôn hiệu lực**; Lõi Thông Dụng ~40% pool; set bonus tính theo Hệ, bất kể đạo |
| Bù trừ Tam năng | 1 slot Hồn Cốt phụ miễn phí ở cấp 50 |

**Chỉ số hợp nhất** (cho mọi chỉ số chiến đấu): mỗi chỉ số lấy **giá trị cao nhất** trong các đạo (không cộng dồn), rồi nhân **hệ số Phân Tâm**:

| | Công, Thần Thức, Hấp Huyết | HP, Phòng | Tốc, Bạo Kích |
|---|---|---|---|
| Song năng | ×0.80 | ×0.90 | giữ nguyên (max) |
| Tam năng | ×0.70 | ×0.85 | giữ nguyên (max) |

Tài nguyên tối đa và hồi tài nguyên: giữ nguyên từng kho (không nhân Phân Tâm).

### 3.2 Xung đột luật cứng — nguyên tắc "THEO NGUỒN ĐÒN"

**Tám nguồn đòn** (mỗi lượt sát thương mang đúng 1 mã nguồn):

| Mã | Nguồn đòn | Ví dụ |
|----|-----------|-------|
| N1 | Tay không (đòn thường) | Đấm không vũ khí |
| N2 | Vũ khí cầm tay / bắn (tag `hukhong:weapons`) | Kiếm cầm tay (kể cả Bản Mệnh Kiếm cầm tay), cung, nỏ, trident |
| N3 | Gián tiếp | TNT, lava, rơi, thú nuôi |
| N4 | Kỹ năng Kiếm (phi kiếm, không cầm tay) | Trảm Khí, Phi Kiếm |
| N5 | Kỹ năng Thể (quyền/cước) | Chấn Địa, Long Hổ Quyền |
| N6 | Phép Thánh (`thanh_phep`) | Thánh Quang Tiễn, Thiên Phạt |
| N7 | Phép/Huyết Ma | Huyết Trảo, Huyết Tế Đài |
| N8 | Pháp bảo tấn công | Lôi Châu, Chấn Hồn Chung |

**Quy tắc phán xử (theo thứ tự)**:
1. **SỞ HỮU**: nếu nhân vật có đạo **sở hữu** nguồn đó → dùng luật của đạo sở hữu; **cấm của đạo khác bị vô hiệu cho nguồn này**. Sở hữu: Thể ← N1, N5; Kiếm ← N4; Đạo ← N6; Ma ← N7. (N2, N3, N8 không đạo nào sở hữu.)
2. **CẤM**: nếu không ai sở hữu mà một đạo cấm nguồn đó → sát thương = 0. Cấm: **Đạo cấm N1, N2, N3 lên quái/NPC** (không áp dụng lên người chơi); **Thể cấm N2 (mọi mục tiêu) và N8**.
3. Còn lại: hệ số bình thường của đạo. **Hấp Huyết, buff, trần luôn áp dụng sau cùng** lên sát thương còn lại.
4. Luật **không phải sát thương** giải thích riêng ở cuối bảng.

**Bảng cặp song năng** (✓ = gây sát thương bình thường; 0 = bị luật cứng chặn; xét lên quái/NPC):

| Cặp | N1 tay không | N2 vũ khí | N3 gián tiếp | N4 Kiếm skill | N5 Thể skill | N6 Phép Thánh | N7 Phép Ma | N8 Pháp bảo | Ghi chú |
|-----|---|---|---|---|---|---|---|---|---|
| **Kiếm+Đạo** | 0 | 0 | 0 | ✓ | – | ✓ | – | ✓ | Chỉ kỹ năng/phép giết quái; lên **người chơi** N1/N2 ×1 |
| **Kiếm+Thể** | ✓ (Thể) | 0 (Thể cấm, kể cả Bản Mệnh Kiếm cầm tay) | ✓ | ✓ (phi kiếm không cầm tay) | ✓ | – | – | 0 | Kiếm chỉ dùng phi kiếm; Ngự Kiếm đứng trên kiếm hợp lệ |
| **Đạo+Thể** | ✓ (Thể sở hữu, mở cấm Đạo) | 0 | 0 (Đạo cấm) | – | ✓ | ✓ | – | 0 | Tay không đánh quái bằng Công Thể; vũ khí vẫn cấm |
| **Ma+Kiếm** | ✓ | ✓ | ✓ | ✓ | – | – | ✓ | ✓ | Không cấm sát thương; mạnh qua Hấp Huyết (trần T4) |
| **Ma+Đạo** | 0 | 0 | 0 | – | – | ✓ | ✓ | ✓ | Đạo cấm đòn thường; hỗ trợ của Đạo chỉ lên bản thân |
| **Ma+Thể** | ✓ (Thể) | 0 (Thể cấm, mạnh hơn nhánh Ma Thể ×0.6) | ✓ | – | ✓ | – | ✓ | 0 | Combo nguy hiểm nhất (T3, T4) |

**Bảng tam năng**:

| Tổ hợp | N1 | N2 | N3 | N4 | N5 | N6 | N7 | N8 | Ghi chú |
|--------|----|----|----|----|----|----|----|----|---------|
| Kiếm+Đạo+Thể | ✓ | 0 | 0 | ✓ | ✓ | ✓ | – | 0 | Thể mở N1; Đạo cấm N3 |
| Kiếm+Đạo+Ma | 0 | 0 | 0 | ✓ | – | ✓ | ✓ | ✓ | Hỗ trợ Đạo chỉ bản thân |
| Kiếm+Thể+Ma | ✓ | 0 | ✓ | ✓ | ✓ | – | ✓ | 0 | Cần trần hồi mạnh (T4 tam = 1.0%) |
| Đạo+Thể+Ma | ✓ | 0 | 0 | – | ✓ | ✓ | ✓ | 0 | Hỗ trợ Đạo chỉ bản thân |

**Luật không phải sát thương trong đa đạo**:

| Luật | Xử lý |
|------|-------|
| Đạo "không cần ăn" | Áp dụng nguyên (lợi ích đa đạo), trừ khi nhánh Ma Đạo (cần ăn) |
| Đạo "hỗ trợ" + Ma "không hỗ trợ người khác" | **Ma thắng**: mọi kỹ năng hỗ trợ của Đạo (Trị Liệu, Ban Phúc, Đại Phúc, Thánh Khiết, aura cánh) chỉ tác dụng lên **bản thân** (Ban Phúc ×0.5; Trị Liệu 15%), **không** tác dụng lên người khác. Vẫn nhận buff từ người khác |
| Ma "Hấp Huyết" | Áp dụng cho mọi nguồn đòn của nhân vật (kể cả phi kiếm, phép Thánh) |
| Ma "Huyết Tế chỉ NPC" | Áp dụng nguyên cho sát thương từ mọi nguồn khi giết |
| Thể "không vũ khí" | Mất hiệu lực với phi kiếm không cầm tay (N4) và Ngự Kiếm (đứng trên kiếm, không cầm) |
| Bản Mệnh Kiếm | Không rơi khi chết; cầm tay chỉ đánh được khi không có Đạo/Thể cấm |
| Bay | Chọn kiểu bay; chi phí ×2/×3; Thể chỉ tốn Bá Khí; Đạo cần cánh 60 |
| Ác Tính vs Thánh phép | Phép Thánh không giảm Ác Tính; Đạo không "tịnh hóa" Ma được |
| Tâm Ma vs Ác Tính | Ma có Ác Tính; đạo khác khi dùng đồ ma có Tâm Ma; hai thanh tách biệt |

### 3.3 Kiểm tra combo quá mạnh và đề xuất trần

Mô hình: R = TTD/TTK ở cấp 100 (số §1.2), nhân hệ số Phân Tâm (§3.1). **Mục tiêu trần: R của combo ≤ 1.25 × R của đạo mạnh nhất trong tổ hợp** (Thể 4.0 → trần 5.0; Ma ≈ 3.0 → 3.75). ⚠️ Ước lượng lý thuyết, cần playtest.

| Combo | R ước lượng (sau trần) | R nếu **không** có trần | Đánh giá | Trần chính liên quan |
|-------|------------------------|-------------------------|----------|----------------------|
| Kiếm+Đạo | burst 2.1 / bền 1.4 (tự buff ×0.5, Linh Lực ×2) | ~2.6 | **Thấp**: đóng vai DPS có hỗ trợ đội, không melee quái | T5 |
| Kiếm+Thể | burst **4.8** / bền 3.7 | ~5.4 | **Cao** (1.2× Thể) | T1, T2, T3 |
| Đạo+Thể | 2.8 (solo) – 3.5 (có Hộ Thể) | ~3.9 | **TB**; nguy cơ ở đội: "tanker cực bền phát buff" | T3, T5 |
| Ma+Kiếm | 2.3 | ~3.2 | **TB** | T4 |
| Ma+Đạo | 2.6 | ~3.4 | **TB-thấp** (hỗ trợ bị khóa self-only) | T4, T5 |
| **Ma+Thể** | **4.8** | **5.7 (1.43×)** | **Cao nhất**: tank + hút máu | **T3, T4** (trần then chốt) |
| Kiếm+Đạo+Thể | 3.9 | – | TB-cao; tiện ích rộng, DPS thấp | T1, T2 |
| Kiếm+Thể+Ma | 4.9 | 5.6 (tam) | **Cao** (1.23×) | T4 tam = 1.0% |
| Đạo+Thể+Ma | 3.6 | – | TB | T3, T5 |
| Kiếm+Đạo+Ma | 2.2 | – | Thấp | – |

**Sáu trần đề xuất** (áp dụng mọi đạo/combo):

| # | Trần | Giá trị |
|---|------|---------|
| T1 | **Phân Tâm** | Song: Công/TT/HH ×0.80, HP/Phòng ×0.90. Tam: ×0.70 / ×0.85 (§3.1) |
| T2 | **Khóa Tối Thượng chung** | Dùng 1 Tối Thượng → khóa mọi Tối Thượng khác **45s** (tránh burst 2–3 Tối Thượng nối tiếp) |
| T3 | **Trần giảm sát thương** (Phòng + Hộ Thể + Kim Thân + kết giới + lá chắn %) | Solo ≤ **80%**; đa đạo ≤ **70%** |
| T4 | **Trần hồi từ Hấp Huyết** | Solo ≤ **4% HP tối đa/s**; song ≤ **1.8%/s**; tam ≤ **1.0%/s** (Trị Liệu bản thân tính riêng, CD 12s) |
| T5 | **Quy tắc buff** | Tự buff Ban Phúc ×0.5; cùng loại không cộng; trần theo mục tiêu (§2.2); Đạo+Thể không tăng trần |
| T6 | **Trần cộng dồn Công** (Huyết Tế + tầng Bá Khí + trạng thái Ác Tính + Kiếm Ý + buff) | Solo ≤ **+90%**; đa đạo ≤ **+70%** |

### 3.4 UI tạo nhân vật (mock)

```
┌────────────────────── TẠO NHÂN VẬT ───────────────────────┐
│ Tên: [__________]   Xuất thân: Thôn Làng (roll lại: 0/1)   │
├──────────┬──────────┬──────────┬──────────┬───────────────┤
│ KIẾM TU  │ ĐẠO TU   │ THỂ TU   │ MA TU    │ SỐ NĂNG: 2    │
│  [ S ]   │  [ B ]   │  🔒 khóa │  [ A ]   │ (Song năng)   │
│   ✓      │   ✓      │          │          │               │
├──────────┴──────────┴──────────┴──────────┴───────────────┤
│ Kiếm Tu (S): EXP ×1.30   Kỹ Năng Bản Mệnh: Nhất Kiếm [S]   │
│ Đa đạo: dùng ĐỒNG THỜI · Chi phí ×2 · EXP cần ×1.6         │
│ Phân Tâm: Công/TT ×0.80 · HP/Phòng ×0.90                   │
│ Cảnh báo: Kiếm+Đạo → đòn thường lên quái = 0 (dùng kỹ năng)│
│ [Reroll tư chất 1/1]     [Xác nhận]     [Quay lại]         │
└───────────────────────────────────────────────────────────┘
```

---

## 4. Kỹ Năng Bản Mệnh (A trở lên)

Mỗi đạo có 3 skill; roll 1 (đều 33%) khi lộ tư chất A+. Skill là **hồi chiêu dài, mốc 40–120s**; tier nâng cấp thay đổi số + 1 cơ chế mới. Đây không thay Lõi. **Ô gắn: Kỹ Năng Bản Mệnh dùng chung ô Tối Thượng** (giữ 3+1): người chơi A+ chọn gắn **Tối Thượng Đạo (cấp 70)** hoặc **Kỹ Năng Bản Mệnh**, đổi được ngoài combat (xem câu hỏi Q2). Kỹ Năng Bản Mệnh mở từ cấp 10, dùng chung khóa Tối Thượng 45s (T2).

| Đạo | Skill | CD | A | S | SS | SSS |
|-----|-------|----|---|---|----|-----|
| Kiếm | **Nhất Kiếm Khai Thiên** | 45s | Lướt 8 block chém 300% Công | 450%, 12 block | 650%, xuyên khiên, để vệt 3s | 900%, 20 block, vệt kiếm khí 5s gây 100%/s |
| Kiếm | **Vạn Kiếm Quy Tông** | 60s | 8 kiếm bay, 40% Công/kiếm | 12 kiếm, 45% | 20 kiếm, 50%, truy đuổi | 36 kiếm, 55%, hợp nhất cuối thành 400% |
| Kiếm | **Kiếm Tâm Thông Minh** | 50s | +20% Bạo Kích 10s | +30% & bạo +30% | +40%, mọi đòn bạo hồi 2% Linh Lực | +50%, bạo kích bỏ qua 30% Phòng, kéo dài 14s |
| Đạo | **Thiên Sứ Giáng Lâm** | 90s | AoE 8 block: hồi 40% HP + 200% TT quái | 55% & 280% | 70% & 380%, 3s bất tử cho đồng đội | 100% & 500%, hồi sinh người ngã |
| Đạo | **Thánh Quang Phán Quyết** | 60s | Tia 500% TT, 1 mục tiêu | 700% | 950%, ×2 vs Hư Hóa | 1300% ×2.5 vs Hư Hóa, xuyên giáp 30% |
| Đạo | **Tịnh Hóa Kết Giới** | 100s | Vòm 6 block 8s: −30% sát thương nhận, gỡ Hư Hóa | 8 block, −40% | 10 block, −50%, hồi 3% HP/s | 12 block 12s, −60%, quái yếu Hư Hóa tan biến |
| Thể | **Bất Hoại Kim Thân** | 80s | −35% sát thương 8s | −45%, 10s | −55%, miễn khống chế | −65%, 12s, phản 30% sát thương |
| Thể | **Địa Liệt Quyền** | 55s | Sóng 8 block 250% Công, choáng 1.5s | 350%, 2s | 480%, 2.5s, kéo lại | 700%, 3s, nứt đất 4s |
| Thể | **Bá Vương Nộ** | 70s | Bá Khí→100 ngay, tầng IV 6s | 8s, +20% Công | 10s, giữ vĩnh viễn khi đang bật | 12s, tầng IV uy áp cả quái +5 cấp |
| Ma | **Huyết Hải Tràn Thiên** | 70s | Vùng 7 block 6s, 60%/s, hút 50% | 80%/s, hút 70% | 110%/s, hút 100% | 150%/s, hút 130%, +5 Huyết Tế/giết (chỉ khi giết NPC) |
| Ma | **Ma Thần Thân** | 120s | Biến hình 10s: +30% Công, Hấp Huyết +10% | 12s, +40%, +15% | 14s, +50%, +20% | 16s, +60%, +25%, không tăng Ác Tính |
| Ma | **Huyết Khế Đoạt Mệnh** | 60s | Giết ngay mục tiêu <15% HP (không boss, không người chơi) | <20%, boss 5% (boss: chỉ gây sát thương, không hấp thu) | <25%, boss <10% | <30%, boss <15%, hồi 30% HP khi thành công |

Ma Thần Thân: +10 Ác Tính khi bật (tier SSS: 0). Mọi buff Công/Hấp Huyết ở bảng chịu trần T4/T6 (§3.3).

Cân bằng: Kiếm/Đạo burst cao hơn nhưng CD ≥45s; Thể giảm sát thương cực mạnh nhưng sát thương tấn công thấp; Ma execute/hút dựa trên điều kiện.

---

## 5. Xuất Thân (6 loại, canon)

Tuổi khởi đầu (6–16) do **S6** quy định (canon §6); bảng dưới chỉ giữ buff/debuff và quà.

| Xuất thân | Tỉ lệ | Điểm spawn | Quà khởi đầu | Buff | Debuff |
|-----------|-------|-----------|--------------|------|--------|
| Thôn Làng | 28% | Làng nhỏ (vanilla village) | Cuốc, 16 bánh mì, 5 Hồn Tinh Bạch | Giá mua dân làng −10%, hồi Hunger +10% | Không có quan hệ phe |
| Thành Thị | 22% | Cổng thành có Ấn Hồn Đài | 30 Linh Thạch hạ phẩm, áo vải, Thiên Cơ Phù ×1 | Giá chợ −5%, +1 slot nhiệm vụ | HP gốc −5% |
| Con Hoang | 18% | Hoang dã ngẫu nhiên | Dao đá, da sống, lều (không quà khởi đầu thêm) | HP +10%, Hunger giảm chậm 20%, nhìn được kẻ thù xa +8 block | Không quan hệ phe, giá mua ×1.2, **danh vọng thành khởi đầu thấp** (debuff thật, R13) |
| Đệ Tử Tông Môn | 14% | Thành Nhân cấp gần nhất (không tự vào tông) | **Thư Tiến Cử** vào 1 tông Nhân cấp (R13), áo tông môn, 1 sách kỹ năng, quan hệ phe 200 | Hồi tài nguyên +10% khi ở tông môn | Nhiệm vụ tông bắt buộc 1 lần/tuần |
| Di Dân Phế Tích | 10% | Rìa Phế Tích Hư Hóa | Mảnh đồ cũ, 3 Hồn Tinh Bạch, la bàn tới thành gần nhất | **Kháng Hư Hóa +15%** | Thành lạ giá ×1.1; ác mộng (sương mù ngẫu nhiên) |
| Hậu Duệ Thế Gia | 8% | Trang viên thế gia | 100 Linh Thạch hạ phẩm, ngọc bội gia tộc, 1 pháp bảo Linh | +1 phẩm Hồn Tinh khởi đầu, quan hệ phe 100, **sàn tư chất C** (R13, nhận vào canon) | Kẻ thù gia tộc: tập kích nhỏ định kỳ (cấp thấp) |

Đa đạo + Đệ Tử Tông Môn: Thư Tiến Cử cho tông theo đạo có tư chất cao nhất (hòa → tông Đa đạo Vạn Tượng Thánh Tông nếu đủ điều kiện, S6). Thư Tiến Cử bỏ qua giới hạn tuổi vào tông (R14, S6).

### 5.1 Onboarding: mở khóa dần theo cảnh giới (R21)

Không phô bày toàn bộ hệ thống ngay từ đầu:

| Mốc | Hiện ra |
|-----|---------|
| Thường Dân (cấp 1–9) | Chỉ EXP, đạo, tư chất |
| Cấp 10 | Lõi; tông / thành chức vị |
| Bình Cảnh đầu tiên | Đan dược, Thiên Địa Linh Vật |
| Cấp 30 | Sứ Đồ, Văn Kiện |

Phần còn lại mở theo S6/S7; UI ẩn mục chưa mở, không khóa cứng cơ chế nền.

---

## 6. Pháp Bảo

### 6.1 Phẩm cấp & slot

| Phẩm | Yêu cầu cảnh giới (cấp) | Tỉ lệ luyện thành công (cơ bản) | Hệ số sức mạnh |
|------|------------------------|--------------------------------|----------------|
| Phàm | 1 | 95% | ×1.0 |
| Linh | 10 | 80% | ×1.4 |
| Huyền | 30 | 60% | ×2.0 |
| Địa | 50 | 40% | ×3.0 |
| Thiên | 70 | 20% | ×4.5 |
| Tiên | 90 | 8% | ×7.0 |
| Thần khí | 100 | 3% (chỉ từ Thần Khí Lò + Khảo Hạch) | ×12 |

**Slot (tự làm, không dùng Curios — canon §12)**: Chủ Bảo ×1, Hộ Bảo ×1, Giới ×2, Phù ×2, Túi Trữ Vật ×1 (bên cạnh 6 slot Hồn Cốt). Hồn Cốt khác pháp bảo (lấy từ boss, không luyện).
**Luyện khí**: Lò Luyện Khí tại thành/tông môn; công thức = Phôi + Hồn Tinh cùng phẩm + vật liệu (quặng linh, lõi quái Hư Hóa). Thất bại: mất vật liệu 50% (Hồn Tinh mất, Phôi còn). Có thể "tăng cấp" +1..+5 bằng Đá Tẩy Luyện (thành công giảm dần 90→50%). Ghi chú: phẩm quá cấp so với cảnh giới = có thể trang bị nhưng hiệu quả ×0.5.

### 6.2 Pháp bảo mẫu (≥8)

| # | Tên | Phẩm | Slot | Hiệu ứng | Dùng được Thể tu? |
|---|-----|------|------|----------|-------------------|
| 1 | Thanh Tâm Ngọc | Linh | Giới | Tâm Ma −0.3/s, Hồi Linh +10% (không giảm Ác Tính) | Có (hỗ trợ) |
| 2 | Hộ Thân Phù | Linh | Phù | Chặn 1 đòn chí mạng/90s (không chặn Tẩu Hỏa Nhập Ma) | Có |
| 3 | Kim Cang Hộ Tâm Kính | Huyền | Hộ Bảo | Phòng +25, giảm sát thương phép 15% | Có |
| 4 | Càn Khôn Túi | Huyền | Túi | 54 ô trữ, nhẹ | Có |
| 5 | Huyết Sát Châu | Huyền | Giới | Hấp Huyết +4% (chịu trần T4) | Có (hỗ trợ) |
| 6 | Phi Vân Chu | Địa | Chủ Bảo | Phi hành 1.4× cho 2 người, tiêu hao 3 LL/s hoặc đá | Có (chở người) |
| 7 | Cửu Thiên Lôi Châu | Thiên | Chủ Bảo | Ném sét AoE 500% Công/Thần Thức (CD 30s) | **Không** (tấn công, N8) |
| 8 | Chấn Hồn Chung | Địa | Chủ Bảo | Choáng 2s trong 6 block (CD 25s) | **Không** (N8) |
| 9 | Hồn Linh Đỉnh | Tiên | Chủ Bảo | Hồi 20% tài nguyên/20s, thanh tẩy Tâm Ma 40; với Ma tu: **Ác Tính −20 mỗi 180s** | Có |
| 10 | Thần Khí: Hư Không Kiếm Chủng | Thần | Chủ Bảo | Gắn Bản Mệnh Kiếm: +100% hệ số phẩm | Kiếm Tu/Ma Kiếm |

### 6.3 Thể tu & pháp bảo
- Tag `hukhong:offensive_treasure` (Lôi Châu, Chấn Hồn Chung…) và mọi pháp bảo gây sát thương trực tiếp (nguồn N8) → Thể tu: **vô hiệu, không kích hoạt** (Thể cấm N8, §3.2); hiển thị tooltip rõ.
- Thể tu **được dùng** pháp bảo thuộc nhóm `defensive` / `support` (hộ thân, phù, trữ vật, di chuyển), **không** cầm làm vũ khí. Bonus phẩm áp dụng ×1.0 (không phạt).
- Ma Thể (nhánh): pháp bảo tấn công hoạt động ×0.6 (đồng bộ luật vũ khí).

---

## 7. Câu hỏi cho user (còn mở, ≤3)

Đã chốt (không hỏi lại): Thể bay chỉ Bá Khí · Đa đạo dùng đồng thời ×2/×3 · Đạo tu đòn thường lên người chơi ×1 · Bản Mệnh Kiếm không rơi.

| # | Câu hỏi | Lựa chọn | Gợi ý |
|---|---------|---------|-------|
| Q1 | Chấp nhận **trần đa đạo** vì canon chưa quy định chỉ số hợp nhất: Phân Tâm (song Công/TT/HH ×0.80, HP/Phòng ×0.90; tam ×0.70/×0.85) + khóa Tối Thượng chung 45s + trần hồi Hấp Huyết + hệ số EXP = trung bình cộng các đạo? | A) Chấp nhận (bản này) · B) Không giảm chỉ số, chỉ tăng phạt EXP lên song ×2 / tam ×3.5 · C) Cộng dồn đầy đủ (không khuyến nghị) | **A** — nếu không có Phân Tâm, Ma+Thể có R≈5.7 (1.43× Thể solo) |
| Q2 | Kỹ Năng Bản Mệnh (A+) gắn ở đâu trong khung **3 phím + 1 Tối Thượng**? | A) Dùng chung ô Tối Thượng, chọn 1 trong 2 (bản này) · B) Thêm phím thứ 5 riêng | **A** — giữ đúng canon "3 + 1", tránh bàn phím quá tải khi đa đạo |
| Q3 | PvP Ma (giết người chơi: chỉ Ác Tính +10, Ác Danh, bounty, không lợi ích) bật mặc định thế nào? | A) Bật mặc định · B) Chỉ khi hai bên đồng ý/đang "chiến" · C) Tắt PvP Ma | **B** nếu server thân thiện, **A** nếu server hardcore |

---

## Đề xuất đổi canon (KHÔNG tự sửa PLAN-v2)

1. **§3.1 Chỉ số hợp nhất + Phân Tâm** (song Công/TT/HH ×0.80, HP/Phòng ×0.90; tam ×0.70/×0.85), hệ số EXP tư chất = **trung bình cộng** các đạo.
2. **§3.1 Khóa Tối Thượng chung 45s** và **phím `G` chuyển trang** (mỗi đạo 1 trang 3+1) cho đa đạo.
3. **§3.1 Luật phán xử "theo nguồn đòn"** thành canon: 8 nguồn N1–N8, quy tắc SỞ HỮU rồi CẤM (mở rộng ví dụ Thể/Đạo hiện có); Đạo cấm N1–N3 lên **quái/NPC** (không phải chỉ "quái").
4. **§3 Ma**: khi đa đạo có Ma → nhánh Ma cố định **Ma Thuần**; mọi hỗ trợ của đạo khác **chỉ tác dụng lên bản thân**.
5. **§3 Thể**: bay **1.0 BK/s, ngưỡng cất cánh 20 BK**, bị động Khí Hải (sàn 20 BK ngoài combat từ cấp 60), thiền +5 BK/s tới 60.
6. **§3 Ma / §8**: thêm **Danh Tiếng Thành** (−1000…+1000 theo thành), `KillRateGate` dùng chung (R3) và quota Huyết Tế (R17) (phối hợp S4/S5/S6).
7. **§2/§3 Ma**: bảng Ác Tính với ngưỡng Mất Kiểm Soát (≥80, xác suất `(AT−75)%`/20s, 4s, Ác Tính −10 sau đó) và Tẩu Hỏa (95 đếm ngược 8s, 100 chết); Căn Cơ Tổn Thương = chuẩn S3 (R11) cho Mất Kiểm Soát/chết thường; Tẩu Hỏa = phế tu vĩnh viễn, làm lại từ đầu (R23).
8. **§5 Lõi**: Kỹ Năng Bản Mệnh dùng chung ô Tối Thượng (nếu user chọn Q2-A).
9. **Trần chung** T3 (giảm sát thương 80%/70%), T4 (trần hồi Hấp Huyết 4%/1.8%/1.0% HP/s), T6 (cộng dồn Công +90%/+70%) thêm vào canon cân bằng.

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | S2-paths.md (v1, bản nháp đầu bám PLAN-v1) | 4 đạo, tư chất, xuất thân, pháp bảo | ✅ Superseded bởi v2 |
| v2 | S2-paths.md | v2: Thể bay chỉ Bá Khí, đa đạo đồng thời + trần, Đạo hỗ trợ, Ma viết lại, bộ phím 3+1 | Superseded bởi v2.1 |
| v2.1 | S2-paths.md (this file) | R3, R10, R11, R13, R17, R21 | ✅ Current |
| canon | ../PLAN-v2-tu-tien-mod-canon.md | Luật chung | ✅ Current |
