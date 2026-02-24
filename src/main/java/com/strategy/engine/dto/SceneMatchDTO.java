package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 场景匹配请求 DTO
 */
@Data
@Schema(description = "场景匹配请求")
public class SceneMatchDTO {

    @NotNull(message = "引擎ID不能为空")
    @Schema(description = "引擎ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long engineId;

    @NotNull(message = "匹配数据不能为空")
    @Schema(description = "待匹配数据，key为字段名，value为字段值", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> data;
}
