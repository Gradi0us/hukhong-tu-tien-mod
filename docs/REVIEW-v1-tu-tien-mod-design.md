# REVIEW-v1-tu-tien-mod-design

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — Reviewer rà soát PLAN-v2 canon + S1…S7 (v2)
**Status**: Draft review, chờ user chốt mục 6 và thứ tự mục 7
**Phạm vi đọc**: canon đọc đủ. S3, S6, S7, S4 §0/§1/§3.5/§3.6/§4.0/§8, S1 §3/§5–§11, S2 §2.4/§3.3/§5/§7, S5 §0/§3.6/§3.8/§6–§8 đọc kỹ. Các phần khác (S2 §1/§2.1–2.3/§4/§6, S5 §2/§4, S1 §1–2/§4, S4 §2/§5/§7) chỉ lướt tiêu đề, nên mâu thuẫn ở đó có thể còn sót.
**Quy ước**: "S3§1.5" = file S3, mục 1.5. Số liệu là tính lại từ công thức trong chính các file (Fermi), chưa phải playtest.

---

## Key Findings (kết luận trước)

| # | Kết luận | Mức |
|---|----------|-----|
| 1 | Bộ tài liệu **nhất quán ở tầng ý tưởng** nhưng **lệch ở tầng số liệu và luật biên**: tìm được 18 mâu thuẫn, 6 mức CAO. Phần lớn do các file viết song song, mỗi file tự đề xuất chuẩn riêng (đồng hồ tuổi ×3, Tẩy Hồn ×2, Thọ Nguyên Đan ×2, chống farm ×3) | CAO |
| 2 | **Lỗ hổng nặng nhất là AFK farm quái tự nhiên**: ngưỡng chống farm (30 con/10 phút/loài) cho ~0,9 E(L)/giờ chỉ với **một loài**, tức gần bằng tốc độ thiết kế của cả hành trình. Spawner = 0 không chặn được | CAO |
| 3 | **EXP vanilla cấp 1–9 sai bậc độ lớn**: S4 nói "~670 con/cấp", thực tế 667 con (cấp 1–5) tăng tới **6.667 con ở cấp 9**, tổng ≈ 16.000 con để lên cấp 10 | CAO |
| 4 | **Cờ "≤500 giờ" chỉ hợp với Tư chất B đơn năng**. Tam năng không vật liệu (~1.239 giờ) chạm Thọ Nguyên Đấu Giả trước cấp 90 và, vì quá 500 giờ, sẽ bị kết liễu bởi tuổi | CAO |
| 5 | Đường **chuyển đan / Linh Vật / Tẩy Tủy Đan giữa người chơi** (kể cả qua alt) phá toàn bộ mô hình 110 giờ / 500 giờ; chưa có bind nào | CAO |
| 6 | Độ phức tạp: ~12 hệ thống lớn lộ ra cùng lúc ở cấp 1–10 (Lõi, Đan Độc, Tâm Ma, tuổi, tông, thành, chức vị, Danh Vọng, Cống Hiến, Ác Danh…). Cần **mở khóa dần theo cảnh giới** (mục 4.3) | CAO |

---

## 1. Bảng MÂU THUẪN giữa các file

Chọn chuẩn theo nguyên tắc: (1) user đã chốt > (2) file sở hữu miền đó (S3 sở hữu EXP/Lõi, S6 sở hữu tuổi/tông/thành, S7 sở hữu đan, S4 sở hữu bản đồ/quái) > (3) file viết sau và có mô hình tính.

| # | Chủ đề | Bên A | Bên B | Mức | Đề xuất chuẩn |
|---|--------|-------|-------|-----|---------------|
| C1 | **Tư chất D ≈826 giờ vs luật 500 giờ** (yêu cầu (a)) | S3§1.5: D không VL 826 h, C 620 h, song 793 h, tam 1.239 h; S3§9 Q1 tự cảnh báo | S6§1.5: "Thọ Tận" chỉ miễn nếu **tổng giờ ≤500** (cờ Bảo Hộ); bảng đệm §1.4 chỉ mô phỏng đường B. Canon §6 cũng chỉ nói "≤500 giờ" | CAO | Dùng số S3 làm chuẩn. **Bỏ cái chết vì tuổi hẳn** (Ngũ Suy dừng ở giai đoạn V). Tính lại: tam năng không VL đạt cấp 89 ở ~1.150 giờ ⇒ tuổi 16+287 = 303 > Thọ 300 ⇒ vào Ngũ Suy trước khi tới Thần. D và song năng thì còn đệm (D: tuổi ~208 tại cấp 89) |
| C2 | **Ba đồng hồ tuổi** | S6§1.2: `onlineTicks` cá nhân, 24.000 tick = 1 tháng, ngủ/`/time set` không tính | S5§3.8: đếm `getDayTime()/24000`, ngủ nhảy ngày vẫn cộng (chặn 12 tháng/lần); S7§8.2#3: dùng `getGameTime()`; canon §6: "1 ngày MC = 1 tháng" | CAO | Chuẩn = S6 (`onlineTicks` cá nhân). Sửa S5§3.8 và S7. S4 chưa ghi chú Thánh Địa tuổi ×0,5 mà S6§1.2 yêu cầu |
| C3 | **Thọ Nguyên Đan** | S6§1.5: +5…+100 năm (Nhất…Cửu), tổng Thọ Bổ Sung ≤50% Thọ cơ bản, CD 4 h | S7§2.D Diên Thọ Đan: +10…**+2.500** năm, "2 viên đầy hiệu quả mỗi bậc"; Trường Sinh Thần Đan +5.000 năm "chỉ cấp 100" trong khi S6 nói cấp 100 tắt hệ Tuổi | CAO | Chuẩn = S6. Xóa Trường Sinh Thần Đan hoặc đổi thành vật phẩm cosmetic |
| C4 | **Vị trí Thiên Khải Hoàng Đô** (yêu cầu (b)) | S1§6: "Trung tâm bản đồ" | S4§1.2: (3.300, −2.500), vòng 4, ĐB; S4 tự đề xuất sửa S1 (Đề xuất #5). S4 còn ghi Vạn Tượng "đối xứng Hoàng Đô qua tâm" nhưng (−3.000, 3.600) không đối xứng (3.300, −2.500) | TB | Chuẩn = S4. Sửa S1 thành "trung tâm chính trị/giao thông". Đổi chú thích đối xứng của Vạn Tượng. Cân nhắc dời Hoàng Đô về vòng 3–4 gần tâm hơn (xem 4.2 #8) |
| C5 | **Thanh Vân "điểm khởi đầu phổ biến"** | S1§6 | S4§1.1–1.2: spawn ở vòng 1 gần Lưu Ly Cảng; Thanh Vân (1.500, 2.700) ở vòng 3, cấp vùng 35–59 | TB | Chuẩn = S4. S1 đổi Lưu Ly Cảng làm "cửa vào phổ biến", Thanh Vân là "thành lớn nhất" |
| C6 | **Tỉ lệ rơi Văn Kiện** (yêu cầu (c)) | S1§3.4: Hạ 4% · Trung 10% · Thượng 22% · Đại 45% | S4§3.6: 6/14/28/55% × hệ số loại (Ám Ngữ ×2,0 … Hộ Pháp ×0,6), trần 100%, bắt sống Ám Ngữ = 100% | TB | Chuẩn = S4 (có mô hình "3 văn kiện trong ≈3 cứ điểm"). Nhưng xem E8: bắt sống 100% là cửa farm |
| C7 | **EXP quái vanilla cấp 1–9** (yêu cầu (d)) | S3§1.6 + S4§3.5: hệ số ×20 (m = 0,30→0,03), "~670 con/cấp" | Tính lại từ chính công thức: số con = E(L)/(m·E(L)/200) = 200/m. m=0,30 ⇒ **667** (cấp 1–5); m=0,20 ⇒ 1.000; 0,12 ⇒ 1.667; 0,06 ⇒ 3.333; 0,03 ⇒ **6.667** (cấp 9). Tổng ≈ **16.000** con | CAO | Không chấp nhận "~670/cấp". Chọn S4 Q3-B nhưng cao hơn: m **phẳng 1,5** cho cấp 1–9 (≈130 con/cấp, ≈1.200 con tổng) + nhiệm vụ nhập môn gánh phần còn lại; từ cấp 10 về 1,5% như cũ |
| C8 | **Tỉ trọng quái Hư Hóa** | S3§1.6: 26% ⇒ ≈52 con/cấp | S4§3.5: "≈70 con/cấp ở tỉ trọng 35%" (tàn dư v1) | THẤP | Chuẩn = S3 (tổng tỉ trọng cộng đủ 100%). Sửa S4 |
| C9 | **Luật chống farm quái** | S3§1.6: >30 con cùng loại / 32 block / 10 phút ⇒ ×0,5 dần ×0,1 | S4§3.5: >30 cùng loài / **100 block** / 10 phút ⇒ dần 10%; S2§2.4.5: Chuỗi Cùng Loại 128 block, bậc 10/25/50, **chỉ Ma tu** | TB (cao hơn ở E2) | Một cổng duy nhất `KillRateGate` trong `gates` (xem 4.2 #1); xóa 3 bản |
| C10 | **Vật phẩm mốc nhẹ 91–95/96/99** (yêu cầu (e)) | S3§1.3.2: dùng Hoàng Cực Đan (phẩm 5) cho 91–95, Thánh Khai (7) cho 96, Kình Thiên (9) cho 99; "S7 xác nhận" còn treo | S7§2.A: mỗi Phá Cảnh Đan "chỉ dùng được ở Bình Cảnh của mốc đó", "1 viên/mốc"; S7§0: phẩm thấp hơn ≥2 bậc so với mốc ⇒ **vô hiệu**; Hoàng Cực chỉ có nguồn L, R (không M) | CAO | Giữ ý S3 (đúng ngân sách 17,7 h) nhưng S7 thêm nhóm **"Tiểu Phá Đan" (7 loại, phẩm 5/7/9 gắn nhãn mốc nhẹ)**, miễn luật vô hiệu, có nguồn M. Nếu không, mốc nhẹ thành 7 Cửu phẩm ⇒ tuột khỏi mô hình |
| C11 | **Tẩy Hồn** | S3§4.5: Tu Tệ 20k–1M, Hồn Tinh, Tẩy Hồn Thạch 3–8, Luân Hồi Hoa 1–2; roll **3 ô thường, không Phó Mặc**; Thạch ghép 5 Mảnh; ≈12–30 h | S7§6: 1M Linh Thạch, Hồn Tinh 40% quỹ, **Thọ −10 năm**, mất EXP cấp, CD 14 ngày, **mở lại 4 ô có Phó Mặc**; Thạch ghép 9 Mảnh, 20% từ Chấp Hành Giả; ≈8–12 h | CAO | Chuẩn = S3 (sở hữu miền Lõi, đã có bảng pity). S7 §6 chỉ giữ nguồn Luân Hồi Hoa ở "Vong Xuyên Ngạn". Thọ −10 và mất EXP cấp: bỏ (trái "không bao giờ mất EXP" của S3§8) |
| C12 | **Đơn vị tiền** | S2§5: "đồng" | S3: "Tu Tệ"; S7§5.1: Linh Thạch 4 bậc [GIẢ ĐỊNH]; S6 không nêu | TB | Chuẩn = Linh Thạch (S7). Emerald chỉ dùng cho giao dịch dân làng vanilla. Quy đổi bảng giá S3 (1 Tu Tệ = 1 Hạ) |
| C13 | **Căn Cơ Tổn Thương** | S3§8 (user chốt B): tối đa 3 tầng, −15%/tầng, 10 phút/tầng; Tẩu Hỏa = 3 tầng 30 phút; **không mất EXP** | S2§2.4.3: −25% HP/Công/Thần Thức, **EXP ×0,5, 90 phút online**, ×2 thời lượng lần 2, **mất 10% EXP cấp hiện tại** | CAO | Chuẩn = S3. S2 sửa; nếu muốn Tẩu Hỏa nặng hơn thì chỉnh số tầng/thời gian, không thêm mất EXP |
| C14 | **Ấn Hồn Đài theo cấp thành; chỗ làm Khảo Hạch** | Canon §1.1 + S3§2: về thành/tông/Thánh Địa bất kỳ; S3§7: Khảo Hạch ở Thiên Kiếp Đài trong Thánh Địa | S6§3.1: Nhân mốc 10–40, Địa 10–70, Thiên mọi mốc **+ Khảo Hạch** | TB | Xem Q10. Khuyến nghị bỏ giới hạn theo cấp thành: một thành thất thủ không được khóa tiến độ cả server |
| C15 | **Xuất thân "Đệ Tử Tông Môn"** | S2§5: spawn ở cổng tông **của đạo chính**; đa đạo → Vạn Tượng | S6§1.3: Ngoại Môn của **tông Nhân**, không phải Huyết Ảnh. Vạn Tượng đòi ≥Lv30 và Địa đòi ≥Lv20 (S6§2.2) | TB | Chuẩn = S6 (Nhân tông). S2 sửa; Ma tu Đệ Tử ⇒ Thư Tiến Cử (không auto vào Huyết Ảnh) |
| C16 | **Thăng Thần: Thần Đan vs Hỗn Độn Thanh Liên Tử** | S3§7: Thần Đan bắt buộc | S7§3.1: Hỗn Độn Thanh Liên Tử chỉ **xuất hiện sau khi Tầng II mở**, mà Tầng II mở **sau** Khảo Hạch + kết thúc (canon §9) ⇒ vòng lặp | CAO | Xóa Hỗn Độn Thanh Liên Tử khỏi đường Thăng Thần; chuyển thành vật phẩm Tầng II |
| C17 | **Giới hạn tuổi tông vs nhịp S3** | S6§2.2: Thiên ≤30 tuổi, Địa ≤36, Nhân 45–50 | S3§1.5: B không VL tới cấp 30 ở ~85 giờ (tuổi 37 nếu khởi đầu 16). Vạn Tượng đòi Song ≥A + Lv30 + ≤30 tuổi ⇒ phải xong Lv30 trong ≤56–96 giờ, song năng B có VL mới ~27 giờ | TB | Chấp nhận (ý "cơ duyên") nhưng nới Thiên lên 40, Địa 45 hoặc trừ giờ Thánh Địa; giữ Thư Tiến Cử làm cửa sau |
| C18 | **Tuổi khởi đầu** (yêu cầu (f)) | S1§9 = S6§1.3 = 6/8/10/12/14/16, **không mâu thuẫn số** | Nhưng S6§1.3 thêm "Hậu Duệ: tư chất sàn C" mà canon §4 và S2§5 không có; Con Hoang 6 tuổi cho dư 40 giờ so với Hậu Duệ khi tính trần tuổi tông/Thiên Tài Trẻ ⇒ meta chọn Con Hoang | TB | Giữ số. Đưa "sàn C" vào đề xuất đổi canon; cân Con Hoang bằng debuff có thật (xem E5) |
| C19 | **Roadmap P1** | S5§6: P1 gồm cấp 31–60, Phá Cảnh Đan phẩm 3–5 | Cấp 60 cần Đế Hoàn Đan phẩm 6 (S7§2.A); S5 xếp phẩm 6–9 vào P2 | THẤP | Chuyển phẩm 6 sang P1 hoặc cắt P1 ở cấp 59 |

---

## 2. LỖ HỔNG KHAI THÁC (exploit)

Mỗi mục: kịch bản → hậu quả → cách chặn.

**E1. Farm dân làng cho Ma tu (bỏ qua 3 lớp chống farm).**
- Kịch bản: (a) S2§2.4.5 giới hạn 3 dân/ngày **mỗi làng**, nhưng "làng" định nghĩa bằng gì? Người chơi dựng nhiều chuông (bell) gần nhau, mỗi chuông một "làng", mỗi làng 3 suất ⇒ 20 làng = 60 dân/ngày. (b) Ác Danh theo S6§3.4 chỉ tính trong 400 block quanh thành/thôn của 10 thành; **làng vanilla xa thành không bị Ác Danh**, chỉ Ác Tính (+5) và Danh Tiếng Thành cục bộ. (c) Người chơi khác (không phải Ma) nuôi dân làng ở sảnh nhân giống rồi đưa cho Ma bạn; "dân non 3 ngày game" chỉ chặn trẻ mới sinh, 3 ngày game = 1 giờ thật.
- Hậu quả: Ma tu hấp thu ×1,45 EXP ổn định, "chi phí" chỉ là Ác Tính mà thiền hạ 1,5/s, Thanh Tâm Đan hạ 15–80. Lộ Hấp Thu thắng áp đảo, phá cân bằng 100 h vs 129 h của S2.
- Chặn: quota **toàn cục theo người chơi** (ví dụ 20 lợi ích dân/ngày thực, không theo làng); dân sinh ra do nhân giống hoặc cứu chữa (zombie villager cure) gắn tag `hukhong:bred` ⇒ 0 lợi ích; áp Ác Danh cho **mọi `Villager`** ở mọi nơi; mỗi con trong "làng" giết xong ghi vào SavedData để Tra Xét (S6§4) thấy.

**E2. AFK farm quái tự nhiên (không dùng spawner).**
- Kịch bản: bẫy rơi trong vùng Hư Hóa, hoặc nền sinh quái kiểu vanilla trong bóng tối. Quái tự nhiên ⇒ không dính tag spawner. Tính: mỗi con cho `E(L)/200`. Ngưỡng S3: 30 con/loài/10 phút ⇒ 180 con/giờ ⇒ **0,9 E(L)/giờ chỉ với một loài**; tốc độ thiết kế cả hành trình là ~1 E(L)/giờ ở cấp 40–50. Thêm 3 loài khác (zombie, skeleton, spider, creeper biến thể) ⇒ ~3,6 E(L)/giờ. Với bẫy 600 con/giờ, giảm ×0,1 vẫn còn ≈222 con-tương-đương ≈ **1,1 E(L)/giờ** khi không ai điều khiển.
- Hậu quả: 110 giờ và 500 giờ đều thành vô nghĩa; mục tiêu "quái chỉ 26% EXP" bị phá; server vừa AFK vừa tạo lag (S5 R5).
- Chặn: `KillRateGate` đếm **theo người, mọi loài gộp**, trần EXP quái ≈ 1,2 × tốc độ thiết kế mỗi giờ (cửa sổ trượt 60 phút); chỉ tính kill khi người chơi gây ≥50% sát thương **và đã di chuyển/nhập lệnh gần đây** (chống AFK); kill bằng lava, rơi, ngạt, mob trap (quái không từng có đường đến người chơi) = 0 EXP; mob không "aggro" quá 3 s thì không tính. Đặt trần này vào S3 chứ đừng giao cho 3 file.

**E3. Chuyển đan / Linh Vật giữa người chơi phá Bình Cảnh.**
- Kịch bản: người cày hardcore farm Hồng Mông Tử Khí Chi, Huyết Bồ Đề… (S7§3.1 cho phép "đổi chủ" Luân Hồi Hoa, và không thấy bind nào cho Phá Cảnh Đan) rồi phát cho cả hội: người mới nhảy qua cổng 56 giờ (mốc 70/80/90) ngay.
- Hậu quả: mất pillar 2 ("cơ duyên quyết định tốc độ"); điểm Linh Vật cấp 7–9 chỉ vài chỗ/thế giới nên **một hội ăn hết, phần còn lại của server không có**; S7§3.1 tự thừa nhận không đủ cho cả server.
- Chặn: Phá Cảnh Đan và Linh Vật phẩm ≥5 là **bind-on-use** (dùng xong ghi dấu chủ, 3 Chu Kỳ không giao dịch lại) và **bind-on-pickup cho người nhặt Linh Vật**; cho phép trao đổi **nguyên liệu thô và đan phương**, không trao đổi thành phẩm đột phá; cấm dùng đan cho mốc cao hơn cảnh giới người nhận quá 1 mốc.

**E4. Alt account làm Thành Chủ / tông môn / bơm Cống Hiến.**
- Kịch bản: S6§5 đã chặn bầu cử (cùng IP = 1 phiếu, cư trú ≥3 Chu Kỳ) nhưng còn: (1) alt Hộ Vệ chỉ cần DV 500 + ĐPT 100 + 1 raid ⇒ trọng số phiếu ×2 mỗi alt, 5 alt = 10 phiếu; VPN vượt rào IP. (2) **Đường ống Cống Hiến → đan**: mỗi alt ở tông Nhân kiếm 400 CH/Chu Kỳ (CH không giao dịch được) nhưng mua Phá Cảnh/Thọ Đan **là đồ giao dịch được** (S6§2.1: "1/Chu Kỳ") rồi tặng chính. (3) Tiến cử: Chân Truyền+ bảo lãnh bỏ tuổi, tư chất −1 bậc, mỗi Chu Kỳ 1 người ⇒ đưa alt vào Vạn Tượng. (4) Thành Chủ trục xuất 1 người/Chu Kỳ để gây khó người mới (kho riêng giữ nhưng mất cư trú ⇒ mất Ấn Hồn Đài nếu C14 giữ nguyên).
- Hậu quả: CH, đan hiếm, ghế quyền lực thành tài sản của hội.
- Chặn: bind đan mua bằng CH vào người mua (kết hợp E3); phiếu bầu cần **giờ chơi thật ở thành ≥5 giờ + đã làm ≥1 hành vi phòng thủ khác alt**; giới hạn tiến cử theo tổng bảo lãnh trọn đời (3 người); trục xuất cần quá bán Hội Đồng; log hành vi gộp theo account-cluster (admin xem).

**E5. "Ép tuổi" và lách đồng hồ tuổi.**
- Kịch bản: (1) vì C2, server dùng S5 (`getDayTime`) cho phép ngủ để nhảy ngày ⇒ mọi người online già thêm khi một người ngủ. (2) Bảo Hộ ≤500 giờ tính theo **tổng giờ online**, nên người AFK 300 giờ trong Thánh Địa hết Bảo Hộ mà chưa tiến bộ; ngược lại Thánh Địa tuổi ×0,5 cho phép "ngâm" ở đó để giữ tuổi ≤30 qua cổng Vạn Tượng. (3) Con Hoang (6 tuổi) luôn tối ưu cho tuổi trần và danh hiệu "Thiên Tài Trẻ" (cấp 20 trước 18 tuổi dễ hơn Hậu Duệ ~40 giờ). (4) Chuyển Thọ Nguyên Đan giữa người (không bind).
- Chặn: một đồng hồ `onlineTicks` có chặn AFK (không tăng khi không input 5 phút); Bảo Hộ tính theo **giờ hoạt động** không phải tổng giờ, hoặc bỏ hẳn chết vì tuổi; Thánh Địa ×0,5 chỉ khi đang Thiền (không đứng đợi); Con Hoang cần debuff thật (ví dụ không có Thư Tiến Cử miễn khảo hạch, DV khởi đầu −100 đã có nhưng yếu).

**E6. "Mắc Kẹt 1%" + rời server / save-scum.**
- Kịch bản: (1) Alt+F4 hoặc kill process sau khi thấy ô Phó Mặc ra xấu (Δ≤−2 hoặc Mắc Kẹt) trước khi autosave ⇒ reroll miễn phí, đặc biệt singleplayer và server crash-recover. (2) Đang trong instance Phá Chấp thì rời server hoặc server restart ⇒ dimension tạm biến mất; đăng nhập lại rơi vào void/spawn và có thể bị đánh dấu "đã xong". (3) Người bị Kẹt bỏ server vĩnh viễn: EXP đóng băng, vô hại, nhưng nếu họ đang giữ chức vị thì chức vị vẫn mở (xử lý bằng hạn chót vắng mặt 7/14 ngày của S6 là ổn). (4) Mở/đóng Ấn Hồn Đài nếu roll 3 ô sinh lúc mở menu mà không lưu ⇒ reroll vô hạn không cần Thiên Cơ Phù.
- Chặn: **seed roll lưu vào Data Attachment ngay khi đạt mốc** (trước khi mở Ấn Hồn Đài), kết quả chọn ghi + `server.saveEverything` đồng bộ trước khi trả UI; nhiệm vụ Phá Chấp lưu theo tiến độ (màn 1/2/3), thoát giữa chừng ⇒ đăng nhập ở cổng Ấn Hồn Đài với màn dở dang; instance không phải nguồn trạng thái.

**E7. Boss: ké loot, power-level, phá HP bằng đám đông.**
- Kịch bản: (1) Loot cá nhân đủ điều kiện nếu "có mặt ≥60% thời lượng" (S4§4.0) ⇒ 10 alt đứng ngoài tầm đòn nhận 10 lượt roll Tẩy Hồn Thạch/Mảnh Thiên Môn (100% mỗi người lần đầu). (2) Không có hiệu chỉnh chênh cấp cho người chơi **thấp hơn** boss: nhân vật cấp 20 làm Đạo tu buff trong trận Thạch Hống (67) vẫn có Điểm Đóng Góp, EXP trần 2,0 E(L) mỗi trận. (3) HP tăng 0,75/người trong 48 block: nhóm 3 người bị 3 kẻ phá đám đi vào bán kính ⇒ HP ×2,5, không thể giết. Tỉ lệ %HP giữ, nhưng hết người ra thì mất ≥10 s (S4§4.0).
- Chặn: n chỉ tính người **có đóng góp trong 30 s gần nhất hoặc cùng party**; loot cần Điểm Đóng Góp ≥5% **và** sát thương/hồi/đỡ tuyệt đối tối thiểu theo cấp; EXP boss nhân hệ số chênh cấp (người thấp hơn boss ≥15 cấp ×0,1); nhật ký cho admin.

**E8. Văn Kiện: kho chung server và farm Ám Ngữ Giả.**
- Kịch bản: kho **chung toàn server** (S4 Q1-A) ⇒ một kẻ phá đám dùng Dẫn Dụ (6 văn kiện) kích hoạt sớm Chấp Hành Giả trong lúc cả server còn cấp 40; "bắt sống Ám Ngữ Giả = 100% 1 Văn Kiện cao nhất" (S4§3.6) + Thương Đội ngụy trang lặp lại ⇒ farm. Trùng ID đổi Mảnh Văn Kiện nên không có ngưỡng chết.
- Chặn: Dẫn Dụ chỉ dành Thống Lĩnh+ hoặc có xác nhận cả party; "bắt sống" 100% chỉ 1 lần/ngày thực/cá nhân; kho chung chỉ ảnh hưởng lộ hang ổ, còn "kích hoạt sớm" yêu cầu người kích hoạt ở trong party tham chiến.

**E9. Raid hoãn khi không ai online = kho vô địch.**
- Kịch bản: S4§5.3 và canon §8 hoãn tập kích khi cư dân offline. Hội có thành giữ lịch online lệch giờ raid ⇒ thành không bao giờ mất, trong khi "mất thành" là cơ chế nguy hiểm trụ cột.
- Chặn: cho phép **tích lũy áp lực** (đồng hồ raid đóng băng nhưng tăng cường độ lần sau, có trần), và raid chạy khi online đủ ngưỡng ≥ ngưỡng giờ/tuần; thành vô chủ NPC vẫn có raid mô phỏng.

---

## 3. INVERSION (giả định sai → lật ngược → chân lý)

**Giả định sai #1: "500 giờ không vật liệu là chấp nhận được."**
- Lật ngược: nếu 500 giờ là nhiều, ai sẽ thấy nửa sau game? Với 6 giờ/tuần (3 phiên × 2 giờ) thì 500 giờ = 83 tuần ≈ 1,6 năm; 110 giờ = 18 tuần.
- Chân lý: game tu tiên Trung Quốc có "đời" ngắn nên tuổi thọ tạo áp lực tốt. *Amazing Cultivation Simulator* (Mịch Trường Sinh) và *Tale of Immortal* (Quỷ Cốc Bát Hoang) là sim/roguelite: một đời vài giờ đến vài chục giờ, chết thì làm đời mới, nên tuổi thọ và đột phá thất bại là **chất liệu chơi lại**, không phải xóa 500 giờ. Mod này là **một nhân vật xuyên suốt**. *Diablo 3/4* chia theo mùa với mục tiêu khoảng vài chục đến ~100 giờ; *Terraria* khoảng 40–60 giờ cho toàn bộ tiến trình, bị chặn bởi boss/vật liệu chứ không bởi "giờ". Nên coi 500 giờ chỉ là "đường khổ nhất", cần có **nhịp thưởng mỗi 5–10 giờ** (cấp 1–99 đã có mốc nhưng đoạn 56 giờ ở cấp 89 là trống). (Các con số giờ của game khác là ước lượng từ kinh nghiệm cộng đồng, cần xác minh.)

**Giả định sai #2: "Người chơi sẽ đọc cốt truyện qua Văn Kiện."**
- Lật ngược: nếu 90% bỏ qua chữ thì cốt truyện (20 mảnh ở S1§10) có ý nghĩa gì?
- Chân lý: trong *Diablo*, *Path of Exile* và các modpack như *RLCraft*, văn bản lore trong inventory hầu như bị bỏ qua; *Dark Souls* thành công vì mô tả vật phẩm **ngắn, gắn cơ chế** và vật phẩm đó dùng được. Văn Kiện cần **lợi ích cơ chế ngay** (bản đồ, hướng hang ổ — S4 đã có) và **giọng nói/NPC/cảnh** để kể, còn chữ chỉ là lớp phụ. Xem ý cốt truyện 4.1 #1–#3.

**Giả định sai #3: "Mất thành tạo áp lực tích cực."**
- Lật ngược: nếu thua là mất công sức tháng trời thì người chơi chọn đóng game, không chọn bảo vệ thành.
- Chân lý: *RimWorld* làm "thất bại thú vị" vì colony nhỏ, có save và lịch sử; *Rust/ARK* wipe base khiến phần đông rời server sau vài lần. S4 đã có Phế Tích + tái chiếm + kho riêng giữ, rất đúng hướng. Điều còn thiếu: người chơi phải thấy **vì sao thua và chuẩn bị được** (báo trước raid trong N phút, quyền đóng góp phòng thủ cho cả cư dân), và **thua không khóa tiến trình** (Ấn Hồn Đài ở C14).

**Giả định sai #4: "Giao dịch tự do làm kinh tế khỏe."**
- Lật ngược: nếu mọi thứ mua được thì hành trình 110 giờ có còn "cơ duyên"?
- Chân lý: *Diablo 3* đóng nhà đấu giá tiền thật/vàng (2014) vì người chơi chơi game bằng cách mua đồ; *Path of Exile* tách hẳn **SSF (Solo Self-Found)** cho người muốn không giao dịch và cho phép trade ở league thường nhưng tiến trình chính chạy theo trade. Bài học: trade tốt cho nguyên liệu và đồ hoàn thiện, **độc** cho "vé qua cổng tiến trình". Xem E3.

**Giả định sai #5: "Càng nhiều hệ thống càng sâu, người chơi sẽ tự học."**
- Lật ngược: nếu người chơi mới phải hiểu 12 hệ thống trong 10 phút đầu thì họ rời đi.
- Chân lý: *Amazing Cultivation Simulator* và *Tale of Immortal* rất phức tạp nhưng là sim menu có tooltip và cho **tạm dừng**; trong Minecraft, modpack kiểu *Vault Hunters* hay *RLCraft* có tỉ lệ bỏ cuộc cao ở tuần đầu vì áp lực sinh tồn + quest book dày. *Terraria* giải bằng NPC Guide và mở khóa theo boss. Cần **progressive disclosure** (mục 4.3).

**Giả định sai #6: "RNG cơ duyên là hấp dẫn."**
- Lật ngược: nếu 56 giờ phụ thuộc may rủi rơi đan thì người xui chịu gấp 4,5 lần người may (110 vs 500 giờ).
- Chân lý: người chơi *Diablo/PoE* chịu RNG khi có **bảo hiểm** (pity, crafting xác định, đồ rơi từ boss cụ thể). S3§4.5 đã có pity cho Tẩy Hồn nhưng **Phá Cảnh Đan chưa có pity**. Ô Phó Mặc giống "Kadala" của Diablo 3: vui vì thua vẫn có đồ; ở đây thua nặng nhất vẫn nhận Lõi và 1% Kẹt có đền +10 Điểm Vận, nên giữ được; nhưng nếu E6 không sửa, người chơi sẽ save-scum và mất niềm vui.

**Giả định sai #7: "Boss scale 0,75 HP/người là công bằng."**
- Lật ngược: nếu mỗi người thêm làm HP tăng ít hơn 1 người, nhóm đông luôn hiệu quả hơn solo trên đầu người (n=10: ×7,75 HP chia 10 ⇒ 0,775 HP/người).
- Chân lý: MMO/ARPG đều học bài này; đây là cố ý (khuyến khích chơi nhóm), nhưng cần **trần lợi ích đầu người** để người solo (singleplayer là chế độ chính của nhiều người) không bị phạt. Đề xuất khuyến khích nhóm bằng nội dung riêng (Bí Cảnh nhóm), không bằng giảm thời gian giết.

---

## 4. CẢI THIỆN

### 4.1 Làm dày cốt truyện (gắn cơ chế)

1. **Hồ Sơ Chấp Hành Giả**: mỗi Văn Kiện đã đọc lưu vào sổ; đủ 3/6 mở **lợi ích chiến đấu** (kháng đòn đặc trưng của CHG đó +5%/+10%, hiển thị chiêu trước 1 s). Đọc = có lợi, không phải bắt buộc đọc chữ. (Giải #2.)
2. **Ký ức NPC quen**: NPC lưu 3 sự kiện gần nhất với người chơi (mua gì, hỏi gì). Ảnh Tuyến nói **sai** một chi tiết (tên món đồ, lời hứa) ⇒ người chơi tự nhận ra. Cơ chế là một vòng nhỏ trong `SavedData`, không cần AI sinh text.
3. **Bia Công Trạng server**: người đầu tiên hạ mỗi Chấp Hành Giả, người chọn Phong Ấn/Hấp Thụ… được khắc tên ở Hoàng Đô (cộng DV). Multiplayer có lịch sử chung; singleplayer hiện bia rỗng "chờ bạn".
4. **Di Huấn theo đạo**: hạ CHG mô phỏng cùng đạo (Kiếm tu hạ Lăng Uyên…) kích hoạt "Giấc Mơ Bản Sao" dài hơn và tặng kỹ năng/bị động **Di Huấn** của bản thật. Thể↔Thạch Hống, Đạo↔Bạch Từ Âm, Kiếm↔Lăng Uyên, Ma↔Cơ Vô Tận (Huyết Ảnh chuộc danh, mở thoại và nhiệm vụ riêng cho Ma tu, giảm Ác Danh toàn cục sau khi hạ).
5. **Chu kỳ trăng**: Nguyệt Khuyết ấn chỉ hiện dưới trăng tròn (S1§3.1): gắn 8 pha trăng vanilla vào hoạt động Sứ Đồ (trăng khuyết ít nội gián, trăng tròn dễ bóc mặt nhưng Dạ Hộ Pháp xuất hiện). Người chơi học nhịp thay vì đọc chữ.
6. **Mộ Dao/Hạng Chiêu trong multiplayer**: dùng bản "Linh Ảnh" per-player (instanced) có thoại theo lựa chọn từng người; ghi lựa chọn vào Data Attachment. Tránh việc 2 người chơi tranh một đồng hành.
7. **Bí mật thế giới theo mùa Xâm Thực**: mỗi vùng Xâm Thực lùi có thể mở **di tích** (Hầm lạnh Bắc Hàn, bia Lạc Nhật) ⇒ lý do quay lại thành cũ sau khi đã vượt cấp.

### 4.2 Làm chặt hệ thống

1. **`KillRateGate` duy nhất** trong `gates` (E2): thay S2 CCL, S3 1.6, S4 3.5.
2. **Bind & trade policy** cho Phá Cảnh Đan/Linh Vật/Tẩy Tủy/Thọ Đan (E3, E4, E5).
3. **Bỏ chết vì tuổi** (C1): Thọ Tận ⇒ giữ Ngũ Suy GĐ V mãi; "Hồi Quang" làm đường cứu tự nguyện (đổi 1 giờ + Căn Cơ), không phải kết liễu.
4. **Một đồng hồ tuổi** (`PlayerClock.onlineActiveTicks`) dùng chung S5/S6/S7 + chặn AFK.
5. **Seed-before-roll + lưu trước UI** cho Ấn Hồn Đài/Phó Mặc (E6).
6. **Boss scaling theo người đóng góp** và hệ số chênh cấp (E7).
7. **Quota toàn cục cho dân/NPC** và gắn tag nhân giống (E1).
8. **Pity cho Phá Cảnh Đan**: không có đan sau N lần boss/Bí Cảnh đúng mốc thì đảm bảo rơi **Đan Phương**; NPC bán Nhất–Tam phẩm đã có, mở rộng Tứ–Lục phẩm bằng đấu giá cá nhân có giá trần.
9. **Hợp nhất Tẩy Hồn/tiền tệ/Thọ Đan** theo C3, C11, C12.
10. **Bộ mô phỏng cân bằng** (script Python đọc `exp_curve.json`, `gates.json`, `age.json`) xuất bảng giờ theo tư chất; CI so với bảng S3§1.5 ±10%. AI-coder làm được và chặn lệch số khi chỉnh datapack.
11. **Preset server** (Casual ×0,5 Chướng, Standard, Hardcore) ánh xạ vào cùng các tham số `exp_gain_mult` và `gates.json` (S3§1.4 đã dự trù 0,33).

### 4.3 Độ phức tạp và onboarding: mở khóa dần theo cảnh giới

Nguyên tắc: **giữ nguyên số liệu, chỉ trì hoãn việc lộ UI/nhắc nhở**; mọi hệ thống vẫn tồn tại ngầm từ đầu (tránh lệch canon).

| Cảnh giới | Lộ ra | Ẩn / mờ đến lúc |
|-----------|-------|-----------------|
| Thường Dân (1–9) | HP, EXP, 1 đạo, nhiệm vụ nhập môn, vài đan hồi phục; HUD tuổi đơn giản | Đan Độc, Tâm Ma/Ác Tính (Ma tu thì lộ Ác Tính sớm), Danh Vọng/Cống Hiến, chức vị |
| Nhất Giả (10–19) | **Ấn Hồn Đài, Lõi**, Hồn Tinh, Thọ Nguyên, lời mời tông | Hồn Cốt, Tẩy Hồn, luyện đan |
| Tông Sư (20–29) | Tông môn, bay Kiếm (cấp 20), đa đạo (nếu có đạo thứ hai: **kích hoạt đạo phụ** ở đây để người mới không bị ×2 tài nguyên ngay) | Thành chủ, Cống Hiến sâu |
| Đại Tông Sư (30–39) | Luyện đan (Đan Đạo), Hồn Cốt, Sứ Đồ/Văn Kiện, Danh Vọng thành | Raid cấp cao |
| Vương Giả (40–49) | Chức vị thành, tập kích tham chiến | Linh Vật cấp ≥6 |
| Hoàng Giả → Thánh Giả (50–79) | Chấp Hành Giả, Bí Cảnh cao, bay Đạo/Thể (60) | Tẩy Hồn |
| Đấu Giả trở lên (80+) | Tẩy Hồn, Mốc nhẹ, Khảo Hạch, kết thúc | — |

Quest nhập môn nên là một **chuỗi 7–10 bước** dạy từng hệ thống một (không popup). Mục tiêu: 30 phút đầu chỉ cần hiểu HP, EXP, đạo, 1 quái, 1 NPC.

### 4.4 Cân bằng

- **Tư chất**: bảng S3 (D 826 h … SSS 310 h) chênh 2,7×. Để tránh người xui chơi khổ, thêm **"Bù Tư Chất"**: D/C nhận Chướng ×0,5 ở 3 cổng đầu (mốc 10/20/30), D còn ≈640 h thay vì 826 h. Chi phí rất nhỏ trên mô hình.
- **Tam năng 1.239 h** không hợp lý với người thường: giữ trần ≈800 h bằng cách đặt phạt EXP đa đạo giảm khi vào Vạn Tượng (S6 đã làm 1,6→1,3, 2,5→1,8) và thêm ưu đãi Thư Tiến Cử.
- **Lõi/Phó Mặc**: Δ phẩm kỳ vọng +0,10…+0,17 bù mù Lõi, hợp lý; giữ.
- **Ma Hấp Thu (~100 h/455 h) rẻ hơn Kiếm**: chỉ hợp nếu E1 được chặn; còn không phải tăng Ác Tính.
- **Mốc 70/80/90 tốn 56 h/cổng**: thêm **đường trung gian**: phẩm thấp hơn 1 bậc −40% (S7 đã có) kèm pity đan phương.
- **Boss EXP trần 2,0 E(L)/trận** trong nhóm 10 người cho mỗi người 0,775×: giữ.

---

## 5. Góc nhìn đa chiều

| Vai | Mối lo chính | Kết luận |
|-----|--------------|----------|
| **Người chơi mới** | 12 hệ thống lộ cùng lúc; cấp 1–9 vanilla ≈16.000 con (C7); không hiểu "Bình Cảnh" ở cấp 9 vì tự nhiên phải đợi 12–32 giờ nếu thiếu đan Nhất–Tam | Mở khóa dần (4.3); sửa m=1,5; NPC bán đan Nhất–Tam gần như miễn phí; tooltip giờ ước tính ở Bình Cảnh |
| **Người chơi hardcore** | Đã có đường tối ưu (AFK farm, ống CH → đan, alt) nên mô hình 110 h bị phá; ngược lại Căn Cơ Tổn Thương chỉ nhẹ nên thiếu rủi ro | Chặn exploit (mục 2) rồi mới tăng độ khó; thêm `hukhongHardcore` có thưởng EXP bù |
| **Chủ server** | Phải cấu hình nhiều hệ (tuổi, thành, tông, raid, PvP Ma), khó giám sát alt/exploit; lag do tập kích/Hư Hóa (S5 R5) | Preset Casual/Standard/Hardcore; lệnh `/hukhong audit` (kill rate, giao dịch đan, cụm alt); log boss n; whitelist IP gia đình |
| **AI-coder (khả thi kỹ thuật)** | Khối lượng P0→P3 ≈ 210–320 vòng build-test (S5§6); API NeoForge 1.21.1 hay bị nhớ lẫn (S5 R1); số hệ thống phải đi qua `gates` (R10); tránh nhiều đồng hồ/nhiều luật trùng. Mâu thuẫn C1–C19 sẽ thành bug nếu giao cho agent khác nhau | Giải mâu thuẫn trước khi code; **một nguồn chân lý data** (datapack) cho số; thêm simulator; giữ P0 nhỏ như S5§6.1 (có AP2 đã tránh) |
| **Người viết truyện** | 20 mảnh lore + 8 hồi hay nhưng phân phối bằng chữ; hai cách chơi (Tuyến truyện/Tự do) có nguy cơ Tự do hụt cảm xúc; nhiều NPC có tên và bí mật nhưng thiếu cơ chế khiến họ đáng nhớ | Cốt truyện gắn cơ chế (4.1); giữ nhân vật trọng tâm (Mộ Dao, Hạng Chiêu, Cơ Vô Tận); cắt bớt tên NPC phụ khi cần |

**Tổng hợp**: thiết kế có ý tưởng mạnh (luật cứng mỗi đạo, phá cảnh nghi thức, Phế Tích + tái chiếm, Hư Không ngoại lai + Dạ Khuyết phản thần, Chấp Hành Giả là bản sao) và đã tự nhận nhiều rủi ro. Ba rủi ro quyết định là: **(1)** số liệu rời rạc giữa file (đặc biệt tuổi/EXP/Tẩy Hồn); **(2)** không có chặn "đi đường tắt" (AFK farm, chuyển đan, alt); **(3)** quá nhiều hệ thống lộ cùng lúc. Cả ba **rẻ để sửa ở giai đoạn tài liệu** và đắt ở giai đoạn code. Trước khi code P0 nên làm mục 7 P0.

---

## 6. CÂU HỎI CÒN MỞ (gộp từ 7 file, 12 câu)

| # | Câu hỏi | Nguồn | Khuyến nghị |
|---|---------|-------|-------------|
| Q1 | Hết thọ nguyên: chết (Hồi Quang) hay Ngũ Suy mãi mãi? Tuổi chỉ khi online? | S6 Q1, S5 Q2, S3 Q1 | **Không bao giờ chết vì tuổi**; chỉ online (onlineTicks có chặn AFK) |
| Q2 | Tư chất thấp/đa đạo không vật liệu (D 826 h, tam 1.239 h): chấp nhận hay "Bù Tư Chất"? | S3 Q1 | Thêm Bù Tư Chất (mục 4.4); mục tiêu D ≤ ~650 h |
| Q3 | Khảo Hạch là cá nhân hay khóa sau cốt truyện? Kết thúc Phong Ấn/Hấp Thụ chọn theo ai? Tầng II dimension mới hay biến đổi tại chỗ? Dùng Thần Đan hay Hỗn Độn Thanh Liên Tử? | S3 Q2, S4 Q2, S7 Q4 | Khảo Hạch **cá nhân** (tối đa 3 hộ pháp); kết thúc là lựa chọn **cá nhân** ghi vào Bia Công Trạng; Tầng II **dimension mới** (A); **Thần Đan** (bỏ Hỗn Độn, C16) |
| Q4 | Kho Văn Kiện chung hay riêng từng người? | S4 Q1, S5 Q1 | **Chung** cho lộ hang ổ; "kích hoạt sớm" yêu cầu party (E8) |
| Q5 | EXP vanilla cấp 1–9 | S4 Q3 | m phẳng 1,5 (C7) + nhiệm vụ nhập môn |
| Q6 | Tiền tệ | S7 Q1 | **Linh Thạch** duy nhất (C12) |
| Q7 | Luyện đan mở cho mọi người? Tẩy Tủy lên A có tự nhận Kỹ Năng Bản Mệnh? | S7 Q2, Q3 | Mở cho mọi người (Đan Đạo độc lập); lên A **được chọn 1 KNBM từ pool** 1 lần, không tự động |
| Q8 | PvP Ma và khiêu chiến Thành Chủ | S2 Q3, S6 Q2 | Mặc định **opt-in ("chiến")**; `city_duel` tắt |
| Q9 | Chấp nhận trần đa đạo (Phân Tâm); KNBM dùng chung ô Tối Thượng | S2 Q1, Q2 | Chấp nhận (A, A) |
| Q10 | Đệ Tử Tông Môn khởi đầu Ngoại Môn thật hay Thư Tiến Cử? Ấn Hồn Đài theo cấp thành? | S6 Q3, Q4 | **Thư Tiến Cử** (không sinh nhập môn tự động); **bỏ giới hạn cấp thành**, thay bằng trần phẩm Lõi theo cấp thành |
| Q11 | Cốt truyện: Dạ Khuyết boss ẩn; Mộ Dao hy sinh; tố cáo nội gián; Hấp Thụ để lại debuff Hư Hóa | S1 Q1–Q4 | A, A, A (có điểm uy tín tông); debuff Hư Hóa **chỉ cosmetic**, không giảm chỉ số |
| Q12 | Độ đắt Tẩy Hồn (20–30 h) | S3 Q3 | Hạ còn ≈10–12 giờ ở Hắc/Hồng, thống nhất với S7 (C11) |

---

## 7. Top 10 việc sửa ưu tiên (trước khi code)

| # | Ưu tiên | Việc | Ref |
|---|---------|------|-----|
| 1 | **P0** | Chốt một đồng hồ tuổi (S6) và bỏ chết vì tuổi; sửa S5§3.8, S7 | C1, C2, E5 |
| 2 | **P0** | Viết `KillRateGate` duy nhất vào S3/S5 `gates`; xóa 3 luật chống farm trùng | C9, E2 |
| 3 | **P0** | Sửa EXP vanilla cấp 1–9 (m phẳng 1,5) và bảng "con/cấp" ở S3/S4 | C7, C8 |
| 4 | **P0** | Chính sách bind/trade cho Phá Cảnh Đan, Linh Vật, Tẩy Tủy, Thọ Đan; giới hạn ống CH → đan | E3, E4 |
| 5 | **P0** | Đồng bộ mốc nhẹ S3↔S7 (thêm "Tiểu Phá Đan"); xóa Hỗn Độn khỏi đường Thăng Thần | C10, C16 |
| 6 | **P0** | Seed-before-roll và lưu trước UI cho Ấn Hồn Đài/Phá Chấp | E6 |
| 7 | **P1** | Thống nhất Tẩy Hồn, Thọ Nguyên Đan, tiền tệ, Căn Cơ Tổn Thương (S2/S3/S6/S7) | C3, C11, C12, C13 |
| 8 | **P1** | Boss: n theo người đóng góp, hệ số chênh cấp, loot cần đóng góp tối thiểu; Dẫn Dụ yêu cầu party | E7, E8 |
| 9 | **P1** | Quota toàn cục cho dân/NPC, gắn tag nhân giống, Ác Danh cho mọi Villager | E1 |
| 10 | **P1** | Onboarding mở khóa theo cảnh giới + quest nhập môn; sửa S1 (Hoàng Đô, Thanh Vân, Văn Kiện) + S2 (Đệ Tử Tông Môn) | 4.3, C4, C5, C6, C15 |

*Cách kiểm sau khi sửa*: chạy simulator (4.2 #10), grep tìm các số cũ (`670`, `4/10/22/45`, `Tu Tệ`, `getDayTime`), và đọc lại bảng C1–C19.

---

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | REVIEW-v1-tu-tien-mod-design.md (file này) | Rà soát canon v2 + S1…S7 v2 | 🔄 Current |
| — | PLAN-v2-tu-tien-mod-canon.md | Nguồn canon | ✅ Current |
| — | sections/S1…S7 | Các section v2 được rà soát | Xem từng file |
