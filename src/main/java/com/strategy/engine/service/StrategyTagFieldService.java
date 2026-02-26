package com.strategy.engine.service;

import com.strategy.engine.dto.StrategyTagFieldDTO;
import com.strategy.engine.vo.StrategyTagFieldGroupVO;
import com.strategy.engine.vo.StrategyTagFieldVO;

import java.util.List;

/**
 * 条件字段元数据 Service
 */
public interface StrategyTagFieldService {

    List<StrategyTagFieldGroupVO> listGroupedByEngineId(Long engineId);

    List<StrategyTagFieldVO> listAll();

    StrategyTagFieldVO getById(Long id);

    Long create(StrategyTagFieldDTO dto);

    void update(StrategyTagFieldDTO dto);

    void delete(Long id);

    void toggleStatus(Long id);
}
