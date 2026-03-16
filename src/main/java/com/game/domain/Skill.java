package com.game.domain;

import java.util.Objects;

/**
 * 技能实体。
 * 用于武将技能描述与逻辑扩展（含 AI 生成武将时的元数据）。
 */
public class Skill {

    private final String id;
    private final String name;
    private final String description;
    /** 效果类型标识，便于扩展与 AI 理解，如 "SUBSTITUTE_CARD" */
    private final String effectType;

    public Skill(String id, String name, String description, String effectType) {
        this.id = Objects.requireNonNull(id);
        this.name = name != null ? name : id;
        this.description = description != null ? description : "";
        this.effectType = effectType;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getEffectType() { return effectType; }
}
