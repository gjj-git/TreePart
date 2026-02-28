package com.strategy.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.strategy.engine.entity.StrategyTagField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 条件字段元数据 Mapper
 */
@Mapper
public interface StrategyTagFieldMapper extends BaseMapper<StrategyTagField> {

    /**
     * 根据适用对象查询可用字段
     * 返回通用字段（ALL）以及包含指定对象的字段
     * 使用 XML ResultMap 确保 JSON 列（operators/applicable_objects）正确反序列化
     */
    List<StrategyTagField> listByApplicableObject(@Param("applicableObject") String applicableObject);
}
