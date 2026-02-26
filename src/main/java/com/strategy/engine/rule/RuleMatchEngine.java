package com.strategy.engine.rule;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 规则条件树匹配引擎
 *
 * 使用方式：
 *   boolean matched = ruleMatchEngine.match(ruleConfigJson, dataMap);
 *
 * dataMap 为传入的待匹配数据，key 为字段名，value 为字段值字符串
 */
@Component
public class RuleMatchEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判断 dataMap 是否满足 ruleConfigJson 描述的条件树
     *
     * @param ruleConfigJson 条件树 JSON 字符串
     * @param dataMap        待匹配数据
     * @return true=满足，false=不满足
     */
    public boolean match(String ruleConfigJson, Map<String, String> dataMap) {
        if (StrUtil.isBlank(ruleConfigJson)) {
            // 无规则配置视为始终满足
            return true;
        }
        try {
            RuleNode root = objectMapper.readValue(ruleConfigJson, RuleNode.class);
            return evaluate(root, dataMap);
        } catch (Exception e) {
            throw new RuntimeException("规则配置解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归求值
     */
    private boolean evaluate(RuleNode node, Map<String, String> dataMap) {
        if ("group".equals(node.getType())) {
            return evaluateGroup(node, dataMap);
        } else {
            return evaluateCondition(node, dataMap);
        }
    }

    /**
     * 求值条件组（AND / OR）
     */
    private boolean evaluateGroup(RuleNode group, Map<String, String> dataMap) {
        List<RuleNode> children = group.getChildren();
        if (children == null || children.isEmpty()) {
            return true;
        }

        if ("OR".equalsIgnoreCase(group.getOperator())) {
            return children.stream().anyMatch(child -> evaluate(child, dataMap));
        } else {
            // 默认 AND
            return children.stream().allMatch(child -> evaluate(child, dataMap));
        }
    }

    /**
     * 求值单个条件
     */
    private boolean evaluateCondition(RuleNode condition, Map<String, String> dataMap) {
        String field = condition.getField();
        String op = condition.getOperator();
        String ruleValue = condition.getValue();

        if (StrUtil.isBlank(field) || StrUtil.isBlank(op)) {
            return true;
        }

        String dataValue = dataMap.get(field);
        if (dataValue == null) {
            return false;
        }

        switch (op.toUpperCase()) {
            case "=":
            case "EQ":
                return dataValue.equals(ruleValue);
            case "!=":
            case "NEQ":
                return !dataValue.equals(ruleValue);
            case ">":
            case "GT":
                return compareNumeric(dataValue, ruleValue) > 0;
            case ">=":
            case "GTE":
                return compareNumeric(dataValue, ruleValue) >= 0;
            case "<":
            case "LT":
                return compareNumeric(dataValue, ruleValue) < 0;
            case "<=":
            case "LTE":
                return compareNumeric(dataValue, ruleValue) <= 0;
            case "CONTAINS":
                return dataValue.contains(ruleValue);
            case "NOT_CONTAINS":
                return !dataValue.contains(ruleValue);
            case "IN":
                // ruleValue 格式：val1,val2,val3
                List<String> values = Arrays.asList(ruleValue.split(","));
                return values.contains(dataValue);
            case "NOT_IN":
                List<String> notValues = Arrays.asList(ruleValue.split(","));
                return !notValues.contains(dataValue);
            default:
                return false;
        }
    }

    /**
     * 数值比较，返回 -1/0/1
     */
    private int compareNumeric(String a, String b) {
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b));
        } catch (NumberFormatException e) {
            // 非数字则降级为字符串比较
            return a.compareTo(b);
        }
    }
}
