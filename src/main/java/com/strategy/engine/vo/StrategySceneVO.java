package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景策略视图对象（包含标签配置）
 */
@Data
@Schema(description = "场景策略响应对象")
public class StrategySceneVO {

    @Schema(description = "场景ID")
    private Long id;

    @Schema(description = "引擎ID")
    private Long engineId;

    @Schema(description = "场景名称")
    private String name;

    @Schema(description = "场景说明")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "关联的标签配置列表")
    private List<StrategySceneTagVO> tags;
}
