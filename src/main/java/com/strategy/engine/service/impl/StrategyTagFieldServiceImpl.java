package com.strategy.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.strategy.engine.dto.StrategyTagFieldDTO;
import com.strategy.engine.entity.StrategyEngine;
import com.strategy.engine.entity.StrategyTagField;
import com.strategy.engine.exception.BusinessException;
import com.strategy.engine.mapper.StrategyEngineMapper;
import com.strategy.engine.mapper.StrategyTagFieldMapper;
import com.strategy.engine.service.StrategyTagFieldService;
import com.strategy.engine.vo.StrategyTagFieldGroupVO;
import com.strategy.engine.vo.StrategyTagFieldVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 条件字段元数据 Service 实现
 */
@Service
@RequiredArgsConstructor
public class StrategyTagFieldServiceImpl implements StrategyTagFieldService {

    private final StrategyTagFieldMapper strategyTagFieldMapper;
    private final StrategyEngineMapper strategyEngineMapper;

    private static final Map<String, String> CATEGORY_NAME_MAP = Map.of(
            "INHERENT",      "固有属性",
            "EXAM",          "考试属性",
            "COMPREHENSIVE", "综合属性"
    );

    private static final List<String> CATEGORY_ORDER = List.of("INHERENT", "EXAM", "COMPREHENSIVE");

    @Override
    public List<StrategyTagFieldGroupVO> listGroupedByEngineId(Long engineId) {
        StrategyEngine engine = strategyEngineMapper.selectById(engineId);
        if (engine == null) {
            throw new BusinessException("引擎不存在");
        }

        List<StrategyTagField> fields = strategyTagFieldMapper.listByApplicableObject(engine.getApplicableObject());

        Map<String, List<StrategyTagFieldVO>> grouped = new LinkedHashMap<>();
        CATEGORY_ORDER.forEach(cat -> grouped.put(cat, new ArrayList<>()));

        for (StrategyTagField field : fields) {
            StrategyTagFieldVO vo = BeanUtil.copyProperties(field, StrategyTagFieldVO.class);
            grouped.computeIfAbsent(field.getCategory(), k -> new ArrayList<>()).add(vo);
        }

        return grouped.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> {
                    StrategyTagFieldGroupVO group = new StrategyTagFieldGroupVO();
                    group.setCategory(entry.getKey());
                    group.setCategoryName(CATEGORY_NAME_MAP.getOrDefault(entry.getKey(), entry.getKey()));
                    group.setFields(entry.getValue());
                    return group;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyTagFieldVO> listAll() {
        LambdaQueryWrapper<StrategyTagField> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(StrategyTagField::getCategory)
               .orderByAsc(StrategyTagField::getSort);
        return strategyTagFieldMapper.selectList(wrapper).stream()
                .map(f -> BeanUtil.copyProperties(f, StrategyTagFieldVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public StrategyTagFieldVO getById(Long id) {
        StrategyTagField field = strategyTagFieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException("字段不存在");
        }
        return BeanUtil.copyProperties(field, StrategyTagFieldVO.class);
    }

    @Override
    public Long create(StrategyTagFieldDTO dto) {
        LambdaQueryWrapper<StrategyTagField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyTagField::getFieldKey, dto.getFieldKey());
        if (strategyTagFieldMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("字段标识 " + dto.getFieldKey() + " 已存在");
        }

        StrategyTagField field = BeanUtil.copyProperties(dto, StrategyTagField.class);
        if (field.getSort() == null) {
            field.setSort(0);
        }
        strategyTagFieldMapper.insert(field);
        return field.getId();
    }

    @Override
    public void update(StrategyTagFieldDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("字段ID不能为空");
        }
        StrategyTagField field = strategyTagFieldMapper.selectById(dto.getId());
        if (field == null) {
            throw new BusinessException("字段不存在");
        }

        if (!field.getFieldKey().equals(dto.getFieldKey())) {
            LambdaQueryWrapper<StrategyTagField> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StrategyTagField::getFieldKey, dto.getFieldKey());
            if (strategyTagFieldMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("字段标识 " + dto.getFieldKey() + " 已存在");
            }
        }

        BeanUtil.copyProperties(dto, field, "id");
        strategyTagFieldMapper.updateById(field);
    }

    @Override
    public void delete(Long id) {
        StrategyTagField field = strategyTagFieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException("字段不存在");
        }
        strategyTagFieldMapper.deleteById(id);
    }

    @Override
    public void toggleStatus(Long id) {
        StrategyTagField field = strategyTagFieldMapper.selectById(id);
        if (field == null) {
            throw new BusinessException("字段不存在");
        }
        LambdaUpdateWrapper<StrategyTagField> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(StrategyTagField::getId, id)
                .set(StrategyTagField::getStatus, field.getStatus() == 1 ? 0 : 1);
        strategyTagFieldMapper.update(null, wrapper);
    }
}
