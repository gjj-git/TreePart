package com.strategy.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.common.Result;
import com.strategy.engine.dto.SceneStrategyDTO;
import com.strategy.engine.dto.SceneTagConfigDTO;
import com.strategy.engine.service.SceneStrategyService;
import com.strategy.engine.vo.SceneStrategyVO;
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
public class SceneStrategyController {

    private final SceneStrategyService sceneStrategyService;

    @Operation(summary = "根据引擎ID分页查询场景列表")
    @GetMapping("/page/{engineId}")
    public Result<Page<SceneStrategyVO>> pageByEngineId(
            @Parameter(description = "引擎ID") @PathVariable Long engineId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SceneStrategyVO> page = sceneStrategyService.pageByEngineId(engineId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据引擎ID获取所有场景（不分页）", description = "用于编辑页面展示所有场景")
    @GetMapping("/list/{engineId}")
    public Result<List<SceneStrategyVO>> listByEngineId(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        List<SceneStrategyVO> list = sceneStrategyService.listByEngineId(engineId);
        return Result.success(list);
    }

    @Operation(summary = "查询场景详情")
    @GetMapping("/{id}")
    public Result<SceneStrategyVO> getById(@Parameter(description = "场景ID") @PathVariable Long id) {
        SceneStrategyVO vo = sceneStrategyService.getById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建场景")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody SceneStrategyDTO dto) {
        Long id = sceneStrategyService.create(dto);
        return Result.success("创建成功", id);
    }

    @Operation(summary = "更新场景")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SceneStrategyDTO dto) {
        sceneStrategyService.update(dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除场景")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "场景ID") @PathVariable Long id) {
        sceneStrategyService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "切换场景启用/禁用状态")
    @PutMapping("/{id}/toggleStatus")
    public Result<Void> toggleStatus(@Parameter(description = "场景ID") @PathVariable Long id) {
        sceneStrategyService.toggleStatus(id);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "批量配置场景标签关联")
    @PostMapping("/{sceneId}/tags")
    public Result<Void> configSceneTags(
            @Parameter(description = "场景ID") @PathVariable Long sceneId,
            @Validated @RequestBody List<SceneTagConfigDTO> configs) {
        sceneStrategyService.configSceneTags(sceneId, configs);
        return Result.success("配置成功", null);
    }
}
