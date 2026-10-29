# PLAN-v2 — Tương thích shader (Iris) + Resource pack "Hư Không"

**Ngày tạo**: 2026-10-29
**Cập nhật**: 2026-10-29 — Bản đầu. Làm ở session sau
**Trạng thái**: ⬜ Chưa làm
**Shader khuyến nghị**: Complementary Unbound (thay thế: Reimagined / BSL cho máy yếu, Bliss cho cảnh đẹp)
**Liên quan**: PLAN-v1-vfx-boss-mob-scaling.md (VFX), commit 6cfe49d (VFX mới)

---

## 0. Tóm tắt

| # | Việc | Vì sao | Ưu tiên |
|---|---|---|---|
| A | Phát hiện Iris đang bật shader → đổi sang cách vẽ tương thích | Iris thay pipeline vẽ: `PostChain` của mod (`AmbientPost`, `void_grade`) thường không chạy hoặc vẽ sai; FX cộng màu ở `AFTER_PARTICLES` có thể sai sương mù/độ sáng | P0 |
| B | Trời tím / vùng xâm thực **không dùng post shader** khi có Iris → dùng sương mù + màu trời qua sự kiện fog/sky | Giữ cảm giác "Hư Không xâm thực" khi người chơi bật shader | P0 |
| C | Kiểm thử ma trận: không shader / Complementary Unbound / BSL × 6 cảnh | Chỉ thấy lỗi khi nhìn thật | P0 |
| D | Resource pack "Hư Không": texture riêng của mod + giao diện cổ phong | Đồng bộ thẩm mỹ, đẹp hơn | P1 |
| E | Hướng dẫn cài (README trong dist) + preset cài đặt Complementary | Người chơi cài đúng ngay | P2 |

---

## 1. Hiện trạng (đã rà mã)

| Thành phần | Vị trí | Rủi ro dưới Iris |
|---|---|---|
| Chỉnh màu hậu kỳ (giảm bão hòa, ám tím, vignette) | `client/ambient/AmbientPost.java` + `shaders/post/void_grade.json`, `shaders/program/void_grade.fsh` | **Cao** — PostChain vanilla thường bị bỏ qua/vẽ đè sai khi shaderpack bật |
| Post shader vùng zone / ám màu kỹ năng (`SCREEN_TINT`) | `client/fx` + ambient | Cao — cùng lý do |
| FX kỹ năng (ribbon, decal, cự kiếm, quad, hạt) | `client/fx/*` vẽ ở `RenderLevelStageEvent` (5 chỗ `AFTER_PARTICLES`, 1 `AFTER_ENTITIES`, 1 `AFTER_LEVEL`, 1 `AFTER_SKY`) | Trung bình — có thể bị sương mù shader phủ, mất blend cộng, hoặc vẽ sai độ sâu |
| ObjectiveBeam (cột sáng nhiệm vụ), minimap | `client/fx/ObjectiveBeam.java` | Trung bình (beam), thấp (HUD) |
| Model GeckoLib (quái, boss) | `geo/entity/*` | Thấp — GeckoLib tương thích Iris tốt; cần kiểm tra emissive |
| Texture mod | block 35, entity 72, gui 86, item 122, particle 9 | — (phần D) |

---

## 2. Việc A–B: tương thích Iris

### A1. Phát hiện shader
- Thêm `client/compat/ShaderCompat.java`:
  - `isIrisLoaded()` = `ModList.get().isLoaded("iris")`
  - `shadersActive()` = gọi `net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse()` **qua reflection** (không thêm phụ thuộc biên dịch), cache mỗi 20 tick, mọi lỗi → `false`.
- Không thêm Iris vào build.gradle (giữ mod chạy khi không có Iris). Nếu muốn chạy thử trong dev: thêm `localRuntime` Iris + Sodium NeoForge từ Modrinth maven (chỉ runtime, không bắt buộc).

### A2. Hậu kỳ khi có shader
- `AmbientPost`: nếu `shadersActive()` → **không** chạy PostChain.
- Thay bằng (B):
  - **Sương mù**: `ViewportEvent.ComputeFogColor` (màu tím theo trọng số zone) + `ViewportEvent.RenderFog` (kéo gần `nearPlane/farPlane`). Iris/Complementary tôn trọng màu fog vanilla ở mức nhất định — cần kiểm C.
  - **Màu trời**: dùng `DimensionSpecialEffects` hoặc hook `getSkyColor`/`getCloudColor` qua event/mixin nhẹ để ám tím ở vùng xâm thực.
  - **Vignette/ám màu chiêu**: vẽ quad 2D ở HUD (`RenderGuiLayerEvent`) với alpha thấp thay cho post — HUD luôn hiển thị dưới Iris.
- Không có shader → giữ nguyên PostChain hiện tại (đã đẹp).

### A3. FX kỹ năng dưới shader
- Kiểm tra trong cảnh thật (C). Các chỉnh dự kiến:
  - Nếu FX bị sương mù phủ: vẽ với `RenderSystem.setShaderFogStart(Float.MAX_VALUE)` trong lúc vẽ FX rồi khôi phục.
  - Nếu blend cộng bị mất/quá chói: giảm alpha 20–30% khi `shadersActive()`; tùy chọn ClientConfig `fxShaderBrightness` (0.5–1.2).
  - Nếu sai độ sâu (FX xuyên tường / bị che hết): chuyển các lượt vẽ chính sang `AFTER_TRANSLUCENT_BLOCKS` hoặc `AFTER_WEATHER` (thử từng stage, chọn stage ổn với cả có/không shader).
  - Hạt `FxParticle` (ParticleRenderType) thường ổn dưới Iris — chỉ kiểm tra.
- Không đổi logic FX, chỉ đổi cách vẽ.

### A4. Emissive (phát sáng) cho shader
- Complementary đọc **emissive qua `_e` texture / block ánh sáng / entity glowing**. Cho boss và khối Hư Không phát sáng đẹp dưới shader:
  - Khối Hư Không: đặt `lightEmission` hợp lý (3–7) cho khối xâm thực, lõi, cổng.
  - GeckoLib: dùng `AutoGlowingGeoLayer` + texture `*_glowmask.png` cho mắt/lõi boss (Hư Vương, Sứ Đồ, Thần Dạ Khuyết, Nguyên Thủy Tịch Diệt).

---

## 3. Việc C: kiểm thử (ma trận)

| Cảnh | Không shader | Complementary Unbound | BSL |
|---|---|---|---|
| 1. Vùng xâm thực ban ngày (trời tím, mảnh vụn bay) | | | |
| 2. Ban đêm + Hư Không đột kích | | | |
| 3. Kiếm: Trảm Khí, Liên Hoàn, Kiếm Vực, Thiên Kiếm Giáng | | | |
| 4. Đạo/Ma/Thể ult + Thiên Phạt, Thôn Thiên Ma Công | | | |
| 5. Boss Hư Vương (aura, phase, nhạc) | | | |
| 6. Thành + cột sáng nhiệm vụ + Bí Cảnh | | | |

Mỗi ô ghi: thấy đúng / sai thế nào + ảnh chụp vào `docs/shader-review/`. Đo FPS từng ô (F3).
**Lưu ý:** kiểm thử cần người chơi nhìn thật — agent chỉ chạy được build + log. Session sau: agent làm A, người chơi chụp C, agent sửa theo ảnh.

**Hoàn thành khi:** cả 18 ô không lỗi nghiêm trọng (FX mất hẳn, màn hình đen, trời tím mất hẳn, crash).

---

## 4. Việc D: Resource pack "Hư Không"

### D1. Định hướng thẩm mỹ
- **Phong cách**: cổ phong tiên hiệp, nét pixel 16×16 (giữ đúng tỉ lệ Minecraft, không lên 32× để hợp gói vanilla và shader).
- **Bảng màu chung**:
  - Hư Không: tím đậm `#2A0F3D` → tím sáng `#9B5CFF`, viền đen.
  - Tiên khí: vàng kim `#E8C060`, ngọc `#5FD3C0`.
  - Giao diện: giấy cổ `#E9DDBF`, mực `#2B2118`, viền vàng đồng `#B08A3E`.
- Theo bảng màu đạo ở PLAN-v1 §2.1 cho icon kỹ năng.

### D2. Phạm vi (chỉ phần của mod)

| Nhóm | Số file | Việc |
|---|---|---|
| Khối Hư Không / xâm thực / cổng / bàn thờ (`textures/block`) | 35 | Vẽ lại có vân xoáy, khối động (`.mcmeta` animation) cho khối xâm thực và cổng; texture glowmask cho shader |
| Vật phẩm: đan dược, nguyên liệu, kiếm, bùa, sách (`textures/item`) | 122 | Đồng bộ phong cách: đan dược tròn có hào quang theo phẩm (Phàm xám, Huyền lam, Địa tím, Thiên vàng) |
| Giao diện (`textures/gui`) | 86 | Khung cổ phong: nền giấy cổ, viền vàng, nút dạng ngọc bội; icon kỹ năng 35+ theo màu đạo; thanh máu/mana/Kiếp Số |
| Thực thể (`textures/entity`) | 72 | Quái Hư: thêm vân tím phát sáng + glowmask; boss chi tiết hơn |
| Hạt (`textures/particle`) | 9 | Hạt Linh Khí, Ma Khí, Bá Khí, Thánh Quang mềm hơn, có gradient |

### D3. Cách làm (thực tế với agent)
- Agent **không vẽ tay được** — dùng script Python (Pillow) sinh texture theo quy tắc (gradient, noise, viền, hào quang) như đã làm icon kỹ năng (`tools/gen_book_skill_icons.py`).
  - `tools/gen_rp_blocks.py`, `gen_rp_items.py`, `gen_rp_gui.py`, `gen_rp_glow.py`.
- Chất lượng mong đợi: đồng bộ, sạch, nhưng không bằng họa sĩ. Texture quan trọng (boss, logo, khung giao diện chính) → người chơi có thể vẽ lại trên Aseprite/Blockbench sau, giữ tên file.
- Đóng gói hai cách:
  1. **Nhúng vào mod** (texture mặc định mới thay luôn) — đơn giản nhất. **Đề xuất.**
  2. Resource pack riêng `dist/HuKhong-RP.zip` (`pack.mcmeta` `pack_format` 34 cho 1.21.1) — nếu muốn giữ texture cũ làm mặc định.
- **Không** làm lại texture vanilla (đất, đá, cây…) — dùng gói sẵn có nếu muốn.

### D4. Gói vanilla đi kèm (tùy chọn, người chơi tự tải)
- Hợp phong cách: Faithful 32× (gọn, an toàn với shader) hoặc gói kiểu "Fresh Animations" cho động vật vanilla (cần Entity Model Features/Texture Features bản NeoForge — kiểm tra có bản 1.21.1).

---

## 5. Việc E: hướng dẫn + preset
- `dist/README-cai-dat.txt`:
  - Mod bắt buộc: hukhong, GeckoLib, playerAnimator.
  - Tùy chọn: Better Combat + Cloth Config, Iris + Sodium.
  - Shader khuyến nghị và cài đặt.
- Preset Complementary Unbound (ghi tay trong README, người chơi chỉnh trong menu):
  - Mây khối: Bật.
  - Tia sáng xuyên: Vừa.
  - Phản chiếu nước: Bật.
  - Bão hòa: +10%.
  - Bloom: Thấp–Vừa, để FX mod không quá chói.

---

## 6. Lộ trình session sau

| Bước | Nội dung | Agent | Ghi chú |
|---|---|---|---|
| 1 | A1–A4 + B | Sonnet | build + GameTest + runClient (có Iris localRuntime) không crash |
| 2 | Người chơi chụp ma trận C | Người chơi | Gửi ảnh |
| 3 | Sửa theo ảnh | Sonnet | |
| 4 | D: sinh texture block + item + particle | Haiku/Sonnet (script) | Chạy song song bước 1 |
| 5 | D: giao diện cổ phong + glowmask boss | Sonnet | Sau bước 4 |
| 6 | E: README + commit + jar vào dist | Haiku | |

## 7. Rủi ro
| Rủi ro | Giảm thiểu |
|---|---|
| API Iris đổi tên lớp | Reflection + try/catch, fallback coi như "không shader" |
| Shader pack khác nhau xử lý fog khác nhau | Kiểm ít nhất 2 gói (Complementary, BSL) |
| Texture sinh bằng script trông "nhựa" | Chỉ thay nhóm nào đẹp hơn rõ ràng; giữ bản cũ để so trước khi thay |
| Sodium xung đột với render tùy chỉnh | Thử runClient có Sodium; FX dùng RenderLevelStageEvent thường ổn |

---

## Document Lineage
| Version | Document | Status |
|---|---|---|
| v1 | PLAN-v1-vfx-boss-mob-scaling.md | 🔄 Đang làm (VFX xong phần nền + kỹ năng sách) |
| v2 | PLAN-v2-shader-resourcepack.md (file này) | ⬜ Current, chưa làm |
