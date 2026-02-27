package com.strategy.engine.controller;

import com.strategy.engine.common.Result;
import com.strategy.engine.dto.EngineFullConfigDTO;
import com.strategy.engine.service.EngineFullConfigService;
import com.strategy.engine.vo.EngineFullConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 【备用方案 - 当前未使用】
 * 引擎完整配置 Controller（统一保存方案）
 *
 * <p>本接口提供统一保存功能，允许一次性保存引擎的所有配置（基本信息 + 标签规则 + 场景策略）。</p>
 *
 * <h3>当前系统采用分步保存方案：</h3>
 * <ul>
 *   <li>Tab 1: 使用 {@code PUT /api/engine/{id}} 保存基本信息</li>
 *   <li>Tab 2: 使用 {@code POST/PUT/DELETE /api/tag} 操作标签</li>
 *   <li>Tab 3: 使用 {@code POST/PUT/DELETE /api/scene} 操作场景</li>
 * </ul>
 *
 * <p><strong>此 Controller 暂未启用</strong>，保留作为备选方案。如需启用统一保存功能，请参考：</p>
 * <ul>
 *   <li>设计文档：{@code docs/UNIFIED_SAVE_DESIGN.md}</li>
 *   <li>对比文档：{@code docs/FINAL_SOLUTION.md}</li>
 * </ul>
 *
 * @see com.strategy.engine.controller.StrategyEngineController 引擎管理（分步保存）
 * @see com.strategy.engine.controller.StrategyTagRuleController 标签管理（分步保存）
 * @see com.strategy.engine.controller.StrategySceneController 场景管理（分步保存）
 */
@Tag(name = "【备用】引擎完整配置管理（统一保存）")
@RestController
@RequestMapping("/engine-config")
@RequiredArgsConstructor
public class EngineFullConfigController {

    private final EngineFullConfigService engineFullConfigService;

    @Operation(summary = "获取引擎完整配置（三个Tab的所有数据）")
    @GetMapping("/{engineId}")
    public Result<EngineFullConfigVO> getFullConfig(
            @Parameter(description = "引擎ID") @PathVariable Long engineId) {
        EngineFullConfigVO vo = engineFullConfigService.getFullConfig(engineId);
        return Result.success(vo);
    }

    @Operation(summary = "保存引擎完整配置（创建或更新）", description = "统一保存三个Tab的所有配置，事务性操作")
    @PostMapping
    public Result<Long> saveFullConfig(@Validated @RequestBody EngineFullConfigDTO dto) {
        Long engineId = engineFullConfigService.saveFullConfig(dto);
        return Result.success("保存成功", engineId);
    }

    @Operation(summary = "验证配置有效性")
    @PostMapping("/validate")
    public Result<String> validateConfig(@RequestBody EngineFullConfigDTO dto) {
        String errorMsg = engineFullConfigService.validateConfig(dto);
        if (errorMsg != null) {
            return Result.error(errorMsg);
        }
        return Result.success("验证通过", null);
    }
}
