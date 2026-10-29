package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/** DEV ONLY (chi chay khi -Dhukhong.autotest=1, tuc ./gradlew runClient -Pautotest=<world>): tu danh quai + hoi mau + chup anh de kiem chung HUD. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevAutoTest {
    private DevAutoTest() {}
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("hukhong.autotest") != null;
    private static int t;
    private static Mob dummy;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (mc.isPaused()) return;
        t++;
        var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return;
        if (t == 200) server.execute(() -> {
            sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 at = sp.position().add(look.scale(4));
            dummy = EntityType.ZOMBIE.create(sp.serverLevel());
            dummy.moveTo(at.x, sp.getY(), at.z);
            dummy.setNoAi(true);
            dummy.setPersistenceRequired();
            dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            dummy.setHealth(200);
            sp.serverLevel().addFreshEntity(dummy);
            sp.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, dummy.position().add(0, 1, 0));
            LOG.info("AUTOTEST dummy spawned at {}", at);
        });
        if (t == 236 || t == 246 || t == 276) server.execute(() -> {
            if (dummy == null) return;
            sp.setNoGravity(true); sp.setDeltaMovement(Vec3.ZERO); sp.hurtMarked = true;
            Vec3 look = sp.getLookAngle();
            Vec3 at = sp.getEyePosition().add(look.scale(4)).add(0, -1.6, 0);
            dummy.setNoGravity(true); dummy.setDeltaMovement(Vec3.ZERO);
            dummy.moveTo(at.x, at.y, at.z);
            sp.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, dummy.position().add(0, 1, 0));
        });
        if (t == 240 || t == 250) server.execute(() -> {
            if (dummy != null) dummy.hurt(sp.damageSources().playerAttack(sp), 17f);
            LOG.info("AUTOTEST hit sent");
        });
        if (t == 244 || t == 254) shot("hit" + t);
        if (t == 280) server.execute(() -> { sp.setHealth(3f); sp.heal(8f); LOG.info("AUTOTEST heal sent"); });
        if (t == 284) shot("heal");
        if (t == 290 || t == 294 || t == 298 || t == 330) server.execute(() -> { // Thể Tu tay không vung 3 cái liền + 1 cái sau cửa sổ
            sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            sp.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        });
        if (t == 292) bcCheck();
        if (t == 340) { LOG.info("AUTOTEST done"); mc.stop(); }
    }

    private static void bcCheck() {
        if (!net.neoforged.fml.ModList.get().isLoaded("bettercombat")) { LOG.info("AUTOTEST BC not loaded"); return; }
        LOG.info("AUTOTEST BC attributes for ban_menh_kiem = {}", BcProbe.attrs());
    }

    private static final class BcProbe {
        static String attrs() {
            var st = new net.minecraft.world.item.ItemStack(com.hukhong.item.ModItems.BAN_MENH_KIEM.get());
            var a = net.bettercombat.logic.WeaponRegistry.getAttributes(st);
            return a == null ? "NULL" : "attacks=" + a.attacks().length + " category=" + a.category();
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), c -> LOG.info("AUTOTEST screenshot {}: {}", name, c.getString()));
    }
}
