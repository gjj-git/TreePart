package com.strategy.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.common.Result;
import com.strategy.engine.dto.TagRuleDTO;
import com.strategy.engine.service.TagRuleService;
import com.strategy.engine.vo.TagRuleVO;
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
public class TagRuleController {

    private final TagRuleService tagRuleService;

    @Operation(summary = "根据引擎ID分页查询标签列表")
    @GetMapping("/page/{engineId}")
    public Result<Page<TagRuleVO>> pageByEngineId(
            @Parameter(description = "引擎ID") @PathVariable Long engineId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<TagRuleVO> page = tagRuleService.pageByEngineId(engineId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "查询标签详情")
    @GetMapping("/{id}")
    public Result<TagRuleVO> getById(@Parameter(description = "标签ID") @PathVariable Long id) {
        TagRuleVO vo = tagRuleService.getById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody TagRuleDTO dto) {
        Long id = tagRuleService.create(dto);
        return Result.success("创建成功", id);
    }

    @Operation(summary = "更新标签")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody TagRuleDTO dto) {
        tagRuleService.update(dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "标签ID") @PathVariable Long id) {
        tagRuleService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "根据引擎ID批量删除标签")
    @DeleteMapping("/batch/{engineId}")
    public Result<Void> batchDelete(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        tagRuleService.batchDelete(engineId);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "根据引擎ID获取所有标签（不分页）")
    @GetMapping("/list/{engineId}")
    public Result<List<TagRuleVO>> listByEngineId(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        List<TagRuleVO> list = tagRuleService.listByEngineId(engineId);
        return Result.success(list);
    }

    @Operation(summary = "根据引擎ID获取所有启用的标签（不分页）", description = "用于场景配置时选择标签")
    @GetMapping("/list/{engineId}/enabled")
    public Result<List<TagRuleVO>> listEnabledByEngineId(@Parameter(description = "引擎ID") @PathVariable Long engineId) {
        List<TagRuleVO> list = tagRuleService.listEnabledByEngineId(engineId);
        return Result.success(list);
    }

    @Operation(summary = "切换标签启用/禁用状态")
    @PutMapping("/{id}/toggleStatus")
    public Result<Void> toggleStatus(@Parameter(description = "标签ID") @PathVariable Long id) {
        tagRuleService.toggleStatus(id);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "查询标签使用情况", description = "查询有多少场景使用了该标签")
    @GetMapping("/{id}/usage")
    public Result<TagUsageVO> getTagUsage(@Parameter(description = "标签ID") @PathVariable Long id) {
        TagUsageVO vo = tagRuleService.getTagUsage(id);
        return Result.success(vo);
    }
}
