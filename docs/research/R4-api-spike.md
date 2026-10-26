# R4 — API spike (NeoForge 21.1.265, MC 1.21.1)

Tất cả đã COMPILE được (xem `src/main/java/com/hukhong/spike/Spike.java`). Chưa chạy runtime / GameTest.

| Chủ đề | Class / chữ ký đúng (1.21.1) | Lưu ý |
|---|---|---|
| Attachment | `DeferredRegister<AttachmentType<?>>` với `NeoForgeRegistries.ATTACHMENT_TYPES`; `AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build()` (`net.neoforged.neoforge.attachment`) | `copyOnDeath()` chỉ copy khi clone do chết; cần `serialize` để lưu NBT. Dùng `entity.getData(TYPE)` / `setData`. |
| Payload | `record X implements CustomPacketPayload`; `CustomPacketPayload.Type<X>(ResourceLocation.fromNamespaceAndPath(..))`; `StreamCodec<RegistryFriendlyByteBuf,X>` qua `StreamCodec.composite(ByteBufCodecs.VAR_INT, ...)`; override `type()` | Đăng ký trong `RegisterPayloadHandlersEvent` (mod bus): `event.registrar("1").playToClient/playToServer/playBidirectional(TYPE, CODEC, (payload, IPayloadContext ctx) -> ...)`. Handler chạy main thread mặc định; `ctx.enqueueWork`. |
| Sát thương | `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent` (game bus) | `getAmount()/setAmount()`, `getSource()`, cancelable. Sau đó còn `LivingDamageEvent.Pre/Post` (giáp/absorption). |
| Spawn | `net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent` (game bus) | `getSpawnType()` trả `net.minecraft.world.entity.MobSpawnType`; `getEntity()` là `Mob`; cancel bằng `setSpawnCancelled(true)`. |
| MobSpawnType.SPAWNER | `MobSpawnType.SPAWNER` (enum, tồn tại ở 1.21.1) | Cũng có `SPAWN_EGG`, `NATURAL`, `COMMAND`, `TRIAL_SPAWNER`... (1.21.2+ đổi tên thành `EntitySpawnReason`). |
| Datapack registry | `DataPackRegistryEvent.NewRegistry` (mod bus): `event.dataPackRegistry(ResourceKey<Registry<T>>, Codec<T> codec, Codec<T> networkCodec)` | Key tạo bằng `ResourceKey.createRegistryKey(rl)`. JSON nằm ở `data/<ns>/<path>/*.json`. |
| ResourceLocation | `ResourceLocation.fromNamespaceAndPath(ns, path)` | Constructor `new ResourceLocation(..)` đã private từ 1.21. |

GeckoLib: `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.3`, repo `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`; khai báo required dependency `geckolib` trong mods.toml.
GameTest: chưa viết (cần structure template); gameTestServer sẽ crash nếu không có test nào.
