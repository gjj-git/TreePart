package com.strategy.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.common.Result;
import com.strategy.engine.dto.StrategyTagRuleDTO;
import com.strategy.engine.service.StrategyTagRuleService;
import com.strategy.engine.vo.StrategyTagRuleVO;
import com.strategy.engine.vo.TagUsageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签规则 Controller
 */
@Tag(name = "标签规则管理")
@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
public class StrategyTagRuleController {

    private final StrategyTagRuleService strategyTagRuleService;

    @Operation(summary = "根据引擎ID分页查询标签列表")
    @GetMapping("/page/{engineId}")
    public Result<Page<StrategyTagRuleVO>> pageByEngineId(
            @Parameter(description = "引擎ID") @PathVariable Long engineId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(strategyTagRuleService.pageByEngineId(engineId, pageNum, pageSize));
    }

    @Operation(summary = "查询标签详情")
    @GetMapping("/{id}")
    public Result<StrategyTagRuleVO> getById(@Parameter(description = "标签ID") @PathVariable Long id) {
        return Result.success(strategyTagRuleService.getById(id));
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody StrategyTagRuleDTO dto) {
        return Result.success("创建成功", strategyTagRuleService.create(dto));
    }

    @Operation(summary = "更新标签")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody StrategyTagRuleDTO dto) {
        strategyTagRuleService.update(dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "标签ID") @PathVariable Long id) {
        strategyTagRuleService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "根据引擎ID批量删除标签")
    @DeleteMapping("/batch/{engineId}")
    public Result<Void> batchDelete(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        strategyTagRuleService.batchDelete(engineId);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "根据引擎ID获取所有标签（不分页）")
    @GetMapping("/list/{engineId}")
    public Result<List<StrategyTagRuleVO>> listByEngineId(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        return Result.success(strategyTagRuleService.listByEngineId(engineId));
    }

    @Operation(summary = "根据引擎ID获取所有启用的标签（不分页）", description = "供调用方获取参与求值的规则")
    @GetMapping("/list/{engineId}/enabled")
    public Result<List<StrategyTagRuleVO>> listEnabledByEngineId(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        return Result.success(strategyTagRuleService.listEnabledByEngineId(engineId));
    }

    @Operation(summary = "切换标签启用/禁用状态")
    @PutMapping("/{id}/toggleStatus")
    public Result<Void> toggleStatus(@Parameter(description = "标签ID") @PathVariable Long id) {
        strategyTagRuleService.toggleStatus(id);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "查询标签使用情况")
    @GetMapping("/{id}/usage")
    public Result<TagUsageVO> getTagUsage(@Parameter(description = "标签ID") @PathVariable Long id) {
        return Result.success(strategyTagRuleService.getTagUsage(id));
    }
}
