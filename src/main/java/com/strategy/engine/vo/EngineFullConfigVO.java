package com.strategy.engine.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 引擎完整配置 VO（用于返回所有Tab的数据）
 */
@Data
@Schema(description = "引擎完整配置响应对象")
public class EngineFullConfigVO {

    @Schema(description = "引擎ID")
    private Long engineId;

    /**
     * Tab 1: 基本信息
     */
    @Schema(description = "引擎基本信息")
    private BasicInfo basicInfo;

    /**
     * Tab 2: 标签规则列表
     */
    @Schema(description = "标签规则列表")
    private List<StrategyTagRuleVO> tags;

    /**
     * Tab 3: 场景策略列表
     */
    @Schema(description = "场景策略列表")
    private List<SceneWithTagsVO> scenes;

    /**
     * 基本信息
     */
    @Data
    @Schema(description = "引擎基本信息")
    public static class BasicInfo {
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

        @Schema(description = "场景总数")
        private Integer sceneCount;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "更新时间")
        private LocalDateTime updatedTime;
    }

    /**
     * 场景及其标签配置
     */
    @Data
    @Schema(description = "场景及标签配置")
    public static class SceneWithTagsVO {
        @Schema(description = "场景ID")
        private Long id;

        @Schema(description = "场景名称")
        private String name;

        @Schema(description = "场景说明")
        private String description;

        @Schema(description = "创建时间")
        private LocalDateTime createdTime;

        @Schema(description = "场景关联的标签配置")
        private List<StrategySceneTagVO> tagConfigs;
    }
}
