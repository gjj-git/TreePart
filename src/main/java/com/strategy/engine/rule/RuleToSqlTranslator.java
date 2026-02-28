package com.strategy.engine.rule;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则条件树 → SQL WHERE 片段转换器
 *
 * 输入：ruleConfig JSON 字符串（与 RuleMatchEngine 使用相同的条件树格式）
 * 输出：SQL WHERE 条件字符串，如：
 *   exam_mastery > 60 AND (difficulty_level IN ('HIGH', 'VERY_HIGH') OR kp_stage = '初中')
 *
 * 用途：在标签规则保存时由后端调用，将结果存入 rule_sql 列，
 *       供 Superset / 数据库层直接使用，避免查询时实时转换。
 *
 * 注意：此类为纯工具类，无 Spring 依赖，可在任意上下文调用。
 */
public class RuleToSqlTranslator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将 ruleConfig JSON 转为 SQL WHERE 片段
     *
     * @param ruleConfigJson 条件树 JSON 字符串，为空时返回 null
     * @return SQL WHERE 片段字符串，如 "a > 1 AND (b = 'x' OR c IN ('p','q'))"
     */
    public static String translate(String ruleConfigJson) {
        if (StrUtil.isBlank(ruleConfigJson)) {
            return null;
        }
        try {
            RuleNode root = MAPPER.readValue(ruleConfigJson, RuleNode.class);
            return nodeToSql(root);
        } catch (Exception e) {
            throw new IllegalArgumentException("规则配置解析失败，无法转换为SQL: " + e.getMessage(), e);
        }
    }

    private static String nodeToSql(RuleNode node) {
        if (node.getType() == null) {
            throw new IllegalArgumentException("节点 type 不能为空");
        }
        if ("group".equals(node.getType())) {
            return groupToSql(node);
        } else if ("condition".equals(node.getType())) {
            return conditionToSql(node);
        } else {
            throw new IllegalArgumentException("不支持的节点类型: " + node.getType());
        }
    }

    /**
     * 条件组：children 用 AND/OR 拼接，超过一个子节点时加括号
     */
    private static String groupToSql(RuleNode group) {
        List<RuleNode> children = group.getChildren();
        if (children == null || children.isEmpty()) {
            return "1=1";
        }
        if (StrUtil.isBlank(group.getOperator())) {
            throw new IllegalArgumentException("条件组 operator 不能为空");
        }
        String op = group.getOperator().toUpperCase();
        if (!"AND".equals(op) && !"OR".equals(op)) {
            throw new IllegalArgumentException("条件组 operator 只支持 AND/OR，当前值: " + group.getOperator());
        }
        String connector = " " + op + " ";
        List<String> parts = children.stream()
                .map(child -> {
                    String part = nodeToSql(child);
                    // 子节点是 group 时加括号，保证优先级正确
                    if ("group".equals(child.getType())
                            && child.getChildren() != null
                            && child.getChildren().size() > 1) {
                        return "(" + part + ")";
                    }
                    return part;
                })
                .collect(Collectors.toList());
        return String.join(connector, parts);
    }

    /**
     * 单个条件节点 → SQL 片段
     */
    private static String conditionToSql(RuleNode condition) {
        String field = condition.getField();
        String op = condition.getOperator();
        String value = condition.getValue();

        if (StrUtil.isBlank(field) || StrUtil.isBlank(op)) {
            return "1=1";
        }

        // 字段名只允许字母、数字、下划线，防止字段名注入
        if (!field.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("非法字段名: " + field);
        }

        String opUpper = op.toUpperCase();

        // 数值运算符校验 value 必须为数字
        if (("GT".equals(opUpper) || ">".equals(opUpper)
                || "GTE".equals(opUpper) || ">=".equals(opUpper)
                || "LT".equals(opUpper) || "<".equals(opUpper)
                || "LTE".equals(opUpper) || "<=".equals(opUpper))
                && !isNumeric(value)) {
            throw new IllegalArgumentException(
                    "字段 " + field + " 使用数值运算符 " + op + "，但 value 不是合法数字: " + value);
        }

        // CONTAINS / NOT_CONTAINS 的 value 不能为 null
        if (("CONTAINS".equals(opUpper) || "NOT_CONTAINS".equals(opUpper)) && value == null) {
            throw new IllegalArgumentException("字段 " + field + " 使用 " + op + " 运算符时 value 不能为 null");
        }

        switch (opUpper) {
            case "=":
            case "EQ":
                return field + " = " + sqlValue(value);
            case "!=":
            case "NEQ":
                return field + " != " + sqlValue(value);
            case ">":
            case "GT":
                return field + " > " + sqlValue(value);
            case ">=":
            case "GTE":
                return field + " >= " + sqlValue(value);
            case "<":
            case "LT":
                return field + " < " + sqlValue(value);
            case "<=":
            case "LTE":
                return field + " <= " + sqlValue(value);
            case "CONTAINS":
                return field + " LIKE '%" + escapeLike(value) + "%'";
            case "NOT_CONTAINS":
                return field + " NOT LIKE '%" + escapeLike(value) + "%'";
            case "IN":
                return field + " IN (" + inValues(value) + ")";
            case "NOT_IN":
                return field + " NOT IN (" + inValues(value) + ")";
            default:
                throw new IllegalArgumentException("不支持的运算符: " + op);
        }
    }

    /**
     * 将单个值包装为 SQL 字面量：纯数字不加引号，其他加单引号并转义
     */
    private static String sqlValue(String value) {
        if (value == null) {
            return "NULL";
        }
        if (isNumeric(value)) {
            return value;
        }
        return "'" + escapeSql(value) + "'";
    }

    /**
     * IN/NOT_IN 的多值转换：逗号分隔，每个值独立处理
     */
    private static String inValues(String value) {
        if (StrUtil.isBlank(value)) {
            return "NULL";
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(RuleToSqlTranslator::sqlValue)
                .collect(Collectors.joining(", "));
    }

    private static boolean isNumeric(String value) {
        try {
            new java.math.BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 转义 SQL 单引号
     */
    private static String escapeSql(String value) {
        return value.replace("'", "''");
    }

    /**
     * 转义 LIKE 通配符
     */
    private static String escapeLike(String value) {
        return escapeSql(value)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
