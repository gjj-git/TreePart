package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 场景标签关联配置 DTO（单独配置标签接口使用，含 sceneId）
 */
@Data
@Schema(description = "场景标签关联配置对象")
public class StrategySceneTagConfigDTO {

    @NotNull(message = "场景ID不能为空")
    @Schema(description = "场景ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sceneId;

    @NotNull(message = "标签ID不能为空")
    @Schema(description = "标签ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tagId;

    @NotNull(message = "权重系数不能为空")
    @Min(value = 1, message = "权重系数最小为1")
    @Max(value = 10, message = "权重系数最大为10")
    @Schema(description = "权重系数（1-10）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer weightCoefficient;
}
