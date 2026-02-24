package com.strategy.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.SceneStrategyDTO;
import com.strategy.engine.dto.SceneTagConfigDTO;
import com.strategy.engine.vo.SceneStrategyVO;

import java.util.List;

/**
 * 场景策略 Service
 */
public interface SceneStrategyService {

    /**
     * 根据引擎ID分页查询场景列表
     */
    Page<SceneStrategyVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize);

    /**
     * 根据引擎ID获取所有场景（不分页）
     */
    List<SceneStrategyVO> listByEngineId(Long engineId);

    /**
     * 根据ID查询场景详情（包含标签配置）
     */
    SceneStrategyVO getById(Long id);

    /**
     * 创建场景
     */
    Long create(SceneStrategyDTO dto);

    /**
     * 更新场景
     */
    void update(SceneStrategyDTO dto);

    /**
     * 删除场景
     */
    void delete(Long id);

    /**
     * 根据引擎ID批量删除场景及其标签关联（引擎删除时调用）
     */
    void batchDeleteByEngineId(Long engineId);

    /**
     * 切换场景启用/禁用状态
     */
    void toggleStatus(Long id);

    /**
     * 批量配置场景标签关联
     */
    void configSceneTags(Long sceneId, List<SceneTagConfigDTO> configs);
}
