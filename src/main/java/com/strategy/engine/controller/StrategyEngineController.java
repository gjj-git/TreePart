package com.strategy.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.strategy.engine.common.Result;
import com.strategy.engine.dto.StrategyEngineDTO;
import com.strategy.engine.dto.StrategyEngineQueryDTO;
import com.strategy.engine.enums.ApplicableObject;
import com.strategy.engine.enums.EngineType;
import com.strategy.engine.service.StrategyEngineService;
import com.strategy.engine.vo.StrategyEngineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Operation(summary = "获取枚举选项（引擎类型、适用对象）")
    @GetMapping("/enums")
    public Result<Map<String, List<Map<String, String>>>> getEnums() {
        List<Map<String, String>> engineTypes = Arrays.stream(EngineType.values())
                .map(e -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("code", e.getCode());
                    item.put("label", e.getDescription());
                    return item;
                }).collect(Collectors.toList());

        List<Map<String, String>> applicableObjects = Arrays.stream(ApplicableObject.values())
                .map(e -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("code", e.getCode());
                    item.put("label", e.getDescription());
                    return item;
                }).collect(Collectors.toList());

        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        result.put("engineTypes", engineTypes);
        result.put("applicableObjects", applicableObjects);
        return Result.success(result);
    }

    @Operation(summary = "获取默认引擎")
    @GetMapping("/default")
    public Result<StrategyEngineVO> getDefault(
            @Parameter(description = "适用对象类型") @RequestParam String applicableObject) {
        StrategyEngineVO vo = strategyEngineService.getDefault(applicableObject);
        return Result.success(vo);
    }
}
