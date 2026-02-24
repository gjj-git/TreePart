package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 策略引擎创建/更新 DTO
 */
@Data
@Schema(description = "策略引擎请求对象")
public class StrategyEngineDTO {

    @Schema(description = "引擎ID（更新时必填）")
    private Long id;

    @NotBlank(message = "引擎名称不能为空")
    @Schema(description = "引擎名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "引擎类型不能为空")
    @Schema(description = "引擎类型：COMPREHENSIVE_REVIEW-综合复习, SINGLE_EXAM-单场考试", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotBlank(message = "适用对象不能为空")
    @Schema(description = "适用对象：STUDENT-学生, CLASS-班级, GRADE-年级, BUREAU-教育局", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applicableObject;

    @Schema(description = "引擎描述")
    private String description;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0-禁用, 1-启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
