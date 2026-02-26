package com.strategy.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.strategy.engine.entity.StrategyTagField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 条件字段元数据 Mapper
 */
@Mapper
public interface StrategyTagFieldMapper extends BaseMapper<StrategyTagField> {

    /**
     * 根据适用对象查询可用字段
     * 返回通用字段（ALL）以及包含指定对象的字段
     */
    @Select("SELECT * FROM strategy_tag_field " +
            "WHERE status = 1 " +
            "AND (JSON_CONTAINS(applicable_objects, '\"ALL\"') " +
            "  OR JSON_CONTAINS(applicable_objects, CONCAT('\"', #{applicableObject}, '\"'))) " +
            "ORDER BY category, sort")
    List<StrategyTagField> listByApplicableObject(@Param("applicableObject") String applicableObject);
}
