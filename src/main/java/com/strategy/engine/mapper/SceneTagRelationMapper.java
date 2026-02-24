package com.strategy.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.strategy.engine.entity.SceneTagRelation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场景标签关联 Mapper
 */
@Mapper
public interface SceneTagRelationMapper extends BaseMapper<SceneTagRelation> {

    /**
     * 批量插入场景标签关联
     */
    @Insert("<script>" +
            "INSERT INTO scene_tag_relation (scene_id, tag_id, enabled, weight_coefficient) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.sceneId}, #{item.tagId}, #{item.enabled}, #{item.weightCoefficient})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<SceneTagRelation> list);
}
