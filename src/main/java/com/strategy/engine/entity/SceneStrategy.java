package com.strategy.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 场景策略实体
 */
@Data
@TableName("scene_strategy")
public class SceneStrategy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 引擎ID
     */
    private Long engineId;

    /**
     * 场景名称
     */
    private String name;

    /**
     * 场景说明
     */
    private String description;

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
