package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.TagRuleDTO;
import com.strategy.engine.entity.SceneStrategy;
import com.strategy.engine.entity.SceneTagRelation;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.TagRule;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.SceneStrategyMapper;
import com.strategy.engine.mapper.SceneTagRelationMapper;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.TagRuleMapper;
import com.strategy.engine.service.TagRuleService;
import com.strategy.engine.vo.TagRuleVO;
import com.strategy.engine.vo.TagUsageVO;
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
public class TagRuleServiceImpl implements TagRuleService {

    private final TagRuleMapper tagRuleMapper;
    private final StrategyEngineMapper strategyEngineMapper;
    private final SceneTagRelationMapper sceneTagRelationMapper;
    private final SceneStrategyMapper sceneStrategyMapper;

    @Override
    public Page<TagRuleVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize) {
        Page<TagRule> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagRule::getEngineId, engineId)
                .orderByDesc(TagRule::getCreatedTime);

        Page<TagRule> resultPage = tagRuleMapper.selectPage(page, wrapper);

        return (Page<TagRuleVO>) resultPage.convert(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class));
    }

    @Override
    public TagRuleVO getById(Long id) {
        TagRule tag = tagRuleMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return BeanUtil.copyProperties(tag, TagRuleVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TagRuleDTO dto) {
        TagRule tag = BeanUtil.copyProperties(dto, TagRule.class);
        tagRuleMapper.insert(tag);

        // 更新引擎的标签总数
        updateEngineTagCount(dto.getEngineId());

        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TagRuleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("标签ID不能为空");
        }

        TagRule tag = tagRuleMapper.selectById(dto.getId());
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        BeanUtil.copyProperties(dto, tag, "id", "createdTime", "updatedTime", "deleted");
        tagRuleMapper.updateById(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TagRule tag = tagRuleMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        // 检查标签是否被场景使用
        LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(SceneTagRelation::getTagId, id);
        Long usageCount = sceneTagRelationMapper.selectCount(relationWrapper);

        if (usageCount > 0) {
            throw new BusinessException("该标签正在被 " + usageCount + " 个场景使用，请先移除场景中的标签配置后再删除");
        }

        // 未被使用，可以删除
        tagRuleMapper.deleteById(id);

        // 更新引擎的标签总数
        updateEngineTagCount(tag.getEngineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long engineId) {
        LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagRule::getEngineId, engineId);

        tagRuleMapper.delete(wrapper);

        // 更新引擎的标签总数
        updateEngineTagCount(engineId);
    }

    @Override
    public List<TagRuleVO> listByEngineId(Long engineId) {
        LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagRule::getEngineId, engineId)
                .orderByDesc(TagRule::getCreatedTime);

        List<TagRule> tags = tagRuleMapper.selectList(wrapper);
        return tags.stream()
                .map(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<TagRuleVO> listEnabledByEngineId(Long engineId) {
        LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagRule::getEngineId, engineId)
                .eq(TagRule::getStatus, 1)
                .orderByDesc(TagRule::getCreatedTime);

        List<TagRule> tags = tagRuleMapper.selectList(wrapper);
        return tags.stream()
                .map(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        TagRule tag = tagRuleMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        // 启用 → 禁用时，检查是否有场景正在引用该标签
        if (tag.getStatus() == 1) {
            LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(SceneTagRelation::getTagId, id);
            Long usageCount = sceneTagRelationMapper.selectCount(relationWrapper);
            if (usageCount > 0) {
                throw new BusinessException("该标签已被 " + usageCount + " 个场景引用，禁用后这些场景的匹配规则将不完整，请先移除场景中的标签配置后再禁用");
            }
        }

        LambdaUpdateWrapper<TagRule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TagRule::getId, id)
                .set(TagRule::getStatus, tag.getStatus() == 1 ? 0 : 1);
        tagRuleMapper.update(null, updateWrapper);
    }

    @Override
    public TagUsageVO getTagUsage(Long tagId) {
        TagUsageVO vo = new TagUsageVO();
        vo.setTagId(tagId);

        // 查询使用该标签的场景标签关联
        LambdaQueryWrapper<SceneTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SceneTagRelation::getTagId, tagId);
        List<SceneTagRelation> relations = sceneTagRelationMapper.selectList(wrapper);

        vo.setSceneCount((long) relations.size());

        // 查询场景详细信息
        if (!relations.isEmpty()) {
            List<Long> sceneIds = relations.stream()
                    .map(SceneTagRelation::getSceneId)
                    .collect(Collectors.toList());

            List<SceneStrategy> scenes = sceneStrategyMapper.selectBatchIds(sceneIds);
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

    /**
     * 更新引擎的标签总数
     */
    private void updateEngineTagCount(Long engineId) {
        LambdaQueryWrapper<TagRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagRule::getEngineId, engineId);
        Long count = tagRuleMapper.selectCount(wrapper);

        LambdaUpdateWrapper<StrategyEngine> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StrategyEngine::getId, engineId)
                .set(StrategyEngine::getTagCount, count.intValue());

        strategyEngineMapper.update(null, updateWrapper);
    }
}
