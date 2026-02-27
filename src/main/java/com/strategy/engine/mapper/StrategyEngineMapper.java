package com.strategy.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.strategy.engine.entity.StrategyEngine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 策略引擎 Mapper
 */
@Mapper
public interface StrategyEngineMapper extends BaseMapper<StrategyEngine> {

    /**
     * 原子性设置默认引擎（一条 SQL 完成清除+设置，避免并发问题）
     */
    @Update("UPDATE strategy_engine SET is_default = CASE WHEN id = #{id} THEN 1 ELSE 0 END WHERE deleted = 0")
    void atomicSetDefault(@Param("id") Long id);
}
