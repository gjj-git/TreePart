package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 策略引擎查询 DTO
 */
@Data
@Schema(description = "策略引擎查询对象")
public class StrategyEngineQueryDTO {

    @Schema(description = "引擎名称（模糊查询）")
    private String name;

    @Schema(description = "引擎类型")
    private String type;

    @Schema(description = "适用对象")
    private String applicableObject;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
