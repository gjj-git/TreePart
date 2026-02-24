package com.strategy.engine.service;

import com.strategy.engine.dto.EngineFullConfigDTO;
import com.strategy.engine.vo.EngineFullConfigVO;

/**
 * 【备用方案 - 当前未使用】
 * 引擎完整配置 Service（统一保存方案）
 *
 * <p>提供统一保存功能，一次性保存引擎的所有配置。</p>
 * <p><strong>当前系统采用分步保存方案，此 Service 暂未使用。</strong></p>
 *
 * @see com.strategy.engine.service.StrategyEngineService 引擎管理（分步保存）
 * @see com.strategy.engine.service.TagRuleService 标签管理（分步保存）
 * @see com.strategy.engine.service.SceneStrategyService 场景管理（分步保存）
 */
public interface EngineFullConfigService {

    /**
     * 获取引擎的完整配置（包含三个Tab的所有数据）
     * @param engineId 引擎ID
     * @return 完整配置数据
     */
    EngineFullConfigVO getFullConfig(Long engineId);

    /**
     * 保存引擎的完整配置（创建或更新）
     * 事务性操作：要么全部成功，要么全部失败
     *
     * @param dto 完整配置数据
     * @return 引擎ID
     */
    Long saveFullConfig(EngineFullConfigDTO dto);

    /**
     * 验证配置的有效性（保存前调用）
     * @param dto 完整配置数据
     * @return 验证结果消息
     */
    String validateConfig(EngineFullConfigDTO dto);
}
