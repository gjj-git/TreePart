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
     * 逻辑运算符：AND / OR（type=group 时有效）
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
     * 比较操作符：= != > < >= <= CONTAINS IN（type=condition 时有效）
     */
    private String conditionOperator;

    /**
     * 比较值（type=condition 时有效）
     */
    private String value;
}
