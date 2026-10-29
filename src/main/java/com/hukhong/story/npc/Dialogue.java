package com.hukhong.story.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hukhong.core.CultivationService;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bảng thoại dữ liệu (data/hukhong/hukhong/dialogue/*.json): điều kiện + hành động + node. */
public record Dialogue(String npc, String city, String nameKey, List<Entry> entry, Map<String, Node> nodes) {
    public record Cond(int minLevel, int maxLevel, int chapterMin, int chapterMax, List<String> flags,
                       List<String> notFlags, List<String> docs, int docsMin, int lair, List<String> steps) {
        public static final Cond ANY = new Cond(0, 999, 0, 99, List.of(), List.of(), List.of(), 0, 0, List.of());

        static Cond parse(JsonObject o) {
            if (o == null) return ANY;
            return new Cond(i(o, "min_level", 0), i(o, "max_level", 999), i(o, "chapter_min", 0), i(o, "chapter_max", 99),
                    strs(o, "flags"), strs(o, "not_flags"), strs(o, "docs"), i(o, "docs_min", 0), i(o, "lair", 0), strs(o, "steps"));
        }

        public boolean test(ServerPlayer p) {
            int lv = CultivationService.get(p).level();
            QuestData q = DialogueService.quest(p);
            StoryData s = p.getData(StoryRegistry.STORY.get());
            if (lv < minLevel || lv > maxLevel || q.current() < chapterMin || q.current() > chapterMax) return false;
            for (String f : flags) if (!q.has(f)) return false;
            for (String f : notFlags) if (q.has(f)) return false;
            for (String d : docs) if (!s.has(d)) return false;
            if (!steps.isEmpty() && !steps.contains(q.step())) return false;
            if (s.docs().size() < docsMin) return false;
            return lair == 0 || (s.lair() & lair) != 0;
        }
    }

    public record Action(String type, String str, long num) {
        static Action parse(JsonObject o) {
            String t = o.get("type").getAsString();
            return switch (t) {
                case "set_flag" -> new Action(t, o.get("flag").getAsString(), 0);
                case "give_item" -> new Action(t, o.get("item").getAsString(), i(o, "count", 1));
                case "give_skill" -> new Action(t, o.get("skill").getAsString(), 1);
                case "exp" -> new Action(t, "", o.get("amount").getAsLong());
                case "chapter" -> new Action(t, "", o.get("value").getAsInt());
                case "step_done" -> new Action(t, o.get("step").getAsString(), 0);
                case "duel" -> new Action(t, "", 0);
                case "qc" -> new Action(t, o.get("chain").getAsString(), o.get("stage").getAsInt());
                case "doc" -> new Action(t, o.get("id").getAsString(), 0);
                case "message" -> new Action(t, o.get("key").getAsString(), 0);
                default -> throw new IllegalArgumentException("unknown action " + t);
            };
        }

        public void run(ServerPlayer p) {
            switch (type) {
                case "set_flag" -> DialogueService.setQuest(p, DialogueService.quest(p).withFlag(str));
                case "chapter" -> { } // cũ: chương nay theo cảnh giới (StoryFlow), giữ để tương thích dữ liệu
                case "step_done" -> com.hukhong.story.flow.StoryFlow.complete(p, str);
                case "duel" -> com.hukhong.story.flow.StoryFlow.startDuel(p);
                case "qc" -> com.hukhong.story.chain.QuestChains.setStage(p, str, (int) num);
                case "doc" -> com.hukhong.story.StoryService.collect(p, str);
                case "exp" -> CultivationService.addExp(p, num);
                case "message" -> p.displayClientMessage(Component.translatable(str), false);
                case "give_skill" -> com.hukhong.skillbook.SkillBookRewards.giveStory(p, str);
                case "give_item" -> {
                    var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(str));
                    if (item.isEmpty()) return;
                    ItemStack st = new ItemStack(item.get(), (int) num);
                    if (!p.getInventory().add(st)) p.drop(st, false);
                }
                default -> { }
            }
        }
    }

    public record Choice(String textKey, Cond cond, List<Action> actions, String next) {}

    public record Node(List<String> text, List<Action> actions, List<Choice> choices) {}

    public record Entry(Cond cond, String node) {}

    public String firstNode(ServerPlayer p) {
        for (Entry e : entry) if (e.cond().test(p)) return e.node();
        return null;
    }

    static Dialogue parse(JsonObject o) {
        List<Entry> en = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("entry")) {
            JsonObject eo = e.getAsJsonObject();
            en.add(new Entry(Cond.parse(eo.has("if") ? eo.getAsJsonObject("if") : null), eo.get("node").getAsString()));
        }
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (var ne : o.getAsJsonObject("nodes").entrySet()) {
            JsonObject no = ne.getValue().getAsJsonObject();
            List<Choice> cs = new ArrayList<>();
            if (no.has("choices")) for (JsonElement c : no.getAsJsonArray("choices")) {
                JsonObject co = c.getAsJsonObject();
                cs.add(new Choice(co.get("text").getAsString(), Cond.parse(co.has("if") ? co.getAsJsonObject("if") : null),
                        actions(co), co.has("next") ? co.get("next").getAsString() : null));
            }
            nodes.put(ne.getKey(), new Node(strs(no, "text"), actions(no), cs));
        }
        return new Dialogue(o.get("npc").getAsString(), o.get("city").getAsString(), o.get("name_key").getAsString(), en, nodes);
    }

    private static List<Action> actions(JsonObject o) {
        List<Action> l = new ArrayList<>();
        if (o.has("actions")) for (JsonElement a : o.getAsJsonArray("actions")) l.add(Action.parse(a.getAsJsonObject()));
        return l;
    }

    private static int i(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }

    private static List<String> strs(JsonObject o, String k) {
        List<String> l = new ArrayList<>();
        if (o.has(k)) for (JsonElement e : (JsonArray) o.get(k)) l.add(e.getAsString());
        return l;
    }
}
