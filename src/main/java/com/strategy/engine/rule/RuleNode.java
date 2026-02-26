package com.strategy.engine.rule;

import lombok.Data;

import java.util.List;

/**
 * 规则条件树节点
 *
 * type = "group"     → 条件组，包含 operator（AND/OR）和 children
 * type = "condition" → 叶子条件，包含 field、operator、value
 */
@Data
public class RuleNode {

    /**
     * 节点类型：group / condition
     */
    private String type;

    // -------- group 专属 --------

    /**
     * 运算符：
     * type=group     时表示逻辑运算符：AND / OR
     * type=condition 时表示比较运算符：= != > >= < <= CONTAINS NOT_CONTAINS IN NOT_IN
     */
    private String operator;

    /**
     * 子节点（type=group 时有效）
     */
    private List<RuleNode> children;

    // -------- condition 专属 --------

    /**
     * 字段名（type=condition 时有效）
     */
    private String field;

    /**
     * 比较值（type=condition 时有效）
     */
    private String value;
}
