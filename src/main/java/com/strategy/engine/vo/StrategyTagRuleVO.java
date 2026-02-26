package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签规则视图对象
 */
@Data
@Schema(description = "标签规则响应对象")
public class StrategyTagRuleVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "引擎ID")
    private Long engineId;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签说明")
    private String description;

    @Schema(description = "规则配置（条件树JSON字符串）")
    private String ruleConfig;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
