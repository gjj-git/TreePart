package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.dto.StrategyEngineDTO;
import com.strategy.engine.dto.StrategyEngineQueryDTO;
import com.strategy.engine.entity.SceneStrategy;
import com.strategy.engine.entity.SceneTagRelation;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.TagRule;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.SceneStrategyMapper;
import com.strategy.engine.mapper.SceneTagRelationMapper;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.TagRuleMapper;
import com.strategy.engine.service.StrategyEngineService;
import com.strategy.engine.vo.StrategyEngineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 策略引擎 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StrategyEngineServiceImpl implements StrategyEngineService {

    private final StrategyEngineMapper strategyEngineMapper;
    private final TagRuleMapper tagRuleMapper;
    private final SceneStrategyMapper sceneStrategyMapper;
    private final SceneTagRelationMapper sceneTagRelationMapper;

    @Override
    public Page<StrategyEngineVO> pageQuery(StrategyEngineQueryDTO queryDTO) {
        Page<StrategyEngine> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<StrategyEngine> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getName()), StrategyEngine::getName, queryDTO.getName())
                .eq(StringUtils.hasText(queryDTO.getType()), StrategyEngine::getType, queryDTO.getType())
                .eq(StringUtils.hasText(queryDTO.getApplicableObject()), StrategyEngine::getApplicableObject, queryDTO.getApplicableObject())
                .eq(queryDTO.getStatus() != null, StrategyEngine::getStatus, queryDTO.getStatus())
                .orderByDesc(StrategyEngine::getUpdatedTime);

        Page<StrategyEngine> resultPage = strategyEngineMapper.selectPage(page, wrapper);

        return (Page<StrategyEngineVO>) resultPage.convert(engine -> BeanUtil.copyProperties(engine, StrategyEngineVO.class));
    }

    @Override
    public StrategyEngineVO getById(Long id) {
        StrategyEngine engine = strategyEngineMapper.selectById(id);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }
        return BeanUtil.copyProperties(engine, StrategyEngineVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StrategyEngineDTO dto) {
        StrategyEngine engine = BeanUtil.copyProperties(dto, StrategyEngine.class);
        engine.setTagCount(0);
        engine.setSceneCount(0);
        engine.setIsDefault(0);

        strategyEngineMapper.insert(engine);
        return engine.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(StrategyEngineDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("引擎ID不能为空");
        }

        StrategyEngine engine = strategyEngineMapper.selectById(dto.getId());
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        BeanUtil.copyProperties(dto, engine, "id", "tagCount", "sceneCount", "isDefault", "createdTime", "updatedTime", "deleted");
        strategyEngineMapper.updateById(engine);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StrategyEngine engine = strategyEngineMapper.selectById(id);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        // 1. 查出该引擎下所有场景ID
        LambdaQueryWrapper<SceneStrategy> sceneWrapper = new LambdaQueryWrapper<>();
        sceneWrapper.eq(SceneStrategy::getEngineId, id);
        List<SceneStrategy> scenes = sceneStrategyMapper.selectList(sceneWrapper);

        if (!scenes.isEmpty()) {
            List<Long> sceneIds = scenes.stream()
                    .map(SceneStrategy::getId)
                    .collect(Collectors.toList());

            // 2. 删除所有场景的标签关联（物理删除，实体无 @TableLogic）
            LambdaQueryWrapper<SceneTagRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.in(SceneTagRelation::getSceneId, sceneIds);
            sceneTagRelationMapper.delete(relationWrapper);

            // 3. 删除所有场景
            sceneStrategyMapper.deleteBatchIds(sceneIds);
        }

        // 4. 删除所有标签
        LambdaQueryWrapper<TagRule> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(TagRule::getEngineId, id);
        tagRuleMapper.delete(tagWrapper);

        // 5. 删除引擎
        strategyEngineMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        StrategyEngine engine = strategyEngineMapper.selectById(id);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        LambdaUpdateWrapper<StrategyEngine> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StrategyEngine::getId, id)
                .set(StrategyEngine::getStatus, engine.getStatus() == 1 ? 0 : 1);

        strategyEngineMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        StrategyEngine engine = strategyEngineMapper.selectById(id);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        // 先取消所有默认引擎
        LambdaUpdateWrapper<StrategyEngine> cancelWrapper = new LambdaUpdateWrapper<>();
        cancelWrapper.set(StrategyEngine::getIsDefault, 0);
        strategyEngineMapper.update(null, cancelWrapper);

        // 设置当前引擎为默认
        LambdaUpdateWrapper<StrategyEngine> setWrapper = new LambdaUpdateWrapper<>();
        setWrapper.eq(StrategyEngine::getId, id)
                .set(StrategyEngine::getIsDefault, 1);
        strategyEngineMapper.update(null, setWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDefault(Long id) {
        StrategyEngine engine = strategyEngineMapper.selectById(id);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        LambdaUpdateWrapper<StrategyEngine> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StrategyEngine::getId, id)
                .set(StrategyEngine::getIsDefault, 0);

        strategyEngineMapper.update(null, wrapper);
    }
}
