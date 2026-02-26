package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场景标签配置视图对象
 */
@Data
@Schema(description = "场景标签配置响应对象")
public class StrategySceneTagVO {

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签说明")
    private String tagDescription;

    @Schema(description = "权重系数")
    private Integer weightCoefficient;
}
