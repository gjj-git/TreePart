package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略引擎视图对象
 */
@Data
@Schema(description = "策略引擎响应对象")
public class StrategyEngineVO {

    @Schema(description = "引擎ID")
    private Long id;

    @Schema(description = "引擎名称")
    private String name;

    @Schema(description = "引擎类型")
    private String type;

    @Schema(description = "适用对象")
    private String applicableObject;

    @Schema(description = "引擎描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "是否默认")
    private Integer isDefault;

    @Schema(description = "标签总数")
    private Integer tagCount;

    @Schema(description = "策略总数")
    private Integer sceneCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
