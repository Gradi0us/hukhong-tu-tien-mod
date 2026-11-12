# PLAN-v1: Khảo hạch Dạ Khuyết · Cutscene đột phá Lv10 · Đan dược nhanh · Thế chiến đấu · Pháp khí hộ thể · Nghề phụ

**Created**: 2026-11-12
**Last Updated**: 2026-11-12 — Người chơi đã chốt 4 câu hỏi (xem cuối file); C gộp vào D
**Status**: 🔄 Đã chốt, đang làm
**Phân công**: Haiku code từng hạng mục · Opus lập kế hoạch + review lại sau mỗi hạng mục

---

## Key Findings (hiện trạng, đã đọc code)

| # | Hiện có | Bằng chứng | Ảnh hưởng tới kế hoạch |
|---|---------|-----------|------------------------|
| 1 | Dạ Khuyết máu **6.000**, NTTD **510.000** | `endgame/DaKhuyet.java:61`, `endgame/EndgameKit.java:38` | Cần tăng ~70× để "chỉ thua NTTD một chút" |
| 2 | Dạ Khuyết hiện chỉ xuất hiện giữa trận NTTD (đồng minh) và trong phiên Siêu Độ | `NguyenThuyTichDiet.java:973`, `SieuDo.java:87` | Trận khảo hạch là luồng **mới**, phải chèn trước NTTD |
| 3 | Đã có **Đan Lô** + 9+ đan phương | `alchemy/DanLoBlock*.java`, `AlchemyRecipes.java` | Luyện đan **không làm lại**, chỉ gắn thêm nghề + phím dùng nhanh |
| 4 | Đã có "Thế Tu Luyện" (phím G): khoá phím số, tự cầm vũ khí bản mệnh | `skill/StanceHands.java` | "Trạng thái chiến đấu" = mở rộng thế này, không làm hệ mới |
| 5 | Đã có 4 Pháp Khí **Hộ** dạng phù tiêu hao (Hộ Thân, Kim Cang, Độn Không, Hồi Xuân) | `phapkhi/PhapKhiKind.java` | Pháp khí hộ thể mặc trên người là loại **mới** (đồ bền, không tiêu hao) |
| 6 | Mộ Dao là NPC cốt truyện có sẵn | `story/flow/FlowSteps.java:43`, `CityArcs.java:478` | Dùng lại model/tên cho cutscene Lv10 |
| 7 | Pháp tu có hệ **Kim** | `phaptu/PhapElement.java:10` | Thưởng hiệu quả cho Đoán Tạo Sư |
| 8 | Hook lên cấp | `core/CultivationService.java:129` | Điểm gắn cutscene đột phá Lv10 |

---

## Hạng mục A — Dạ Khuyết: trận khảo hạch trước NTTD

| Việc | Chi tiết |
|------|---------|
| A1 Chỉ số | Lv **99**. Máu ≈ **90% NTTD** (≈ 460.000, nhân theo số người chơi như NTTD). Sát thương/giáp nâng tương ứng |
| A2 Khi nào gặp | Lần đầu người chơi mở/bước vào cổng Endgame → Dạ Khuyết chặn đường, cutscene mở màn "khảo hạch". Chưa thắng thì **không vào được** trận NTTD |
| A3 Trận đấu | Dùng toàn bộ bộ chiêu + kiểu di chuyển đã làm (3 pha, Huyết Hoa...) |
| A4 Thua / thắng | Thua: hồi sinh, đánh lại được. Thắng: Dạ Khuyết **không chết** — quỳ xuống, cutscene **cải tà quy chính** (lời thoại, ánh trăng máu chuyển sang trăng bạc, cánh đổi màu) → cờ cốt truyện `dk_redeemed` |
| A5 Nối truyện | Có cờ `dk_redeemed` → trong trận NTTD nàng xuất hiện làm **đồng minh** (logic đồng minh đã có); thiếu cờ thì giữ như cũ |
| A6 Lệnh test | `/hukhong dakhuyet trial` để bắt đầu thẳng trận khảo hạch |

## Hạng mục B — Cutscene đột phá cấp 10

1. Ngay sau khi lên cấp 10: màn hình tối, **bóng Nguyên Thủy Tịch Diệt** (dạng 1, mờ) hiện ra, giơ búa đánh người chơi.
2. **Mộ Dao** lao ra chắn, đỡ đòn — 2 bên đánh qua lại 2 nhát (VFX va chạm, rung camera).
3. Đối thoại ngắn (NTTD đe doạ, Mộ Dao cảnh báo người chơi), NTTD tan biến, Mộ Dao dặn dò rồi biến mất.
4. Người chơi **bất tử + khoá di chuyển** trong cutscene; chỉ chạy **1 lần** (cờ `lv10_cutscene_seen`); bấm phím bỏ qua cutscene sẵn có.
5. Lời thoại vi/en (Haiku viết).

## Hạng mục C — Đan dược dùng nhanh

| Việc | Chi tiết |
|------|---------|
| C1 Phím | Phím mới **"Dùng đan"** (mặc định **R**) |
| C2 Bấm | Uống ngay viên đan **đang chọn** (lấy từ túi đồ, có hồi chiêu theo loại) |
| C3 Giữ | Giữ ≥ 0,3s → mở **vòng chọn** (radial) các loại đan đang có trong túi; thả chuột trên loại nào thì chọn loại đó |
| C4 HUD | Ô nhỏ cạnh thanh kỹ năng: icon đan đang chọn + số lượng + vòng hồi chiêu |
| C5 Luyện | Giữ Đan Lô hiện có; công thức có thêm **yêu cầu cấp Luyện Đan Sư** (xem E) |

## Hạng mục D — Thế chiến đấu (mở rộng Thế Tu Luyện, phím G)

- Khi vào thế: thanh chọn chỉ còn **2 nhóm**: ô 1 = **vũ khí bản mệnh** (nếu đạo có), các ô sau = **các loại đan dược đang có** (mỗi loại 1 ô, tự gom).
- Đồ khác bị ẩn tạm (không mất), ra khỏi thế thì trả lại nguyên vị trí.
- Phím số chọn đan; chuột phải ô đan = uống.

## Hạng mục E — Nghề phụ (chức nghiệp phụ)

| Nghề | Làm gì | Ưu thế |
|------|--------|--------|
| **Luyện Đan Sư** | Luyện đan ở Đan Lô; cấp cao mở đan phương cao, tăng phẩm/số viên | Đạo **Đan** mặc định là Luyện Đan Sư |
| **Phù Văn Sư** | Vẽ phù (các Pháp Khí phù Công/Hộ/Trận đã có) tại bàn vẽ phù mới | — |
| **Đoán Tạo Sư** | Rèn **pháp khí hộ thể** (hạng mục F), về sau **cơ giáp** | Pháp tu hệ **Kim** rèn nhanh hơn, tỉ lệ phẩm cao hơn |

- Mỗi người chỉ **1 nghề**, chọn 1 lần (NPC hướng dẫn trong thành), không học thêm nghề khác.
- Cấp nghề tăng bằng **kinh nghiệm chế tạo**, **trần cấp nghề = theo cảnh giới tu luyện**.
- Người không có nghề vẫn **dùng** được đan/phù/pháp khí, chỉ không **chế** được.

## Hạng mục F — 4 ô giáp → 4 pháp khí hộ thể

| Ô | Pháp khí | Tác dụng gợi ý |
|---|----------|---------------|
| Mũ | **Tịnh Tâm Quan** | Kháng choáng/khống chế, giảm thời gian hiệu ứng xấu |
| Áo | **Hộ Tâm Giáp** | Giảm sát thương, máu tối đa |
| Quần | **Kim Cang Hộ Thối** | Giáp, kháng đẩy lùi |
| Giày | **Phi Vân Ngoa** | Tốc chạy, giảm thể lực khi lách né |

- 5 phẩm như Pháp Khí hiện có; do **Đoán Tạo Sư** rèn; có thể rơi từ boss.
- **Cơ giáp** (bộ đặc biệt cho Đoán Tạo Sư cấp cao) để **đợt sau**.

---

## Thứ tự làm & phân công

| Bước | Hạng mục | Người làm | Review |
|------|----------|-----------|--------|
| 1 | A — Dạ Khuyết khảo hạch | Haiku | Opus |
| 2 | B — Cutscene Lv10 | Haiku | Opus |
| 3 | C — Đan dùng nhanh | Haiku | Opus |
| 4 | D — Thế chiến đấu | Haiku | Opus |
| 5 | E — Nghề phụ (khung + Luyện Đan Sư) | Haiku | Opus |
| 6 | F — Pháp khí hộ thể + Đoán Tạo Sư, Phù Văn Sư | Haiku | Opus |

Mỗi bước: worktree riêng, `./gradlew build -q` phải qua, **không viết test mới** (theo yêu cầu), không mở game. Opus đọc diff, kiểm tra tính năng có được gọi trong game thật (không code treo), rồi mới gộp.

## Quyết định đã chốt (2026-11-12)

1. **Nghề phụ**: mọi người (kể cả Đạo Đan) **tự chọn 1 trong 3**, chọn 1 lần, không học thêm. → bỏ dòng "Đạo Đan mặc định Luyện Đan Sư" ở E.
2. **Ô giáp**: **chỉ nhận pháp khí hộ thể**; giáp vanilla không mặc được.
3. **Khảo hạch Dạ Khuyết**: lần đầu vào cõi Endgame; chưa thắng thì không đánh được NTTD.
4. **Không có phím dùng đan riêng.** Thay vào đó (gộp C vào D): vào **Thế G** → mọi đồ khác biến khỏi thanh chọn, chỉ còn **vũ khí bản mệnh** (nếu có); **đan dược và phù** đang có tự xếp vào các ô **1 → 8** còn lại. Bấm phím số chọn, chuột phải dùng. HUD hồi chiêu hiện trên từng ô.

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-story-pills-professions.md (this file) | ⬜ Chờ duyệt |
