package com.strategy.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.common.Result;
import com.strategy.engine.dto.StrategySceneDTO;
import com.strategy.engine.dto.StrategySceneTagItemDTO;
import com.strategy.engine.service.StrategySceneService;
import com.strategy.engine.vo.StrategySceneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场景策略 Controller
 */
@Tag(name = "场景策略管理")
@RestController
@RequestMapping("/scene")
@RequiredArgsConstructor
public class StrategySceneController {

    private final StrategySceneService strategySceneService;

    @Operation(summary = "根据引擎ID分页查询场景列表")
    @GetMapping("/page/{engineId}")
    public Result<Page<StrategySceneVO>> pageByEngineId(
            @Parameter(description = "引擎ID") @PathVariable Long engineId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(strategySceneService.pageByEngineId(engineId, pageNum, pageSize));
    }

    @Operation(summary = "根据引擎ID获取所有场景（不分页）", description = "供调用方获取场景权重配置")
    @GetMapping("/list/{engineId}")
    public Result<List<StrategySceneVO>> listByEngineId(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        return Result.success(strategySceneService.listByEngineId(engineId));
    }

    @Operation(summary = "查询场景详情")
    @GetMapping("/{id}")
    public Result<StrategySceneVO> getById(@Parameter(description = "场景ID") @PathVariable Long id) {
        return Result.success(strategySceneService.getById(id));
    }

    @Operation(summary = "创建场景")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody StrategySceneDTO dto) {
        return Result.success("创建成功", strategySceneService.create(dto));
    }

    @Operation(summary = "更新场景")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody StrategySceneDTO dto) {
        strategySceneService.update(dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除场景")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "场景ID") @PathVariable Long id) {
        strategySceneService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "配置场景标签关联（全量替换）")
    @PostMapping("/{sceneId}/tags")
    public Result<Void> configSceneTags(
            @Parameter(description = "场景ID") @PathVariable Long sceneId,
            @Validated @RequestBody List<StrategySceneTagItemDTO> configs) {
        strategySceneService.configSceneTags(sceneId, configs);
        return Result.success("配置成功", null);
    }
}
