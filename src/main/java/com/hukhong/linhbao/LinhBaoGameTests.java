package com.hukhong.linhbao;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.artifact.ArtifactService;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.gametest.GameTestSupport;
import com.hukhong.pathrules.MaRules;
import com.hukhong.skill.SkillAttachments;
import com.hukhong.stats.StatService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Linh Bảo: cấp khi nhập đạo, bonus theo giai đoạn thật sự vào sát thương (qua hurt -> PathRules), tia Đạo Linh, giữ qua chết. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class LinhBaoGameTests {
    private LinhBaoGameTests() {}

    private static ServerPlayer player(GameTestHelper h, int level, Set<Path> paths) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-lb"));
        Map<Path, Aptitude> apt = new EnumMap<>(Path.class);
        for (Path x : paths) apt.put(x, Aptitude.B);
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 2000, 2000, paths, apt, false));
        StatService.refresh(p);
        GameTestSupport.noCrit(p);
        p.setHealth(p.getMaxHealth());
        return p;
    }

    private static void setStage(ServerPlayer p, Path path, int st) {
        p.setData(ArtifactAttachments.ARTIFACT.get(), ArtifactService.get(p).with(path, st));
    }

    private static Zombie dummy(GameTestHelper h, double rx, double rz) {
        Zombie z = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 v = h.absoluteVec(new Vec3(rx, 1, rz));
        z.moveTo(v.x, v.y, v.z, 0f, 0f);
        z.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(z.blockPosition()), MobSpawnType.COMMAND, null);
        z.setPersistenceRequired();
        z.setNoAi(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100000);
        z.getAttribute(Attributes.ARMOR).setBaseValue(0);
        GameTestSupport.deterministic(z);
        z.setHealth(100000);
        h.getLevel().addFreshEntity(z);
        return z;
    }

    /** Một đòn thường tay không lên zombie mới; trả máu mất. */
    private static float punch(GameTestHelper h, ServerPlayer p, double rz) {
        Zombie z = dummy(h, 2, rz);
        z.invulnerableTime = 0;
        float before = z.getHealth();
        z.hurt(h.getLevel().damageSources().playerAttack(p), 1f);
        float lost = before - z.getHealth();
        z.discard();
        return lost;
    }

    @GameTest(template = "empty")
    public static void linhBaoCapKhiNhapDao(GameTestHelper h) {
        ServerPlayer all = player(h, 10, Set.of(Path.DAO, Path.THE, Path.MA));
        for (LinhBaoType t : LinhBaoType.values()) if (!LinhBao.has(all, t)) h.fail("thieu Linh Bao " + t);
        ServerPlayer kiem = player(h, 10, Set.of(Path.KIEM));
        for (LinhBaoType t : LinhBaoType.values()) if (LinhBao.has(kiem, t)) h.fail("Kiem Tu khong duoc co " + t);
        // nguoi choi cu (chua tung tien hoa) van co o giai doan 0, va tran theo cap
        if (LinhBao.stage(all, LinhBaoType.LINH_THE) != 0) h.fail("giai doan mac dinh phai 0");
        setStage(all, Path.THE, 5);
        if (LinhBao.stage(all, LinhBaoType.LINH_THE) != 0) h.fail("cap 10 phai bi tran ve giai doan 0, thuc te " + LinhBao.stage(all, LinhBaoType.LINH_THE));
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void linhTheCongSatThuongTheoGiaiDoan(GameTestHelper h) {
        ServerPlayer p = player(h, 60, Set.of(Path.THE));
        setStage(p, Path.THE, 0);
        float lost0 = punch(h, p, 2);
        setStage(p, Path.THE, 3);
        float lost6 = punch(h, p, 4);
        double ap = StatService.attackPower(p);
        double expect = LinhBaoMath.theHit(ap, 3) - LinhBaoMath.theHit(ap, 0);
        if (lost6 - lost0 < expect - 1.0 || lost6 - lost0 > expect + 1.0)
            h.fail("chenh lech sat thuong " + (lost6 - lost0) + " != ky vong " + expect + " (st0=" + lost0 + ", st6=" + lost6 + ")");
        if (expect <= 0) h.fail("bonus phai > 0");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void maCongCongSatThuongVaHutMau(GameTestHelper h) {
        ServerPlayer p = player(h, 100, Set.of(Path.MA));
        setStage(p, Path.MA, 0);
        float lost0 = punch(h, p, 2);
        setStage(p, Path.MA, 6);
        float lost6 = punch(h, p, 4);
        double ap = StatService.attackPower(p);
        double expect = LinhBaoMath.maHit(ap, 6) - LinhBaoMath.maHit(ap, 0);
        if (Math.abs((lost6 - lost0) - expect) > 1.0) h.fail("chenh lech " + (lost6 - lost0) + " != " + expect);
        // hut mau: mat 20 mau, danh 1 don -> hoi
        p.setHealth(p.getMaxHealth() - 20f);
        float hp = p.getHealth();
        punch(h, p, 6);
        if (p.getHealth() <= hp) h.fail("Ma Cong khong hut mau: " + hp + " -> " + p.getHealth());
        // Mat Kiem Soat (Ac Tinh): tat hut mau
        MaRules.data(p).lostTicks = 100;
        hp = p.getHealth();
        punch(h, p, 8);
        if (p.getHealth() > hp) h.fail("Mat Kiem Soat van hut mau");
        MaRules.data(p).lostTicks = 0;
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void daoLinhBanTiaVaChuaDongDoi(GameTestHelper h) {
        ServerPlayer p = player(h, 60, Set.of(Path.DAO));
        setStage(p, Path.DAO, 3);
        Vec3 pos = h.absoluteVec(new Vec3(3, 1, 3));
        p.setPos(pos.x, pos.y, pos.z);
        // dong doi = thu cung cua minh (FakePlayer khong nam trong level.players())
        net.minecraft.world.entity.animal.Wolf ally = EntityType.WOLF.create(h.getLevel());
        ally.moveTo(pos.x + 1, pos.y, pos.z, 0f, 0f);
        ally.setNoAi(true);
        ally.tame(p);
        h.getLevel().addFreshEntity(ally);
        ally.setHealth(2f);
        Zombie z = dummy(h, 3, 5);
        // Dao danh quai = 0 sat thuong nhung van ghi nho muc tieu + danh dau combat
        z.invulnerableTime = 0;
        z.hurt(h.getLevel().damageSources().playerAttack(p), 1f);
        p.getData(SkillAttachments.STATE).lastCombat = h.getLevel().getGameTime();
        float before = z.getHealth();
        LinhBaoEvents.onTick(new PlayerTickEvent.Post(p));      // duong that: onTick -> daoTick -> fireBolt
        float lost = before - z.getHealth();
        float expect = (float) LinhBaoMath.daoBolt(StatService.spellPower(p), 3);
        if (lost < expect * 0.5f) h.fail("tia Dao Linh qua yeu: mat " + lost + " ky vong ~" + expect);
        // tia thu 2 (chan so chan) chua dong doi thap mau nhat
        float allyHp = ally.getHealth();
        LinhBaoEvents.fireBolt(p, h.getLevel(), z);
        if (ally.getHealth() <= allyHp) { h.fail("Dao Linh khong chua dong doi: " + allyHp + " -> " + ally.getHealth()); return; }
        z.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void linhBaoGiuQuaChet(GameTestHelper h) {
        ServerPlayer p = player(h, 100, Set.of(Path.DAO, Path.THE, Path.MA));
        setStage(p, Path.DAO, 3);
        setStage(p, Path.THE, 4);
        setStage(p, Path.MA, 5);
        ServerPlayer neo = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-lb-respawn"));
        neo.restoreFrom(p, false); // duong that khi chet + hoi sinh (khong giu moi thu)
        var a = ArtifactService.get(neo);
        if (a.dao() != 3 || a.the() != 4 || a.ma() != 5) h.fail("mat giai doan sau chet: " + a);
        for (LinhBaoType t : LinhBaoType.values()) if (!LinhBao.has(neo, t)) h.fail("mat Linh Bao sau chet: " + t);
        if (LinhBao.stage(neo, LinhBaoType.MA_CONG) != 5) h.fail("giai doan Ma Cong sai sau chet");
        h.succeed();
    }
}
