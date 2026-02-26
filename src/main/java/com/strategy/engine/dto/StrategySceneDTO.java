package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 场景策略创建/更新 DTO
 */
@Data
@Schema(description = "场景策略请求对象")
public class StrategySceneDTO {

    @Schema(description = "场景ID（更新时必填）")
    private Long id;

    @NotNull(message = "引擎ID不能为空")
    @Schema(description = "引擎ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long engineId;

    @NotBlank(message = "场景名称不能为空")
    @Schema(description = "场景名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "场景说明")
    private String description;

    @Valid
    @Schema(description = "标签关联配置列表（可选，不传则不修改已有关联）")
    private List<StrategySceneTagItemDTO> tags;
}
