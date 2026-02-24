package com.strategy.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 场景标签关联实体（权重配置）
 */
@Data
@TableName("scene_tag_relation")
public class SceneTagRelation {

    @TableId(type = IdType.AUTO)
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
     * 是否启用：0-否, 1-是
     */
    private Integer enabled;

    /**
     * 权重系数（1-10）
     */
    private Integer weightCoefficient;
}
