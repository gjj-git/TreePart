package com.strategy.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;

/**
 * 条件字段元数据实体
 * 定义在配置标签规则条件时可选用的字段
 */
@Data
@TableName(value = "strategy_tag_field", autoResultMap = true)
public class StrategyTagField {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字段标识，与条件树 field 值对应，如 difficulty_level
     */
    private String fieldKey;

    /**
     * 字段展示名称，如 难度等级
     */
    private String fieldName;

    /**
     * 字段分类：INHERENT-固有属性 / EXAM-考试属性 / COMPREHENSIVE-综合属性
     */
    private String category;

    /**
     * 数据类型：NUMBER-数值 / STRING-字符串 / ENUM-枚举
     */
    private String dataType;

    /**
     * 允许的运算符列表，JSON 数组，如 [">",">=","<","<=","="]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> operators;

    /**
     * 适用对象列表，JSON 数组，["ALL"] 表示所有引擎通用
     * 可选值：ALL / STUDENT / CLASS / SCHOOL / BUREAU
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> applicableObjects;

    /**
     * 分类内排序
     */
    private Integer sort;
}
