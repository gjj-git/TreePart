package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 按分类分组的字段列表响应对象
 * 供前端规则编辑器左侧字段库使用
 */
@Data
@Schema(description = "字段分类及其字段列表")
public class StrategyTagFieldGroupVO {

    @Schema(description = "分类标识：INHERENT / EXAM / COMPREHENSIVE")
    private String category;

    @Schema(description = "分类展示名称")
    private String categoryName;

    @Schema(description = "该分类下的字段列表")
    private List<StrategyTagFieldVO> fields;
}
