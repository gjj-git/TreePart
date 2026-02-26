package com.strategy.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.strategy.engine.entity.StrategySceneTag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场景标签关联 Mapper
 */
@Mapper
public interface StrategySceneTagMapper extends BaseMapper<StrategySceneTag> {

    /**
     * 批量插入场景标签关联
     */
    @Insert("<script>" +
            "INSERT INTO strategy_scene_tag (scene_id, tag_id, weight_coefficient) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.sceneId}, #{item.tagId}, #{item.weightCoefficient})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<StrategySceneTag> list);
}
