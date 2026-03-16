package com.game.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 武将实体。
 * 包含武将名称与技能列表，便于扩展（含 AI 生成武将）。
 */
public class General {

    private final String id;
    private final String name;
    private final List<Skill> skills;

    public General(String id, String name, List<Skill> skills) {
        this.id = Objects.requireNonNull(id);
        this.name = name != null ? name : id;
        this.skills = skills != null ? List.copyOf(skills) : List.of();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<Skill> getSkills() { return Collections.unmodifiableList(skills); }

    public Skill getSkillById(String skillId) {
        return skills.stream().filter(s -> s.getId().equals(skillId)).findFirst().orElse(null);
    }
}
