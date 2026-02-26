package com.strategy.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.StrategyTagRuleDTO;
import com.strategy.engine.vo.StrategyTagRuleVO;
import com.strategy.engine.vo.TagUsageVO;

import java.util.List;

/**
 * 标签规则 Service
 */
public interface StrategyTagRuleService {

    Page<StrategyTagRuleVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize);

    StrategyTagRuleVO getById(Long id);

    Long create(StrategyTagRuleDTO dto);

    void update(StrategyTagRuleDTO dto);

    void delete(Long id);

    void batchDelete(Long engineId);

    List<StrategyTagRuleVO> listByEngineId(Long engineId);

    List<StrategyTagRuleVO> listEnabledByEngineId(Long engineId);

    void toggleStatus(Long id);

    TagUsageVO getTagUsage(Long tagId);
}
