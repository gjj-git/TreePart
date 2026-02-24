package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.strategy.engine.dto.EngineFullConfigDTO;
import com.strategy.engine.entity.SceneStrategy;
import com.strategy.engine.entity.SceneTagRelation;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.TagRule;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.SceneStrategyMapper;
import com.strategy.engine.mapper.SceneTagRelationMapper;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.TagRuleMapper;
import com.strategy.engine.service.EngineFullConfigService;
import com.strategy.engine.vo.EngineFullConfigVO;
import com.strategy.engine.vo.SceneTagVO;
import com.strategy.engine.vo.TagRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引擎完整配置 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EngineFullConfigServiceImpl implements EngineFullConfigService {

    private final StrategyEngineMapper engineMapper;
    private final TagRuleMapper tagRuleMapper;
    private final SceneStrategyMapper sceneStrategyMapper;
    private final SceneTagRelationMapper sceneTagRelationMapper;

    @Override
    public EngineFullConfigVO getFullConfig(Long engineId) {
        // 1. 查询引擎基本信息
        StrategyEngine engine = engineMapper.selectById(engineId);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        EngineFullConfigVO vo = new EngineFullConfigVO();
        vo.setEngineId(engineId);

        // 2. 填充基本信息
        EngineFullConfigVO.BasicInfo basicInfo = BeanUtil.copyProperties(engine, EngineFullConfigVO.BasicInfo.class);
        vo.setBasicInfo(basicInfo);

        // 3. 查询标签列表
        LambdaQueryWrapper<TagRule> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(TagRule::getEngineId, engineId).orderByDesc(TagRule::getCreatedTime);
        List<TagRule> tags = tagRuleMapper.selectList(tagWrapper);
        vo.setTags(tags.stream().map(tag -> BeanUtil.copyProperties(tag, TagRuleVO.class)).collect(Collectors.toList()));

        // 4. 查询场景列表及其标签配置
        LambdaQueryWrapper<SceneStrategy> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(SceneStrategy::getEngineId, engineId).orderByDesc(SceneStrategy::getCreatedTime);
        List<SceneStrategy> scenes = sceneStrategyMapper.selectList(sceneWrapper);

        List<EngineFullConfigVO.SceneWithTagsVO> sceneVOs = new ArrayList<>();
        for (SceneStrategy scene : scenes) {
            EngineFullConfigVO.SceneWithTagsVO sceneVO = BeanUtil.copyProperties(scene, EngineFullConfigVO.SceneWithTagsVO.class);

            // 查询场景的标签配置
            LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(SceneTagRelation::getSceneId, scene.getId());
            List<SceneTagRelation> relations = sceneTagRelationMapper.selectList(relationWrapper);

            // 获取标签名称映射
            if (!relations.isEmpty()) {
                List<Long> tagIds = relations.stream().map(SceneTagRelation::getTagId).collect(Collectors.toList());
                List<TagRule> sceneTags = tagRuleMapper.selectBatchIds(tagIds);
                Map<Long, String> tagNameMap = sceneTags.stream().collect(Collectors.toMap(TagRule::getId, TagRule::getName));

                List<SceneTagVO> tagConfigs = relations.stream().map(relation -> {
                    SceneTagVO tagVO = new SceneTagVO();
                    tagVO.setId(relation.getId());
                    tagVO.setTagId(relation.getTagId());
                    tagVO.setTagName(tagNameMap.get(relation.getTagId()));
                    tagVO.setEnabled(relation.getEnabled());
                    tagVO.setWeightCoefficient(relation.getWeightCoefficient());
                    return tagVO;
                }).collect(Collectors.toList());

                sceneVO.setTagConfigs(tagConfigs);
            } else {
                sceneVO.setTagConfigs(new ArrayList<>());
            }

            sceneVOs.add(sceneVO);
        }
        vo.setScenes(sceneVOs);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFullConfig(EngineFullConfigDTO dto) {
        // 验证配置
        String errorMsg = validateConfig(dto);
        if (StrUtil.isNotBlank(errorMsg)) {
            throw new BusinessException(errorMsg);
        }

        Long engineId = dto.getEngineId();
        boolean isUpdate = engineId != null;

        // 1. 保存引擎基本信息
        StrategyEngine engine;
        if (isUpdate) {
            engine = engineMapper.selectById(engineId);
            if (engine == null) {
                throw new BusinessException("引擎不存在");
            }
            BeanUtil.copyProperties(dto.getBasicInfo(), engine, "id", "createdTime", "updatedTime", "deleted");
            engineMapper.updateById(engine);
        } else {
            engine = BeanUtil.copyProperties(dto.getBasicInfo(), StrategyEngine.class);
            engineMapper.insert(engine);
            engineId = engine.getId();
        }

        // 2. 保存标签规则（建立临时ID到真实ID的映射）
        Map<String, Long> tempIdToRealIdMap = new HashMap<>();
        if (dto.getTags() != null) {
            for (EngineFullConfigDTO.TagRuleItem tagItem : dto.getTags()) {
                if ("delete".equals(tagItem.getAction()) && tagItem.getId() != null) {
                    // 删除标签
                    tagRuleMapper.deleteById(tagItem.getId());
                    // 同时删除相关的场景标签关联
                    LambdaQueryWrapper<SceneTagRelation> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(SceneTagRelation::getTagId, tagItem.getId());
                    sceneTagRelationMapper.delete(deleteWrapper);
                } else if ("update".equals(tagItem.getAction()) && tagItem.getId() != null) {
                    // 更新已有标签
                    TagRule tag = tagRuleMapper.selectById(tagItem.getId());
                    if (tag != null) {
                        BeanUtil.copyProperties(tagItem, tag, "id", "engineId", "createdTime", "updatedTime", "deleted");
                        tagRuleMapper.updateById(tag);
                        if (StrUtil.isNotBlank(tagItem.getTempId())) {
                            tempIdToRealIdMap.put(tagItem.getTempId(), tag.getId());
                        }
                    }
                } else if ("add".equals(tagItem.getAction())) {
                    // 新增标签
                    TagRule newTag = BeanUtil.copyProperties(tagItem, TagRule.class);
                    newTag.setEngineId(engineId);
                    newTag.setId(null);
                    tagRuleMapper.insert(newTag);
                    if (StrUtil.isNotBlank(tagItem.getTempId())) {
                        tempIdToRealIdMap.put(tagItem.getTempId(), newTag.getId());
                    }
                }
            }
        }

        // 3. 保存场景策略
        if (dto.getScenes() != null) {
            for (EngineFullConfigDTO.SceneStrategyItem sceneItem : dto.getScenes()) {
                Long sceneId;

                if ("delete".equals(sceneItem.getAction()) && sceneItem.getId() != null) {
                    // 删除场景
                    sceneStrategyMapper.deleteById(sceneItem.getId());
                    // 删除场景的标签关联
                    LambdaQueryWrapper<SceneTagRelation> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(SceneTagRelation::getSceneId, sceneItem.getId());
                    sceneTagRelationMapper.delete(deleteWrapper);
                    continue;
                } else if ("update".equals(sceneItem.getAction()) && sceneItem.getId() != null) {
                    // 更新已有场景
                    SceneStrategy scene = sceneStrategyMapper.selectById(sceneItem.getId());
                    if (scene == null) {
                        throw new BusinessException("场景不存在: " + sceneItem.getId());
                    }
                    BeanUtil.copyProperties(sceneItem, scene, "id", "engineId", "createdTime", "updatedTime", "deleted");
                    sceneStrategyMapper.updateById(scene);
                    sceneId = scene.getId();
                } else if ("add".equals(sceneItem.getAction())) {
                    // 新增场景
                    SceneStrategy newScene = BeanUtil.copyProperties(sceneItem, SceneStrategy.class);
                    newScene.setEngineId(engineId);
                    newScene.setId(null);
                    sceneStrategyMapper.insert(newScene);
                    sceneId = newScene.getId();
                } else {
                    continue;
                }

                // 保存场景的标签配置
                if (sceneItem.getTagConfigs() != null) {
                    // 先删除该场景的所有标签关联
                    LambdaQueryWrapper<SceneTagRelation> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(SceneTagRelation::getSceneId, sceneId);
                    sceneTagRelationMapper.delete(deleteWrapper);

                    // 重新插入标签关联
                    for (EngineFullConfigDTO.SceneTagConfig tagConfig : sceneItem.getTagConfigs()) {
                        Long tagId;
                        if (tagConfig.getTagId() != null) {
                            // 使用已有标签的ID
                            tagId = tagConfig.getTagId();
                        } else if (StrUtil.isNotBlank(tagConfig.getTagTempId())) {
                            // 使用新增标签的临时ID，从映射表中获取真实ID
                            tagId = tempIdToRealIdMap.get(tagConfig.getTagTempId());
                            if (tagId == null) {
                                throw new BusinessException("标签临时ID无效: " + tagConfig.getTagTempId());
                            }
                        } else {
                            throw new BusinessException("标签配置缺少tagId或tagTempId");
                        }

                        SceneTagRelation relation = new SceneTagRelation();
                        relation.setSceneId(sceneId);
                        relation.setTagId(tagId);
                        relation.setEnabled(tagConfig.getEnabled());
                        relation.setWeightCoefficient(tagConfig.getWeightCoefficient());
                        sceneTagRelationMapper.insert(relation);
                    }
                }
            }
        }

        // 4. 更新引擎的统计字段
        updateEngineStatistics(engineId);

        return engineId;
    }

    @Override
    public String validateConfig(EngineFullConfigDTO dto) {
        if (dto.getBasicInfo() == null) {
            return "基本信息不能为空";
        }

        // 验证标签
        if (dto.getTags() != null) {
            for (EngineFullConfigDTO.TagRuleItem tag : dto.getTags()) {
                if (!"delete".equals(tag.getAction()) && StrUtil.isBlank(tag.getName())) {
                    return "标签名称不能为空";
                }
            }
        }

        // 验证场景
        if (dto.getScenes() != null) {
            for (EngineFullConfigDTO.SceneStrategyItem scene : dto.getScenes()) {
                if (!"delete".equals(scene.getAction()) && StrUtil.isBlank(scene.getName())) {
                    return "场景名称不能为空";
                }

                // 验证场景的标签配置
                if (scene.getTagConfigs() != null) {
                    for (EngineFullConfigDTO.SceneTagConfig tagConfig : scene.getTagConfigs()) {
                        if (tagConfig.getWeightCoefficient() == null || tagConfig.getWeightCoefficient() < 1 || tagConfig.getWeightCoefficient() > 10) {
                            return "权重系数必须在1-10之间";
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 更新引擎的统计字段
     */
    private void updateEngineStatistics(Long engineId) {
        // 统计标签数量
        LambdaQueryWrapper<TagRule> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(TagRule::getEngineId, engineId);
        Long tagCount = tagRuleMapper.selectCount(tagWrapper);

        // 统计场景数量
        LambdaQueryWrapper<SceneStrategy> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(SceneStrategy::getEngineId, engineId);
        Long sceneCount = sceneStrategyMapper.selectCount(sceneWrapper);

        // 更新引擎
        StrategyEngine engine = new StrategyEngine();
        engine.setId(engineId);
        engine.setTagCount(tagCount.intValue());
        engine.setSceneCount(sceneCount.intValue());
        engineMapper.updateById(engine);
    }
}
