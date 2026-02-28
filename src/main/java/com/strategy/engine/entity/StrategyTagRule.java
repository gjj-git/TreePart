package com.strategy.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 标签规则实体
 */
@Data
@TableName("strategy_tag_rule")
public class StrategyTagRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 引擎ID
     */
    private Long engineId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签说明
     */
    private String description;

    /**
     * 规则配置（JSON格式的条件树）
     */
    private String ruleConfig;

    /**
     * 规则SQL（由 ruleConfig 在保存时自动转换，供数据库/BI直接使用）
     */
    private String ruleSql;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除：0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
