package com.strategy.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.TagRuleDTO;
import com.strategy.engine.vo.TagRuleVO;
import com.strategy.engine.vo.TagUsageVO;

import java.util.List;

/**
 * 标签规则 Service
 */
public interface TagRuleService {

    /**
     * 根据引擎ID分页查询标签列表
     */
    Page<TagRuleVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize);

    /**
     * 根据ID查询标签详情
     */
    TagRuleVO getById(Long id);

    /**
     * 创建标签
     */
    Long create(TagRuleDTO dto);

    /**
     * 更新标签
     */
    void update(TagRuleDTO dto);

    /**
     * 删除标签
     */
    void delete(Long id);

    /**
     * 批量删除标签
     */
    void batchDelete(Long engineId);

    /**
     * 根据引擎ID获取所有标签（不分页）
     */
    List<TagRuleVO> listByEngineId(Long engineId);

    /**
     * 根据引擎ID获取所有启用的标签（不分页，用于场景配置选择标签）
     */
    List<TagRuleVO> listEnabledByEngineId(Long engineId);

    /**
     * 切换标签启用/禁用状态
     */
    void toggleStatus(Long id);

    /**
     * 查询标签使用情况
     */
    TagUsageVO getTagUsage(Long tagId);
}
