package com.strategy.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.StrategySceneDTO;
import com.strategy.engine.dto.StrategySceneTagConfigDTO;
import com.strategy.engine.vo.StrategySceneVO;

import java.util.List;

/**
 * 场景策略 Service
 */
public interface StrategySceneService {

    Page<StrategySceneVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize);

    List<StrategySceneVO> listByEngineId(Long engineId);

    StrategySceneVO getById(Long id);

    Long create(StrategySceneDTO dto);

    void update(StrategySceneDTO dto);

    void delete(Long id);

    void batchDeleteByEngineId(Long engineId);

    void configSceneTags(Long sceneId, List<StrategySceneTagConfigDTO> configs);
}
