package com.hukhong.story.npc;

import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** NPC tuyến chính (mô hình dân làng): đứng trong thành, bấm phải để thoại. Chỉ hoạt động ở Tuyến truyện. */
public class StoryNpc extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC = SynchedEntityData.defineId(StoryNpc.class, EntityDataSerializers.STRING);

    public StoryNpc(EntityType<? extends StoryNpc> t, Level l) {
        super(t, l);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(NPC, "");
    }

    public String npcId() { return entityData.get(NPC); }

    public void setNpcId(String id) {
        entityData.set(NPC, id);
        Dialogue d = DialogueManager.get(id);
        setCustomName(Component.translatable(d != null ? d.nameKey() : "hukhong.npc." + id));
        setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override public boolean isInvulnerableTo(DamageSource s) { return !s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (sp.getData(StoryRegistry.STORY.get()).mode() != StoryData.MODE_STORY)
                sp.displayClientMessage(Component.translatable("hukhong.npc.free_mode"), true);
            else DialogueService.start(sp, this);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putString("Npc", npcId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("Npc")) setNpcId(t.getString("Npc"));
    }
}
