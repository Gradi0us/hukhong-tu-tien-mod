# R2 — Tech research: NeoForge (as of 2026-10-26)

## 1. Trạng thái NeoForge

- **1.21.1 vs 26.1**: NeoForge công bố 26.1 là bản ổn định kế tiếp, thay thế 1.21.1 (ước đoán, không phải thông báo chính thức đầy đủ). Build 26.1 cũ còn gắn nhãn beta; changelog Modrinth tháng 7/2026 ghi build 26.1.2.82 là bản "stable" nhưng nguồn bên thứ ba. [CHƯA XÁC MINH] trạng thái stable chính thức.
- **Java/Gradle**: 26.1 dùng Java 25; cần Gradle ≥ 9.1.0 (theo snapshot post). [CHƯA XÁC MINH] số chính xác trên bản release.
- **Template**: ModDevGradle 2.0.142 (tháng 7/2026); MDK 1.21.1 ModDevGradle có sẵn (mirror cập nhật 2026-07-05). Dùng MDK 1.21.1 nếu chọn 1.21.1.
- **Breaking changes 26.1**: chưa xác minh được danh sách đầy đủ (fetch trang release thất bại). Cần đọc release notes trực tiếp.
- **Chọn phiên bản**: 1.21.1 có hệ sinh thái mod lớn nhất (Iron's, Curios, GeckoLib, Epic Fight đều hỗ trợ). 26.1 ít addon hơn.

## 2. API chính (1.21.1)

- **Data Attachments** (`AttachmentType`, `DeferredRegister` attachment types): lưu/đồng bộ dữ liệu player/entity/chunk/level. Serializer là tùy chọn; không có serializer thì không persist. Dùng cho mana, linh căn, cảnh giới. Đồng bộ client qua `copyOnDeath` + payload riêng. [CHƯA XÁC MINH] chi tiết tên method trên 1.21.1.
- **SavedData**: dữ liệu toàn level (ví dụ bảng sect/đạo quán toàn server).
- **Custom attributes**: đăng ký `Attribute` qua `DeferredRegister` và gắn vào `RegisterEvent`/`EntityAttributeModificationEvent` cho player (tu vi, linh khí làm stat). [CHƯA XÁC MINH] tên event chính xác.
- **Network payload**: record implement `CustomPacketPayload` với `type()` trả về `CustomPacketPayload.Type`; codec bằng `StreamCodec.composite`; đăng ký qua `RegisterPayloadHandlersEvent` / `PayloadRegistrar`. Trang docs tìm được là 1.21.9–1.21.10 (dùng `Identifier`); 1.21.1 dùng `ResourceLocation`.
- **Custom dimension & worldgen**: dimension qua datapack (`dimension`, `dimension_type`); structure bằng jigsaw pool (`structure_set`, `template_pool`) + `structure`; đăng ký qua datapack, không cần code. [CHƯA XÁC MINH] chi tiết cho 1.21.1.
- **Custom HUD layer**: `RegisterGuiLayersEvent` (`GuiLayerManager`/`LayeredDraw`). [CHƯA XÁC MINH] tên class trên 1.21.1.
- **Keybind**: `KeyMapping` + `RegisterKeyMappingsEvent`; xử lý bằng `ClientTickEvent.Post`.
- **Custom RenderLayer (cánh)**: `RenderLayer` subclass, đăng ký qua `EntityRenderersEvent.AddLayers`; với player dùng `PlayerRenderer`. [CHƯA XÁC MINH] tương thích wings với model mới.

## 3. Mod tham chiếu (1.21.1)

- **Iron's Spells 'n Spellbooks**: dòng 1.21.1-3.16.x (3.16.1 là bản thấy trong dependency). Mana/spell casting là mẫu tốt; addon yêu cầu ≥ 3.15/3.16. [CHƯA XÁC MINH] version mới nhất trên CurseForge.
- **Curios API**, **Epic Fight**, **GeckoLib**: chưa tìm được version 1.21.1 → [CHƯA XÁC MINH]. Kiểm tra trên Modrinth.
- **Cultivation mod**:
  - Immortal Cultivation (修仙) 1.0.0 NeoForge 1.21.1: chỉ thấy trên Mado Hosting (trang hosting, ngày 11/06/2026); không thấy trang CurseForge/Modrinth chính. 9Minecraft ghi ngày khác (10/08/2026) → [CHƯA XÁC MINH].
  - Immortality: The Path of Cultivation: Fabric 1.21.11 (không phải NeoForge).
  - Journey to Immortality (Fabric) và Wuxia Block: chưa tìm được thông tin → [CHƯA XÁC MINH].

## 4. Gotchas

- **Không mất hunger**: exhaustion đi qua `Player.causeFoodExhaustion(float)` → `FoodData.addExhaustion(float)` (HungerDial dùng điểm này). Chặn ở đó hoặc giữ `FoodData` cố định khi có buff. Cẩn thận: hướng này chưa xác minh là bao phủ cả flight. [CHƯA XÁC MINH]
- **Miễn sát thương melee theo vũ khí cầm**: hook `LivingIncomingDamageEvent` (cancelable, có `setAmount`), kiểm tra `DamageSource` là melee + `getMainHandItem()` của attacker. Tên event từ kiến thức nền, không xác minh được trong tìm kiếm → [CHƯA XÁC MINH].
- **Bay kiểu creative với trừ tài nguyên**: dùng `Player.getAbilities().mayfly` + `flying` đồng bộ qua `ClientboundPlayerAbilitiesPacket` (server tự sync); trừ mana mỗi tick khi `isFlying()`, dừng và tắt flight khi cạn. Cần kiểm tra client-server desync. [CHƯA XÁC MINH]

## Sources

- https://neoforged.net/news/26.1release/
- https://neoforged.net/news/26.1snapshots/
- https://neoforged.net/news/moddevgradle2/
- https://codeberg.org/QuantumLS-Studio/MDK-1.21.1-ModDevGradle
- https://modrinth.com/mod/common-capabilities/version/XdGZPPrp
- https://docs.neoforged.net/docs/datastorage/attachments
- https://docs.neoforged.net/docs/networking/payload
- https://docs.neoforged.net/docs/1.21.10/networking/payload
- https://neoforged.net/news/20.5release
- https://www.curseforge.com/minecraft/mc-mods/irons-spellbooks-tweaks/files/8075546
- https://www.curseforge.com/minecraft/mc-mods/irons-magic-trees-bridge/files/8371315
- https://www.mado-hosting.com/minecraft-mods/immortal-cultivation-mod
- https://www.9minecraft.net/?p=964532
- https://www.curseforge.com/minecraft/mc-mods/immortality-the-path-of-cultivation/files/8525306
- https://moddex.gg/mod/hungerdial
- https://www.curseforge.com/minecraft/mc-mods/hungerdial
