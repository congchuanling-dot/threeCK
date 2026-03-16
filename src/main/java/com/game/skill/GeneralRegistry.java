package com.game.skill;

import com.game.domain.General;
import com.game.domain.Skill;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 武将注册表。
 * 便于扩展：未来可接入 AI 生成的武将，通过 register 动态添加。
 */
public class GeneralRegistry {

    private static final Map<String, General> BY_ID = new ConcurrentHashMap<>();

    static {
        // 赵云 - 龙胆：杀当闪，发动时摸一张牌
        Skill longdan = new Skill(
                "longdan",
                "龙胆",
                "你可以将一张杀当闪使用或打出。当你以此法使用或打出一张牌时，摸一张牌。",
                "SUBSTITUTE_CARD"
        );
        General zhaoyun = new General("zhaoyun", "赵云", List.of(longdan));
        BY_ID.put("zhaoyun", zhaoyun);

        // 默认武将（无技能，用于机器人等）
        General defaultGeneral = new General("default", "默认", List.of());
        BY_ID.put("default", defaultGeneral);
    }

    public static Optional<General> get(String generalId) {
        return Optional.ofNullable(BY_ID.get(generalId));
    }

    public static General getOrDefault(String generalId) {
        return BY_ID.getOrDefault(generalId, BY_ID.get("default"));
    }

    /** 扩展点：未来 AI 生成武将后可动态注册 */
    public static void register(General general) {
        if (general != null) BY_ID.put(general.getId(), general);
    }

    /** 可选武将列表（排除 default，用于选将框） */
    public static List<General> listSelectable() {
        return BY_ID.entrySet().stream()
                .filter(e -> !"default".equals(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }
}
