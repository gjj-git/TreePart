package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 条件字段元数据创建/更新 DTO
 */
@Data
@Schema(description = "条件字段元数据请求对象")
public class StrategyTagFieldDTO {

    @Schema(description = "字段ID（更新时必填）")
    private Long id;

    @NotBlank(message = "字段标识不能为空")
    @Schema(description = "字段标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldKey;

    @NotBlank(message = "字段名称不能为空")
    @Schema(description = "字段展示名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @NotBlank(message = "字段分类不能为空")
    @Schema(description = "字段分类：INHERENT / EXAM / COMPREHENSIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @NotBlank(message = "数据类型不能为空")
    @Schema(description = "数据类型：NUMBER / STRING / ENUM", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataType;

    @NotNull(message = "运算符列表不能为空")
    @Schema(description = "允许的运算符列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> operators;

    @NotNull(message = "适用对象不能为空")
    @Schema(description = "适用对象列表，[\"ALL\"] 表示通用", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> applicableObjects;

    @Schema(description = "分类内排序，默认0")
    private Integer sort;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0-禁用 / 1-启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
