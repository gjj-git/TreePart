package com.strategy.engine.controller;

import com.strategy.engine.common.Result;
import com.strategy.engine.dto.StrategyTagFieldDTO;
import com.strategy.engine.service.StrategyTagFieldService;
import com.strategy.engine.vo.StrategyTagFieldGroupVO;
import com.strategy.engine.vo.StrategyTagFieldVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 条件字段元数据 Controller
 */
@Tag(name = "条件字段管理")
@RestController
@RequestMapping("/field")
@RequiredArgsConstructor
public class StrategyTagFieldController {

    private final StrategyTagFieldService strategyTagFieldService;

    @Operation(summary = "根据引擎ID查询可用字段（按分类分组）", description = "供前端规则编辑器左侧字段库使用，自动按引擎适用对象过滤")
    @GetMapping("/grouped/{engineId}")
    public Result<List<StrategyTagFieldGroupVO>> listGroupedByEngineId(
            @Parameter(description = "引擎ID") @PathVariable Long engineId) {
        return Result.success(strategyTagFieldService.listGroupedByEngineId(engineId));
    }

    @Operation(summary = "查询所有字段（管理页面使用）")
    @GetMapping("/list")
    public Result<List<StrategyTagFieldVO>> listAll() {
        return Result.success(strategyTagFieldService.listAll());
    }

    @Operation(summary = "查询字段详情")
    @GetMapping("/{id}")
    public Result<StrategyTagFieldVO> getById(@Parameter(description = "字段ID") @PathVariable Long id) {
        return Result.success(strategyTagFieldService.getById(id));
    }

    @Operation(summary = "创建字段")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody StrategyTagFieldDTO dto) {
        return Result.success("创建成功", strategyTagFieldService.create(dto));
    }

    @Operation(summary = "更新字段")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody StrategyTagFieldDTO dto) {
        strategyTagFieldService.update(dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除字段")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "字段ID") @PathVariable Long id) {
        strategyTagFieldService.delete(id);
        return Result.success("删除成功", null);
    }
}
