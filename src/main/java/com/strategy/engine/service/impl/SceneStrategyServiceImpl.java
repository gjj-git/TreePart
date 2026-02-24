package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.SceneStrategyDTO;
import com.strategy.engine.dto.SceneTagConfigDTO;
import com.strategy.engine.dto.SceneTagItemDTO;
import com.strategy.engine.entity.SceneStrategy;
import com.strategy.engine.entity.SceneTagRelation;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.TagRule;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.SceneStrategyMapper;
import com.strategy.engine.mapper.SceneTagRelationMapper;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.TagRuleMapper;
import com.strategy.engine.service.SceneStrategyService;
import com.strategy.engine.vo.SceneStrategyVO;
import com.strategy.engine.vo.SceneTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 场景策略 Service 实现
 */
@Service
@RequiredArgsConstructor
public class SceneStrategyServiceImpl implements SceneStrategyService {

    private final SceneStrategyMapper sceneStrategyMapper;
    private final SceneTagRelationMapper sceneTagRelationMapper;
    private final TagRuleMapper tagRuleMapper;
    private final StrategyEngineMapper strategyEngineMapper;

    @Override
    public Page<SceneStrategyVO> pageByEngineId(Long engineId, Integer pageNum, Integer pageSize) {
        Page<SceneStrategy> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SceneStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SceneStrategy::getEngineId, engineId)
                .orderByDesc(SceneStrategy::getCreatedTime);

        Page<SceneStrategy> resultPage = sceneStrategyMapper.selectPage(page, wrapper);

        return (Page<SceneStrategyVO>) resultPage.convert(scene -> {
            SceneStrategyVO vo = BeanUtil.copyProperties(scene, SceneStrategyVO.class);
            vo.setTags(getSceneTags(scene.getId()));
            return vo;
        });
    }

    @Override
    public List<SceneStrategyVO> listByEngineId(Long engineId) {
        LambdaQueryWrapper<SceneStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SceneStrategy::getEngineId, engineId)
                .orderByDesc(SceneStrategy::getCreatedTime);

        List<SceneStrategy> scenes = sceneStrategyMapper.selectList(wrapper);

        return scenes.stream().map(scene -> {
            SceneStrategyVO vo = BeanUtil.copyProperties(scene, SceneStrategyVO.class);
            vo.setTags(getSceneTags(scene.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SceneStrategyVO getById(Long id) {
        SceneStrategy scene = sceneStrategyMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }

        SceneStrategyVO vo = BeanUtil.copyProperties(scene, SceneStrategyVO.class);
        vo.setTags(getSceneTags(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SceneStrategyDTO dto) {
        SceneStrategy scene = BeanUtil.copyProperties(dto, SceneStrategy.class);
        sceneStrategyMapper.insert(scene);

        // 保存标签关联（若传入）
        saveTagRelations(scene.getId(), dto.getTags());

        // 更新引擎的策略总数
        updateEngineSceneCount(dto.getEngineId());

        return scene.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SceneStrategyDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("场景ID不能为空");
        }

        SceneStrategy scene = sceneStrategyMapper.selectById(dto.getId());
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }

        BeanUtil.copyProperties(dto, scene, "id", "engineId", "createdTime", "updatedTime", "deleted");
        sceneStrategyMapper.updateById(scene);

        // 更新标签关联（若传入）
        if (dto.getTags() != null) {
            saveTagRelations(dto.getId(), dto.getTags());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SceneStrategy scene = sceneStrategyMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }

        // 删除场景标签关联（物理删除，实体无 @TableLogic）
        LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(SceneTagRelation::getSceneId, id);
        sceneTagRelationMapper.delete(relationWrapper);

        sceneStrategyMapper.deleteById(id);

        // 更新引擎的策略总数
        updateEngineSceneCount(scene.getEngineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteByEngineId(Long engineId) {
        // 查出该引擎下所有场景ID
        LambdaQueryWrapper<SceneStrategy> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(SceneStrategy::getEngineId, engineId);
        List<SceneStrategy> scenes = sceneStrategyMapper.selectList(sceneWrapper);

        if (!scenes.isEmpty()) {
            List<Long> sceneIds = scenes.stream()
                    .map(SceneStrategy::getId)
                    .collect(Collectors.toList());

            // 删除所有场景的标签关联（物理删除，实体无 @TableLogic）
            LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.in(SceneTagRelation::getSceneId, sceneIds);
            sceneTagRelationMapper.delete(relationWrapper);

            // 删除所有场景
            sceneStrategyMapper.deleteBatchIds(sceneIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        SceneStrategy scene = sceneStrategyMapper.selectById(id);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }

        LambdaUpdateWrapper<SceneStrategy> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SceneStrategy::getId, id)
                .set(SceneStrategy::getStatus, scene.getStatus() == 1 ? 0 : 1);
        sceneStrategyMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void configSceneTags(Long sceneId, List<SceneTagConfigDTO> configs) {
        SceneStrategy scene = sceneStrategyMapper.selectById(sceneId);
        if (scene == null) {
            throw new BusinessException("场景不存在");
        }

        // 先删除现有关联（物理删除，实体无 @TableLogic）
        LambdaQueryWrapper<SceneTagRelation> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SceneTagRelation::getSceneId, sceneId);
        sceneTagRelationMapper.delete(deleteWrapper);

        // 批量插入新关联
        if (!configs.isEmpty()) {
            List<SceneTagRelation> relations = new ArrayList<>();
            for (SceneTagConfigDTO config : configs) {
                SceneTagRelation relation = new SceneTagRelation();
                relation.setSceneId(sceneId);
                relation.setTagId(config.getTagId());
                relation.setEnabled(config.getEnabled());
                relation.setWeightCoefficient(config.getWeightCoefficient());
                relations.add(relation);
            }
            sceneTagRelationMapper.insertBatch(relations);
        }
    }

    /**
     * 保存场景标签关联（全量替换）
     */
    private void saveTagRelations(Long sceneId, List<SceneTagItemDTO> tags) {
        // 删除现有关联
        LambdaQueryWrapper<SceneTagRelation> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SceneTagRelation::getSceneId, sceneId);
        sceneTagRelationMapper.delete(deleteWrapper);

        // 批量插入新关联
        if (tags != null && !tags.isEmpty()) {
            List<SceneTagRelation> relations = new ArrayList<>();
            for (SceneTagItemDTO item : tags) {
                SceneTagRelation relation = new SceneTagRelation();
                relation.setSceneId(sceneId);
                relation.setTagId(item.getTagId());
                relation.setEnabled(item.getEnabled());
                relation.setWeightCoefficient(item.getWeightCoefficient());
                relations.add(relation);
            }
            sceneTagRelationMapper.insertBatch(relations);
        }
    }

    /**
     * 获取场景的标签配置列表
     */
    private List<SceneTagVO> getSceneTags(Long sceneId) {
        LambdaQueryWrapper<SceneTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SceneTagRelation::getSceneId, sceneId);
        List<SceneTagRelation> relations = sceneTagRelationMapper.selectList(wrapper);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询标签信息
        List<Long> tagIds = relations.stream().map(SceneTagRelation::getTagId).collect(Collectors.toList());
        List<TagRule> tags = tagRuleMapper.selectBatchIds(tagIds);
        Map<Long, TagRule> tagMap = tags.stream().collect(Collectors.toMap(TagRule::getId, t -> t));

        return relations.stream().map(relation -> {
            SceneTagVO vo = new SceneTagVO();
            vo.setId(relation.getId());
            vo.setTagId(relation.getTagId());
            TagRule tag = tagMap.get(relation.getTagId());
            if (tag != null) {
                vo.setTagName(tag.getName());
                vo.setTagDescription(tag.getDescription());
            }
            vo.setEnabled(relation.getEnabled());
            vo.setWeightCoefficient(relation.getWeightCoefficient());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 更新引擎的策略总数
     */
    private void updateEngineSceneCount(Long engineId) {
        LambdaQueryWrapper<SceneStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SceneStrategy::getEngineId, engineId);
        Long count = sceneStrategyMapper.selectCount(wrapper);

        LambdaUpdateWrapper<StrategyEngine> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StrategyEngine::getId, engineId)
                .set(StrategyEngine::getSceneCount, count.intValue());

        strategyEngineMapper.update(null, updateWrapper);
    }
}
