package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 标签规则创建/更新 DTO
 */
@Data
@Schema(description = "标签规则请求对象")
public class TagRuleDTO {

    @Schema(description = "标签ID（更新时必填）")
    private Long id;

    @NotNull(message = "引擎ID不能为空")
    @Schema(description = "引擎ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long engineId;

    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "标签说明")
    private String description;

    @Schema(description = "规则配置（条件树JSON字符串）")
    private String ruleConfig;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0-禁用, 1-启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
