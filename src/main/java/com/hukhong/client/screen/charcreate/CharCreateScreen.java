package com.hukhong.client.screen.charcreate;

import com.hukhong.charcreate.CharCreateService;
import com.hukhong.charcreate.CharRoll;
import com.hukhong.core.Aptitude;
import com.hukhong.core.Path;
import com.hukhong.network.ConfirmCharCreatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.Set;

/** Màn tạo nhân vật: bảng 4 đạo + tư chất (lộ dần), xuất thân, tuổi. */
public class CharCreateScreen extends Screen {
    private static final int CELL_W = 90, CELL_H = 70, GAP = 8, REVEAL_STEP = 14;

    private final CharRoll roll;
    private final Set<Path> selected = EnumSet.noneOf(Path.class);
    private final boolean[] revealed = new boolean[Path.values().length];
    private int ticks;
    private Button confirm;

    public CharCreateScreen(CharRoll roll) {
        super(Component.translatable("hukhong.charcreate.title"));
        this.roll = roll;
    }

    public static void open(CharRoll roll) {
        Minecraft.getInstance().setScreen(new CharCreateScreen(roll));
    }

    @Override
    protected void init() {
        confirm = addRenderableWidget(Button.builder(Component.translatable("hukhong.charcreate.confirm"), b -> {
            int mask = 0;
            for (Path p : selected) mask |= 1 << p.ordinal();
            PacketDistributor.sendToServer(new ConfirmCharCreatePayload(mask));
            onClose();
        }).bounds(width / 2 - 50, height / 2 + 60, 100, 20).build());
        confirm.active = false;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    private boolean allRevealed() { return ticks >= REVEAL_STEP * Path.values().length + 6; }

    private int required() { return CharCreateService.requiredPicks(roll); }

    private int cellX(int i) {
        int total = Path.values().length * CELL_W + (Path.values().length - 1) * GAP;
        return width / 2 - total / 2 + i * (CELL_W + GAP);
    }

    private int cellY() { return height / 2 - 55; }

    @Override
    public void tick() {
        ticks++;
        for (Path p : Path.values()) {
            int i = p.ordinal();
            if (!revealed[i] && ticks >= REVEAL_STEP * (i + 1)) {
                revealed[i] = true;
                playReveal(roll.aptitude().get(p));
            }
        }
        confirm.active = allRevealed() && selected.size() == required();
    }

    private static void playReveal(Aptitude a) {
        SoundEvent s = switch (a) {
            case D, C, B -> SoundEvents.UI_BUTTON_CLICK.value();
            case A -> SoundEvents.EXPERIENCE_ORB_PICKUP;
            case S, SS -> SoundEvents.PLAYER_LEVELUP;
            case SSS -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
        };
        float pitch = a.ordinal() >= Aptitude.A.ordinal() ? 1.0f : 1.4f;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch));
    }

    static int tierColor(Aptitude a, float time) {
        return switch (a) {
            case D -> 0xFF9A9A9A;
            case C -> 0xFF55FF55;
            case B -> 0xFF55AAFF;
            case A -> 0xFFC070FF;
            case S -> 0xFFFFD34F;
            case SS -> 0xFFFF5555;
            case SSS -> 0xFF000000 | Mth.hsvToRgb((time * 0.02f) % 1f, 0.7f, 1f);
        };
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && allRevealed()) {
            for (Path p : Path.values()) {
                int x = cellX(p.ordinal()), y = cellY();
                if (mx >= x && mx < x + CELL_W && my >= y && my < y + CELL_H) {
                    if (!CharCreateService.UNLOCKED.contains(p)) return true;
                    if (!selected.remove(p) && selected.size() < required()) selected.add(p);
                    Minecraft.getInstance().getSoundManager()
                            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderTransparentBackground(g);
        float time = ticks + partial;
        g.drawCenteredString(font, title, width / 2, cellY() - 40, 0xFFFFFFFF);
        Component origin = Component.translatable(roll.origin().langKey());
        g.drawCenteredString(font, Component.translatable("hukhong.charcreate.origin", origin)
                .append("   ").append(Component.translatable("hukhong.hud.age", roll.startAge())), width / 2, cellY() - 26, 0xFFFFD34F);
        g.drawCenteredString(font, Component.translatable("hukhong.charcreate.slots." + roll.slots())
                .append("  ").append(Component.translatable("hukhong.charcreate.pick", selected.size(), required())),
                width / 2, cellY() - 14, 0xFFCCCCCC);

        for (Path p : Path.values()) {
            int x = cellX(p.ordinal()), y = cellY();
            boolean locked = !CharCreateService.UNLOCKED.contains(p);
            boolean sel = selected.contains(p);
            boolean shown = revealed[p.ordinal()];
            Aptitude a = roll.aptitude().get(p);
            boolean hot = shown && a.ordinal() >= Aptitude.A.ordinal();
            int border = sel ? 0xFFFFFFFF : 0xFF555555;
            if (hot && !locked) {
                int c = tierColor(a, time);
                float pulse = 0.5f + 0.5f * Mth.sin(time * 0.35f);
                int alpha = (int) (110 + 145 * pulse);
                g.fill(x - 2, y - 2, x + CELL_W + 2, y + CELL_H + 2, (alpha << 24) | (c & 0xFFFFFF));
            }
            g.fill(x - 1, y - 1, x + CELL_W + 1, y + CELL_H + 1, border);
            g.fill(x, y, x + CELL_W, y + CELL_H, locked ? 0xFF222222 : 0xFF111111);
            g.fill(x, y, x + CELL_W, y + 3, p.argb);

            g.drawCenteredString(font, Component.translatable(pathLang(p)), x + CELL_W / 2, y + 8, locked ? 0xFF777777 : p.argb);
            if (locked) {
                g.drawCenteredString(font, Component.translatable("hukhong.charcreate.locked"), x + CELL_W / 2, y + 32, 0xFF888888);
                g.drawCenteredString(font, Component.literal("✖"), x + CELL_W / 2, y + 48, 0xFF666666);
                continue;
            }
            if (!shown) {
                String q = ((int) (time / 2) % 2 == 0) ? "?" : "??";
                g.pose().pushPose();
                g.pose().translate(x + CELL_W / 2f, y + 28, 0);
                g.pose().scale(2f, 2f, 1f);
                g.drawCenteredString(font, q, 0, 0, 0xFF777777);
                g.pose().popPose();
            } else {
                g.pose().pushPose();
                g.pose().translate(x + CELL_W / 2f, y + 26, 0);
                float sc = a.ordinal() >= Aptitude.S.ordinal() ? 3f : 2.5f;
                g.pose().scale(sc, sc, 1f);
                g.drawCenteredString(font, Component.translatable("hukhong.aptitude." + a.name().toLowerCase()),
                        0, 0, tierColor(a, time));
                g.pose().popPose();
                if (sel) g.drawCenteredString(font, Component.literal("✔"), x + CELL_W / 2, y + 54, 0xFF55FF55);
            }
        }
        super.render(g, mx, my, partial);
    }

    private static String pathLang(Path p) {
        return switch (p) {
            case KIEM -> "hukhong.path.sword";
            case DAO -> "hukhong.path.dao";
            case THE -> "hukhong.path.body";
            case MA -> "hukhong.path.demon";
        };
    }
}
