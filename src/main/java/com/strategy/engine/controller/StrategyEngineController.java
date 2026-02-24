package com.strategy.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.common.Result;
import com.strategy.engine.dto.StrategyEngineDTO;
import com.strategy.engine.dto.StrategyEngineQueryDTO;
import com.strategy.engine.service.StrategyEngineService;
import com.strategy.engine.vo.StrategyEngineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 策略引擎 Controller
 */
@Tag(name = "策略引擎管理")
@RestController
@RequestMapping("/engine")
@RequiredArgsConstructor
public class StrategyEngineController {

    private final StrategyEngineService strategyEngineService;

    @Operation(summary = "分页查询引擎列表")
    @GetMapping("/page")
    public Result<Page<StrategyEngineVO>> pageQuery(@Validated StrategyEngineQueryDTO queryDTO) {
        Page<StrategyEngineVO> page = strategyEngineService.pageQuery(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "查询引擎详情")
    @GetMapping("/{id}")
    public Result<StrategyEngineVO> getById(@Parameter(description = "引擎ID") @PathVariable Long id) {
        StrategyEngineVO vo = strategyEngineService.getById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建引擎")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody StrategyEngineDTO dto) {
        Long id = strategyEngineService.create(dto);
        return Result.success("创建成功", id);
    }

    @Operation(summary = "更新引擎")
    @PutMapping
    public Result<Void> update(@Validated @RequestBody StrategyEngineDTO dto) {
        strategyEngineService.update(dto);
        return Result.success("更新成功", null);
    }

    @Operation(summary = "删除引擎")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "引擎ID") @PathVariable Long id) {
        strategyEngineService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "切换引擎状态")
    @PutMapping("/{id}/toggleStatus")
    public Result<Void> toggleStatus(@Parameter(description = "引擎ID") @PathVariable Long id) {
        strategyEngineService.toggleStatus(id);
        return Result.success("状态切换成功", null);
    }

    @Operation(summary = "设置为默认引擎")
    @PutMapping("/{id}/setDefault")
    public Result<Void> setDefault(@Parameter(description = "引擎ID") @PathVariable Long id) {
        strategyEngineService.setDefault(id);
        return Result.success("设置默认成功", null);
    }

    @Operation(summary = "取消默认引擎")
    @PutMapping("/{id}/cancelDefault")
    public Result<Void> cancelDefault(@Parameter(description = "引擎ID") @PathVariable Long id) {
        strategyEngineService.cancelDefault(id);
        return Result.success("取消默认成功", null);
    }
}
