package com.strategy.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.StrategyEngineDTO;
import com.strategy.engine.dto.StrategyEngineQueryDTO;
import com.strategy.engine.vo.StrategyEngineVO;

/**
 * 策略引擎 Service
 */
public interface StrategyEngineService {

    /**
     * 分页查询引擎列表
     */
    Page<StrategyEngineVO> pageQuery(StrategyEngineQueryDTO queryDTO);

    /**
     * 根据ID查询引擎详情
     */
    StrategyEngineVO getById(Long id);

    /**
     * 创建引擎
     */
    Long create(StrategyEngineDTO dto);

    /**
     * 更新引擎
     */
    void update(StrategyEngineDTO dto);

    /**
     * 删除引擎
     */
    void delete(Long id);

    /**
     * 切换引擎状态
     */
    void toggleStatus(Long id);

    /**
     * 设置为默认引擎
     */
    void setDefault(Long id);

    /**
     * 取消默认引擎
     */
    void cancelDefault(Long id);
}
