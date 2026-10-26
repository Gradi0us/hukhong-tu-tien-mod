# REVIEW-v2 — P1 systems bug review (Bước 16)

Reviewed: pathrules, banmenh, skill, flight, alchemy, age, city, society, events, gates, network. Not tested in-game.

## Fixed
| # | Bug | Fix |
|---|-----|-----|
| 1 | Đan Độc attachment lacked copyOnDeath: dying wiped toxicity (pill-toxicity bypass) | AlchemyRegistry.DAN_DOC `.copyOnDeath()` |
| 2 | Giải Độc Đan blocked at toxicity 100 (canUse) -> cannot cure burst | UtilityPillItem: canUse only for HEAL |
| 3 | Đại Bỉ trial mobs (non-spawner, EVENT) granted cultivation EXP via GateEvents | RewardGate.canReward excludes SectTrial mobs |
| 4 | FlightService STATE/GRACE static, not cleared on server stop (SP re-enter -> phantom flight state) | ServerStoppedEvent clear |
| 5 | CorruptionManager.bossCooldown stale across worlds | clear on ServerStoppedEvent |
| 6 | BanMenh HIT_COUNT/FREEZE_CD/SUMMON_CD leaked per player | clear on logout |
| 7 | Skills (Chấn Địa, Thánh Quang Tiễn, Kiếm/Ma skills) hit villagers/NPCs and own pets | SkillEffects.valid excludes Npc and owned Tamable |
| 8 | Ultimate key T == vanilla chat key: chat opens while casting | SkillKeyHandler swallows vanilla keys sharing a skill key while stance on |

## Checked, OK
Payload registration (Sync/Skill/Flight/Ma in one registrar), client classes only inside handler lambdas; STATE attachments transient by design; MaData/Cultivation/Age copyOnDeath; SectBuffs/AgeService/MaRules modifiers use fixed ids (no stacking); logout cleanup for AFK/meditation/flight/trial; spawner mark + KillRateGate path; Invasion caps (40 alive, 2 spawns/5 ticks).

## Residual / not fixed
- City/Sect builders call getChunk synchronously (one-time stall when first built); bounded, not changed.
- Altar fee is re-charged each new sitting (FEE_SECONDS reset on stop) - design question, not exploit.
- SectTrial: unloaded trial mob is dropped from the set but counted as spawned -> trial can only time out.
- ServerBossEvent players not removed on disconnect (minor).
- Dedicated-server and in-game verification still required.
