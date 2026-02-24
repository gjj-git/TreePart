package com.strategy.engine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 引擎完整配置 DTO（用于一次性保存所有配置）
 */
@Data
@Schema(description = "引擎完整配置对象")
public class EngineFullConfigDTO {

    @Schema(description = "引擎ID（编辑时必填，新增时为空）")
    private Long engineId;

    /**
     * Tab 1: 基本信息
     */
    @Valid
    @NotNull(message = "基本信息不能为空")
    @Schema(description = "引擎基本信息")
    private BasicInfo basicInfo;

    /**
     * Tab 2: 标签规则列表
     */
    @Valid
    @Schema(description = "标签规则列表")
    private List<TagRuleItem> tags;

    /**
     * Tab 3: 场景策略列表
     */
    @Valid
    @Schema(description = "场景策略列表")
    private List<SceneStrategyItem> scenes;

    /**
     * 基本信息
     */
    @Data
    @Schema(description = "引擎基本信息")
    public static class BasicInfo {
        @NotBlank(message = "引擎名称不能为空")
        @Schema(description = "引擎名称")
        private String name;

        @NotBlank(message = "引擎类型不能为空")
        @Schema(description = "引擎类型")
        private String type;

        @NotBlank(message = "适用对象不能为空")
        @Schema(description = "适用对象")
        private String applicableObject;

        @Schema(description = "引擎描述")
        private String description;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "是否默认")
        private Integer isDefault;
    }

    /**
     * 标签规则项
     */
    @Data
    @Schema(description = "标签规则项")
    public static class TagRuleItem {
        @Schema(description = "标签ID（前端临时ID，后端会重新分配）")
        private String tempId;

        @Schema(description = "数据库ID（编辑已有标签时才有）")
        private Long id;

        @NotBlank(message = "标签名称不能为空")
        @Schema(description = "标签名称")
        private String name;

        @Schema(description = "标签说明")
        private String description;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "操作类型：add-新增, update-更新, delete-删除")
        private String action;
    }

    /**
     * 场景策略项
     */
    @Data
    @Schema(description = "场景策略项")
    public static class SceneStrategyItem {
        @Schema(description = "数据库ID（编辑已有场景时才有）")
        private Long id;

        @NotBlank(message = "场景名称不能为空")
        @Schema(description = "场景名称")
        private String name;

        @Schema(description = "场景说明")
        private String description;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "操作类型：add-新增, update-更新, delete-删除")
        private String action;

        @Valid
        @Schema(description = "场景关联的标签配置")
        private List<SceneTagConfig> tagConfigs;
    }

    /**
     * 场景标签配置
     */
    @Data
    @Schema(description = "场景标签配置")
    public static class SceneTagConfig {
        @Schema(description = "标签的临时ID（用于引用新增的标签）")
        private String tagTempId;

        @Schema(description = "标签的数据库ID（用于引用已有标签）")
        private Long tagId;

        @NotNull(message = "是否启用不能为空")
        @Schema(description = "是否启用")
        private Integer enabled;

        @NotNull(message = "权重系数不能为空")
        @Schema(description = "权重系数(1-10)", minimum = "1", maximum = "10")
        private Integer weightCoefficient;
    }
}
