package com.strategy.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 场景标签关联实体（权重配置）
 */
@Data
@TableName("strategy_scene_tag")
public class StrategySceneTag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 场景ID
     */
    private Long sceneId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 权重系数（1-10）
     */
    private Integer weightCoefficient;
}
