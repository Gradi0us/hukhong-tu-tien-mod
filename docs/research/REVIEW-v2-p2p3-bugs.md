# REVIEW-v2 P2/P3 bugs (Bước 23)
Scope: honcot, story(+npc), chg, bicanh, trial, endgame, entity, client/{chg,endgame,story,renderer}.

## Verified OK (evidence: code + resources)
- Geo/animation/texture for all 13 GeckoLib entities exist and ids match DefaultedEntityGeoModel; every animation name used in code exists in the animation JSON.
- EntityAttributeCreationEvent covers HuThi/Cot/Chu/Vuong, CHG 1-4, boss, DaKhuyet, SuDo x3, StoryNpc. Spawn eggs + models exist.
- Dimension / dimension_type JSON valid for 1.21.1. Dialogue lang keys (3 files) all present in vi_vn + en_us; dynamic key families present.
- Bossbars (CHG, HuVuong, SuDo, Boss) removed in remove()/stopSeenByPlayer. StoryData/QuestData persisted with copyOnDeath.
- No client-only (net.minecraft.client) classes referenced from common code; only payload handlers reach com.hukhong.client.* via lambdas/method refs (not executed on dedicated server).

## Fixed
1. BiCanh: bossbar kept a stale ServerPlayer after logout -> added PlayerLoggedOut handler (BiCanhManager.dropBar). Stranded-in-dimension on relog/restart was already handled (onLogin -> leave).
2. Endgame: relog during cutscene left Resistance V + Slowness VII (hidden) on player, and ending-complete players could stay in hu_khong_ban_nguyen -> onLogin now strips those effects and sends them to Overworld.
3. Trial: Tâm Ma clone (persistent Husk) survived server stop -> orphan; EntityJoinLevelEvent now cancels clones loaded from disk.

## Notes / not changed (risk accepted)
- BiCanh onDamage rescue fires on pre-armor damage (rescues slightly early; safe side).
- Trial: arena-distance teleport ignores dimension change mid-trial (minor). Retry timers are RAM-only (restart re-begins via unlockMilestone).
- Endgame login when boss absent: not auto-evicted (chunk may be unloaded at login -> false positives).
- HuHoa.color() reads ClientCache (plain class, safe) but yields client-default level on server; cosmetic.
- Nothing validated in-game; unit/mocks only (AP4).
