package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.StrategyTagRuleDTO;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.StrategySceneTag;
import com.strategy.engine.entity.StrategyTagRule;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.StrategySceneTagMapper;
import com.strategy.engine.mapper.StrategyTagRuleMapper;
import com.strategy.engine.rule.RuleToSqlTranslator;
import com.strategy.engine.service.StrategyTagRuleService;
import com.strategy.engine.vo.StrategyTagRuleVO;
import com.strategy.engine.vo.TagUsageVO;
import com.strategy.engine.entity.StrategyScene;
import com.strategy.engine.mapper.StrategySceneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签规则 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StrategyTagRuleServiceImpl implements StrategyTagRuleService {

    private final StrategyTagRuleMapper strategyTagRuleMapper;
    private final StrategyEngineMapper strategyEngineMapper;
    private final StrategySceneTagMapper strategySceneTagMapper;
    private final StrategySceneMapper strategySceneMapper;

    @Override
    public Page<StrategyTagRuleVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize, String name, Integer status) {
        Page<StrategyTagRule> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<StrategyTagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyTagRule::getEngineId, engineId);
        if (name != null && !name.isBlank()) {
            wrapper.like(StrategyTagRule::getName, name);
        }
        if (status != null) {
            wrapper.eq(StrategyTagRule::getStatus, status);
        }
        wrapper.orderByDesc(StrategyTagRule::getCreatedTime);

        Page<StrategyTagRule> resultPage = strategyTagRuleMapper.selectPage(page, wrapper);
        return (Page<StrategyTagRuleVO>) resultPage.convert(tag -> BeanUtil.copyProperties(tag, StrategyTagRuleVO.class));
    }

    @Override
    public StrategyTagRuleVO getById(Long id) {
        StrategyTagRule tag = strategyTagRuleMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return BeanUtil.copyProperties(tag, StrategyTagRuleVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StrategyTagRuleDTO dto) {
        StrategyTagRule tag = BeanUtil.copyProperties(dto, StrategyTagRule.class);
        if (tag.getStatus() == null) {
            tag.setStatus(1);
        }
        tag.setRuleSql(RuleToSqlTranslator.translate(dto.getRuleConfig()));
        strategyTagRuleMapper.insert(tag);
        updateEngineTagCount(dto.getEngineId());
        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StrategyTagRuleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("标签ID不能为空");
        }
        StrategyTagRule tag = strategyTagRuleMapper.selectById(dto.getId());
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        BeanUtil.copyProperties(dto, tag, "id", "createdTime", "updatedTime", "deleted");
        tag.setRuleSql(RuleToSqlTranslator.translate(dto.getRuleConfig()));
        strategyTagRuleMapper.updateById(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StrategyTagRule tag = strategyTagRuleMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        LambdaQueryWrapper<StrategySceneTag> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(StrategySceneTag::getTagId, id);
        Long usageCount = strategySceneTagMapper.selectCount(relationWrapper);
        if (usageCount > 0) {
            throw new BusinessException("该标签正在被 " + usageCount + " 个场景使用，请先移除场景中的标签配置后再删除");
        }

        strategyTagRuleMapper.deleteById(id);
        updateEngineTagCount(tag.getEngineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long engineId) {
        LambdaQueryWrapper<StrategyTagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyTagRule::getEngineId, engineId);
        strategyTagRuleMapper.delete(wrapper);
        updateEngineTagCount(engineId);
    }

    @Override
    public List<StrategyTagRuleVO> listByEngineId(Long engineId) {
        LambdaQueryWrapper<StrategyTagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyTagRule::getEngineId, engineId)
                .orderByDesc(StrategyTagRule::getCreatedTime);
        return strategyTagRuleMapper.selectList(wrapper).stream()
                .map(tag -> BeanUtil.copyProperties(tag, StrategyTagRuleVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyTagRuleVO> listEnabledByEngineId(Long engineId) {
        LambdaQueryWrapper<StrategyTagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyTagRule::getEngineId, engineId)
                .eq(StrategyTagRule::getStatus, 1)
                .orderByDesc(StrategyTagRule::getCreatedTime);
        return strategyTagRuleMapper.selectList(wrapper).stream()
                .map(tag -> BeanUtil.copyProperties(tag, StrategyTagRuleVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        StrategyTagRule tag = strategyTagRuleMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        if (tag.getStatus() == 1) {
            LambdaQueryWrapper<StrategySceneTag> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(StrategySceneTag::getTagId, id);
            Long usageCount = strategySceneTagMapper.selectCount(relationWrapper);
            if (usageCount > 0) {
                throw new BusinessException("该标签已被 " + usageCount + " 个场景引用，请先移除关联后再禁用");
            }
        }

        LambdaUpdateWrapper<StrategyTagRule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StrategyTagRule::getId, id)
                .set(StrategyTagRule::getStatus, tag.getStatus() == 1 ? 0 : 1);
        strategyTagRuleMapper.update(null, updateWrapper);
    }

    @Override
    public TagUsageVO getTagUsage(Long tagId) {
        TagUsageVO vo = new TagUsageVO();
        vo.setTagId(tagId);

        LambdaQueryWrapper<StrategySceneTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategySceneTag::getTagId, tagId);
        List<StrategySceneTag> relations = strategySceneTagMapper.selectList(wrapper);

        vo.setSceneCount((long) relations.size());

        if (!relations.isEmpty()) {
            List<Long> sceneIds = relations.stream()
                    .map(StrategySceneTag::getSceneId)
                    .collect(Collectors.toList());

            List<StrategyScene> scenes = strategySceneMapper.selectBatchIds(sceneIds);
            List<TagUsageVO.SceneInfo> sceneInfos = scenes.stream()
                    .map(scene -> {
                        TagUsageVO.SceneInfo info = new TagUsageVO.SceneInfo();
                        info.setSceneId(scene.getId());
                        info.setSceneName(scene.getName());
                        return info;
                    })
                    .collect(Collectors.toList());
            vo.setScenes(sceneInfos);
        }

        return vo;
    }

    private void updateEngineTagCount(Long engineId) {
        LambdaQueryWrapper<StrategyTagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyTagRule::getEngineId, engineId);
        Long count = strategyTagRuleMapper.selectCount(wrapper);

        LambdaUpdateWrapper<StrategyEngine> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StrategyEngine::getId, engineId)
                .set(StrategyEngine::getTagCount, count.intValue());
        strategyEngineMapper.update(null, updateWrapper);
    }
}
