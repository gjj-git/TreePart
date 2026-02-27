package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.StrategySceneDTO;
import com.strategy.engine.dto.StrategySceneTagItemDTO;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.StrategyScene;
import com.strategy.engine.entity.StrategySceneTag;
import com.strategy.engine.entity.StrategyTagRule;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.StrategySceneMapper;
import com.strategy.engine.mapper.StrategySceneTagMapper;
import com.strategy.engine.mapper.StrategyTagRuleMapper;
import com.strategy.engine.service.StrategySceneService;
import com.strategy.engine.vo.StrategySceneTagVO;
import com.strategy.engine.vo.StrategySceneVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 场景策略 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StrategySceneServiceImpl implements StrategySceneService {

    private final StrategySceneMapper strategySceneMapper;
    private final StrategySceneTagMapper strategySceneTagMapper;
    private final StrategyTagRuleMapper strategyTagRuleMapper;
    private final StrategyEngineMapper strategyEngineMapper;

    @Override
    public Page<StrategySceneVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize) {
        Page<StrategyScene> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StrategyScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyScene::getEngineId, engineId)
                .orderByDesc(StrategyScene::getCreatedTime);
        Page<StrategyScene> resultPage = strategySceneMapper.selectPage(page, wrapper);
        return (Page<StrategySceneVO>) resultPage.convert(scene -> {
            StrategySceneVO vo = BeanUtil.copyProperties(scene, StrategySceneVO.class);
            vo.setTags(getSceneTags(scene.getId()));
            return vo;
        });
    }

    @Override
    public List<StrategySceneVO> listByEngineId(Long engineId) {
        LambdaQueryWrapper<StrategyScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyScene::getEngineId, engineId)
                .orderByDesc(StrategyScene::getCreatedTime);
        return strategySceneMapper.selectList(wrapper).stream().map(scene -> {
            StrategySceneVO vo = BeanUtil.copyProperties(scene, StrategySceneVO.class);
            vo.setTags(getSceneTags(scene.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public StrategySceneVO getById(Long id) {
        StrategyScene scene = strategySceneMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }
        StrategySceneVO vo = BeanUtil.copyProperties(scene, StrategySceneVO.class);
        vo.setTags(getSceneTags(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StrategySceneDTO dto) {
        StrategyScene scene = BeanUtil.copyProperties(dto, StrategyScene.class);
        strategySceneMapper.insert(scene);
        saveTagRelations(scene.getId(), dto.getTags());
        updateEngineSceneCount(dto.getEngineId());
        return scene.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StrategySceneDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("场景ID不能为空");
        }
        StrategyScene scene = strategySceneMapper.selectById(dto.getId());
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }
        BeanUtil.copyProperties(dto, scene, "id", "engineId", "createdTime", "updatedTime", "deleted");
        strategySceneMapper.updateById(scene);
        if (dto.getTags() != null) {
            saveTagRelations(dto.getId(), dto.getTags());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StrategyScene scene = strategySceneMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }
        LambdaQueryWrapper<StrategySceneTag> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(StrategySceneTag::getSceneId, id);
        strategySceneTagMapper.delete(relationWrapper);
        strategySceneMapper.deleteById(id);
        updateEngineSceneCount(scene.getEngineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteByEngineId(Long engineId) {
        LambdaQueryWrapper<StrategyScene> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(StrategyScene::getEngineId, engineId);
        List<StrategyScene> scenes = strategySceneMapper.selectList(sceneWrapper);
        if (!scenes.isEmpty()) {
            List<Long> sceneIds = scenes.stream().map(StrategyScene::getId).collect(Collectors.toList());
            LambdaQueryWrapper<StrategySceneTag> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.in(StrategySceneTag::getSceneId, sceneIds);
            strategySceneTagMapper.delete(relationWrapper);
            strategySceneMapper.deleteBatchIds(sceneIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void configSceneTags(Long sceneId, List<StrategySceneTagItemDTO> configs) {
        StrategyScene scene = strategySceneMapper.selectById(sceneId);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }
        saveTagRelations(sceneId, configs);
    }

    private void saveTagRelations(Long sceneId, List<StrategySceneTagItemDTO> tags) {
        LambdaQueryWrapper<StrategySceneTag> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(StrategySceneTag::getSceneId, sceneId);
        strategySceneTagMapper.delete(deleteWrapper);
        if (tags != null && !tags.isEmpty()) {
            validateTagIds(tags);
            List<StrategySceneTag> relations = new ArrayList<>();
            for (StrategySceneTagItemDTO item : tags) {
                StrategySceneTag relation = new StrategySceneTag();
                relation.setSceneId(sceneId);
                relation.setTagId(item.getTagId());
                relation.setWeightCoefficient(item.getWeightCoefficient());
                relations.add(relation);
            }
            strategySceneTagMapper.insertBatch(relations);
        }
    }

    /**
     * 验证标签ID列表中的所有标签是否存在
     */
    private void validateTagIds(List<StrategySceneTagItemDTO> tags) {
        Set<Long> tagIds = tags.stream()
                .map(StrategySceneTagItemDTO::getTagId)
                .collect(Collectors.toSet());
        List<StrategyTagRule> existingTags = strategyTagRuleMapper.selectBatchIds(tagIds);
        if (existingTags.size() != tagIds.size()) {
            Set<Long> existingIds = existingTags.stream()
                    .map(StrategyTagRule::getId)
                    .collect(Collectors.toSet());
            Set<Long> invalidIds = tagIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toSet());
            throw new BusinessException("以下标签ID不存在: " + invalidIds);
        }
    }

    private List<StrategySceneTagVO> getSceneTags(Long sceneId) {
        LambdaQueryWrapper<StrategySceneTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategySceneTag::getSceneId, sceneId);
        List<StrategySceneTag> relations = strategySceneTagMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> tagIds = relations.stream().map(StrategySceneTag::getTagId).collect(Collectors.toList());
        List<StrategyTagRule> tags = strategyTagRuleMapper.selectBatchIds(tagIds);
        Map<Long, StrategyTagRule> tagMap = tags.stream().collect(Collectors.toMap(StrategyTagRule::getId, t -> t));
        return relations.stream().map(relation -> {
            StrategySceneTagVO vo = new StrategySceneTagVO();
            vo.setId(relation.getId());
            vo.setTagId(relation.getTagId());
            StrategyTagRule tag = tagMap.get(relation.getTagId());
            if (tag != null) {
                vo.setTagName(tag.getName());
                vo.setTagDescription(tag.getDescription());
            }
            vo.setWeightCoefficient(relation.getWeightCoefficient());
            return vo;
        }).collect(Collectors.toList());
    }

    private void updateEngineSceneCount(Long engineId) {
        LambdaQueryWrapper<StrategyScene> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyScene::getEngineId, engineId);
        Long count = strategySceneMapper.selectCount(wrapper);
        LambdaUpdateWrapper<StrategyEngine> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StrategyEngine::getId, engineId)
                .set(StrategyEngine::getSceneCount, count.intValue());
        strategyEngineMapper.update(null, updateWrapper);
    }
}
