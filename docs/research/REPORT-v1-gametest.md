# REPORT-v1: GameTest runtime evidence

**Last Updated**: 2026-10-27 - 43 GameTests (2 cu + 41 moi) pass qua `./gradlew runGameTestServer`.
**File**: `src/main/java/com/hukhong/gametest/HuKhongRuntimeGameTests.java` (+ `HuKhongGameTests.java` cu). Run config `gameTestServer` da co san trong build.gradle.

## Coverage (41 test moi)
| Nhom | Test | Bang chung |
|------|------|-----------|
| Entity (15) | HuThi/HuCot/HuChu/HuVuong/TamMa/SuDo x3/CHG 1-4/NguyenThuyTichDiet/DaKhuyet/StoryNpc | spawn + finalizeSpawn, co MAX_HEALTH + MOVEMENT_SPEED, HP > 0, `kill()` -> chet, khong exception |
| Skill (16) | Q/E/R/T x Kiem/Dao/The/Ma bang FakePlayer cap 60 | tru tai nguyen (tru MA_1 cost 0); gay sat thuong len muc tieu truoc mat cho moi skill tan cong (MA R sau 30 tick qua SkillScheduler); E Dao/The/Ma va T Dao/The/Ma la buff nen chi kiem tra tru tai nguyen |
| Bay (5) | toggle bat/tat tung dao; cam gan `hukhong_world_boss` | mayfly set/clear (The khong dung mayfly), FlightBans chan ca 4 dao |
| Dan Lo (1) | khai_mach: bat dau -> giu nhiet bang button -> serverTick 400 tick | ra KHAI_MACH_DAN o slot OUT |
| Dan Doc (1) | an Chi Huyet Dan -> Dan Doc tang; `restoreFrom` (chet) | gia tri Dan Doc giu nguyen |
| Bi Canh (1) | enter/leave tra vi tri | xem han che 1 |
| World tier (1) | tier 1 -> 2, `HuHoa.applyStats` | HP ~x1.5, ATK ~x1.3 |
| Loi Kiep (1) | `TrialService.test(LOI_KIEP)` voi FakePlayer cap 90 | session chay het wave, ket thuc >= 60 tick, khong exception |

## Bug production tim thay
Khong co. Moi hanh vi kiem tra deu dung; khong sua file production nao.

## Phat hien ve harness (khong phai bug production)
1. **Bi Canh dimension khong duoc nap**: GameTestServer chi co 3 dimension vanilla, `BiCanhManager.enter` tra `hukhong.bicanh.err.nodim`. Test xac nhan do va kiem tra duong tra vi tri bang `evacuate()` (reflection) voi the RET giong `enter()` ghi. Vao/ra dimension that van can kiem tra bang client/server that.
2. **`FakePlayerNetHandler.teleport` la no-op**: test thay bang `ServerGamePacketListenerImpl` co `moveTo` de quan sat vi tri.
3. **Test song song anh huong nhau**: `EndgameService.onBossDefeated` discard moi HuMob trong 120 block, nen test giet boss cuoi (NguyenThuyTichDiet/DaKhuyet) dat batch `bossdie` rieng; test cam bay dung boss the gioi dat batch `flightban`. Day la hanh vi thiet ke, khong doi.
4. **FakePlayer bat tu (`isInvulnerableTo`)**: khong quan sat duoc sat thuong set Loi Kiep len nguoi choi; thay bang kiem tra session chay du thoi gian roi ket thuc. FakePlayer duoc dua tam vao `PlayerList.playersByUUID` (reflection) de `TrialService.onTick` tra cuu duoc, roi go ra.

## Gioi han con lai (AP4)
Khong co client that; khong test Tam Ma voi nguoi that; Dan Lo chi 1 phuong; Bi Canh khong vao dimension that.
