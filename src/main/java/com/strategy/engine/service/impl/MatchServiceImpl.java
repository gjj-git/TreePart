package com.strategy.engine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.strategy.engine.dto.SceneMatchDTO;
import com.strategy.engine.entity.SceneStrategy;
import com.strategy.engine.entity.SceneTagRelation;
import com.strategy.engine.entity.TagRule;
import com.strategy.engine.mapper.SceneStrategyMapper;
import com.strategy.engine.mapper.SceneTagRelationMapper;
import com.strategy.engine.mapper.TagRuleMapper;
import com.strategy.engine.rule.RuleMatchEngine;
import com.strategy.engine.service.MatchService;
import com.strategy.engine.vo.SceneMatchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 场景匹配 ServiceImpl
 */
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final SceneStrategyMapper sceneStrategyMapper;
    private final SceneTagRelationMapper sceneTagRelationMapper;
    private final TagRuleMapper tagRuleMapper;
    private final RuleMatchEngine ruleMatchEngine;

    @Override
    public SceneMatchVO match(SceneMatchDTO dto) {
        Long engineId = dto.getEngineId();
        Map<String, String> dataMap = dto.getData();

        // 1. 查询该引擎下所有启用的场景
        LambdaQueryWrapper<SceneStrategy> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(SceneStrategy::getEngineId, engineId)
                .eq(SceneStrategy::getStatus, 1);
        List<SceneStrategy> scenes = sceneStrategyMapper.selectList(sceneWrapper);

        if (scenes.isEmpty()) {
            SceneMatchVO vo = new SceneMatchVO();
            vo.setMatched(false);
            vo.setMatchedScenes(new ArrayList<>());
            return vo;
        }

        List<SceneMatchVO.MatchedScene> matchedScenes = new ArrayList<>();

        for (SceneStrategy scene : scenes) {
            // 2. 查询该场景下所有启用的标签关联
            LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(SceneTagRelation::getSceneId, scene.getId())
                    .eq(SceneTagRelation::getEnabled, 1);
            List<SceneTagRelation> relations = sceneTagRelationMapper.selectList(relationWrapper);

            if (relations.isEmpty()) {
                // 无启用标签的场景视为不匹配
                continue;
            }

            // 3. 取出所有关联的标签ID，批量查询标签规则
            List<Long> tagIds = relations.stream()
                    .map(SceneTagRelation::getTagId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<TagRule> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.in(TagRule::getId, tagIds)
                    .eq(TagRule::getStatus, 1);
            List<TagRule> tagRules = tagRuleMapper.selectList(tagWrapper);

            if (tagRules.isEmpty()) {
                // 关联的标签均已禁用，视为不匹配
                continue;
            }

            // 4. 所有启用标签规则均满足，场景才算命中（AND 关系）
            boolean allMatch = tagRules.stream()
                    .allMatch(tag -> ruleMatchEngine.match(tag.getRuleConfig(), dataMap));

            if (allMatch) {
                SceneMatchVO.MatchedScene matchedScene = new SceneMatchVO.MatchedScene();
                matchedScene.setSceneId(scene.getId());
                matchedScene.setSceneName(scene.getName());
                matchedScene.setDescription(scene.getDescription());
                matchedScene.setMatchedTagCount(tagRules.size());
                matchedScenes.add(matchedScene);
            }
        }

        SceneMatchVO vo = new SceneMatchVO();
        vo.setMatched(!matchedScenes.isEmpty());
        vo.setMatchedScenes(matchedScenes);
        return vo;
    }
}
