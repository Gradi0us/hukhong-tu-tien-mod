# PLAN-v1 — Chỉ số, VFX, quái & boss kiểu Elden Ring

**Ngày tạo**: 2026-10-29
**Cập nhật**: 2026-10-29 — Bản đầu, sau khi rà mã nguồn
**Trạng thái**: 🔄 Đã chốt 29/10. Chốt: chỉ số trước. playerAnimator thành bắt buộc. **Không lồng tiếng, chỉ phụ đề điện ảnh** (§6 và B3 bỏ phần âm thanh). Q2, Q4, Q6 theo đề xuất.
**Phạm vi**: cân bằng chỉ số 5 đạo, VFX kỹ năng người chơi, quái mới cùng kỹ năng của chúng, boss nhiều phase, cutscene, lồng tiếng

---

## 0. Tóm tắt

| # | Vấn đề tìm thấy khi rà mã | Bằng chứng | Mức |
|---|---|---|---|
| 1 | **19 kỹ năng học từ sách không có VFX riêng**. Chúng chỉ dùng hạt vanilla. | `skillbook/BookSkillEffects.java`: 0 lời gọi `Fx.*`, 14 `sendParticles` | CAO |
| 2 | Chỉ số lệch: kỹ năng 1 của Đạo gây ~2,7× sát thương/giây so với Kiếm ở cấp 60 (chưa tính mana và chí mạng, cần mô phỏng để xác nhận). Kỹ năng 1 của Thể chỉ ~25/giây. | Tính từ `StatService.table()` + `Skill.java` (§1.2) | CAO |
| 3 | Chú thích và số thật lệch nhau: chú thích ghi "Trảm Khí 120%, Liên Hoàn 3×70%", nhưng mã dùng `KIEM_1_COEF = 0.50` và `KIEM_2_COEF = 0.50`. | `skill/Skill.java:23-25` | TRUNG |
| 4 | Chỉ số người chơi tăng tuyến tính (~40× từ cấp 1 đến 100), nhưng HP quái tăng theo hàm bậc ba (×50 ở cấp 60, ×251 ở cấp 100) và **nhảy gấp đôi đột ngột ở cấp 100**. | `HuHoa.hpScale()`, `THAN_HP_MUL = 2.0` | CAO |
| 5 | Boss chuyển phase chỉ hiện chữ "Phase 2", chạy 1 animation và đổi danh sách chiêu. Không có cutscene, camera, lồng tiếng hay biến đổi đấu trường. | `WorldBoss.enterPhase()`, `HuVuong.enterPhase()` | CAO |
| 6 | Quái thường chỉ có 8 loại, phần lớn mang 1–2 chiêu, VFX là hạt vanilla. | `entity/HuSkill.java` (0 `Fx.*`) | TRUNG |
| 7 | Âm thanh kỹ năng toàn là âm vanilla (tiếng vung kiếm, golem, rồng). | `SkillEffects.java` (`SoundEvents.*`) | TRUNG |

**Thứ tự đề xuất**: chỉ số → nền VFX → VFX kỹ năng → khung boss và cutscene → làm lại từng boss → quái mới → lồng tiếng.

Làm chỉ số trước vì VFX và chiêu boss đều dựa vào "vai trò" của mỗi đạo. Nếu vai trò còn lệch thì làm hiệu ứng trước sẽ phải sửa lại.

---

## 1. Giai đoạn S — Rà và cân lại chỉ số (QUAN TRỌNG NHẤT)

### 1.1 Mục tiêu (định nghĩa "cân")

Chỉ số không cần bằng nhau. Mỗi đạo có một vai trò, và vai trò đó phải đo được:

| Đạo | Vai trò | Sát thương/giây duy trì (so với Kiếm = 100) | Độ trâu (thời gian sống trước quái cùng cấp) |
|---|---|---|---|
| Kiếm | Sát thương đơn mục tiêu, cơ động | 100 | 1,0× |
| Ma | Đánh liên tục, hút máu, rủi ro cao | 95–105 | 1,1× (nhờ hút máu) |
| Thể | Chống chịu, khống chế, bùng nổ theo nhịp | 75–85 | 1,8× |
| Đạo | Hỗ trợ, hồi máu, sát thương ánh sáng | 65–75 | 1,0× (+ hồi cho cả đội) |
| Pháp | Diện rộng theo nguyên tố | 90 (đơn) / 140 (đám đông) | 0,9× |

Số lần đánh để hạ quái thường cùng cấp phải nằm trong khoảng **4–8 đòn kỹ năng** ở mọi cấp 1–100. Đây là dải TTK.

Boss thế giới cùng cấp, một người chơi: **90–180 giây**.

### 1.2 Số liệu hiện tại (tính từ mã, chưa gồm Lõi, trang bị, chí mạng)

```
Cấp   Kiếm atk  Đạo sp  Thể atk  Ma atk  Pháp sp   HP quái (×base)
L1        4        8       4        4       7         ×2.5
L30      50       66      49       38      62         ×22
L60      98      126      95       74     119         ×50
L100    162      206     157      122     195         ×251  ← nhảy ×2 ở cấp Thần

Sát thương/giây kỹ năng 1 ở L60 (bỏ qua mana):
  Kiếm 82   Đạo 223   Ma 116   Thể 25
```

→ **Giả thuyết cần xác nhận bằng mô phỏng**: (a) Đạo quá mạnh khi đủ mana; (b) Thể quá yếu khi đánh bằng kỹ năng; (c) người chơi tụt so với quái sau cấp 60 nếu không có Lõi hay trang bị.

### 1.3 Việc cần làm

| # | Việc | Ghi chú |
|---|---|---|
| S1 | Viết `tools/balance_sim.py` mô phỏng trận 60 giây cho 5 đạo × cấp {1,10,30,60,80,100} × tư chất {B, S}. Có tính mana, hồi mana, hồi chiêu, nội tại, chí mạng, Lõi theo chất lượng trung bình, kỹ năng sách đã học. | Đọc hằng số trực tiếp từ mã Java bằng regex, để mô phỏng không lệch với code |
| S2 | Xuất bảng HTML/CSV gồm sát thương/giây, TTK quái thường, TTK boss, thời gian sống. Tô đỏ ô nằm ngoài dải ở §1.1. | |
| S3 | Chỉnh hệ số: `Skill.*_COEF`, `BookSkill` mult, `StatService.table()`, `HuHoa.hpScale()`. Bỏ cú nhảy ×2 ở cấp 100 (đổi thành tăng mượt từ cấp 90–100). | Mỗi lần chỉnh phải chạy lại mô phỏng |
| S4 | Chuẩn hóa kỹ năng sách: mỗi phẩm (Phàm, Huyền, Địa, Thiên) có ngân sách sát thương/mana cố định. Kỹ năng dùng Công và kỹ năng dùng Phép quy đổi về cùng thang đo. | Hiện Thiên phẩm có mult 4,5–5,5 trên hai thang khác nhau |
| S5 | Sửa chú thích sai trong `Skill.java`, để số trong chú thích bằng số thật | |
| S6 | Thêm GameTest `BalanceBandTest`: chạy mô phỏng rút gọn trong Java và fail nếu một đạo rơi ra ngoài dải | Chống lệch lại về sau |
| S7 | Đa đạo (Phân Tâm ×0,8 / ×0,7): kiểm tra song tu và tam tu không mạnh hơn đơn tu ở sát thương duy trì | |

**Hoàn thành khi**: bảng mô phỏng không còn ô đỏ, GameTest dải pass, và người chơi thử 3 đạo ở cấp 20–30 thấy "ổn".

---

## 2. Giai đoạn F — Nâng cấp nền VFX (bộ công cụ dùng chung)

Hiện có 20 loại `FxType`, đa số vẽ bằng hạt và quad đơn giản. Mọi hiệu ứng phía sau dựng trên bộ công cụ này, nên phải làm trước.

| # | Thành phần mới | Dùng cho |
|---|---|---|
| F1 | **Dải vệt (ribbon trail)**: vệt sáng bám theo vũ khí hoặc đạn, mờ dần, có màu chuyển | Kiếm khí, đường lướt, đòn boss |
| F2 | **Decal mặt đất**: vòng pháp trận, vết nứt, vũng máu, cảnh báo chiêu boss (vùng đỏ lan dần tới lúc nổ) | Telegraph, AoE |
| F3 | **Mesh kiếm hoặc vũ khí khổng lồ** từ model item, phóng to, có phát sáng viền | Thiên Kiếm Giáng, boss |
| F4 | **Méo không gian (distortion)** bằng post shader quanh điểm nổ hoặc cổng | Nổ lớn, Hư Không |
| F5 | **Bloom hoặc phát sáng cộng màu** cho hạt chính | Toàn bộ |
| F6 | **Phản hồi đòn đánh**: khựng ngắn 2–4 tick khi đòn nặng trúng, rung camera theo độ mạnh, chớp trắng mục tiêu, số sát thương to hơn khi chí mạng | Cảm giác đánh "có lực" |
| F7 | **Âm thanh nhiều lớp**: mỗi kỹ năng có 3 lớp (đầu, thân, đuôi), file .ogg riêng, có biến thể pitch | Thay âm vanilla |
| F8 | **Ngân sách hiệu năng**: giới hạn số hạt và vệt mỗi khung hình, giảm theo khoảng cách. Thêm tùy chọn "Chất lượng VFX: Thấp, Vừa, Cao, Cực" trong ClientConfig | Người chơi cho phép "đẩy cao nhất" nhưng tránh lòe loẹt (§2.1) |

### 2.1 Nguyên tắc thẩm mỹ (theo yêu cầu: "đẩy cao nhưng đừng lòe loẹt")

- Mỗi đạo một **bảng màu cố định**:
  - Kiếm: trắng bạc và xanh lam nhạt
  - Đạo: vàng kim và trắng
  - Thể: cam đất và vàng đồng
  - Ma: đỏ thẫm và đen
  - Pháp: màu theo nguyên tố
- **Nhịp 3 nhịp**: tụ lực (nhỏ, sáng dần) → bùng (to, nhanh, 3–6 tick) → dư âm (tan chậm). Không giữ độ sáng tối đa lâu.
- Hiệu ứng của người chơi khác giảm 50% độ đậm, để giao tranh nhiều người vẫn nhìn rõ.
- Chiêu boss luôn **dễ đọc hơn đẹp**. Telegraph rõ ràng, chiêu càng mạnh thì cảnh báo càng dài.

### 2.2 Quyết định kỹ thuật cần chốt (§9-Q2)

Có hai hướng:
- **Tự làm tiếp** trên `client/fx` hiện có. Không thêm thư viện, nhưng tốn công hơn cho ribbon, bloom và distortion.
- **Dùng thư viện VFX** (ví dụ Lodestone hoặc Photon). Có sẵn trail và bloom, nhưng thêm phụ thuộc bắt buộc và rủi ro lệch phiên bản NeoForge 1.21.1. Cần kiểm tra bản tương thích trước khi chọn.

**Đề xuất**: tự làm. Code đã có `FxClient`, `FxDraw` và post shader. Ribbon và decal không khó, và không phải lo người chơi thiếu mod.

---

## 3. Giai đoạn K — VFX kỹ năng người chơi (rà từng kỹ năng)

### 3.1 Bảng rà hiện trạng

| Nhóm | Số kỹ năng | Hiện trạng VFX | Việc cần làm |
|---|---|---|---|
| Kỹ năng gốc (Kiếm, Đạo, Thể, Ma × 3) | 12 | Có `Fx.*` (2–5 lớp mỗi chiêu) | Nâng cấp bằng F1–F7. Thêm tụ lực, phản hồi trúng, âm riêng |
| Pháp tu (6 nguyên tố × 4 phím) | 24 biến thể | Có (4–10 lời gọi `Fx.*` mỗi nguyên tố) | Rà Quang: chỉ 4 FX, ít nhất. Thêm âm riêng |
| Bản Mệnh (4 chiêu + Cộng Hưởng) | 5 | Có (2–4) | Huyết Hải mới có 2 lớp, cần thêm |
| **Kỹ năng sách** | **19** | **Không có `Fx.*`, chỉ hạt vanilla** | **Làm VFX mới cho toàn bộ, ưu tiên cao nhất** |
| Nội tại có kích hoạt (Lôi giật, tháp Thổ, hồi máu khi giết, Bá Khí...) | ~10 | Chưa rà | Mỗi lần kích hoạt cần dấu hiệu nhìn thấy được, nhỏ và rõ |
| Đột phá, Lôi Kiếp, cường hóa | 3 | Có `BREAKTHROUGH` và `UPGRADE_BURST` | Gắn với cutscene ngắn (§5) |

### 3.2 Ý tưởng VFX cho 19 kỹ năng sách

| Kỹ năng | Đạo | Ý tưởng hình ảnh |
|---|---|---|
| Phá Không Trảm | Kiếm | Một nhát chém ngang để lại vết rách không gian màu đen viền trắng, khép lại sau 0,5s |
| Phi Kiếm Truy Hồn | Kiếm | 3–5 phi kiếm có vệt sáng bay vòng cung, đuổi theo mục tiêu, đâm xuyên và bật ngược |
| Kiếm Vũ Hoa Lạc | Kiếm | Cánh hoa và kiếm nhỏ rơi xoáy trong vùng tròn, mỗi lần trúng nở một bông hoa sáng |
| Thiên Kiếm Giáng Thế | Kiếm | Mây tách đôi, một cự kiếm (mesh F3) giáng xuống. Màn hình khựng lại, sóng xung kích và vết nứt hình chữ thập |
| Thanh Tâm Chú | Đạo | Bùa vàng bay ra và dán lên đồng đội, tỏa vòng sáng thanh tẩy |
| Thiên Lôi Chú | Đạo | Bùa ném ra, mây tụ trên đầu mục tiêu, sét giáng (đánh dấu trước 0,4s) |
| Quang Trận | Đạo | Pháp trận vàng nhiều vòng xoay ngược chiều trên mặt đất, cột sáng nhỏ nhô lên mỗi nhịp |
| Thiên Phạt | Đạo | Bầu trời trong vùng tối lại, mắt trận khổng lồ mở ra, chùm sáng giáng xuống, chớp trắng màn hình |
| Long Hổ Quyền | Thể | Bóng rồng và hổ mờ ảo theo hai nắm đấm |
| Thiết Sơn Kháo | Thể | Lướt vai có vệt bụi, va chạm làm khựng màn hình và tạo sóng đá |
| Bá Thể Chấn | Thể | Dậm đất, mặt đất nứt hình tròn, đá bay lên rồi rơi xuống |
| Phá Thiên Quyền | Thể | Tụ lực quanh nắm đấm, có vầng hào quang. Đấm lên trời, cột khí xé mây, sóng chấn lan ra |
| Huyết Tế Chú | Ma | Phù văn máu dưới chân mục tiêu, máu bị rút thành dòng về người chơi |
| Ma Ảnh | Ma | Để lại phân thân bóng đen, đường lướt màu đỏ đen |
| Huyết Tế Đài | Ma | Đài máu trồi lên từ đất, xích máu bắn ra trói kẻ địch |
| Thôn Thiên Ma Công | Ma | Hố đen đỏ hút mọi thứ, cái bóng ma thần khổng lồ há miệng phía sau |
| Lăng Ba Vi Bộ | Chung | Gợn sóng nước dưới chân, bóng mờ của các bước trước |
| Hộ Thân Cương Khí | Chung | Lớp vỏ lục giác trong suốt, nứt vỡ khi hấp thụ đòn |
| Hồi Xuân Thuật | Chung | Dây leo và lá xanh nở quanh người, hạt sáng bay lên |

### 3.3 Hoạt ảnh nhân vật khi tung chiêu

Hiện tại người chơi không có tư thế niệm chú. Better Combat và playerAnimator đã có, nhưng chỉ là phụ thuộc tùy chọn.

- Có playerAnimator: mỗi kỹ năng có một animation ngắn (ví dụ bắt ấn, giơ tay, đấm, xoay người).
- Không có playerAnimator: dùng hiệu ứng thay thế (vòng sáng quanh tay, cánh tay vung theo vanilla).
- Cần quyết định có biến playerAnimator thành bắt buộc hay không (§9-Q3).

**Hoàn thành khi**: cả 35+ kỹ năng có VFX (≥3 lớp tụ lực, bùng, dư âm) và âm riêng, đúng bảng màu, có ảnh chụp kiểm tra từng chiêu trong `docs/vfx-review/`.

---

## 4. Giai đoạn M — Quái mới, kỹ năng và model

### 4.1 Hiện trạng

- **Quái thường**: Hư Thi, Hư Cốt, Hư Chu, Hư Ảnh, Hư Giáp, Hư Trùng, Hư Triệu Sư, Huyết Ma (bản tinh anh).
- **Boss**: Hư Vương (3 phase), Băng Hư Cự Nhân, Độc Mẫu, Sa Hạt Vương (2 phase), Tâm Ma, 3 Sứ Đồ, 4 Chấp Hành Giả, Thần Dạ Khuyết, Nguyên Thủy Tịch Diệt.

### 4.2 Khung quái theo vai trò

Mỗi vùng có đủ 6 vai trò, để mỗi trận đánh buộc người chơi đổi cách chơi:

| Vai trò | Hành vi | Ví dụ mới |
|---|---|---|
| Xung phong | Lao vào, đánh combo 2–3 nhát | Hư Lang (sói Hư Không, đi theo bầy 3–5 con) |
| Đỡ đòn | Khiên trước mặt, phải đánh vòng ra sau hoặc phá khiên | Hư Giáp (thêm khiên) |
| Tầm xa | Giữ khoảng cách, bắn đạn có quỹ đạo | Hư Cung Thủ (cung xương bắn tên rơi theo hình vòng cung) |
| Phép thuật | Đặt vùng nguy hiểm trên đất, có cảnh báo | Hư Chú Đồng (pháp sư nhỏ, tạo vùng tím rồi nổ) |
| Ám sát | Tàng hình, đánh lén từ sau lưng | Hư Ảnh (thêm tàng hình) |
| Triệu hồi hoặc tăng sức | Gọi thêm quái, hoặc buff đồng bọn | Hư Triệu Sư, Hư Kỳ Thủ (cầm cờ tăng sức cho bầy) |

**Quái theo khu vực** (mỗi biome 2–3 loại đặc trưng):
- Băng nguyên: Băng Hồn
- Sa mạc: Sa Trùng chui dưới cát
- Đầm lầy: Độc Thiềm
- Rừng: Mộc Yêu
- Biển: Hải Quỷ, Thủy Xà — đi cùng boss biển còn thiếu
- Vùng xâm thực: biến thể bị xâm thực của quái thường

**Tiền tố tinh anh** (kiểu Diablo, ngẫu nhiên khi sinh ra): Cuồng Bạo, Hư Không, Băng Giá, Đa Phân (chết tách thành 2), Phản Chấn, Hút Hồn. Mỗi tiền tố có hào quang và màu tên riêng.

### 4.3 Kỹ năng quái — khung dữ liệu

Hiện kỹ năng quái viết cứng trong `HuSkill.java`. Đề xuất chuyển sang khung chung `MobMove`:

```
MobMove {
  id, tầm, hồi chiêu, trọng số chọn,
  windup  (tick tụ lực + animation + telegraph),
  active  (khung gây sát thương, hình dạng: nón / tròn / đường / đạn),
  recovery(tick hở sau chiêu — cửa sổ để người chơi phản công),
  vfx/sfx theo từng giai đoạn
}
```

Lợi ích: quái và boss dùng chung khung, telegraph tự động đồng bộ, dễ thêm chiêu mới.

### 4.4 Model

Model GeckoLib hiện được sinh bằng script (đơn giản, khối vuông). Có ba hướng (§9-Q4):
1. **Agent sinh model bằng script** (`tools/gen_geo.py`): nhanh, đồng bộ, nhưng tạo hình đơn giản.
2. **Người chơi hoặc người quen dựng trên Blockbench**: đẹp nhất, nhưng chậm.
3. **Kết hợp**: script dựng khung xương và animation cơ bản, Blockbench tinh chỉnh cho boss.

**Đề xuất**: hướng 3. Quái thường dùng script. Boss lớn đáng đầu tư Blockbench, có thể lấy cảm hứng phong cách (không sao chép) từ Abyss Herald hay NightFallCraft.

---

## 5. Giai đoạn B — Khung boss: phase, cutscene, camera

### 5.1 Học từ Elden Ring (phân tích, không sao chép)

| Boss tham khảo | Chuyển phase làm gì | Bài học |
|---|---|---|
| Godrick | Chặt tay rồng ghép vào người, phun lửa | Phase 2 **thay đổi cơ thể**, có chiêu mới hẳn |
| Rennala | Sân khấu thay đổi, chuyển sang trận đấu phép | **Đấu trường biến đổi** |
| Radahn | Bay lên trời rồi rơi như thiên thạch, gọi thêm đồng minh | Mở màn phase bằng **một chiêu "điện ảnh"** |
| Malenia | Hoa thối nở. Chiêu Waterfowl Dance | Có **chiêu đặc trưng** buộc người chơi phải học cách né |
| Maliketh | Rút kiếm thật ra, đổi nhịp đánh hoàn toàn | **Đổi nhịp** (chậm, nặng → nhanh, liên hoàn) |
| Godfrey / Hoarah Loux | Cởi bỏ danh xưng, đánh tay không tàn bạo | Phase 2 **lộ bản chất thật**, gắn cốt truyện |

**Nguyên tắc rút ra**:
1. Chuyển phase là **một khoảnh khắc**: boss bất tử 3–6 giây, có cutscene ngắn, câu thoại lồng tiếng, nhạc đổi lớp, đấu trường hoặc ngoại hình đổi.
2. Phase mới có ít nhất **2 chiêu mới** và **1 chiêu đặc trưng**, không chỉ triệu hồi thêm quái.
3. Có chiêu **đánh trễ nhịp** (delayed attack), combo nhiều nhát, chiêu tóm, và chiêu diện rộng phải chạy hoặc né đúng lúc.
4. Mọi chiêu đều có cửa sổ hở để phản công.

### 5.2 Hệ thống cần xây

| # | Thành phần | Mô tả |
|---|---|---|
| B1 | `BossPhaseDirector` | Máy trạng thái: Đánh → Chuyển phase (bất tử, khóa AI) → Đánh. Cấu hình theo boss: ngưỡng HP, kịch bản cutscene, bộ chiêu, thay đổi đấu trường |
| B2 | **Camera cutscene** (client) | Đường camera theo keyframe (vị trí, góc, FOV), nội suy mượt. Thêm thanh đen điện ảnh trên dưới và làm chậm hiển thị. Bấm phím để bỏ qua. Nhiều người chơi: ai trong bán kính 60 block thì xem, mỗi người tự bỏ qua được |
| B3 | **Phụ đề + lồng tiếng** | Mỗi câu thoại là 1 `SoundEvent` (.ogg) kèm phụ đề có tên người nói. Không có file âm thì chỉ hiện phụ đề |
| B4 | **Nhạc nhiều lớp** | Mỗi boss có nhạc phase 1 và phase 2 (hoặc lớp thêm), chuyển nhạc khớp với cutscene |
| B5 | **Biến đổi đấu trường** | Đổi khối theo kịch bản (sàn vỡ, dung nham, cột mọc lên, trời đổi màu). Khôi phục sau trận |
| B6 | **Thanh máu boss** | Tên, danh xưng, vạch chia phase, thanh "choáng/gãy thế" nếu có |
| B7 | **Thanh gãy thế (stance)** | Đánh đủ nặng làm boss quỵ, mở đòn chí mạng. Thưởng cho lối chơi Thể và Kiếm |

### 5.3 Mẫu thiết kế một boss (áp dụng cho mọi boss)

```
Tên + danh xưng | Cốt truyện (3–5 câu, gắn với mạch chính)
Đấu trường      | mô tả + biến đổi khi qua phase
Phase 1         | 4–5 chiêu (tên, telegraph, cửa sổ hở)
Chuyển phase    | kịch bản cutscene từng giây + câu thoại
Phase 2         | 2–3 chiêu mới + 1 chiêu đặc trưng + thay đổi nhịp
(Phase 3)       | chỉ cho boss cuối hoặc boss chương
Thoại           | lúc vào trận, lúc chuyển phase, lúc chết, lúc người chơi chết
Thưởng          | vật phẩm, kỹ năng sách, mảnh cốt truyện
```

### 5.4 Phác thảo từng boss (cần đối chiếu cốt truyện chính trước khi viết chi tiết)

| Boss | Ý tưởng chuyển phase |
|---|---|
| **Hư Vương** | Phase 1: kiếm khổng lồ đánh chậm, nặng. Chuyển phase 2: bóp nát vương miện, giáp vỡ để lộ thân Hư Không trong suốt, trời chuyển tím. Đánh nhanh, dịch chuyển chớp nhoáng. Chuyển phase 3: kéo cả đấu trường vào khoảng không, sàn vỡ thành đảo nổi. Chiêu đặc trưng "Hư Không Nuốt Trời" (hố đen hút, phải chạy ra rìa) |
| **Băng Hư Cự Nhân** | Phase 2: tự đóng băng rồi phá băng, vũ khí thành rìu băng. Đấu trường đóng băng trơn trượt, gai băng mọc theo nhịp |
| **Độc Mẫu** | Phase 2: trứng quanh đấu trường nở thành bầy con, sương độc dâng lên, phải đứng trên bệ cao. Chiêu đặc trưng "Mưa Độc" |
| **Sa Hạt Vương** | Phase 2: chui xuống cát và cả đấu trường thành cát lún, trồi lên bất ngờ có cảnh báo bằng gợn cát. Đuôi chích có độc trễ nhịp |
| **Tâm Ma** | Biến thành hình bóng của chính người chơi, dùng chiêu của đạo người chơi. Phase 2 phân thành 3 bóng, chỉ 1 bóng thật |
| **3 Sứ Đồ** | Mỗi người một nhịp. Dạ Kiếm đánh liên hoàn tốc độ. Hư Chú Sư khống chế bằng trận pháp. Huyết Tế Tư hiến tế quái để hồi máu. Khi một Sứ Đồ chết, hai người còn lại hấp thụ sức mạnh nếu gặp lại |
| **4 Chấp Hành Giả** | Đối thủ tầm trung, có lời thoại đối đáp với người chơi. Phase 2 lộ "chấp niệm" riêng của từng người |
| **Thần Dạ Khuyết** | Boss áp chót, 3 phase. Mỗi phase là một mặt trăng khuyết dần. Phase 3: trăng máu, trận đánh dưới mặt trăng |
| **Nguyên Thủy Tịch Diệt** | Boss cuối, 3 phase + cảnh kết. Phase 3 kéo người chơi vào Tâm Thức. Có câu thoại phản ứng theo lựa chọn trong các chuỗi nhiệm vụ |
| **Boss vùng xâm thực** (agent thế giới đang làm) | Khi giải phóng vùng: cutscene trời tím tan, ánh sáng trở lại |
| **Boss biển** (chưa có) | Hải Long Hư Hóa. Phase 2 nhấn chìm một nửa đấu trường |

---

## 6. Giai đoạn V — Lồng tiếng

| Hướng | Ưu điểm | Nhược điểm |
|---|---|---|
| A. Người chơi hoặc bạn bè tự thu | Giọng thật, có cảm xúc, miễn phí | Mất thời gian, chất lượng micro |
| B. TTS AI tiếng Việt có giấy phép thương mại hoặc dùng cá nhân | Nhanh, làm được hàng trăm câu | Ít cảm xúc. Phải kiểm tra giấy phép từng dịch vụ |
| C. Kết hợp | TTS làm bản tạm, thay dần bằng giọng thật cho boss chính | Hai đợt công việc |

**Đề xuất**: hướng C. Hệ thống B3 làm sao cho thay file .ogg là xong, không cần sửa mã.

**Khối lượng ước tính**: ~12 boss × ~6 câu + NPC cốt truyện ~80 câu ≈ **150 câu**, ~10 phút âm thanh, ~8–12 MB .ogg.

Kịch bản thoại mỗi boss sẽ nằm trong `docs/voice/SCRIPT-<boss>.md`. Mỗi câu ghi rõ người nói, cảm xúc và độ dài.

---

## 7. Lộ trình và phân công

| Đợt | Nội dung | Agent | Phụ thuộc |
|---|---|---|---|
| 1 | S1–S7: mô phỏng và cân chỉ số | Sonnet (khó) | — |
| 2 | F1–F8: nền VFX + tùy chọn chất lượng | Sonnet | — (chạy song song với đợt 1) |
| 3 | K: VFX 19 kỹ năng sách + nâng 12 kỹ năng gốc + nội tại | 2× Sonnet chia theo đạo | Đợt 2 |
| 4 | B1–B7: khung boss, cutscene, phụ đề, nhạc lớp, gãy thế | Sonnet | Đợt 2 |
| 5 | Làm lại Hư Vương làm boss mẫu (đủ 3 phase + cutscene + thoại TTS tạm) | Sonnet | Đợt 4 |
| — | **Người chơi thử boss mẫu và góp ý** | — | Đợt 5 |
| 6 | 3 boss thế giới + Tâm Ma theo mẫu | 2× Sonnet | Đợt 5 + góp ý |
| 7 | Sứ Đồ, Chấp Hành Giả, Thần Dạ Khuyết, boss cuối | 2× Sonnet | Đợt 6 |
| 8 | `MobMove` + 8–10 quái mới + tiền tố tinh anh | Sonnet (khung) + Haiku (dữ liệu, lang) | Đợt 4 |
| 9 | Lồng tiếng thật thay TTS | Người chơi + Haiku (gắn file) | Đợt 5+ |
| 10 | Boss biển + vùng biển | Sonnet | Đợt 8 |

Mỗi đợt kết thúc bằng: build, GameTest, chạy thử client chụp ảnh, commit + push. Sau đó dừng chờ góp ý.

---

## 8. Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| VFX nặng gây giật trên máy yếu | F8: ngân sách hạt + 4 mức chất lượng |
| Cutscene gây lỗi trong nhiều người chơi (người thì xem, người thì đang đánh) | Boss bất tử và đứng yên trong lúc chuyển phase. Cutscene chỉ là camera phía client, không khóa người chơi phía server |
| Cân chỉ số làm hỏng cảm giác hiện tại | Mô phỏng trước, chỉnh theo bước nhỏ, người chơi thử ở 3 mốc cấp |
| Model script trông thô | Hướng 3 (§4.4). Boss lớn dựng Blockbench |
| Giấy phép giọng TTS và nhạc | Ghi nguồn vào `docs/research/CREDITS-*.md` như nhạc hiện tại |

---

## 9. Câu hỏi cần người chơi chốt

- **Q1 — Thứ tự**: làm chỉ số trước rồi mới VFX (đề xuất), hay làm VFX trước?
- **Q2 — Thư viện VFX**: tự làm (đề xuất), hay thêm thư viện ngoài?
- **Q3 — playerAnimator**: thành bắt buộc để có tư thế tung chiêu, hay giữ tùy chọn?
- **Q4 — Model boss**: script + Blockbench (đề xuất), hay chỉ script?
- **Q5 — Lồng tiếng**: TTS tạm rồi thay giọng thật (đề xuất)? Ai thu giọng thật?
- **Q6 — Boss mẫu**: Hư Vương (đề xuất), hay boss khác?

---

## Document Lineage

| Version | Document | Status |
|---|---|---|
| v1 | docs/PLAN-v1-vfx-boss-mob-scaling.md | ✅ Current |

Liên quan: `docs/DESIGN-v2-quest-chains.md` (cốt truyện chuỗi nhiệm vụ, cần khớp với thoại boss).
