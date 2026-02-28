package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 条件字段元数据响应对象
 */
@Data
@Schema(description = "条件字段元数据")
public class StrategyTagFieldVO {

    @Schema(description = "字段ID")
    private Long id;

    @Schema(description = "字段标识，与条件树 field 值对应")
    private String fieldKey;

    @Schema(description = "字段展示名称")
    private String fieldName;

    @Schema(description = "字段分类：INHERENT-固有属性 / EXAM-考试属性 / COMPREHENSIVE-综合属性")
    private String category;

    @Schema(description = "数据类型：NUMBER-数值 / STRING-字符串 / ENUM-枚举")
    private String dataType;

    @Schema(description = "允许的运算符列表")
    private List<String> operators;

    @Schema(description = "适用对象列表")
    private List<String> applicableObjects;

    @Schema(description = "分类内排序")
    private Integer sort;
}
